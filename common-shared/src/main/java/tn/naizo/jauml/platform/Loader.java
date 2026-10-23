package tn.naizo.jauml.platform;

/**
 * The mod loader JAUML is running on.
 *
 * @since 3.0.0
 */
public enum Loader {
    FABRIC("Fabric"),
    NEOFORGE("NeoForge"),
    FORGE("Forge"),
    UNKNOWN("Unknown");

    private final String displayName;

    Loader(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Maps a platform name such as {@code "NeoForge"} to its loader, or {@link #UNKNOWN}.
     */
    public static Loader fromName(String name) {
        if (name != null) {
            for (Loader loader : values()) {
                if (loader.displayName.equalsIgnoreCase(name.trim())) {
                    return loader;
                }
            }
        }
        return UNKNOWN;
    }
}
