package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.sythiex.backwiththebundle.bundle.BundleInteractionPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

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

    @Test
    void dragInsertionRequiresOneBundleAndAnEligibleSlottedItem() {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        ItemStack apple = new ItemStack(Items.APPLE);

        assertTrue(BundleInteractionPolicy.canDragIntoBundle(bundle, apple, true));

        ItemStack stackedBundles = bundle.copyWithCount(2);
        assertFalse(BundleInteractionPolicy.canDragIntoBundle(stackedBundles, apple, true));
        assertFalse(BundleInteractionPolicy.canDragIntoBundle(new ItemStack(Items.CHEST), apple, true));
        assertFalse(BundleInteractionPolicy.canDragIntoBundle(bundle, ItemStack.EMPTY, true));
        assertFalse(BundleInteractionPolicy.canDragIntoBundle(bundle, new ItemStack(Items.SHULKER_BOX), true));
        assertFalse(BundleInteractionPolicy.canDragIntoBundle(bundle, apple, false));
    }

    @Test
    void dragRemovalAcceptsEmptyAndExactSlotsButNotMismatchesOrRestrictedSlots() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.DIRT), new ItemStack(Items.IRON_INGOT, 10));
        Slot emptySlot = slot(ItemStack.EMPTY);
        Slot matchingSlot = slot(new ItemStack(Items.IRON_INGOT, 2));
        Slot fullSlot = slot(new ItemStack(Items.IRON_INGOT, 64));
        Slot mismatchedSlot = slot(new ItemStack(Items.GOLD_INGOT));
        Slot rejectingSlot = new TestSlot(new ItemStack(Items.IRON_INGOT), false, true, false, 64);
        Slot inactiveSlot = new TestSlot(ItemStack.EMPTY, true, false, false, 64);
        Slot fakeSlot = new TestSlot(ItemStack.EMPTY, true, true, true, 64);

        assertTrue(BundleInteractionPolicy.canDragOutOfBundle(bundle, emptySlot));
        assertTrue(BundleInteractionPolicy.canDragOutOfBundle(bundle, matchingSlot));
        assertTrue(BundleInteractionPolicy.shouldMergeBundleIntoSlot(bundle, matchingSlot));
        assertTrue(BundleInteractionPolicy.canTransferMatchingToSlot(bundle, matchingSlot));

        assertTrue(BundleInteractionPolicy.canDragOutOfBundle(bundle, fullSlot));
        assertTrue(BundleInteractionPolicy.shouldMergeBundleIntoSlot(bundle, fullSlot));
        assertFalse(BundleInteractionPolicy.canTransferMatchingToSlot(bundle, fullSlot));

        assertFalse(BundleInteractionPolicy.canDragOutOfBundle(bundle, mismatchedSlot));
        assertFalse(BundleInteractionPolicy.canDragOutOfBundle(bundle, rejectingSlot));
        assertFalse(BundleInteractionPolicy.canDragOutOfBundle(bundle, inactiveSlot));
        assertFalse(BundleInteractionPolicy.canDragOutOfBundle(bundle, fakeSlot));
        assertFalse(BundleInteractionPolicy.canDragOutOfBundle(bundle.copyWithCount(2), emptySlot));
    }

    @Test
    void emptySlotRemovalUsesTheSelectedItemForPlacementChecks() {
        ItemStack bundle = bundleWithItems(new ItemStack(Items.DIRT), new ItemStack(Items.IRON_INGOT));
        com.sythiex.backwiththebundle.bundle.BundleSelection.setSelectedItem(bundle, 1);
        Slot ironOnlySlot = new Slot(new SimpleContainer(1), 0, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.IRON_INGOT);
            }
        };

        assertTrue(BundleInteractionPolicy.canDragOutOfBundle(bundle, ironOnlySlot));
    }

    private static ItemStack bundleWithItems(ItemStack... items) {
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(items)));
        return bundle;
    }

    private static Slot slot(ItemStack stack) {
        return new Slot(new SimpleContainer(stack), 0, 0, 0);
    }

    private static final class TestSlot extends Slot {
        private final boolean acceptsItems;
        private final boolean active;
        private final boolean fake;
        private final int maximum;

        private TestSlot(ItemStack stack, boolean acceptsItems, boolean active, boolean fake, int maximum) {
            super(new SimpleContainer(stack), 0, 0, 0);
            this.acceptsItems = acceptsItems;
            this.active = active;
            this.fake = fake;
            this.maximum = maximum;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.acceptsItems;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }

        @Override
        public boolean isFake() {
            return this.fake;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.maximum;
        }
    }
}
