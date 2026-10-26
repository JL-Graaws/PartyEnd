## PartyEnd v0.1.1

A small update that fixes End difficulty and makes it configurable.

> Still an early release. Back up your server and try updates on a copy first.

### What's new
- **New `difficulty` setting.** Choose `inherit` (the default, copies your main overworld) or force `peaceful`, `easy`, `normal` or `hard` for every player's End.
- **Fix:** End instances didn't reliably follow the server's difficulty, so mobs could spawn at the wrong difficulty. It's now set explicitly whenever an End is created or loaded.

### Upgrading from 0.1.0
1. Stop the server, replace the old jar in `plugins/` with `PartyEnd-0.1.1.jar`, and start it again.
2. Add this line to your existing `plugins/PartyEnd/config.yml` (it isn't added automatically):
   ```yaml
   difficulty: inherit
   ```
   Without it the plugin behaves as `inherit`.

### Requirements
Paper 26.2 and the Java version it requires. No other plugins are needed.

### Known issues
- Parties reset on restart.
- Ends created before a player cleared the dragon keep their dragon until they expire.
- A dragon in a dragonless End can take about a minute to appear before it is removed.
- Some paths (exit portal, post-restart restore, gateway placement) have had limited testing.

Full details are in the [README](https://github.com/<your-username>/PartyEnd/blob/main/README.md) and [CHANGELOG](https://github.com/<your-username>/PartyEnd/blob/main/CHANGELOG.md).

Licensed under GPL-3.0-or-later.
