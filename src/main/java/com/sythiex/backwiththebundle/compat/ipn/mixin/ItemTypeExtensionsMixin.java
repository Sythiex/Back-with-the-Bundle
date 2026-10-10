package com.sythiex.backwiththebundle.compat.ipn.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.sythiex.backwiththebundle.compat.ipn.InventoryProfilesNextMixinPlugin;
import net.minecraft.world.item.BundleItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = InventoryProfilesNextMixinPlugin.EXTENSIONS, remap = false)
public abstract class ItemTypeExtensionsMixin {
    @ModifyReturnValue(
        method = InventoryProfilesNextMixinPlugin.BUNDLE_SELECTOR,
        at = @At("RETURN"),
        require = 0
    )
    private static boolean backwiththebundle$recognizeBundle(boolean original, @Coerce Object itemType) {
        return original || ((ItemTypeAccessor) itemType).backwiththebundle$getItem() instanceof BundleItem;
    }
}
