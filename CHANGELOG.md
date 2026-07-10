# Changelog

All notable changes to SlimeJumps are documented in this file.

## [2.4.0] - 2026-07-10

### Added
- **Holograms**: `/sj sethologram <pad> <text|none>` shows floating text above a pad using native 1.21 text displays — no external hologram plugin required. Supports `&` colors and `|` for multiple lines; height configurable via `holograms.height`.
- **Editor wand**: `/sj wand` gives a tagged stick — left click a block to create a pad (auto-named), right click a pad block to remove it.
- **Pad presets**: `/sj create <name> --preset <preset>` with bundled `parkour`, `cannon` and `bounce` presets; define your own (power, vertical, sound, particle, cooldown, message, effect) in the `presets` config section.
- **Flight whoosh**: a continuous, configurable wind sound while flying routes (`routes.flight-sound`).
- **bStats metrics** scaffolding with a `metrics` config toggle (activates once a bStats service id is registered).

## [2.3.0] - 2026-07-10

### Added
- **Pads GUI**: `/sj gui` opens a paginated chest menu listing every pad with its details; clicking a pad teleports you to it.
- **Toggle pads**: `/sj toggle <pad>` enables/disables a pad without deleting it (disabled pads show as barriers in the GUI and stop emitting particles).
- **Rename pads**: `/sj rename <pad> <newname>` keeps every setting and transfers the pad's statistics.
- **Potion effects per pad**: `/sj seteffect <pad> <effect|none> [seconds] [level]` applies an effect when the pad is used.
- **Action bar messages per pad**: `/sj setmessage <pad> <text...|none>` with `&` color codes and `%player%` placeholder.
- **Disabled worlds**: pads, slime block mode and routes are inert in worlds listed under `disabled-worlds`.
- `/sj info` now also shows status, effect and message.

## [2.2.0] - 2026-07-10

### Added
- **Double jump** (optional): lobby-style mid-air boost when pressing the jump key, with its own power, cooldown, sound and particles. Disabled in creative/spectator and compatible with `/fly` from other plugins (permission: `slimejumps.doublejump`).
- **Per-pad console commands**: `/sj setcommand <pad> <command...|none>` runs a command as the console whenever the pad is used, with a `%player%` placeholder.
- **Per-pad cooldown**: `/sj setcooldown <pad> <ms|default>` overrides the global cooldown.
- **Launch statistics**: `/sj stats` shows the server-wide launch total and the top 10 most used pads, persisted in `stats.yml` (toggle with `stats.enabled`).
- **`/sj near [radius]`**: lists the pads around you sorted by distance — handy while building.
- **Update checker**: checks GitHub releases on startup and notifies the console and joining admins when a new version is available (toggle with `update-checker`).
- `/sj info` now also shows the pad's cooldown and command.

## [2.1.0] - 2026-07-10

### Added
- **Flight routes**: link a pad to a route (`/sj setroute`) and players who step on it fly along its waypoints to a destination, with a configurable speed, particle trail, arrival sound/particles, damage immunity during the flight and a fall damage grace period on landing.
- Route management commands: `/sj route create|addpoint|delpoint|remove|list|info`.
- **Fixed launch direction** per pad: `/sj setdirection <pad> <look|here>`.
- **Per-pad sound and particle** overrides: `/sj setsound` and `/sj setparticle`.
- Optional **sneak bypass** (`pads.ignore-sneaking`): sneaking players are not launched.
- Extended `/sj info` output showing route, direction and effect overrides.
- Tab completion for all new subcommands.

## [2.0.0] - 2026-07-10

### Changed
- Complete rewrite for **Minecraft 1.21.x** (Java 21, Spigot API 1.21.4).
- Replaced NMS reflection particles with the native particle API.
- Pads are now named, stored in `pads.yml`, and support per-pad horizontal/vertical launch power.
- New `/slimejumps` command (aliases `/sj`, `/jumppads`) with create, remove, list, info, tp, setpower, setvertical, reload and help, plus tab completion.

### Added
- Per-player launch cooldown.
- Proper fall damage protection after launches.
- Configurable launch sound, launch particles and ambient pad particles.
- Optional legacy slime block mode.
- Multi-language messages (English and Spanish).
- Automatic migration of pads created with 1.x.

## [1.0] - 2016

- Original release: every slime block launches players; NMS reflection particles; `/config setpad` command.
