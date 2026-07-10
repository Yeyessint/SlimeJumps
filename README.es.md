# SlimeJumps

[![Build](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml/badge.svg)](https://github.com/Yeyessint/SlimeJumps/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)

[English](README.md) | **Español**

SlimeJumps añade **jump pads** configurables a tu servidor de Minecraft: las plataformas de salto que se colocan en los lobbies y hubs de los servidores para lanzar a los jugadores por los aires al pisarlas.

## Características

- ⚡ **Jump pads con nombre** sobre cualquier bloque, creados con un solo comando.
- ✈️ **Rutas de vuelo** — vincula un pad a una ruta y los jugadores que lo pisen saldrán volando por tus waypoints hasta un destino, con estela de partículas y efectos de llegada (perfecto para llevar jugadores del lobby a una zona de juego).
- 🎯 **Potencia por pad** — fuerza horizontal y vertical configurable para cada pad.
- 🧭 **Dirección fija o libre** — los pads pueden lanzar hacia donde mira el jugador, o siempre en una dirección fija.
- 🎨 **Sonido, partícula y cooldown por pad**, además de los valores globales por defecto.
- 🚀 **Doble salto** (opcional): impulso en el aire al pulsar la tecla de salto, estilo lobby, compatible con el `/fly` de otros plugins.
- ⌨️ **Comandos de consola por pad** — ejecuta cualquier comando al usar un pad (placeholder `%player%`), p. ej. enviar jugadores a otro servidor o dar recompensas.
- 📊 **Estadísticas de lanzamientos** — `/sj stats` muestra el total de lanzamientos y los pads más usados.
- 📍 **`/sj near`** — encuentra los pads a tu alrededor mientras construyes.
- 🔔 **Aviso de actualizaciones** — notifica cuando se publica una nueva versión en GitHub (desactivable).
- ✨ **Partículas ambientales** sobre cada pad para que los jugadores los vean (totalmente configurables).
- 🛡️ **Protección contra daño de caída** tras lanzamientos y aterrizajes de ruta, e inmunidad total mientras se vuela una ruta.
- ⏱️ **Cooldown anti-spam** para que los pads no se puedan abusar.
- 🤫 **Bypass agachado** (opcional): los jugadores agachados pasan sobre los pads sin ser lanzados — ideal para el staff mientras construye.
- 🟩 **Modo bloque de slime** (opcional): haz que *todos* los bloques de slime del mundo actúen como pad, como en la versión 1.x clásica.
- 🌍 **Mensajes multi-idioma** — inglés y español incluidos; puedes añadir tu propio idioma.
- 🔁 **Migración automática** de los pads creados con SlimeJumps 1.x.
- ⌨️ **Autocompletado (tab)** en todos los comandos.

Si SlimeJumps te resulta útil, ¡considera darle una ⭐ al repositorio — ayuda mucho al proyecto!

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
| `/sj near [radio]` | Lista los pads cercanos a ti (radio por defecto: 20 bloques) |
| `/sj stats` | Estadísticas de lanzamientos y pads más usados |
| `/sj setpower <nombre> <valor>` | Cambia la fuerza horizontal de un pad |
| `/sj setvertical <nombre> <valor>` | Cambia la fuerza vertical de un pad |
| `/sj setcooldown <pad> <ms\|default>` | Cooldown propio del pad |
| `/sj setcommand <pad> <comando...\|none>` | Comando de consola al usar el pad (placeholder `%player%`) |
| `/sj setroute <pad> <ruta\|none>` | Haz que un pad lleve volando por una ruta (o vuelva a ser un pad normal) |
| `/sj setdirection <pad> <look\|here>` | Lanzar hacia donde mira el jugador, o siempre hacia donde tú miras |
| `/sj setsound <pad> <sonido\|default>` | Sonido de lanzamiento por pad |
| `/sj setparticle <pad> <partícula\|default>` | Partícula de lanzamiento por pad |
| `/sj route create <nombre>` | Crea una ruta de vuelo con su primer punto en tu posición |
| `/sj route addpoint <nombre>` | Añade tu posición actual a una ruta |
| `/sj route delpoint <nombre> <número>` | Elimina un punto de una ruta |
| `/sj route remove <nombre>` | Elimina una ruta (los pads que la usaban vuelven a ser normales) |
| `/sj route list` | Lista todas las rutas |
| `/sj route info <nombre>` | Muestra los puntos de una ruta |
| `/sj reload` | Recarga configuración, mensajes, pads y rutas |
| `/sj help` | Muestra la ayuda |

### Cómo montar una ruta de vuelo

1. Crea la ruta donde debe empezar el vuelo: `/sj route create ajuegos`
2. Vuela por el camino que quieres que sigan los jugadores y ejecuta `/sj route addpoint ajuegos` en cada giro. El último punto que añadas es el lugar de aterrizaje.
3. Vincula un pad: `/sj setroute mipad ajuegos`

Los jugadores que pisen `mipad` volarán por tus waypoints, dejando una estela de partículas, y aterrizarán sanos y salvos en el destino.

## Permisos

| Permiso | Descripción | Por defecto |
|---|---|---|
| `slimejumps.admin` | Acceso a todos los comandos | OP |
| `slimejumps.use` | Ser lanzado por los jump pads | Todos |
| `slimejumps.doublejump` | Doble salto (cuando la función está activada) | Todos |

## Configuración

`config.yml` permite ajustar todos los aspectos del plugin:

```yaml
language: es            # en (inglés) o es (español)
update-checker: true    # Avisa a los admins cuando hay nueva versión

stats:
  enabled: true         # Contabiliza el uso de pads para /sj stats

pads:
  default-power: 1.6    # Fuerza horizontal por defecto de los pads nuevos
  default-vertical: 1.0 # Fuerza vertical por defecto de los pads nuevos
  max-power: 10.0       # Valor máximo aceptado en los comandos
  cooldown-ms: 500      # Cooldown anti-spam por jugador
  prevent-fall-damage: true
  fall-protection-ms: 10000
  ignore-sneaking: false # Los jugadores agachados no son lanzados

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
  speed: 1.2            # Velocidad de vuelo en bloques por tick
  timeout-seconds: 30
  protect-during-flight: true
  landing-protection-ms: 5000
  trail:
    enabled: true
    name: END_ROD
    count: 3
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
  enabled: false        # Doble salto estilo lobby (slimejumps.doublejump)
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
  enabled: false        # Todos los bloques de slime se convierten en pads (modo clásico)
  power: 1.6
  vertical: 1.0
```

Los pads registrados se guardan en `plugins/SlimeJumps/pads.yml` y las rutas en `plugins/SlimeJumps/routes.yml`.

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
