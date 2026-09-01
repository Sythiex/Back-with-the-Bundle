package com.sythiex.backwiththebundle.bundle;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public final class BundleInteractionPolicy {
    private BundleInteractionPolicy() {
    }

    public static boolean shouldRenderTooltip(boolean carriedStackEmpty, ItemStack hoveredStack) {
        return carriedStackEmpty
            || hoveredStack.getTooltipImage().filter(BundleTooltipData.class::isInstance).isPresent();
    }

    public static boolean shouldClearSelectionBeforeClick(ClickType type) {
        return type == ClickType.QUICK_MOVE
            || type == ClickType.SWAP;
    }
}
