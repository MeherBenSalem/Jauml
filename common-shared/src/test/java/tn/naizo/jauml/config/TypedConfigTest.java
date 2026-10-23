package tn.naizo.jauml.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tn.naizo.jauml.api.JaumlConfig;
import tn.naizo.jauml.api.JsonMigrator;
import tn.naizo.jauml.api.TestPlatformProvider;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TypedConfigTest {

    public enum Difficulty { EASY, NORMAL, HARD }

    @Comment("Example settings\nSecond header line")
    public static class Settings {
        @Comment("Maximum boss level")
        @Range(min = 1, max = 100)
        public int maxLevel = 50;

        @Range(min = 0.0, max = 1.0)
        public double dropChance = 0.25;

        public boolean enabled = true;
        public String greeting = "Hello, \"world\"";
        public Difficulty difficulty = Difficulty.NORMAL;
        public List<String> blockedDimensions = new ArrayList<>(List.of("minecraft:the_end"));
        public Map<String, Integer> weights = new LinkedHashMap<>(Map.of("common", 10));
        public Drops drops = new Drops();
        public transient int cached = 7;
    }

    public static class Drops {
        @Comment("Coins per kill")
        @Range(min = 0)
        public int coins = 3;
        public boolean announce = false;
    }

    public static class Versioned {
        public String version = "2";
        public int radius = 8;
    }

    public static class NoDefaultConstructor {
        public int value;

        public NoDefaultConstructor(int value) {
            this.value = value;
        }
    }

    @TempDir
    public Path tempDir;

    @BeforeEach
    public void setUp() {
        TestPlatformProvider.setTempDir(tempDir);
    }

    private TypedConfig<Settings> bind(Path file) {
        return TypedConfig.builder(Settings.class).path(file).build();
    }

    private static String read(Path file) throws Exception {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    public void testCreatesFileWithDefaultsAndComments() throws Exception {
        Path file = tempDir.resolve("settings.json");
        TypedConfig<Settings> config = bind(file);

        assertEquals(50, config.get().maxLevel);
        assertTrue(Files.exists(file));
        String text = read(file);
        assertTrue(text.startsWith("// Example settings\n// Second header line\n{"), text);
        assertTrue(text.contains("// Maximum boss level"), text);
        assertTrue(text.contains("// Range: 1 to 100"), text);
        assertTrue(text.contains("// Default: 50"), text);
        assertTrue(text.contains("// Allowed: EASY, NORMAL, HARD"), text);
        assertTrue(text.contains("// Range: >= 0"), text);
        assertTrue(text.contains("\"blockedDimensions\": [\"minecraft:the_end\"]"), text);
        assertTrue(text.contains("\"greeting\": \"Hello, \\\"world\\\"\""), text);
        assertFalse(text.contains("cached"), text);
    }

    @Test
    public void testReadsHandEditedFileWithComments() throws Exception {
        Path file = tempDir.resolve("edited.json");
        Files.writeString(file, "// my notes\n{\n  \"maxLevel\": 80, // inline note\n  \"difficulty\": \"HARD\",\n"
                + "  /* block\n     comment */\n  \"drops\": { \"coins\": 9 }\n}\n// trailing note\n", StandardCharsets.UTF_8);

        Settings settings = bind(file).get();
        assertEquals(80, settings.maxLevel);
        assertEquals(Difficulty.HARD, settings.difficulty);
        assertEquals(9, settings.drops.coins);
        // Missing keys were filled in and written back.
        assertFalse(settings.drops.announce);
        assertEquals(0.25, settings.dropChance);
        assertTrue(read(file).contains("\"announce\": false"));
    }

    @Test
    public void testClampsRangesAndRepairsInvalidValues() throws Exception {
        Path file = tempDir.resolve("invalid.json");
        Files.writeString(file, "{ \"maxLevel\": 500, \"dropChance\": -2, \"enabled\": \"yes\", "
                + "\"difficulty\": \"IMPOSSIBLE\", \"drops\": { \"coins\": -4 }, \"blockedDimensions\": 3 }", StandardCharsets.UTF_8);

        Settings settings = bind(file).get();
        assertEquals(100, settings.maxLevel);
        assertEquals(0.0, settings.dropChance);
        assertTrue(settings.enabled);
        assertEquals(Difficulty.NORMAL, settings.difficulty);
        assertEquals(0, settings.drops.coins);
        assertEquals(List.of("minecraft:the_end"), settings.blockedDimensions);
        assertTrue(read(file).contains("\"maxLevel\": 100"));
    }

    @Test
    public void testMapEntriesRemovedByUserStayRemoved() throws Exception {
        Path file = tempDir.resolve("maps.json");
        Files.writeString(file, "{ \"weights\": { \"rare\": 2 } }", StandardCharsets.UTF_8);

        Settings settings = bind(file).get();
        assertEquals(Map.of("rare", 2), settings.weights);
    }

    @Test
    public void testTwoRootObjectsAreRejected() throws Exception {
        Path file = tempDir.resolve("double.json");
        Files.writeString(file, "{ \"maxLevel\": 9 } { \"maxLevel\": 10 }", StandardCharsets.UTF_8);
        assertEquals(50, bind(file).get().maxLevel);
        assertTrue(Files.exists(tempDir.resolve("double.json.bak")));
    }

    @Test
    public void testBrokenFileAtStartupIsBackedUp() throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ \"maxLevel\": ", StandardCharsets.UTF_8);

        TypedConfig<Settings> config = bind(file);
        assertEquals(50, config.get().maxLevel);
        assertTrue(Files.exists(tempDir.resolve("broken.json.bak")));
        assertEquals("{ \"maxLevel\": ", read(tempDir.resolve("broken.json.bak")));
    }

    @Test
    public void testReloadKeepsPreviousValuesWhenFileBreaks() throws Exception {
        Path file = tempDir.resolve("reload.json");
        TypedConfig<Settings> config = bind(file);
        AtomicInteger notified = new AtomicInteger();
        config.addListener(settings -> notified.incrementAndGet());

        Files.writeString(file, "{ \"maxLevel\": 70 }", StandardCharsets.UTF_8);
        assertTrue(config.reload());
        assertEquals(70, config.get().maxLevel);
        assertEquals(1, notified.get());

        // Reloading identical content does not notify again.
        assertTrue(config.reload());
        assertEquals(1, notified.get());

        Files.writeString(file, "{ broken", StandardCharsets.UTF_8);
        assertFalse(config.reload());
        assertEquals(70, config.get().maxLevel);
        assertEquals(1, notified.get());
    }

    @Test
    public void testReloadIfChangedSkipsUntouchedFiles() throws Exception {
        Path file = tempDir.resolve("changed.json");
        TypedConfig<Settings> config = bind(file);
        assertFalse(config.reloadIfChanged());

        Files.writeString(file, "{ \"maxLevel\": 12, \"padding\": \"make the size differ\" }", StandardCharsets.UTF_8);
        assertTrue(config.reloadIfChanged());
        assertEquals(12, config.get().maxLevel);
    }

    @Test
    public void testUpdateSavesClampsAndNotifies() throws Exception {
        Path file = tempDir.resolve("update.json");
        TypedConfig<Settings> config = bind(file);
        List<Integer> seen = new ArrayList<>();
        config.addListener(settings -> seen.add(settings.maxLevel));

        config.update(settings -> settings.maxLevel = 1_000);
        assertEquals(100, config.get().maxLevel);
        assertEquals(List.of(100), seen);
        assertTrue(read(file).contains("\"maxLevel\": 100"));

        config.resetToDefaults();
        assertEquals(50, config.get().maxLevel);
        assertEquals(List.of(100, 50), seen);
    }

    @Test
    public void testBindingTheSameFileReturnsTheSameInstance() {
        Path file = tempDir.resolve("same.json");
        assertSame(bind(file), bind(file));
        assertThrows(IllegalStateException.class, () -> TypedConfig.builder(Drops.class).path(file).build());
    }

    @Test
    public void testJaumlConfigBindUsesTheConfigDirectory() {
        TypedConfig<Settings> config = JaumlConfig.bind("typed", "common", Settings.class);
        assertEquals(tempDir.resolve("typed").resolve("common.json").toAbsolutePath().normalize(), config.path());
        assertTrue(Files.exists(config.path()));
    }

    @Test
    public void testMigratorRunsBeforeBinding() throws Exception {
        Path file = tempDir.resolve("versioned.json");
        Files.writeString(file, "{ \"version\": \"1\", \"range\": 4 }", StandardCharsets.UTF_8);
        JsonMigrator migrator = new JsonMigrator();
        migrator.register("1", "2", old -> {
            old.add("radius", old.remove("range"));
            old.addProperty("version", "2");
            return old;
        });

        Versioned value = TypedConfig.builder(Versioned.class).path(file).migrator(migrator, "2").build().get();
        assertEquals(4, value.radius);
        assertEquals("2", value.version);
    }

    @Test
    public void testPlainJsonWhenCommentsAreOff() throws Exception {
        Path file = tempDir.resolve("plain.json");
        TypedConfig.builder(Settings.class).path(file).comments(false).build();
        assertFalse(read(file).contains("//"));
    }

    @Test
    public void testRejectsClassWithoutNoArgConstructor() {
        assertThrows(IllegalArgumentException.class,
                () -> TypedConfig.builder(NoDefaultConstructor.class).path(tempDir.resolve("x.json")).build());
    }
}
