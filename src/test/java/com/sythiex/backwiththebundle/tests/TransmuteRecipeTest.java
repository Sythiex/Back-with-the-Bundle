package com.sythiex.backwiththebundle.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sythiex.backwiththebundle.recipe.TransmuteRecipe;
import com.sythiex.backwiththebundle.registration.ModItems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;

class TransmuteRecipeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
    }

    @Test
    void serializerAcceptsInlineAndObjectResults() {
        TransmuteRecipe.Serializer serializer = new TransmuteRecipe.Serializer();

        TransmuteRecipe inline = serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString("""
            {
              "category": "equipment",
              "input": "minecraft:bundle",
              "material": "minecraft:white_dye",
              "result": "minecraft:white_wool"
            }
            """)).getOrThrow();
        assertEquals(Items.WHITE_WOOL, inline.result().getItem());
        assertEquals(1, inline.result().getCount());

        TransmuteRecipe object = serializer.codec().codec().parse(JsonOps.INSTANCE, JsonParser.parseString("""
            {
              "category": "equipment",
              "input": "#minecraft:bundles",
              "material": "minecraft:white_dye",
              "result": {
                "id": "minecraft:white_bundle",
                "count": 1,
                "components": {
                  "minecraft:custom_model_data": 42
                }
              }
            }
            """)).getOrThrow();
        assertEquals(ModItems.WHITE_BUNDLE.get(), object.result().getItem());
        assertEquals(1, object.result().getCount());
        assertEquals(42, object.result().get(DataComponents.CUSTOM_MODEL_DATA).value());
    }

    @Test
    void outputPreservesInputComponentsThenAppliesResultPatchAndCount() {
        ItemStack result = new ItemStack(Items.WHITE_WOOL, 2);
        result.set(DataComponents.CUSTOM_NAME, Component.literal("Patched result"));
        TransmuteRecipe recipe = new TransmuteRecipe(
            "bundle_dye",
            CraftingBookCategory.EQUIPMENT,
            Ingredient.of(Items.BUNDLE),
            Ingredient.of(Items.WHITE_DYE),
            result
        );

        ItemStack source = new ItemStack(Items.BUNDLE);
        source.set(DataComponents.REPAIR_COST, 7);
        CraftingInput input = CraftingInput.of(2, 1, List.of(source, new ItemStack(Items.WHITE_DYE)));

        assertTrue(recipe.matches(input, null));
        ItemStack output = recipe.assemble(input, RegistryAccess.EMPTY);
        assertEquals(Items.WHITE_WOOL, output.getItem());
        assertEquals(2, output.getCount());
        assertEquals(7, output.get(DataComponents.REPAIR_COST));
        assertEquals(Component.literal("Patched result"), output.get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void matchingRejectsTheTargetItemAndExtraInputs() {
        TransmuteRecipe recipe = new TransmuteRecipe(
            "bundle_dye",
            CraftingBookCategory.EQUIPMENT,
            Ingredient.of(Items.BUNDLE, Items.WHITE_WOOL),
            Ingredient.of(Items.WHITE_DYE),
            new ItemStack(Items.WHITE_WOOL)
        );

        assertFalse(recipe.matches(
            CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.WHITE_DYE))),
            null
        ));
        assertFalse(recipe.matches(
            CraftingInput.of(3, 1, List.of(new ItemStack(Items.BUNDLE), new ItemStack(Items.WHITE_DYE), new ItemStack(Items.STICK))),
            null
        ));
    }

    @Test
    void displayedInputOmitsTheTargetItem() {
        TransmuteRecipe recipe = new TransmuteRecipe(
            "bundle_dye",
            CraftingBookCategory.EQUIPMENT,
            Ingredient.of(Items.BUNDLE, Items.WHITE_WOOL),
            Ingredient.of(Items.WHITE_DYE),
            new ItemStack(Items.WHITE_WOOL)
        );

        Ingredient displayedInput = recipe.getIngredients().get(0);
        assertTrue(displayedInput.test(new ItemStack(Items.BUNDLE)));
        assertFalse(displayedInput.test(new ItemStack(Items.WHITE_WOOL)));
    }
}
