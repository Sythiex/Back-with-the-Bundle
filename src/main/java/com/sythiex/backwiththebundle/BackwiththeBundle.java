package com.sythiex.backwiththebundle;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;

@Mod(BackwiththeBundle.MODID)
public class BackwiththeBundle {
    public static final String MODID = "backwiththebundle";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BackwiththeBundle(IEventBus modEventBus, ModContainer modContainer) {
    }
}
