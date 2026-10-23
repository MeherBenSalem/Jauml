package tn.naizo.jauml.internal;

import tn.naizo.jauml.spi.PlatformProvider;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Loads the loader's {@link PlatformProvider} once, so every JAUML entry point shares one instance.
 */
public final class PlatformProviders {

    private static volatile PlatformProvider provider;

    private PlatformProviders() {}

    /**
     * Returns the platform provider registered by the loader module.
     *
     * @throws IllegalStateException when no provider is registered
     */
    public static PlatformProvider get() {
        return find().orElseThrow(() -> new IllegalStateException(
                "Failed to find any registered PlatformProvider. Make sure a service file exists in META-INF/services/tn.naizo.jauml.spi.PlatformProvider"));
    }

    /**
     * Returns the platform provider, or empty when none is registered (for fail-safe callers).
     */
    public static Optional<PlatformProvider> find() {
        PlatformProvider found = provider;
        if (found == null) {
            synchronized (PlatformProviders.class) {
                if (provider == null) {
                    provider = load();
                }
                found = provider;
            }
        }
        return Optional.ofNullable(found);
    }

    private static PlatformProvider load() {
        try {
            for (PlatformProvider candidate : ServiceLoader.load(PlatformProvider.class)) {
                return candidate;
            }
            // The thread context loader may not see the mod's own jar (early loading, worker threads).
            for (PlatformProvider candidate : ServiceLoader.load(PlatformProvider.class, PlatformProvider.class.getClassLoader())) {
                return candidate;
            }
        } catch (ServiceConfigurationError | RuntimeException e) {
            return null;
        }
        return null;
    }
}
