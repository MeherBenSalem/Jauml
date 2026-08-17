# Jauml

Just Another Utility Minecraft Lib — a MultiLoader library mod providing JSON configuration utilities for Minecraft mods.

## Features

- **ConfigFile** — load, save, and manage JSON config files with corruption recovery
- **JsonLib** — parsing, path navigation, merge, normalization, and stable serialization
- **JsonSchema** — lightweight schema validation
- **JsonMigrator** — sequential config version migrations
- **JaumlConfig** — unified API for opening configs with schemas, defaults, and migrations

Shared source lives in [`common-shared/`](common-shared/). Each Minecraft version has its own Gradle workspace with Fabric and Forge/NeoForge loader modules.

## Supported versions

| Minecraft | Loaders | Java |
|-----------|---------|------|
| 1.20.1 | Fabric, Forge | 17 |
| 1.21.1 | Fabric, NeoForge | 21 |
| 1.21.11 | Fabric, NeoForge | 21 |
| 26.1.2 | Fabric, NeoForge | 25 |
| 26.2 | Fabric, NeoForge | 25 |

## Installation

Download the jar matching your Minecraft version and loader from [Modrinth](https://modrinth.com), [CurseForge](https://www.curseforge.com), or [GitHub Releases](https://github.com/MeherBenSalem/jauml/releases).

Place the jar in your mods folder. Jauml is a library dependency — end users only need it if a mod they use requires it.

## Usage

See [`MIGRATION_GUIDE.md`](MIGRATION_GUIDE.md) for API examples and upgrade notes from 2.0.x.

```java
ConfigFile config = JaumlConfig.open("my_mod", "settings");
JsonObject data = config.get();
```

## Building from source

Requirements: JDK 17+ (use JDK 25 when building `26.1.2` or `26.2` workspaces).

Build and test all workspaces:

```powershell
.\verify_launch.ps1
```

Build all loader jars into `dist/`:

```powershell
.\build_all_jars.ps1
```

Build a single workspace:

```powershell
cd 1.21.1
.\gradlew.bat test build
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](.github/SECURITY.md).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
