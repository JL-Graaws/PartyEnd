## PartyEnd v0.1.0

Give every player or party their own private End, so nobody races for the dragon, the End cities or the loot.

> **Early release.** This version is lightly tested. Back up your server and try it on a copy first.

### Highlights
- A separate End per party host, created on demand
- Parties so groups can fight the dragon together, each in their own time
- Players who have already cleared the dragon get a dragonless End, with an exit portal and an outer-island gateway ready on arrival
- Delayed opening date, automatic cleanup of inactive Ends, and restart-safe player restoring
- Works with Geyser/Floodgate (Bedrock) players out of the box

### Requirements
- Paper 26.2
- The Java version Paper 26.2 requires
- No other plugins needed

### Install
1. Stop your server and put `PartyEnd-0.1.0.jar` in `plugins/`.
2. Start the server, then edit `plugins/PartyEnd/config.yml` if you like and restart.

### Commands
`/endparty invite <player>`, `accept`, `leave`, `kick <player>`, `info`, `exit` (alias `/ep`)

### Known issues
- Parties reset on restart.
- Ends created before a player cleared the dragon keep their dragon until they expire.
- A dragon in a dragonless End can take about a minute to appear before it is removed.
- Some paths (exit portal, post-restart restore, gateway placement) have had limited testing.

Full details are in the [README](README.md) and [CHANGELOG](CHANGELOG.md).

Licensed under GPL-3.0-or-later.
