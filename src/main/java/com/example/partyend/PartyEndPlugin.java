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

        PartyCommand cmd = new PartyCommand(parties, instances, data);
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
