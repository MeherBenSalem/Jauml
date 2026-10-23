package tn.naizo.jauml.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Loads a mod's own loader-specific implementations through {@link ServiceLoader}, the MultiLoader pattern where
 * {@code common} declares an interface and each loader module lists its implementation in
 * {@code META-INF/services/<interface name>}.
 * <pre>{@code
 * public static final IMyPlatform PLATFORM = Services.load(IMyPlatform.class);
 * }</pre>
 *
 * @since 3.0.0
 */
public final class Services {

    private Services() {}

    /**
     * Loads the single implementation of a service.
     *
     * @throws IllegalStateException when no implementation is registered
     */
    public static <T> T load(Class<T> service) {
        return find(service).orElseThrow(() -> new IllegalStateException(
                "No implementation of " + service.getName() + " found. Add META-INF/services/" + service.getName()
                        + " to each loader module."));
    }

    /**
     * Loads the first implementation of a service, or empty when none is registered.
     */
    public static <T> Optional<T> find(Class<T> service) {
        for (T implementation : ServiceLoader.load(service, service.getClassLoader())) {
            return Optional.of(implementation);
        }
        for (T implementation : ServiceLoader.load(service)) {
            return Optional.of(implementation);
        }
        return Optional.empty();
    }

    /**
     * Loads every registered implementation of a service, for extension points several mods contribute to.
     */
    public static <T> List<T> loadAll(Class<T> service) {
        List<T> implementations = new ArrayList<>();
        for (T implementation : ServiceLoader.load(service, service.getClassLoader())) {
            implementations.add(implementation);
        }
        return implementations;
    }
}
