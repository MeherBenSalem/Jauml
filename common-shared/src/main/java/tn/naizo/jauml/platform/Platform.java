package tn.naizo.jauml.platform;

import tn.naizo.jauml.internal.PlatformProviders;
import tn.naizo.jauml.spi.PlatformProvider;
import tn.naizo.jauml.util.Version;
import tn.naizo.jauml.util.VersionRange;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Loader-neutral facts about the running game, so mods can drop their own {@code IPlatformHelper}.
 * <pre>{@code
 * if (Platform.isModLoaded("geckolib", ">=4.4")) { ... }
 * Path worldsDir = Platform.gameDir().resolve("saves");
 * }</pre>
 *
 * @since 3.0.0
 */
public final class Platform {

    private Platform() {}

    private static PlatformProvider provider() {
        return PlatformProviders.get();
    }

    public static Loader loader() {
        return Loader.fromName(provider().getPlatformName());
    }

    public static boolean isFabric() {
        return loader() == Loader.FABRIC;
    }

    public static boolean isNeoForge() {
        return loader() == Loader.NEOFORGE;
    }

    public static boolean isForge() {
        return loader() == Loader.FORGE;
    }

    /**
     * True on the physical client (a game window, including its integrated server).
     */
    public static boolean isClient() {
        return provider().isPhysicalClient();
    }

    /**
     * True on a dedicated server.
     */
    public static boolean isDedicatedServer() {
        return !provider().isPhysicalClient();
    }

    /**
     * True in a development environment. Fail-safe: false when no loader module is present or the loader cannot say.
     */
    public static boolean isDevelopment() {
        try {
            return PlatformProviders.find().map(PlatformProvider::isDevelopmentEnvironment).orElse(false);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public static Path gameDir() {
        return provider().getGameDirectory();
    }

    public static Path configDir() {
        return provider().getConfigDirectory();
    }

    public static String minecraftVersion() {
        return provider().getMinecraftVersion();
    }

    /**
     * Checks the running Minecraft version against a range such as {@code ">=26.2"} or {@code "[1.20.1]"}.
     */
    public static boolean isMinecraft(String range) {
        return VersionRange.parse(range).contains(minecraftVersion());
    }

    public static boolean isModLoaded(String modId) {
        return provider().isModLoaded(modId);
    }

    /**
     * Checks that a mod is loaded and its version is inside a range, for optional integrations that need a minimum
     * version. See {@link VersionRange#parse(String)} for the range syntax.
     */
    public static boolean isModLoaded(String modId, String versionRange) {
        if (!isModLoaded(modId)) {
            return false;
        }
        Optional<Version> version = modVersion(modId);
        return version.isPresent() && VersionRange.parse(versionRange).contains(version.get());
    }

    public static Optional<String> modVersionString(String modId) {
        return provider().getModVersion(modId);
    }

    /**
     * Gets a loaded mod's parsed version, or empty when the mod is absent or its version does not parse.
     */
    public static Optional<Version> modVersion(String modId) {
        return provider().getModVersion(modId).map(Version::tryParse);
    }

    public static Optional<String> modName(String modId) {
        return provider().getModName(modId);
    }

    public static Collection<String> loadedMods() {
        return provider().getLoadedModIds();
    }
}
