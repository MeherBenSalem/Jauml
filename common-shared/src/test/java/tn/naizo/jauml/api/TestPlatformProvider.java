package tn.naizo.jauml.api;

import tn.naizo.jauml.spi.PlatformProvider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * Mock PlatformProvider for running JUnit tests without a Minecraft server/client environment.
 */
public class TestPlatformProvider implements PlatformProvider {

    private static final Map<String, String> MOD_VERSIONS = Map.of("minecraft", "26.3", "examplelib", "1.4.2");

    private static Path tempDir;

    public static void setTempDir(Path dir) {
        tempDir = dir;
    }

    @Override
    public String getPlatformName() {
        return "UnitTest";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return MOD_VERSIONS.containsKey(modId);
    }

    @Override
    public Optional<String> getModVersion(String modId) {
        return Optional.ofNullable(MOD_VERSIONS.get(modId));
    }

    @Override
    public Optional<String> getModName(String modId) {
        return "examplelib".equals(modId) ? Optional.of("Example Lib") : Optional.empty();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return true;
    }

    @Override
    public Path getConfigDirectory() {
        if (tempDir == null) {
            return Paths.get(System.getProperty("java.io.tmpdir")).resolve("jauml-test-config");
        }
        return tempDir;
    }
}
