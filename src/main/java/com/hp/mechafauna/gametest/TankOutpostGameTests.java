package com.hp.mechafauna.gametest;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechafauna.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankOutpostGameTests {
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "tank_outpost");

    private TankOutpostGameTests() {
    }

    @GameTest(template = "tank_outpost", setupTicks = 5, timeoutTicks = 160)
    public static void outpostNorth(GameTestHelper helper) {
        verifyOutpost(helper);
    }

    @GameTest(template = "tank_outpost", rotationSteps = 1, setupTicks = 5, timeoutTicks = 160)
    public static void outpostEast(GameTestHelper helper) {
        verifyOutpost(helper);
    }

    @GameTest(template = "tank_outpost", rotationSteps = 2, setupTicks = 5, timeoutTicks = 160)
    public static void outpostSouth(GameTestHelper helper) {
        verifyOutpost(helper);
    }

    @GameTest(template = "tank_outpost", rotationSteps = 3, setupTicks = 5, timeoutTicks = 160)
    public static void outpostWest(GameTestHelper helper) {
        verifyOutpost(helper);
    }

    private static void verifyOutpost(GameTestHelper helper) {
        var level = helper.getLevel();
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STRUCTURE);
        helper.assertTrue(structure instanceof JigsawStructure, "Tank outpost worldgen structure is missing");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).get(STRUCTURE) != null,
            "Tank outpost template pool is missing");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(STRUCTURE) != null,
            "Tank outpost structure set is missing");
        var generator = level.getChunkSource().getGenerator();
        var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
            level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
            new ChunkPos(helper.absolutePos(BlockPos.ZERO)), level, biome -> structure.biomes().contains(biome));
        var start = ((JigsawStructure) structure).findGenerationPoint(context);
        helper.assertTrue(start.isPresent() && !start.orElseThrow().getPiecesBuilder().isEmpty(),
            "Tank outpost jigsaw generation produced no pieces");

        var tanks = helper.getEntities(ModEntities.MECHANICAL_TANK.get());
        helper.assertTrue(tanks.size() == 1, "Tank outpost must contain exactly one tank");
        var tank = tanks.getFirst();
        tank.setNoAi(true);
        helper.assertTrue(tank.isPersistenceRequired(), "The outpost tank must not naturally despawn");
        helper.assertTrue(tank.getBbWidth() > 3.4F && tank.getBbHeight() > 2.7F,
            "Tank scale was lost during template loading: width=" + tank.getBbWidth()
                + ", height=" + tank.getBbHeight() + ", scale=" + tank.getScale());
        helper.assertTrue(level.noCollision(tank, tank.getBoundingBox()), "Tank spawn intersects the building");
        var wolves = helper.getEntities(ModEntities.MECHANICAL_WOLF.get());
        helper.assertTrue(wolves.size() == 2, "Tank outpost must contain two fixed wolf guards");
        for (var wolf : wolves) {
            wolf.setNoAi(true);
            helper.assertTrue(wolf.isPersistenceRequired(), "Outpost wolf guard must not naturally despawn");
            helper.assertTrue(level.noCollision(wolf, wolf.getBoundingBox()), "Wolf guard intersects the building");
        }

        var origin = tank.position();
        float yaw = tank.getYRot();
        for (int z = 9; z <= 54; z += 3) {
            var point = helper.absoluteVec(new Vec3(28.5, 0.0, z + 0.5));
            tank.moveTo(point.x, origin.y, point.z, yaw, 0.0F);
            helper.assertTrue(level.noCollision(tank, tank.getBoundingBox()), "Tank cannot clear the garage-to-gate lane at z=" + z);
        }
        for (int step = 0; step < 16; step++) {
            double angle = step * Math.PI / 8.0;
            var point = helper.absoluteVec(new Vec3(28.5 + 8.0 * Math.cos(angle), 0.0, 29.5 + 8.0 * Math.sin(angle)));
            tank.moveTo(point.x, origin.y, point.z, yaw, 0.0F);
            helper.assertTrue(level.noCollision(tank, tank.getBoundingBox()), "Tank cannot clear its eight-block combat ring at step=" + step);
        }
        tank.moveTo(origin.x, origin.y, origin.z, yaw, 0.0F);

        int chests = 0;
        int armories = 0;
        int spawners = 0;
        for (var local : BlockPos.betweenClosed(0, 0, 0, 56, 19, 60)) {
            var pos = helper.absolutePos(local.above());
            var state = level.getBlockState(pos);
            if (state.is(Blocks.CHEST)) {
                chests++;
                helper.assertTrue(level.getBlockEntity(pos) instanceof ChestBlockEntity, "Outpost loot chest block entity is missing");
                var chest = (ChestBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(chest.getLootTable() != null, "Outpost chest lost its loot table");
                var loot = chest.getLootTable().location();
                if (loot.equals(ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "chests/fortress_armory"))) {
                    armories++;
                }
                helper.assertTrue(!chest.isEmpty(), "Outpost loot table generated an empty chest");
                helper.assertTrue(level.getBlockState(pos.above()).isAir(), "Outpost chest lid is obstructed");
            } else if (state.is(Blocks.SPAWNER)) {
                spawners++;
                helper.assertTrue(level.getBlockEntity(pos) instanceof SpawnerBlockEntity, "Outpost spawner block entity is missing");
                var spawner = (SpawnerBlockEntity) level.getBlockEntity(pos);
                var nbt = spawner.saveWithoutMetadata(level.registryAccess());
                var data = SpawnData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData")).getOrThrow();
                helper.assertTrue(data.getEntityToSpawn().getString("id").equals("mechafauna:mechanical_wolf"), "Outpost spawner contains an unexpected entity");
                helper.assertTrue(data.getCustomSpawnRules().isPresent()
                    && data.getCustomSpawnRules().orElseThrow().isValidPosition(pos, level), "Outpost spawner rejects workshop lighting");
                helper.assertTrue(nbt.getShort("SpawnCount") == 1 && nbt.getShort("MaxNearbyEntities") == 2,
                    "Outpost spawner population limits are incorrect");
            }
        }
        helper.assertTrue(chests == 4 && armories == 1, "Outpost must contain three supply chests and one armory chest");
        helper.assertTrue(spawners == 1, "Outpost must contain one reinforcement spawner");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(28, 5, 53));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(28, 13, 10));
        helper.assertBlockPresent(Blocks.STONE_BRICK_STAIRS, new BlockPos(9, 5, 12));
        helper.succeed();
    }
}
