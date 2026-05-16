package com.example.buildingtotem;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.Random;

public class GambleTotemEffect implements TotemEffect {

    private static final int BUFF_DURATION = 600;        // 增益 30 秒
    private static final int SLOWNESS_DURATION = 600;    // 缓慢 30 秒
    private static final int WEAKNESS_DURATION = 600;    // 虚弱 30 秒
    private static final int WITHER_DURATION = 100;      // 凋零 III，5 秒
    private static final int DARKNESS_DURATION = 300;    // 黑暗，15 秒
    private static final int MIN_MOBS = 4;
    private static final int MAX_MOBS = 10;
    private static final int MOB_TYPE_COUNT = 8;

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        Random random = new Random();
        boolean win = random.nextBoolean();

        if (win) {
            // 赌赢了！屏幕中央金色大字提示
            player.sendMessage(Text.literal("算你走运！赢是过程输是结果！")
                    .formatted(Formatting.GOLD, Formatting.BOLD), true);

            player.setHealth(player.getMaxHealth());
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, BUFF_DURATION, 4, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, BUFF_DURATION, 1, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, BUFF_DURATION, 2, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, BUFF_DURATION, 1, false, true));
            player.getHungerManager().add(4, 0.5f);

            // 召唤 2 只铁傀儡
            for (int i = 0; i < 2; i++) {
                IronGolemEntity golem = new IronGolemEntity(EntityType.IRON_GOLEM, world);
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 2.0 + random.nextDouble() * 1.5;
                golem.refreshPositionAndAngles(
                        player.getX() + Math.cos(angle) * distance,
                        player.getY(),
                        player.getZ() + Math.sin(angle) * distance,
                        random.nextFloat() * 360.0F, 0.0F);
                golem.setPlayerCreated(true);
                world.spawnEntity(golem);
            }

            // 金色粒子
            for (int i = 0; i < 30; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double radius = 1.5 + random.nextDouble() * 2.0;
                world.spawnParticles(ParticleTypes.WAX_ON,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + 1.0 + random.nextDouble() * 2.0,
                        player.getZ() + Math.sin(angle) * radius,
                        1, 0, 0, 0, 0);
            }
            world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_TOTEM_USE,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);

        } else {
            // 赌输了！屏幕中央红色大字提示
            player.sendMessage(Text.literal("RUN")
                    .formatted(Formatting.RED, Formatting.BOLD), true);

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, WITHER_DURATION, 2, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLOWNESS_DURATION, 0, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, WEAKNESS_DURATION, 2, false, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, DARKNESS_DURATION, 0, false, true));

            // 随机 4~10 只怪物
            int mobCount = MIN_MOBS + random.nextInt(MAX_MOBS - MIN_MOBS + 1);
            for (int i = 0; i < mobCount; i++) {
                spawnRandomMob(world, player, random.nextInt(MOB_TYPE_COUNT));
            }

            // 1% 彩蛋：坚守者
            if (random.nextInt(100) < 1) {
                WardenEntity warden = new WardenEntity(EntityType.WARDEN, world);
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 2.0 + random.nextDouble() * 3.0;
                warden.refreshPositionAndAngles(
                        player.getX() + Math.cos(angle) * distance,
                        player.getY(),
                        player.getZ() + Math.sin(angle) * distance,
                        random.nextFloat() * 360.0F, 0.0F);
                world.spawnEntity(warden);
            }

            // 黑色烟雾粒子
            world.spawnParticles(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.5, 1.5, 0.5, 0.1);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SKELETON_DEATH,
                    SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void spawnRandomMob(ServerWorld world, ServerPlayerEntity player, int type) {
        HostileEntity entity = switch (type) {
            case 0 -> new ZombieEntity(EntityType.ZOMBIE, world);
            case 1 -> new SkeletonEntity(EntityType.SKELETON, world);
            case 2 -> new SpiderEntity(EntityType.SPIDER, world);
            case 3 -> new CreeperEntity(EntityType.CREEPER, world);
            case 4 -> new WitchEntity(EntityType.WITCH, world);
            case 5 -> new PillagerEntity(EntityType.PILLAGER, world);
            case 6 -> new HuskEntity(EntityType.HUSK, world);
            case 7 -> new StrayEntity(EntityType.STRAY, world);
            default -> null;
        };
        if (entity == null) return;

        // 远程怪物配发武器
        if (entity instanceof SkeletonEntity || entity instanceof StrayEntity) {
            entity.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BOW));
        } else if (entity instanceof PillagerEntity) {
            entity.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
        }

        double angle = world.random.nextDouble() * Math.PI * 2;
        double distance = 2.0 + world.random.nextDouble() * 3.0;
        entity.refreshPositionAndAngles(
                player.getX() + Math.cos(angle) * distance,
                player.getY(),
                player.getZ() + Math.sin(angle) * distance,
                world.random.nextFloat() * 360.0F, 0.0F);
        world.spawnEntity(entity);
    }
}