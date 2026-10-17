# Changelog

All notable changes to PartyEnd are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/), and the project uses [Semantic Versioning](https://semver.org/).

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

[0.1.0]: https://github.com/joshliddle03-glitch/EndPlugin/releases/tag/v0.1.0
