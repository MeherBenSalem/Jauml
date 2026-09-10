# Building JAUML

JAUML is a MultiLoader Minecraft mod library. Shared Java sources live in **`common-shared/`** at the repository root; each Minecraft-version workspace compiles those sources into loader-specific jars (Fabric, NeoForge, or legacy Forge).

## Workspace layout

| Directory   | Minecraft | Build conventions        | JDK   |
|------------|-----------|--------------------------|-------|
| `1.20.1/`  | 1.20.1    | `buildSrc`               | 17    |
| `1.21.1/`  | 1.21.1    | `buildSrc`               | 21    |
| `1.21.11/` | 1.21.11   | `buildSrc`               | 21    |
| `26.1.2/`  | 26.1.2    | `build-logic` (composite)| 25    |
| `26.2/`    | 26.2      | `build-logic` (composite)| 25    |

- **`common-shared/`** — single source of truth for library API and tests (`src/main/java`, `src/test/java`).
- **Older workspaces (1.20.1–1.21.11)** — Gradle plugins live in `buildSrc/src/main/groovy/` (`multiloader-common.gradle`, `multiloader-loader.gradle`).
- **26.1.2+** — plugins live in `build-logic/` and are wired via `includeBuild('build-logic')` in `settings.gradle`.

Each workspace has `common`, `fabric`, and `neoforge` (or `forge` on 1.20.1) subprojects.

## Dependencies (Gson / SLF4J)

`multiloader-common.gradle` declares Gson and SLF4J as **`compileOnly`**:

- `com.google.code.gson:gson:2.10.1`
- `org.slf4j:slf4j-api` (version matches the workspace’s test SLF4J line: **1.7.36** on 1.20.1–26.x)

They are **not** bundled in the mod jar. Minecraft and the loader provide Gson and logging at runtime; `compileOnly` makes types available at compile time without shipping duplicate libraries.

Tests use `testImplementation` / `testRuntimeOnly` for Gson and SLF4J (with `slf4j-simple`).

## Build one workspace

From the repo root, `cd` into the target version folder and run Gradle with the matching JDK:

```powershell
# Example: 1.21.1 (JDK 21)
cd 1.21.1
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
.\gradlew.bat :common:test
.\gradlew.bat build

# Example: 26.1.2 (JDK 25)
cd 26.1.2
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot"
.\gradlew.bat :common:test
.\gradlew.bat build
```

Useful tasks:

- `:common:test` — unit tests from `common-shared` (no full game launch).
- `build` — compile and package all loader jars for that workspace.
- `clean` — remove build outputs.

## Verify all workspaces

From the repository root:

```powershell
.\verify_launch.ps1
```

This script cleans, runs `test`, and runs `build` for each version directory (`1.20.1`, `1.21.1`, `1.21.11`, `26.1.2`, `26.2`). Ensure the default `JAVA_HOME` (or toolchain resolution via Foojay) can satisfy each workspace’s Java version.

## Adding gradle.properties fields (26.1.2+)

For `26.1.2` and `26.2`, any new property used in resource expansion must be added to both `gradle.properties` and the `expandProps` map in `build-logic/src/main/groovy/multiloader-common.gradle`.
