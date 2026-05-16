package com.example.buildingtotem;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;

public interface TotemEffect {
    void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world);
}