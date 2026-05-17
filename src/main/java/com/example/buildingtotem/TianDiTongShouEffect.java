package com.example.buildingtotem;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Timer;
import java.util.TimerTask;

public class TianDiTongShouEffect implements TotemEffect {

    private static final SoundEvent EXPLODE_SOUND = Registries.SOUND_EVENT.get(Identifier.ofVanilla("entity.generic.explode"));

    private static final int MAX_RADIUS = 150;
    private static final int RADIUS_STEP = 10;
    private static final int INTERVAL_MS = 2000;
    private static final int TOTAL_STEPS = MAX_RADIUS / RADIUS_STEP; // 15步

    private static final double PARTICLE_RING_Y_OFFSET = 1.5;
    private static final int PARTICLES_PER_POINT = 2;

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        System.out.println("[天地同寿] 球形毁灭领域启动！");

        BlockPos center = player.getBlockPos();

        player.setInvulnerable(true);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 2400, 0, false, false));

        world.playSound(null, center, EXPLODE_SOUND, SoundCategory.PLAYERS, 2.0F, 1.0F);

        // 启动第一轮毁灭
        Timer firstRoundTimer = new Timer();
        scheduleStep(firstRoundTimer, player, world, center, 0);
    }

    /**
     * 递归调度一步毁灭。当第一轮推进到第5步（step=4）时，启动第二轮毁灭。
     */
    private void scheduleStep(Timer timer, ServerPlayerEntity player, ServerWorld world, BlockPos center, int step) {
        if (step >= TOTAL_STEPS) {
            // 本轮结束，取消Timer
            timer.cancel();
            // 如果是第二轮结束，关闭无敌
            if (timer == null) { // 此判断仅用于说明，实际每个Timer独立，第二轮结束时取消即可
                // 由于第一轮和第二轮的 timer 不是同一个对象，我们直接在 step==TOTAL_STEPS 且是第二轮时关闭无敌。
                // 简单处理：在下面的 killAllEntitiesInSphere 中关闭无敌。
            }
            return;
        }

        int currentRadius = (step + 1) * RADIUS_STEP;
        int innerRadius = step * RADIUS_STEP;

        world.getServer().execute(() -> {
            // 删除当前球壳
            deleteSphericalShell(world, center, innerRadius, currentRadius);
            spawnLayerParticles(world, center, currentRadius);

            // 第一轮推进到第5步（step=4）时，启动第二轮毁灭
            if (step == 4 && timer != null) {
                System.out.println("[天地同寿] 第二轮毁灭领域启动！");
                Timer secondRoundTimer = new Timer();
                scheduleStep(secondRoundTimer, player, world, center, 0);
            }

            // 每轮的最后一步执行秒杀，并关闭无敌
            if (step == TOTAL_STEPS - 1) {
                killAllEntitiesInSphere(world, center, player);
                player.setInvulnerable(false);
            }
        });

        // 延迟2秒后执行下一步
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                scheduleStep(timer, player, world, center, step + 1);
            }
        }, INTERVAL_MS);
    }

    private void deleteSphericalShell(ServerWorld world, BlockPos center, int innerRadius, int outerRadius) {
        for (BlockPos pos : BlockPos.iterateOutwards(center, outerRadius, outerRadius, outerRadius)) {
            double distSq = pos.getSquaredDistance(center);
            double innerDistSq = (double) innerRadius * innerRadius;
            double outerDistSq = (double) outerRadius * outerRadius;

            if (distSq <= outerDistSq && distSq > innerDistSq) {
                BlockState state = world.getBlockState(pos);
                if (state.isAir() || state.getBlock() == Blocks.BEDROCK) {
                    continue;
                }
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
            }
        }
    }

    private void spawnLayerParticles(ServerWorld world, BlockPos center, int radius) {
        double centerX = center.getX() + 0.5;
        double centerY = center.getY() + PARTICLE_RING_Y_OFFSET;
        double centerZ = center.getZ() + 0.5;

        for (int i = 0; i < 2; i++) {
            for (int angle = 0; angle < 360; angle += 10) {
                double rad = Math.toRadians(angle);
                double x, y, z;

                if (i == 0) {
                    x = centerX + radius * Math.cos(rad);
                    y = centerY;
                    z = centerZ + radius * Math.sin(rad);
                } else {
                    x = centerX + radius * Math.cos(rad);
                    y = centerY + radius * Math.sin(rad);
                    z = centerZ;
                }

                world.spawnParticles(ParticleTypes.FLAME, x, y, z, PARTICLES_PER_POINT, 0, 0, 0, 0.01);
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, 0, 0, 0.01);
            }
        }
    }

    private void killAllEntitiesInSphere(ServerWorld world, BlockPos center, ServerPlayerEntity player) {
        Box boundingBox = new Box(
                center.getX() - MAX_RADIUS, center.getY() - MAX_RADIUS, center.getZ() - MAX_RADIUS,
                center.getX() + MAX_RADIUS, center.getY() + MAX_RADIUS, center.getZ() + MAX_RADIUS
        );

        DamageSource voidDamage = player.getDamageSources().outOfWorld();

        world.getEntitiesByClass(LivingEntity.class, boundingBox, entity -> {
            if (entity == player) return false;
            return entity.getBlockPos().getSquaredDistance(center) <= MAX_RADIUS * MAX_RADIUS;
        }).forEach(entity -> {
            entity.damage(world, voidDamage, Float.MAX_VALUE);
        });
    }
}