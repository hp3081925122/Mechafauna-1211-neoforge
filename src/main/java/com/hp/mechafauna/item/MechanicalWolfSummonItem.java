package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.RideableMechanicalWolf;
import com.hp.mechafauna.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class MechanicalWolfSummonItem extends Item {
    public MechanicalWolfSummonItem() {
        super(new Item.Properties().stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Vec3 point = context.getClickedPos().relative(context.getClickedFace()).getCenter();
        return this.spawn(serverLevel, player, context.getItemInHand(), point.x, context.getClickedPos().relative(context.getClickedFace()).getY(), point.z)
            ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        Vec3 forward = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (forward.lengthSqr() < 1.0E-4) {
            forward = player.getForward();
        }
        forward = forward.normalize();
        boolean spawned = this.spawn(serverLevel, player, stack,
            player.getX() + forward.x * 1.6, player.getY(), player.getZ() + forward.z * 1.6);
        return spawned ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private boolean spawn(ServerLevel level, Player player, ItemStack stack, double x, double y, double z) {
        RideableMechanicalWolf wolf = ModEntities.RIDEABLE_MECHANICAL_WOLF.get().create(level);
        if (wolf == null) {
            return false;
        }
        wolf.setOwner(player);
        wolf.moveTo(x, y, z, player.getYRot(), 0.0F);
        if (!level.noCollision(wolf, wolf.getBoundingBox()) || !level.addFreshEntity(wolf)) {
            wolf.discard();
            return false;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mechafauna.mechanical_wolf_summon.tooltip"));
    }
}
