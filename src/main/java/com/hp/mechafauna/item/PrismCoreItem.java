package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.PrismBeam;
import com.hp.mechafauna.registry.ModEntities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
import net.neoforged.neoforge.entity.PartEntity;

public final class PrismCoreItem extends Item {
    private static final int MAX_FIRE_TICKS = 100;
    private static final int COOLDOWN_TICKS = 100;
    private static final int DAMAGE_INTERVAL = 5;
    private static final double LASER_RANGE = 24.0;
    private static final double BEAM_DROP = 0.45;
    private static final float LASER_DAMAGE = 4.0F;

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
        return MAX_FIRE_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (entity instanceof Player player && level instanceof ServerLevel serverLevel) {
            int usedTicks = MAX_FIRE_TICKS - remainingUseTicks;
            this.fireLaser(serverLevel, player, usedTicks % DAMAGE_INTERVAL == 0);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        this.beginCooldown(entity);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        this.beginCooldown(entity);
        return stack;
    }

    private void beginCooldown(LivingEntity entity) {
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
    }

    private void fireLaser(ServerLevel serverLevel, Player player, boolean damage) {
        Vec3 eye = player.getEyePosition();
        Vec3 aimPoint = eye.add(player.getLookAngle().scale(LASER_RANGE));
        Vec3 origin = eye.subtract(0.0, BEAM_DROP, 0.0);
        Vec3 direction = aimPoint.subtract(origin);
        direction = direction.lengthSqr() < 1.0E-6 ? player.getLookAngle() : direction.normalize();
        Vec3 maxEnd = origin.add(direction.scale(LASER_RANGE));
        BlockHitResult blockHit = serverLevel.clip(new ClipContext(origin, maxEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        float beamLength = (float)origin.distanceTo(end);
        float horizontal = (float)Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float)Math.toDegrees(-Mth.atan2(direction.x, direction.z));
        float pitch = (float)Math.toDegrees(-Math.atan2(direction.y, horizontal));

        PrismBeam beam = this.activeBeam(serverLevel, player);
        if (beam == null) {
            beam = ModEntities.PRISM_BEAM.get().create(serverLevel);
            if (beam == null) {
                return;
            }
            beam.configure(player, origin, yaw, pitch, beamLength);
            serverLevel.addFreshEntity(beam);
        } else {
            beam.configure(player, origin, yaw, pitch, beamLength);
        }
        if (!damage) {
            return;
        }

        List<TargetHit> hits = new ArrayList<>();
        // 武器部位伸出主体碰撞箱，搜索范围要盖住这些部位，再按光束精确判定
        for (LivingEntity target : serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(origin, end).inflate(6.0), candidate -> candidate != player && candidate.isAlive()
                && !player.isAlliedTo(candidate))) {
            if (target.isMultipartEntity() && target.getParts() != null) {
                for (PartEntity<?> part : target.getParts()) {
                    if (part.isPickable()) {
                        this.collectHit(hits, part, origin, end);
                    }
                }
            } else {
                this.collectHit(hits, target, origin, end);
            }
        }
        hits.sort(Comparator.comparingDouble(TargetHit::distanceSquared));
        var source = player.damageSources().playerAttack(player);
        for (TargetHit hit : hits) {
            if (!hit.target().hurt(source, LASER_DAMAGE)) {
                continue;
            }
            Entity burning = hit.target() instanceof PartEntity<?> part ? part.getParent() : hit.target();
            if (burning instanceof LivingEntity living) {
                living.igniteForSeconds(2.0F);
            }
        }
    }

    private void collectHit(List<TargetHit> hits, Entity target, Vec3 origin, Vec3 end) {
        AABB targetBox = target.getBoundingBox().inflate(0.09);
        Vec3 hitPoint = targetBox.contains(origin) ? origin : targetBox.clip(origin, end).orElse(null);
        if (hitPoint != null) {
            hits.add(new TargetHit(target, origin.distanceToSqr(hitPoint)));
        }
    }

    private PrismBeam activeBeam(ServerLevel level, Player player) {
        return level.getEntitiesOfClass(PrismBeam.class, player.getBoundingBox().inflate(4.0), beam -> beam.ownedBy(player))
            .stream().findFirst().orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.mechafauna.prism_core.tooltip"));
    }

    private record TargetHit(Entity target, double distanceSquared) {
    }
}
