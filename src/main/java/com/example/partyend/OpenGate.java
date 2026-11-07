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

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

/** Reads the open-at setting leniently. */
final class OpenGate {
    private static final List<String> OPEN_WORDS = List.of("empty", "none", "now", "open", "off", "false", "null");

    private OpenGate() { }

    /**
     * @return the time the End opens, or null if it is open now
     * @throws IllegalArgumentException if the value isn't a usable time
     */
    static Instant opensAt(FileConfiguration cfg) {
        Object v = cfg.get("open-at");
        if (v == null) return null;
        if (v instanceof Date d) return d.toInstant(); // YAML turns an unquoted ISO time into a Date
        String s = v.toString().trim();
        if (s.isEmpty() || OPEN_WORDS.contains(s.toLowerCase())) return null;
        try {
            return Instant.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("'" + s + "' is not a valid time. Use a UTC time like "
                    + "2026-11-02T18:00:00Z, or \"\" to open now.");
        }
    }

    /** True if the End is open now. An invalid value counts as closed. */
    static boolean isOpen(FileConfiguration cfg) {
        try {
            Instant t = opensAt(cfg);
            return t == null || Instant.now().isAfter(t);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Plain-English status, for logs and /endparty reload. */
    static String describe(FileConfiguration cfg) {
        try {
            Instant t = opensAt(cfg);
            if (t == null) return "open now";
            return Instant.now().isAfter(t) ? "open (since " + t + ")" : "closed until " + t;
        } catch (IllegalArgumentException e) {
            return "INVALID open-at, so the End is closed: " + e.getMessage();
        }
    }
}
