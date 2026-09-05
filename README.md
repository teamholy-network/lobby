# Holy Lobby

> [!IMPORTANT]
> **This repository is archived and read-only.** It is preserved as a historical snapshot of the TeamHoly Network lobby plugin. It is no longer maintained and receives no support, updates, or security fixes.

Holy Lobby is the legacy Minecraft lobby plugin used by the TeamHoly Network. It targets the Spigot/CraftBukkit 1.8.8 server API and connects the lobby experience to TeamHoly's shared services through the [Holy Core repository](https://github.com/teamholy-network/holy-core), which is also open-sourced and archived.

This repository is documentation and reference material. It is not a standalone or production-ready server distribution.

## Features

- Player session setup, hotbar navigation, scoreboards, ranks, coins, and playtime
- NPC-based game navigation and seasonal event entry points
- CloudNet service discovery, server selection, quick join, and lobby switching
- Bedwars and Rush Bedwars server browsers and live spectating menus
- Friend lists, friend requests, party actions, and friend-server jumping
- Player settings, perks, automatic nicking, and statistics resets
- Redis-backed daily, monthly, and all-time leaderboards
- Holographic game status displays
- A legacy bridge to the TeamHoly web inventory and webshop

## Architecture

| Area | Responsibility |
| --- | --- |
| `Lobby` | Bukkit lifecycle, component wiring, listener registration, recurring tasks, and seasonal skin data |
| `lobbyplayer` | Per-player state, scoreboards, menus, settings, friends, and server routing |
| `listeners` | Bukkit, Holy Core, NPC, and CloudNet event handling |
| `bedwars` | Bedwars/Rush server selection and spectating inventories |
| `handlers` | Cloud service cache, holograms, and statistics resets |
| `leaderboard` | Redis/Redisson-backed leaderboard cache and inventory UI |
| `webshop` | Legacy HTTP integration for in-game purchases |

The plugin depends heavily on APIs, data models, cache services, locations, and messaging conventions supplied by Holy Core. Running it outside the original TeamHoly infrastructure requires adaptation.

## Technology and dependencies

- Java toolchain 21, producing Java 17 bytecode
- Gradle 9.5 via the included wrapper
- Spigot API and CraftBukkit/NMS `1.8.8-R0.1` / `v1_8_R3`
- Holy Core `2.9.0` (`holy-core-api`, `bukkit-core-api`, and `bukkit-markupapi`)
- CloudNet Bridge and Wrapper `3.4.5-RELEASE`
- Holographic Displays API `2.4.9`
- Redisson `3.19.1`, MongoDB Java Driver `3.12.8`, LuckPerms API `5.4`, and Gson `2.2.4`
- Lombok `1.18.36`

Most dependencies are declared as `compileOnly` and must therefore be supplied by the server or by Holy Core at runtime.

## Building

### Prerequisites

1. Install JDK 21.
2. Provide GitHub Packages credentials that can read the archived Holy Core packages. The build accepts either the Gradle properties `gpr.user` and `gpr.key` or the environment variables `GITHUB_ACTOR` and `GITHUB_TOKEN`.
3. Install Holographic Displays API `2.4.9` in your local Maven repository under this coordinate:

   ```text
   com.gmail.filoghost.holographicdisplays:holographicdisplays-api:2.4.9
   ```

The Holographic Displays artifact is intentionally resolved from `mavenLocal()` because the historical build does not define a working remote source for it.

### Build command

On Linux or macOS:

```bash
export GITHUB_ACTOR="your-github-user"
export GITHUB_TOKEN="a-token-with-package-read-access"
./gradlew clean shadowJar
```

On Windows PowerShell:

```powershell
$env:GITHUB_ACTOR = "your-github-user"
$env:GITHUB_TOKEN = "a-token-with-package-read-access"
.\gradlew.bat clean shadowJar
```

The shaded plugin JAR is written to `build/libs/`.

An unauthenticated clean build currently stops during dependency resolution because Gradle cannot access the three Holy Core `2.9.0` artifacts without a GitHub Packages username and token. The repository contains no automated tests or CI workflow.

## Historical deployment contract

The plugin descriptor registers `de.teamholy.lobby.Lobby` as the entry point and declares these runtime plugins as hard dependencies:

- `bukkit-core`
- `CloudNet-Bridge`
- `HolographicDisplays`

It also registers the `/spawn` command and the administrative `/test1` diagnostics command.

The original installation expected Holy Core location entries including:

```text
lobby
bedwars
bw_spawn
bw_spawn_bw
bw_spawn_rbw
bw_spawn_spec
mlgrush
clutches
bridge
eloholo
xmas_npc
namemc_npc
webinventory_npc
wm2026_npc
```

It also relies on original CloudNet group names and service metadata. These include `Lobby`, `PremiumLobby`, `MLGRush`, `Clutches`, `Bridge`, `wm2026`, and the `BW`/`RBW` variants `2x1`, `4x2`, and `8x1`.

## Known limitations

- The code uses CraftBukkit/NMS internals tied specifically to Minecraft 1.8.8 and is not compatible with modern server versions without a port.
- Infrastructure URLs, service groups, permissions, inventory contents, NPCs, seasonal content, and location keys are hard-coded.
- Several integrations assume the original TeamHoly databases, Redis data, CloudNet messages, and website endpoints.
- There are no tests, example server configuration, migration tooling, or reproducible local integration environment.
- Some asynchronous callbacks access Bukkit objects, while other paths perform potentially blocking data or cloud operations. Reuse requires a thread-safety and performance review.
- Several event and parsing paths suppress exceptions or assume that external metadata is complete, which can make runtime failures difficult to diagnose.
- The Gradle publishing target still points to the historical `holy-games` GitHub Packages repository.

## Security notice

This is historical code and has not been security-hardened for reuse. The webshop integration contains a credential-like request value and performs a fire-and-forget API request after deducting coins. Treat every embedded value as compromised, rotate or revoke it, remove the legacy integration, and audit the complete Git history before deploying or mirroring the repository. A normal file deletion does not remove a value from Git history.

Do not use the archived code against live TeamHoly services or production data.

## Project status and contributions

The project is archived. Issues, pull requests, feature requests, and support requests are not actively handled. Fork the repository if you want to study, modernize, or adapt the code.

## License

No top-level license file was present when this archive README was added, and at least one source file contains a historical proprietary notice. Public source availability alone does not grant open-source usage rights. The repository owners must add the intended OSI-approved license and reconcile conflicting file headers before describing the project as open source.
