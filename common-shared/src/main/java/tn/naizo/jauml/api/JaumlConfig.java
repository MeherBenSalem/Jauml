package tn.naizo.jauml.api;

import tn.naizo.jauml.config.TypedConfig;
import tn.naizo.jauml.internal.PathValidator;
import tn.naizo.jauml.internal.PlatformProviders;
import tn.naizo.jauml.spi.PlatformProvider;
import tn.naizo.jauml.util.Version;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class JaumlConfig {

    private static final PlatformProvider PLATFORM_PROVIDER = PlatformProviders.get();
    private static final Map<Path, ConfigFile> CACHE = new ConcurrentHashMap<>();

    public static final String LIBRARY_VERSION = "3.0.0";

    /**
     * The oldest major version whose public API this library still ships in full. JAUML 3 removes nothing from
     * the 2.x API, so mods written against 2.x keep passing {@link #isCompatible(String)}.
     */
    public static final int API_BASELINE_MAJOR = 2;

    /**
     * Checks if the current library version is compatible with the required version.
     * Within the current major version the minor version must be greater than or equal to the required one;
     * a required version from an older major is compatible down to {@link #API_BASELINE_MAJOR}.
     */
    public static boolean isCompatible(String requiredVersion) {
        if (requiredVersion == null || requiredVersion.trim().isEmpty()) {
            return false;
        }
        try {
            String[] reqParts = requiredVersion.trim().split("\\.");
            int reqMajor = Integer.parseInt(reqParts[0]);
            int reqMinor = Integer.parseInt(reqParts[1]);
            Version current = Version.parse(LIBRARY_VERSION);

            if (reqMajor < API_BASELINE_MAJOR || reqMajor > current.major()) {
                return false;
            }
            return reqMajor < current.major() || current.minor() >= reqMinor;
        } catch (Exception e) {
            return false;
        }
    }

    private JaumlConfig() {}

    /**
     * Opens or creates a config file relative to the game's config directory.
     * The file is cached in memory, ensuring that subsequent calls with the same path
     * return the same ConfigFile instance.
     *
     * @param subdirectory the target subdirectory in the game's config folder
     * @param fileName the name of the config file
     * @return a thread-safe ConfigFile instance
     */
    public static ConfigFile open(String subdirectory, String fileName) {
        Path resolved = PathValidator.resolveSafe(PLATFORM_PROVIDER.getConfigDirectory(), subdirectory, fileName);
        return CACHE.computeIfAbsent(resolved, ConfigFile::new);
    }

    /**
     * Opens or creates a config file relative to the game's config directory with a schema.
     */
    public static ConfigFile open(String subdirectory, String fileName, JsonSchema schema) {
        return open(subdirectory, fileName, schema, null, null, null);
    }

    /**
     * Opens or creates a config file relative to the game's config directory with a schema and defaults.
     */
    public static ConfigFile open(String subdirectory, String fileName, JsonSchema schema, JsonObject defaultData) {
        return open(subdirectory, fileName, schema, null, null, defaultData);
    }

    /**
     * Opens or creates a config file relative to the game's config directory with a schema, migrator, target version, and defaults.
     */
    public static ConfigFile open(String subdirectory, String fileName, JsonSchema schema, JsonMigrator migrator, String targetVersion, JsonObject defaultData) {
        Path resolved = PathValidator.resolveSafe(PLATFORM_PROVIDER.getConfigDirectory(), subdirectory, fileName);
        return CACHE.compute(resolved, (path, existing) -> {
            ConfigFile created = existing != null ? existing : new ConfigFile(path);
            created.configure(schema, migrator, targetVersion, defaultData);
            return created;
        });
    }

    /**
     * Opens or creates a config file relative to the game's config directory.
     * If the file is newly opened and does not physically exist on disk,
     * the provided defaults Consumer is executed to initialize default properties,
     * and the file is automatically saved to disk.
     *
     * @param subdirectory the target subdirectory in the game's config folder
     * @param fileName the name of the config file
     * @param defaults a consumer that populates default values
     * @return a thread-safe ConfigFile instance
     */
    public static ConfigFile open(String subdirectory, String fileName, Consumer<ConfigFile> defaults) {
        Path resolved = PathValidator.resolveSafe(PLATFORM_PROVIDER.getConfigDirectory(), subdirectory, fileName);
        
        return CACHE.compute(resolved, (path, existing) -> {
            if (existing != null) {
                return existing;
            }
            ConfigFile created = new ConfigFile(path);
            if (!created.exists() && defaults != null) {
                defaults.accept(created);
                created.save();
            }
            return created;
        });
    }

    /**
     * Binds {@code <config dir>/<subdirectory>/<fileName>.json} to a plain Java class whose fields are the options.
     * See {@link TypedConfig} for comments, ranges, reloading and change listeners.
     *
     * @since 3.0.0
     */
    public static <T> TypedConfig<T> bind(String subdirectory, String fileName, Class<T> type) {
        return TypedConfig.bind(subdirectory, fileName, type);
    }

    /**
     * Gets the current platform's configuration directory.
     */
    public static Path configDirectory() {
        return PLATFORM_PROVIDER.getConfigDirectory();
    }

    /**
     * Gets the name of the current platform (e.g. "Fabric", "NeoForge", or "Forge").
     */
    public static String platform() {
        return PLATFORM_PROVIDER.getPlatformName();
    }

    /**
     * Checks if a mod with the given ID is loaded on the current platform.
     */
    public static boolean isModLoaded(String modId) {
        return PLATFORM_PROVIDER.isModLoaded(modId);
    }

    /**
     * Removes a cached config entry for the given subdirectory and file name.
     * A subsequent {@link #open(String, String)} call will create a fresh {@link ConfigFile}.
     */
    public static void invalidate(String subdirectory, String fileName) {
        Path resolved = PathValidator.resolveSafe(PLATFORM_PROVIDER.getConfigDirectory(), subdirectory, fileName);
        invalidate(resolved);
    }

    /**
     * Removes a cached config entry for the given resolved path.
     * A subsequent {@link #open(String, String)} call will create a fresh {@link ConfigFile}.
     */
    public static void invalidate(Path path) {
        if (path != null) {
            CACHE.remove(path);
        }
    }

    /**
     * Clears the entire config cache. Subsequent {@link #open} calls create fresh instances.
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
