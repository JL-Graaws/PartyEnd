# PartyEnd

A Paper plugin that gives every player (or party) their own private End dimension, so nobody races for the dragon, the End cities or the loot.

Built for Paper 26.2 with Geyser/Floodgate (Bedrock) servers in mind.

> **Status:** early and lightly tested. See [Known limitations](#known-limitations) before running it on a server you care about, and try it on a copy first.

## Features

- **One End per host.** Entering an End portal sends you to `end_<hostUuid>`, created on demand. Solo players are their own host.
- **Parties.** Members enter the host's End, so a group can fight the dragon together, each in their own time.
- **Dragon fights follow the host.** If the host has never killed the dragon, the End has a normal fight. If the host has, the End is created dragonless, with an exit portal and an outer-island gateway already in place.
- **"Cleared" is stored per player,** not per world, so resetting or deleting an End never brings the fight back for someone who has done it.
- **Delayed opening.** Set a date before which nobody (except players with the bypass permission) can enter.
- **Automatic cleanup.** Empty Ends unload straight away. Ends are deleted after a period of inactivity, with a login warning beforehand.
- **Restart-safe.** Players who were in an End when the server restarted are put back into it on join.
- **Bedrock friendly.** Everything is keyed by UUID, so Floodgate players work with no extra setup.

## Requirements

- Paper 26.2 (it uses Paper APIs, so Spigot and Bukkit-only servers won't work)
- The Java version Paper 26.2 requires (the project is set up for Java 25)
- No other plugins are required. Geyser and Floodgate are optional.

## Installation

1. Download the jar (from your build artifacts, or build it yourself, see [Building](#building)).
2. Stop the server and put the jar in `plugins/`.
3. Start the server. `plugins/PartyEnd/config.yml` is created on first run.
4. Edit the config if you like, then restart.

Avoid `/reload`; restart the server instead.

## Commands

`/ep` works as an alias for `/endparty`.

| Command | Description |
|---|---|
| `/endparty invite <player>` | Invite an online player. You become the host if you aren't in a party. Invites expire after 60 seconds. |
| `/endparty accept` | Accept a pending invite. |
| `/endparty leave` | Leave your party. If the host leaves, the party disbands. |
| `/endparty kick <player>` | Host only. Remove a member. |
| `/endparty info` | Show the host and members, and whether each has cleared the dragon. |
| `/endparty exit` | Move yourself out of an End instance to your respawn point. |

Bedrock players are typed with their Floodgate prefix (for example `.Graaws`).

## Permissions

| Permission | Default | Description |
|---|---|---|
| `partyend.bypass` | op | Enter the End before the `open-at` date. |

## Configuration

`plugins/PartyEnd/config.yml`

| Key | Default | Description |
|---|---|---|
| `open-at` | `""` | ISO-8601 time the End opens, for example `2026-11-01T18:00:00Z`. Empty means open now. An invalid value keeps the End closed. |
| `seed` | `20261015` | Seed for new Ends only. Existing Ends keep theirs. |
| `border-radius` | `1500` | World border radius per End. Keep it above about 1300 so outer-island gateways stay inside the border. |
| `retention-days` | `14` | Days of inactivity before an End is deleted. |
| `cleared-retention-days` | `7` | Same, once every member of the host's party has cleared. |
| `warn-days` | `3` | Warn players on login when their End will be deleted within this many days. |
| `max-loaded-instances` | `10` | Cap on Ends loaded at once. World creation is main-thread work. |

## How it works

- Portal trips into the End are cancelled and replaced with a teleport into the player's host End, creating it if needed.
- The host's cleared status is read **once, when an End is created**. A player who clears the dragon later does not change Ends that already exist.
- For a dragonless End, the plugin builds the exit portal and one gateway straight away, then removes the first dragon the game spawns (if any) through the normal death path so the fight finishes cleanly. That cleanup kill never counts as a clear for anyone.
- Dragon kills are detected both from the death event and by watching the battle's state, so `/kill` and normal fights are both recorded.
- Data lives in `plugins/PartyEnd/data.yml`: which Ends exist, who has cleared, and where players last stood.

## Resetting for testing

1. Stop the server.
2. Delete the `end_<uuid>` world folders from the server directory.
3. Delete `plugins/PartyEnd/data.yml` (or just the `instances:`, `inInstance:` and `cleared:` sections you want gone).
4. Start the server.

Delete the folders and the data together. Deleting only one leaves the plugin and the worlds disagreeing about what exists.

## Known limitations

- **Parties are in memory only.** A restart disbands them; players fall back to their own End.
- **Existing Ends are never re-checked.** If a player's own End was created while they were uncleared, it keeps its dragon even after they clear elsewhere, until it expires or is deleted.
- **The dragon-cleanup relies on the game's own timer.** The game decides when a dragon spawns, which can be about a minute after arrival. The plugin cannot shorten that.
- **Untested on some paths.** Leaving through the exit portal, restoring players after a restart and the gateway placement have had limited testing. Report anything odd.
- **World folder location.** Deletion assumes Ends live at `<world container>/end_<uuid>`.
- **EssentialsX** replaces vanilla `/kill` with its own version. Use `/minecraft:kill`, or add `kill` to `disabled-commands` in Essentials' config.
- **Bedrock block rendering** (such as obsidian showing as a "?") is a Geyser or client matter, not something this plugin controls.

## Building

Requires JDK 25 (or whatever Paper 26.2 needs) and Gradle.

```
gradle build
```

The jar is written to `build/libs/`. The Paper API version is set in `build.gradle.kts`; update it if you target a different build.

You can also build with GitHub Actions: add a workflow that sets up Java, runs `gradle build` and uploads `build/libs/*.jar` as an artifact.

## Contributing

Issues and pull requests are welcome. When reporting a bug, include your Paper version, Geyser/Floodgate versions if used, and the relevant console output.

## License

MIT. See [LICENSE](LICENSE). Replace the copyright holder's name in that file with your own before publishing.

Not affiliated with or endorsed by Mojang or PaperMC.
