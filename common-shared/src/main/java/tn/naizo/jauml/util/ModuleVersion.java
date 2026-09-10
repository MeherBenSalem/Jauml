package tn.naizo.jauml.util;

/**
 * Describes a versioned optional utility module.
 * <p>
 * Future JAUML utility modules should declare a {@code ModuleVersion} contract so
 * dependents can verify compatibility at startup.
 */
public record ModuleVersion(String name, String version) {

    /**
     * Checks whether this module's version satisfies the required version.
     * Uses the same semver rules as {@link tn.naizo.jauml.api.JaumlConfig#isCompatible}:
     * major versions must match and the minor version must be greater than or equal.
     */
    public boolean isCompatibleWith(String required) {
        if (required == null || required.trim().isEmpty() || version == null || version.trim().isEmpty()) {
            return false;
        }
        try {
            String[] currentParts = version.split("\\.");
            String[] reqParts = required.split("\\.");
            int curMajor = Integer.parseInt(currentParts[0]);
            int curMinor = Integer.parseInt(currentParts[1]);
            int reqMajor = Integer.parseInt(reqParts[0]);
            int reqMinor = Integer.parseInt(reqParts[1]);

            if (curMajor != reqMajor) {
                return false;
            }
            return curMinor >= reqMinor;
        } catch (Exception e) {
            return false;
        }
    }
}
