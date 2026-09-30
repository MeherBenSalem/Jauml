package tn.naizo.jauml;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import tn.naizo.jauml.api.JaumlInitializer;

@Mod(Constants.MOD_ID)
public class Jauml {

    public Jauml(IEventBus modEventBus, ModContainer modContainer) {
        Constants.LOG.info("Hello NeoForge world from Jauml!");
        JaumlInitializer.initialize();
    }
}
