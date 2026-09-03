package com.sythiex.backwiththebundle.network;

import com.sythiex.backwiththebundle.bundle.BundleInteractionHooks;
import com.sythiex.backwiththebundle.bundle.BundleSelectionRequest;
import com.sythiex.backwiththebundle.bundle.BundleSlotTransferRequest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
        registrar.playToServer(
            TransferBundleToSlotPayload.TYPE,
            TransferBundleToSlotPayload.STREAM_CODEC,
            ModNetworking::handleTransferBundleToSlot
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

    private static void handleTransferBundleToSlot(TransferBundleToSlotPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || !menu.stillValid(player)) {
            menu.broadcastFullState();
            return;
        }

        if (!BundleSlotTransferRequest.isMatchingTarget(menu, payload.containerId(), payload.slotIndex())) {
            menu.broadcastFullState();
            return;
        }

        if (BundleInteractionHooks.onStackedOnOther(menu, menu.getSlot(payload.slotIndex()), player)) {
            menu.broadcastChanges();
            return;
        }

        if (!BundleSlotTransferRequest.canApply(menu, player, payload.containerId(), payload.slotIndex())) {
            menu.broadcastFullState();
            return;
        }

        int transferred = BundleSlotTransferRequest.apply(menu, player, payload.containerId(), payload.slotIndex());
        if (transferred > 0) {
            player.playSound(
                SoundEvents.BUNDLE_REMOVE_ONE,
                0.8F,
                0.8F + player.level().getRandom().nextFloat() * 0.4F
            );
            menu.broadcastChanges();
        } else {
            menu.broadcastFullState();
        }
    }
}
