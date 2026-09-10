package tn.naizo.jauml.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A lightweight JSON Schema validator supporting type checks, required keys,
 * nested objects, array item validation, and optional constraint keywords.
 */
public final class JsonSchema {

    private final JsonObject schemaObject;

    public JsonSchema(JsonObject schemaObject) {
        if (schemaObject == null) {
            throw new IllegalArgumentException("Schema object cannot be null");
        }
        this.schemaObject = schemaObject;
    }

    /**
     * Parses a JSON Schema from a string representation.
     */
    public static JsonSchema parse(String jsonSchema) throws JsonException {
        if (jsonSchema == null || jsonSchema.trim().isEmpty()) {
            throw new JsonException("Schema string cannot be null or empty");
        }
        try {
            JsonElement el = JsonParser.parseString(jsonSchema);
            if (!el.isJsonObject()) {
                throw new JsonException("Schema must be a valid JSON Object");
            }
            return new JsonSchema(el.getAsJsonObject());
        } catch (Exception e) {
            throw new JsonException("Failed to parse JSON schema: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the provided JsonElement against this schema.
     * Throws a JsonException if validation fails.
     */
    public void validate(JsonElement data) throws JsonException {
        validate(data, schemaObject, "#");
    }

    /**
     * Returns true if the provided JsonElement is valid according to this schema, false otherwise.
     */
    public boolean isValid(JsonElement data) {
        try {
            validate(data);
            return true;
        } catch (JsonException e) {
            return false;
        }
    }

    private void validate(JsonElement data, JsonObject schema, String path) throws JsonException {
        if (schema == null) {
            return;
        }

        // 1. Check type
        if (schema.has("type")) {
            String expectedType = schema.get("type").getAsString();
            if (!checkType(data, expectedType)) {
                throw new JsonException("Validation failed at " + path + ": expected type '" + expectedType + "', but found '" + getActualType(data) + "'");
            }
        }

        // 2. Check enum
        if (schema.has("enum")) {
            validateEnum(data, schema.getAsJsonArray("enum"), path);
        }

        // 3. String length constraints
        if (data != null && !data.isJsonNull() && data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) {
            String value = data.getAsString();
            if (schema.has("minLength")) {
                int minLength = schema.get("minLength").getAsInt();
                if (value.length() < minLength) {
                    throw new JsonException("Validation failed at " + path + ": string length " + value.length() + " is less than minLength " + minLength);
                }
            }
            if (schema.has("maxLength")) {
                int maxLength = schema.get("maxLength").getAsInt();
                if (value.length() > maxLength) {
                    throw new JsonException("Validation failed at " + path + ": string length " + value.length() + " exceeds maxLength " + maxLength);
                }
            }
        }

        // 4. Number range constraints
        if (data != null && !data.isJsonNull() && data.isJsonPrimitive() && data.getAsJsonPrimitive().isNumber()) {
            double value = data.getAsDouble();
            if (schema.has("minimum")) {
                double minimum = schema.get("minimum").getAsDouble();
                if (value < minimum) {
                    throw new JsonException("Validation failed at " + path + ": value " + value + " is less than minimum " + minimum);
                }
            }
            if (schema.has("maximum")) {
                double maximum = schema.get("maximum").getAsDouble();
                if (value > maximum) {
                    throw new JsonException("Validation failed at " + path + ": value " + value + " exceeds maximum " + maximum);
                }
            }
        }

        // 5. Check object properties
        if (data != null && data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();

            // Validate required properties
            if (schema.has("required")) {
                JsonArray required = schema.getAsJsonArray("required");
                for (JsonElement req : required) {
                    String reqKey = req.getAsString();
                    if (!obj.has(reqKey) || obj.get(reqKey).isJsonNull()) {
                        throw new JsonException("Validation failed at " + path + ": missing required property '" + reqKey + "'");
                    }
                }
            }

            // Reject undeclared properties when additionalProperties is explicitly false
            if (schema.has("additionalProperties")
                    && schema.get("additionalProperties").isJsonPrimitive()
                    && !schema.get("additionalProperties").getAsBoolean()) {
                JsonObject properties = schema.has("properties")
                        ? schema.getAsJsonObject("properties")
                        : new JsonObject();
                for (String key : obj.keySet()) {
                    if (!properties.has(key)) {
                        throw new JsonException("Validation failed at " + path + ": additional property '" + key + "' is not allowed");
                    }
                }
            }

            // Validate nested properties
            if (schema.has("properties")) {
                JsonObject properties = schema.getAsJsonObject("properties");
                for (String key : properties.keySet()) {
                    if (obj.has(key)) {
                        validate(obj.get(key), properties.getAsJsonObject(key), path + "." + key);
                    }
                }
            }
        }

        // 6. Validate array constraints and items
        if (data != null && data.isJsonArray()) {
            JsonArray arr = data.getAsJsonArray();
            if (schema.has("minItems")) {
                int minItems = schema.get("minItems").getAsInt();
                if (arr.size() < minItems) {
                    throw new JsonException("Validation failed at " + path + ": array has " + arr.size() + " items, fewer than minItems " + minItems);
                }
            }
            if (schema.has("maxItems")) {
                int maxItems = schema.get("maxItems").getAsInt();
                if (arr.size() > maxItems) {
                    throw new JsonException("Validation failed at " + path + ": array has " + arr.size() + " items, more than maxItems " + maxItems);
                }
            }
            if (schema.has("items")) {
                JsonObject itemsSchema = schema.getAsJsonObject("items");
                for (int i = 0; i < arr.size(); i++) {
                    validate(arr.get(i), itemsSchema, path + "[" + i + "]");
                }
            }
        }
    }

    private void validateEnum(JsonElement data, JsonArray enumValues, String path) throws JsonException {
        for (JsonElement allowed : enumValues) {
            if (elementsEqual(data, allowed)) {
                return;
            }
        }
        throw new JsonException("Validation failed at " + path + ": value is not one of the allowed enum values");
    }

    private boolean elementsEqual(JsonElement a, JsonElement b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private boolean checkType(JsonElement data, String type) {
        if (data == null || data.isJsonNull()) {
            return "null".equals(type);
        }
        switch (type) {
            case "string":
                return data.isJsonPrimitive() && data.getAsJsonPrimitive().isString();
            case "number":
                return data.isJsonPrimitive() && data.getAsJsonPrimitive().isNumber();
            case "boolean":
                return data.isJsonPrimitive() && data.getAsJsonPrimitive().isBoolean();
            case "array":
                return data.isJsonArray();
            case "object":
                return data.isJsonObject();
            case "null":
                return data.isJsonNull();
            default:
                return true;
        }
    }

    private String getActualType(JsonElement data) {
        if (data == null || data.isJsonNull()) {
            return "null";
        }
        if (data.isJsonObject()) {
            return "object";
        }
        if (data.isJsonArray()) {
            return "array";
        }
        if (data.isJsonPrimitive()) {
            var prim = data.getAsJsonPrimitive();
            if (prim.isString()) return "string";
            if (prim.isNumber()) return "number";
            if (prim.isBoolean()) return "boolean";
        }
        return "unknown";
    }
}
