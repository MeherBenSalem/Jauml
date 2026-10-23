package tn.naizo.jauml.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import tn.naizo.jauml.server.PlayerEvents;
import tn.naizo.jauml.server.ServerEvents;

/**
 * Fires JAUML's {@link ServerEvents} and {@link PlayerEvents} from Forge events.
 */
public final class ForgeEventBridge {

    private ForgeEventBridge() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.addListener(ForgeEventBridge::onPlayerLoggedOut);
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

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        // Early 47.x builds have no ServerTickEvent#getServer, so read the server Forge tracks itself.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            ServerEvents.Hooks.tickStart(server);
        } else {
            ServerEvents.Hooks.tickEnd(server);
        }
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
