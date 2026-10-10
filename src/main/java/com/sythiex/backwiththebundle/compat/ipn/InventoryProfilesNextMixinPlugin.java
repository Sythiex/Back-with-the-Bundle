package com.sythiex.backwiththebundle.compat.ipn;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class InventoryProfilesNextMixinPlugin implements IMixinConfigPlugin {
    public static final String ITEM_TYPE = "org.anti_ad.mc.ipnext.item.ItemType";
    public static final String EXTENSIONS = "org.anti_ad.mc.ipnext.item.ItemTypeExtensionsKt";
    public static final String BUNDLE_METHOD = "get(isBundle)";
    public static final String BUNDLE_DESCRIPTOR = "(Lorg/anti_ad/mc/ipnext/item/ItemType;)Z";
    // The parentheses are part of the original method name. The checks in this class disable
    // the compat mixin if IPN adds overloads later so that we don't target the wrong method.
    public static final String BUNDLE_SELECTOR = "/^get\\(isBundle\\)$/";
    private static final String GET_ITEM_DESCRIPTOR = "()Lnet/minecraft/world/item/Item;";
    private static final Logger LOGGER = LoggerFactory.getLogger("Back with the Bundle/IPN compat");

    private boolean enabled;
    private boolean warned;
    private boolean detectorApplied;
    private boolean accessorApplied;
    private String versions;

    @Override
    public void onLoad(String mixinPackage) {
        if (FMLLoader.getDist() != Dist.CLIENT) {
            return;
        }
        Map<String, String> loadedVersions = FMLLoader.getLoadingModList().getMods().stream()
            .collect(Collectors.toMap(mod -> mod.getModId(), mod -> mod.getVersion().toString()));
        initialize(true, loadedVersions, name -> MixinService.getService().getBytecodeProvider().getClassNode(name));
    }

    void initialize(boolean client, Map<String, String> loadedVersions, BytecodeLookup lookup) {
        if (!client || !loadedVersions.containsKey("inventoryprofilesnext")) {
            return;
        }
        versions = "Inventory Profiles Next " + loadedVersions.get("inventoryprofilesnext")
            + ", Back with the Bundle " + loadedVersions.getOrDefault("backwiththebundle", "unknown");
        try {
            String problem = inspectTargets(lookup);
            if (problem != null) {
                warnDisabled(problem);
                return;
            }
            enabled = true;
        } catch (ClassNotFoundException | IOException | RuntimeException exception) {
            warnDisabled("Cannot inspect " + EXTENSIONS + "." + BUNDLE_METHOD + BUNDLE_DESCRIPTOR
                + " and " + ITEM_TYPE + ".getItem" + GET_ITEM_DESCRIPTOR + ": " + exception);
        }
    }

    private static String inspectTargets(BytecodeLookup lookup) throws ClassNotFoundException, IOException {
        ClassNode extensions = lookup.getClassNode(EXTENSIONS);
        List<MethodNode> candidates = extensions.methods.stream()
            .filter(method -> method.name.equals(BUNDLE_METHOD)).toList();
        if (candidates.size() != 1 || !isConcreteMethod(candidates.getFirst(), BUNDLE_DESCRIPTOR, true)) {
            return "Expected one concrete static " + EXTENSIONS + "." + BUNDLE_METHOD + BUNDLE_DESCRIPTOR
                + "; found " + candidates.stream().map(method -> method.name + method.desc + " access=" + method.access).toList();
        }
        boolean hasReturn = false;
        for (var instruction : candidates.getFirst().instructions) {
            hasReturn |= instruction.getOpcode() == Opcodes.IRETURN;
        }
        if (!hasReturn) {
            return "No boolean return instruction in " + EXTENSIONS + "." + BUNDLE_METHOD + BUNDLE_DESCRIPTOR;
        }
        ClassNode itemType = lookup.getClassNode(ITEM_TYPE);
        if (itemType.methods.stream().noneMatch(method -> method.name.equals("getItem")
            && isConcreteMethod(method, GET_ITEM_DESCRIPTOR, false))) {
            return "Missing concrete instance accessor " + ITEM_TYPE + ".getItem" + GET_ITEM_DESCRIPTOR;
        }
        return null;
    }

    private static boolean isConcreteMethod(MethodNode method, String descriptor, boolean isStatic) {
        return method.desc.equals(descriptor)
            && ((method.access & Opcodes.ACC_STATIC) != 0) == isStatic
            && (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
    }

    private void warnDisabled(String reason) {
        enabled = false;
        if (!warned) {
            warned = true;
            LOGGER.warn("Bundle sorting compatibility disabled for {}. {}.", versions, reason);
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return enabled;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!enabled) {
            return;
        }
        if (targetClassName.equals(EXTENSIONS)) {
            for (MethodNode method : targetClass.methods) {
                if (method.name.equals(BUNDLE_METHOD) && method.desc.equals(BUNDLE_DESCRIPTOR)) {
                    for (var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode call
                            && call.owner.equals(targetClass.name)
                            && call.name.contains("backwiththebundle$recognizeBundle")) {
                            detectorApplied = true;
                        }
                    }
                }
            }
            if (!detectorApplied) {
                warnDisabled("Return-value hook did not apply to " + EXTENSIONS + "." + BUNDLE_METHOD + BUNDLE_DESCRIPTOR
                    + "; another transformer may have changed the target");
            }
        } else if (targetClassName.equals(ITEM_TYPE)) {
            accessorApplied = true;
        }
        if (enabled && detectorApplied && accessorApplied) {
            LOGGER.info("Bundle sorting compatibility applied for {}", versions);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @FunctionalInterface
    interface BytecodeLookup {
        ClassNode getClassNode(String name) throws ClassNotFoundException, IOException;
    }
}
