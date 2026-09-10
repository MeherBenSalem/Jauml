package tn.naizo.jauml.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tn.naizo.jauml.JaumlConfigLib;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LegacyCompatibilityTest {

    @TempDir
    public Path tempDir;

    @BeforeEach
    public void setUp() {
        TestPlatformProvider.setTempDir(tempDir);
        JaumlConfig.clearCache();
    }

    @Test
    public void testLibraryVersionCompatibility() {
        assertTrue(JaumlConfig.LIBRARY_VERSION.startsWith("2."));
        assertTrue(JaumlConfig.isCompatible("2.0.0"));
    }

    @Test
    public void testLegacyStaticApi() {
        String dir = "legacy";
        String file = "config";

        // 1. Create file and verify existence
        assertTrue(JaumlConfigLib.createConfigFile(dir, file));
        assertTrue(JaumlConfigLib.configFileExists(dir, file));

        // 2. Write and read strings
        assertTrue(JaumlConfigLib.setStringValue(dir, file, "host", "localhost"));
        assertEquals("localhost", JaumlConfigLib.getStringValue(dir, file, "host"));
        assertEquals("default", JaumlConfigLib.getStringValue(dir, file, "nonexistent", "default"));

        // 3. Write and read numbers (double, int, long)
        assertTrue(JaumlConfigLib.setNumberValue(dir, file, "port", 3306.0));
        assertEquals(3306.0, JaumlConfigLib.getNumberValue(dir, file, "port"));
        assertEquals(3306, JaumlConfigLib.getIntValue(dir, file, "port"));
        assertEquals(3306L, JaumlConfigLib.getLongValue(dir, file, "port"));

        assertTrue(JaumlConfigLib.setIntValue(dir, file, "players", 10));
        assertEquals(10, JaumlConfigLib.getIntValue(dir, file, "players"));

        assertTrue(JaumlConfigLib.setLongValue(dir, file, "timestamp", 123456789L));
        assertEquals(123456789L, JaumlConfigLib.getLongValue(dir, file, "timestamp"));

        // 4. Write and read booleans
        assertTrue(JaumlConfigLib.setBooleanValue(dir, file, "enabled", true));
        assertTrue(JaumlConfigLib.getBooleanValue(dir, file, "enabled"));

        // 5. List/Array operations
        String arrayKey = "whitelist";
        assertTrue(JaumlConfigLib.addStringToArray(dir, file, arrayKey, "Alice"));
        assertTrue(JaumlConfigLib.addStringToArray(dir, file, arrayKey, "Bob"));
        // Alice should be present
        assertTrue(JaumlConfigLib.stringExistsInArray(dir, file, arrayKey, "Alice"));
        // Duplicate check (Alice shouldn't be added again, addStringToArray returns false or true?
        // Original addStringToArray returns false if duplicate exists)
        assertFalse(JaumlConfigLib.addStringToArray(dir, file, arrayKey, "Alice"));

        assertEquals(2, JaumlConfigLib.getArrayLength(dir, file, arrayKey));
        assertEquals("Bob", JaumlConfigLib.getArrayElement(dir, file, arrayKey, 1));

        List<String> list = JaumlConfigLib.getArrayAsList(dir, file, arrayKey);
        assertEquals(2, list.size());
        assertTrue(list.contains("Alice"));
        assertTrue(list.contains("Bob"));

        // Remove element
        assertTrue(JaumlConfigLib.removeArrayElement(dir, file, arrayKey, "Alice"));
        assertEquals(1, JaumlConfigLib.getArrayLength(dir, file, arrayKey));
        assertFalse(JaumlConfigLib.stringExistsInArray(dir, file, arrayKey, "Alice"));

        // Clear array
        assertTrue(JaumlConfigLib.clearArray(dir, file, arrayKey));
        assertEquals(0, JaumlConfigLib.getArrayLength(dir, file, arrayKey));

        // 6. Key utilities
        assertTrue(JaumlConfigLib.hasKey(dir, file, "host"));
        List<String> keys = JaumlConfigLib.getAllKeys(dir, file);
        assertTrue(keys.contains("host"));
        assertTrue(keys.contains("port"));

        assertTrue(JaumlConfigLib.removeKey(dir, file, "host"));
        assertFalse(JaumlConfigLib.hasKey(dir, file, "host"));

        // 7. Delete config file
        assertTrue(JaumlConfigLib.deleteConfigFile(dir, file));
        assertFalse(JaumlConfigLib.configFileExists(dir, file));
    }
}
