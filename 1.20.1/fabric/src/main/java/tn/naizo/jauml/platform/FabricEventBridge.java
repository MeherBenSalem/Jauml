package tn.naizo.jauml.platform;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import tn.naizo.jauml.Constants;
import tn.naizo.jauml.server.PlayerEvents;
import tn.naizo.jauml.server.ServerEvents;

/**
 * Fires JAUML's {@link ServerEvents} and {@link PlayerEvents} from Fabric API events.
 */
public final class FabricEventBridge {

    private FabricEventBridge() {}

    /**
     * Registers the bridge when Fabric API is installed; without it JAUML's server events and scheduler stay idle.
     */
    public static void registerIfAvailable() {
        FabricLoader loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("fabric-lifecycle-events-v1") || !loader.isModLoaded("fabric-networking-api-v1")) {
            Constants.LOG.warn("Fabric API is not installed: JAUML server events and the scheduler are inactive.");
            return;
        }
        Listeners.register();
    }

    // Kept in its own class so Fabric API types load only after the check above.
    private static final class Listeners {

        static void register() {
            ServerLifecycleEvents.SERVER_STARTING.register(ServerEvents.Hooks::starting);
            ServerLifecycleEvents.SERVER_STARTED.register(ServerEvents.Hooks::started);
            ServerLifecycleEvents.SERVER_STOPPING.register(ServerEvents.Hooks::stopping);
            ServerLifecycleEvents.SERVER_STOPPED.register(ServerEvents.Hooks::stopped);
            ServerTickEvents.START_SERVER_TICK.register(ServerEvents.Hooks::tickStart);
            ServerTickEvents.END_SERVER_TICK.register(ServerEvents.Hooks::tickEnd);
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> PlayerEvents.Hooks.join(handler.getPlayer()));
            ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PlayerEvents.Hooks.leave(handler.getPlayer()));
        }
    }
}
