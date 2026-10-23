package tn.naizo.jauml.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tn.naizo.jauml.api.TestPlatformProvider;
import tn.naizo.jauml.spi.PlatformProvider;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformTest {

    @TempDir
    public Path tempDir;

    @BeforeEach
    public void setUp() {
        TestPlatformProvider.setTempDir(tempDir.resolve("config"));
    }

    @Test
    public void testFacadeReadsTheRegisteredProvider() {
        assertEquals(Loader.UNKNOWN, Platform.loader());
        assertFalse(Platform.isFabric());
        assertFalse(Platform.isNeoForge());
        assertFalse(Platform.isForge());
        assertTrue(Platform.isDevelopment());
        assertEquals(tempDir.resolve("config"), Platform.configDir());
        // The default game directory is the config directory's parent.
        assertEquals(tempDir.toAbsolutePath(), Platform.gameDir());
        assertTrue(Platform.isDedicatedServer());
    }

    @Test
    public void testModVersionChecks() {
        assertTrue(Platform.isModLoaded("examplelib"));
        assertEquals("1.4.2", Platform.modVersionString("examplelib").orElse(null));
        assertEquals(4, Platform.modVersion("examplelib").get().minor());
        assertTrue(Platform.isModLoaded("examplelib", ">=1.4"));
        assertFalse(Platform.isModLoaded("examplelib", "[2.0,)"));
        assertFalse(Platform.isModLoaded("absent", "*"));
        assertEquals("Example Lib", Platform.modName("examplelib").orElse(null));
    }

    @Test
    public void testMinecraftVersion() {
        assertEquals("26.3", Platform.minecraftVersion());
        assertTrue(Platform.isMinecraft(">=26.2"));
        assertFalse(Platform.isMinecraft("[1.20.1]"));
    }

    @Test
    public void testLoaderNames() {
        assertEquals(Loader.FABRIC, Loader.fromName("Fabric"));
        assertEquals(Loader.NEOFORGE, Loader.fromName("neoforge"));
        assertEquals(Loader.FORGE, Loader.fromName(" Forge "));
        assertEquals(Loader.UNKNOWN, Loader.fromName(null));
        assertEquals(Loader.UNKNOWN, Loader.fromName("Quilt"));
    }

    @Test
    public void testServicesLoadsRegisteredImplementation() {
        assertNotNull(Services.load(PlatformProvider.class));
        assertTrue(Services.find(PlatformProvider.class).isPresent());
        // These tests also run inside the loader modules, where the real provider is registered too.
        assertTrue(Services.loadAll(PlatformProvider.class).stream().anyMatch(TestPlatformProvider.class::isInstance));
    }

    @Test
    public void testServicesFailsClearlyWhenMissing() {
        assertFalse(Services.find(Runnable.class).isPresent());
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> Services.load(Runnable.class));
        assertTrue(error.getMessage().contains("META-INF/services/java.lang.Runnable"));
    }
}
