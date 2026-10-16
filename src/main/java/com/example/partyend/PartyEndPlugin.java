package com.example.partyend;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PartyEndPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        DataStore data = new DataStore(this);
        PartyManager parties = new PartyManager();
        InstanceManager instances = new InstanceManager(this, data, parties);

        getServer().getPluginManager().registerEvents(new EndListener(this, data, parties, instances), this);

        PartyCommand cmd = new PartyCommand(parties, instances);
        PluginCommand pc = getCommand("endparty");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        // detect dragon kills that skip the death event (e.g. /kill), every second
        getServer().getScheduler().runTaskTimer(this, instances::checkClears, 20L, 20L);

        // first run after 1 minute, then every 10 minutes
        getServer().getScheduler().runTaskTimer(this, instances::cleanup, 20L * 60, 20L * 60 * 10);
    }
}
