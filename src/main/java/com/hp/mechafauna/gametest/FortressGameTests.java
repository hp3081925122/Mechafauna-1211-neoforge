package com.hp.mechafauna.gametest;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class FortressGameTests {
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "heavy_mech_fortress");

    private FortressGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        GameTestRegistration.register(event, "fortress_north", "heavy_mech_fortress", 160, 0, Rotation.NONE, FortressGameTests::verifyFortress);
        GameTestRegistration.register(event, "fortress_east", "heavy_mech_fortress", 160, 0, Rotation.CLOCKWISE_90, FortressGameTests::verifyFortress);
        GameTestRegistration.register(event, "fortress_south", "heavy_mech_fortress", 160, 0, Rotation.CLOCKWISE_180, FortressGameTests::verifyFortress);
        GameTestRegistration.register(event, "fortress_west", "heavy_mech_fortress", 160, 0, Rotation.COUNTERCLOCKWISE_90, FortressGameTests::verifyFortress);
        GameTestRegistration.register(event, "fortress_spawners", "heavy_mech_fortress", 160, 0, Rotation.NONE, FortressGameTests::fortressSpawnersProduceMobs);
    }

    public static void fortressNorth(GameTestHelper helper) {
        verifyFortress(helper);
    }

    public static void fortressSpawnersProduceMobs(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "FortressTest"));
        var initialPos = helper.absolutePos(new BlockPos(10, 5, 18));
        player.snapTo(initialPos.getX() + 0.5, initialPos.getY(), initialPos.getZ() + 0.5, 0, 0);
        level.addNewPlayer(player);
        try {
            int[][] locations = {{8, 4, 16}, {40, 4, 16}, {8, 4, 35}, {40, 4, 35}};
            for (int i = 0; i < locations.length; i++) {
                int[] location = locations[i];
                var pos = helper.absolutePos(new BlockPos(location[0], location[1], location[2]));
                var spawner = (SpawnerBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(spawner != null, "Missing spawner during spawn test");
                player.snapTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 2.5, 0, 0);
                CompoundTag nbt = spawner.saveWithoutMetadata(level.registryAccess());
                nbt.putShort("Delay", (short) 0);
                spawner.getSpawner().load(level, pos, TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), nbt));
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
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(STRUCTURE) != null,
            "Fortress worldgen structure is missing");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL).getValue(STRUCTURE) != null,
            "Fortress template pool is missing");
        helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getValue(STRUCTURE) != null,
            "Fortress structure set is missing");
        var structure = (JigsawStructure) level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(STRUCTURE);
        var generator = level.getChunkSource().getGenerator();
        var context = new Structure.GenerationContext(level.registryAccess(), generator, generator.getBiomeSource(),
            level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(),
            ChunkPos.containing(helper.absolutePos(BlockPos.ZERO)), level, biome -> structure.biomes().contains(biome));
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
                SpawnData data = SpawnData.CODEC.parse(NbtOps.INSTANCE, nbt.getCompound("SpawnData").orElseGet(CompoundTag::new)).getOrThrow();
                String spawnId = data.getEntityToSpawn().getString("id").orElse("");
                spawnIds.add(spawnId);
                helper.assertTrue(data.getCustomSpawnRules().isPresent(), "Lit fortress requires custom spawner light rules");
                if (spawnId.equals("mechafauna:prism_sentinel")) {
                    helper.assertTrue(nbt.getShort("SpawnCount").orElse((short)0) == 1 && nbt.getShort("MaxNearbyEntities").orElse((short)0) == 1,
                        "Prism Sentinel population limit is incorrect");
                    helper.assertTrue(nbt.getShort("MinSpawnDelay").orElse((short)0) == 3600 && nbt.getShort("MaxSpawnDelay").orElse((short)0) == 7200,
                        "Prism Sentinel rarity cooldown is incorrect");
                    helper.assertTrue(data.getCustomSpawnRules().orElseThrow().skyLightLimit().maxInclusive() == 7,
                        "Prism Sentinel sky light limit is incorrect");
                } else {
                    helper.assertTrue(data.getCustomSpawnRules().orElseThrow().isValidPosition(pos, level), "Spawner rejects fortress lighting");
                    helper.assertTrue(nbt.getShort("SpawnCount").orElse((short)0) == 1 && nbt.getShort("MaxNearbyEntities").orElse((short)0) == 3,
                        "Spawner population limits are incorrect");
                    helper.assertTrue(nbt.getShort("MinSpawnDelay").orElse((short)0) == 500 && nbt.getShort("MaxSpawnDelay").orElse((short)0) == 800,
                        "Spawner cooldown is incorrect");
                }
            } else if (state.is(Blocks.CHEST)) {
                chests++;
                helper.assertTrue(level.getBlockEntity(pos) instanceof ChestBlockEntity, "Loot chest block entity is missing");
                var chest = (ChestBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(chest.getLootTable() != null, "Loot table was not retained during placement");
                var loot = chest.getLootTable().identifier();
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
        helper.assertBlockPresent(Blocks.STONE_BRICK_STAIRS, new BlockPos(9, 4, 30));
        helper.assertBlockPresent(Blocks.STONE_BRICK_STAIRS, new BlockPos(39, 10, 24));
        helper.succeed();
    }
}
