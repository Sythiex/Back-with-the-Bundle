package com.sythiex.backwiththebundle.network;

import com.sythiex.backwiththebundle.bundle.BundleSelectionRequest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "1";

    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(
            SelectBundleItemPayload.TYPE,
            SelectBundleItemPayload.STREAM_CODEC,
            ModNetworking::handleSelectBundleItem
        );
    }

    private static void handleSelectBundleItem(SelectBundleItemPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            BundleSelectionRequest.apply(
                player.containerMenu,
                payload.containerId(),
                payload.slotIndex(),
                payload.selectedItemIndex()
            );
        }
    }
}
