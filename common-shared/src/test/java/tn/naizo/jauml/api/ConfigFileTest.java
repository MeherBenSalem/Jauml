package tn.naizo.jauml.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigFileTest {

    @TempDir
    public Path tempDir;

    @BeforeEach
    public void setUp() {
        TestPlatformProvider.setTempDir(tempDir);
        JaumlConfig.clearCache();
    }

    @Test
    public void testBasicSaveAndLoad() {
        ConfigFile config = JaumlConfig.open("sub", "test_config");
        config.set("key1", "value1");
        config.set("key2", 123);
        config.set("key3", true);
        config.save();

        assertTrue(config.exists());

        config.reload();
        assertEquals("value1", config.getString("key1"));
        assertEquals(123, config.getInt("key2", 0));
        assertTrue(config.getBoolean("key3", false));
    }

    @Test
    public void testCoercionAndNormalization() {
        JsonObject defaults = new JsonObject();
        defaults.addProperty("version", "1.0");
        defaults.addProperty("enabled", false);
        defaults.addProperty("count", 10);

        ConfigFile config = JaumlConfig.open("sub", "coercion_config");
        config.setDefaultData(defaults);
        
        config.set("enabled", "true");
        config.set("count", "50");
        config.save();

        config.reload();
        assertTrue(config.getBoolean("enabled"));
        assertEquals(50, config.getInt("count", 0));
    }

    @Test
    public void testConfigMigration() {
        JsonMigrator migrator = new JsonMigrator();
        migrator.register("1.0", "2.0", old -> {
            JsonObject upgraded = JsonLib.deepClone(old).getAsJsonObject();
            upgraded.addProperty("newKey", "migratedValue");
            return upgraded;
        });

        JsonObject defaults = new JsonObject();
        defaults.addProperty("version", "2.0");
        defaults.addProperty("newKey", "defaultValue");

        ConfigFile preConfig = JaumlConfig.open("sub", "migrate_config");
        preConfig.set("version", "1.0");
        preConfig.set("oldKey", "oldValue");
        preConfig.save();

        ConfigFile config = JaumlConfig.open("sub", "migrate_config", null, migrator, "2.0", defaults);

        assertEquals("2.0", config.getString("version"));
        assertEquals("migratedValue", config.getString("newKey"));
        assertEquals("oldValue", config.getString("oldKey"));
    }

    @Test
    public void testCorruptedConfigRecovery() throws IOException {
        Path configPath = tempDir.resolve("sub").resolve("corrupt_config.json");
        Files.createDirectories(configPath.getParent());
        Files.write(configPath, "invalid json {[[}".getBytes());

        JsonObject defaults = new JsonObject();
        defaults.addProperty("recovered", true);

        ConfigFile config = JaumlConfig.open("sub", "corrupt_config", null, null, null, defaults);

        assertTrue(config.getBoolean("recovered"));

        Path backupPath = tempDir.resolve("sub").resolve("corrupt_config.json.bak");
        assertTrue(Files.exists(backupPath));
        assertEquals("invalid json {[[}", new String(Files.readAllBytes(backupPath)));
    }

    @Test
    public void testAsJsonObjectIsDeepClone() {
        ConfigFile config = JaumlConfig.open("sub", "clone_config");
        config.set("key", "value");
        JsonObject clone = config.asJsonObject();
        clone.addProperty("key", "mutated");
        assertEquals("value", config.getString("key"));
        assertEquals("value", config.getRoot().get("key").getAsString());
    }

    @Test
    public void testPathAccessors() {
        ConfigFile config = JaumlConfig.open("sub", "path_config");
        config.setByPath("settings.theme", new JsonPrimitive("dark"));
        config.setByPath("settings.port", new JsonPrimitive(9090));
        config.save();

        assertEquals("dark", config.getStringByPath("settings.theme"));
        assertEquals(9090, config.getIntByPath("settings.port", 0));
        assertTrue(config.getByPath("settings.theme").isPresent());

        config.reload();
        assertEquals("dark", config.getStringByPath("settings.theme", "light"));
        assertEquals(42.5, config.getDoubleByPath("settings.missing", 42.5));
        assertFalse(config.getBooleanByPath("settings.missing", false));
    }

    @Test
    public void testAtomicSaveIsReadable() throws IOException {
        ConfigFile config = JaumlConfig.open("sub", "atomic_config");
        config.set("atomic", true);
        config.save();

        Path configPath = tempDir.resolve("sub").resolve("atomic_config.json");
        assertTrue(Files.exists(configPath));
        String content = Files.readString(configPath);
        assertTrue(content.contains("\"atomic\""));
        assertTrue(content.contains("true"));
    }

    @Test
    public void testReloadIfChanged() throws IOException, InterruptedException {
        ConfigFile config = JaumlConfig.open("sub", "reload_config");
        config.set("v", 1);
        config.save();

        assertFalse(config.reloadIfChanged());

        Path configPath = tempDir.resolve("sub").resolve("reload_config.json");
        Files.writeString(configPath, "{\"v\": 2}");
        Thread.sleep(50);

        assertTrue(config.reloadIfChanged());
        assertEquals(2, config.getInt("v", 0));
    }

    @Test
    public void testStableSave() throws IOException {
        ConfigFile config = JaumlConfig.open("sub", "stable_config");
        config.set("z", 1);
        config.set("a", 2);
        config.setStableSave(true);
        config.save();

        Path configPath = tempDir.resolve("sub").resolve("stable_config.json");
        String content = Files.readString(configPath);
        int aPos = content.indexOf("\"a\"");
        int zPos = content.indexOf("\"z\"");
        assertTrue(aPos >= 0 && zPos >= 0);
        assertTrue(aPos < zPos, "Stable save should sort keys alphabetically");
    }

    @Test
    public void testBackupRotation() throws IOException {
        Path configPath = tempDir.resolve("sub").resolve("rotate_config.json");
        Files.createDirectories(configPath.getParent());
        Files.write(configPath, "not json".getBytes());

        JsonObject defaults = new JsonObject();
        defaults.addProperty("recovered", true);

        ConfigFile config = JaumlConfig.open("sub", "rotate_config", null, null, null, defaults);
        Files.write(configPath, "still bad".getBytes());
        config.setBackupRotation(3);
        config.reload();

        assertTrue(Files.exists(tempDir.resolve("sub").resolve("rotate_config.json.bak")));
    }
}
