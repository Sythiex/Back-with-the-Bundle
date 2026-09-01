package com.sythiex.backwiththebundle.network;

import com.sythiex.backwiththebundle.BackwiththeBundle;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SelectBundleItemPayload(int containerId, int slotIndex, int selectedItemIndex)
    implements CustomPacketPayload {
    public static final Type<SelectBundleItemPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(BackwiththeBundle.MODID, "select_bundle_item")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectBundleItemPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SelectBundleItemPayload::containerId,
        ByteBufCodecs.VAR_INT,
        SelectBundleItemPayload::slotIndex,
        ByteBufCodecs.VAR_INT,
        SelectBundleItemPayload::selectedItemIndex,
        SelectBundleItemPayload::new
    );

    @Override
    public Type<SelectBundleItemPayload> type() {
        return TYPE;
    }
}
