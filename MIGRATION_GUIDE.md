# JAUML JSON Utility Library Upgrade & Migration Guide (v2.0.0 to v2.1.0)

This guide documents the changes introduced in version `2.1.0` of the JSON library and how to safely adopt them.

## Compatibility Matrix

| Library Version | Supported JAUML App Versions | Compatible Java Versions | Gson Dependency Version |
| :--- | :--- | :--- | :--- |
| **2.0.0** | Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |
| **2.1.0** (Current) | Minecraft 1.20.1, 1.21.1, 1.21.11, 26.1.2, 26.2 | Java 17, 21, 25 | Gson 2.10.x |

---

## Upgrade Overview

Version `2.1.0` maintains backward compatibility with the public API of version `2.0.0`. Existing config files, code invocations, and static helpers continue to function as before.

The primary additions are:
1. **Validation & Type Coercion**: Configs are validated against a schema and auto-coerced if keys are malformed.
2. **Corrupted Config Recovery**: Malformed JSON config files are safely renamed to `<filename>.bak` and recreated from defaults instead of causing a startup crash.
3. **Sequential Migrations**: Upgrades old configurations through a registered directed path.

---

## Code Examples

### 1. Simple Config opening (Legacy)
```java
// Still supported, returns ConfigFile with standard 2.0.0 functionality
ConfigFile config = JaumlConfig.open("my_sub_dir", "my_config");
```

### 2. Config opening with Schema and Defaults (New in 2.1.0)
```java
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.api.ConfigFile;
import tn.naizo.jauml.api.JsonSchema;
import com.google.gson.JsonObject;

// 1. Define schema
JsonSchema schema = JsonSchema.parse("{" +
    "\"type\":\"object\"," +
    "\"properties\":{" +
        "\"enabled\":{\"type\":\"boolean\"}," +
        "\"port\":{\"type\":\"number\"}" +
    "}" +
"}");

// 2. Define defaults template
JsonObject defaults = new JsonObject();
defaults.addProperty("enabled", true);
defaults.addProperty("port", 8080);

// 3. Open config (enforces type coercion and normalizes config against template)
ConfigFile config = JaumlConfig.open("my_sub_dir", "my_config", schema, defaults);
```

### 3. Config opening with Sequential Migrations (New in 2.1.0)
```java
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.api.ConfigFile;
import tn.naizo.jauml.api.JsonMigrator;
import tn.naizo.jauml.api.JsonSchema;
import com.google.gson.JsonObject;

// 1. Set up migrator
JsonMigrator migrator = new JsonMigrator();
migrator.register("1.0", "1.1", old -> {
    JsonObject upgraded = old.deepCopy();
    upgraded.addProperty("newKey", "newValue");
    return upgraded;
});
migrator.register("1.1", "2.0", old -> {
    JsonObject upgraded = old.deepCopy();
    upgraded.addProperty("enabled", true);
    return upgraded;
});

// 2. Open config specifying current target schema version ("2.0")
ConfigFile config = JaumlConfig.open("my_sub_dir", "my_config", schema, migrator, "2.0", defaults);
```

### 4. Compatibility checks during startup
To verify that the current runtime library matches your mod requirements:
```java
if (!JaumlConfig.isCompatible("2.1.0")) {
    LOGGER.warn("JAUML JSON library is outdated! Expecting 2.1.0, found: " + JaumlConfig.LIBRARY_VERSION);
}
```
