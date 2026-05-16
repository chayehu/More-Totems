package com.example.buildingtotem;

import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportTotemEffect implements TotemEffect {

    public static final Map<UUID, BlockPos> BOUND_POSITIONS = new HashMap<>();

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        BlockPos targetPos = BOUND_POSITIONS.get(player.getUuid());
        if (targetPos == null) return;

        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY();
        double z = targetPos.getZ() + 0.5;

        player.requestTeleport(x, y, z);

        world.playSound(null, targetPos, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.spawnParticles(ParticleTypes.PORTAL,
                x, y + 1.0, z, 30, 0.5, 1.0, 0.5, 0.1);
    }
}