# JAUML JSON Utility Library Upgrade & Migration Guide (v2.0.0 to v2.3.0)

This guide documents the changes introduced through version `2.3.0` of the JSON library and how to safely adopt them.

## Compatibility Matrix

| Library Version | Supported JAUML App Versions | Compatible Java Versions | Gson Dependency Version |
| :--- | :--- | :--- | :--- |
| **2.0.0** | Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |
| **2.1.0** | Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |
| **2.3.0** (Current) | Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |

All 2.0.x public APIs remain supported. Existing config files, code invocations, and static helpers continue to function as before.

---

## Upgrade Overview

Version `2.3.0` is fully backward compatible with `2.0.0` and `2.1.0` public APIs.

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

## Code Examples

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
