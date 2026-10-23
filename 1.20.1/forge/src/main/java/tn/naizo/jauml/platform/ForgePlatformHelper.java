package tn.naizo.jauml.platform;

import tn.naizo.jauml.spi.PlatformProvider;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public class ForgePlatformHelper implements PlatformProvider {

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getGameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public Optional<String> getModVersion(String modId) {
        ModList mods = ModList.get();
        if (mods == null) {
            return Optional.empty();
        }
        return mods.getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString());
    }

    @Override
    public Optional<String> getModName(String modId) {
        ModList mods = ModList.get();
        if (mods == null) {
            return Optional.empty();
        }
        return mods.getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName());
    }

    @Override
    public Collection<String> getLoadedModIds() {
        ModList mods = ModList.get();
        if (mods == null) {
            return Collections.emptyList();
        }
        return mods.getMods().stream()
                .map(IModInfo::getModId)
                .collect(Collectors.toList());
    }
}
