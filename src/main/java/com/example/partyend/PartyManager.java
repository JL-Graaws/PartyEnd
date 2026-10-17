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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory parties. A player not in a party is their own host.
 * Parties reset on restart (players simply fall back to their own End).
 */
final class PartyManager {
    private static final long INVITE_MS = 60_000;

    private record Invite(UUID host, long expires) { }

    private final Map<UUID, UUID> hostOfMember = new HashMap<>();   // member -> host (host maps to itself)
    private final Map<UUID, Set<UUID>> members = new HashMap<>();   // host -> members incl. host
    private final Map<UUID, Invite> invites = new HashMap<>();      // target -> invite

    UUID hostOf(UUID id) { return hostOfMember.getOrDefault(id, id); }

    boolean inParty(UUID id) { return hostOfMember.containsKey(id); }

    boolean isHost(UUID id) { return id.equals(hostOfMember.get(id)); }

    Set<UUID> membersOf(UUID host) {
        Set<UUID> s = members.get(host);
        return s == null ? Set.of(host) : Set.copyOf(s);
    }

    /** @return error message, or null on success */
    String invite(UUID inviter, UUID target) {
        if (inviter.equals(target)) return "You can't invite yourself.";
        if (inParty(target)) return "That player is already in a party.";
        if (inParty(inviter) && !isHost(inviter)) return "Only the party host can invite.";
        invites.put(target, new Invite(inviter, System.currentTimeMillis() + INVITE_MS));
        return null;
    }

    /** @return error message, or null on success */
    String accept(UUID target) {
        Invite inv = invites.remove(target);
        if (inv == null || inv.expires() < System.currentTimeMillis()) return "You have no pending invite.";
        if (inParty(target)) return "Leave your current party first.";
        UUID host = inv.host();
        if (inParty(host) && !isHost(host)) return "That player is no longer a party host.";
        Set<UUID> set = members.computeIfAbsent(host, h -> {
            Set<UUID> s = new LinkedHashSet<>();
            s.add(h);
            return s;
        });
        hostOfMember.put(host, host);
        set.add(target);
        hostOfMember.put(target, host);
        return null;
    }

    /** @return players whose host may have changed (so the caller can eject them from an instance) */
    Set<UUID> leave(UUID id) {
        if (!inParty(id)) return Set.of();
        UUID host = hostOfMember.get(id);
        if (host.equals(id)) return disband(host);
        Set<UUID> set = members.get(host);
        set.remove(id);
        hostOfMember.remove(id);
        if (set.size() <= 1) disband(host);
        return Set.of(id);
    }

    /** @return error message, or null on success */
    String kick(UUID host, UUID target) {
        if (!isHost(host)) return "Only the party host can kick.";
        if (host.equals(target) || !host.equals(hostOfMember.get(target))) return "That player isn't in your party.";
        leave(target);
        return null;
    }

    private Set<UUID> disband(UUID host) {
        Set<UUID> s = members.remove(host);
        if (s == null) return Set.of();
        s.forEach(hostOfMember::remove);
        return s;
    }
}
