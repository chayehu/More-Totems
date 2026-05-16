package com.example.buildingtotem;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.BlockRotation;

public class BuildingTotemEffect implements TotemEffect {

    private static final Identifier STRUCTURE_ID = Identifier.of("building-totem", "main_building");

    // 动画参数
    private static final int ANIMATION_TICKS = 100;
    private static final double SPIRAL_RADIUS = 1.0;
    private static final double SPIRAL_HEIGHT = 5.0;
    private static final double CIRCLE_RADIUS = 4.5;
    private static final int CIRCLE_POINTS = 50;
    private static final int STAR_EDGE_POINTS = 5;
    private static final int CIRCLE_COLOR = 0x9932CC;   // 紫色
    private static final int STAR_COLOR = 0xFFFFFF;     // 白色

    @Override
    public void onTrigger(ServerPlayerEntity player, ItemStack totemStack, ServerWorld world) {
        Direction playerFacing = player.getHorizontalFacing();
        BlockRotation rotation = getRotationToFace(Direction.SOUTH, playerFacing);
        BlockPos placementPos = player.getBlockPos()
                .offset(playerFacing.getOpposite(), 1)
                .offset(playerFacing.rotateYClockwise(), 7);

        startMagicAnimation(world, placementPos, rotation, player.getBlockPos());

        // 召唤 3 个铁傀儡
        for (int i = 0; i < 3; i++) {
            IronGolemEntity golem = new IronGolemEntity(EntityType.IRON_GOLEM, world);
            double angle = world.random.nextDouble() * Math.PI * 2;
            double distance = 2.0 + world.random.nextDouble() * 1.5;
            golem.refreshPositionAndAngles(
                    player.getX() + Math.cos(angle) * distance,
                    player.getY(),
                    player.getZ() + Math.sin(angle) * distance,
                    world.random.nextFloat() * 360.0F, 0.0F);
            golem.setPlayerCreated(true);
            world.spawnEntity(golem);
        }
    }

    // ---- 建筑动画（魔法阵 + 生成） ----
    private void startMagicAnimation(ServerWorld world, BlockPos buildingPos,
                                     BlockRotation rotation, BlockPos playerPos) {
        StructureTemplate template = world.getStructureTemplateManager()
                .getTemplate(STRUCTURE_ID).orElse(null);
        if (template == null) {
            // 找不到结构时生成金块柱子
            for (int y = 0; y < 10; y++) {
                world.setBlockState(buildingPos.up(y), net.minecraft.block.Blocks.GOLD_BLOCK.getDefaultState());
            }
            return;
        }
        StructurePlacementData placementData = new StructurePlacementData().setRotation(rotation);
        scheduleMagicTicks(world, buildingPos, template, placementData, playerPos, 0);
    }

    private void scheduleMagicTicks(ServerWorld world, BlockPos buildingPos,
                                    StructureTemplate template,
                                    StructurePlacementData placementData,
                                    BlockPos playerPos, int tick) {
        if (tick >= ANIMATION_TICKS) {
            // 放置建筑
            template.place(world, buildingPos, buildingPos, placementData, world.getRandom(), 2);
            // 顶部庆祝粒子
            world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    playerPos.getX() + 0.5, playerPos.getY() + 2.0, playerPos.getZ() + 0.5,
                    50, 0.5, 0.5, 0.5, 0.1);
            return;
        }

        double progress = (double) tick / ANIMATION_TICKS;

        // 螺旋粒子
        double spiralHeight = playerPos.getY() + 0.5 + progress * SPIRAL_HEIGHT;
        double spiralAngle = Math.toRadians(tick * 30.0);
        double sx = playerPos.getX() + 0.5 + SPIRAL_RADIUS * Math.cos(spiralAngle);
        double sz = playerPos.getZ() + 0.5 + SPIRAL_RADIUS * Math.sin(spiralAngle);
        world.spawnParticles(ParticleTypes.END_ROD, sx, spiralHeight, sz, 1, 0, 0, 0, 0);

        // 紫色光圈
        double groundY = playerPos.getY() + 0.05;
        double circleAngleOffset = Math.toRadians(tick * 5.0);
        DustParticleEffect purpleDust = new DustParticleEffect(CIRCLE_COLOR, 1.5f);
        for (int i = 0; i < CIRCLE_POINTS; i++) {
            double angle = circleAngleOffset + Math.toRadians(i * 360.0 / CIRCLE_POINTS);
            double cx = playerPos.getX() + 0.5 + CIRCLE_RADIUS * Math.cos(angle);
            double cz = playerPos.getZ() + 0.5 + CIRCLE_RADIUS * Math.sin(angle);
            world.spawnParticles(purpleDust, cx, groundY, cz, 0, 0, 0, 0, 0);
        }

        // 五角星
        DustParticleEffect starDust = new DustParticleEffect(STAR_COLOR, 2.0f);
        int vertices = 5;
        double[] angles = new double[vertices];
        for (int v = 0; v < vertices; v++) {
            angles[v] = circleAngleOffset + Math.toRadians(v * 72.0 + 18.0);
        }
        for (int i = 0; i < vertices; i++) {
            int j = (i + 2) % vertices;
            double startAngle = angles[i];
            double endAngle = angles[j];
            for (int k = 0; k <= STAR_EDGE_POINTS; k++) {
                double t = (double) k / STAR_EDGE_POINTS;
                double angle = startAngle + (endAngle - startAngle) * t;
                double vx = playerPos.getX() + 0.5 + CIRCLE_RADIUS * Math.cos(angle);
                double vz = playerPos.getZ() + 0.5 + CIRCLE_RADIUS * Math.sin(angle);
                world.spawnParticles(starDust, vx, groundY, vz, 0, 0, 0, 0, 0);
            }
        }

        world.getServer().execute(() ->
                scheduleMagicTicks(world, buildingPos, template, placementData, playerPos, tick + 1));
    }

    // ---- 朝向计算 ----
    private BlockRotation getRotationToFace(Direction from, Direction to) {
        int fromIdx = directionToIndex(from);
        int toIdx = directionToIndex(to);
        int diff = (toIdx - fromIdx + 4) % 4;
        return switch (diff) {
            case 1 -> BlockRotation.CLOCKWISE_90;
            case 2 -> BlockRotation.CLOCKWISE_180;
            case 3 -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    private int directionToIndex(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST  -> 1;
            case NORTH -> 2;
            case EAST  -> 3;
            default   -> 0;
        };
    }
}