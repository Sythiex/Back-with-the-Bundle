package com.sythiex.backwiththebundle;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.sythiex.backwiththebundle.registration.ModCreativeTabs;
import com.sythiex.backwiththebundle.registration.ModItems;
import com.sythiex.backwiththebundle.registration.ModRecipeSerializers;
import com.sythiex.backwiththebundle.registration.ModSoundEvents;
import com.sythiex.backwiththebundle.network.ModNetworking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(BackwiththeBundle.MODID)
public class BackwiththeBundle {
    public static final String MODID = "backwiththebundle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BackwiththeBundle(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Back with the Bundle loading");
        ModItems.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        modEventBus.addListener(ModNetworking::register);
        modEventBus.addListener(ModCreativeTabs::addBundleVariants);
    }
}
