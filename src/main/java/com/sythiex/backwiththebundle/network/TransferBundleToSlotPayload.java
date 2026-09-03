package com.sythiex.backwiththebundle.network;

import com.sythiex.backwiththebundle.BackwiththeBundle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TransferBundleToSlotPayload(int containerId, int slotIndex) implements CustomPacketPayload {
    public static final Type<TransferBundleToSlotPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(BackwiththeBundle.MODID, "transfer_bundle_to_slot")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TransferBundleToSlotPayload> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TransferBundleToSlotPayload::containerId,
            ByteBufCodecs.VAR_INT,
            TransferBundleToSlotPayload::slotIndex,
            TransferBundleToSlotPayload::new
        );

    @Override
    public Type<TransferBundleToSlotPayload> type() {
        return TYPE;
    }
}
