package tn.naizo.jauml.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Text written as {@code //} lines above a {@link TypedConfig} field, or at the top of the file when placed on the
 * config class. Use {@code \n} for several lines.
 *
 * @since 3.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Comment {
    String value();
}
