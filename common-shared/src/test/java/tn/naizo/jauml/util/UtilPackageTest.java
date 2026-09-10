package tn.naizo.jauml.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.api.TestPlatformProvider;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class UtilPackageTest {

    @TempDir
    public Path tempDir;

    @BeforeEach
    public void setUp() {
        TestPlatformProvider.setTempDir(tempDir);
        JaumlConfig.clearCache();
    }

    @Test
    public void testPlatformUtilBasics() {
        assertEquals("UnitTest", PlatformUtil.platformName());
        assertEquals(tempDir, PlatformUtil.configDir());
        assertFalse(PlatformUtil.isModLoaded("some_mod"));
        assertTrue(PlatformUtil.isDevelopmentEnvironment());
    }

    @Test
    public void testModuleVersionCompatibility() {
        ModuleVersion module = new ModuleVersion("example", "2.3.0");

        assertTrue(module.isCompatibleWith("2.0.0"));
        assertTrue(module.isCompatibleWith("2.3.0"));
        assertFalse(module.isCompatibleWith("2.4.0"));
        assertFalse(module.isCompatibleWith("3.0.0"));
        assertFalse(module.isCompatibleWith(""));
        assertFalse(module.isCompatibleWith(null));
    }

    @Test
    public void testLifecycleRequireCompatibleLibraryDoesNotThrow() {
        assertDoesNotThrow(() -> Lifecycle.requireCompatibleLibrary("2.0.0"));
        assertDoesNotThrow(() -> Lifecycle.requireCompatibleLibrary("99.0.0"));
    }

    @Test
    public void testLifecycleOnStartupRunsAction() {
        boolean[] ran = {false};
        Lifecycle.onStartup("test-mod", () -> ran[0] = true);
        assertTrue(ran[0]);
    }

    @Test
    public void testLifecycleOnStartupCatchesExceptions() {
        assertDoesNotThrow(() -> Lifecycle.onStartup("test-mod", () -> {
            throw new RuntimeException("startup failure");
        }));
    }

    @Test
    public void testJaumlLoggerFactory() {
        assertNotNull(JaumlLogger.get());
        assertNotNull(JaumlLogger.get("CustomLogger"));
    }
}
