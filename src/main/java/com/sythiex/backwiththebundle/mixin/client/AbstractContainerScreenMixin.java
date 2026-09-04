package com.sythiex.backwiththebundle.mixin.client;

import javax.annotation.Nullable;

import java.util.Set;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.sythiex.backwiththebundle.bundle.BundleInteractionPolicy;
import com.sythiex.backwiththebundle.client.BundleDragScreenAccess;
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
public abstract class AbstractContainerScreenMixin implements BundleDragScreenAccess {
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
    private int backwiththebundle$bundleDragCandidateButton = BACKWITHTHEBUNDLE$NO_DRAG_BUTTON;

    @Unique
    @Nullable
    private Slot backwiththebundle$bundleDragCandidateSlot;

    @Unique
    private boolean backwiththebundle$bundleDragCandidateStartedOnEmptySlot;

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
    private void backwiththebundle$prepareBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        CallbackInfoReturnable<Boolean> callback
    ) {
        this.backwiththebundle$clearBundleDragCandidate();
        if (mouseButton == this.backwiththebundle$bundleDragButton) {
            this.backwiththebundle$clearBundleDragState();
            this.skipNextRelease = false;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        boolean bundleDragEnabled = ClientConfig.BUNDLE_DRAG_ENABLED.get();
        boolean touchscreen = screen.getMinecraft().options.touchscreen().get();
        if (!bundleDragEnabled || touchscreen || (mouseButton != 0 && mouseButton != 1)) {
            return;
        }

        Slot slot = this.findSlot(mouseX, mouseY);
        boolean eligibleSlot = mouseButton == 0
            ? BundleMouseActions.canStartDragIntoBundle(screen, slot)
            : BundleMouseActions.canDragOutOfBundle(screen, slot);
        if (BundleInteractionPolicy.shouldBeginBundleDrag(
            screen.getMenu().getCarried(),
            mouseButton,
            bundleDragEnabled,
            touchscreen,
            eligibleSlot
        )) {
            this.backwiththebundle$bundleDragCandidateButton = mouseButton;
            this.backwiththebundle$bundleDragCandidateSlot = slot;
            this.backwiththebundle$bundleDragCandidateStartedOnEmptySlot = slot != null && slot.getItem().isEmpty();
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
        if (this.backwiththebundle$handleBundleDrag(mouseX, mouseY, mouseButton, dragX, dragY)) {
            callback.setReturnValue(true);
        }
    }

    @Override
    public boolean backwiththebundle$handleBundleDrag(
        double mouseX,
        double mouseY,
        int mouseButton,
        double dragX,
        double dragY
    ) {
        boolean bundleDragEnabled = ClientConfig.BUNDLE_DRAG_ENABLED.get();
        if (!bundleDragEnabled) {
            this.backwiththebundle$clearBundleDragCandidate();
            if (this.backwiththebundle$bundleDragButton != BACKWITHTHEBUNDLE$NO_DRAG_BUTTON) {
                this.backwiththebundle$clearBundleDragState();
                this.skipNextRelease = false;
            }
            return false;
        }

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        Slot slot = this.findSlot(mouseX, mouseY);
        if (this.backwiththebundle$bundleDragButton == BACKWITHTHEBUNDLE$NO_DRAG_BUTTON) {
            if (mouseButton != this.backwiththebundle$bundleDragCandidateButton
                || !BundleInteractionPolicy.shouldOwnBundleDrag(
                screen.getMenu().getCarried(),
                mouseButton,
                bundleDragEnabled,
                screen.getMinecraft().options.touchscreen().get()
            )) {
                return false;
            }

            Slot startingSlot = this.backwiththebundle$bundleDragCandidateSlot;
            boolean enteredDifferentSlot = slot != null && slot != startingSlot;
            if (BundleInteractionPolicy.shouldKeepBundleDragPending(
                mouseButton,
                this.backwiththebundle$bundleDragCandidateStartedOnEmptySlot,
                enteredDifferentSlot
            )) {
                // consume motion without suppressing release so vanilla still performs a normal pickup click
                return true;
            }

            this.backwiththebundle$bundleDragButton = mouseButton;
            this.backwiththebundle$clearBundleDragCandidate();
            this.backwiththebundle$suppressVanillaDragRelease();
            this.backwiththebundle$handleBundleDragSlot(screen, startingSlot, mouseButton);
            this.backwiththebundle$lastDragSlot = startingSlot;
        } else if (this.backwiththebundle$bundleDragButton != mouseButton) {
            return false;
        }

        this.skipNextRelease = true;
        if (slot != this.backwiththebundle$lastDragSlot) {
            this.backwiththebundle$lastDragSlot = slot;
            this.backwiththebundle$handleBundleDragSlot(screen, slot, mouseButton);
        }

        return true;
    }

    @Unique
    private void backwiththebundle$handleBundleDragSlot(
        AbstractContainerScreen<?> screen,
        @Nullable Slot slot,
        int mouseButton
    ) {
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
        if (mouseButton == this.backwiththebundle$bundleDragCandidateButton) {
            this.backwiththebundle$clearBundleDragCandidate();
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

    @Unique
    private void backwiththebundle$clearBundleDragCandidate() {
        this.backwiththebundle$bundleDragCandidateButton = BACKWITHTHEBUNDLE$NO_DRAG_BUTTON;
        this.backwiththebundle$bundleDragCandidateSlot = null;
        this.backwiththebundle$bundleDragCandidateStartedOnEmptySlot = false;
    }
}
