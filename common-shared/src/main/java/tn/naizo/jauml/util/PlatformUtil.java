package tn.naizo.jauml.util;

import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.platform.Platform;
import tn.naizo.jauml.spi.PlatformProvider;

import java.nio.file.Path;

/**
 * Thin wrappers around platform detection helpers from {@link JaumlConfig}.
 * {@link Platform} offers the full set (loader, physical side, mod versions, game directory).
 */
public final class PlatformUtil {

    private PlatformUtil() {}

    public static String platformName() {
        return JaumlConfig.platform();
    }

    public static Path configDir() {
        return JaumlConfig.configDirectory();
    }

    public static boolean isModLoaded(String modId) {
        return JaumlConfig.isModLoaded(modId);
    }

    /**
     * Returns whether the current platform reports a development environment.
     * Fail-safe: returns {@code false} when no {@link PlatformProvider} is available.
     */
    public static boolean isDevelopmentEnvironment() {
        return Platform.isDevelopment();
    }
}
