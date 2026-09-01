package com.sythiex.backwiththebundle.recipe;

import java.util.Arrays;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sythiex.backwiththebundle.registration.ModRecipeSerializers;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Backport of the general crafting transmute recipe introduced with dyed bundles.
 */
public final class TransmuteRecipe implements CraftingRecipe {
    private final String group;
    private final CraftingBookCategory category;
    private final Ingredient input;
    private final Ingredient material;
    private final ItemStack result;

    public TransmuteRecipe(
        String group,
        CraftingBookCategory category,
        Ingredient input,
        Ingredient material,
        ItemStack result
    ) {
        this.group = group;
        this.category = category;
        this.input = input;
        this.material = material;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput craftingInput, Level level) {
        if (craftingInput.ingredientCount() != 2) {
            return false;
        }

        boolean foundInput = false;
        boolean foundMaterial = false;
        for (int slot = 0; slot < craftingInput.size(); slot++) {
            ItemStack stack = craftingInput.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (!foundInput && input.test(stack) && !stack.is(result.getItem())) {
                foundInput = true;
            } else if (!foundMaterial && material.test(stack)) {
                foundMaterial = true;
            } else {
                return false;
            }
        }

        return foundInput && foundMaterial;
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider registries) {
        ItemStack source = ItemStack.EMPTY;
        for (int slot = 0; slot < craftingInput.size(); slot++) {
            ItemStack stack = craftingInput.getItem(slot);
            if (!stack.isEmpty() && input.test(stack) && !stack.is(result.getItem())) {
                source = stack;
                break;
            }
        }

        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack output = source.transmuteCopy(result.getItem(), result.getCount());
        output.applyComponents(result.getComponentsPatch());
        return output;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient displayInput = Ingredient.of(
            Arrays.stream(input.getItems())
                .filter(stack -> !stack.is(result.getItem()))
                .map(ItemStack::copy)
        );
        return NonNullList.of(Ingredient.EMPTY, displayInput, material);
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.CRAFTING_TRANSMUTE.get();
    }

    public Ingredient input() {
        return input;
    }

    public Ingredient material() {
        return material;
    }

    public ItemStack result() {
        return result;
    }

    public static final class Serializer implements RecipeSerializer<TransmuteRecipe> {
        private static final Codec<Ingredient> INLINE_INGREDIENT_CODEC = Codec.STRING.flatXmap(
            Serializer::parseInlineIngredient,
            ingredient -> DataResult.error(() -> "Inline ingredient encoding is decode-only")
        );
        private static final Codec<Ingredient> INGREDIENT_CODEC = Codec.either(INLINE_INGREDIENT_CODEC, Ingredient.CODEC_NONEMPTY)
            .xmap(either -> either.map(ingredient -> ingredient, ingredient -> ingredient), Either::right);
        private static final Codec<ItemStack> RESULT_CODEC = Codec.either(ItemStack.SIMPLE_ITEM_CODEC, ItemStack.STRICT_CODEC)
            .xmap(either -> either.map(stack -> stack, stack -> stack), Either::right);

        private static final MapCodec<TransmuteRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(TransmuteRecipe::getGroup),
            CraftingBookCategory.CODEC
                .fieldOf("category")
                .orElse(CraftingBookCategory.MISC)
                .forGetter(TransmuteRecipe::category),
            INGREDIENT_CODEC.fieldOf("input").forGetter(TransmuteRecipe::input),
            INGREDIENT_CODEC.fieldOf("material").forGetter(TransmuteRecipe::material),
            RESULT_CODEC.fieldOf("result").forGetter(TransmuteRecipe::result)
        ).apply(instance, TransmuteRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> STREAM_CODEC = StreamCodec.of(
            Serializer::toNetwork,
            Serializer::fromNetwork
        );

        @Override
        public MapCodec<TransmuteRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TransmuteRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static DataResult<Ingredient> parseInlineIngredient(String value) {
            boolean isTag = value.startsWith("#");
            String idText = isTag ? value.substring(1) : value;
            ResourceLocation id = ResourceLocation.tryParse(idText);
            if (id == null) {
                return DataResult.error(() -> "Invalid ingredient id: " + value);
            }
            if (isTag) {
                return DataResult.success(Ingredient.of(TagKey.create(Registries.ITEM, id)));
            }

            return BuiltInRegistries.ITEM.getOptional(id)
                .filter(item -> item != Items.AIR)
                .<DataResult<Ingredient>>map(item -> DataResult.success(Ingredient.of(item)))
                .orElseGet(() -> DataResult.error(() -> "Unknown ingredient item: " + id));
        }

        private static TransmuteRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            Ingredient material = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            return new TransmuteRecipe(group, category, input, material, result);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, TransmuteRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeEnum(recipe.category);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.material);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    }
}
