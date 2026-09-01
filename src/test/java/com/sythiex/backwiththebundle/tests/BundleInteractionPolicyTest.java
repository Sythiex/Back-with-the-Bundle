package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sythiex.backwiththebundle.bundle.BundleInteractionPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class BundleInteractionPolicyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void bundleTooltipRemainsVisibleWhileCarryingAnItem() {
        ItemStack hiddenBundle = new ItemStack(Items.BUNDLE);
        hiddenBundle.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        assertTrue(BundleInteractionPolicy.shouldRenderTooltip(true, new ItemStack(Items.CHEST)));
        assertTrue(BundleInteractionPolicy.shouldRenderTooltip(false, new ItemStack(Items.BUNDLE)));
        assertFalse(BundleInteractionPolicy.shouldRenderTooltip(false, new ItemStack(Items.CHEST)));
        assertFalse(BundleInteractionPolicy.shouldRenderTooltip(false, hiddenBundle));
        assertFalse(BundleInteractionPolicy.shouldRenderTooltip(false, ItemStack.EMPTY));
    }

    @Test
    void inventoryMovesClearSelectionBeforeTheStackChangesLocation() {
        assertTrue(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.QUICK_MOVE));
        assertTrue(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.SWAP));

        assertFalse(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.PICKUP));
        assertFalse(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.THROW));
        assertFalse(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.CLONE));
        assertFalse(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.QUICK_CRAFT));
        assertFalse(BundleInteractionPolicy.shouldClearSelectionBeforeClick(ClickType.PICKUP_ALL));
    }
}
