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

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class PartyCommand implements TabExecutor {
    private final PartyManager parties;
    private final InstanceManager instances;
    private final DataStore data;

    PartyCommand(PartyManager parties, InstanceManager instances, DataStore data) {
        this.parties = parties;
        this.instances = instances;
        this.data = data;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) { p.sendMessage("Usage: /endparty <invite|accept|leave|kick|info|exit> [player]"); return true; }
        UUID me = p.getUniqueId();

        switch (args[0].toLowerCase()) {
            case "invite" -> {
                Player t = target(p, args);
                if (t == null) return true;
                String err = parties.invite(me, t.getUniqueId());
                if (err != null) { p.sendMessage(err); return true; }
                p.sendMessage("Invited " + t.getName() + ". The invite lasts 60 seconds.");
                t.sendMessage(p.getName() + " invited you to their End party. Type /endparty accept.");
            }
            case "accept" -> {
                String err = parties.accept(me);
                if (err != null) { p.sendMessage(err); return true; }
                p.sendMessage("You joined the party. You will now enter the host's End.");
                instances.ejectIfInvalid(me); // if you were standing in your own End, move out
            }
            case "leave" -> {
                if (!parties.inParty(me)) { p.sendMessage("You aren't in a party."); return true; }
                Set<UUID> affected = parties.leave(me);
                p.sendMessage("You left the party.");
                affected.forEach(instances::ejectIfInvalid);
            }
            case "kick" -> {
                Player t = target(p, args);
                if (t == null) return true;
                String err = parties.kick(me, t.getUniqueId());
                if (err != null) { p.sendMessage(err); return true; }
                p.sendMessage("Kicked " + t.getName() + ".");
                t.sendMessage("You were removed from the party.");
                instances.ejectIfInvalid(t.getUniqueId());
            }
            case "info" -> {
                UUID host = parties.hostOf(me);
                p.sendMessage("Host: " + describe(host));
                for (UUID m : parties.membersOf(host)) {
                    if (!m.equals(host)) p.sendMessage("Member: " + describe(m));
                }
            }
            case "exit" -> {
                if (!instances.isInstance(p.getWorld())) { p.sendMessage("You aren't in an End instance."); return true; }
                p.teleportAsync(instances.exitLocation(p));
                p.sendMessage("Moved out of the End.");
            }
            default -> p.sendMessage("Usage: /endparty <invite|accept|leave|kick|info|exit> [player]");
        }
        return true;
    }

    private String describe(UUID id) {
        return Bukkit.getOfflinePlayer(id).getName() + " - "
                + (data.hasCleared(id) ? "has cleared the dragon" : "has NOT cleared the dragon");
    }

    private Player target(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage("Specify a player."); return null; }
        Player t = Bukkit.getPlayerExact(args[1]); // Bedrock names include the Floodgate prefix
        if (t == null) p.sendMessage("Player not found (they must be online).");
        return t;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("invite", "accept", "leave", "kick", "info", "exit");
        if (a.length == 2 && (a[0].equalsIgnoreCase("invite") || a[0].equalsIgnoreCase("kick")))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
