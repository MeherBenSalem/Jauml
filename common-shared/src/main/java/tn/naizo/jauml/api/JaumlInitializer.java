package tn.naizo.jauml.api;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.naizo.jauml.spi.PlatformProvider;

import java.util.ServiceLoader;

/**
 * Startup initialization helper that runs compatibility checks, registers migrations,
 * loads the application configuration, validates it, and ensures startup resilience.
 */
public final class JaumlInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlInit");
    private static final String DEMO_CONFIG_PROPERTY = "jauml.writeDemoConfig";

    private JaumlInitializer() {}

    /**
     * Runs JAUML library startup initialization.
     * Performs a compatibility check and logs the library version.
     * Demo config write is skipped in production unless {@code -Djauml.writeDemoConfig=true}
     * or the platform reports a development environment.
     */
    public static void initialize() {
        LOGGER.info("Initializing JAUML JSON library and verifying configuration...");
        runCompatibilityCheck();

        if (shouldWriteDemoConfig()) {
            initializeWithDemoConfig();
        } else {
            LOGGER.info("JAUML demo config write skipped (production mode). Use initializeWithDemoConfig() or -D{}=true to enable.", DEMO_CONFIG_PROPERTY);
        }
    }

    /**
     * Runs the full demo configuration workflow: opens the sample config with schema,
     * migrator, and defaults, then saves it to disk. Intended for development or explicit opt-in.
     */
    public static void initializeWithDemoConfig() {
        LOGGER.info("Initializing JAUML demo configuration...");
        runCompatibilityCheck();

        JsonObject defaultData = new JsonObject();
        defaultData.addProperty("version", "2.0");
        defaultData.addProperty("enabled", true);
        defaultData.addProperty("maxPlayers", 20);

        JsonSchema schema = null;
        try {
            String schemaJson = "{" +
                    "\"type\":\"object\"," +
                    "\"properties\":{" +
                        "\"version\":{\"type\":\"string\"}," +
                        "\"enabled\":{\"type\":\"boolean\"}," +
                        "\"maxPlayers\":{\"type\":\"number\"}" +
                    "}," +
                    "\"required\":[\"version\",\"enabled\"]" +
                    "}";
            schema = JsonSchema.parse(schemaJson);
        } catch (Exception e) {
            LOGGER.error("Failed to parse configuration schema", e);
        }

        JsonMigrator migrator = new JsonMigrator();
        migrator.register("1.0", "2.0", oldJson -> {
            LOGGER.info("Migrating configuration shape from 1.0 to 2.0...");
            JsonObject migrated = JsonLib.deepClone(oldJson).getAsJsonObject();
            if (!migrated.has("maxPlayers")) {
                migrated.addProperty("maxPlayers", 20);
            }
            return migrated;
        });

        try {
            ConfigFile configFile = JaumlConfig.open("jauml", "config", schema, migrator, "2.0", defaultData);
            LOGGER.info("JAUML config loaded successfully. Version: {}", configFile.getString("version"));

            boolean enabled = configFile.getBoolean("enabled", true);
            int maxPlayers = configFile.getInt("maxPlayers", 20);
            LOGGER.info("Active configuration state - enabled: {}, maxPlayers: {}", enabled, maxPlayers);

            configFile.save();
            LOGGER.info("JAUML configuration startup initialization completed successfully.");
        } catch (Exception e) {
            LOGGER.error("Non-fatal startup error: recoverable config check encountered an issue", e);
        }
    }

    private static void runCompatibilityCheck() {
        String requiredVersion = "2.1.0";
        if (!JaumlConfig.isCompatible(requiredVersion)) {
            LOGGER.warn("COMPATIBILITY WARNING: App requires JSON library version {}, but current library version is {}.",
                    requiredVersion, JaumlConfig.LIBRARY_VERSION);
        } else {
            LOGGER.info("Compatibility check passed. Library version: {}, Required: {}", JaumlConfig.LIBRARY_VERSION, requiredVersion);
        }
    }

    private static boolean shouldWriteDemoConfig() {
        if (Boolean.getBoolean(DEMO_CONFIG_PROPERTY)) {
            return true;
        }
        try {
            ServiceLoader<PlatformProvider> loader = ServiceLoader.load(PlatformProvider.class);
            for (PlatformProvider provider : loader) {
                return provider.isDevelopmentEnvironment();
            }
        } catch (Exception e) {
            LOGGER.debug("Could not determine development environment status", e);
        }
        return false;
    }
}
