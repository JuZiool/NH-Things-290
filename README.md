# NH-Things-290

Utility items and an Applied Energistics 2 addon for **GT New Horizons 2.9** on Minecraft 1.7.10.

Current development release: **v0.3.1-dev**.

## Current target

The initial development target is the local **GTNH 2.9.0-beta-3** instance:

- Applied Energistics 2 `rv3-beta-1050-GTNH`
- AE2 Fluid Crafting `1.5.106-gtnh`
- GregTech 5 Unofficial `5.09.54.133`

## Implemented

- Unrestricted item storage cells: 1k through 16384k
- Unrestricted fluid storage cells: 1k through 16384k
- Shared unrestricted cell housing and crafting recipes
- Item and fluid partition filters, upgrades, drive/chest status and safe empty-cell disassembly
- UUID-backed world storage for cell contents
- Flight charm with hunger-based creative flight
- LV/MV/HV/EV/IV wireless charging stations and a source-selection marker card

## Development

Requirements:

- JDK 25 for Gradle
- Java 8-compatible mod bytecode (handled by the build)

Build:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-25'
.\gradlew.bat build
```

The `参考项目` directory contains local reference material and is intentionally excluded from Git.

## Wireless charging stations

| Tier | Sphere radius | Shared output budget | Player charging |
| --- | ---: | ---: | --- |
| LV | 16 blocks | 512 EU/t | No |
| MV | 32 blocks | 2048 EU/t | No |
| HV | 64 blocks | 8192 EU/t | No |
| EV | 128 blocks | 32768 EU/t | Yes |
| IV | 256 blocks | 131072 EU/t | Yes |

Each station accepts up to 16A at its own voltage and keeps a 20-second buffer.
The 16A output is one shared power budget for all machines and player equipment.
Lower-voltage targets receive safe step-down power; their own input amperage limit still applies.
Power is deducted only when accepted. Targets rotate so the first connection cannot monopolize service.

Marker card:

- Shift + right-click a station to select its dimension, coordinates, and identity.
- Right-click a GT electric machine or multiblock energy hatch to connect it; click again with the same selected station to disconnect it. The card keeps the selection.
- Shift + right-click air clears the card without disconnecting machines.
- Each machine has one source. Selecting a different station and clicking the machine replaces its previous source.
- Both blocks must be in the same dimension and inside the station sphere.

Connections survive world saves and chunk unloads. The station never scans the world or loads target chunks.
Once per second, connections are removed if their chunk is loaded and the target position no longer contains a GT machine. Unloaded targets keep their connections.
Replaced stations get a new identity and do not inherit the old station's connections.
Use the station GUI to browse/remove links, toggle each service, and choose equipment or machine priority.
The outline button to the right of Disconnect highlights the selected machine in red for about 10 seconds, visible through blocks and after closing the GUI.
Connections show the machine's localized name, dimension, coordinates, and status. The saved machine type keeps the name available while its chunk is unloaded; older connections are identified when their chunks load.
EV/IV player charging covers the owner and online ServerUtilities teammates inside the same sphere and dimension.
All five stations and the reusable card have assembler recipes; LV materials are sufficient for the LV station and card.
Circuit inputs accept any circuit registered for the required tier: two for each station, one LV circuit for the card.
MetaTileEntity IDs: 31990 through 31994.
