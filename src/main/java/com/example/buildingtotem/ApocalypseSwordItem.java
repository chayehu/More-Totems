package com.example.buildingtotem;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ApocalypseSwordItem extends Item {

    public static final Set<UUID> JUDGMENT_MODE = new HashSet<>();

    public ApocalypseSwordItem(Settings settings) {
        super(settings);
    }

    // 读取认主信息
    public static UUID getOwner(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent != null) {
            NbtCompound nbt = nbtComponent.copyNbt();
            if (nbt.contains("owner")) {
                String uuidString = nbt.getString("owner").orElse(null);
                if (uuidString != null) {
                    try {
                        return UUID.fromString(uuidString);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return null;
    }

    // 设置认主信息
    public static void setOwner(ItemStack stack, UUID uuid) {
        stack.apply(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT, component -> {
            NbtCompound nbt = component.copyNbt();
            nbt.putString("owner", uuid.toString());
            return NbtComponent.of(nbt);
        });
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        if (world.isClient()) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        ServerWorld serverWorld = (ServerWorld) world;

        // 首次潜行右键：认主
        if (getOwner(stack) == null) {
            setOwner(stack, player.getUuid());
            player.sendMessage(Text.literal("§6天启剑已认你为主！"), true);
            serverWorld.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }

        // 潜行右键：切换审判模式
        if (player.isSneaking()) {
            UUID uuid = player.getUuid();
            boolean active = JUDGMENT_MODE.contains(uuid);
            if (active) {
                JUDGMENT_MODE.remove(uuid);
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().allowFlying = false;
                    player.getAbilities().flying = false;
                }
                player.sendMessage(Text.literal("§4审判模式 §c关闭"), true);
                serverWorld.playSound(null, player.getBlockPos(),
                        SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            } else {
                JUDGMENT_MODE.add(uuid);
                if (!player.isCreative() && !player.isSpectator()) {
                    player.getAbilities().allowFlying = true;
                    player.getAbilities().flying = true;
                }
                player.sendMessage(Text.literal("§4审判模式 §a开启"), true);
                serverWorld.playSound(null, player.getBlockPos(),
                        SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
            player.sendAbilitiesUpdate();

            for (int i = 0; i < 10; i++) {
                serverWorld.spawnParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        1,
                        serverWorld.random.nextGaussian() * 0.5,
                        serverWorld.random.nextGaussian() * 0.5,
                        serverWorld.random.nextGaussian() * 0.5,
                        0.1);
            }
            return ActionResult.SUCCESS;
        }

        // 普通右键：范围秒杀（25格半径，无音效）
        double radius = 25.0;
        Box box = new Box(
                player.getX() - radius, player.getY() - radius, player.getZ() - radius,
                player.getX() + radius, player.getY() + radius, player.getZ() + radius
        );
        int count = 0;
        for (LivingEntity target : serverWorld.getEntitiesByClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
            target.kill(serverWorld);
            count++;
        }

        // 粒子效果
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1.0, player.getZ(),
                3, radius * 0.5, 1.0, radius * 0.5, 0.1);
        serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, radius * 0.5, 1.0, radius * 0.5, 0.05);
        player.sendMessage(Text.literal("§4§l审判降临，万物皆灭！击杀 " + count + " 个目标"), true);

        return ActionResult.SUCCESS;
    }

    public static boolean isJudgmentActive(PlayerEntity player) {
        return JUDGMENT_MODE.contains(player.getUuid());
    }
}