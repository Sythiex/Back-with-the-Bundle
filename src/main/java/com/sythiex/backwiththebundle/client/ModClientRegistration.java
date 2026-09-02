package com.sythiex.backwiththebundle.client;

import com.sythiex.backwiththebundle.BackwiththeBundle;
import com.sythiex.backwiththebundle.bundle.BundleTooltipData;
import com.sythiex.backwiththebundle.client.tooltip.ClientBundleTooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(modid = BackwiththeBundle.MODID, value = Dist.CLIENT)
public final class ModClientRegistration {
    private ModClientRegistration() {
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(BundleTooltipData.class, ClientBundleTooltip::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        BundleGuiItemRenderer.openModelLocations().forEach(event::register);
    }
}
