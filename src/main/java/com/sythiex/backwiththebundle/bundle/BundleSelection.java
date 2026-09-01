package com.sythiex.backwiththebundle.bundle;

import com.sythiex.backwiththebundle.BackwiththeBundle;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;

/**
 * Keeps track of the item selected in a bundle. Since 1.21.1 has no place for this in {@link BundleContents},
 * store it in custom data instead.
 */
public final class BundleSelection {
    public static final int NO_SELECTED_ITEM = -1;

    private static final String MOD_DATA_KEY = BackwiththeBundle.MODID;
    private static final String SELECTED_ITEM_KEY = "selected_item";

    private BundleSelection() {
    }

    public static int getSelectedItem(ItemStack bundle) {
        int selectedItem = getStoredSelectedItem(bundle);
        return isSelectable(bundle, selectedItem) ? selectedItem : NO_SELECTED_ITEM;
    }

    public static ItemStack getSelectedItemStack(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        int selectedItem = getSelectedItem(bundle);
        return contents != null && selectedItem != NO_SELECTED_ITEM
            ? contents.getItemUnsafe(selectedItem)
            : ItemStack.EMPTY;
    }

    public static int getNumberOfItemsToShow(ItemStack bundle) {
        return getNumberOfItemsToShow(bundle.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY));
    }

    public static int getNumberOfItemsToShow(BundleContents contents) {
        int size = contents.size();
        int maximum = size > 12 ? 11 : 12;
        int remainder = size % 4;
        int emptyGridSlots = remainder == 0 ? 0 : 4 - remainder;
        return Math.min(size, maximum - emptyGridSlots);
    }

    public static void setSelectedItem(ItemStack bundle, int selectedItem) {
        if (!isSelectable(bundle, selectedItem)) {
            clear(bundle);
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, bundle, root -> {
            CompoundTag modData = root.contains(MOD_DATA_KEY, Tag.TAG_COMPOUND)
                ? root.getCompound(MOD_DATA_KEY).copy()
                : new CompoundTag();
            modData.putInt(SELECTED_ITEM_KEY, selectedItem);
            root.put(MOD_DATA_KEY, modData);
        });
    }

    public static void toggleSelectedItem(ItemStack bundle, int selectedItem) {
        if (selectedItem == getSelectedItem(bundle) || !isSelectable(bundle, selectedItem)) {
            clear(bundle);
        } else {
            setSelectedItem(bundle, selectedItem);
        }
    }

    public static void normalize(ItemStack bundle) {
        int storedSelectedItem = getStoredSelectedItem(bundle);
        if (hasStoredSelectionMarker(bundle) && !isSelectable(bundle, storedSelectedItem)) {
            clear(bundle);
        }
    }

    public static void clear(ItemStack bundle) {
        if (!bundle.has(DataComponents.CUSTOM_DATA)) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, bundle, root -> {
            if (!root.contains(MOD_DATA_KEY, Tag.TAG_COMPOUND)) {
                return;
            }

            CompoundTag modData = root.getCompound(MOD_DATA_KEY).copy();
            modData.remove(SELECTED_ITEM_KEY);
            if (modData.isEmpty()) {
                root.remove(MOD_DATA_KEY);
            } else {
                root.put(MOD_DATA_KEY, modData);
            }
        });
    }

    private static boolean isSelectable(ItemStack bundle, int selectedItem) {
        return selectedItem >= 0 && selectedItem < getNumberOfItemsToShow(bundle);
    }

    private static int getStoredSelectedItem(ItemStack bundle) {
        CustomData customData = bundle.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return NO_SELECTED_ITEM;
        }

        CompoundTag root = customData.copyTag();
        if (!root.contains(MOD_DATA_KEY, Tag.TAG_COMPOUND)) {
            return NO_SELECTED_ITEM;
        }

        CompoundTag modData = root.getCompound(MOD_DATA_KEY);
        return modData.contains(SELECTED_ITEM_KEY, Tag.TAG_INT)
            ? modData.getInt(SELECTED_ITEM_KEY)
            : NO_SELECTED_ITEM;
    }

    private static boolean hasStoredSelectionMarker(ItemStack bundle) {
        CustomData customData = bundle.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag root = customData.copyTag();
        return root.contains(MOD_DATA_KEY, Tag.TAG_COMPOUND)
            && root.getCompound(MOD_DATA_KEY).contains(SELECTED_ITEM_KEY);
    }
}
