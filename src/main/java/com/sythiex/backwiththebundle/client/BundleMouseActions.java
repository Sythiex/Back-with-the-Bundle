package com.sythiex.backwiththebundle.client;

import com.sythiex.backwiththebundle.BackwiththeBundle;
import com.sythiex.backwiththebundle.bundle.BundleContentsOperations;
import com.sythiex.backwiththebundle.bundle.BundleSelection;
import com.sythiex.backwiththebundle.bundle.BundleSelectionScroll;
import com.sythiex.backwiththebundle.network.SelectBundleItemPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = BackwiththeBundle.MODID, value = Dist.CLIENT)
public final class BundleMouseActions {
    private static final BundleScrollAccumulator SCROLL_ACCUMULATOR = new BundleScrollAccumulator();

    private static AbstractContainerScreen<?> selectedScreen;
    private static Slot selectedSlot;
    private static ItemStack selectedBundle = ItemStack.EMPTY;
    private static ServerSlotTarget selectedServerTarget = ServerSlotTarget.NONE;

    private BundleMouseActions() {
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
            && selectFromScroll(screen, event.getScrollDeltaX(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenRendered(ScreenEvent.Render.Post event) {
        if (selectedScreen == null) {
            return;
        }

        if (event.getScreen() != selectedScreen) {
            clearTrackedSelection();
            return;
        }

        Slot hoveredSlot = selectedScreen.getSlotUnderMouse();
        if (hoveredSlot == null
            || hoveredSlot != selectedSlot
            || !BundleContentsOperations.isBundle(hoveredSlot.getItem())) {
            clearTrackedSelection();
        } else if (BundleSelection.getSelectedItem(hoveredSlot.getItem()) == BundleSelection.NO_SELECTED_ITEM) {
            BundleSelection.clear(selectedBundle);
            forgetTrackedSelection();
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (event.getScreen() == selectedScreen) {
            clearTrackedSelection();
        }

        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            SCROLL_ACCUMULATOR.reset();
        }
    }

    public static void clearSelectionBeforeClick(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null || !BundleContentsOperations.isBundle(slot.getItem())) {
            return;
        }

        if (screen == selectedScreen && slot == selectedSlot) {
            clearTrackedSelection();
        } else if (BundleSelection.getSelectedItem(slot.getItem()) != BundleSelection.NO_SELECTED_ITEM) {
            BundleSelection.clear(slot.getItem());
            sendSelection(resolveServerSlot(screen, slot), BundleSelection.NO_SELECTED_ITEM);
        }
    }

    private static boolean selectFromScroll(AbstractContainerScreen<?> screen, double scrollX, double scrollY) {
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) {
            return false;
        }

        ItemStack bundle = slot.getItem();
        if (!BundleContentsOperations.isBundle(bundle)) {
            return false;
        }

        int selectionSize = BundleSelection.getNumberOfItemsToShow(bundle);
        if (selectionSize == 0) {
            return false;
        }

        BundleScrollAccumulator.Step step = SCROLL_ACCUMULATOR.add(scrollX, scrollY);
        int scrollAmount = step.y() == 0 ? -step.x() : step.y();
        if (scrollAmount != 0) {
            int selectedItem = BundleSelection.getSelectedItem(bundle);
            int nextSelection = BundleSelectionScroll.getNextSelection(scrollAmount, selectedItem, selectionSize);
            if (selectedItem != nextSelection) {
                setSelection(screen, slot, nextSelection);
            }
        }

        return true;
    }

    private static void setSelection(AbstractContainerScreen<?> screen, Slot slot, int selectedItem) {
        if (selectedScreen != null && (screen != selectedScreen || slot != selectedSlot)) {
            clearTrackedSelection();
        }

        ItemStack bundle = slot.getItem();
        BundleSelection.setSelectedItem(bundle, selectedItem);
        selectedScreen = screen;
        selectedSlot = slot;
        selectedBundle = bundle;
        selectedServerTarget = resolveServerSlot(screen, slot);
        sendSelection(selectedServerTarget, selectedItem);
    }

    private static void clearTrackedSelection() {
        Slot slot = selectedSlot;
        ItemStack bundle = selectedBundle;
        ServerSlotTarget serverTarget = selectedServerTarget;
        forgetTrackedSelection();

        if (BundleContentsOperations.isBundle(bundle)) {
            BundleSelection.clear(bundle);
        }
        if (slot != null) {
            ItemStack currentBundle = slot.getItem();
            if (currentBundle != bundle && BundleContentsOperations.isBundle(currentBundle)) {
                BundleSelection.clear(currentBundle);
            }
        }
        sendSelection(serverTarget, BundleSelection.NO_SELECTED_ITEM);
    }

    private static void forgetTrackedSelection() {
        selectedScreen = null;
        selectedSlot = null;
        selectedBundle = ItemStack.EMPTY;
        selectedServerTarget = ServerSlotTarget.NONE;
    }

    private static ServerSlotTarget resolveServerSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof CreativeModeInventoryScreen) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return ServerSlotTarget.NONE;
            }

            int slotIndex = CreativeInventorySlotResolver.findInventoryMenuSlot(player.inventoryMenu, slot);
            return slotIndex == CreativeInventorySlotResolver.NO_SERVER_SLOT
                ? ServerSlotTarget.NONE
                : new ServerSlotTarget(player.inventoryMenu.containerId, slotIndex);
        }

        int slotIndex = screen.getMenu().slots.indexOf(slot);
        return slotIndex < 0
            ? ServerSlotTarget.NONE
            : new ServerSlotTarget(screen.getMenu().containerId, slotIndex);
    }

    private static void sendSelection(ServerSlotTarget target, int selectedItem) {
        if (target.isValid() && Minecraft.getInstance().getConnection() != null) {
            PacketDistributor.sendToServer(
                new SelectBundleItemPayload(target.containerId(), target.slotIndex(), selectedItem)
            );
        }
    }

    private record ServerSlotTarget(int containerId, int slotIndex) {
        private static final ServerSlotTarget NONE = new ServerSlotTarget(-1, -1);

        private boolean isValid() {
            return this.containerId >= 0 && this.slotIndex >= 0;
        }
    }
}
