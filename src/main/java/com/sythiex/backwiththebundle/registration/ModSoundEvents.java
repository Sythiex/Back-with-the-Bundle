package com.sythiex.backwiththebundle.registration;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(
        Registries.SOUND_EVENT,
        ResourceLocation.DEFAULT_NAMESPACE
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> BUNDLE_INSERT_FAIL = SOUND_EVENTS.register(
        "item.bundle.insert_fail",
        SoundEvent::createVariableRangeEvent
    );

    private ModSoundEvents() {
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
