package tn.naizo.jauml.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Modern utility library for safe, strict parsing, stable serialization, path-based access,
 * merging, and schema/version operations.
 */
public final class JsonLib {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    private JsonLib() {}

    /**
     * Parses a JSON string safely, returning an Optional containing the parsed element,
     * or Optional.empty() if the input is malformed or invalid.
     */
    public static Optional<JsonElement> safeParse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonParser.parseString(json));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a JSON string safely, returning the parsed element or a fallback value if invalid.
     */
    public static JsonElement safeParse(String json, JsonElement fallback) {
        return safeParse(json).orElse(fallback);
    }

    /**
     * Parses a JSON string strictly according to standard JSON syntax.
     * Enforces that there is no trailing data, unquoted keys, or other non-standard formats.
     */
    public static JsonElement strictParse(String json) throws JsonException {
        if (json == null || json.trim().isEmpty()) {
            throw new JsonException("JSON content is null or empty");
        }
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setLenient(false);
            JsonElement element = PRETTY_GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new JsonException("Extra content found after the end of JSON document");
            }
            return element;
        } catch (Exception e) {
            throw new JsonException("Strict JSON parse failed: " + e.getMessage(), e);
        }
    }

    /**
     * Serializes a JsonElement into pretty-printed, deterministic JSON string
     * with object keys sorted alphabetically.
     */
    public static String stableStringify(JsonElement element) {
        if (element == null) {
            return "null";
        }
        return PRETTY_GSON.toJson(sortJsonKeys(element));
    }

    /**
     * Recursively sorts the keys of all JsonObjects in the hierarchy.
     */
    public static JsonElement sortJsonKeys(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject sorted = new JsonObject();
            List<String> keys = new ArrayList<>(obj.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                sorted.add(key, sortJsonKeys(obj.get(key)));
            }
            return sorted;
        }
        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            JsonArray sorted = new JsonArray();
            for (JsonElement item : arr) {
                sorted.add(sortJsonKeys(item));
            }
            return sorted;
        }
        return element;
    }

    /**
     * Deep clones a JsonElement recursively.
     */
    public static JsonElement deepClone(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject clone = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                clone.add(entry.getKey(), deepClone(entry.getValue()));
            }
            return clone;
        }
        if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            JsonArray clone = new JsonArray();
            for (JsonElement item : arr) {
                clone.add(deepClone(item));
            }
            return clone;
        }
        // JsonPrimitive is immutable
        return element;
    }

    /**
     * Fetches a nested JsonElement using dot-bracket notation, e.g., "settings.theme.color" or "users[0].name".
     */
    public static Optional<JsonElement> getByPath(JsonElement root, String path) {
        if (root == null || path == null || path.isEmpty()) {
            return Optional.empty();
        }
        JsonElement current = root;
        String[] parts = path.split("\\.");
        for (String part : parts) {
            if (current == null || current.isJsonNull()) {
                return Optional.empty();
            }
            if (part.contains("[")) {
                String key = part.substring(0, part.indexOf('['));
                if (!key.isEmpty()) {
                    if (!current.isJsonObject()) {
                        return Optional.empty();
                    }
                    current = current.getAsJsonObject().get(key);
                }
                String arrayPart = part.substring(part.indexOf('['));
                while (arrayPart.startsWith("[")) {
                    if (current == null || !current.isJsonArray()) {
                        return Optional.empty();
                    }
                    int endBracket = arrayPart.indexOf(']');
                    if (endBracket == -1) {
                        return Optional.empty();
                    }
                    String indexStr = arrayPart.substring(1, endBracket);
                    try {
                        int index = Integer.parseInt(indexStr);
                        JsonArray arr = current.getAsJsonArray();
                        if (index < 0 || index >= arr.size()) {
                            return Optional.empty();
                        }
                        current = arr.get(index);
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                    arrayPart = arrayPart.substring(endBracket + 1);
                }
            } else {
                if (!current.isJsonObject()) {
                    return Optional.empty();
                }
                current = current.getAsJsonObject().get(part);
            }
        }
        return current == null ? Optional.empty() : Optional.of(current);
    }

    /**
     * Sets a nested value on a JsonObject using a path. Creates intermediate objects if needed.
     */
    public static void setByPath(JsonObject root, String path, JsonElement value) {
        if (root == null || path == null || path.isEmpty()) {
            return;
        }
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (part.contains("[")) {
                String key = part.substring(0, part.indexOf('['));
                String indexStr = part.substring(part.indexOf('[') + 1, part.indexOf(']'));
                int index = Integer.parseInt(indexStr);

                JsonArray arr;
                if (current.has(key) && current.get(key).isJsonArray()) {
                    arr = current.getAsJsonArray(key);
                } else {
                    arr = new JsonArray();
                    current.add(key, arr);
                }

                while (arr.size() <= index) {
                    arr.add(new JsonObject());
                }

                JsonElement item = arr.get(index);
                if (item == null || !item.isJsonObject()) {
                    JsonObject newObj = new JsonObject();
                    arr.set(index, newObj);
                    current = newObj;
                } else {
                    current = item.getAsJsonObject();
                }
            } else {
                if (!current.has(part) || !current.get(part).isJsonObject()) {
                    current.add(part, new JsonObject());
                }
                current = current.getAsJsonObject(part);
            }
        }

        String lastPart = parts[parts.length - 1];
        if (lastPart.contains("[")) {
            String key = lastPart.substring(0, lastPart.indexOf('['));
            String indexStr = lastPart.substring(lastPart.indexOf('[') + 1, lastPart.indexOf(']'));
            int index = Integer.parseInt(indexStr);

            JsonArray arr;
            if (current.has(key) && current.get(key).isJsonArray()) {
                arr = current.getAsJsonArray(key);
            } else {
                arr = new JsonArray();
                current.add(key, arr);
            }

            while (arr.size() <= index) {
                arr.add(JsonNull.INSTANCE);
            }
            arr.set(index, value);
        } else {
            current.add(lastPart, value);
        }
    }

    /**
     * Performs a deep merge of two JSON hierarchies, preferring the original (user config) values,
     * but populating missing structures/values from defaults.
     */
    public static JsonElement merge(JsonElement original, JsonElement defaults) {
        if (original == null || original.isJsonNull()) {
            return deepClone(defaults);
        }
        if (defaults == null || defaults.isJsonNull()) {
            return deepClone(original);
        }
        if (original.isJsonObject() && defaults.isJsonObject()) {
            JsonObject originalObj = original.getAsJsonObject();
            JsonObject defaultsObj = defaults.getAsJsonObject();
            JsonObject merged = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : defaultsObj.entrySet()) {
                String key = entry.getKey();
                if (originalObj.has(key)) {
                    merged.add(key, merge(originalObj.get(key), entry.getValue()));
                } else {
                    merged.add(key, deepClone(entry.getValue()));
                }
            }
            for (Map.Entry<String, JsonElement> entry : originalObj.entrySet()) {
                String key = entry.getKey();
                if (!defaultsObj.has(key)) {
                    merged.add(key, deepClone(entry.getValue()));
                }
            }
            return merged;
        }
        return deepClone(original);
    }

    /**
     * Recursively normalizes the types in the config based on a defaults template.
     * Performs safe type coercion where types mismatch.
     */
    public static JsonElement normalize(JsonElement config, JsonElement defaults) {
        if (defaults == null || defaults.isJsonNull()) {
            return config == null ? JsonNull.INSTANCE : deepClone(config);
        }
        if (config == null || config.isJsonNull()) {
            return deepClone(defaults);
        }

        if (defaults.isJsonObject()) {
            if (!config.isJsonObject()) {
                return deepClone(defaults);
            }
            JsonObject configObj = config.getAsJsonObject();
            JsonObject defaultsObj = defaults.getAsJsonObject();
            JsonObject normalized = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : defaultsObj.entrySet()) {
                String key = entry.getKey();
                if (configObj.has(key)) {
                    normalized.add(key, normalize(configObj.get(key), entry.getValue()));
                } else {
                    normalized.add(key, deepClone(entry.getValue()));
                }
            }
            for (Map.Entry<String, JsonElement> entry : configObj.entrySet()) {
                String key = entry.getKey();
                if (!defaultsObj.has(key)) {
                    normalized.add(key, deepClone(entry.getValue()));
                }
            }
            return normalized;
        }

        if (defaults.isJsonArray()) {
            if (!config.isJsonArray()) {
                return deepClone(defaults);
            }
            JsonArray configArr = config.getAsJsonArray();
            JsonArray defaultsArr = defaults.getAsJsonArray();
            JsonArray normalized = new JsonArray();

            if (defaultsArr.size() == 0) {
                for (JsonElement item : configArr) {
                    normalized.add(deepClone(item));
                }
            } else {
                JsonElement template = defaultsArr.get(0);
                for (JsonElement item : configArr) {
                    normalized.add(normalize(item, template));
                }
            }
            return normalized;
        }

        if (defaults.isJsonPrimitive()) {
            if (!config.isJsonPrimitive()) {
                return deepClone(defaults);
            }
            JsonPrimitive configPrim = config.getAsJsonPrimitive();
            JsonPrimitive defaultsPrim = defaults.getAsJsonPrimitive();

            if (defaultsPrim.isBoolean()) {
                if (configPrim.isBoolean()) {
                    return configPrim;
                }
                String s = configPrim.getAsString().toLowerCase().trim();
                if ("true".equals(s) || "1".equals(s) || "yes".equals(s) || "on".equals(s)) {
                    return new JsonPrimitive(true);
                }
                if ("false".equals(s) || "0".equals(s) || "no".equals(s) || "off".equals(s)) {
                    return new JsonPrimitive(false);
                }
                return deepClone(defaults);
            }

            if (defaultsPrim.isNumber()) {
                if (configPrim.isNumber()) {
                    return configPrim;
                }
                try {
                    double val = Double.parseDouble(configPrim.getAsString().trim());
                    return new JsonPrimitive(val);
                } catch (NumberFormatException e) {
                    return deepClone(defaults);
                }
            }

            if (defaultsPrim.isString()) {
                if (configPrim.isString()) {
                    return configPrim;
                }
                return new JsonPrimitive(configPrim.getAsString());
            }
        }

        return deepClone(config);
    }

    /**
     * Detects version number defined in the JSON configuration, checking keys like
     * "version", "configVersion", "schemaVersion", "config_version", etc.
     */
    public static Optional<String> detectVersion(JsonElement root) {
        return detectVersion(root, null);
    }

    /**
     * Detects version number, checking {@code preferredKey} first when provided,
     * then falling back to the standard alias list.
     */
    public static Optional<String> detectVersion(JsonElement root, String preferredKey) {
        if (root == null || !root.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject obj = root.getAsJsonObject();
        if (preferredKey != null && !preferredKey.isEmpty() && obj.has(preferredKey)) {
            JsonElement el = obj.get(preferredKey);
            if (el.isJsonPrimitive()) {
                return Optional.of(el.getAsString());
            }
        }
        String[] versionKeys = {"version", "configVersion", "schemaVersion", "config_version", "file_version"};
        for (String key : versionKeys) {
            if (obj.has(key)) {
                JsonElement el = obj.get(key);
                if (el.isJsonPrimitive()) {
                    return Optional.of(el.getAsString());
                }
            }
        }
        return Optional.empty();
    }
}
