# Developer setup

Add Jauml to your mod's build from the Modrinth Maven, declare the dependency in your mod metadata, and you can use every API on this site from common code.

## Gradle

Use the version number shown on the Modrinth Versions tab. It ends in your loader and Minecraft version.

```groovy
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    // Fabric 1.20.1 / 1.21.1 and Forge 1.20.1 remap mod jars:
    modImplementation "maven.modrinth:jauml:3.0.0+fabric-1.21.1"
    // NeoForge, and Fabric on 26.x (unobfuscated):
    implementation "maven.modrinth:jauml:3.0.0+neoforge-26.3"
}
```

In a MultiLoader project, depend on the matching loader jar from each loader module and compile your common module against one of them; the `tn.naizo.jauml` API is identical on every loader.

## Declare the dependency

Fabric, in `fabric.mod.json`:

```json
"depends": {
  "jauml": ">=3.0.0"
}
```

NeoForge (`neoforge.mods.toml`) or Forge (`mods.toml`):

```toml
[[dependencies.my_mod]]
    modId = "jauml"
    type = "required"          # NeoForge; on Forge use mandatory = true
    versionRange = "[3.0.0,)"
    ordering = "NONE"
    side = "BOTH"
```

Ranges written for 2.x, such as `[2.3.1,)` or `>=2.3.1`, accept 3.0.0 unchanged.

## Check the version at startup

```java
import tn.naizo.jauml.api.JaumlConfig;

if (!JaumlConfig.isCompatible("2.0.0")) {
    LOGGER.warn("Jauml is too old: need 2.0.0+, found " + JaumlConfig.LIBRARY_VERSION);
}
```

`JaumlConfig.isCompatible("2.x.y")` returns `true` on 3.x, because 3.x ships the whole 2.x API (`JaumlConfig.API_BASELINE_MAJOR = 2`).

## Packages

| Package | What's in it |
| --- | --- |
| `tn.naizo.jauml.config` | [Typed configs](typed-configs.md): `TypedConfig`, `@Comment`, `@Range` |
| `tn.naizo.jauml.api` | [JSON configs](json-configs.md): `JaumlConfig`, `ConfigFile`, `JsonLib`, `JsonSchema`, `JsonMigrator` |
| `tn.naizo.jauml.server` | [Server events](server-events.md): `ServerEvents`, `PlayerEvents`, `Scheduler` |
| `tn.naizo.jauml.platform` | [Platform](platform.md): `Platform`, `Services`, `Loader` |
| `tn.naizo.jauml.util`, `tn.naizo.jauml.event` | [Utilities](utilities.md): `Cooldowns`, `Version`, `VersionRange`, `Event` and helpers |

## Check a boot

Start a server with `-Djauml.selfTest=true` to log every server event and one scheduler run. It's an easy way to confirm Jauml is wired up on a new loader or Minecraft version.
