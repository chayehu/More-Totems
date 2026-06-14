package com.example.buildingtotem;

import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class VoidTotemEffect implements TotemEffect {

    private static final int SEARCH_RADIUS_END = 300;         // 末地搜索半径
    private static final int SEARCH_RADIUS_NETHER = 150;     // 下界搜索半径
    private static final int SLOW_FALLING_SHORT = 600;       // 30秒缓降（主世界、下界）
    private static final int SLOW_FALLING_END = 2400;        // 2分钟缓降（末地）

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        double x, y, z;

        if (world.getRegistryKey() == World.END) {
            // 末地：300格搜索安全地面，2分钟缓降
            BlockPos safePos = findSafePosition(world, player.getBlockPos(), SEARCH_RADIUS_END, false);
            if (safePos != null) {
                x = safePos.getX() + 0.5;
                y = safePos.getY() + 1.0;
                z = safePos.getZ() + 0.5;
            } else {
                x = player.getX() + 0.5;
                y = 256;
                z = player.getZ() + 0.5;
            }
            player.requestTeleport(x, y, z);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, SLOW_FALLING_END, 0, false, false));
        } else if (world.getRegistryKey() == World.NETHER) {
            // 下界：150格搜索，从底部向上找最低的安全地面，30秒缓降
            BlockPos safePos = findSafePosition(world, player.getBlockPos(), SEARCH_RADIUS_NETHER, true);
            if (safePos != null) {
                x = safePos.getX() + 0.5;
                y = safePos.getY() + 1.0;
                z = safePos.getZ() + 0.5;
            } else {
                x = player.getX() + 0.5;
                y = 256;
                z = player.getZ() + 0.5;
            }
            player.requestTeleport(x, y, z);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, SLOW_FALLING_SHORT, 0, false, false));
        } else {
            // 主世界：检查当前列是否有方块，有则 y=150，无则 y=256，30秒缓降
            BlockPos playerPos = player.getBlockPos();
            x = playerPos.getX() + 0.5;
            z = playerPos.getZ() + 0.5;
            if (columnHasBlock(world, playerPos.getX(), playerPos.getZ())) {
                y = 150;
            } else {
                y = 256;
            }
            player.requestTeleport(x, y, z);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, SLOW_FALLING_SHORT, 0, false, false));
        }

        // 粒子与音效
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.spawnParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 0.5, 1.0, 0.5, 0.1);
    }

    /**
     * 检查指定 X/Z 列是否有任何方块（从 y=-64 到 y=320）。
     */
    private boolean columnHasBlock(ServerWorld world, int x, int z) {
        BlockPos.Mutable mPos = new BlockPos.Mutable(x, 0, z);
        for (int y = -64; y <= 320; y++) {
            mPos.setY(y);
            if (!world.getBlockState(mPos).isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 以 startPos 为中心，指定半径内寻找安全地面（由近到远）。
     * @param fromBottom true 表示从底部向上搜索（下界用），false 表示从顶部向下搜索（末地用）
     */
    private BlockPos findSafePosition(ServerWorld world, BlockPos startPos, int radius, boolean fromBottom) {
        for (BlockPos pos : BlockPos.iterateOutwards(startPos, radius, 0, radius)) {
            BlockPos safe;
            if (fromBottom) {
                safe = checkColumnFromBottom(world, pos.getX(), pos.getZ());
            } else {
                safe = checkColumnFromTop(world, pos.getX(), pos.getZ());
            }
            if (safe != null) return safe;
        }
        return null;
    }

    /**
     * 从顶部向下搜索：找到的第一个安全空地（通常是最高的），用于末地。
     */
    private BlockPos checkColumnFromTop(ServerWorld world, int x, int z) {
        BlockPos.Mutable mPos = new BlockPos.Mutable(x, 0, z);
        for (int y = 320; y > world.getBottomY(); y--) {
            mPos.setY(y);
            BlockState blockBelow = world.getBlockState(mPos.down());
            BlockState blockHere = world.getBlockState(mPos);
            BlockState blockAbove = world.getBlockState(mPos.up());
            if (blockBelow.isSolidBlock(world, mPos.down()) && blockHere.isAir() && blockAbove.isAir()) {
                return mPos.toImmutable();
            }
        }
        return null;
    }

    /**
     * 从底部向上搜索：找到的第一个安全空地（通常是最低的），用于下界。
     */
    private BlockPos checkColumnFromBottom(ServerWorld world, int x, int z) {
        BlockPos.Mutable mPos = new BlockPos.Mutable(x, 0, z);
        for (int y = world.getBottomY() + 1; y <= 320; y++) {
            mPos.setY(y);
            BlockState blockBelow = world.getBlockState(mPos.down());
            BlockState blockHere = world.getBlockState(mPos);
            BlockState blockAbove = world.getBlockState(mPos.up());
            if (blockBelow.isSolidBlock(world, mPos.down()) && blockHere.isAir() && blockAbove.isAir()) {
                return mPos.toImmutable();
            }
        }
        return null;
    }
}