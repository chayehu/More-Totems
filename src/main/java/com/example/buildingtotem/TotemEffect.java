package com.example.buildingtotem;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public interface TotemEffect {
    void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world);
}