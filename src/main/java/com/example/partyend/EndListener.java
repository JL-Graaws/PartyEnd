/*
 * PartyEnd - per-party instanced End dimensions for Paper
 * Copyright (C) 2026 PartyEnd Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.partyend;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class EndListener implements Listener {
    private final PartyEndPlugin plugin;
    private final DataStore data;
    private final PartyManager parties;
    private final InstanceManager instances;
    private final Map<UUID, Long> lastNotice = new HashMap<>();

    EndListener(PartyEndPlugin plugin, DataStore data, PartyManager parties, InstanceManager instances) {
        this.plugin = plugin;
        this.data = data;
        this.parties = parties;
        this.instances = instances;
    }

    private boolean gateOpen(Player p) {
        if (p.hasPermission("partyend.bypass")) return true;
        if (!OpenGate.isOpen(plugin.getConfig())) {
            String why = OpenGate.describe(plugin.getConfig());
            if (why.startsWith("INVALID")) plugin.getLogger().warning(why);
            return false;
        }
        return true;
    }

    /** Sends a message at most once every 3 seconds per player (portals can fire every tick). */
    private void notice(Player p, String msg) {
        long now = System.currentTimeMillis();
        Long last = lastNotice.get(p.getUniqueId());
        if (last != null && now - last < 3000) return;
        lastNotice.put(p.getUniqueId(), now);
        p.sendMessage(msg);
    }

    /** The shared entry flow: gate check, then send the player to their host's End. */
    private void enterEnd(Player p) {
        if (!gateOpen(p)) {
            notice(p, "The End hasn't opened yet. Keep an eye on announcements!");
            return;
        }
        UUID host = parties.hostOf(p.getUniqueId());
        World w = instances.getOrCreate(host);
        if (w == null) {
            notice(p, "The End is busy right now. Please try again in a moment.");
            return;
        }
        p.teleportAsync(instances.entryLocation(w));
    }

    /** A player walking into an End portal. */
    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.END_PORTAL) return;
        Player p = e.getPlayer();
        World from = e.getFrom().getWorld();

        // Leaving an instance (if this event fires for the exit portal on your build)
        if (instances.isInstance(from)) {
            e.setTo(instances.exitLocation(p));
            return;
        }

        // Entering: only intercept trips whose destination is an End
        World to = e.getTo() == null ? null : e.getTo().getWorld();
        if (to == null || to.getEnvironment() != World.Environment.THE_END) return;

        e.setCancelled(true);
        enterEnd(p);
    }

    /**
     * A vehicle (boat, minecart, rideable mob) or any other entity entering an End portal. Riders do NOT
     * trigger PlayerPortalEvent; the vehicle triggers this instead and carries them along, which used to
     * bypass the gate and the instances. Cancel it for everything, and send any riders through the normal flow.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent e) {
        Location to = e.getTo();
        if (to == null || to.getWorld() == null || to.getWorld().getEnvironment() != World.Environment.THE_END) return;
        if (instances.isInstance(e.getFrom().getWorld())) return;

        e.setCancelled(true);
        List<Player> riders = new ArrayList<>();
        collectRiders(e.getEntity(), riders);
        if (riders.isEmpty()) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Player p : riders) {
                if (!p.isOnline()) continue;
                p.leaveVehicle();
                enterEnd(p);
            }
        });
    }

    private void collectRiders(Entity vehicle, List<Player> out) {
        for (Entity passenger : vehicle.getPassengers()) {
            if (passenger instanceof Player p) out.add(p);
            collectRiders(passenger, out);
        }
    }

    /**
     * Commands and plugins (/tp, /tpa, ...). Nobody but admins may teleport into someone else's End, or
     * into the old shared End. Walking through a portal is handled above.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Location to = e.getTo();
        if (to == null || to.getWorld() == null) return;
        Player p = e.getPlayer();
        if (p.hasPermission("partyend.admin")) return;

        World w = to.getWorld();
        if (instances.isInstance(w)) {
            UUID host = instances.hostOfWorld(w);
            if (host != null && !parties.membersOf(host).contains(p.getUniqueId())) {
                e.setCancelled(true);
                notice(p, "That End belongs to another party.");
            }
        } else if (w.getEnvironment() == World.Environment.THE_END) {
            e.setCancelled(true);
            notice(p, "That End isn't available. Use an End portal to reach your own.");
        }
    }

    /** Remember which instance a player is in, so a server restart can put them back. */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        World now = p.getWorld();
        if (instances.isInstance(now)) {
            try {
                data.setInInstance(p.getUniqueId(),
                        UUID.fromString(now.getName().substring(InstanceManager.PREFIX.length())));
            } catch (IllegalArgumentException ignored) { }
        } else {
            data.clearInInstance(p.getUniqueId());
        }
    }

    /** Mark the host's party members in the instance as cleared. Stored per player, so resets never bring the fight back. */
    @EventHandler
    public void onDragonDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderDragon)) return;
        World w = e.getEntity().getWorld();
        if (!instances.isInstance(w)) return;
        UUID host = instances.hostOfWorld(w);
        if (host != null && data.suppress(host)) return; // the plugin's own cleanup kill, not a player clear
        instances.markCleared(w);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        UUID saved = data.inInstance(id);

        // Anyone who got into the old shared End (for example before this fix) is moved out.
        World cur = p.getWorld();
        if (cur.getEnvironment() == World.Environment.THE_END && !instances.isInstance(cur)
                && !p.hasPermission("partyend.admin")) {
            p.teleportAsync(instances.exitLocation(p));
            p.sendMessage("That End isn't available, so you were moved out. Use an End portal to reach your own.");
        }

        // After a restart the instance isn't loaded, so the server dropped them into the overworld.
        if (saved != null && !instances.isInstance(p.getWorld())) {
            if (saved.equals(parties.hostOf(id))) {
                World w = instances.getOrCreate(saved);
                if (w != null) {
                    Location l = p.getLocation();
                    p.teleportAsync(new Location(w, l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()));
                } else {
                    data.clearInInstance(id);
                    p.teleportAsync(instances.exitLocation(p));
                    p.sendMessage("The End is busy right now, so you were moved to safety.");
                }
            } else {
                // The player no longer belongs to that host's End.
                data.clearInInstance(id);
                p.teleportAsync(instances.exitLocation(p));
                p.sendMessage("You're no longer part of that End, so you were moved out.");
            }
        }

        instances.ejectIfInvalid(id);
        instances.warnIfExpiring(p, parties.hostOf(id));
    }
}
