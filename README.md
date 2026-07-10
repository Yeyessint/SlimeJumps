# SlimeJumps

[![Build](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml/badge.svg)](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)

**English** | [Español](README.es.md)

SlimeJumps adds configurable **jump pads** to your Minecraft server — the launch pads you find in server lobbies and hubs that fling players into the air when they step on them.

## Features

- ⚡ **Named jump pads** on any block, created with a single command.
- 🎯 **Per-pad launch power** — horizontal and vertical strength configurable for every pad.
- ✨ **Ambient particles** above each pad so players can spot them (fully configurable).
- 🔊 **Launch sound and particle burst**, both configurable.
- 🛡️ **Fall damage protection** after being launched.
- ⏱️ **Anti-spam cooldown** so pads can't be abused.
- 🟩 **Slime block mode** (optional): make *every* slime block in the world act as a pad, like the classic 1.x behaviour.
- 🌍 **Multi-language messages** — English and Spanish included; add your own language file.
- 🔁 **Automatic migration** of pads created with SlimeJumps 1.x.
- 🧭 **Tab completion** for every command.

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
| `/sj remove <name>` | Delete a pad |
| `/sj list` | List all pads with their coordinates |
| `/sj info <name>` | Show the details of a pad |
| `/sj tp <name>` | Teleport to a pad |
| `/sj setpower <name> <value>` | Change a pad's horizontal strength |
| `/sj setvertical <name> <value>` | Change a pad's vertical strength |
| `/sj reload` | Reload configuration, messages and pads |
| `/sj help` | Show the help screen |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `slimejumps.admin` | Access to all commands | OP |
| `slimejumps.use` | Being launched by jump pads | Everyone |

## Configuration

`config.yml` lets you tune every aspect of the plugin:

```yaml
language: en            # en (English) or es (Spanish)

pads:
  default-power: 1.6    # Default horizontal strength for new pads
  default-vertical: 1.0 # Default vertical strength for new pads
  max-power: 10.0       # Maximum value accepted in commands
  cooldown-ms: 500      # Anti-spam cooldown per player
  prevent-fall-damage: true
  fall-protection-ms: 10000

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

slime-block-mode:
  enabled: false        # Every slime block becomes a pad (legacy mode)
  power: 1.6
  vertical: 1.0
```

Registered pads are stored in `plugins/SlimeJumps/pads.yml`.

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
