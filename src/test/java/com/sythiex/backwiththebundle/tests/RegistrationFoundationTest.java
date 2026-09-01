package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sythiex.backwiththebundle.registration.ModItems;
import com.sythiex.backwiththebundle.registration.ModRecipeSerializers;
import com.sythiex.backwiththebundle.registration.ModSoundEvents;
import com.sythiex.backwiththebundle.recipe.TransmuteRecipe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BundleItem;

class RegistrationFoundationTest {
    @Test
    void dyedBundlesUseVanillaIdsAndVanillaCreativeOrder() {
        List<String> expectedPaths = List.of(
            "white_bundle",
            "light_gray_bundle",
            "gray_bundle",
            "black_bundle",
            "brown_bundle",
            "red_bundle",
            "orange_bundle",
            "yellow_bundle",
            "lime_bundle",
            "green_bundle",
            "cyan_bundle",
            "light_blue_bundle",
            "blue_bundle",
            "purple_bundle",
            "magenta_bundle",
            "pink_bundle"
        );

        assertEquals(
            expectedPaths,
            ModItems.DYED_BUNDLES_IN_CREATIVE_ORDER.stream().map(holder -> holder.getId().getPath()).toList()
        );
        assertEquals(
            List.of("minecraft"),
            ModItems.DYED_BUNDLES_IN_CREATIVE_ORDER.stream()
                .map(holder -> holder.getId().getNamespace())
                .distinct()
                .toList()
        );
        ModItems.DYED_BUNDLES_IN_CREATIVE_ORDER.forEach(holder -> {
            assertInstanceOf(BundleItem.class, holder.get());
            assertEquals(1, holder.get().getDefaultInstance().getMaxStackSize());
            assertTrue(holder.get().getDefaultInstance().getOrDefault(DataComponents.BUNDLE_CONTENTS, null).isEmpty());
        });
    }

    @Test
    void vanillaNamespaceFoundationObjectsHaveExpectedIds() {
        assertEquals(
            ResourceLocation.withDefaultNamespace("crafting_transmute"),
            ModRecipeSerializers.CRAFTING_TRANSMUTE.getId()
        );
        assertEquals(
            ResourceLocation.withDefaultNamespace("item.bundle.insert_fail"),
            ModSoundEvents.BUNDLE_INSERT_FAIL.getId()
        );
        assertInstanceOf(TransmuteRecipe.Serializer.class, ModRecipeSerializers.CRAFTING_TRANSMUTE.get());
        assertEquals(
            ResourceLocation.withDefaultNamespace("item.bundle.insert_fail"),
            ModSoundEvents.BUNDLE_INSERT_FAIL.get().getLocation()
        );
    }
}
