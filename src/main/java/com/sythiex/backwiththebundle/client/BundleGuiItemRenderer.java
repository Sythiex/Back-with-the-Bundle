package com.sythiex.backwiththebundle.client;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sythiex.backwiththebundle.bundle.BundleSelection;
import com.sythiex.backwiththebundle.registration.ModItems;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BundleGuiItemRenderer {
    private static final String OPEN_BACK_SUFFIX = "_open_back";
    private static final String OPEN_FRONT_SUFFIX = "_open_front";

    private static final List<ResourceLocation> SUPPORTED_BUNDLE_IDS = Stream.concat(
        Stream.of(ResourceLocation.withDefaultNamespace("bundle")),
        ModItems.DYED_BUNDLES_IN_CREATIVE_ORDER.stream().map(holder -> holder.getId())
    ).toList();
    private static final Set<ResourceLocation> SUPPORTED_BUNDLE_ID_SET = Set.copyOf(SUPPORTED_BUNDLE_IDS);
    private static final List<ModelResourceLocation> OPEN_MODEL_LOCATIONS = SUPPORTED_BUNDLE_IDS.stream()
        .flatMap(id -> Stream.of(openBackModel(id), openFrontModel(id)))
        .toList();

    private BundleGuiItemRenderer() {
    }

    public static boolean renderOpenBundle(
        ItemRenderer itemRenderer,
        ItemStack bundle,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int combinedLight,
        int combinedOverlay,
        @Nullable Level level,
        @Nullable LivingEntity entity,
        int seed
    ) {
        if (!shouldRenderOpen(bundle, displayContext)) {
            return false;
        }

        ItemStack selectedStack = BundleSelection.getSelectedItemStack(bundle);
        ModelManager modelManager = itemRenderer.getItemModelShaper().getModelManager();
        ResourceLocation bundleId = BuiltInRegistries.ITEM.getKey(bundle.getItem());
        BakedModel openBack = modelManager.getModel(openBackModel(bundleId));
        BakedModel openFront = modelManager.getModel(openFrontModel(bundleId));
        BakedModel missingModel = modelManager.getMissingModel();
        if (openBack == missingModel || openFront == missingModel) {
            return false;
        }

        itemRenderer.render(
            bundle,
            displayContext,
            leftHand,
            poseStack,
            bufferSource,
            combinedLight,
            combinedOverlay,
            openBack
        );
        BakedModel selectedModel = itemRenderer.getModel(selectedStack, level, entity, seed);
        itemRenderer.render(
            selectedStack,
            displayContext,
            leftHand,
            poseStack,
            bufferSource,
            combinedLight,
            combinedOverlay,
            selectedModel
        );
        itemRenderer.render(
            bundle,
            displayContext,
            leftHand,
            poseStack,
            bufferSource,
            combinedLight,
            combinedOverlay,
            openFront
        );
        return true;
    }

    static boolean shouldRenderOpen(ItemStack stack, ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.GUI
            && isSupportedBundle(BuiltInRegistries.ITEM.getKey(stack.getItem()))
            && !BundleSelection.getSelectedItemStack(stack).isEmpty();
    }

    static boolean isSupportedBundle(ResourceLocation itemId) {
        return SUPPORTED_BUNDLE_ID_SET.contains(itemId);
    }

    static List<ResourceLocation> supportedBundleIds() {
        return SUPPORTED_BUNDLE_IDS;
    }

    static List<ModelResourceLocation> openModelLocations() {
        return OPEN_MODEL_LOCATIONS;
    }

    static ModelResourceLocation openBackModel(ResourceLocation itemId) {
        return openModel(itemId, OPEN_BACK_SUFFIX);
    }

    static ModelResourceLocation openFrontModel(ResourceLocation itemId) {
        return openModel(itemId, OPEN_FRONT_SUFFIX);
    }

    private static ModelResourceLocation openModel(ResourceLocation itemId, String suffix) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(
            itemId.getNamespace(),
            "item/" + itemId.getPath() + suffix
        );
        return ModelResourceLocation.standalone(modelId);
    }
}
