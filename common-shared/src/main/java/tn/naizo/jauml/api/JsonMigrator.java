package tn.naizo.jauml.api;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

/**
 * Sequential migration manager that tracks and runs functional migration steps
 * to upgrade JSON structures from older versions to newer versions.
 */
public final class JsonMigrator {

    // Map of: fromVersion -> (toVersion -> MigrationFunction)
    private final Map<String, Map<String, Function<JsonObject, JsonObject>>> migrations = new HashMap<>();
    private String preferredVersionKey;

    /**
     * Sets the preferred version key used when reading and updating version fields during migration.
     * When unset, the default alias list is used for detection and "version" is created if none exist.
     */
    public void setPreferredVersionKey(String key) {
        this.preferredVersionKey = key;
    }

    public String getPreferredVersionKey() {
        return preferredVersionKey;
    }

    /**
     * Registers a migration function to upgrade from one version to another.
     */
    public void register(String fromVersion, String toVersion, Function<JsonObject, JsonObject> migration) {
        if (fromVersion == null || toVersion == null || migration == null) {
            throw new IllegalArgumentException("Versions and migration function cannot be null");
        }
        migrations.computeIfAbsent(fromVersion, k -> new HashMap<>()).put(toVersion, migration);
    }

    /**
     * Migrates the provided JSON structure to the target version using registered paths.
     */
    public JsonObject migrate(JsonObject json, String targetVersion) throws JsonException {
        if (json == null || targetVersion == null) {
            throw new IllegalArgumentException("JSON and target version cannot be null");
        }
        
        JsonObject current = JsonLib.deepClone(json).getAsJsonObject();
        String currentVersion = JsonLib.detectVersion(current, preferredVersionKey).orElse(null);
        if (currentVersion == null) {
            throw new JsonException("Migration failed: no source version key detected in JSON");
        }

        if (currentVersion.equals(targetVersion)) {
            return current;
        }

        List<String> path = findPath(currentVersion, targetVersion);
        if (path == null || path.size() < 2) {
            throw new JsonException("Migration path not found from version '" + currentVersion + "' to '" + targetVersion + "'");
        }

        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            Function<JsonObject, JsonObject> step = migrations.get(from).get(to);
            try {
                current = step.apply(current);
                updateVersionKey(current, to);
            } catch (Exception e) {
                throw new JsonException("Failed during migration step from '" + from + "' to '" + to + "': " + e.getMessage(), e);
            }
        }

        return current;
    }

    private List<String> findPath(String start, String target) {
        Queue<List<String>> queue = new LinkedList<>();
        queue.add(Collections.singletonList(start));
        Set<String> visited = new HashSet<>();
        visited.add(start);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);
            if (current.equals(target)) {
                return path;
            }
            Map<String, Function<JsonObject, JsonObject>> nextSteps = migrations.get(current);
            if (nextSteps != null) {
                for (String next : nextSteps.keySet()) {
                    if (!visited.contains(next)) {
                        visited.add(next);
                        List<String> newPath = new ArrayList<>(path);
                        newPath.add(next);
                        queue.add(newPath);
                    }
                }
            }
        }
        return null;
    }

    private void updateVersionKey(JsonObject obj, String newVersion) {
        if (preferredVersionKey != null && !preferredVersionKey.isEmpty()) {
            obj.addProperty(preferredVersionKey, newVersion);
            return;
        }
        String[] versionKeys = {"version", "configVersion", "schemaVersion", "config_version", "file_version"};
        for (String key : versionKeys) {
            if (obj.has(key)) {
                obj.addProperty(key, newVersion);
                return;
            }
        }
        obj.addProperty("version", newVersion);
    }
}
