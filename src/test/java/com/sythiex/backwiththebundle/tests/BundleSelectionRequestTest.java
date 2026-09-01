package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.sythiex.backwiththebundle.bundle.BundleSelection;
import com.sythiex.backwiththebundle.bundle.BundleSelectionRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;

class BundleSelectionRequestTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void validRequestSelectsAndClearsAnItem() {
        ItemStack bundle = bundleWithItemTypes(3);
        TestMenu menu = new TestMenu(7, bundle);

        assertTrue(BundleSelectionRequest.apply(menu, 7, 0, 2));
        assertEquals(2, BundleSelection.getSelectedItem(bundle));

        assertTrue(BundleSelectionRequest.apply(menu, 7, 0, BundleSelection.NO_SELECTED_ITEM));
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void staleContainerIdIsRejectedWithoutChangingSelection() {
        ItemStack bundle = bundleWithItemTypes(2);
        BundleSelection.setSelectedItem(bundle, 0);
        TestMenu menu = new TestMenu(7, bundle);

        assertFalse(BundleSelectionRequest.apply(menu, 8, 0, 1));
        assertEquals(0, BundleSelection.getSelectedItem(bundle));
    }

    @Test
    void outOfBoundsSlotsAreRejected() {
        TestMenu menu = new TestMenu(7, bundleWithItemTypes(1));

        assertFalse(BundleSelectionRequest.apply(menu, 7, -1, 0));
        assertFalse(BundleSelectionRequest.apply(menu, 7, 1, 0));
    }

    @Test
    void nonBundleSlotsAreRejected() {
        TestMenu menu = new TestMenu(7, new ItemStack(Items.CHEST));

        assertFalse(BundleSelectionRequest.apply(menu, 7, 0, 0));
        assertFalse(BundleSelectionRequest.apply(menu, 7, 0, BundleSelection.NO_SELECTED_ITEM));
    }

    @Test
    void malformedAndHiddenIndicesAreRejected() {
        ItemStack bundle = bundleWithItemTypes(13);
        TestMenu menu = new TestMenu(7, bundle);
        assertEquals(8, BundleSelection.getNumberOfItemsToShow(bundle));

        assertFalse(BundleSelectionRequest.apply(menu, 7, 0, -2));
        assertFalse(BundleSelectionRequest.apply(menu, 7, 0, 8));
        assertEquals(BundleSelection.NO_SELECTED_ITEM, BundleSelection.getSelectedItem(bundle));

        assertTrue(BundleSelectionRequest.apply(menu, 7, 0, 7));
        assertEquals(7, BundleSelection.getSelectedItem(bundle));
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

        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(items)));
        return bundle;
    }

    private static final class TestMenu extends AbstractContainerMenu {
        private final SimpleContainer container;

        private TestMenu(int containerId, ItemStack... stacks) {
            super(null, containerId);
            this.container = new SimpleContainer(stacks);
            for (int index = 0; index < stacks.length; index++) {
                this.addSlot(new Slot(this.container, index, 0, 0));
            }
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
