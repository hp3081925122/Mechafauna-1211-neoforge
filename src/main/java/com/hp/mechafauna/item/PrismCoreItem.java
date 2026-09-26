package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.PrismBeam;
import com.hp.mechafauna.registry.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PrismCoreItem extends Item {
    private static final int CHARGE_TICKS = 20;
    private static final int COOLDOWN_TICKS = 60;
    private static final double LASER_RANGE = 24.0;
    private static final float LASER_DAMAGE = 8.0F;

    public PrismCoreItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        int chargedTicks = this.getUseDuration(stack, entity) - remainingUseTicks;
        if (!(entity instanceof Player player) || chargedTicks < CHARGE_TICKS) {
            return;
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 origin = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 maxEnd = origin.add(direction.scale(LASER_RANGE));
        BlockHitResult blockHit = serverLevel.clip(new ClipContext(origin, maxEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        float beamLength = (float)origin.distanceTo(end);

        PrismBeam beam = ModEntities.PRISM_BEAM.get().create(serverLevel);
        if (beam != null) {
            beam.configure(origin, player.getYRot(), player.getXRot(), beamLength);
            serverLevel.addFreshEntity(beam);
        }

        List<TargetHit> hits = new ArrayList<>();
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(origin, end).inflate(0.12), candidate -> candidate != player && candidate.isAlive()
                && !player.isAlliedTo(candidate))) {
            AABB targetBox = target.getBoundingBox().inflate(0.09);
            Vec3 hitPoint = targetBox.contains(origin) ? origin : targetBox.clip(origin, end).orElse(null);
            if (hitPoint != null) {
                hits.add(new TargetHit(target, origin.distanceToSqr(hitPoint)));
            }
        }
        hits.sort(Comparator.comparingDouble(TargetHit::distanceSquared));
        for (TargetHit hit : hits) {
            if (hit.target().hurt(player.damageSources().playerAttack(player), LASER_DAMAGE)) {
                hit.target().igniteForSeconds(4.0F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mechafauna.prism_core.tooltip"));
    }

    private record TargetHit(LivingEntity target, double distanceSquared) {
    }
}
