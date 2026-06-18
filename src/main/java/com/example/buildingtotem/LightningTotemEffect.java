package com.example.buildingtotem;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LightningTotemEffect implements TotemEffect {

    private static final double STRIKE_RADIUS = 15.0;
    private static final double VISUAL_STRIKE_RADIUS = 8.0;
    private static final int EFFECT_DURATION = 600;          // 30 秒 (tick)
    private static final int STRIKE_INTERVAL_MS = 2000;     // 雷击间隔 2 秒
    private static final int FOLLOW_INTERVAL_MS = 200;      // 三叉戟跟随间隔 0.2 秒

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // 生成三叉戟在玩家头顶
        ItemEntity tridentEntity = new ItemEntity(world,
                px, py + 2.0, pz,
                new ItemStack(Items.TRIDENT));
        tridentEntity.setPickupDelay(32767);
        tridentEntity.setNoGravity(true);
        tridentEntity.setInvulnerable(true);
        tridentEntity.setNeverDespawn();
        tridentEntity.setGlowing(true);
        world.spawnEntity(tridentEntity);

        // 发光 + 防火效果（30 秒）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, EFFECT_DURATION, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, false, false, true));

        // 初始雷声音效
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 记录开始时间（毫秒）
        final long startTime = System.currentTimeMillis();

        // 单一定时器，每 50ms 检查一次
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            private long lastStrike = 0;
            private long lastFollow = 0;

            @Override
            public void run() {
                world.getServer().execute(() -> {
                    // 玩家不在或效果结束
                    if (!player.isAlive() || !player.hasStatusEffect(StatusEffects.GLOWING)) {
                        timer.cancel();
                        if (tridentEntity.isAlive()) {
                            tridentEntity.remove(Entity.RemovalReason.DISCARDED);
                        }
                        return;
                    }

                    long now = System.currentTimeMillis();
                    long elapsed = now - startTime;

                    // 超过 30 秒自动结束
                    if (elapsed >= EFFECT_DURATION * 50L) {
                        timer.cancel();
                        if (tridentEntity.isAlive()) {
                            tridentEntity.remove(Entity.RemovalReason.DISCARDED);
                        }
                        return;
                    }

                    // 雷击：距离上次超过 2 秒
                    if (now - lastStrike >= STRIKE_INTERVAL_MS) {
                        lastStrike = now;
                        performStrike(player, world);
                    }

                    // 三叉戟跟随：距离上次超过 0.2 秒
                    if (now - lastFollow >= FOLLOW_INTERVAL_MS) {
                        lastFollow = now;
                        double newX = player.getX();
                        double newY = player.getY();
                        double newZ = player.getZ();
                        tridentEntity.setPosition(newX, newY + 2.0, newZ);

                        // 三叉戟周围极少量电粒子
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                                tridentEntity.getX(), tridentEntity.getY() + 0.5, tridentEntity.getZ(),
                                1, 0.2, 0.2, 0.2, 0.02);
                    }
                });
            }
        }, 0, 50);
    }

    private void performStrike(ServerPlayerEntity player, ServerWorld world) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // 1. 劈向周围的敌对生物（装饰性闪电 + 代码伤害）
        Box searchBox = player.getBoundingBox().expand(STRIKE_RADIUS);
        List<Entity> monsters = world.getOtherEntities(player, searchBox,
                entity -> entity instanceof HostileEntity);
        for (Entity monster : monsters) {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAndAngles(
                    monster.getX(), monster.getY(), monster.getZ(),
                    world.random.nextFloat() * 360.0F, 0.0F);
            // 关键修复：闪电设为装饰性，不引火
            lightning.setCosmetic(true);
            world.spawnEntity(lightning);
            // 用代码造成伤害（5 点雷击伤害）
            monster.damage(world, player.getDamageSources().lightningBolt(), 5.0F);
        }

        // 2. 玩家周围随机 3 道装饰性闪电（纯视觉，无伤害，无火）
        for (int i = 0; i < 3; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = VISUAL_STRIKE_RADIUS * world.random.nextDouble();
            double x = px + Math.cos(angle) * distance;
            double z = pz + Math.sin(angle) * distance;

            LightningEntity visualLightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            visualLightning.refreshPositionAndAngles(x, py, z, world.random.nextFloat() * 360.0F, 0.0F);
            visualLightning.setCosmetic(true); // 装饰性，无伤害，无火
            world.spawnEntity(visualLightning);
        }
    }
}