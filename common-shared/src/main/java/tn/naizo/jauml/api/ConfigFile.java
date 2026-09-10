package tn.naizo.jauml.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigFile {

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path filePath;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private JsonObject rootData;

    private JsonSchema schema;
    private JsonMigrator migrator;
    private String targetVersion;
    private JsonObject defaultData;

    private int backupRotation = 1;
    private boolean stableSave = false;
    private long lastKnownSize = -1;
    private long lastKnownMtime = -1;

    ConfigFile(Path filePath) {
        this.filePath = filePath;
        this.rootData = new JsonObject();
        loadFromDisk();
        updateFileSnapshot();
    }

    /**
     * Loads or reloads the configuration data from disk.
     * If the file does not exist, an empty configuration is maintained in memory.
     */
    public void reload() {
        lock.writeLock().lock();
        try {
            loadFromDisk();
            updateFileSnapshot();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Reloads from disk only if the file exists and its size or modification time has changed
     * since the last load or save.
     *
     * @return true if the file was reloaded, false if unchanged or missing
     */
    public boolean reloadIfChanged() {
        lock.writeLock().lock();
        try {
            if (!Files.exists(filePath)) {
                return false;
            }
            long size = Files.size(filePath);
            long mtime = Files.getLastModifiedTime(filePath).toMillis();
            if (size == lastKnownSize && mtime == lastKnownMtime) {
                return false;
            }
            loadFromDisk();
            updateFileSnapshot();
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to check or reload config file: " + filePath, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void updateFileSnapshot() {
        try {
            if (Files.exists(filePath)) {
                lastKnownSize = Files.size(filePath);
                lastKnownMtime = Files.getLastModifiedTime(filePath).toMillis();
            } else {
                lastKnownSize = -1;
                lastKnownMtime = -1;
            }
        } catch (IOException e) {
            lastKnownSize = -1;
            lastKnownMtime = -1;
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(filePath)) {
            this.rootData = defaultData != null ? JsonLib.deepClone(defaultData).getAsJsonObject() : new JsonObject();
            return;
        }

        boolean corrupted = false;
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                LOGGER.warn("Config file at {} is empty.", filePath);
                this.rootData = defaultData != null ? JsonLib.deepClone(defaultData).getAsJsonObject() : new JsonObject();
                save();
                return;
            }

            JsonElement parsed = JsonLib.strictParse(content);
            if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();

                // 1. Migration
                if (migrator != null && targetVersion != null) {
                    try {
                        obj = migrator.migrate(obj, targetVersion);
                    } catch (Exception e) {
                        LOGGER.error("Migration failed for config file " + filePath + ". Reverting to default config.", e);
                        corrupted = true;
                    }
                }

                // 2. Normalization / Default Merge
                if (!corrupted && defaultData != null) {
                    obj = JsonLib.normalize(obj, defaultData).getAsJsonObject();
                }

                // 3. Schema Validation
                if (!corrupted && schema != null) {
                    try {
                        schema.validate(obj);
                    } catch (Exception e) {
                        LOGGER.error("Schema validation failed for config file " + filePath + ": " + e.getMessage(), e);
                        if (defaultData != null) {
                            LOGGER.warn("Attempting to normalize config to schema template.");
                            obj = JsonLib.normalize(obj, defaultData).getAsJsonObject();
                            schema.validate(obj); // validation check after normalization
                        } else {
                            corrupted = true;
                        }
                    }
                }

                if (!corrupted) {
                    this.rootData = obj;
                    return;
                }
            } else {
                LOGGER.warn("Config file at {} did not contain a valid JSON object.", filePath);
                corrupted = true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load or parse config file: " + filePath, e);
            corrupted = true;
        }

        if (corrupted) {
            handleCorruptedConfig();
        }
    }

    private void handleCorruptedConfig() {
        try {
            if (Files.exists(filePath)) {
                rotateBackups(filePath);
                LOGGER.warn("Corrupted config file backed up before reset: {}", filePath);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create backup of corrupted config file: " + filePath, e);
        }

        // Reset to default config in memory
        this.rootData = defaultData != null ? JsonLib.deepClone(defaultData).getAsJsonObject() : new JsonObject();
        // Save clean copy so startup doesn't fail
        save();
    }

    private void rotateBackups(Path sourceFile) throws IOException {
        String baseName = sourceFile.getFileName().toString();
        Path parent = sourceFile.getParent();
        Path bakPath = parent.resolve(baseName + ".bak");

        if (backupRotation <= 1) {
            Files.deleteIfExists(bakPath);
            if (Files.exists(sourceFile)) {
                Files.move(sourceFile, bakPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }

        int maxGen = backupRotation - 1;
        Path oldest = parent.resolve(baseName + ".bak." + maxGen);
        Files.deleteIfExists(oldest);
        for (int i = maxGen - 1; i >= 1; i--) {
            Path from = parent.resolve(baseName + ".bak." + i);
            Path to = parent.resolve(baseName + ".bak." + (i + 1));
            if (Files.exists(from)) {
                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (Files.exists(bakPath)) {
            Files.move(bakPath, parent.resolve(baseName + ".bak.1"), StandardCopyOption.REPLACE_EXISTING);
        }
        if (Files.exists(sourceFile)) {
            Files.move(sourceFile, bakPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }


    /**
     * Writes the current in-memory configuration back to disk atomically.
     */
    public void save() {
        lock.readLock().lock();
        JsonObject snapshot;
        boolean useStable;
        try {
            snapshot = JsonLib.deepClone(rootData).getAsJsonObject();
            useStable = stableSave;
        } finally {
            lock.readLock().unlock();
        }

        try {
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            String content = useStable
                    ? JsonLib.stableStringify(snapshot)
                    : GSON.toJson(snapshot);

            Path tempFile = Files.createTempFile(parent != null ? parent : filePath.toAbsolutePath().getParent(), ".jauml-", ".tmp");
            try {
                Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                try {
                    Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempFile);
            }
            updateFileSnapshot();
        } catch (IOException e) {
            LOGGER.error("Failed to save config file: " + filePath, e);
        }
    }

    /**
     * Checks if the configuration file physically exists on disk.
     */
    public boolean exists() {
        return Files.exists(filePath);
    }

    /**
     * Deletes the configuration file from disk and clears the in-memory cache.
     * @return true if successful, false otherwise
     */
    public boolean delete() {
        lock.writeLock().lock();
        try {
            this.rootData = new JsonObject();
            boolean deleted = Files.deleteIfExists(filePath);
            lastKnownSize = -1;
            lastKnownMtime = -1;
            return deleted;
        } catch (IOException e) {
            LOGGER.error("Failed to delete config file: " + filePath, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the safe, normalized Path of this config file.
     */
    public Path path() {
        return filePath;
    }

    public ConfigFile configure(JsonSchema schema, JsonMigrator migrator, String targetVersion, JsonObject defaultData) {
        lock.writeLock().lock();
        try {
            this.schema = schema;
            this.migrator = migrator;
            this.targetVersion = targetVersion;
            this.defaultData = defaultData;
            loadFromDisk();
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile setSchema(JsonSchema schema) {
        lock.writeLock().lock();
        try {
            this.schema = schema;
            loadFromDisk();
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile setMigrator(JsonMigrator migrator, String targetVersion) {
        lock.writeLock().lock();
        try {
            this.migrator = migrator;
            this.targetVersion = targetVersion;
            loadFromDisk();
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile setDefaultData(JsonObject defaultData) {
        lock.writeLock().lock();
        try {
            this.defaultData = defaultData;
            loadFromDisk();
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets how many backup generations to keep when rotating corrupted config backups.
     * Values of 0 or 1 retain the single {@code .bak} file behavior; values greater than 1
     * rotate through {@code .bak}, {@code .bak.1}, and so on.
     */
    public ConfigFile setBackupRotation(int generations) {
        lock.writeLock().lock();
        try {
            this.backupRotation = Math.max(0, generations);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * When true, saves use {@link JsonLib#stableStringify} for deterministic key ordering.
     * Default is false (standard pretty Gson output).
     */
    public ConfigFile setStableSave(boolean stable) {
        lock.writeLock().lock();
        try {
            this.stableSave = stable;
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a deep clone of the root JSON object. Mutations to the returned object
     * do not affect the in-memory config until written back via {@link #setByPath} or setters.
     */
    public JsonObject asJsonObject() {
        lock.readLock().lock();
        try {
            return JsonLib.deepClone(rootData).getAsJsonObject();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Alias for {@link #asJsonObject()}.
     */
    public JsonObject getRoot() {
        return asJsonObject();
    }

    /**
     * Returns a deep clone of the value at the given path, if present.
     * Mutations to the returned element do not affect the in-memory config.
     */
    public Optional<JsonElement> getByPath(String path) {
        lock.readLock().lock();
        try {
            return JsonLib.getByPath(rootData, path).map(JsonLib::deepClone);
        } finally {
            lock.readLock().unlock();
        }
    }

    public ConfigFile setByPath(String path, JsonElement value) {
        lock.writeLock().lock();
        try {
            JsonLib.setByPath(rootData, path, value);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getStringByPath(String path, String defaultValue) {
        return getByPath(path)
                .filter(el -> el.isJsonPrimitive() && el.getAsJsonPrimitive().isString())
                .map(JsonElement::getAsString)
                .orElse(defaultValue);
    }

    public String getStringByPath(String path) {
        return getStringByPath(path, null);
    }

    public int getIntByPath(String path, int defaultValue) {
        return getByPath(path)
                .filter(el -> el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber())
                .map(JsonElement::getAsInt)
                .orElse(defaultValue);
    }

    public double getDoubleByPath(String path, double defaultValue) {
        return getByPath(path)
                .filter(el -> el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber())
                .map(JsonElement::getAsDouble)
                .orElse(defaultValue);
    }

    public boolean getBooleanByPath(String path, boolean defaultValue) {
        return getByPath(path)
                .filter(el -> el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean())
                .map(JsonElement::getAsBoolean)
                .orElse(defaultValue);
    }

    // ==================== GETTERS ====================

    public String getString(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                return el.getAsString();
            }
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public int getInt(String key, int defaultValue) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return el.getAsInt();
            }
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    public OptionalInt getInt(String key) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return OptionalInt.of(el.getAsInt());
            }
            return OptionalInt.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public long getLong(String key, long defaultValue) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return el.getAsLong();
            }
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    public OptionalLong getLong(String key) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return OptionalLong.of(el.getAsLong());
            }
            return OptionalLong.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getDouble(String key, double defaultValue) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return el.getAsDouble();
            }
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    public OptionalDouble getDouble(String key) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
                return OptionalDouble.of(el.getAsDouble());
            }
            return OptionalDouble.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
                return el.getAsBoolean();
            }
            return defaultValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Boolean getBoolean(String key) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
                return el.getAsBoolean();
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== SETTERS ====================

    public ConfigFile set(String key, String value) {
        lock.writeLock().lock();
        try {
            if (value == null) {
                rootData.remove(key);
            } else {
                rootData.addProperty(key, value);
            }
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile set(String key, int value) {
        lock.writeLock().lock();
        try {
            rootData.addProperty(key, value);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile set(String key, long value) {
        lock.writeLock().lock();
        try {
            rootData.addProperty(key, value);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile set(String key, double value) {
        lock.writeLock().lock();
        try {
            rootData.addProperty(key, value);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigFile set(String key, boolean value) {
        lock.writeLock().lock();
        try {
            rootData.addProperty(key, value);
            return this;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== ARRAY OPERATIONS ====================

    /**
     * Gets a list of string elements from a JSON array under the given key.
     */
    public List<String> getStringList(String key) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonArray()) {
                JsonArray array = el.getAsJsonArray();
                List<String> list = new ArrayList<>(array.size());
                for (JsonElement item : array) {
                    if (item.isJsonPrimitive()) {
                        list.add(item.getAsString());
                    } else {
                        list.add(item.toString());
                    }
                }
                return list;
            }
            return Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Adds a value to a JSON array under the given key.
     * Prevents duplicate strings.
     * @return true if the item was added, false if it already existed
     */
    public boolean addToList(String key, String value) {
        lock.writeLock().lock();
        try {
            JsonArray array;
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonArray()) {
                array = el.getAsJsonArray();
            } else {
                array = new JsonArray();
                rootData.add(key, array);
            }

            // Duplicate check (exact match)
            for (JsonElement item : array) {
                if (item.isJsonPrimitive() && item.getAsString().equals(value)) {
                    return false;
                }
            }

            array.add(value);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a value from a JSON array under the given key.
     * @return true if the item was removed, false if not found
     */
    public boolean removeFromList(String key, String value) {
        lock.writeLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el == null || !el.isJsonArray()) {
                return false;
            }

            JsonArray array = el.getAsJsonArray();
            JsonArray updated = new JsonArray();
            boolean removed = false;

            for (JsonElement item : array) {
                if (item.isJsonPrimitive() && item.getAsString().equals(value)) {
                    removed = true;
                    continue;
                }
                updated.add(item);
            }

            if (removed) {
                rootData.add(key, updated);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if a JSON array contains the given value (EXACT match).
     */
    public boolean listContains(String key, String value) {
        lock.readLock().lock();
        try {
            JsonElement el = rootData.get(key);
            if (el != null && el.isJsonArray()) {
                for (JsonElement item : el.getAsJsonArray()) {
                    if (item.isJsonPrimitive() && item.getAsString().equals(value)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all elements from the JSON array under the given key.
     */
    public void clearList(String key) {
        lock.writeLock().lock();
        try {
            rootData.add(key, new JsonArray());
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== STRUCTURAL ====================

    /**
     * Checks if a top-level key exists.
     */
    public boolean hasKey(String key) {
        lock.readLock().lock();
        try {
            return rootData.has(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a set of all top-level keys in the configuration.
     */
    public Set<String> keys() {
        lock.readLock().lock();
        try {
            return new HashSet<>(rootData.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes a key entirely from the configuration.
     * @return true if the key existed and was removed, false otherwise
     */
    public boolean removeKey(String key) {
        lock.writeLock().lock();
        try {
            if (rootData.has(key)) {
                rootData.remove(key);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
