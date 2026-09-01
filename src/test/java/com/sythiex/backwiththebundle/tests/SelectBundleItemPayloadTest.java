package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sythiex.backwiththebundle.network.SelectBundleItemPayload;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

class SelectBundleItemPayloadTest {
    @Test
    void payloadCodecRoundTripsSelectionAndClearRequests() {
        assertCodecRoundTrip(new SelectBundleItemPayload(12, 37, 6));
        assertCodecRoundTrip(new SelectBundleItemPayload(12, 37, -1));
    }

    private static void assertCodecRoundTrip(SelectBundleItemPayload payload) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
            Unpooled.buffer(),
            RegistryAccess.EMPTY,
            ConnectionType.OTHER
        );
        try {
            SelectBundleItemPayload.STREAM_CODEC.encode(buffer, payload);
            assertEquals(payload, SelectBundleItemPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
