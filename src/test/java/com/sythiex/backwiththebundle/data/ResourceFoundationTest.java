package com.sythiex.backwiththebundle.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ResourceFoundationTest {
    private static final List<String> COLORS = List.of(
        "white",
        "orange",
        "magenta",
        "light_blue",
        "yellow",
        "lime",
        "pink",
        "gray",
        "light_gray",
        "cyan",
        "purple",
        "blue",
        "brown",
        "green",
        "red",
        "black"
    );

    private static final List<String> VILLAGE_TABLES = List.of(
        "village_cartographer",
        "village_tannery",
        "village_weaponsmith",
        "village_plains_house",
        "village_desert_house",
        "village_snowy_house",
        "village_savanna_house",
        "village_taiga_house"
    );

    private static final List<String> BUNDLE_TOOLTIP_SPRITES = List.of(
        "slot_background",
        "slot_highlight_back",
        "slot_highlight_front",
        "bundle_progressbar_border",
        "bundle_progressbar_fill",
        "bundle_progressbar_full"
    );

    private static final List<String> BUNDLES = Stream.concat(
        Stream.of("bundle"),
        COLORS.stream().map(color -> color + "_bundle")
    ).toList();

    @Test
    void bundleTagContainsTheBaseAndAllSixteenVariants() throws IOException {
        JsonArray values = json("data/minecraft/tags/item/bundles.json").getAsJsonObject().getAsJsonArray("values");
        Set<String> ids = new HashSet<>();
        values.forEach(value -> ids.add(value.getAsString()));

        assertEquals(17, ids.size());
        assertTrue(ids.contains("minecraft:bundle"));
        COLORS.forEach(color -> assertTrue(ids.contains("minecraft:" + color + "_bundle")));
    }

    @Test
    void everyRegisteredVariantHasModelTextureRecipeAndUnlockAdvancement() throws IOException {
        for (String color : COLORS) {
            String item = color + "_bundle";
            JsonObject model = json("assets/minecraft/models/item/" + item + ".json").getAsJsonObject();
            assertEquals("minecraft:item/" + item, model.getAsJsonObject("textures").get("layer0").getAsString());
            assertResource("assets/minecraft/textures/item/" + item + ".png");

            JsonObject recipe = json("data/minecraft/recipe/" + item + ".json").getAsJsonObject();
            assertEquals("minecraft:crafting_transmute", recipe.get("type").getAsString());
            assertEquals("#minecraft:bundles", recipe.get("input").getAsString());
            assertEquals("minecraft:" + color + "_dye", recipe.get("material").getAsString());
            assertEquals("minecraft:" + item, recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());

            assertResource("data/minecraft/advancement/recipes/tools/" + item + ".json");
        }
    }

    @Test
    void everyBundleHasClosedAndLayeredOpenModelsAndTextures() throws IOException {
        for (String item : BUNDLES) {
            JsonObject closedModel = json("assets/minecraft/models/item/" + item + ".json").getAsJsonObject();
            assertEquals("minecraft:item/generated", closedModel.get("parent").getAsString());
            assertEquals(
                "minecraft:item/" + item,
                closedModel.getAsJsonObject("textures").get("layer0").getAsString()
            );
            assertResource("assets/minecraft/textures/item/" + item + ".png");

            for (String part : List.of("back", "front")) {
                String openItem = item + "_open_" + part;
                JsonObject openModel = json("assets/minecraft/models/item/" + openItem + ".json").getAsJsonObject();
                assertEquals("minecraft:item/template_bundle_open_" + part, openModel.get("parent").getAsString());
                assertEquals(
                    "minecraft:item/" + openItem,
                    openModel.getAsJsonObject("textures").get("layer0").getAsString()
                );
                assertResource("assets/minecraft/textures/item/" + openItem + ".png");
            }
        }

        JsonObject baseModel = json("assets/minecraft/models/item/bundle.json").getAsJsonObject();
        assertFalse(baseModel.has("overrides"));
    }

    @Test
    void openBundleTemplatesSeparateTheBackAndFrontLayersInGuiSpace() throws IOException {
        assertBundleTemplateTranslation("back", -16);
        assertBundleTemplateTranslation("front", 16);
    }

    @Test
    void baseBundleRecipeUsesStringOverLeather() throws IOException {
        JsonObject recipe = json("data/minecraft/recipe/bundle.json").getAsJsonObject();
        assertEquals(List.of("-", "#"), recipe.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
        assertEquals("minecraft:string", recipe.getAsJsonObject("key").getAsJsonObject("-").get("item").getAsString());
        assertEquals("minecraft:leather", recipe.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
    }

    @Test
    void insertFailSoundHasDefinitionTranslationAndAudio() throws IOException {
        JsonObject sounds = json("assets/minecraft/sounds.json").getAsJsonObject();
        JsonObject definition = sounds.getAsJsonObject("item.bundle.insert_fail");
        assertEquals("item/bundle/insert_fail", definition.getAsJsonArray("sounds").get(0).getAsString());
        assertEquals("subtitles.item.bundle.insert_fail", definition.get("subtitle").getAsString());
        assertResource("assets/minecraft/sounds/item/bundle/insert_fail.ogg");

        JsonObject language = json("assets/minecraft/lang/en_us.json").getAsJsonObject();
        assertNotNull(language.get("subtitles.item.bundle.insert_fail"));
        COLORS.forEach(color -> assertNotNull(language.get("item.minecraft." + color + "_bundle")));
    }

    @Test
    void villageLootUsesEightScopedAddTableModifiers() throws IOException {
        JsonObject global = json("data/neoforge/loot_modifiers/global_loot_modifiers.json").getAsJsonObject();
        assertFalse(global.get("replace").getAsBoolean());
        JsonArray entries = global.getAsJsonArray("entries");
        assertEquals(8, entries.size());

        for (String table : VILLAGE_TABLES) {
            String modifierId = "backwiththebundle:" + table + "_bundle";
            assertTrue(entries.asList().stream().anyMatch(entry -> modifierId.equals(entry.getAsString())));

            JsonObject modifier = json("data/backwiththebundle/loot_modifiers/" + table + "_bundle.json").getAsJsonObject();
            assertEquals("neoforge:add_table", modifier.get("type").getAsString());
            assertEquals("backwiththebundle:chests/village_bundle", modifier.get("table").getAsString());
            JsonObject condition = modifier.getAsJsonArray("conditions").get(0).getAsJsonObject();
            assertEquals("neoforge:loot_table_id", condition.get("condition").getAsString());
            assertEquals("minecraft:chests/village/" + table, condition.get("loot_table_id").getAsString());
        }

        JsonObject extraTable = json("data/backwiththebundle/loot_table/chests/village_bundle.json").getAsJsonObject();
        JsonArray lootEntries = extraTable.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries");
        assertEquals(1, lootEntries.get(0).getAsJsonObject().get("weight").getAsInt());
        assertEquals(2, lootEntries.get(1).getAsJsonObject().get("weight").getAsInt());
    }

    @Test
    void modernBundleTooltipHasItsVanillaSpritesAndTranslations() throws IOException {
        for (String sprite : BUNDLE_TOOLTIP_SPRITES) {
            String basePath = "assets/minecraft/textures/gui/sprites/container/bundle/" + sprite + ".png";
            assertResource(basePath);
            JsonObject metadata = json(basePath + ".mcmeta").getAsJsonObject();
            assertEquals("nine_slice", metadata.getAsJsonObject("gui")
                .getAsJsonObject("scaling")
                .get("type")
                .getAsString());
        }

        JsonObject language = json("assets/minecraft/lang/en_us.json").getAsJsonObject();
        assertNotNull(language.get("item.minecraft.bundle.empty"));
        assertNotNull(language.get("item.minecraft.bundle.empty.description"));
        assertNotNull(language.get("item.minecraft.bundle.full"));
    }

    @Test
    void clientConfigurationHasTranslatedTitleOptionAndTooltip() throws IOException {
        JsonObject language = json("assets/backwiththebundle/lang/en_us.json").getAsJsonObject();

        assertNotNull(language.get("backwiththebundle.configuration.title"));
        assertNotNull(language.get("backwiththebundle.configuration.expandBundleTooltip"));
        assertNotNull(language.get("backwiththebundle.configuration.expandBundleTooltip.tooltip"));
        assertNotNull(language.get("backwiththebundle.configuration.bundleDragEnabled"));
        assertNotNull(language.get("backwiththebundle.configuration.bundleDragEnabled.tooltip"));
    }

    @Test
    void easyShulkerBoxesBundleProviderIsDisabledWithAnEmptyItemSelection() throws IOException {
        JsonObject provider = json("data/easyshulkerboxes/item_contents_provider/bundle.json").getAsJsonObject();

        assertEquals("iteminteractions:bundle", provider.get("type").getAsString());
        assertEquals(1, provider.get("capacity_multiplier").getAsInt());
        assertTrue(provider.getAsJsonArray("supported_items").isEmpty());
    }

    private static JsonElement json(String path) throws IOException {
        try (InputStream stream = resource(path);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static void assertBundleTemplateTranslation(String part, int z) throws IOException {
        JsonObject template = json("assets/minecraft/models/item/template_bundle_open_" + part + ".json")
            .getAsJsonObject();
        assertEquals("minecraft:item/generated", template.get("parent").getAsString());
        assertEquals(
            List.of(0, 0, z),
            template.getAsJsonObject("display")
                .getAsJsonObject("gui")
                .getAsJsonArray("translation")
                .asList()
                .stream()
                .map(JsonElement::getAsInt)
                .toList()
        );
    }

    private static void assertResource(String path) throws IOException {
        try (InputStream ignored = resource(path)) {
            assertNotNull(ignored);
        }
    }

    private static InputStream resource(String path) throws IOException {
        InputStream stream = ResourceFoundationTest.class.getModule().getResourceAsStream(path);
        if (stream == null) {
            stream = ResourceFoundationTest.class.getClassLoader().getResourceAsStream(path);
        }
        assertNotNull(stream, "Missing resource " + path);
        return stream;
    }
}
