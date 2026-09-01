package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.sythiex.backwiththebundle.bundle.BundleSelection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;

class BundleSelectionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void selectionRoundTripsWithoutReplacingUnrelatedCustomData() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
        CompoundTag root = new CompoundTag();
        root.putString("unrelated", "preserved");
        CompoundTag modData = new CompoundTag();
        modData.putString("another_value", "also preserved");
        root.put("backwiththebundle", modData);
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        BundleSelection.setSelectedItem(bundle, 1);

        assertEquals(1, BundleSelection.getSelectedItem(bundle));
        CompoundTag storedRoot = bundle.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("preserved", storedRoot.getString("unrelated"));
        assertEquals("also preserved", storedRoot.getCompound("backwiththebundle").getString("another_value"));
        assertEquals(1, storedRoot.getCompound("backwiththebundle").getInt("selected_item"));

        BundleSelection.clear(bundle);

        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
        storedRoot = bundle.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("preserved", storedRoot.getString("unrelated"));
        assertEquals("also preserved", storedRoot.getCompound("backwiththebundle").getString("another_value"));
        assertFalse(storedRoot.getCompound("backwiththebundle").contains("selected_item"));
    }

    @Test
    void clearingTheOnlyMarkerRemovesTheCustomDataComponent() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE));
        BundleSelection.setSelectedItem(bundle, 0);
        assertTrue(bundle.has(DataComponents.CUSTOM_DATA));

        BundleSelection.clear(bundle);

        assertFalse(bundle.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    void invalidOrHiddenSelectionsAreRejectedAndNormalized() {
        ItemStack bundle = bundleWithItemTypes(13);
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundle));

        BundleSelection.setSelectedItem(bundle, 7);
        assertEquals(7, BundleSelection.getSelectedItem(bundle));
        BundleSelection.setSelectedItem(bundle, 8);
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));

        CompoundTag root = new CompoundTag();
        CompoundTag modData = new CompoundTag();
        modData.putInt("selected_item", -1);
        root.put("backwiththebundle", modData);
        bundle.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
        BundleSelection.normalize(bundle);
        assertFalse(bundle.has(DataComponents.CUSTOM_DATA));
    }

    @Test
    void selectingTheCurrentEntryTogglesItOff() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));

        BundleSelection.toggleSelectedItem(bundle, 1);
        assertEquals(1, BundleSelection.getSelectedItem(bundle));
        BundleSelection.toggleSelectedItem(bundle, 1);
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void visibleCountMatchesTheReleasedFourColumnLayout() {
        assertEquals(0, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(0)));
        assertEquals(1, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(1)));
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(8)));
        assertEquals(11, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(11)));
        assertEquals(12, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(12)));
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(13)));
        assertEquals(9, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(14)));
        assertEquals(10, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(15)));
        assertEquals(11, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(16)));
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundleWithItemTypes(17)));
    }

    private static ItemStack bundleWithItemTypes(int count) {
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ItemStack stack = new ItemStack(Items.STONE);
            CompoundTag marker = new CompoundTag();
            marker.putInt("type", index);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
            items.add(stack);
        }
        return bundleWithItems(items.toArray(ItemStack[]::new));
    }

    private static ItemStack bundleWithItems(ItemStack... items) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(items)));
        return bundle;
    }
}
