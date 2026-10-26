package com.hp.mechafauna.entity;

import com.hp.mechafauna.entity.ai.CombatGoal;
import com.hp.mechafauna.entity.ai.CombatMoveControl;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

public abstract class AnimatedHostileMob extends Monster implements GeoEntity {
    public static boolean isIgnoredByOtherMechs(Entity entity) {
        return entity instanceof AnimatedHostileMob && !(entity instanceof MilitaryDrone);
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<String> DATA_ACTION = SynchedEntityData.defineId(AnimatedHostileMob.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(AnimatedHostileMob.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int activeActionTicks;
    private int lastAimTargetId = -1;
    private int lastCombatAimTick = -1;
    private float combatBodyYaw;
    private float combatHeadYaw;
    private int lastShotLogTick = -100;
    private boolean movementAnimationActive;
    private String lastMovementAnimation;
    private int interruptedActionCooldown = -1;

    protected AnimatedHostileMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new CombatMoveControl(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        this.updateCombatAim();
    }

    public final void updateCombatAim() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.entityData.set(DATA_TARGET_ID, -1);
            this.lastCombatAimTick = -1;
            if (this.lastAimTargetId != -1) {
                LOGGER.debug("Mech aim target lost entity={} target={}", this.getId(), this.lastAimTargetId);
                this.lastAimTargetId = -1;
            }
            return;
        }
        this.entityData.set(DATA_TARGET_ID, target.getId());
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float targetYaw = dx * dx + dz * dz > 1.0E-6
            ? (float)(Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F
            : this.yBodyRot;
        if (this.lastAimTargetId != target.getId()) {
            LOGGER.debug("Mech aim target acquired entity={} target={} bodyYaw={} targetYaw={}", this.getId(), target.getId(), this.yBodyRot, targetYaw);
            this.lastAimTargetId = target.getId();
        }
        if (this.lastCombatAimTick != this.tickCount) {
            float previousYaw = this.lastCombatAimTick == this.tickCount - 1 ? this.combatBodyYaw : this.yBodyRot;
            this.combatBodyYaw = Mth.approachDegrees(previousYaw, targetYaw, this.getCombatTurnSpeed());
            this.combatHeadYaw = targetYaw;
            this.lastCombatAimTick = this.tickCount;
        }
        this.setYRot(this.combatBodyYaw);
        this.setYBodyRot(this.combatBodyYaw);
        this.setYHeadRot(this.combatHeadYaw);
    }

    @Override
    protected void tickHeadTurn(float bodyYaw) {
        if (this.hasCombatTarget()) {
            this.yBodyRot = this.getYRot();
            return;
        }
        super.tickHeadTurn(bodyYaw);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, "");
        builder.define(DATA_TARGET_ID, -1);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        if (!this.level().isClientSide()) {
            LivingEntity actualTarget = this.getTarget();
            this.entityData.set(DATA_TARGET_ID, actualTarget != null && actualTarget.isAlive() ? actualTarget.getId() : -1);
        }
    }

    public final boolean hasCombatTarget() {
        return this.entityData.get(DATA_TARGET_ID) != -1;
    }

    public final LivingEntity getCombatTarget() {
        if (this.level().getEntity(this.entityData.get(DATA_TARGET_ID)) instanceof LivingEntity target && target.isAlive()) {
            return target;
        }
        return null;
    }

    public double getModelScale() {
        return this.getScale();
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
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Mob>(this, Mob.class, 10, true, false,
            (target, serverLevel) -> target instanceof Enemy && !(target instanceof AnimatedHostileMob)));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !isIgnoredByOtherMechs(this.mob.getLastHurtByMob()) && super.canUse();
            }

            @Override
            public void start() {
                super.start();
                LivingEntity attacker = this.mob.getTarget();
                if (attacker != null) {
                    LOGGER.debug("Mech retaliation target acquired entity={} attacker={} attackerType={}", this.mob.getId(), attacker.getId(), attacker.getType());
                }
            }
        });
    }

    @Override
    public final void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AnimatedHostileMob> action = new AnimationController<>("action", this.getActionTransitionTicks(), state -> PlayState.STOP);
        AnimationController<AnimatedHostileMob> movement = new AnimationController<>("movement", 3, state -> this.updateMovementAnimation(state, action));
        for (Map.Entry<String, String> entry : this.getActionAnimations().entrySet()) {
            action.triggerableAnim(entry.getKey(), RawAnimation.begin().thenPlay(entry.getValue()));
        }
        controllers.add(movement, action);
        this.addExtraControllers(controllers);
    }

    private PlayState updateMovementAnimation(AnimationTest<AnimatedHostileMob> state, AnimationController<AnimatedHostileMob> actionController) {
        float movementAmount = state.isMoving() ? 1.0F : 0.0F;
        this.movementAnimationActive = state.isMoving();
        String animation = null;
        if (!this.isPerformingAction() && !actionController.isPlayingTriggeredAnimation() && !this.isDeadOrDying()) {
            animation = this.movementAnimationActive ? this.getMovementAnimation() : this.getIdleAnimation();
        }
        if (!Objects.equals(this.lastMovementAnimation, animation)) {
            LOGGER.debug("Mech locomotion entity={} animation={} hasTarget={} movementAmount={}", this.getId(), animation, this.hasCombatTarget(), movementAmount);
            this.lastMovementAnimation = animation;
            if (animation != null) {
            }
        }
        if (animation == null) {
            return PlayState.STOP;
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(animation));
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

    public final String getActionAnimation(String action) {
        return this.getActionAnimations().get(action);
    }

    protected int getInterruptedActionCooldown(String action, int actionTick) {
        return -1;
    }

    public final int consumeInterruptedActionCooldown() {
        int cooldown = this.interruptedActionCooldown;
        this.interruptedActionCooldown = -1;
        return cooldown;
    }

    public abstract List<String> getAvailableActions(net.minecraft.world.entity.LivingEntity target, double distanceSquared);

    public abstract int getActionDuration(String action);

    public abstract int getActionCooldown(String action);

    public abstract double getCombatHoldDistance();

    public double getCombatRetreatDistance() {
        return 0.0;
    }

    public abstract double getChaseSpeed();

    public boolean canMoveDuringAction(String action) {
        return false;
    }

    protected abstract void onActionStarted(String action, net.minecraft.world.entity.LivingEntity target);

    protected abstract void onActionTick(String action, int actionTick, net.minecraft.world.entity.LivingEntity target);

    public final boolean isPerformingAction() {
        return !this.entityData.get(DATA_ACTION).isEmpty();
    }

    public final String getActiveAction() {
        String action = this.entityData.get(DATA_ACTION);
        return action.isEmpty() ? null : action;
    }

    public final double getActionAnimationSeconds(float partialTick) {
        return Math.max(0, this.activeActionTicks - this.getActionTransitionTicks() + partialTick) / 20.0;
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
            this.interruptedActionCooldown = this.getInterruptedActionCooldown(action, actionTick);
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

    protected final boolean damageTarget(net.minecraft.world.entity.LivingEntity target, float damage, double maxDistanceSquared) {
        if (target.isAlive() && this.distanceToSqr(target) <= maxDistanceSquared && this.hasLineOfSight(target)) {
            return target.hurtOrSimulate(this.damageSources().mobAttack(this), damage);
        }
        return false;
    }

    protected final void damageNearby(double radius, float damage, double knockback) {
        double radiusSquared = radius * radius;
        for (net.minecraft.world.entity.LivingEntity victim : this.level().getEntitiesOfClass(
            net.minecraft.world.entity.LivingEntity.class,
            this.getBoundingBox().inflate(radius)
        )) {
            if (victim != this && victim.isAlive() && victim.distanceToSqr(this) <= radiusSquared) {
                if (victim.hurtOrSimulate(this.damageSources().mobAttack(this), damage)) {
                    net.minecraft.world.phys.Vec3 push = victim.position().subtract(this.position()).normalize();
                    victim.push(push.x * knockback, 0.18, push.z * knockback);
                }
            }
        }
    }

    protected final void launchProjectile(LivingEntity target, WeaponMount mount, Vec3 muzzle, boolean missile, boolean laser, double speed, float impactDamage) {
        if (target == null) {
            return;
        }
        this.launchProjectileInternal(target, mount, muzzle, target.getBoundingBox().getCenter(), missile, laser, false, speed, impactDamage, false);
    }

    protected final void launchProjectile(LivingEntity target, WeaponMount mount, Vec3 muzzle, Vec3 aimPoint, boolean missile, boolean laser, double speed, float impactDamage) {
        this.launchProjectileInternal(target, mount, muzzle, aimPoint, missile, laser, false, speed, impactDamage, false);
    }

    protected final void launchCyanLaser(LivingEntity target, WeaponMount mount, Vec3 muzzle, double speed, float impactDamage) {
        if (target == null) {
            return;
        }
        this.launchCyanLaser(target, mount, muzzle, target.getBoundingBox().getCenter(), speed, impactDamage);
    }

    protected final void launchCyanLaser(LivingEntity target, WeaponMount mount, Vec3 muzzle, Vec3 aimPoint, double speed, float impactDamage) {
        this.launchCyanLaser(target, mount, muzzle, aimPoint, speed, impactDamage, false);
    }

    protected final void launchCyanLaser(LivingEntity target, WeaponMount mount, Vec3 muzzle, double speed, float impactDamage, boolean bypassHurtCooldown) {
        if (target == null) {
            return;
        }
        this.launchCyanLaser(target, mount, muzzle, target.getBoundingBox().getCenter(), speed, impactDamage, bypassHurtCooldown);
    }

    protected final void launchCyanLaser(LivingEntity target, WeaponMount mount, Vec3 muzzle, Vec3 aimPoint, double speed, float impactDamage, boolean bypassHurtCooldown) {
        this.launchProjectileInternal(target, mount, muzzle, aimPoint, false, true, false, speed, impactDamage, bypassHurtCooldown);
    }

    protected final void launchPrismLaserFromPosition(LivingEntity target, Vec3 muzzle, double speed, float impactDamage) {
        if (target == null) {
            return;
        }
        this.launchProjectileInternal(target, null, muzzle, target.getBoundingBox().getCenter(), false, true, true, speed, impactDamage, false);
    }

    private void launchProjectileInternal(LivingEntity target, WeaponMount mount, Vec3 muzzle, Vec3 aimPoint, boolean missile, boolean laser, boolean prismLaser, double speed, float impactDamage, boolean bypassHurtCooldown) {
        if (this.level().isClientSide() || target == null || !target.isAlive() || aimPoint == null) {
            return;
        }
        this.updateCombatAim();
        Vec3 start = muzzle;
        float weaponYaw = 0.0F;
        float weaponPitch = 0.0F;
        if (mount != null) {
            WeaponPose pose = WeaponPose.forModel(mount.modelName());
            String action = this.getActiveAction();
            String animation = action == null ? null : this.getActionAnimations().get(action);
            double seconds = Math.max(0, this.activeActionTicks - this.getActionTransitionTicks()) / 20.0;
            WeaponMount.Aim weaponAim = mount.aim(pose.parentTransform(mount,
                WeaponMount.modelToWorld(this.position(), this.yBodyRot, this.getModelScale()), animation, seconds),
                target.getBoundingBox().getCenter());
            start = weaponAim.muzzle(pose.muzzle(mount, muzzle, animation, seconds));
            weaponYaw = weaponAim.yaw() * Mth.RAD_TO_DEG;
            weaponPitch = weaponAim.pitch() * Mth.RAD_TO_DEG;
        }
        Vec3 aim = aimPoint.subtract(start).normalize();
        MechProjectile projectile = new MechProjectile(com.hp.mechafauna.registry.ModEntities.MECH_PROJECTILE.get(), this.level(), this, missile, laser, prismLaser, impactDamage);
        projectile.setBypassHurtCooldown(bypassHurtCooldown);
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(aim.x, aim.y, aim.z, (float)speed, 0.0F);
        this.level().addFreshEntity(projectile);
        if (this.tickCount - this.lastShotLogTick >= 20) {
            LOGGER.debug("Mech weapon shot entity={} weapon={} target={} muzzle={} direction={} damage={} bodyYaw={} weaponYaw={} weaponPitch={} projectileYaw={} projectilePitch={}",
                this.getId(), mount == null ? "direct" : mount.yawBone(), target.getId(), start, aim, impactDamage, this.yBodyRot,
                weaponYaw, weaponPitch, projectile.getYRot(), projectile.getXRot());
            this.lastShotLogTick = this.tickCount;
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    public double getTick(Object animatable) {
        return this.tickCount;
    }
}
