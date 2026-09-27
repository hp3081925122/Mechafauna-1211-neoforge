package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.MechProjectile;
import com.hp.mechafauna.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.hp.mechafauna.client.renderer.HeavyMechMachineGunRenderer;
import java.util.function.Consumer;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

public final class HeavyMechMachineGunItem extends Item implements GeoItem {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_DURABILITY = 1500;
    private static final int MAX_FIRE_TICKS = 200;
    private static final int SHOT_INTERVAL_TICKS = 2;
    private static final int IGNITION_THRESHOLD_TICKS = 60;
    private static final double MAX_TARGET_RANGE = 72.0;
    private static final double VOLLEY_SPREAD_SLOPE = Math.tan(Math.toRadians(5.0));
    private static final double BULLET_JITTER_SLOPE = Math.tan(Math.toRadians(1.5));
    private static final float BULLET_DAMAGE = 4.0F;
    private static final float BULLET_SPEED = 0.72F;
    private static final double MUZZLE_FORWARD_OFFSET = 1.9;
    private static final double MUZZLE_SIDE_OFFSET = 0.84;
    private static final double MUZZLE_DOWN_OFFSET = 0.30;
    private static final RawAnimation FIRE_ANIMATION = RawAnimation.begin().thenLoop("animation.heavy_mech_machine_gun.fire");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.heavy_mech_machine_gun.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final int[][] VOLLEY_PATTERN = {
        {0, 0},
        {0, 1},
        {0, -1},
        {-1, 0},
        {1, 0},
        {1, 1},
        {1, -1},
        {-1, 1},
        {-1, -1}
    };

    public HeavyMechMachineGunItem(Item.Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private HeavyMechMachineGunRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new HeavyMechMachineGunRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 持有者正在按住使用时循环开火动画，否则保持待机隐藏枪口火焰
        controllers.add(new AnimationController<>("fire", 0, state -> {
            Long instanceId = state.getDataOrDefault(DataTickets.ANIMATABLE_INSTANCE_ID, Long.MAX_VALUE);
            if (HeavyMechMachineGunClientState.isFiring(instanceId)) {
                return state.setAndContinue(FIRE_ANIMATION);
            }
            return state.setAndContinue(IDLE_ANIMATION);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mechafauna.heavy_mech_machine_gun.tooltip").withStyle(ChatFormatting.GRAY));
        if (ReinforcementCoreItem.hasBulletDamageUpgrade(stack)) {
            tooltip.accept(Component.translatable("item.mechafauna.reinforcement_core.weapon_upgrade").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return MAX_FIRE_TICKS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, net.minecraft.world.entity.EquipmentSlot slot) {
        if (entity instanceof Player player && GeoItem.getId(stack) == Long.MAX_VALUE) {
            long instanceId = GeoItem.getOrAssignId(stack, level);
            if (instanceId != Long.MAX_VALUE) {
                LOGGER.debug("Assigned machine gun animation stack id={} player={} slot={}", instanceId, player.getId(), slot);
            }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int usedTicks = MAX_FIRE_TICKS - remainingUseTicks;
        if (usedTicks % SHOT_INTERVAL_TICKS != 0) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            this.fireVolley(serverLevel, player, stack, usedTicks > IGNITION_THRESHOLD_TICKS);
        } else if (level.isClientSide()) {
            HeavyMechMachineGunClientState.spawnFirstPersonMuzzleFlash(level, player);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingUseTicks) {
        if (entity instanceof Player player) {
            int usedTicks = Math.max(0, Math.min(MAX_FIRE_TICKS, MAX_FIRE_TICKS - remainingUseTicks));
            if (usedTicks > 0) {
                player.getCooldowns().addCooldown(stack, usedTicks);
            }
            return true;
        }
        return false;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(stack, MAX_FIRE_TICKS);
        }
        return stack;
    }

    private void fireVolley(ServerLevel level, Player player, ItemStack stack, boolean incendiary) {
        Vec3 lookDirection = player.getLookAngle().normalize();
        Vec3 muzzle = this.muzzlePosition(level, player, lookDirection);
        Vec3 aimPoint = this.findAimPoint(level, player, player.getEyePosition(), lookDirection);
        Vec3 direction = aimPoint.subtract(muzzle).normalize();
        Vec3 reference = Math.abs(direction.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        LOGGER.debug("Machine gun volley player={} count={} muzzleOffset={} aimDistance={} spreadDegrees=5", player.getId(), VOLLEY_PATTERN.length, muzzle.subtract(player.getEyePosition()), aimPoint.subtract(muzzle).length());
        for (int[] offset : VOLLEY_PATTERN) {
            double horizontalJitter = (level.getRandom().nextDouble() * 2.0 - 1.0) * BULLET_JITTER_SLOPE;
            double verticalJitter = (level.getRandom().nextDouble() * 2.0 - 1.0) * BULLET_JITTER_SLOPE;
            Vec3 shotDirection = direction.add(right.scale(offset[0] * VOLLEY_SPREAD_SLOPE + horizontalJitter))
                .add(up.scale(offset[1] * VOLLEY_SPREAD_SLOPE + verticalJitter)).normalize();
            boolean reinforced = ReinforcementCoreItem.hasBulletDamageUpgrade(stack);
            float bulletDamage = reinforced ? BULLET_DAMAGE * 0.25F : BULLET_DAMAGE;
            MechProjectile projectile = new MechProjectile(
                ModEntities.MECH_PROJECTILE.get(), level, player, false, true, bulletDamage);
            projectile.setBypassHurtCooldown(reinforced);
            projectile.setReinforcedBulletDamage(reinforced);
            projectile.setIgniteOnHit(incendiary);
            projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
            projectile.shoot(shotDirection.x, shotDirection.y, shotDirection.z, BULLET_SPEED, 0.0F);
            level.addFreshEntity(projectile);
            stack.hurtAndBreak(1, player, player.getUsedItemHand().asEquipmentSlot());
            if (stack.isEmpty()) {
                return;
            }
        }
    }

    static Vec3 muzzlePosition(Level level, Player player, Vec3 lookDirection) {
        Vec3 eye = player.getEyePosition();
        Vec3 reference = new Vec3(0.0, 1.0, 0.0);
        Vec3 right;
        if (Math.abs(lookDirection.y) > 0.95) {
            Vec3 horizontalForward = Vec3.directionFromRotation(0.0F, player.getYRot());
            right = horizontalForward.cross(reference).normalize();
        } else {
            right = lookDirection.cross(reference).normalize();
        }
        Vec3 up = right.cross(lookDirection).normalize();
        double side = MUZZLE_SIDE_OFFSET;
        boolean mainHandUsed = player.getUsedItemHand() == InteractionHand.MAIN_HAND;
        if ((player.getMainArm() == HumanoidArm.LEFT) == mainHandUsed) {
            side = -side;
        }
        Vec3 muzzle = eye.add(lookDirection.scale(MUZZLE_FORWARD_OFFSET)).add(right.scale(side)).add(up.scale(-MUZZLE_DOWN_OFFSET));
        BlockHitResult blocked = level.clip(new ClipContext(eye, muzzle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blocked.getType() != HitResult.Type.MISS) {
            LOGGER.debug("Machine gun muzzle blocked player={} fallback=eye", player.getId());
            return eye.add(lookDirection.scale(0.3));
        }
        return muzzle;
    }

    private Vec3 findAimPoint(ServerLevel level, Player player, Vec3 start, Vec3 direction) {
        Vec3 maxEnd = start.add(direction.scale(MAX_TARGET_RANGE));
        BlockHitResult blockHit = level.clip(new ClipContext(start, maxEnd,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? maxEnd : blockHit.getLocation();
        Vec3 aimPoint = end;
        double nearestDistance = start.distanceToSqr(end);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
            new AABB(start, end).inflate(6.0), candidate -> candidate != player && candidate.isAlive()
                && !player.isAlliedTo(candidate))) {
            Vec3 hitPoint = target.getBoundingBox().inflate(0.09).clip(start, end).orElse(null);
            if (hitPoint != null) {
                double distance = start.distanceToSqr(hitPoint);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    aimPoint = hitPoint;
                }
            }
        }
        return aimPoint;
    }
}
