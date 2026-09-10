package tn.naizo.jauml.api;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonMigrationTest {

    @Test
    public void testSequentialMigration() {
        JsonMigrator migrator = new JsonMigrator();

        // 1.0 -> 1.1: rename "usr" to "user"
        migrator.register("1.0", "1.1", old -> {
            JsonObject upgraded = JsonLib.deepClone(old).getAsJsonObject();
            upgraded.addProperty("user", upgraded.get("usr").getAsString());
            upgraded.remove("usr");
            return upgraded;
        });

        // 1.1 -> 2.0: add "enabled" with default true
        migrator.register("1.1", "2.0", old -> {
            JsonObject upgraded = JsonLib.deepClone(old).getAsJsonObject();
            upgraded.addProperty("enabled", true);
            return upgraded;
        });

        JsonObject oldJson = new JsonObject();
        oldJson.addProperty("version", "1.0");
        oldJson.addProperty("usr", "Alice");

        JsonObject migrated = migrator.migrate(oldJson, "2.0");

        assertEquals("2.0", migrated.get("version").getAsString());
        assertEquals("Alice", migrated.get("user").getAsString());
        assertTrue(migrated.get("enabled").getAsBoolean());
        assertNull(migrated.get("usr"));
    }

    @Test
    public void testMigrationNoPath() {
        JsonMigrator migrator = new JsonMigrator();
        migrator.register("1.0", "1.1", old -> old);

        JsonObject oldJson = new JsonObject();
        oldJson.addProperty("version", "1.0");

        // Requesting migration to 2.0 when only path to 1.1 exists
        assertThrows(JsonException.class, () -> migrator.migrate(oldJson, "2.0"));
    }

    @Test
    public void testMigrationNoVersion() {
        JsonMigrator migrator = new JsonMigrator();
        JsonObject oldJson = new JsonObject();
        oldJson.addProperty("name", "Alice"); // no version key

        assertThrows(JsonException.class, () -> migrator.migrate(oldJson, "2.0"));
    }

    @Test
    public void testPreferredVersionKey() {
        JsonMigrator migrator = new JsonMigrator();
        migrator.setPreferredVersionKey("configVersion");
        migrator.register("1.0", "2.0", old -> {
            JsonObject upgraded = JsonLib.deepClone(old).getAsJsonObject();
            upgraded.addProperty("migrated", true);
            return upgraded;
        });

        JsonObject oldJson = new JsonObject();
        oldJson.addProperty("configVersion", "1.0");
        oldJson.addProperty("name", "Alice");

        JsonObject migrated = migrator.migrate(oldJson, "2.0");

        assertEquals("2.0", migrated.get("configVersion").getAsString());
        assertTrue(migrated.get("migrated").getAsBoolean());
    }
}
