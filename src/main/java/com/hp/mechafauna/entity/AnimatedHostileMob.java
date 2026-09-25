package com.hp.mechafauna.entity;

import com.hp.mechafauna.entity.ai.CombatGoal;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class AnimatedHostileMob extends Monster implements GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<String> DATA_ACTION = SynchedEntityData.defineId(AnimatedHostileMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_HAS_TARGET = SynchedEntityData.defineId(AnimatedHostileMob.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int activeActionTicks;
    private int lastAimTargetId = -1;
    private boolean movementAnimationActive;
    private String lastMovementAnimation;

    protected AnimatedHostileMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (this.lastAimTargetId != -1) {
                LOGGER.debug("Mech aim target lost entity={} target={}", this.getId(), this.lastAimTargetId);
                this.lastAimTargetId = -1;
            }
            return;
        }
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float targetYaw = dx * dx + dz * dz > 1.0E-6
            ? (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F
            : this.yBodyRot;
        if (this.lastAimTargetId != target.getId()) {
            LOGGER.debug("Mech aim target acquired entity={} target={} bodyYaw={} targetYaw={}", this.getId(), target.getId(), this.yBodyRot, targetYaw);
            this.lastAimTargetId = target.getId();
        }
        float bodyYaw = Mth.approachDegrees(this.yBodyRot, targetYaw, this.getCombatTurnSpeed());
        this.setYRot(bodyYaw);
        this.setYBodyRot(bodyYaw);
        this.setYHeadRot(targetYaw);
    }

    @Override
    protected float tickHeadTurn(float bodyYaw, float headYaw) {
        if (this.hasCombatTarget()) {
            this.yBodyRot = this.getYRot();
            return headYaw;
        }
        return super.tickHeadTurn(bodyYaw, headYaw);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, "");
        builder.define(DATA_HAS_TARGET, false);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (!this.level().isClientSide) {
            LivingEntity actualTarget = this.getTarget();
            this.entityData.set(DATA_HAS_TARGET, actualTarget != null && actualTarget.isAlive());
        }
    }

    public final boolean hasCombatTarget() {
        return this.entityData.get(DATA_HAS_TARGET);
    }

    protected static AttributeSupplier.Builder createAttributes(double health, double speed, double armor, double followRange, double knockbackResistance) {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, health)
            .add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.ARMOR, armor)
            .add(Attributes.FOLLOW_RANGE, followRange)
            .add(Attributes.KNOCKBACK_RESISTANCE, knockbackResistance);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal(this));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public final void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AnimatedHostileMob> action = new AnimationController<>(this, "action", this.getActionTransitionTicks(), state -> PlayState.STOP);
        AnimationController<AnimatedHostileMob> movement = new AnimationController<>(this, "movement", 3, state -> this.updateMovementAnimation(state, action));
        for (Map.Entry<String, String> entry : this.getActionAnimations().entrySet()) {
            action.triggerableAnim(entry.getKey(), RawAnimation.begin().thenPlay(entry.getValue()));
        }
        controllers.add(movement, action);
        this.addExtraControllers(controllers);
    }

    private PlayState updateMovementAnimation(AnimationState<AnimatedHostileMob> state, AnimationController<AnimatedHostileMob> actionController) {
        float movementAmount = state.getLimbSwingAmount();
        this.movementAnimationActive = movementAmount > (this.movementAnimationActive ? 0.02F : 0.05F);
        String animation = null;
        if (!this.isPerformingAction() && !actionController.isPlayingTriggeredAnimation() && !this.isDeadOrDying()) {
            animation = this.movementAnimationActive ? this.getMovementAnimation() : this.getIdleAnimation();
        }
        if (!Objects.equals(this.lastMovementAnimation, animation)) {
            LOGGER.debug("Mech locomotion entity={} animation={} hasTarget={} movementAmount={}", this.getId(), animation, this.hasCombatTarget(), movementAmount);
            this.lastMovementAnimation = animation;
            if (animation != null) {
                state.resetCurrentAnimation();
            }
        }
        if (animation == null) {
            return PlayState.STOP;
        }
        state.setAnimation(RawAnimation.begin().thenLoop(animation));
        return PlayState.CONTINUE;
    }

    protected void addExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    protected abstract String getMovementAnimation();

    protected String getIdleAnimation() {
        return null;
    }

    protected int getActionTransitionTicks() {
        return 0;
    }

    protected float getCombatTurnSpeed() {
        return 18.0F;
    }

    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return target.isAlive();
    }

    protected abstract Map<String, String> getActionAnimations();

    public abstract List<String> getAvailableActions(net.minecraft.world.entity.LivingEntity target, double distanceSquared);

    public abstract int getActionDuration(String action);

    public abstract int getActionCooldown(String action);

    public abstract double getCombatHoldDistance();

    public abstract double getChaseSpeed();

    protected abstract void onActionStarted(String action, net.minecraft.world.entity.LivingEntity target);

    protected abstract void onActionTick(String action, int actionTick, net.minecraft.world.entity.LivingEntity target);

    public final boolean isPerformingAction() {
        return !this.entityData.get(DATA_ACTION).isEmpty();
    }

    public final String getActiveAction() {
        String action = this.entityData.get(DATA_ACTION);
        return action.isEmpty() ? null : action;
    }

    public final void beginAction(String action, net.minecraft.world.entity.LivingEntity target) {
        this.entityData.set(DATA_ACTION, action);
        this.activeActionTicks = 0;
        this.triggerAnim("action", action);
        this.onActionStarted(action, target);
        LOGGER.debug("Mech action started entity={} action={} target={} durationTicks={} transitionTicks={}", this.getId(), action, target.getId(), this.getActionDuration(action), this.getActionTransitionTicks());
    }

    public final void tickAction(net.minecraft.world.entity.LivingEntity target) {
        String action = this.getActiveAction();
        if (action == null) {
            return;
        }
        this.activeActionTicks++;
        int actionTick = Math.max(0, this.activeActionTicks - this.getActionTransitionTicks());
        if (!this.shouldContinueAction(action, actionTick, target)) {
            LOGGER.debug("Mech action interrupted entity={} action={} actionTick={} target={} targetAlive={} hasLineOfSight={} distanceSquared={}", this.getId(), action, actionTick, target.getId(), target.isAlive(), this.hasLineOfSight(target), this.distanceToSqr(target));
            this.cancelAction();
            return;
        }
        if (actionTick > 0) {
            this.onActionTick(action, actionTick, target);
        }
        if (actionTick >= this.getActionDuration(action)) {
            this.endAction(false);
        }
    }

    public final void cancelAction() {
        this.endAction(true);
    }

    private void endAction(boolean interrupted) {
        String action = this.getActiveAction();
        if (action == null) {
            return;
        }
        LOGGER.debug("Mech action ended entity={} action={} elapsedTicks={} interrupted={}", this.getId(), action, this.activeActionTicks, interrupted);
        this.entityData.set(DATA_ACTION, "");
        this.activeActionTicks = 0;
        if (interrupted) {
            this.stopTriggeredAnim("action", action);
        }
    }

    protected final void damageTarget(net.minecraft.world.entity.LivingEntity target, float damage, double maxDistanceSquared) {
        if (target.isAlive() && this.distanceToSqr(target) <= maxDistanceSquared && this.hasLineOfSight(target)) {
            target.hurt(this.damageSources().mobAttack(this), damage);
        }
    }

    protected final void damageNearby(double radius, float damage, double knockback) {
        double radiusSquared = radius * radius;
        for (net.minecraft.world.entity.LivingEntity victim : this.level().getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            this.getBoundingBox().inflate(radius)
        )) {
            if (victim != this && victim.isAlive() && victim.distanceToSqr(this) <= radiusSquared) {
                if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                    net.minecraft.world.phys.Vec3 push = victim.position().subtract(this.position()).normalize();
                    victim.push(push.x * knockback, 0.18, push.z * knockback);
                }
            }
        }
    }

    protected final void launchProjectile(net.minecraft.world.entity.LivingEntity target, boolean missile, double speed) {
        MechProjectile projectile = new MechProjectile(com.hp.mechafauna.registry.ModEntities.MECH_PROJECTILE.get(), this.level(), this, missile);
        net.minecraft.world.phys.Vec3 start = this.getEyePosition().add(this.getLookAngle().scale(0.8));
        net.minecraft.world.phys.Vec3 aim = target.isAlive()
            ? target.getBoundingBox().getCenter().subtract(start).normalize()
            : this.getLookAngle();
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(aim.x, aim.y, aim.z, (float)speed, 0.0F);
        this.level().addFreshEntity(projectile);
    }

    protected final void emitShotParticles(net.minecraft.world.entity.LivingEntity target) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel && target.isAlive()) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(), 2, 0.12, 0.12, 0.12, 0.02);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public double getTick(Object animatable) {
        return this.tickCount;
    }
}
