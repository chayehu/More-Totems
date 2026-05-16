package com.example.buildingtotem;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class SupremeTotemEffect implements TotemEffect {

    // 核心爆发 30 秒 = 600 tick
    private static final int BURST_DURATION = 600;
    // 抗火 2 分钟 = 2400 tick
    private static final int FIRE_RESIST_DURATION = 2400;

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        // 力量 XX（等级 19），近战伤害 +60
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, BURST_DURATION, 19, false, true));

        // 抗性提升 V（等级 4），100% 物理免伤
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, BURST_DURATION, 4, false, true));

        // 生命恢复 V（等级 4），高速回血
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, BURST_DURATION, 4, false, true));

        // 伤害吸收 V（等级 4），+20 颗金心
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, BURST_DURATION, 4, false, true));

        // 抗火 I，2 分钟
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, FIRE_RESIST_DURATION, 0, false, true));

        // 音效：末影龙死亡
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                SoundCategory.PLAYERS, 1.5F, 1.0F);

        // 金色光柱粒子（冲天）
        for (int i = 0; i < 50; i++) {
            world.spawnParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    3, 0.5, 3.0, 0.5, 0.1);
        }

        // 金色光环粒子（环绕）
        for (int i = 0; i < 30; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = 1.5 + world.random.nextDouble() * 2.0;
            world.spawnParticles(ParticleTypes.WAX_ON,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 1.0 + world.random.nextDouble() * 2.0,
                    player.getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
    }
}