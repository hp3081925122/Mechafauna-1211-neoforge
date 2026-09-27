package com.hp.mechafauna.gametest;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

final class StructureEntityTestUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StructureEntityTestUtil() {
    }

    static <T extends Entity> List<T> getEntities(GameTestHelper helper, EntityType<T> type, ResourceLocation structureId) {
        StructureTemplate template = helper.getLevel().getStructureManager().get(structureId).orElseThrow();
        Vec3i size = template.getSize();
        BlockPos first = helper.absolutePos(BlockPos.ZERO);
        BlockPos last = helper.absolutePos(new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1));
        AABB bounds = new AABB(first, last).inflate(1.0);
        List<T> entities = helper.getLevel().getEntities(type, bounds, Entity::isAlive);
        LOGGER.info("Structure entity scan structure={} bounds={} type={} matches={}", structureId, bounds, type,
            entities.stream().map(entity -> entity.getId() + "@" + entity.position()).toList());
        return entities;
    }
}
