package tn.naizo.jauml.server;

import net.minecraft.server.MinecraftServer;
import tn.naizo.jauml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Server lifecycle and tick events, fired by every JAUML loader module (Fabric, NeoForge and Forge) so common code
 * can hook them without loader-specific wiring. They fire for dedicated and integrated (single-player) servers.
 * <pre>{@code
 * ServerEvents.STARTED.register(server -> MyMod.LOG.info("Running on {}", server.getServerModName()));
 * }</pre>
 *
 * @since 3.0.0
 */
public final class ServerEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlEvents");

    private static volatile MinecraftServer currentServer;

    /** Before worlds load. */
    public static final Event<ServerCallback> STARTING = create("STARTING");
    /** Worlds are loaded and the server is about to tick. */
    public static final Event<ServerCallback> STARTED = create("STARTED");
    /** The server begins shutting down; worlds are still loaded. */
    public static final Event<ServerCallback> STOPPING = create("STOPPING");
    /** The server has shut down. */
    public static final Event<ServerCallback> STOPPED = create("STOPPED");
    /** Start of every server tick. */
    public static final Event<ServerCallback> TICK_START = create("TICK_START");
    /** End of every server tick. */
    public static final Event<ServerCallback> TICK_END = create("TICK_END");

    private ServerEvents() {}

    /**
     * Returns the running server from {@link #STARTING} until {@link #STOPPED}, or empty outside that window
     * (and on a client that is not hosting a world).
     */
    public static Optional<MinecraftServer> currentServer() {
        return Optional.ofNullable(currentServer);
    }

    @FunctionalInterface
    public interface ServerCallback {
        void on(MinecraftServer server);
    }

    private static Event<ServerCallback> create(String name) {
        return Event.create(listeners -> server -> {
            for (ServerCallback listener : listeners) {
                try {
                    listener.on(server);
                } catch (RuntimeException e) {
                    LOGGER.error("ServerEvents.{} listener {} failed", name, listener.getClass().getName(), e);
                }
            }
        });
    }

    /**
     * Called by JAUML loader modules; mods do not call these.
     */
    public static final class Hooks {

        private Hooks() {}

        public static void starting(MinecraftServer server) {
            currentServer = server;
            STARTING.invoker().on(server);
        }

        public static void started(MinecraftServer server) {
            currentServer = server;
            STARTED.invoker().on(server);
        }

        public static void stopping(MinecraftServer server) {
            STOPPING.invoker().on(server);
        }

        public static void stopped(MinecraftServer server) {
            try {
                STOPPED.invoker().on(server);
            } finally {
                Scheduler.clear();
                currentServer = null;
            }
        }

        public static void tickStart(MinecraftServer server) {
            TICK_START.invoker().on(server);
        }

        public static void tickEnd(MinecraftServer server) {
            Scheduler.tick();
            TICK_END.invoker().on(server);
        }
    }
}
