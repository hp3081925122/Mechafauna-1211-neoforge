package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class FlyingSaucerLocatorItem extends Item {
    public FlyingSaucerLocatorItem() {
        super(new Item.Properties().stacksTo(1));
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
        double x = player.getX() + forward.x * 16.0;
        double y = player.getY() + 8.0;
        double z = player.getZ() + forward.z * 16.0;
        FlyingSaucerBoss boss = ModEntities.FLYING_SAUCER_BOSS.get().create(serverLevel);
        if (boss == null) {
            return InteractionResultHolder.fail(stack);
        }
        float yaw = (float)(Math.atan2(player.getZ() - z, player.getX() - x) * 180.0 / Math.PI) - 90.0F;
        boss.moveTo(x, y, z, yaw, 0.0F);
        boss.setYHeadRot(yaw);
        boss.yBodyRot = yaw;
        boss.setTarget(player);
        if (!serverLevel.noCollision(boss, boss.getBoundingBox())) {
            boss.discard();
            player.displayClientMessage(Component.translatable("message.mechafauna.insufficient_space").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!serverLevel.addFreshEntity(boss)) {
            boss.discard();
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) {
            stack.consume(1, player);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mechafauna.flying_saucer_locator.tooltip.obtain"));
        tooltip.add(Component.translatable("item.mechafauna.flying_saucer_locator.tooltip.use"));
    }
}
