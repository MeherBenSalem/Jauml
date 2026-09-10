package tn.naizo.jauml.util;

import org.slf4j.Logger;
import tn.naizo.jauml.api.JaumlConfig;

/**
 * Lightweight helpers for common JAUML startup patterns.
 */
public final class Lifecycle {

    private Lifecycle() {}

    /**
     * Logs a compatibility warning when the installed library does not satisfy the required version.
     */
    public static void requireCompatibleLibrary(String requiredVersion) {
        if (!JaumlConfig.isCompatible(requiredVersion)) {
            Logger logger = JaumlLogger.get();
            logger.warn("COMPATIBILITY WARNING: App requires JAUML library version {}, but current library version is {}.",
                    requiredVersion, JaumlConfig.LIBRARY_VERSION);
        }
    }

    /**
     * Runs a startup action and logs any exception without rethrowing.
     */
    public static void onStartup(String modId, Runnable action) {
        Logger logger = JaumlLogger.get(modId);
        try {
            action.run();
        } catch (Exception e) {
            logger.error("Non-fatal startup error in {}", modId, e);
        }
    }
}
