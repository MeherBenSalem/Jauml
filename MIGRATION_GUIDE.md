# JAUML Upgrade & Migration Guide (v2.0.0 to v3.0.0)

This guide documents the changes introduced through version `3.0.0` and how to safely adopt them.

## Compatibility Matrix

| Library Version | Minecraft | Compatible Java Versions | Gson Dependency Version |
| :--- | :--- | :--- | :--- |
| **2.0.0** | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |
| **2.1.0** | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |
| **2.3.0** | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |
| **2.3.1** | 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2, 26.3 | Java 17, 21, 25 | Gson 2.10.x |
| **3.0.0** (Current) | 1.20.1, 1.21.1, 26.2, 26.3 | Java 17, 21, 25 | Gson 2.10.x |

All 2.x public APIs remain supported in 3.x. Existing config files, code invocations, and static helpers continue to function as before.

---

## Upgrading to 3.0.0

### What changed for players and modpacks

- **Supported Minecraft versions are now 1.20.1, 1.21.1, 26.2 and 26.3.** 1.21.11 and 26.1.2 builds are no longer produced; keep using 2.3.1 there.
- Each jar now declares only its own Minecraft version (`[26.2,26.3)`, `[26.3,26.4)`), so a 26.2 jar no longer offers itself on 26.3.
- On Fabric, JAUML **recommends** Fabric API. Configs work without it; server events and the scheduler need it and log a warning when it is missing.

### What changed for mod code

Nothing was removed, and no signature changed:

- `JaumlConfigLib` keeps every method and signature for the whole 3.x line. It is still deprecated in favour of `JaumlConfig`, but it is no longer marked for removal, so compiling against it gives a deprecation warning instead of a removal warning.
- `JaumlConfig.isCompatible("2.x.y")` still returns `true`: 3.x ships the full 2.x API (`JaumlConfig.API_BASELINE_MAJOR = 2`). Dependency ranges such as `[2.3.1,)` or `>=2.3.1` in your `mods.toml`/`fabric.mod.json` accept 3.0.0 unchanged.
- `PlatformProvider` gained default methods (`getGameDirectory`, `isPhysicalClient`, `getModVersion`, `getModName`, `getLoadedModIds`, `getMinecraftVersion`), so existing third-party implementations still compile.

### New: replace your MultiLoader `Services` / `IPlatformHelper` copy

```java
import tn.naizo.jauml.platform.Platform;
import tn.naizo.jauml.platform.Services;

// Your own loader-specific services, same META-INF/services files as before
public static final IMyNetwork NETWORK = Services.load(IMyNetwork.class);

if (Platform.isClient()) { /* physical client */ }
if (Platform.isModLoaded("geckolib", ">=4.4")) { /* optional integration */ }
if (Platform.isMinecraft(">=26.2")) { /* version-specific data */ }
Path saves = Platform.gameDir().resolve("saves");
String loader = Platform.loader().displayName(); // Fabric, NeoForge or Forge
```

### New: server events and the scheduler (replaces `queueServerWork` work queues)

```java
import tn.naizo.jauml.server.PlayerEvents;
import tn.naizo.jauml.server.Scheduler;
import tn.naizo.jauml.server.ServerEvents;

ServerEvents.STARTED.register(server -> MyMod.LOG.info("Server ready"));
ServerEvents.STOPPING.register(server -> MyData.flush());
PlayerEvents.JOIN.register(player -> Welcome.send(player));

Scheduler.queueServerWork(20, () -> spawnWave());        // same meaning as the MCreator helper
Scheduler.Task pulse = Scheduler.runRepeating(0, 100, MyMod::pulse);
pulse.cancel();

ServerEvents.currentServer().ifPresent(server -> ...);   // between STARTING and STOPPED
```

Events fire on Fabric (with Fabric API), NeoForge and Forge, for dedicated and integrated servers. Scheduled tasks run on the server thread at the end of a tick and are dropped when the server stops.

### New: typed configs

```java
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.config.Comment;
import tn.naizo.jauml.config.Range;
import tn.naizo.jauml.config.TypedConfig;

@Comment("My Mod settings")
public class MyConfig {
    @Comment("Maximum boss level")
    @Range(min = 1, max = 100)
    public int maxLevel = 50;
    public List<String> blockedDimensions = new ArrayList<>();
    public Drops drops = new Drops(); // nested classes become nested objects
}

public static final TypedConfig<MyConfig> CONFIG = JaumlConfig.bind("my_mod", "common", MyConfig.class);

int max = CONFIG.get().maxLevel;               // read through get(): reloads swap the instance
CONFIG.update(c -> c.maxLevel = 75);           // edit, clamp, save, notify listeners
CONFIG.addListener(c -> rebuildCaches(c));
Scheduler.runRepeating(100, 100, CONFIG::reloadIfChanged); // optional hot reload
```

The file is written with `//` comments (description, range, allowed enum values and default) and read back leniently, so hand edits with comments are fine. Missing keys get defaults, wrong types are coerced or reset, out-of-range numbers are clamped with a warning. A file that does not parse at startup is moved to `<name>.json.bak` and replaced with defaults; one that breaks during `reload()` keeps the previous values. Use `TypedConfig.builder(MyConfig.class).file(...).migrator(migrator, "2").build()` to run `JsonMigrator` steps first, or `.comments(false)` for plain JSON.

Do not open a commented typed-config file with `JaumlConfig.open(...)`: `ConfigFile` parses strictly and would treat the comments as corruption.

### New: small utilities

```java
import tn.naizo.jauml.util.Cooldowns;
import tn.naizo.jauml.util.Version;

private static final Cooldowns<UUID> DASH = Cooldowns.millis();
if (DASH.tryUse(player.getUUID(), 3_000)) { dash(player); }

Cooldowns<UUID> tickCooldowns = Cooldowns.withClock(Scheduler::currentTick);

Version.parse("26.3.0.95-beta").isAtLeast("26.3");
Version.parse("2.3.1").satisfies("[2.1,3)");   // Maven ranges, >=/</~/^, 2.x wildcards, || alternatives
```

---

## Upgrade Overview (2.x)

Version `2.3.1` is a patch release that adds Minecraft 26.3 support with no API changes. Version `2.3.0` remains fully backward compatible with `2.0.0` and `2.1.0` public APIs.

Primary additions in 2.3.0:
1. **Root JSON access**: `asJsonObject()` / `getRoot()` return a safe deep clone.
2. **Path accessors**: Dot-path getters and setters on `ConfigFile` delegating to `JsonLib`.
3. **Cache control**: `JaumlConfig.invalidate()` and `clearCache()` for fresh config instances.
4. **Atomic saves**: Config writes use temp-file-then-move for crash safety.
5. **Reload on change**: `reloadIfChanged()` avoids unnecessary disk reads.
6. **Stable save opt-in**: `setStableSave(true)` for deterministic key ordering on save.
7. **Backup rotation**: `setBackupRotation(n)` for multi-generation `.bak` files on corruption recovery.
8. **Preferred version key**: `JsonMigrator.setPreferredVersionKey()` for custom version field names.
9. **Production-safe init**: `JaumlInitializer.initialize()` skips demo config write unless in dev or `-Djauml.writeDemoConfig=true`.
10. **Optional utilities** (`tn.naizo.jauml.util`): `PlatformUtil`, `JaumlLogger`, `ModuleVersion`, and `Lifecycle` are additive helpers. The core `tn.naizo.jauml.api` package is unchanged. `JaumlConfigLib` remains available (deprecated but not removed).

---

## Code Examples (2.x APIs, all still supported)

### 1. Simple Config opening (Legacy — still supported)
```java
ConfigFile config = JaumlConfig.open("my_sub_dir", "my_config");
```

### 2. Reading root JSON and nested paths (New in 2.3.0)
```java
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

JsonObject root = config.asJsonObject(); // deep clone; safe to inspect
String theme = config.getStringByPath("settings.theme", "dark");
int port = config.getIntByPath("server.port", 8080);
config.setByPath("settings.theme", new JsonPrimitive("light"));
config.save();
```

### 3. Config opening with Schema and Defaults (2.1.0+)
```java
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.api.ConfigFile;
import tn.naizo.jauml.api.JsonSchema;
import com.google.gson.JsonObject;

JsonSchema schema = JsonSchema.parse("{" +
    "\"type\":\"object\"," +
    "\"properties\":{" +
        "\"enabled\":{\"type\":\"boolean\"}," +
        "\"port\":{\"type\":\"number\"}" +
    "}" +
"}");

JsonObject defaults = new JsonObject();
defaults.addProperty("enabled", true);
defaults.addProperty("port", 8080);

ConfigFile config = JaumlConfig.open("my_sub_dir", "my_config", schema, defaults);
```

`JsonSchema` in 2.3.0 also supports optional constraint keywords (`enum`, `minimum`/`maximum`, `minLength`/`maxLength`, `additionalProperties: false`, `minItems`/`maxItems`). These are only enforced when present in the schema; existing schemas without them are unchanged.

### 4. Config opening with Sequential Migrations (2.1.0+)
```java
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.api.ConfigFile;
import tn.naizo.jauml.api.JsonMigrator;
import tn.naizo.jauml.api.JsonLib;
import com.google.gson.JsonObject;

JsonMigrator migrator = new JsonMigrator();
migrator.setPreferredVersionKey("configVersion"); // optional, 2.3.0+
migrator.register("1.0", "2.0", old -> {
    JsonObject upgraded = JsonLib.deepClone(old).getAsJsonObject();
    upgraded.addProperty("enabled", true);
    return upgraded;
});

ConfigFile config = JaumlConfig.open("my_sub_dir", "my_config", schema, migrator, "2.0", defaults);
```

### 5. Cache invalidation (New in 2.3.0)
```java
JaumlConfig.invalidate("my_sub_dir", "my_config");
ConfigFile fresh = JaumlConfig.open("my_sub_dir", "my_config"); // new instance from disk
```

### 6. Compatibility checks during startup
```java
if (!JaumlConfig.isCompatible("2.0.0")) {
    LOGGER.warn("JAUML JSON library is outdated! Expecting 2.0.0+, found: " + JaumlConfig.LIBRARY_VERSION);
}
```

### 7. Stable save and reload (New in 2.3.0)
```java
config.setStableSave(true);
config.save(); // keys sorted alphabetically

if (config.reloadIfChanged()) {
    LOGGER.info("Config reloaded from disk after external edit");
}
```

### 8. Initializer behavior (Changed in 2.3.0)
```java
// Production: compatibility log only, no demo config write
JaumlInitializer.initialize();

// Development or explicit opt-in: full demo config workflow
JaumlInitializer.initializeWithDemoConfig();
// Or: java -Djauml.writeDemoConfig=true ...
```

### 9. Optional utilities (New in 2.3.0)
```java
import tn.naizo.jauml.util.Lifecycle;
import tn.naizo.jauml.util.ModuleVersion;
import tn.naizo.jauml.util.PlatformUtil;

Lifecycle.requireCompatibleLibrary("2.0.0");
Lifecycle.onStartup("my_mod", () -> { /* startup work */ });

ModuleVersion storage = new ModuleVersion("jauml-storage", "1.2.0");
if (storage.isCompatibleWith("1.0.0")) {
    // safe to use storage module
}

boolean dev = PlatformUtil.isDevelopmentEnvironment();
```
