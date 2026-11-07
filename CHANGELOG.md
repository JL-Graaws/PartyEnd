# Changelog

All notable changes to PartyEnd are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/), and the project uses [Semantic Versioning](https://semver.org/).

## [0.2.0] - 2026-11-07

### Added
- `/endparty reload` (permission `partyend.admin`) re-reads the config and reports whether the End is open.
- Parties are saved to `data.yml`, so they survive restarts.
- Dragon health scales with the size of the host's party when the dragon first appears (`dragon-health-per-extra-member` and `dragon-max-health-multiplier` in the config).

### Fixed
- Players riding a boat, minecart or other vehicle could enter an End portal without triggering the plugin, bypassing the opening date and the instances and landing in the old shared End. Vehicles are now intercepted, and riders go through the normal entry flow.

### Changed
- Only the host's party members can teleport into an instance (`/tp`, `/tpa` and similar); `partyend.admin` bypasses this. The old shared End is closed to non-admins, and anyone found in it on join is moved out.
- Killing the dragon only marks the host's party members as cleared. Other players who happen to be in the End are no longer credited.
- `open-at` now accepts words like `empty`, `none` and `now` as "open", accepts an unquoted time, and logs a clearer error for anything else.

### Upgrading from 0.1.1
Replace the jar and restart. Parties from before the restart are lost one last time, because earlier versions never saved them. New config options (`dragon-health-per-extra-member`, `dragon-max-health-multiplier`) are optional: if they are missing from your `config.yml`, the defaults are used. Set `open-at: ""` if you want the End open. Anyone already standing in the old shared End is moved out the next time they join.

## [0.1.1] - 2026-10-26

### Added
- `difficulty` config option: `inherit` (default, copies the main overworld) or `peaceful`, `easy`, `normal`, `hard`.

### Fixed
- End instances did not reliably follow the server's difficulty, so mobs could spawn at the wrong difficulty in a player's End. The difficulty is now set explicitly whenever an instance is created or loaded.

### Build
- The Gradle project name is pinned, so the jar is always named `PartyEnd-<version>.jar`.

### Upgrading from 0.1.0
Replace the jar and restart. Your existing `config.yml` is not updated automatically: add `difficulty: inherit` (or your chosen value) to it. Without the line the plugin behaves as `inherit`.

## [0.1.0] - 2026-10-17

First public release. Early and lightly tested; try it on a copy of your server first.

### Added
- Per-host End dimensions (`end_<hostUuid>`), created on demand when a player enters an End portal.
- Parties: `/endparty invite`, `accept`, `leave`, `kick`, `info` and `exit` (alias `/ep`). Members enter the host's End.
- Dragon fights decided by the host's record at the time an End is created. Dragonless Ends get an instant exit portal and one outer-island gateway.
- Per-player "cleared" records, so resetting or deleting an End never brings the fight back.
- Delayed opening via `open-at`, with the `partyend.bypass` permission for staff.
- Automatic cleanup: empty Ends unload at once; inactive Ends are deleted after a configurable number of days, with a login warning beforehand.
- Restore players into their End after a server restart.
- Dragon-kill detection that also catches `/kill`, not just the death event.
- Geyser/Floodgate support through UUID-keyed data (no extra setup).
- `/endparty info` shows whether each party member has cleared the dragon.

### Known issues
- Parties are stored in memory and reset on restart.
- Ends created while a player was uncleared keep their dragon until they expire or are deleted.
- The game, not the plugin, decides when a dragon spawns, which can take about a minute in a dragonless End before the plugin removes it.
- Leaving through the exit portal, post-restart restoring and gateway placement have had limited testing.
- Paper 26.2 only; built against `paper-api:26.2.build.121-stable`.

[0.2.0]: https://github.com/OWNER/REPO/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/OWNER/REPO/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/OWNER/REPO/releases/tag/v0.1.0
