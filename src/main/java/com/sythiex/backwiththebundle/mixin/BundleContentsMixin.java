package com.sythiex.backwiththebundle.mixin;

import com.sythiex.backwiththebundle.bundle.BundleContentsSelectionAccess;

import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BundleContents.class)
public abstract class BundleContentsMixin implements BundleContentsSelectionAccess {
    @Unique
    private int backwiththebundle$selectedItem = -1;

    @Override
    public int backwiththebundle$getSelectedItem() {
        return this.backwiththebundle$selectedItem;
    }

    @Override
    public void backwiththebundle$setSelectedItem(int selectedItem) {
        this.backwiththebundle$selectedItem = selectedItem;
    }
}
