package com.hp.mechafauna.gametest;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Mechafauna.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FortressGameTests {
    private static final ResourceLocation STRUCTURE = ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "heavy_mech_fortress");

    private FortressGameTests() {
    }

    @GameTest(template = "heavy_mech_fortress", timeoutTicks = 160)
    public static void fortressNorth(GameTestHelper helper) {
        verifyFortress(helper);
    }

    @GameTest(template = "heavy_mech_fortress", rotationSteps = 1, timeoutTicks = 160)
    public static void fortressEast(GameTestHelper helper) {
        verifyFortress(helper);
    }

    @GameTest(template = "heavy_mech_fortress", rotationSteps = 2, timeoutTicks = 160)
    public static void fortressSouth(GameTestHelper helper) {
        verifyFortress(helper);
    }

    @GameTest(template = "heavy_mech_fortress", rotationSteps = 3, timeoutTicks = 160)
    public static void fortressWest(GameTestHelper helper) {
        verifyFortress(helper);
    }

    @GameTest(template = "heavy_mech_fortress", timeoutTicks = 160)
    public static void fortressSpawnersProduceMobs(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "FortressTest"));
        var initialPos = helper.absolutePos(new BlockPos(10, 5, 18));
        player.moveTo(initialPos.getX() + 0.5, initialPos.getY(), initialPos.getZ() + 0.5, 0, 0);
        level.addNewPlayer(player);
        try {
            int[][] locations = {{8, 4, 16}, {40, 4, 16}, {8, 4, 35}, {40, 4, 35}};
            for (int i = 0; i < locations.length; i++) {
                int[] location = locations[i];
                var pos = helper.absolutePos(new BlockPos(location[0], location[1] + 1, location[2]));
                var spawner = (SpawnerBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(spawner != null, "Missing spawner during spawn test");
                player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 2.5, 0, 0);
                CompoundTag nbt = spawner.saveWithoutMetadata(level.registryAccess());
                nbt.putShort("Delay", (short) 0);
                spawner.getSpawner().load(level, pos, nbt);
                for (int attempt = 0; attempt < 100; attempt++) {
                    spawner.getSpawner().serverTick(level, pos);
                }
                var type = i == 0 || i == 3 ? ModEntities.MECHANICAL_WOLF.get() : ModEntities.MECHANICAL_SPIDER.get();
                helper.assertTrue(!level.getEntities(type, new AABB(pos).inflate(5), entity -> entity.isAlive()).isEmpty(),
                    "Spawner did not produce its configured mechanical mob");
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    private static void verifyFortress(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STRUCTURE) != null,
            "Fortress worldgen structure is missing");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL).get(STRUCTURE) != null,
            "Fortress template pool is missing");
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(STRUCTURE) != null,
            "Fortress structure set is missing");
        var structure = (JigsawStructure) level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(STRUCTURE);
        var generator = level.getChunkSource().getGenerator();
        var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
            level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
            new ChunkPos(helper.absolutePos(BlockPos.ZERO)), level, biome -> structure.biomes().contains(biome));
        var start = structure.findGenerationPoint(context);
        helper.assertTrue(start.isPresent() && !start.orElseThrow().getPiecesBuilder().isEmpty(),
            "Fortress jigsaw generation produced no pieces");
        var mechs = helper.getEntities(ModEntities.HEAVY_MECH.get());
        helper.assertTrue(mechs.size() == 1, "Fortress must contain exactly one heavy mech");
        var mech = mechs.getFirst();
        helper.assertTrue(mech.isPersistenceRequired(), "Fortress guard must not naturally despawn");
        helper.assertTrue(level.noCollision(mech.getBoundingBox()), "Fortress guard intersects the building");
        var crawlers = helper.getEntities(ModEntities.FLAME_CRAWLER.get());
        helper.assertTrue(crawlers.size() == 2, "The second floor must contain exactly two flame crawlers");
        for (var crawler : crawlers) {
            helper.assertTrue(crawler.isPersistenceRequired(), "Fixed flame crawlers must not naturally despawn");
            helper.assertTrue(level.noCollision(crawler.getBoundingBox()), "A fixed flame crawler intersects the building");
        }
        helper.assertTrue(helper.getEntities(ModEntities.MECHANICAL_TANK.get()).isEmpty(), "Fortress must not contain a tank");
        mech.setNoAi(true);

        int spawners = 0;
        int chests = 0;
        int armoryChests = 0;
        Set<String> spawnIds = new HashSet<>();
        for (var local : BlockPos.betweenClosed(0, 0, 0, 56, 22, 44)) {
            var pos = helper.absolutePos(local.above());
            var state = level.getBlockState(pos);
            if (state.is(Blocks.SPAWNER)) {
                spawners++;
                helper.assertTrue(level.getBlockEntity(pos) instanceof SpawnerBlockEntity, "Spawner block entity is missing");
                var spawner = (SpawnerBlockEntity) level.getBlockEntity(pos);
                CompoundTag nbt = spawner.saveWithoutMetadata(level.registryAccess());
                SpawnData data = SpawnData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData")).getOrThrow();
                String spawnId = data.getEntityToSpawn().getString("id");
                spawnIds.add(spawnId);
                helper.assertTrue(data.getCustomSpawnRules().isPresent(), "Lit fortress requires custom spawner light rules");
                if (spawnId.equals("mechafauna:prism_sentinel")) {
                    helper.assertTrue(nbt.getShort("SpawnCount") == 1 && nbt.getShort("MaxNearbyEntities") == 1,
                        "Prism Sentinel population limit is incorrect");
                    helper.assertTrue(nbt.getShort("MinSpawnDelay") == 3600 && nbt.getShort("MaxSpawnDelay") == 7200,
                        "Prism Sentinel rarity cooldown is incorrect");
                    helper.assertTrue(data.getCustomSpawnRules().orElseThrow().skyLightLimit().maxInclusive() == 7,
                        "Prism Sentinel sky light limit is incorrect");
                } else {
                    helper.assertTrue(data.getCustomSpawnRules().orElseThrow().isValidPosition(pos, level), "Spawner rejects fortress lighting");
                    helper.assertTrue(nbt.getShort("SpawnCount") == 1 && nbt.getShort("MaxNearbyEntities") == 3,
                        "Spawner population limits are incorrect");
                    helper.assertTrue(nbt.getShort("MinSpawnDelay") == 500 && nbt.getShort("MaxSpawnDelay") == 800,
                        "Spawner cooldown is incorrect");
                }
            } else if (state.is(Blocks.CHEST)) {
                chests++;
                helper.assertTrue(level.getBlockEntity(pos) instanceof ChestBlockEntity, "Loot chest block entity is missing");
                var chest = (ChestBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(chest.getLootTable() != null, "Loot table was not retained during placement");
                var loot = chest.getLootTable().location();
                helper.assertTrue(loot.getNamespace().equals(Mechafauna.MOD_ID), "Unexpected fortress loot table");
                if (loot.getPath().equals("chests/fortress_armory")) {
                    armoryChests++;
                }
                helper.assertTrue(!chest.isEmpty(), "Fortress loot table produced an empty chest");
                helper.assertTrue(level.getBlockState(pos.above()).isAir(), "Loot chest lid is obstructed");
            }
        }
        helper.assertTrue(spawners == 5, "Fortress must contain five spawners");
        helper.assertTrue(chests == 5 && armoryChests == 1, "Fortress requires four supply chests and one armory chest");
        helper.assertTrue(spawnIds.equals(Set.of("mechafauna:mechanical_wolf", "mechafauna:mechanical_spider", "mechafauna:prism_sentinel")),
            "Fortress spawners contain an unexpected entity");
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(24, 5, 40));
        helper.assertBlockPresent(Blocks.STONE_BRICK_STAIRS, new BlockPos(9, 5, 30));
        helper.assertBlockPresent(Blocks.STONE_BRICK_STAIRS, new BlockPos(39, 11, 24));
        helper.succeed();
    }
}
