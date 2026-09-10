package tn.naizo.jauml.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for SLF4J loggers used by JAUML utility helpers.
 */
public final class JaumlLogger {

    private static final String DEFAULT_NAME = "Jauml";

    private JaumlLogger() {}

    public static Logger get() {
        return get(DEFAULT_NAME);
    }

    public static Logger get(String name) {
        return LoggerFactory.getLogger(name);
    }
}
