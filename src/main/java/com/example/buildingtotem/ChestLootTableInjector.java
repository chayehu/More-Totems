package com.example.buildingtotem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ChestLootTableInjector implements ModInitializer {

    @Override
    public void onInitialize() {
        Item creeperTotem = Registries.ITEM.get(Identifier.of("building-totem", "creeper_totem"));
        Item healTotem = Registries.ITEM.get(Identifier.of("building-totem", "heal_totem"));
        Item teleportTotem = Registries.ITEM.get(Identifier.of("building-totem", "teleport_totem"));
        Item lightningTotem = Registries.ITEM.get(Identifier.of("building-totem", "lightning_totem"));
        Item gambleTotem = Registries.ITEM.get(Identifier.of("building-totem", "gamble_totem"));

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            // 沙漠神殿 → 苦力怕图腾 (3%)
            if (LootTables.DESERT_PYRAMID_CHEST.equals(key) && creeperTotem != null) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.AIR).weight(97))
                        .with(ItemEntry.builder(creeperTotem).weight(3));
                tableBuilder.pool(pool);
            }

            // 前哨站 → 治疗图腾 (3%)
            if (LootTables.PILLAGER_OUTPOST_CHEST.equals(key) && healTotem != null) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.AIR).weight(97))
                        .with(ItemEntry.builder(healTotem).weight(3));
                tableBuilder.pool(pool);
            }

            // 末地城 → 传送图腾 (3%)
            if (LootTables.END_CITY_TREASURE_CHEST.equals(key) && teleportTotem != null) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.AIR).weight(97))
                        .with(ItemEntry.builder(teleportTotem).weight(3));
                tableBuilder.pool(pool);
            }

            // 要塞图书馆 → 传送图腾 (3%)
            if (LootTables.STRONGHOLD_LIBRARY_CHEST.equals(key) && teleportTotem != null) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.AIR).weight(97))
                        .with(ItemEntry.builder(teleportTotem).weight(3));
                tableBuilder.pool(pool);
            }

            // 丛林神庙 → 雷电图腾 (3%)
            if (LootTables.JUNGLE_TEMPLE_CHEST.equals(key) && lightningTotem != null) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.AIR).weight(97))
                        .with(ItemEntry.builder(lightningTotem).weight(3));
                tableBuilder.pool(pool);
            }

            // 丛林神庙 → 赌徒图腾 (3%)
            if (LootTables.JUNGLE_TEMPLE_CHEST.equals(key) && gambleTotem != null) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .with(ItemEntry.builder(Items.AIR).weight(97))
                        .with(ItemEntry.builder(gambleTotem).weight(3));
                tableBuilder.pool(pool);
            }
        });
    }
}