package com.sythiex.backwiththebundle.compat.ipn.mixin;

import com.sythiex.backwiththebundle.compat.ipn.InventoryProfilesNextMixinPlugin;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = InventoryProfilesNextMixinPlugin.ITEM_TYPE, remap = false)
public interface ItemTypeAccessor {
    @Invoker("getItem")
    Item backwiththebundle$getItem();
}
