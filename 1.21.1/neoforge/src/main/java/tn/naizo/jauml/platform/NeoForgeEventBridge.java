package tn.naizo.jauml.platform;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import tn.naizo.jauml.server.PlayerEvents;
import tn.naizo.jauml.server.ServerEvents;

/**
 * Fires JAUML's {@link ServerEvents} and {@link PlayerEvents} from NeoForge events.
 */
public final class NeoForgeEventBridge {

    private NeoForgeEventBridge() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onServerStarting);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onServerStopping);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onServerStopped);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onServerTickPre);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onServerTickPost);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(NeoForgeEventBridge::onPlayerLoggedOut);
    }

    private static void onServerStarting(ServerStartingEvent event) {
        ServerEvents.Hooks.starting(event.getServer());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        ServerEvents.Hooks.started(event.getServer());
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerEvents.Hooks.stopping(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerEvents.Hooks.stopped(event.getServer());
    }

    private static void onServerTickPre(ServerTickEvent.Pre event) {
        ServerEvents.Hooks.tickStart(event.getServer());
    }

    private static void onServerTickPost(ServerTickEvent.Post event) {
        ServerEvents.Hooks.tickEnd(event.getServer());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerEvents.Hooks.join(player);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerEvents.Hooks.leave(player);
        }
    }
}
