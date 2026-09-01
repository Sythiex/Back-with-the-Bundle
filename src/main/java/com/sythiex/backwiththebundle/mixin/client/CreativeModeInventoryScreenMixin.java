package com.sythiex.backwiththebundle.mixin.client;

import javax.annotation.Nullable;

import com.sythiex.backwiththebundle.bundle.BundleInteractionPolicy;
import com.sythiex.backwiththebundle.client.BundleMouseActions;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
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
