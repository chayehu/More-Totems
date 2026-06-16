package com.example.buildingtotem;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final RegistryKey<ItemGroup> TOTEM_CRAFT_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("building-totem", "totem_craft_group"));

    public static final ItemGroup TOTEM_CRAFT_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(BuildingTotem.BUILDING_TOTEM)) // 用平地起高楼图腾作为图标
            .displayName(Text.translatable("itemGroup.building-totem.totem_craft_group"))
            .build();

    public static void register() {
        Registry.register(Registries.ITEM_GROUP, TOTEM_CRAFT_GROUP_KEY, TOTEM_CRAFT_GROUP);
    }
}