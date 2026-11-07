## PartyEnd v0.2.0

Parties now survive restarts, the dragon scales with party size, and config changes no longer need a restart.

> Still an early release. Back up your server and try updates on a copy first.

### What's new
- **Saved parties.** Parties are written to `data.yml`, so a restart keeps them and members are put back in their host's End.
- **Dragon health scales with party size.** Max health is multiplied by `1 + 0.5 × (partySize − 1)`, capped at 5×. The size is read when the dragon first appears. Tune it with `dragon-health-per-extra-member` and `dragon-max-health-multiplier`, or set the first to `0` to turn it off.
- **`/endparty reload`.** Admins (permission `partyend.admin`, ops by default) can re-read `config.yml` without restarting. It also reports whether the End is currently open.
- **Friendlier `open-at`.** Empty, `none` or `now` all mean "open", an unquoted time works, and a bad value now logs a clear message about the expected format instead of silently keeping the End closed.

### Fixes and tighter access
- **Vehicle bypass fixed.** Players riding a boat, minecart or other vehicle could enter an End portal without the plugin noticing, skipping the opening date and landing in the old shared End. Riders now go through the normal entry flow.
- **Only the host's party can enter a host's End.** `/tp`, `/tpa` and similar teleports into someone else's End are refused, and the old shared End is closed to non-admins. Anyone found in the old End when they join is moved out. Players with `partyend.admin` (ops by default) are exempt.
- **Fairer dragon credit.** Killing the dragon now marks only the host's party members as cleared, not everyone who happens to be standing in the End.

### Upgrading from 0.1.1
1. Stop the server, replace the old jar in `plugins/` with `PartyEnd-0.2.0.jar`, and start it again.
2. Parties from before this restart are lost one last time, since older versions never saved them. Players can re-form them with `/endparty invite` and `/endparty accept`, and they persist from now on.
3. Optional: add the new dragon-scaling lines to your existing `config.yml`. If you don't, the defaults are used and scaling is on.
4. Check `open-at` is `""` (or a past time) if you want the End open.
5. Anyone already standing in the old shared End is moved out the next time they join.

### Requirements
Paper 26.2 and the Java version it requires. No other plugins are needed.

### Known issues
- Ends created before a player cleared the dragon keep their dragon until they expire.
- A dragon in a dragonless End can take about a minute to appear before it is removed.
- Dragon health scaling and a few paths (exit portal, post-restart restore, gateway placement) have had limited testing.
- A new jar still needs a server restart; only config changes can be reloaded.

Full details are in the [README](https://github.com/JL-Graaws/PartyEnd/blob/main/README.md) and [CHANGELOG](https://github.com/JL-Graaws/PartyEnd/blob/main/CHANGELOG.md).

Licensed under GPL-3.0-or-later.
