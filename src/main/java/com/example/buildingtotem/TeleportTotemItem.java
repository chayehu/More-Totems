package com.example.buildingtotem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

public class TeleportTotemItem extends Item {
    public TeleportTotemItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient()) {
            if (context.getPlayer() != null) {
                // 将传送点绑在右键方块面向玩家的那一侧
                TeleportTotemEffect.BOUND_POSITIONS.put(
                        context.getPlayer().getUuid(),
                        context.getBlockPos().offset(context.getSide())
                );
                context.getPlayer().sendMessage(
                        Text.translatable("item.building-totem.teleport_totem.bind")
                                .formatted(Formatting.GREEN),
                        true
                );
            }
        }
        return ActionResult.SUCCESS;
    }
}