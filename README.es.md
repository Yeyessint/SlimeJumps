# SlimeJumps

[![Build](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml/badge.svg)](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)

[English](README.md) | **Español**

SlimeJumps añade **jump pads** configurables a tu servidor de Minecraft: las plataformas de salto que se colocan en los lobbies y hubs de los servidores para lanzar a los jugadores por los aires al pisarlas.

## Características

- ⚡ **Jump pads con nombre** sobre cualquier bloque, creados con un solo comando.
- 🎯 **Potencia por pad** — fuerza horizontal y vertical configurable para cada pad.
- ✨ **Partículas ambientales** sobre cada pad para que los jugadores los vean (totalmente configurables).
- 🔊 **Sonido y explosión de partículas al lanzar**, ambos configurables.
- 🛡️ **Protección contra daño de caída** tras ser lanzado.
- ⏱️ **Cooldown anti-spam** para que los pads no se puedan abusar.
- 🟩 **Modo bloque de slime** (opcional): haz que *todos* los bloques de slime del mundo actúen como pad, como en la versión 1.x clásica.
- 🌍 **Mensajes multi-idioma** — inglés y español incluidos; puedes añadir tu propio idioma.
- 🔁 **Migración automática** de los pads creados con SlimeJumps 1.x.
- 🧭 **Autocompletado (tab)** en todos los comandos.

## Requisitos

- Un servidor Spigot o Paper con **Minecraft 1.21.x**.
- Java 21.

## Instalación

1. Descarga el último `SlimeJumps-x.x.x.jar` desde la [página de releases](https://github.com/Yeyessint/SlimeJumps/releases).
2. Colócalo en la carpeta `plugins/` de tu servidor.
3. Reinicia el servidor.
4. Ponte sobre un bloque y ejecuta `/sj create <nombre>` — ¡listo!

## Comandos

El comando principal es `/slimejumps` (alias: `/sj`, `/jumppads`).

| Comando | Descripción |
|---|---|
| `/sj create <nombre> [potencia] [vertical]` | Crea un pad en el bloque donde estás |
| `/sj remove <nombre>` | Elimina un pad |
| `/sj list` | Lista todos los pads con sus coordenadas |
| `/sj info <nombre>` | Muestra los detalles de un pad |
| `/sj tp <nombre>` | Teletranspórtate a un pad |
| `/sj setpower <nombre> <valor>` | Cambia la fuerza horizontal de un pad |
| `/sj setvertical <nombre> <valor>` | Cambia la fuerza vertical de un pad |
| `/sj reload` | Recarga configuración, mensajes y pads |
| `/sj help` | Muestra la ayuda |

## Permisos

| Permiso | Descripción | Por defecto |
|---|---|---|
| `slimejumps.admin` | Acceso a todos los comandos | OP |
| `slimejumps.use` | Ser lanzado por los jump pads | Todos |

## Configuración

`config.yml` permite ajustar todos los aspectos del plugin:

```yaml
language: es            # en (inglés) o es (español)

pads:
  default-power: 1.6    # Fuerza horizontal por defecto de los pads nuevos
  default-vertical: 1.0 # Fuerza vertical por defecto de los pads nuevos
  max-power: 10.0       # Valor máximo aceptado en los comandos
  cooldown-ms: 500      # Cooldown anti-spam por jugador
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
  enabled: false        # Todos los bloques de slime se convierten en pads (modo clásico)
  power: 1.6
  vertical: 1.0
```

Los pads registrados se guardan en `plugins/SlimeJumps/pads.yml`.

## Compilar desde el código fuente

```bash
git clone https://github.com/Yeyessint/SlimeJumps.git
cd SlimeJumps
mvn package
```

El jar del plugin se genera en `target/SlimeJumps-<versión>.jar`.

## Migración desde 1.x

Los pads creados con SlimeJumps 1.x (guardados en el antiguo `config.yml` bajo `locs`) se importan automáticamente la primera vez que arranca la 2.x, con los nombres `pad1`, `pad2`, …

## Contribuir

¡Los issues y pull requests son bienvenidos! Usa las [plantillas de issues](.github/ISSUE_TEMPLATE) para reportar errores o pedir funcionalidades.

## Licencia

Este proyecto está licenciado bajo la [Licencia MIT](LICENSE).
