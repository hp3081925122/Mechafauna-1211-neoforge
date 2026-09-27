package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class WeaponizedDroneItem extends Item {
    public WeaponizedDroneItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        MechanicalDrone drone = ModEntities.MECHANICAL_DRONE.get().create(serverLevel);
        if (drone == null) {
            return InteractionResultHolder.fail(stack);
        }
        drone.setOwner(player);
        drone.moveTo(player.getX() + player.getLookAngle().x * 2.5, player.getY() + 0.7, player.getZ() + player.getLookAngle().z * 2.5, player.getYRot(), 0.0F);
        if (!serverLevel.noCollision(drone, drone.getBoundingBox()) || !serverLevel.addFreshEntity(drone)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
