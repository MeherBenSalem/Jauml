package tn.naizo.jauml.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class CooldownsTest {

    @Test
    public void testTryUseStartsAndBlocksUntilExpiry() {
        AtomicLong now = new AtomicLong(1_000);
        Cooldowns<UUID> cooldowns = Cooldowns.withClock(now::get);
        UUID player = UUID.randomUUID();

        assertTrue(cooldowns.tryUse(player, 100));
        assertTrue(cooldowns.isCoolingDown(player));
        assertEquals(100, cooldowns.remaining(player));
        assertFalse(cooldowns.tryUse(player, 100));

        now.addAndGet(60);
        assertEquals(40, cooldowns.remaining(player));
        assertFalse(cooldowns.tryUse(player, 100));

        now.addAndGet(40);
        assertEquals(0, cooldowns.remaining(player));
        assertFalse(cooldowns.isCoolingDown(player));
        assertTrue(cooldowns.tryUse(player, 100));
    }

    @Test
    public void testKeysAreIndependent() {
        AtomicLong now = new AtomicLong();
        Cooldowns<String> cooldowns = Cooldowns.withClock(now::get);
        assertTrue(cooldowns.tryUse("dash", 10));
        assertTrue(cooldowns.tryUse("blink", 10));
        assertEquals(2, cooldowns.size());
    }

    @Test
    public void testStartResetAndPrune() {
        AtomicLong now = new AtomicLong();
        Cooldowns<String> cooldowns = Cooldowns.withClock(now::get);

        cooldowns.start("a", 5);
        cooldowns.start("b", 50);
        cooldowns.start("a", 20);
        assertEquals(20, cooldowns.remaining("a"));

        cooldowns.reset("a");
        assertFalse(cooldowns.isCoolingDown("a"));

        now.set(60);
        cooldowns.prune();
        assertEquals(0, cooldowns.size());

        cooldowns.start("c", 5);
        cooldowns.clear();
        assertEquals(0, cooldowns.size());
    }

    @Test
    public void testMillisClockAndInvalidClock() {
        Cooldowns<String> cooldowns = Cooldowns.millis();
        assertTrue(cooldowns.tryUse("x", 60_000));
        assertTrue(cooldowns.remaining("x") > 0);
        assertThrows(IllegalArgumentException.class, () -> Cooldowns.withClock(null));
    }
}
