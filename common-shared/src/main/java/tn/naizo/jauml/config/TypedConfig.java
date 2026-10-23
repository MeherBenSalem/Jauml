package tn.naizo.jauml.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.naizo.jauml.api.JsonException;
import tn.naizo.jauml.api.JsonLib;
import tn.naizo.jauml.api.JsonMigrator;
import tn.naizo.jauml.event.Event;
import tn.naizo.jauml.internal.PathValidator;
import tn.naizo.jauml.internal.PlatformProviders;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A config file bound to a plain Java class: public fields are the options, field initializers are the defaults.
 * <pre>{@code
 * @Comment("My Mod settings")
 * public class MyConfig {
 *     @Comment("Maximum boss level")
 *     @Range(min = 1, max = 100)
 *     public int maxLevel = 50;
 *     public List<String> blockedDimensions = new ArrayList<>();
 *     public Drops drops = new Drops();           // nested classes become nested objects
 * }
 *
 * public static final TypedConfig<MyConfig> CONFIG = JaumlConfig.bind("my_mod", "common", MyConfig.class);
 * int max = CONFIG.get().maxLevel;
 * }</pre>
 * On load, keys missing from the file get their defaults, values of the wrong type are coerced or reset,
 * {@link Range} bounds are enforced, and the file is rewritten when any of that changed it. {@link Comment} text is
 * written as {@code //} lines; the reader accepts comments, so the file stays hand-editable.
 * A file that cannot be parsed at startup is moved to {@code <name>.json.bak} and replaced with defaults;
 * a file that breaks during {@link #reload()} keeps the previous values instead.
 *
 * @param <T> the config class; it needs a no-argument constructor
 * @since 3.0.0
 */
public final class TypedConfig<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlConfig");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().serializeSpecialFloatingPointValues().create();
    private static final Map<Path, TypedConfig<?>> BOUND = new ConcurrentHashMap<>();

    private final Path path;
    private final Class<T> type;
    private final JsonMigrator migrator;
    private final String targetVersion;
    private final boolean comments;
    private final Object lock = new Object();
    private final Event<Consumer<T>> changed;

    private volatile T current;
    private JsonObject currentJson;
    private long lastKnownSize = -1;
    private long lastKnownMtime = -1;

    private TypedConfig(Path path, Class<T> type, JsonMigrator migrator, String targetVersion, boolean comments) {
        this.path = path;
        this.type = type;
        this.migrator = migrator;
        this.targetVersion = targetVersion;
        this.comments = comments;
        this.changed = Event.create(listeners -> value -> {
            for (Consumer<T> listener : listeners) {
                try {
                    listener.accept(value);
                } catch (RuntimeException e) {
                    LOGGER.error("Config change listener for {} failed", path, e);
                }
            }
        });
    }

    /**
     * Binds {@code <config dir>/<subdirectory>/<fileName>.json} to a class. Binding the same file twice returns
     * the same instance.
     */
    public static <T> TypedConfig<T> bind(String subdirectory, String fileName, Class<T> type) {
        return builder(type).file(subdirectory, fileName).build();
    }

    public static <T> Builder<T> builder(Class<T> type) {
        return new Builder<>(type);
    }

    /**
     * Returns the current values. A reload swaps in a new instance, so read through {@code get()} each time
     * instead of keeping the object.
     */
    public T get() {
        return current;
    }

    /**
     * Returns a fresh instance holding the defaults.
     */
    public T defaults() {
        return newInstance(type);
    }

    public Path path() {
        return path;
    }

    public Class<T> type() {
        return type;
    }

    /**
     * Runs a listener with the new values whenever a reload, {@link #update} or {@link #resetToDefaults()}
     * changes them.
     */
    public void addListener(Consumer<T> listener) {
        changed.register(listener);
    }

    public boolean removeListener(Consumer<T> listener) {
        return changed.unregister(listener);
    }

    /**
     * Writes the current values (including direct edits to {@link #get()}) to disk, enforcing {@link Range}s first.
     */
    public void save() {
        synchronized (lock) {
            repair(current, newInstance(type), "", new ArrayList<>());
            currentJson = GSON.toJsonTree(current).getAsJsonObject();
            write();
        }
    }

    /**
     * Edits the values, saves them and notifies listeners.
     */
    public void update(Consumer<T> edit) {
        T value;
        synchronized (lock) {
            edit.accept(current);
            repair(current, newInstance(type), "", new ArrayList<>());
            currentJson = GSON.toJsonTree(current).getAsJsonObject();
            write();
            value = current;
        }
        changed.invoker().accept(value);
    }

    public void resetToDefaults() {
        T value;
        synchronized (lock) {
            current = newInstance(type);
            currentJson = GSON.toJsonTree(current).getAsJsonObject();
            write();
            value = current;
        }
        changed.invoker().accept(value);
    }

    /**
     * Reads the file again. If it no longer parses, the previous values stay in place and the error is logged.
     *
     * @return true if the file was read successfully
     */
    public boolean reload() {
        T value;
        boolean valuesChanged;
        synchronized (lock) {
            if (!Files.exists(path)) {
                write();
                return false;
            }
            try {
                Loaded<T> loaded = read();
                valuesChanged = !loaded.canonical.equals(currentJson);
                current = loaded.value;
                currentJson = loaded.canonical;
                if (loaded.rewrite) {
                    write();
                } else {
                    updateSnapshot();
                }
                value = current;
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Could not reload config {}; keeping the previous values. {}", path, e.getMessage());
                updateSnapshot();
                return false;
            }
        }
        if (valuesChanged) {
            changed.invoker().accept(value);
        }
        return true;
    }

    /**
     * Reloads only if the file's size or modification time changed since it was last read or written, so it is
     * cheap enough to call on a timer (for example {@code Scheduler.runRepeating(100, 100, CONFIG::reloadIfChanged)}).
     *
     * @return true if the file changed and was read successfully
     */
    public boolean reloadIfChanged() {
        synchronized (lock) {
            try {
                if (Files.exists(path)
                        && Files.size(path) == lastKnownSize
                        && Files.getLastModifiedTime(path).toMillis() == lastKnownMtime) {
                    return false;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return reload();
    }

    private void loadInitial() {
        synchronized (lock) {
            if (!Files.exists(path)) {
                current = newInstance(type);
                currentJson = GSON.toJsonTree(current).getAsJsonObject();
                write();
                return;
            }
            try {
                Loaded<T> loaded = read();
                current = loaded.value;
                currentJson = loaded.canonical;
                if (loaded.rewrite) {
                    write();
                } else {
                    updateSnapshot();
                }
            } catch (IOException | RuntimeException e) {
                Path backup = path.resolveSibling(path.getFileName() + ".bak");
                LOGGER.error("Could not read config {} ({}). Moved it to {} and wrote defaults.", path, e.getMessage(), backup);
                try {
                    Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException moveError) {
                    LOGGER.error("Failed to back up config {}", path, moveError);
                }
                current = newInstance(type);
                currentJson = GSON.toJsonTree(current).getAsJsonObject();
                write();
            }
        }
    }

    private Loaded<T> read() throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement parsed = text.trim().isEmpty() ? new JsonObject() : parseLenient(text);
        if (!parsed.isJsonObject()) {
            throw new JsonException("Config root must be a JSON object");
        }
        JsonObject data = parsed.getAsJsonObject();
        if (migrator != null && targetVersion != null) {
            data = migrator.migrate(data, targetVersion);
        }

        T defaults = newInstance(type);
        JsonObject normalized = normalizeObject(data, GSON.toJsonTree(defaults).getAsJsonObject(), type);
        T value = GSON.fromJson(normalized, type);
        List<String> notes = new ArrayList<>();
        repair(value, defaults, "", notes);
        for (String note : notes) {
            LOGGER.warn("Config {}: {}", path.getFileName(), note);
        }

        JsonObject canonical = GSON.toJsonTree(value).getAsJsonObject();
        return new Loaded<>(value, canonical, !canonical.equals(parsed));
    }

    // Lenient so hand-edited files may carry // and /* */ comments anywhere, including after the closing brace.
    @SuppressWarnings("deprecation")
    private static JsonElement parseLenient(String text) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(text));
        reader.setLenient(true);
        JsonElement parsed = JsonParser.parseReader(reader);
        if (reader.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonException("Unexpected content after the closing brace");
        }
        return parsed;
    }

    private void write() {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String content = comments
                    ? render(currentJson, type)
                    : GSON.newBuilder().setPrettyPrinting().create().toJson(currentJson) + System.lineSeparator();
            Path temp = Files.createTempFile(parent != null ? parent : path.toAbsolutePath().getParent(), ".jauml-", ".tmp");
            try {
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            updateSnapshot();
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: {}", path, e);
        }
    }

    private void updateSnapshot() {
        try {
            lastKnownSize = Files.size(path);
            lastKnownMtime = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            lastKnownSize = -1;
            lastKnownMtime = -1;
        }
    }

    // ==================== normalization and repair ====================

    private static JsonObject normalizeObject(JsonObject config, JsonObject defaults, Class<?> type) {
        JsonObject out = new JsonObject();
        for (Map.Entry<String, Field> entry : fields(type).entrySet()) {
            String key = entry.getKey();
            Field field = entry.getValue();
            JsonElement fallback = defaults.get(key);
            JsonElement given = config.get(key);
            JsonElement value;
            if (given == null || given.isJsonNull()) {
                value = fallback;
            } else if (Map.class.isAssignableFrom(field.getType())) {
                // Map entries belong to the user: never re-add default keys they removed.
                value = given.isJsonObject() ? given : fallback;
            } else if (isNested(field.getType()) && given.isJsonObject() && fallback != null && fallback.isJsonObject()) {
                value = normalizeObject(given.getAsJsonObject(), fallback.getAsJsonObject(), field.getType());
            } else if (fallback == null) {
                value = given;
            } else {
                value = JsonLib.normalize(given, fallback);
            }
            if (value != null) {
                out.add(key, JsonLib.deepClone(value));
            }
        }
        return out;
    }

    private static void repair(Object target, Object defaults, String prefix, List<String> notes) {
        for (Map.Entry<String, Field> entry : fields(target.getClass()).entrySet()) {
            Field field = entry.getValue();
            String name = prefix + entry.getKey();
            try {
                Object value = field.get(target);
                Object fallback = field.get(defaults);
                if (value == null && fallback != null) {
                    field.set(target, fallback);
                    notes.add(name + " was missing or invalid; using the default");
                    continue;
                }
                Range range = field.getAnnotation(Range.class);
                if (range != null && value instanceof Number) {
                    double number = ((Number) value).doubleValue();
                    double clamped = Math.max(range.min(), Math.min(range.max(), number));
                    if (clamped != number) {
                        field.set(target, toFieldType(clamped, field.getType()));
                        notes.add(name + " = " + value + " is outside " + describe(range) + "; clamped to " + field.get(target));
                    }
                }
                if (value != null && fallback != null && isNested(field.getType())) {
                    repair(value, fallback, name + ".", notes);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access config field " + name, e);
            }
        }
    }

    private static Object toFieldType(double value, Class<?> fieldType) {
        if (fieldType == int.class || fieldType == Integer.class) {
            return (int) value;
        }
        if (fieldType == long.class || fieldType == Long.class) {
            return (long) value;
        }
        if (fieldType == float.class || fieldType == Float.class) {
            return (float) value;
        }
        if (fieldType == short.class || fieldType == Short.class) {
            return (short) value;
        }
        if (fieldType == byte.class || fieldType == Byte.class) {
            return (byte) value;
        }
        return value;
    }

    // ==================== rendering ====================

    private static String render(JsonObject root, Class<?> type) {
        StringBuilder out = new StringBuilder();
        Comment header = type.getAnnotation(Comment.class);
        if (header != null) {
            for (String line : header.value().split("\n", -1)) {
                out.append("// ").append(line).append('\n');
            }
        }
        renderObject(out, root, type, newInstanceOrNull(type), 0);
        return out.append('\n').toString();
    }

    private static void renderObject(StringBuilder out, JsonObject object, Class<?> type, Object defaults, int indent) {
        if (object.size() == 0) {
            out.append("{}");
            return;
        }
        Map<String, Field> fields = type != null ? fields(type) : Map.of();
        String pad = " ".repeat(indent + 2);
        out.append("{\n");
        int index = 0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Field field = fields.get(entry.getKey());
            Object fieldDefault = null;
            if (field != null) {
                for (String line : commentLines(field, defaults)) {
                    out.append(pad).append("// ").append(line).append('\n');
                }
                fieldDefault = readOrNull(field, defaults);
            }
            out.append(pad).append(GSON.toJson(entry.getKey())).append(": ");
            Class<?> nested = field != null && isNested(field.getType()) ? field.getType() : null;
            renderValue(out, entry.getValue(), nested, fieldDefault, indent + 2);
            if (++index < object.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append(" ".repeat(indent)).append('}');
    }

    private static void renderValue(StringBuilder out, JsonElement value, Class<?> nestedType, Object defaults, int indent) {
        if (value.isJsonObject()) {
            renderObject(out, value.getAsJsonObject(), nestedType, defaults, indent);
        } else if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            StringBuilder inlineItems = new StringBuilder("[");
            boolean primitives = true;
            for (JsonElement item : array) {
                primitives &= item.isJsonPrimitive() || item.isJsonNull();
                inlineItems.append(inlineItems.length() > 1 ? ", " : "").append(GSON.toJson(item));
            }
            String inline = inlineItems.append(']').toString();
            if (array.size() == 0 || (primitives && inline.length() <= 80)) {
                out.append(inline);
                return;
            }
            String pad = " ".repeat(indent + 2);
            out.append("[\n");
            for (int i = 0; i < array.size(); i++) {
                out.append(pad);
                renderValue(out, array.get(i), null, null, indent + 2);
                if (i < array.size() - 1) {
                    out.append(',');
                }
                out.append('\n');
            }
            out.append(" ".repeat(indent)).append(']');
        } else {
            out.append(GSON.toJson(value));
        }
    }

    private static List<String> commentLines(Field field, Object defaults) {
        List<String> lines = new ArrayList<>();
        Comment comment = field.getAnnotation(Comment.class);
        if (comment != null) {
            for (String line : comment.value().split("\n", -1)) {
                lines.add(line);
            }
        }
        Range range = field.getAnnotation(Range.class);
        if (range != null) {
            lines.add("Range: " + describe(range));
        }
        if (field.getType().isEnum()) {
            StringBuilder allowed = new StringBuilder("Allowed: ");
            Object[] constants = field.getType().getEnumConstants();
            for (int i = 0; i < constants.length; i++) {
                allowed.append(i == 0 ? "" : ", ").append(GSON.toJsonTree(constants[i]).getAsString());
            }
            lines.add(allowed.toString());
        }
        Object fallback = readOrNull(field, defaults);
        if (comment != null && fallback != null && !isNested(field.getType())) {
            JsonElement json = GSON.toJsonTree(fallback);
            if (json.isJsonPrimitive()) {
                lines.add("Default: " + GSON.toJson(json));
            }
        }
        return lines;
    }

    private static String describe(Range range) {
        boolean hasMin = range.min() != Double.NEGATIVE_INFINITY;
        boolean hasMax = range.max() != Double.POSITIVE_INFINITY;
        if (hasMin && hasMax) {
            return format(range.min()) + " to " + format(range.max());
        }
        if (hasMin) {
            return ">= " + format(range.min());
        }
        if (hasMax) {
            return "<= " + format(range.max());
        }
        return "any";
    }

    private static String format(double value) {
        return value == Math.rint(value) && Math.abs(value) < 1e15 ? String.valueOf((long) value) : String.valueOf(value);
    }

    // ==================== reflection helpers ====================

    private static Map<String, Field> fields(Class<?> type) {
        Map<String, Field> fields = new LinkedHashMap<>();
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            hierarchy.add(0, c);
        }
        for (Class<?> c : hierarchy) {
            for (Field field : c.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                field.trySetAccessible();
                SerializedName name = field.getAnnotation(SerializedName.class);
                fields.put(name != null ? name.value() : field.getName(), field);
            }
        }
        return fields;
    }

    private static boolean isNested(Class<?> type) {
        return !type.isPrimitive()
                && !type.isEnum()
                && !type.isArray()
                && !type.isInterface()
                && !Number.class.isAssignableFrom(type)
                && !Collection.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type)
                && !JsonElement.class.isAssignableFrom(type)
                && !type.getName().startsWith("java.");
    }

    private static <C> C newInstance(Class<C> type) {
        try {
            Constructor<C> constructor = type.getDeclaredConstructor();
            constructor.trySetAccessible();
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Config class " + type.getName() + " needs a no-argument constructor", e);
        }
    }

    private static Object newInstanceOrNull(Class<?> type) {
        try {
            return newInstance(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Object readOrNull(Field field, Object owner) {
        if (owner == null) {
            return null;
        }
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static final class Loaded<T> {
        final T value;
        final JsonObject canonical;
        final boolean rewrite;

        Loaded(T value, JsonObject canonical, boolean rewrite) {
            this.value = value;
            this.canonical = canonical;
            this.rewrite = rewrite;
        }
    }

    /**
     * Options for {@link TypedConfig}; finish with {@link #build()}.
     */
    public static final class Builder<T> {

        private final Class<T> type;
        private Path path;
        private JsonMigrator migrator;
        private String targetVersion;
        private boolean comments = true;

        private Builder(Class<T> type) {
            if (type == null) {
                throw new IllegalArgumentException("Config type cannot be null");
            }
            this.type = type;
        }

        /**
         * Uses {@code <config dir>/<subdirectory>/<fileName>.json}.
         */
        public Builder<T> file(String subdirectory, String fileName) {
            this.path = PathValidator.resolveSafe(PlatformProviders.get().getConfigDirectory(), subdirectory, fileName);
            return this;
        }

        /**
         * Uses an explicit file path, for configs outside the config directory (world folders, tests).
         */
        public Builder<T> path(Path file) {
            this.path = file.toAbsolutePath().normalize();
            return this;
        }

        /**
         * Runs {@link JsonMigrator} migrations on the raw file before binding; the class should declare the version
         * field the migrator reads.
         */
        public Builder<T> migrator(JsonMigrator migrator, String targetVersion) {
            this.migrator = migrator;
            this.targetVersion = targetVersion;
            return this;
        }

        /**
         * Turns {@code //} comment output off, writing plain JSON instead. Default: on.
         */
        public Builder<T> comments(boolean comments) {
            this.comments = comments;
            return this;
        }

        @SuppressWarnings("unchecked")
        public TypedConfig<T> build() {
            if (path == null) {
                throw new IllegalStateException("Call file(...) or path(...) before build()");
            }
            newInstance(type); // fail fast on classes without a no-argument constructor
            TypedConfig<?> config = BOUND.computeIfAbsent(path, p -> {
                TypedConfig<T> created = new TypedConfig<>(p, type, migrator, targetVersion, comments);
                created.loadInitial();
                return created;
            });
            if (config.type != type) {
                throw new IllegalStateException("Config " + path + " is already bound to " + config.type.getName());
            }
            return (TypedConfig<T>) config;
        }
    }
}
