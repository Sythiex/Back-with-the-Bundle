package com.sythiex.backwiththebundle.compat.ipn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.ElementNode;

class InventoryProfilesNextMixinPluginTest {
    private static final Map<String, String> VERSIONS = Map.of(
        "inventoryprofilesnext", "2.2.5",
        "backwiththebundle", "1.0.0"
    );

    @Test
    void absentIpnAndDedicatedServerNeverInspectOptionalClasses() {
        var plugin = new InventoryProfilesNextMixinPlugin();
        InventoryProfilesNextMixinPlugin.BytecodeLookup unexpectedLookup = name -> {
            throw new AssertionError("Must not inspect " + name);
        };
        plugin.initialize(true, Map.of(), unexpectedLookup);
        assertFalse(applies(plugin));
        plugin.initialize(false, VERSIONS, unexpectedLookup);
        assertFalse(applies(plugin));
    }

    @Test
    void acceptsCompatibleReleasesAndAlreadyModifiedDetectorBodies() {
        var extensions = extensions();
        // Another mod / newer IPN already returning true must not disable our additive hook.
        extensions.methods.getFirst().instructions.set(
            extensions.methods.getFirst().instructions.getFirst(), new InsnNode(Opcodes.ICONST_1));
        var plugin = new InventoryProfilesNextMixinPlugin();
        plugin.initialize(true, Map.of("inventoryprofilesnext", "99.0.0"), name ->
            name.equals(InventoryProfilesNextMixinPlugin.EXTENSIONS) ? extensions : itemType());
        assertTrue(applies(plugin));
    }

    @Test
    void missingClassesAndInspectionFailuresDisableBothMixins() {
        var missing = new InventoryProfilesNextMixinPlugin();
        missing.initialize(true, VERSIONS, name -> { throw new ClassNotFoundException(name); });
        assertFalse(applies(missing));
        var unreadable = new InventoryProfilesNextMixinPlugin();
        unreadable.initialize(true, VERSIONS, name -> { throw new IOException("Unreadable IPN jar"); });
        assertFalse(applies(unreadable));
    }

    @Test
    void changedSignatureStaticnessAndOverloadsDisableTheExactNameSelector() {
        ClassNode changedDescriptor = extensions();
        changedDescriptor.methods.getFirst().desc = "(Ljava/lang/Object;)Z";
        assertDisabled(changedDescriptor, itemType());

        ClassNode instanceMethod = extensions();
        instanceMethod.methods.getFirst().access &= ~Opcodes.ACC_STATIC;
        assertDisabled(instanceMethod, itemType());

        ClassNode overloaded = extensions();
        overloaded.methods.add(new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            InventoryProfilesNextMixinPlugin.BUNDLE_METHOD, "()Z", null, null));
        assertDisabled(overloaded, itemType());

        ClassNode missingMethod = extensions();
        missingMethod.methods.clear();
        assertDisabled(missingMethod, itemType());
    }

    @Test
    void missingAccessorOrReturnInstructionDisablesCompatibility() {
        ClassNode missingGetter = itemType();
        missingGetter.methods.clear();
        assertDisabled(extensions(), missingGetter);

        ClassNode staticGetter = itemType();
        staticGetter.methods.getFirst().access |= Opcodes.ACC_STATIC;
        assertDisabled(extensions(), staticGetter);

        ClassNode noReturn = extensions();
        noReturn.methods.getFirst().instructions.clear();
        assertDisabled(noReturn, itemType());
    }

    @Test
    void selectorMatchesLiteralKotlinNameAndNotOtherBundleHelpers() {
        var selector = TargetSelector.parse(InventoryProfilesNextMixinPlugin.BUNDLE_SELECTOR, null).validate();
        ClassNode owner = extensions();
        assertTrue(selector.match(ElementNode.of(owner, owner.methods.getFirst())).isExactMatch());
        MethodNode differentHelper = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "get(isBundleAndHasItems)", InventoryProfilesNextMixinPlugin.BUNDLE_DESCRIPTOR, null, null);
        assertFalse(selector.match(ElementNode.of(owner, differentHelper)).isMatch());
    }

    @Test
    void warnsOnceWithVersionsAndHookDetailsWhenInjectionDisappears() {
        var logger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
            .getLogger("Back with the Bundle/IPN compat");
        var messages = new java.util.ArrayList<String>();
        var appender = new org.apache.logging.log4j.core.appender.AbstractAppender(
            "ipn-compat-test", null, null, true, org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY
        ) {
            @Override
            public void append(org.apache.logging.log4j.core.LogEvent event) {
                if (event.getLevel() == org.apache.logging.log4j.Level.WARN) {
                    messages.add(event.getMessage().getFormattedMessage());
                }
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            var plugin = new InventoryProfilesNextMixinPlugin();
            plugin.initialize(true, VERSIONS, name ->
                name.equals(InventoryProfilesNextMixinPlugin.EXTENSIONS) ? extensions() : itemType());
            plugin.postApply(InventoryProfilesNextMixinPlugin.EXTENSIONS, extensions(), "unused", null);
            plugin.postApply(InventoryProfilesNextMixinPlugin.EXTENSIONS, extensions(), "unused", null);
            assertFalse(applies(plugin));
            org.junit.jupiter.api.Assertions.assertEquals(1, messages.size());
            String warning = messages.getFirst();
            assertTrue(warning.contains("Inventory Profiles Next 2.2.5"));
            assertTrue(warning.contains("Back with the Bundle 1.0.0"));
            assertTrue(warning.contains(InventoryProfilesNextMixinPlugin.BUNDLE_METHOD
                + InventoryProfilesNextMixinPlugin.BUNDLE_DESCRIPTOR));
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
    }

    private static void assertDisabled(ClassNode extensions, ClassNode itemType) {
        var plugin = new InventoryProfilesNextMixinPlugin();
        plugin.initialize(true, VERSIONS, name ->
            name.equals(InventoryProfilesNextMixinPlugin.EXTENSIONS) ? extensions : itemType);
        assertFalse(applies(plugin));
    }

    private static boolean applies(InventoryProfilesNextMixinPlugin plugin) {
        return plugin.shouldApplyMixin(InventoryProfilesNextMixinPlugin.EXTENSIONS, "ItemTypeExtensionsMixin")
            || plugin.shouldApplyMixin(InventoryProfilesNextMixinPlugin.ITEM_TYPE, "ItemTypeAccessor");
    }

    private static ClassNode extensions() {
        var node = new ClassNode();
        node.name = InventoryProfilesNextMixinPlugin.EXTENSIONS.replace('.', '/');
        var method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            InventoryProfilesNextMixinPlugin.BUNDLE_METHOD, InventoryProfilesNextMixinPlugin.BUNDLE_DESCRIPTOR, null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.methods.add(method);
        return node;
    }

    private static ClassNode itemType() {
        var node = new ClassNode();
        node.name = InventoryProfilesNextMixinPlugin.ITEM_TYPE.replace('.', '/');
        node.methods.add(new MethodNode(Opcodes.ACC_PUBLIC, "getItem", "()Lnet/minecraft/world/item/Item;", null, null));
        return node;
    }
}
