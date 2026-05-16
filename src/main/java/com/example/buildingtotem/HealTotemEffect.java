package com.example.buildingtotem;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class HealTotemEffect implements TotemEffect {

    private static final int HEART_DURATION = 1200;       // 爱心持续 1 分钟
    private static final int HEARTS_PER_TICK_MIN = 1;
    private static final int HEARTS_PER_TICK_MAX = 2;
    private static final double HEART_SPAWN_Y_OFFSET = 2.0;

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        // 1. 回满血
        player.setHealth(player.getMaxHealth());

        // 2. 4 颗金心（伤害吸收 II，30 秒）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 1, false, true));

        // 3. 生命恢复 III，1 分钟
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 2, false, true));

        // 4. 饱和 I，30 秒（瞬间恢复饱食度，并显示图标）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 600, 0, false, false));

        // 5. 力量 I，30 秒
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 600, 0, false, true));

        // 6. 信标音效
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 7. 头顶爱心粒子
        scheduleHearts(player, world, HEART_DURATION);
    }

    /**
     * 每 tick 在玩家头顶生成 1~2 颗爱心，持续 durationTicks
     */
    private void scheduleHearts(ServerPlayerEntity player, ServerWorld world, int remainingTicks) {
        if (remainingTicks <= 0) return;

        int count = world.random.nextInt(HEARTS_PER_TICK_MAX - HEARTS_PER_TICK_MIN + 1) + HEARTS_PER_TICK_MIN;
        for (int i = 0; i < count; i++) {
            double x = player.getX();
            double y = player.getY() + HEART_SPAWN_Y_OFFSET;
            double z = player.getZ();
            world.spawnParticles(ParticleTypes.HEART, x, y, z, 1, 0.1, 0.2, 0.1, 0.02);
        }

        world.getServer().execute(() -> scheduleHearts(player, world, remainingTicks - 1));
    }
}