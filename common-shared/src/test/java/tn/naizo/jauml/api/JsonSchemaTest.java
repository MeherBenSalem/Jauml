package tn.naizo.jauml.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonSchemaTest {

    @Test
    public void testBasicTypeValidation() {
        String schemaJson = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}, \"age\": {\"type\": \"number\"}}}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonObject valid = new JsonObject();
        valid.addProperty("name", "Bob");
        valid.addProperty("age", 30);
        assertTrue(schema.isValid(valid));

        JsonObject invalidType = new JsonObject();
        invalidType.addProperty("name", "Bob");
        invalidType.addProperty("age", "30"); // string instead of number
        assertFalse(schema.isValid(invalidType));
    }

    @Test
    public void testRequiredFields() {
        String schemaJson = "{\"type\": \"object\", \"required\": [\"id\", \"enabled\"]}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonObject valid = new JsonObject();
        valid.addProperty("id", "123");
        valid.addProperty("enabled", true);
        assertTrue(schema.isValid(valid));

        JsonObject missingKey = new JsonObject();
        missingKey.addProperty("id", "123");
        assertFalse(schema.isValid(missingKey));
    }

    @Test
    public void testNestedObjects() {
        String schemaJson = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"database\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"host\": {\"type\": \"string\"},\n" +
                "        \"port\": {\"type\": \"number\"}\n" +
                "      },\n" +
                "      \"required\": [\"host\"]\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonObject valid = new JsonObject();
        JsonObject db = new JsonObject();
        db.addProperty("host", "localhost");
        db.addProperty("port", 3306);
        rootAdd(valid, "database", db);
        assertTrue(schema.isValid(valid));

        JsonObject invalidNested = new JsonObject();
        JsonObject invalidDb = new JsonObject();
        invalidDb.addProperty("port", 3306); // missing host
        rootAdd(invalidNested, "database", invalidDb);
        assertFalse(schema.isValid(invalidNested));
    }

    @Test
    public void testArrayItems() {
        String schemaJson = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"tags\": {\n" +
                "      \"type\": \"array\",\n" +
                "      \"items\": {\"type\": \"string\"}\n" +
                "    }\n" +
                "  }\n" +
                "}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonObject valid = new JsonObject();
        JsonArray tags = new JsonArray();
        tags.add("admin");
        tags.add("user");
        valid.add("tags", tags);
        assertTrue(schema.isValid(valid));

        JsonObject invalidTags = new JsonObject();
        JsonArray badTags = new JsonArray();
        badTags.add("admin");
        badTags.add(123); // number instead of string
        invalidTags.add("tags", badTags);
        assertFalse(schema.isValid(invalidTags));
    }

    @Test
    public void testEnumValidation() {
        String schemaJson = "{\"type\": \"string\", \"enum\": [\"red\", \"green\", \"blue\"]}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        assertTrue(schema.isValid(new JsonPrimitive("green")));
        assertFalse(schema.isValid(new JsonPrimitive("yellow")));

        JsonException ex = assertThrows(JsonException.class, () -> schema.validate(new JsonPrimitive("yellow")));
        assertTrue(ex.getMessage().contains("#"));
        assertTrue(ex.getMessage().contains("enum"));
    }

    @Test
    public void testMinimumMaximum() {
        String schemaJson = "{\"type\": \"number\", \"minimum\": 0, \"maximum\": 100}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        assertTrue(schema.isValid(new JsonPrimitive(50)));
        assertTrue(schema.isValid(new JsonPrimitive(0)));
        assertTrue(schema.isValid(new JsonPrimitive(100)));
        assertFalse(schema.isValid(new JsonPrimitive(-1)));
        assertFalse(schema.isValid(new JsonPrimitive(101)));

        JsonException ex = assertThrows(JsonException.class, () -> schema.validate(new JsonPrimitive(-1)));
        assertTrue(ex.getMessage().contains("#"));
        assertTrue(ex.getMessage().contains("minimum"));
    }

    @Test
    public void testMinLengthMaxLength() {
        String schemaJson = "{\"type\": \"string\", \"minLength\": 2, \"maxLength\": 5}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        assertTrue(schema.isValid(new JsonPrimitive("ab")));
        assertTrue(schema.isValid(new JsonPrimitive("abcde")));
        assertFalse(schema.isValid(new JsonPrimitive("a")));
        assertFalse(schema.isValid(new JsonPrimitive("abcdef")));

        JsonException ex = assertThrows(JsonException.class, () -> schema.validate(new JsonPrimitive("a")));
        assertTrue(ex.getMessage().contains("#"));
        assertTrue(ex.getMessage().contains("minLength"));
    }

    @Test
    public void testAdditionalPropertiesFalse() {
        String schemaJson = "{" +
                "\"type\": \"object\"," +
                "\"additionalProperties\": false," +
                "\"properties\": {" +
                "\"name\": {\"type\": \"string\"}," +
                "\"age\": {\"type\": \"number\"}" +
                "}" +
                "}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonObject valid = new JsonObject();
        valid.addProperty("name", "Alice");
        valid.addProperty("age", 25);
        assertTrue(schema.isValid(valid));

        JsonObject extraKey = new JsonObject();
        extraKey.addProperty("name", "Alice");
        extraKey.addProperty("extra", "not allowed");
        assertFalse(schema.isValid(extraKey));

        JsonException ex = assertThrows(JsonException.class, () -> schema.validate(extraKey));
        assertTrue(ex.getMessage().contains("#"));
        assertTrue(ex.getMessage().contains("extra"));
    }

    @Test
    public void testMinItemsMaxItems() {
        String schemaJson = "{\"type\": \"array\", \"minItems\": 1, \"maxItems\": 3, \"items\": {\"type\": \"string\"}}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonArray valid = new JsonArray();
        valid.add("a");
        valid.add("b");
        assertTrue(schema.isValid(valid));

        assertFalse(schema.isValid(new JsonArray()));
        JsonArray tooMany = new JsonArray();
        tooMany.add("a");
        tooMany.add("b");
        tooMany.add("c");
        tooMany.add("d");
        assertFalse(schema.isValid(tooMany));

        JsonException ex = assertThrows(JsonException.class, () -> schema.validate(new JsonArray()));
        assertTrue(ex.getMessage().contains("#"));
        assertTrue(ex.getMessage().contains("minItems"));
    }

    @Test
    public void testSchemasWithoutNewKeywordsUnchanged() {
        String schemaJson = "{\"type\": \"object\", \"properties\": {\"name\": {\"type\": \"string\"}}}";
        JsonSchema schema = JsonSchema.parse(schemaJson);

        JsonObject withExtra = new JsonObject();
        withExtra.addProperty("name", "Bob");
        withExtra.addProperty("unknown", 42);
        assertTrue(schema.isValid(withExtra));
    }

    private void rootAdd(JsonObject root, String key, JsonObject child) {
        root.add(key, child);
    }
}
