package tn.naizo.jauml.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JaumlConfigCacheTest {

    @TempDir
    public Path tempDir;

    @BeforeEach
    public void setUp() {
        TestPlatformProvider.setTempDir(tempDir);
        JaumlConfig.clearCache();
    }

    @Test
    public void testInvalidateRemovesCachedInstance() {
        ConfigFile first = JaumlConfig.open("cache", "config");
        first.set("key", "first");
        first.save();

        JaumlConfig.invalidate("cache", "config");

        ConfigFile second = JaumlConfig.open("cache", "config");
        assertNotSame(first, second);
        assertEquals("first", second.getString("key"));
    }

    @Test
    public void testClearCache() {
        ConfigFile first = JaumlConfig.open("cache", "other");
        JaumlConfig.clearCache();
        ConfigFile second = JaumlConfig.open("cache", "other");
        assertNotSame(first, second);
    }
}
