# GrimCompanion

A companion anti-cheat plugin for Paper 1.21, using PacketEvents to detect common
cheating patterns that other anti-cheats (like GrimAC) may not fully cover or that
need server-specific tuning. Runs standalone or alongside GrimAC.

## Requirements

- Paper/Purpur **1.21.11** (or whichever 1.21.x build your server runs - see the note below)
- Java **21** (Paper from 1.20.5 onward and PacketEvents 2.10+ both require Java 21, Java 17 is NO LONGER supported)
- [PacketEvents](https://modrinth.com/plugin/packetevents) **2.13.0** (required, place it in `plugins/`)
- GrimAC (optional, soft-depend - not required)

> **Important version note**: PacketEvents 2.5.0 (the project's original version) only
> supports up to Minecraft 1.21.1, and does NOT run correctly on 1.21.11. `build.gradle`
> and the installation instructions below have been updated to use PacketEvents 2.13.0 +
> Paper API 1.21.11. If your server runs a different 1.21.x build, update the `paper-api`
> line in `build.gradle` to match your exact minor version, and check
> https://modrinth.com/plugin/packetevents to pick a PacketEvents build that supports
> your server's version.

## Installation

1. Download `PacketEvents-Spigot-2.13.0.jar` (or a newer build matching your server version) into the `plugins/` folder.
2. Build the plugin with Gradle: `./gradlew build` (the jar will be in `build/libs/`).
3. Place the `GrimCompanion-1.0.0.jar` file into `plugins/`.
4. Restart the server. The `config.yml` file will be auto-generated in `plugins/GrimCompanion/`.

## Check List

### Combat
| Check | Description |
|---|---|
| CrystalAura | Placing/breaking End Crystals too fast (< 100ms) |
| AnchorAura | Placing/breaking Respawn Anchors too fast |
| AutoClicker | CPS too high or clicks too evenly-spaced (low stddev) |
| KillAura | Snap aim (sudden camera rotation) when attacking |
| Reach | Attacking from a distance > 3.2 blocks |

### Movement
| Check | Description |
|---|---|
| Flight | Staying airborne for an abnormally long time without a valid reason |
| Speed | Moving faster than the allowed speed |
| NoSlowdown | Not slowing down while blocking/eating/near an exploding bed |

### Exploit
| Check | Description |
|---|---|
| ItemMacro | Macro item usage (ClickPearl, MiddleClickExtra) |
| AutoFirework | Automatically using fireworks while gliding with an elytra |
| ElytraTarget | Automatically tracking a target while gliding with an elytra |
| AutoCart | Automatic TNT Cart |
| CrystalOptimizer | Detects Marlow's Crystal Optimizer |
| ClientBrand | No brand sent, or brand matches the blocked list |
| Ping | High ping or abnormal keep-alive behavior |

### World
| Check | Description |
|---|---|
| Scaffold | Placing blocks too fast (auto-bridge) |

All thresholds (delays, CPS, angles, violation counts...) are configurable in `config.yml`.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/gc reload` | `grimcompanion.admin` | Reload the config |
| `/gc check <player>` | `grimcompanion.admin` | View player info (ping, brand...) |
| `/gc alerts` | `grimcompanion.staff` | Toggle real-time violation alerts |
| `/gc stats <player>` | `grimcompanion.admin` | View violation counts per check |
| `/gc reset <player>` | `grimcompanion.admin` | Reset all violation levels (VL) for a player |

## Permissions

- `grimcompanion.*` - all permissions
- `grimcompanion.admin` - admin (reload, check, stats, reset)
- `grimcompanion.staff` - receive real-time violation alerts
- `grimcompanion.bypass` - bypass all checks (for testing/staff)

## PredictionEngine - GrimCompanion's own movement engine

GrimCompanion ships with its **own movement physics prediction engine**
(`com.grimcompanion.engine.PredictionEngine`), independent from GrimAC:

- On every position packet, the engine calculates the "expected Y velocity" (based on
  gravity/jumping) and the "maximum allowed horizontal speed" (based on sprint/sneak/potions),
  then compares them against the actual data reported by the client.
- Uses a **trust buffer** for each axis: a 1-2 tick deviation from lag/jitter won't be
  flagged immediately - only when the deviation persists across several consecutive ticks
  (default 6, configurable via `engine.max-trust-buffer`) is it actually treated as a
  violation, greatly reducing false positives. The buffer regenerates gradually as the
  player moves legitimately again.
- Automatically **skips** states that are too complex to simulate simply (creative mode,
  elytra, swimming, climbing, in a vehicle, in water/lava...) instead of guessing wrong
  and causing false flags.
- `FlightCheck` and `SpeedCheck` share a SINGLE simulation per tick (computed by
  `CheckManager` and cached in `PlayerData`), avoiding calling the engine twice and
  corrupting its state.
- All tolerance/buffer thresholds are configurable in `config.yml` (under the `engine:` section).

This engine runs **completely independently** and doesn't need GrimAC to function fully.
If the server has GrimAC installed, GrimCompanion still runs alongside it normally with
no conflicts or deference of any kind.

## GrimAC Integration (optional - for cross-checking only)

If the server has GrimAC installed, GrimCompanion will automatically try to "hook" into
GrimAC on startup (`GrimIntegration.java`) to **cross-check** results between the two
systems - it is NO LONGER used to defer to GrimAC's engine as before:

- **`integration.relay-grimac-violations: true`** (default) - relays violations from GrimAC
  into GrimCompanion's shared alert/statistics system (`/gc stats`, `/gc check`), displayed
  with a `Grim:` prefix to distinguish them from violations detected by GrimCompanion's own
  PredictionEngine.
- `/gc check <player>` displays both sources: the trust buffer from GrimCompanion's own
  engine AND (if hooking succeeds) GrimAC's VL for the Speed check, making it convenient
  to compare the two systems.

**Important note on API compatibility**: GrimAC does not guarantee 100% API stability
across versions. The `GrimIntegration.java` file is written following the common structure
of GrimAC's official `api` module (`ac.grim.grimac.api.GrimAPI`), but you **need to verify**
it against the exact GrimAC version running on your server (class/method names may differ).
All integration logic is wrapped in `try-catch(Throwable)`, so if the API doesn't match,
GrimCompanion will automatically fall back to standalone mode (no crash, only the
cross-checking feature is lost - GrimCompanion's own PredictionEngine is completely unaffected).

## Important Notes

This is a **companion (supplementary) plugin**, not a complete anti-cheat meant to replace
GrimAC. The movement checks (Flight, Speed, NoSlowdown) use a simplified heuristic (not a
full prediction engine like GrimAC's), so thresholds may need tuning in `config.yml` based
on your server's specifics to avoid false positives. It's recommended to test thoroughly on
a staging server before using in production, especially for checks tied to auto-kick/ban.

## Data Directory Structure

```
plugins/GrimCompanion/
├── config.yml
└── logs/
    └── violations.log
```
