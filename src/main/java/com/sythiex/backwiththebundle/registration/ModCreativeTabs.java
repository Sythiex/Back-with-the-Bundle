package com.sythiex.backwiththebundle.registration;

import java.util.Set;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModCreativeTabs {
    private ModCreativeTabs() {
    }

    public static void addBundleVariants(BuildCreativeModeTabContentsEvent event) {
        if (!CreativeModeTabs.TOOLS_AND_UTILITIES.equals(event.getTabKey())) {
            return;
        }

        addBundles(event, event.getParentEntries(), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        addBundles(event, event.getSearchEntries(), CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }

    private static void addBundles(
        BuildCreativeModeTabContentsEvent event,
        Set<ItemStack> entries,
        CreativeModeTab.TabVisibility visibility
    ) {
        ItemStack previous = new ItemStack(Items.BUNDLE);
        if (!entries.contains(previous)) {
            ItemStack lead = new ItemStack(Items.LEAD);
            if (entries.contains(lead)) {
                event.insertAfter(lead, previous, visibility);
            } else {
                event.accept(previous, visibility);
            }
        }

        for (var bundle : ModItems.DYED_BUNDLES_IN_CREATIVE_ORDER) {
            ItemStack current = new ItemStack(bundle.get());
            event.insertAfter(previous, current, visibility);
            previous = current;
        }
    }
}
