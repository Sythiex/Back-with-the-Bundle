package com.sythiex.backwiththebundle.mixin.client;

import javax.annotation.Nullable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.sythiex.backwiththebundle.bundle.BundleInteractionPolicy;
import com.sythiex.backwiththebundle.client.BundleMouseActions;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @ModifyExpressionValue(
        method = "renderTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z")
    )
    private boolean backwiththebundle$showBundleTooltipWhileCarrying(boolean carriedStackEmpty) {
        Slot hoveredSlot = ((AbstractContainerScreen<?>)(Object)this).getSlotUnderMouse();
        ItemStack hoveredStack = hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getItem();
        return BundleInteractionPolicy.shouldRenderTooltip(carriedStackEmpty, hoveredStack);
    }

    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void backwiththebundle$clearSelectionBeforeClick(
        @Nullable Slot slot,
        int slotId,
        int mouseButton,
        ClickType type,
        CallbackInfo callback
    ) {
        if (BundleInteractionPolicy.shouldClearSelectionBeforeClick(type)) {
            BundleMouseActions.clearSelectionBeforeClick((AbstractContainerScreen<?>)(Object)this, slot);
        }
    }
}
