package tn.naizo.jauml;

import net.fabricmc.api.ModInitializer;
import tn.naizo.jauml.api.JaumlInitializer;
import tn.naizo.jauml.platform.FabricEventBridge;

public class Jauml implements ModInitializer {

    @Override
    public void onInitialize() {
        JaumlInitializer.initialize();
        FabricEventBridge.registerIfAvailable();
    }
}
