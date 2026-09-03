package com.sythiex.backwiththebundle.mixin.client;

import javax.annotation.Nullable;

import java.util.Set;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.sythiex.backwiththebundle.bundle.BundleInteractionPolicy;
import com.sythiex.backwiththebundle.client.BundleMouseActions;
import com.sythiex.backwiththebundle.config.ClientConfig;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Unique
    private static final int BACKWITHTHEBUNDLE$NO_DRAG_BUTTON = -1;

    @Shadow
    @Final
    protected Set<Slot> quickCraftSlots;

    @Shadow
    protected boolean isQuickCrafting;

    @Shadow
    private boolean skipNextRelease;

    @Shadow
    private boolean doubleclick;

    @Shadow
    private long lastClickTime;

    @Unique
    private int backwiththebundle$bundleDragButton = BACKWITHTHEBUNDLE$NO_DRAG_BUTTON;

    @Unique
    @Nullable
    private Slot backwiththebundle$lastDragSlot;

    @Shadow
    @Nullable
    private Slot findSlot(double mouseX, double mouseY) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void slotClicked(@Nullable Slot slot, int slotId, int mouseButton, ClickType type);

    @ModifyExpressionValue(
        method = "renderTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z")
    )
    private boolean backwiththebundle$showBundleTooltipWhileCarrying(boolean carriedStackEmpty) {
        Slot hoveredSlot = ((AbstractContainerScreen<?>)(Object)this).getSlotUnderMouse();
        ItemStack hoveredStack = hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getItem();
        return BundleInteractionPolicy.shouldRenderTooltip(carriedStackEmpty, hoveredStack);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void backwiththebundle$handleBundleClick(
        @Nullable Slot slot,
        int slotId,
        int mouseButton,
        ClickType type,
        CallbackInfo callback
    ) {
        if (type == ClickType.PICKUP
            && mouseButton == 1
            && ClientConfig.BUNDLE_DRAG_ENABLED.get()
            && BundleMouseActions.handleMatchingTransfer((AbstractContainerScreen<?>)(Object)this, slot)) {
            callback.cancel();
            return;
        }

        if (BundleInteractionPolicy.shouldClearSelectionBeforeClick(type)) {
            BundleMouseActions.clearSelectionBeforeClick((AbstractContainerScreen<?>)(Object)this, slot);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void backwiththebundle$clearStaleBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (mouseButton == this.backwiththebundle$bundleDragButton) {
            this.backwiththebundle$clearBundleDragState();
            this.skipNextRelease = false;
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void backwiththebundle$dragBundleAcrossSlots(
        double mouseX,
        double mouseY,
        int mouseButton,
        double dragX,
        double dragY,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (mouseButton != 0 && mouseButton != 1) {
            return;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        Slot slot = this.findSlot(mouseX, mouseY);
        if (this.backwiththebundle$bundleDragButton == BACKWITHTHEBUNDLE$NO_DRAG_BUTTON) {
            boolean eligibleSlot = mouseButton == 0
                ? BundleMouseActions.canDragIntoBundle(screen, slot)
                : BundleMouseActions.canDragOutOfBundle(screen, slot);
            if (!ClientConfig.BUNDLE_DRAG_ENABLED.get()
                || screen.getMinecraft().options.touchscreen().get()
                || !eligibleSlot) {
                return;
            }

            this.backwiththebundle$bundleDragButton = mouseButton;
            this.backwiththebundle$suppressVanillaDragRelease();
        } else if (this.backwiththebundle$bundleDragButton != mouseButton) {
            return;
        }

        this.skipNextRelease = true;
        if (slot != this.backwiththebundle$lastDragSlot) {
            this.backwiththebundle$lastDragSlot = slot;
            if (mouseButton == 0 && BundleMouseActions.canDragIntoBundle(screen, slot)) {
                this.slotClicked(slot, slot.index, mouseButton, ClickType.PICKUP);
            } else if (mouseButton == 1 && BundleMouseActions.canDragOutOfBundle(screen, slot)) {
                if (slot.getItem().isEmpty()) {
                    this.slotClicked(slot, slot.index, mouseButton, ClickType.PICKUP);
                } else {
                    BundleMouseActions.handleMatchingTransfer(screen, slot);
                }
            }
        }

        callback.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void backwiththebundle$prepareBundleDragRelease(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (mouseButton == this.backwiththebundle$bundleDragButton) {
            this.backwiththebundle$suppressVanillaDragRelease();
        }
    }

    @Inject(method = "mouseReleased", at = @At("RETURN"))
    private void backwiththebundle$finishBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (mouseButton == this.backwiththebundle$bundleDragButton) {
            this.backwiththebundle$clearBundleDragState();
        }
    }

    @Unique
    private void backwiththebundle$suppressVanillaDragRelease() {
        this.isQuickCrafting = false;
        this.quickCraftSlots.clear();
        this.skipNextRelease = true;
        this.doubleclick = false;
        this.lastClickTime = 0L;
    }

    @Unique
    private void backwiththebundle$clearBundleDragState() {
        this.backwiththebundle$bundleDragButton = BACKWITHTHEBUNDLE$NO_DRAG_BUTTON;
        this.backwiththebundle$lastDragSlot = null;
    }
}
