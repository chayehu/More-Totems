package com.example.buildingtotem;

import net.minecraft.advancement.AdvancementEntry;
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

public class CreeperTotemEffect implements TotemEffect {

    private static final SoundEvent EXPLODE_SOUND = Registries.SOUND_EVENT.get(Identifier.ofVanilla("entity.generic.explode"));

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        System.out.println("[苦力怕图腾] 爆炸触发！");

        // 开启无敌 2 秒，防止自己被炸死或摔死
        player.setInvulnerable(true);
        // 缓降 4 秒（80 tick），优雅落地
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 80, 0, false, false));

        // 延迟一帧爆炸
        world.getServer().execute(() -> {
            world.createExplosion(
                    null,                                   // 无源爆炸，正常伤害其他实体
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    8.0F,                                   // 调整后的威力
                    false,                                  // 无火
                    World.ExplosionSourceType.TNT
            );
            world.playSound(null, player.getBlockPos(), EXPLODE_SOUND,
                    SoundCategory.PLAYERS, 2.0F, 1.0F);

            // 2 秒后关闭无敌
            world.getServer().execute(() -> player.setInvulnerable(false));
        });

        // ========== 授予成就：苦力怕的拥抱 ==========
        AdvancementEntry advancement = world.getServer().getAdvancementLoader()
                .get(Identifier.of("building-totem", "creeper_used"));
        if (advancement != null) {
            player.getAdvancementTracker().grantCriterion(advancement, "trigger");
        }
    }
}