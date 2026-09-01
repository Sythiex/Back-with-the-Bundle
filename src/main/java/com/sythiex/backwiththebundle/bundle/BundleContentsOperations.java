package com.sythiex.backwiththebundle.bundle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

public final class BundleContentsOperations {
    private BundleContentsOperations() {
    }

    public static boolean isBundle(ItemStack stack) {
        return stack.getItem() instanceof BundleItem;
    }

    public static int tryInsert(ItemStack bundle, ItemStack insertedStack) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) {
            return 0;
        }

        BundleContents.Mutable mutableContents = new BundleContents.Mutable(contents);
        int inserted = mutableContents.tryInsert(insertedStack);
        if (inserted > 0) {
            replaceContents(bundle, mutableContents.toImmutable());
        }
        return inserted;
    }

    public static int tryTransfer(ItemStack bundle, Slot slot, Player player) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        ItemStack slottedStack = slot.getItem();
        if (contents == null || slottedStack.isEmpty() || !slottedStack.canFitInsideContainerItems()) {
            return 0;
        }

        BundleContents.Mutable mutableContents = new BundleContents.Mutable(contents);
        int inserted = mutableContents.tryTransfer(slot, player);
        if (inserted > 0) {
            replaceContents(bundle, mutableContents.toImmutable());
        }
        return inserted;
    }

    public static ItemStack removeSelected(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            BundleSelection.clear(bundle);
            return ItemStack.EMPTY;
        }

        int selectedItem = BundleSelection.getSelectedItem(bundle);
        int removedIndex = selectedItem == BundleSelection.NO_SELECTED_ITEM ? 0 : selectedItem;
        List<ItemStack> remainingItems = contents.itemCopyStream()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        ItemStack removedStack = remainingItems.remove(removedIndex);
        replaceContents(bundle, new BundleContents(List.copyOf(remainingItems)));
        return removedStack;
    }

    public static void clearContents(ItemStack bundle) {
        if (bundle.has(DataComponents.BUNDLE_CONTENTS)) {
            replaceContents(bundle, BundleContents.EMPTY);
        } else {
            BundleSelection.clear(bundle);
        }
    }

    public static void replaceContents(ItemStack bundle, BundleContents contents) {
        bundle.set(DataComponents.BUNDLE_CONTENTS, contents);
        BundleSelection.clear(bundle);
    }
}
