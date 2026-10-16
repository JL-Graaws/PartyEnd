package com.example.partyend;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tiny YAML-backed store. Everything is keyed by UUID (safe for Floodgate players). */
final class DataStore {
    private final PartyEndPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;

    DataStore(PartyEndPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    // --- players ---
    boolean hasCleared(UUID id) { return yml.getBoolean("cleared." + id, false); }

    void setCleared(UUID id) { yml.set("cleared." + id, true); save(); }

    // --- where players were last standing (so a restart can put them back) ---
    UUID inInstance(UUID player) {
        String s = yml.getString("inInstance." + player);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    void setInInstance(UUID player, UUID host) { yml.set("inInstance." + player, host.toString()); save(); }

    void clearInInstance(UUID player) {
        if (yml.contains("inInstance." + player)) { yml.set("inInstance." + player, null); save(); }
    }

    // --- instances ---
    boolean hasInstance(UUID host) { return yml.contains("instances." + host); }

    void createInstance(UUID host, long seed) {
        yml.set("instances." + host + ".seed", seed);
        yml.set("instances." + host + ".created", System.currentTimeMillis());
        yml.set("instances." + host + ".lastActive", System.currentTimeMillis());
        save();
    }

    /** True once an instance has no live dragon fight (created dragonless, or the dragon was killed). */
    boolean noFight(UUID host) { return yml.getBoolean("instances." + host + ".noFight", false); }

    void setNoFight(UUID host) { yml.set("instances." + host + ".noFight", true); save(); }

    /** Set on instances created for an already-cleared host: the first dragon that spawns is killed off. */
    boolean suppress(UUID host) { return yml.getBoolean("instances." + host + ".suppress", false); }

    void setSuppress(UUID host) { yml.set("instances." + host + ".suppress", true); save(); }

    void clearSuppress(UUID host) { yml.set("instances." + host + ".suppress", null); save(); }

    long seed(UUID host) { return yml.getLong("instances." + host + ".seed"); }

    long lastActive(UUID host) { return yml.getLong("instances." + host + ".lastActive"); }

    void touch(UUID host) { yml.set("instances." + host + ".lastActive", System.currentTimeMillis()); save(); }

    void removeInstance(UUID host) { yml.set("instances." + host, null); save(); }

    Set<UUID> hosts() {
        Set<UUID> out = new HashSet<>();
        ConfigurationSection s = yml.getConfigurationSection("instances");
        if (s == null) return out;
        for (String k : s.getKeys(false)) {
            try { out.add(UUID.fromString(k)); } catch (IllegalArgumentException ignored) { }
        }
        return out;
    }

    private void save() {
        try { yml.save(file); } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }
}
