package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WeaponizedDroneItem extends Item {
    public WeaponizedDroneItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        MechanicalDrone drone = ModEntities.MECHANICAL_DRONE.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (drone == null) {
            return InteractionResult.FAIL;
        }
        drone.setOwner(player);
        drone.setPos(player.getX() + player.getLookAngle().x * 2.5, player.getY() + 0.7, player.getZ() + player.getLookAngle().z * 2.5);
        drone.setYRot(player.getYRot());
        if (!serverLevel.noCollision(drone, drone.getBoundingBox()) || !serverLevel.addFreshEntity(drone)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }
        return InteractionResult.CONSUME;
    }
}
