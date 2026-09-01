package com.sythiex.backwiththebundle.bundle;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class BundleSelectionRequest {
    private BundleSelectionRequest() {
    }

    public static boolean apply(
        AbstractContainerMenu menu,
        int expectedContainerId,
        int slotIndex,
        int selectedItemIndex
    ) {
        if (menu.containerId != expectedContainerId || slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return false;
        }

        ItemStack bundle = menu.getSlot(slotIndex).getItem();
        if (!BundleContentsOperations.isBundle(bundle)) {
            return false;
        }

        if (selectedItemIndex == BundleSelection.NO_SELECTED_ITEM) {
            BundleSelection.clear(bundle);
        } else if (selectedItemIndex >= 0 && selectedItemIndex < BundleSelection.getNumberOfItemsToShow(bundle)) {
            BundleSelection.setSelectedItem(bundle, selectedItemIndex);
        } else {
            return false;
        }

        return true;
    }
}
