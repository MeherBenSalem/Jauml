package tn.naizo.jauml;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import tn.naizo.jauml.api.JaumlInitializer;
import tn.naizo.jauml.platform.ForgeEventBridge;

@Mod(JaumlCore.MODID)
public class JaumlCore {
    public static final String MODID = "jauml";

    public JaumlCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        ForgeEventBridge.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        JaumlInitializer.initialize();
    }
}
