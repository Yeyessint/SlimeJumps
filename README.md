# SlimeJumps

[![Build](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml/badge.svg)](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)

**English** | [Español](README.es.md)

SlimeJumps adds configurable **jump pads** to your Minecraft server — the launch pads you find in server lobbies and hubs that fling players into the air when they step on them.

## Features

- ⚡ **Named jump pads** on any block, created with a single command.
- ✈️ **Flight routes** — link a pad to a route and players who step on it fly along your waypoints to a destination, with a particle trail and arrival effects (perfect for sending players from the lobby to a game area).
- 🎯 **Per-pad launch power** — horizontal and vertical strength configurable for every pad.
- 🧭 **Fixed or free direction** — pads can launch players where they look, or always in a fixed direction.
- 🎨 **Per-pad sound, particle and cooldown** overrides, on top of the global defaults.
- 🚀 **Double jump** (optional): lobby-style mid-air boost when pressing the jump key, compatible with `/fly` from other plugins.
- ⌨️ **Per-pad console commands** — run any command when a pad is used (`%player%` placeholder), e.g. send players to another server or give rewards.
- 📊 **Launch statistics** — `/sj stats` shows total launches and the most used pads.
- 🖱️ **Pads GUI** — `/sj gui` opens a paginated menu of all pads; click one to teleport to it.
- 🏷️ **Holograms** — floating text above any pad (`/sj sethologram`), no external hologram plugin needed.
- 🪄 **Editor wand** — `/sj wand` gives a stick: left click a block to create a pad, right click a pad to remove it.
- 🎛️ **Pad presets** — `/sj create <name> --preset parkour|cannon|bounce` (define your own in the config).
- 🌬️ **Flight whoosh** — a continuous wind sound while flying routes.
- 🧪 **Potion effects per pad** — apply speed, slow falling, levitation… when a pad is used.
- 💬 **Action bar messages per pad**, with color codes and `%player%` placeholder.
- 🔀 **Toggle and rename pads** without recreating them.
- 🌐 **Disabled worlds** list to keep pads inert in build or creative worlds.
- 📍 **`/sj near`** — find the pads around you while building.
- 🔔 **Update checker** — get notified when a new release is published on GitHub (can be disabled).
- ✨ **Ambient particles** above each pad so players can spot them (fully configurable).
- 🛡️ **Fall damage protection** after launches and route landings, plus full damage immunity while flying a route.
- ⏱️ **Anti-spam cooldown** so pads can't be abused.
- 🤫 **Sneak to bypass** (optional): sneaking players walk over pads without being launched — great for staff while building.
- 🟩 **Slime block mode** (optional): make *every* slime block in the world act as a pad, like the classic 1.x behaviour.
- 🌍 **Multi-language messages** — English and Spanish included; add your own language file.
- 🔁 **Automatic migration** of pads created with SlimeJumps 1.x.
- ⌨️ **Tab completion** for every command.

If you find SlimeJumps useful, please consider giving the repository a ⭐ — it really helps the project!

## Requirements

- A Spigot or Paper server running **Minecraft 1.21.x**.
- Java 21.

## Installation

1. Download the latest `SlimeJumps-x.x.x.jar` from the [releases page](https://github.com/Yeyessint/SlimeJumps/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.
4. Stand on a block and run `/sj create <name>` — done!

## Commands

The main command is `/slimejumps` (aliases: `/sj`, `/jumppads`).

| Command | Description |
|---|---|
| `/sj create <name> [power] [vertical]` | Create a pad on the block you are standing on |
| `/sj create <name> --preset <preset>` | Create a pad from a preset (`parkour`, `cannon`, `bounce`, or your own) |
| `/sj wand` | Get the editor wand — left click creates a pad, right click removes one |
| `/sj sethologram <pad> <text...\|none>` | Floating hologram above the pad (`&` colors, `\|` for new lines) |
| `/sj remove <name>` | Delete a pad |
| `/sj list` | List all pads with their coordinates |
| `/sj info <name>` | Show the details of a pad |
| `/sj tp <name>` | Teleport to a pad |
| `/sj gui` | Paginated menu of all pads — click to teleport |
| `/sj near [radius]` | List pads close to you (default radius: 20 blocks) |
| `/sj stats` | Launch statistics and the most used pads |
| `/sj toggle <pad>` | Enable or disable a pad without deleting it |
| `/sj rename <pad> <newname>` | Rename a pad keeping all its settings |
| `/sj seteffect <pad> <effect\|none> [seconds] [level]` | Potion effect applied on use |
| `/sj setmessage <pad> <text...\|none>` | Action bar message shown on use (`%player%`, `&` colors) |
| `/sj setpower <name> <value>` | Change a pad's horizontal strength |
| `/sj setvertical <name> <value>` | Change a pad's vertical strength |
| `/sj setcooldown <pad> <ms\|default>` | Per-pad cooldown override |
| `/sj setcommand <pad> <command...\|none>` | Console command run when the pad is used (`%player%` placeholder) |
| `/sj setroute <pad> <route\|none>` | Make a pad fly players along a route (or turn it back into a normal pad) |
| `/sj setdirection <pad> <look\|here>` | Launch players where they look, or always towards where you are facing |
| `/sj setsound <pad> <sound\|default>` | Per-pad launch sound |
| `/sj setparticle <pad> <particle\|default>` | Per-pad launch particle |
| `/sj route create <name>` | Create a flight route with its first waypoint at your position |
| `/sj route addpoint <name>` | Append your current position to a route |
| `/sj route delpoint <name> <number>` | Remove a waypoint from a route |
| `/sj route remove <name>` | Delete a route (pads using it become normal pads) |
| `/sj route list` | List all routes |
| `/sj route info <name>` | Show a route's waypoints |
| `/sj reload` | Reload configuration, messages, pads and routes |
| `/sj help` | Show the help screen |

### Setting up a flight route

1. Create the route where the flight should start: `/sj route create togames`
2. Fly along the path you want players to follow and run `/sj route addpoint togames` at each turn. The last point you add is the landing spot.
3. Link a pad to it: `/sj setroute mypad togames`

Players stepping on `mypad` now fly through your waypoints, leaving a particle trail, and land safely at the destination.

## Permissions

| Permission | Description | Default |
|---|---|---|
| `slimejumps.admin` | Access to all commands | OP |
| `slimejumps.use` | Being launched by jump pads | Everyone |
| `slimejumps.doublejump` | Double jumping (when the feature is enabled) | Everyone |

## Configuration

`config.yml` lets you tune every aspect of the plugin:

```yaml
language: en            # en (English) or es (Spanish)
update-checker: true    # Notify admins when a new release is out

stats:
  enabled: true         # Track pad usage for /sj stats

disabled-worlds: []     # Worlds where pads and routes are inert
metrics: true           # Anonymous bStats usage metrics

holograms:
  height: 1.6           # Height of hologram texts above pads

presets:                # Used by /sj create <name> --preset <preset>
  parkour: {power: 1.2, vertical: 0.8, sound: entity.rabbit.jump, particle: CRIT, cooldown-ms: 0}
  cannon: {power: 4.0, vertical: 1.6, sound: entity.generic.explode, particle: EXPLOSION}
  bounce: {power: 0.0, vertical: 1.4, sound: block.slime_block.hit, particle: ITEM_SLIME}

pads:
  default-power: 1.6    # Default horizontal strength for new pads
  default-vertical: 1.0 # Default vertical strength for new pads
  max-power: 10.0       # Maximum value accepted in commands
  cooldown-ms: 500      # Anti-spam cooldown per player
  prevent-fall-damage: true
  fall-protection-ms: 10000
  ignore-sneaking: false # Sneaking players are not launched

launch:
  sound:
    enabled: true
    name: entity.ender_dragon.flap
    volume: 1.0
    pitch: 1.0
  particles:
    enabled: true
    name: CLOUD
    count: 20

ambient-particles:
  enabled: true
  name: HAPPY_VILLAGER
  count: 8
  interval-ticks: 10

routes:
  speed: 1.2            # Flight speed in blocks per tick
  timeout-seconds: 30
  protect-during-flight: true
  landing-protection-ms: 5000
  trail:
    enabled: true
    name: END_ROD
    count: 3
  flight-sound:         # Whoosh played while flying
    enabled: true
    name: entity.phantom.flap
    volume: 0.7
    pitch: 1.0
    interval-ticks: 8
  arrival:
    sound:
      enabled: true
      name: entity.player.levelup
      volume: 1.0
      pitch: 1.2
    particles:
      enabled: true
      name: FIREWORK
      count: 30

double-jump:
  enabled: false        # Lobby-style double jump (slimejumps.doublejump)
  power: 0.9
  vertical: 0.9
  cooldown-ms: 500
  prevent-fall-damage: true
  sound:
    enabled: true
    name: entity.bat.takeoff
    volume: 1.0
    pitch: 1.0
  particles:
    enabled: true
    name: CLOUD
    count: 15

slime-block-mode:
  enabled: false        # Every slime block becomes a pad (legacy mode)
  power: 1.6
  vertical: 1.0
```

Registered pads are stored in `plugins/SlimeJumps/pads.yml` and routes in `plugins/SlimeJumps/routes.yml`.

## Building from source

```bash
git clone https://github.com/Yeyessint/SlimeJumps.git
cd SlimeJumps
mvn package
```

The plugin jar is generated in `target/SlimeJumps-<version>.jar`.

## Migrating from 1.x

Pads created with SlimeJumps 1.x (stored in the old `config.yml` under `locs`) are imported automatically the first time 2.x starts, and named `pad1`, `pad2`, …

## Contributing

Issues and pull requests are welcome! Please use the [issue templates](.github/ISSUE_TEMPLATE) when reporting bugs or requesting features.

## License

This project is licensed under the [MIT License](LICENSE).
