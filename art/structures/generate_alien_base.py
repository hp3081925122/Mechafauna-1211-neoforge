# 外星基地结构模板生成脚本：外围守卫区 -> 实验室 -> 核心召唤场地
import math
import os
from nbt_writer import Short, write_nbt

HERE = os.path.dirname(os.path.abspath(__file__))
OUTPUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "data", "mechafauna", "structure", "alien_base", "base.nbt")
DATA_VERSION = 3955
# 补完自制方块后改为 True 重新生成
USE_CUSTOM_BLOCKS = False

# 规划的自制方块与当前使用的原版替身
BLOCK_PLAN = {
    "hull": ("mechafauna:alien_hull_plate", "minecraft:polished_deepslate"),
    "hull_damaged": ("mechafauna:damaged_alien_hull_plate", "minecraft:cracked_deepslate_tiles"),
    "hull_slab": ("mechafauna:alien_hull_slab", "minecraft:polished_deepslate_slab"),
    "panel": ("mechafauna:alien_alloy_panel", "minecraft:smooth_stone"),
    "grate": ("mechafauna:alien_floor_grate", "minecraft:deepslate_tiles"),
    "floor": ("mechafauna:alien_floor_plate", "minecraft:gray_concrete"),
    "strip": ("mechafauna:alien_energy_strip", "minecraft:sea_lantern"),
    "glass": ("mechafauna:alien_energy_glass", "minecraft:cyan_stained_glass"),
    "pipe": ("mechafauna:alien_conduit_pipe", "minecraft:waxed_oxidized_cut_copper"),
    "console": ("mechafauna:alien_console", "minecraft:observer"),
    "crate": ("mechafauna:alien_supply_crate", "minecraft:barrel"),
    "hazard": ("mechafauna:alien_hazard_plate", "minecraft:yellow_concrete"),
    "pylon_core": ("mechafauna:alien_energy_core", "minecraft:beacon"),
    "summoner": ("mechafauna:alien_summoning_core", "minecraft:lodestone"),
}

SIZE_X = 80
SIZE_Y = 25
SIZE_Z = 120
CLEAR_HEIGHT = 24
blocks = {}
entities = {}


def resolve(name, properties=None):
    # 规划名转换为方块 ID，原版 ID 原样使用
    if name in BLOCK_PLAN:
        custom, fallback = BLOCK_PLAN[name]
        block_id = fallback
        if USE_CUSTOM_BLOCKS:
            block_id = custom
        # 替身方块可能不支持方向属性，只保留通用属性
        if not USE_CUSTOM_BLOCKS and properties and name not in ("console", "crate", "hull_slab"):
            properties = None
        return block_id, properties
    return name, properties


def put(x, y, z, name, properties=None, nbt=None):
    # 写入单个方块，越界忽略
    if 0 <= x < SIZE_X and 0 <= y < SIZE_Y and 0 <= z < SIZE_Z:
        blocks[(x, y, z)] = resolve(name, properties)
        if nbt is not None:
            entities[(x, y, z)] = nbt
        else:
            entities.pop((x, y, z), None)


def fill(x1, y1, z1, x2, y2, z2, name, properties=None):
    # 长方体填充
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                put(x, y, z, name, properties)


def hollow(x1, y1, z1, x2, y2, z2, name):
    # 只填四面墙
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                if x in (x1, x2) or z in (z1, z2):
                    put(x, y, z, name)


def clear(x1, y1, z1, x2, y2, z2):
    fill(x1, y1, z1, x2, y2, z2, "minecraft:air")


def spawner(x, y, z, mob, count=2, nearby=4, min_delay=200, max_delay=500, spawn_range=4):
    # 刷怪笼：自定义光照规则可跳过怪物的亮度检查，白天也能刷
    nbt = {
        "id": "minecraft:mob_spawner",
        "Delay": Short(20),
        "MinSpawnDelay": Short(min_delay),
        "MaxSpawnDelay": Short(max_delay),
        "SpawnCount": Short(count),
        "MaxNearbyEntities": Short(nearby),
        "RequiredPlayerRange": Short(18),
        "SpawnRange": Short(spawn_range),
        "SpawnData": {
            "entity": {"id": mob},
            "custom_spawn_rules": {
                "block_light_limit": {"min_inclusive": 0, "max_inclusive": 15},
                "sky_light_limit": {"min_inclusive": 0, "max_inclusive": 15},
            },
        },
    }
    put(x, y, z, "minecraft:spawner", None, nbt)


def chest(x, y, z, loot, facing="north"):
    # 战利品箱，种子由结构放置时自动生成
    nbt = {"id": "minecraft:chest", "LootTable": "mechafauna:chests/" + loot}
    put(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"}, nbt)


def pylon(x, z, height):
    # 能量塔：外壳柱 + 发光核心 + 端地烛
    fill(x, 1, z, x, height, z, "hull")
    put(x, height - 2, z, "strip")
    put(x, height + 1, z, "strip")
    put(x, height + 2, z, "minecraft:end_rod", {"facing": "up"})


def ring(cx, cz, radius, y, name, width=0.5):
    # 地面圆环
    r = int(radius + width + 1)
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            if abs(math.hypot(x - cx, z - cz) - radius) <= width:
                put(x, y, z, name)

# 布局占位
