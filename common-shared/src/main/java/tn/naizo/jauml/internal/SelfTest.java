package tn.naizo.jauml.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.naizo.jauml.platform.Platform;
import tn.naizo.jauml.server.PlayerEvents;
import tn.naizo.jauml.server.Scheduler;
import tn.naizo.jauml.server.ServerEvents;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in boot check ({@code -Djauml.selfTest=true}) that logs every server event and one scheduler run, so a
 * dev or production server boot shows the loader bridge works. Off by default.
 */
public final class SelfTest {

    public static final String PROPERTY = "jauml.selfTest";

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlSelfTest");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private SelfTest() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(PROPERTY) || !REGISTERED.compareAndSet(false, true)) {
            return;
        }
        LOGGER.info("JAUML self-test enabled: loader={}, minecraft={}, client={}",
                Platform.loader().displayName(), Platform.minecraftVersion(), Platform.isClient());
        AtomicBoolean firstTick = new AtomicBoolean(true);
        ServerEvents.STARTING.register(server -> LOGGER.info("JAUML self-test: STARTING"));
        ServerEvents.STARTED.register(server -> {
            LOGGER.info("JAUML self-test: STARTED, currentServer present={}", ServerEvents.currentServer().isPresent());
            Scheduler.runNextTick(() -> LOGGER.info("JAUML self-test: scheduler ran at tick {}", Scheduler.currentTick()));
        });
        ServerEvents.TICK_END.register(server -> {
            if (firstTick.compareAndSet(true, false)) {
                LOGGER.info("JAUML self-test: first TICK_END");
            }
        });
        ServerEvents.STOPPING.register(server -> LOGGER.info("JAUML self-test: STOPPING"));
        ServerEvents.STOPPED.register(server -> LOGGER.info("JAUML self-test: STOPPED"));
        // getUUID is named the same on every supported version; GameProfile accessors are not.
        PlayerEvents.JOIN.register(player -> LOGGER.info("JAUML self-test: JOIN {}", player.getUUID()));
    }
}
