package com.example.buildingtotem;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class TianDiTongShouEffect implements TotemEffect {

    private static final SoundEvent EXPLODE_SOUND = Registries.SOUND_EVENT.get(Identifier.ofVanilla("entity.generic.explode"));

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        System.out.println("[天地同寿] 核爆触发！");

        // 无敌 2 秒
        player.setInvulnerable(true);
        // 缓降 1 分钟，浮空欣赏蘑菇云
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 1200, 0, false, false));

        // 延迟一帧爆炸
        world.getServer().execute(() -> {
            world.createExplosion(
                    null,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1000.0F,                                // 毁天灭地
                    false,
                    World.ExplosionSourceType.TNT
            );
            world.playSound(null, player.getBlockPos(), EXPLODE_SOUND,
                    SoundCategory.PLAYERS, 2.0F, 1.0F);

            // 2 秒后关闭无敌
            world.getServer().execute(() -> player.setInvulnerable(false));
        });
    }
}