# Building JAUML

JAUML is a MultiLoader Minecraft mod library. Shared Java sources live in **`common-shared/`** at the repository root; each Minecraft-version workspace compiles those sources into loader-specific jars (Fabric, NeoForge, or legacy Forge).

## Workspace layout

| Directory  | Minecraft | Build conventions         | Gradle | Fabric Loom | JDK |
|------------|-----------|---------------------------|--------|-------------|-----|
| `1.20.1/`  | 1.20.1    | `buildSrc`                | 8.11   | 1.9.2       | 17  |
| `1.21.1/`  | 1.21.1    | `buildSrc`                | 8.12   | 1.10.5      | 21  |
| `26.2/`    | 26.2      | `build-logic` (composite) | 9.8.1  | 1.19.3      | 25  |
| `26.3/`    | 26.3      | `build-logic` (composite) | 9.8.1  | 1.19.3      | 25  |

- **`common-shared/`** — single source of truth for library API and tests (`src/main/java`, `src/test/java`).
- **1.20.1 and 1.21.1** — Gradle plugins live in `buildSrc/src/main/groovy/` (`multiloader-common.gradle`, `multiloader-loader.gradle`).
- **26.x** — plugins live in `build-logic/` and are wired via `includeBuild('build-logic')` in `settings.gradle`. Minecraft 26.x ships unobfuscated, so these workspaces use `net.fabricmc.fabric-loom` without mappings. Loom 1.18+ requires Gradle itself to run on JDK 25.

Each workspace has `common`, `fabric`, and `neoforge` (or `forge` on 1.20.1) subprojects. Loader modules hold the `PlatformProvider` implementation and the event bridge that fires `ServerEvents`/`PlayerEvents`.

`common-shared/src/main/java` may reference Minecraft classes only when their name is identical on every supported version (today `MinecraftServer` and `ServerPlayer`, in `tn.naizo.jauml.server`). Tests must stay free of Minecraft types: the 1.20.1 `common` test classpath has no Minecraft classes.

## Dependencies (Gson / SLF4J)

`multiloader-common.gradle` declares Gson and SLF4J as **`compileOnly`**:

- `com.google.code.gson:gson:2.10.1`
- `org.slf4j:slf4j-api:1.7.36`

They are **not** bundled in the mod jar. Minecraft and the loader provide Gson and logging at runtime; `compileOnly` makes types available at compile time without shipping duplicate libraries.

Tests use `testImplementation` / `testRuntimeOnly` for Gson and SLF4J (with `slf4j-simple`).

## Build everything (root build)

The repository root is a small Gradle build that drives each workspace's own wrapper:

```powershell
.\gradlew.bat buildAll -Pjava25Home="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
```

| Task | What it does |
|------|--------------|
| `buildAll` (alias `build`, `dist`) | Builds and tests every workspace, then copies the loader jars into `dist/` (older jars there are removed first) |
| `testAll` | Runs the `common-shared` tests in every workspace |
| `cleanAll` | Cleans every workspace and `dist/` |
| `build<ws>` / `test<ws>` / `clean<ws>` | One workspace: `1201`, `1211`, `262`, `263` |
| `syncVersion -PmodVersion=x.y.z` | Sets `version=` in every workspace and `JaumlConfig.LIBRARY_VERSION` |

The 26.x workspaces get `JAVA_HOME` from `-Pjava25Home` or the `JAVA_HOME_25_X64` environment variable (GitHub's `setup-java` sets it). Run the root build itself on JDK 21: Gradle 8.11/8.12 in the older workspaces cannot run on JDK 25.

`LegacyCompatibilityTest` fails when `JaumlConfig.LIBRARY_VERSION` and `version=` disagree, so bump versions with `syncVersion`.

## Build one workspace

From the repo root, `cd` into the target version folder and run Gradle with the matching JDK:

```powershell
# Example: 1.21.1 (JDK 21)
cd 1.21.1
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\gradlew.bat :common:test
.\gradlew.bat build

# Example: 26.3 (JDK 25)
cd 26.3
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
.\gradlew.bat :common:test
.\gradlew.bat build
```

Useful tasks:

- `:common:test` — unit tests from `common-shared` (no full game launch).
- `build` — compile and package all loader jars for that workspace.
- `clean` — remove build outputs.

## Boot check (server events on a real server)

With `-Djauml.selfTest=true`, JAUML logs `JAUML self-test: STARTING`, `STARTED`, `first TICK_END`, `scheduler ran`, `STOPPING` and `STOPPED` from the loader bridge. The 26.x loader modules and 1.20.1 Forge have a GameTest server run that sets it and boots a real world without the EULA prompt:

```powershell
cd 26.3
.\gradlew.bat :neoforge:runGameTestServer   # also 26.2, and :forge:runGameTestServer on 1.20.1
.\gradlew.bat :fabric:runGametest           # also 26.2
```

Vanilla 1.20.1 and 1.21.1 GameTest servers refuse to start without a registered test, so those Fabric (and 1.21.1 NeoForge) modules have no such run. Pass the same property to any server (`java -Djauml.selfTest=true -jar ...`) to check those versions or a production install.

The PowerShell helpers (`verify_launch.ps1`, `build_all_jars.ps1`, `run_all_clients.ps1`) cover the same four workspaces; they use whatever `JAVA_HOME` is set, so prefer the root build for mixed JDKs.

## Adding gradle.properties fields (26.x)

For `26.2` and `26.3`, any new property used in resource expansion must be added to both `gradle.properties` and the `expandProps` map in `build-logic/src/main/groovy/multiloader-common.gradle`.
