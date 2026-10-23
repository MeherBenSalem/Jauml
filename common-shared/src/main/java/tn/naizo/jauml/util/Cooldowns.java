package tn.naizo.jauml.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Per-key cooldowns (per player UUID, per entity, per ability...), replacing hand-rolled {@code Map<UUID, Long>}.
 * Thread-safe. Expired entries are dropped when touched or by {@link #prune()}.
 * <pre>{@code
 * private static final Cooldowns<UUID> DASH = Cooldowns.millis();
 *
 * if (DASH.tryUse(player.getUUID(), 3_000)) { dash(player); }
 * else { tell(player, DASH.remaining(player.getUUID()) + " ms left"); }
 * }</pre>
 *
 * @param <K> the key type
 * @since 3.0.0
 */
public final class Cooldowns<K> {

    private final LongSupplier clock;
    private final Map<K, Long> expiries = new ConcurrentHashMap<>();

    private Cooldowns(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * Cooldowns measured in wall-clock milliseconds; they keep running while the game is paused or lagging.
     */
    public static <K> Cooldowns<K> millis() {
        return new Cooldowns<>(System::currentTimeMillis);
    }

    /**
     * Cooldowns measured on any clock, for example server ticks: {@code Cooldowns.withClock(Scheduler::currentTick)}.
     */
    public static <K> Cooldowns<K> withClock(LongSupplier clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock cannot be null");
        }
        return new Cooldowns<>(clock);
    }

    /**
     * Starts the cooldown if the key is not cooling down.
     *
     * @return true if the key was ready (and is now cooling down), false if it is still cooling down
     */
    public boolean tryUse(K key, long duration) {
        long now = clock.getAsLong();
        boolean[] started = {false};
        expiries.compute(key, (k, expiry) -> {
            if (expiry != null && expiry > now) {
                return expiry;
            }
            started[0] = true;
            return now + Math.max(0, duration);
        });
        return started[0];
    }

    /**
     * Starts (or restarts) the cooldown regardless of its current state.
     */
    public void start(K key, long duration) {
        expiries.put(key, clock.getAsLong() + Math.max(0, duration));
    }

    public boolean isCoolingDown(K key) {
        return remaining(key) > 0;
    }

    /**
     * Returns the time left on the clock's unit, or zero when the key is ready.
     */
    public long remaining(K key) {
        Long expiry = expiries.get(key);
        if (expiry == null) {
            return 0;
        }
        long left = expiry - clock.getAsLong();
        if (left <= 0) {
            expiries.remove(key, expiry);
            return 0;
        }
        return left;
    }

    public void reset(K key) {
        expiries.remove(key);
    }

    public void clear() {
        expiries.clear();
    }

    /**
     * Drops expired entries; call it now and then for long-lived maps with many keys.
     */
    public void prune() {
        long now = clock.getAsLong();
        expiries.values().removeIf(expiry -> expiry <= now);
    }

    /**
     * Returns how many keys are cooling down.
     */
    public int size() {
        prune();
        return expiries.size();
    }
}
