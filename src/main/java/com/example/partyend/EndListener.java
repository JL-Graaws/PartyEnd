package com.example.partyend;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.time.Instant;
import java.util.UUID;

final class EndListener implements Listener {
    private final PartyEndPlugin plugin;
    private final DataStore data;
    private final PartyManager parties;
    private final InstanceManager instances;

    EndListener(PartyEndPlugin plugin, DataStore data, PartyManager parties, InstanceManager instances) {
        this.plugin = plugin;
        this.data = data;
        this.parties = parties;
        this.instances = instances;
    }

    private boolean gateOpen(Player p) {
        if (p.hasPermission("partyend.bypass")) return true;
        String s = plugin.getConfig().getString("open-at", "");
        if (s == null || s.isBlank()) return true;
        try {
            return Instant.now().isAfter(Instant.parse(s));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid open-at value '" + s + "'; keeping the End closed.");
            return false;
        }
    }

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
        if (!gateOpen(p)) {
            p.sendMessage("The End hasn't opened yet. Keep an eye on announcements!");
            return;
        }
        UUID host = parties.hostOf(p.getUniqueId());
        World w = instances.getOrCreate(host);
        if (w == null) {
            p.sendMessage("The End is busy right now. Please try again in a moment.");
            return;
        }
        p.teleportAsync(instances.entryLocation(w));
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

    /** Mark everyone in the instance as cleared. Stored per player, so resets never bring the fight back. */
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
                // Parties reset on restart, so they no longer belong to that host's End.
                data.clearInInstance(id);
                p.teleportAsync(instances.exitLocation(p));
                p.sendMessage("Your party was reset by the restart, so you were moved out of that End.");
            }
        }

        instances.ejectIfInvalid(id);
        instances.warnIfExpiring(p, parties.hostOf(id));
    }
}
