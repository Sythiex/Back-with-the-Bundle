package com.sythiex.backwiththebundle.client;

import com.sythiex.backwiththebundle.BackwiththeBundle;
import com.sythiex.backwiththebundle.config.ClientConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = BackwiththeBundle.MODID, dist = Dist.CLIENT)
public final class BackwiththeBundleClient {
    public BackwiththeBundleClient(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
