# NH-Things-290

An Applied Energistics 2 addon for **GT New Horizons 2.9** on Minecraft 1.7.10.

## Current target

The initial development target is the local **GTNH 2.9.0-beta-3** instance:

- Applied Energistics 2 `rv3-beta-1050-GTNH`
- AE2 Fluid Crafting `1.5.106-gtnh`
- GregTech 5 Unofficial `5.09.54.133`

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
