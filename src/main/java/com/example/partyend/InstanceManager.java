package com.example.partyend;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.boss.DragonBattle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/** Creates, resolves, unloads and deletes per-host End worlds named end_<hostUuid>. */
final class InstanceManager {
    static final String PREFIX = "end_";
    private static final long DAY = 24L * 60 * 60 * 1000;

    private final PartyEndPlugin plugin;
    private final DataStore data;
    private final PartyManager parties;

    InstanceManager(PartyEndPlugin plugin, DataStore data, PartyManager parties) {
        this.plugin = plugin;
        this.data = data;
        this.parties = parties;
    }

    String worldName(UUID host) { return PREFIX + host; }

    boolean isInstance(World w) { return w != null && w.getName().startsWith(PREFIX); }

    private int loadedCount() {
        int n = 0;
        for (World w : Bukkit.getWorlds()) if (isInstance(w)) n++;
        return n;
    }

    /** Main thread only. Returns null if the server is at its instance cap or creation failed. */
    World getOrCreate(UUID host) {
        String name = worldName(host);
        World w = Bukkit.getWorld(name);
        if (w != null) { data.touch(host); return w; }

        if (loadedCount() >= plugin.getConfig().getInt("max-loaded-instances", 10)) return null;

        boolean fresh = !data.hasInstance(host);
        long seed = fresh ? plugin.getConfig().getLong("seed") : data.seed(host);
        if (fresh) data.createInstance(host, seed);

        w = new WorldCreator(name).environment(World.Environment.THE_END).seed(seed).createWorld();
        if (w == null) return null;

        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(plugin.getConfig().getInt("border-radius", 1500) * 2.0);

        // Core rule: the HOST's record decides whether this End has a dragon fight.
        if (fresh && data.hasCleared(host)) {
            DragonBattle battle = w.getEnderDragonBattle();
            if (battle != null) {
                battle.setPreviouslyKilled(true);
                data.setNoFight(host);
                // Don't make players wait for the dragon to spawn and die: build the exit portal now.
                try {
                    w.getChunkAt(0, 0).load(true); // the portal sits at the world origin
                    battle.generateEndPortal(true);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Could not pre-generate the exit portal: " + ex.getMessage());
                }
            }
            data.setSuppress(host);
        }
        data.touch(host);
        return w;
    }

    /** Vanilla-style obsidian platform at 100,48,0, rebuilt if missing. */
    Location entryLocation(World w) {
        w.getChunkAt(100 >> 4, 0).load(true);
        if (w.getBlockAt(100, 48, 0).getType() == Material.AIR) {
            for (int x = 98; x <= 102; x++)
                for (int z = -2; z <= 2; z++) {
                    w.getBlockAt(x, 48, z).setType(Material.OBSIDIAN);
                    for (int y = 49; y <= 51; y++) w.getBlockAt(x, y, z).setType(Material.AIR);
                }
        }
        return new Location(w, 100.5, 49, 0.5, 90f, 0f);
    }

    Location exitLocation(Player p) {
        Location l = p.getRespawnLocation();
        if (l == null || isInstance(l.getWorld())) l = Bukkit.getWorlds().get(0).getSpawnLocation();
        return l;
    }

    /** If the player is standing in an instance that isn't their party host's, send them out. */
    void ejectIfInvalid(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p == null) return;
        String here = p.getWorld().getName();
        if (here.startsWith(PREFIX) && !here.equals(worldName(parties.hostOf(id)))) {
            p.teleportAsync(exitLocation(p));
            p.sendMessage("You're no longer part of that End, so you were moved out.");
        }
    }

    // ---- clear detection ----

    UUID hostOfWorld(World w) {
        if (!isInstance(w)) return null;
        try { return UUID.fromString(w.getName().substring(PREFIX.length())); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** Marks everyone currently in the instance as having beaten the dragon. */
    void markCleared(World w) {
        for (Player p : w.getPlayers()) {
            data.setCleared(p.getUniqueId());
            p.sendMessage("You've defeated the Ender Dragon!");
        }
        UUID host = hostOfWorld(w);
        if (host != null) data.setNoFight(host);
    }

    /**
     * Scheduled task. The death event is skipped when a dragon is removed with /kill, so also watch the
     * battle's "previously killed" flag, which is set either way.
     */
    void checkClears() {
        for (World w : Bukkit.getWorlds()) {
            if (!isInstance(w) || w.getPlayers().isEmpty()) continue;
            UUID host = hostOfWorld(w);
            if (host == null) continue;

            // Instance made for an already-cleared host: kill the first dragon that appears. Killing it
            // through the normal path finishes the fight (exit portal, gateways) and stops it respawning.
            if (data.suppress(host)) {
                boolean killed = false;
                for (EnderDragon d : w.getEntitiesByClass(EnderDragon.class)) {
                    if (d.getHealth() > 0) { d.setHealth(0); killed = true; }
                }
                if (killed) data.clearSuppress(host); // later dragons (end crystals) are left alone
                continue;
            }

            if (data.noFight(host)) continue;
            DragonBattle battle = w.getEnderDragonBattle();
            if (battle != null && battle.hasBeenPreviouslyKilled()) markCleared(w);
        }
    }

    // ---- retention ----

    private boolean allCleared(UUID host) {
        return parties.membersOf(host).stream().allMatch(data::hasCleared);
    }

    private long limitMs(UUID host) {
        int days = allCleared(host)
                ? plugin.getConfig().getInt("cleared-retention-days", 7)
                : plugin.getConfig().getInt("retention-days", 14);
        return days * DAY;
    }

    void warnIfExpiring(Player p, UUID host) {
        if (!data.hasInstance(host)) return;
        long left = limitMs(host) - (System.currentTimeMillis() - data.lastActive(host));
        long warn = plugin.getConfig().getInt("warn-days", 3) * DAY;
        if (left > warn) return;
        long days = Math.max(0, (left + DAY - 1) / DAY);
        p.sendMessage("Your End will be deleted in about " + days
                + " day(s) unless you visit it. Anything left there will be lost.");
    }

    /** Scheduled task: unload empty instances (keeping files), delete expired ones. */
    void cleanup() {
        for (World w : new ArrayList<>(Bukkit.getWorlds())) {
            if (!isInstance(w)) continue;
            if (w.getPlayers().isEmpty()) {
                Bukkit.unloadWorld(w, true);
            } else {
                try { data.touch(UUID.fromString(w.getName().substring(PREFIX.length()))); }
                catch (IllegalArgumentException ignored) { }
            }
        }

        long now = System.currentTimeMillis();
        for (UUID host : data.hosts()) {
            if (Bukkit.getWorld(worldName(host)) != null) continue; // still loaded
            if (now - data.lastActive(host) <= limitMs(host)) continue;

            // NOTE: if Paper 26.2 stores worlds somewhere other than the world container, adjust this path.
            Path dir = new java.io.File(Bukkit.getWorldContainer(), worldName(host)).toPath();
            data.removeInstance(host);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteRecursively(dir));
            plugin.getLogger().info("Deleted expired End instance for " + host);
        }
    }

    private void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException e) {
                    plugin.getLogger().warning("Could not delete " + p + ": " + e.getMessage());
                }
            });
        } catch (IOException e) {
            plugin.getLogger().warning("Could not delete " + dir + ": " + e.getMessage());
        }
    }
}
