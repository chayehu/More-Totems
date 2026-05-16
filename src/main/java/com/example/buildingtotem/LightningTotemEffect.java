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
    private static final int EFFECT_DURATION = 600; // 发光+防火 30 秒

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        // 获取玩家位置（使用 getX/getY/getZ 避免映射问题）
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // 生成三叉戟在玩家头顶
        ItemEntity tridentEntity = new ItemEntity(world,
                px, py + 2.0, pz,
                new ItemStack(Items.TRIDENT));
        tridentEntity.setPickupDelay(32767);        // 禁止拾取
        tridentEntity.setNoGravity(true);           // 悬浮
        tridentEntity.setInvulnerable(true);        // 无敌
        tridentEntity.setNeverDespawn();            // 不会自然消失
        tridentEntity.setGlowing(true);             // 发光轮廓，更显眼
        world.spawnEntity(tridentEntity);

        // 发光 + 防火效果（30 秒）
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, EFFECT_DURATION, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, EFFECT_DURATION, 0, false, false, true));

        // 初始雷声音效
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 1.0F, 1.0F);

        // 启动定时器，每 2 秒执行一次雷击，直到发光效果消失
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 所有对世界的操作必须在服务端线程执行
                world.getServer().execute(() -> {
                    // 检查玩家是否存活，发光效果是否还在
                    if (player.isAlive() && player.hasStatusEffect(StatusEffects.GLOWING)) {
                        performStrike(player, world);

                        // 让三叉戟始终跟随玩家头顶
                        double newX = player.getX();
                        double newY = player.getY();
                        double newZ = player.getZ();
                        tridentEntity.setPosition(newX, newY + 2.0, newZ);

                        // 三叉戟周围电粒子
                        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK,
                                tridentEntity.getX(), tridentEntity.getY() + 0.5, tridentEntity.getZ(),
                                5, 0.3, 0.3, 0.3, 0.05);
                    } else {
                        // 效果结束或玩家死亡，移除三叉戟并停止定时器
                        if (tridentEntity.isAlive()) {
                            tridentEntity.remove(Entity.RemovalReason.DISCARDED);
                        }
                        timer.cancel();
                    }
                });
            }
        }, 0, 2000); // 立即开始，之后每 2000 毫秒（2 秒）一次
    }

    private void performStrike(ServerPlayerEntity player, ServerWorld world) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        Vec3d playerPos = new Vec3d(px, py, pz);

        // 1. 劈向周围的敌对生物（真实伤害）
        Box searchBox = player.getBoundingBox().expand(STRIKE_RADIUS);
        List<Entity> monsters = world.getOtherEntities(player, searchBox,
                entity -> entity instanceof HostileEntity);
        for (Entity monster : monsters) {
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAndAngles(monster.getX(), monster.getY(), monster.getZ(),
                    world.random.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(lightning);
        }

        // 2. 玩家周围随机 3 道装饰性闪电（纯视觉，无伤害，无火）
        for (int i = 0; i < 3; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = VISUAL_STRIKE_RADIUS * world.random.nextDouble();
            double x = px + Math.cos(angle) * distance;
            double z = pz + Math.sin(angle) * distance;

            LightningEntity visualLightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            visualLightning.refreshPositionAndAngles(x, py, z, world.random.nextFloat() * 360.0F, 0.0F);
            visualLightning.setCosmetic(true); // 装饰性，不造成伤害，不起火
            world.spawnEntity(visualLightning);
        }
    }
}