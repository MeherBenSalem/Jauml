# Jauml

Just Another Utility Minecraft Lib — the shared MultiLoader library behind NightBeam mods: JSON and typed configs, a loader-neutral platform layer, server events with a tick scheduler, and small utilities.

## Features

### Configs
- **TypedConfig** — bind a plain Java class to a JSON file with `JaumlConfig.bind(...)`: `@Comment` and `@Range` become `//` comments, hand-edited files with comments load fine, missing keys get defaults, bad values are coerced or clamped, plus change listeners and reload
- **ConfigFile** — load, save, and manage JSON config files with corruption recovery
- **JsonLib** — parsing, path navigation, merge, normalization, and stable serialization
- **JsonSchema** — lightweight schema validation
- **JsonMigrator** — sequential config version migrations
- **JaumlConfig** — unified API for opening configs with schemas, defaults, and migrations

### Platform
- **Platform** — loader, physical side, game/config directories, Minecraft version, mod versions with range checks (`Platform.isModLoaded("geckolib", ">=4.4")`)
- **Services** — `Services.load(IMyHelper.class)` for your own loader-specific implementations (replaces the MultiLoader template's copy)

### Server runtime
- **ServerEvents / PlayerEvents** — server starting/started/stopping/stopped, tick start/end, player join/leave, fired on Fabric (with Fabric API), NeoForge and Forge
- **Scheduler** — `runLater`, `runRepeating` and `queueServerWork` on the server thread
- **Event** — a small prioritized listener list for your own events

### Utilities
- **Version / VersionRange** — compare versions and check Maven (`[2.1,3)`) or comparator (`>=2.1 <3`, `~2.3`, `^2`) ranges
- **Cooldowns** — per-player (or per-anything) cooldowns on wall-clock or tick time
- `PlatformUtil`, `JaumlLogger`, `ModuleVersion`, `Lifecycle` helpers

Shared source lives in [`common-shared/`](common-shared/). Each Minecraft version has its own Gradle workspace with Fabric and Forge/NeoForge loader modules.

## Supported versions

| Minecraft | Loaders | Java |
|-----------|---------|------|
| 1.20.1 | Fabric, Forge | 17 |
| 1.21.1 | Fabric, NeoForge | 21 |
| 26.2 | Fabric, NeoForge | 25 |
| 26.3 | Fabric, NeoForge | 25 |

On Fabric, install Fabric API alongside Jauml for server events and the scheduler (configs work without it). Jauml 2.3.1 remains available for Minecraft 1.21.11 and 26.1.2.

## Installation

Download the jar matching your Minecraft version and loader from [Modrinth](https://modrinth.com), [CurseForge](https://www.curseforge.com), or [GitHub Releases](https://github.com/MeherBenSalem/jauml/releases).

Place the jar in your mods folder. Jauml is a library dependency — end users only need it if a mod they use requires it.

## Usage

See [`MIGRATION_GUIDE.md`](MIGRATION_GUIDE.md) for the full API tour and upgrade notes from 2.x.

```java
// Typed config
public class MyConfig {
    @Comment("Maximum boss level")
    @Range(min = 1, max = 100)
    public int maxLevel = 50;
}
public static final TypedConfig<MyConfig> CONFIG = JaumlConfig.bind("my_mod", "common", MyConfig.class);
int max = CONFIG.get().maxLevel;

// Server runtime
ServerEvents.STARTED.register(server -> MyMod.LOG.info("Ready"));
Scheduler.runLater(20, () -> spawnWave());

// Platform
if (Platform.isModLoaded("geckolib", ">=4.4")) { /* optional integration */ }

// JSON config files (2.x API, still supported)
ConfigFile config = JaumlConfig.open("my_mod", "settings");
boolean enabled = config.getBoolean("enabled", true);
```

## Building from source

Requirements: JDK 21 for the root build and the 1.20.1/1.21.1 workspaces (1.20.1 compiles with a Java 17 toolchain that Gradle downloads), and JDK 25 for the 26.x workspaces.

Build and test every workspace, collecting loader jars into `dist/`:

```powershell
.\gradlew.bat buildAll -Pjava25Home="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
```

(`JAVA_HOME_25_X64` works instead of `-Pjava25Home`.) See [`BUILD.md`](BUILD.md) for single-workspace builds and the other root tasks.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Security

See [SECURITY.md](.github/SECURITY.md).

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE).
