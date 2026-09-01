package com.sythiex.backwiththebundle.registration;

import com.sythiex.backwiththebundle.recipe.TransmuteRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(
        Registries.RECIPE_SERIALIZER,
        ResourceLocation.DEFAULT_NAMESPACE
    );

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TransmuteRecipe>> CRAFTING_TRANSMUTE =
        SERIALIZERS.register("crafting_transmute", TransmuteRecipe.Serializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
