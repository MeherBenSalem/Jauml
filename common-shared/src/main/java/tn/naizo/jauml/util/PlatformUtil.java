package tn.naizo.jauml.util;

import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.spi.PlatformProvider;

import java.nio.file.Path;
import java.util.ServiceLoader;

/**
 * Thin wrappers around platform detection helpers from {@link JaumlConfig}.
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
        try {
            ServiceLoader<PlatformProvider> loader = ServiceLoader.load(PlatformProvider.class);
            for (PlatformProvider provider : loader) {
                return provider.isDevelopmentEnvironment();
            }
        } catch (Exception ignored) {
            // fail-safe
        }
        return false;
    }
}
