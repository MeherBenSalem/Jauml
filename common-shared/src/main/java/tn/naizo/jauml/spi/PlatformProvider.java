package tn.naizo.jauml.spi;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public interface PlatformProvider {

    /**
     * Gets the name of the current platform.
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Gets the configuration directory.
     *
     * @return The configuration directory path.
     */
    Path getConfigDirectory();

    /**
     * Gets the game (instance) directory that holds {@code config/}, {@code mods/} and saves.
     *
     * @since 3.0.0
     */
    default Path getGameDirectory() {
        Path parent = getConfigDirectory().toAbsolutePath().getParent();
        return parent != null ? parent : getConfigDirectory();
    }

    /**
     * Checks whether this is the physical client (a game window), as opposed to a dedicated server.
     *
     * @since 3.0.0
     */
    default boolean isPhysicalClient() {
        return false;
    }

    /**
     * Gets the version a loaded mod declares in its metadata.
     *
     * @since 3.0.0
     */
    default Optional<String> getModVersion(String modId) {
        return Optional.empty();
    }

    /**
     * Gets the display name a loaded mod declares in its metadata.
     *
     * @since 3.0.0
     */
    default Optional<String> getModName(String modId) {
        return Optional.empty();
    }

    /**
     * Gets the ids of every loaded mod.
     *
     * @since 3.0.0
     */
    default Collection<String> getLoadedModIds() {
        return Collections.emptyList();
    }

    /**
     * Gets the running Minecraft version, such as {@code 1.21.1} or {@code 26.3}.
     *
     * @since 3.0.0
     */
    default String getMinecraftVersion() {
        return getModVersion("minecraft").orElse("unknown");
    }
}
