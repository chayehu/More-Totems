/*
 * 图腾扩展工艺 (Totem Expansion Craft)
 * Copyright (c) 2025 Tea_Leaf_Fox (茶叶狐)
 * 许可证：CC BY-NC-ND 4.0
 * https://creativecommons.org/licenses/by-nc-nd/4.0/
 */

package com.example.buildingtotem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.*;

public class BuildingTotem implements ModInitializer {

    private static final Map<String, TotemEffect> EFFECT_MAP = new HashMap<>();
    private static final Map<Item, String> ITEM_TO_ID = new HashMap<>();
    private static final SoundEvent TOTEM_USE_SOUND = Registries.SOUND_EVENT.get(Identifier.ofVanilla("item.totem.use"));

    private static final Map<UUID, ItemStack> DEATH_KEPT_SWORDS = new HashMap<>();
    private static final Identifier APOCALYPSE_SWORD_ID = Identifier.of("building-totem", "apocalypse_sword");

    @Override
    public void onInitialize() {
        // 注册效果
        EFFECT_MAP.put("building_totem", new BuildingTotemEffect());
        EFFECT_MAP.put("creeper_totem", new CreeperTotemEffect());
        EFFECT_MAP.put("heal_totem", new HealTotemEffect());
        EFFECT_MAP.put("teleport_totem", new TeleportTotemEffect());
        EFFECT_MAP.put("supreme_totem", new SupremeTotemEffect());
        EFFECT_MAP.put("lightning_totem", new LightningTotemEffect());
        EFFECT_MAP.put("gamble_totem", new GambleTotemEffect());
        EFFECT_MAP.put("tiandi_totem", new TianDiTongShouEffect());
        EFFECT_MAP.put("void_totem", new VoidTotemEffect());

        // 注册图腾物品
        registerTotemItem("building_totem", "item.building-totem.building_totem", "item.building-totem.building_totem.tooltip");
        registerTotemItem("creeper_totem", "item.building-totem.creeper_totem", "item.building-totem.creeper_totem.tooltip");
        registerTotemItem("heal_totem", "item.building-totem.heal_totem", "item.building-totem.heal_totem.tooltip");
        registerTotemItem("supreme_totem", "item.building-totem.supreme_totem", "item.building-totem.supreme_totem.tooltip");
        registerTotemItem("lightning_totem", "item.building-totem.lightning_totem", "item.building-totem.lightning_totem.tooltip");
        registerTotemItem("gamble_totem", "item.building-totem.gamble_totem", "item.building-totem.gamble_totem.tooltip");
        registerTotemItem("tiandi_totem", "item.building-totem.tiandi_totem", "item.building-totem.tiandi_totem.tooltip");
        registerTotemItem("void_totem", "item.building-totem.void_totem", "item.building-totem.void_totem.tooltip");

        // 单独注册传送图腾
        RegistryKey<Item> teleportKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("building-totem", "teleport_totem"));
        Item teleportItem = Items.register(teleportKey, TeleportTotemItem::new, new Item.Settings()
                .rarity(Rarity.UNCOMMON)
                .maxCount(1)
                .component(DataComponentTypes.LORE, new LoreComponent(List.of(
                        Text.translatable("item.building-totem.teleport_totem.tooltip")
                                .setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))
                ))));
        ITEM_TO_ID.put(teleportItem, "teleport_totem");
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> content.add(teleportItem));

        // ========== 天启剑注册 ==========
        RegistryKey<Item> apocalypseKey = RegistryKey.of(RegistryKeys.ITEM, APOCALYPSE_SWORD_ID);
        Item apocalypseSword = Items.register(apocalypseKey, ApocalypseSwordItem::new, new Item.Settings()
                .rarity(Rarity.EPIC)
                .maxCount(1)
                .fireproof()
                .component(DataComponentTypes.LORE, new LoreComponent(List.of(
                        Text.translatable("item.building-totem.apocalypse_sword.tooltip")
                                .setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(false))
                ))));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> content.add(apocalypseSword));

        // 死亡事件（原有图腾逻辑）
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                ItemStack offHand = player.getOffHandStack();
                if (tryActivate(player, offHand)) return false;
                ItemStack mainHand = player.getMainHandStack();
                return !tryActivate(player, mainHand);
            }
            return true;
        });

        // 虚空图腾：监听虚空伤害
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && source.isOf(DamageTypes.OUT_OF_WORLD)) {
                ItemStack offHand = player.getOffHandStack();
                if ("void_totem".equals(ITEM_TO_ID.getOrDefault(offHand.getItem(), ""))) {
                    offHand.decrement(1);
                    EFFECT_MAP.get("void_totem").onTrigger(player, offHand, (ServerWorld) player.getEntityWorld());
                    return false;
                }
                ItemStack mainHand = player.getMainHandStack();
                if ("void_totem".equals(ITEM_TO_ID.getOrDefault(mainHand.getItem(), ""))) {
                    mainHand.decrement(1);
                    EFFECT_MAP.get("void_totem").onTrigger(player, mainHand, (ServerWorld) player.getEntityWorld());
                    return false;
                }
            }
            return true;
        });

        // ========== 天启剑：攻击秒杀 + 审判无敌 ==========
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                if (weapon.getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID)) {
                    if (entity == attacker) return true;

                    entity.kill((ServerWorld) attacker.getEntityWorld());
                    return false;
                }
            }

            if (entity instanceof ServerPlayerEntity player) {
                if (isHoldingApocalypseSword(player) && ApocalypseSwordItem.isJudgmentActive(player)) {
                    return false;
                }
            }

            return true;
        });

        // ========== 天启剑死亡不掉落 ==========
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                ItemStack sword = null;
                if (player.getMainHandStack().getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID)) {
                    sword = player.getMainHandStack().copy();
                    player.getMainHandStack().setCount(0);
                } else if (player.getOffHandStack().getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID)) {
                    sword = player.getOffHandStack().copy();
                    player.getOffHandStack().setCount(0);
                } else {
                    int size = player.getInventory().size();
                    for (int i = 0; i < size; i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (stack.getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID)) {
                            sword = stack.copy();
                            player.getInventory().setStack(i, ItemStack.EMPTY);
                            break;
                        }
                    }
                }
                if (sword != null) {
                    DEATH_KEPT_SWORDS.put(player.getUuid(), sword);
                }
            }
            return true;
        });

        // 重生时归还天启剑
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ItemStack sword = DEATH_KEPT_SWORDS.remove(newPlayer.getUuid());
            if (sword != null) {
                if (!newPlayer.getInventory().insertStack(sword)) {
                    newPlayer.dropItem(sword, false);
                }
                newPlayer.sendMessage(Text.literal("§6天启剑重新回到你的身边。"), true);
            }
        });

        // ========== 保护掉落的剑：无敌 ==========
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack stack = itemEntity.getStack();
                if (stack.getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID)) {
                    itemEntity.setInvulnerable(true);
                    itemEntity.setNeverDespawn();
                }
            }
        });

        // ========== 虚空复制归还 ==========
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (!(entity instanceof ItemEntity itemEntity)) return;
            ItemStack stack = itemEntity.getStack();
            if (stack.getItem() != Registries.ITEM.get(APOCALYPSE_SWORD_ID)) return;

            if (itemEntity.getY() < -64) {
                UUID ownerUuid = ApocalypseSwordItem.getOwner(stack);
                if (ownerUuid == null) return;

                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerUuid);
                if (owner != null) {
                    ItemStack copy = stack.copy();
                    if (!owner.getInventory().insertStack(copy)) {
                        owner.dropItem(copy, false);
                    }
                    owner.sendMessage(Text.literal("§6天启剑从虚空中回到你的身边。"), true);
                }
            }
        });
    }

    private boolean isHoldingApocalypseSword(ServerPlayerEntity player) {
        return player.getMainHandStack().getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID)
                || player.getOffHandStack().getItem() == Registries.ITEM.get(APOCALYPSE_SWORD_ID);
    }

    private boolean tryActivate(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = ITEM_TO_ID.get(stack.getItem());
        if (id == null) return false;
        TotemEffect effect = EFFECT_MAP.get(id);
        if (effect == null) return false;

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        world.sendEntityStatus(player, (byte)35);
        stack.decrement(1);
        player.setHealth(1.0f);
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 900, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 900, 0));
        world.playSound(player, player.getBlockPos(), TOTEM_USE_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);

        effect.onTrigger(player, stack, world);
        return true;
    }

    private void registerTotemItem(String id, String nameKey, String tooltipKey) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of("building-totem", id));
        Item item = Items.register(key, Item::new, new Item.Settings()
                .rarity(Rarity.UNCOMMON)
                .maxCount(1)
                .component(DataComponentTypes.LORE, new LoreComponent(List.of(
                        Text.translatable(tooltipKey).setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))
                ))));
        ITEM_TO_ID.put(item, id);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> content.add(item));
    }
}