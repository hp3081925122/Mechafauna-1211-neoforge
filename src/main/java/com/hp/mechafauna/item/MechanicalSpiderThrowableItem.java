package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.registry.ModEntities;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

public final class MechanicalSpiderThrowableItem extends Item {
    public MechanicalSpiderThrowableItem(Item.Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        MechanicalSpider spider = ModEntities.MECHANICAL_SPIDER.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (spider == null) {
            return InteractionResult.FAIL;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 look = player.getLookAngle();
        spider.setPos(player.getX() + forward.x * 1.55, player.getY() + 0.9, player.getZ() + forward.z * 1.55);
        spider.setYRot(player.getYRot());
        spider.setXRot(player.getXRot());
        spider.launchFromItem(player.getUUID(), look.scale(1.15).add(0.0, 0.22, 0.0));
        if (!serverLevel.noCollision(spider, spider.getBoundingBox()) || !serverLevel.addFreshEntity(spider)) {
            spider.discard();
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }
        return InteractionResult.CONSUME;
    }
}
