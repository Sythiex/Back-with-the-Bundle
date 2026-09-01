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
    void baseBundleRecipeUsesStringOverLeather() throws IOException {
        JsonObject recipe = json("data/minecraft/recipe/bundle.json").getAsJsonObject();
        assertEquals(List.of("-", "#"), recipe.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList());
        assertEquals("minecraft:string", recipe.getAsJsonObject("key").get("-").getAsString());
        assertEquals("minecraft:leather", recipe.getAsJsonObject("key").get("#").getAsString());
    }

    @Test
    void insertFailSoundHasDefinitionTranslationAndAudio() throws IOException {
        JsonObject sounds = json("assets/minecraft/sounds.json").getAsJsonObject();
        JsonObject definition = sounds.getAsJsonObject("item.bundle.insert_fail");
        assertEquals("item/bundle/insert_fail", definition.getAsJsonArray("sounds").get(0).getAsString());
        assertEquals("subtitles.item.bundle.insert_fail", definition.get("subtitle").getAsString());
        assertResource("assets/minecraft/sounds/item/bundle/insert_fail.ogg");

        JsonObject language = json("assets/minecraft/lang/en_us.json").getAsJsonObject();
        assertEquals("Bundle full", language.get("subtitles.item.bundle.insert_fail").getAsString());
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

    private static JsonElement json(String path) throws IOException {
        try (InputStream stream = resource(path);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
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
