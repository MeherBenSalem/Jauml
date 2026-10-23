package tn.naizo.jauml.server;

import net.minecraft.server.level.ServerPlayer;
import tn.naizo.jauml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side player connection events, fired by every JAUML loader module.
 * <pre>{@code
 * PlayerEvents.JOIN.register(player -> Welcome.send(player));
 * }</pre>
 *
 * @since 3.0.0
 */
public final class PlayerEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger("JaumlEvents");

    /** A player finished logging in and is in the world. */
    public static final Event<PlayerCallback> JOIN = create("JOIN");
    /** A player is disconnecting; they are still in the world. */
    public static final Event<PlayerCallback> LEAVE = create("LEAVE");

    private PlayerEvents() {}

    @FunctionalInterface
    public interface PlayerCallback {
        void on(ServerPlayer player);
    }

    private static Event<PlayerCallback> create(String name) {
        return Event.create(listeners -> player -> {
            for (PlayerCallback listener : listeners) {
                try {
                    listener.on(player);
                } catch (RuntimeException e) {
                    LOGGER.error("PlayerEvents.{} listener {} failed", name, listener.getClass().getName(), e);
                }
            }
        });
    }

    /**
     * Called by JAUML loader modules; mods do not call these.
     */
    public static final class Hooks {

        private Hooks() {}

        public static void join(ServerPlayer player) {
            JOIN.invoker().on(player);
        }

        public static void leave(ServerPlayer player) {
            LEAVE.invoker().on(player);
        }
    }
}
