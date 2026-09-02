package com.sythiex.backwiththebundle.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec.BooleanValue EXPAND_BUNDLE_TOOLTIP;
    public static final ModConfigSpec SPEC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        EXPAND_BUNDLE_TOOLTIP = builder
            .comment("Show every bundle entry in the tooltip and allow scrolling through all of them.")
            .translation("backwiththebundle.configuration.expandBundleTooltip")
            .define("expandBundleTooltip", true);
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
