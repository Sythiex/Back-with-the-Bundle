package com.sythiex.backwiththebundle.registration;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BundleContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ResourceLocation.DEFAULT_NAMESPACE);

    public static final DeferredItem<BundleItem> WHITE_BUNDLE = registerBundle("white_bundle");
    public static final DeferredItem<BundleItem> ORANGE_BUNDLE = registerBundle("orange_bundle");
    public static final DeferredItem<BundleItem> MAGENTA_BUNDLE = registerBundle("magenta_bundle");
    public static final DeferredItem<BundleItem> LIGHT_BLUE_BUNDLE = registerBundle("light_blue_bundle");
    public static final DeferredItem<BundleItem> YELLOW_BUNDLE = registerBundle("yellow_bundle");
    public static final DeferredItem<BundleItem> LIME_BUNDLE = registerBundle("lime_bundle");
    public static final DeferredItem<BundleItem> PINK_BUNDLE = registerBundle("pink_bundle");
    public static final DeferredItem<BundleItem> GRAY_BUNDLE = registerBundle("gray_bundle");
    public static final DeferredItem<BundleItem> LIGHT_GRAY_BUNDLE = registerBundle("light_gray_bundle");
    public static final DeferredItem<BundleItem> CYAN_BUNDLE = registerBundle("cyan_bundle");
    public static final DeferredItem<BundleItem> PURPLE_BUNDLE = registerBundle("purple_bundle");
    public static final DeferredItem<BundleItem> BLUE_BUNDLE = registerBundle("blue_bundle");
    public static final DeferredItem<BundleItem> BROWN_BUNDLE = registerBundle("brown_bundle");
    public static final DeferredItem<BundleItem> GREEN_BUNDLE = registerBundle("green_bundle");
    public static final DeferredItem<BundleItem> RED_BUNDLE = registerBundle("red_bundle");
    public static final DeferredItem<BundleItem> BLACK_BUNDLE = registerBundle("black_bundle");

    public static final List<DeferredItem<BundleItem>> DYED_BUNDLES_IN_CREATIVE_ORDER = List.of(
        WHITE_BUNDLE,
        LIGHT_GRAY_BUNDLE,
        GRAY_BUNDLE,
        BLACK_BUNDLE,
        BROWN_BUNDLE,
        RED_BUNDLE,
        ORANGE_BUNDLE,
        YELLOW_BUNDLE,
        LIME_BUNDLE,
        GREEN_BUNDLE,
        CYAN_BUNDLE,
        LIGHT_BLUE_BUNDLE,
        BLUE_BUNDLE,
        PURPLE_BUNDLE,
        MAGENTA_BUNDLE,
        PINK_BUNDLE
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private static DeferredItem<BundleItem> registerBundle(String name) {
        return ITEMS.registerItem(name, BundleItem::new, bundleProperties());
    }

    private static Item.Properties bundleProperties() {
        return new Item.Properties()
            .stacksTo(1)
            .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
    }
}
