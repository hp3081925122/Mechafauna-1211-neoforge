package com.hp.mechafauna.entity;

import com.hp.mechafauna.entity.ai.CombatGoal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.hp.mechafauna.entity.ai.CombatMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class MilitaryDrone extends MechanicalMob implements OwnableEntity {
    private static final String GUN = "gun";
    private static final String HOVER = "animation.mechanical_drone.hover";
    private static final String ROTORS = "animation.mechanical_drone.rotors";
    private static final double GUARD_RANGE = 24.0;
    private static final double RECALL_RANGE = 32.0;
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(MilitaryDrone.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final Vec3[] BARREL_OFFSETS = {
        new Vec3(-0.624, 1.08, 0.0),
        new Vec3(0.624, 1.08, 0.0),
        new Vec3(-1.248, 0.0, 0.0),
        new Vec3(1.248, 0.0, 0.0),
        new Vec3(-0.624, -1.08, 0.0),
        new Vec3(0.624, -1.08, 0.0)
    };

    private int supportTicks = -1;

    public MilitaryDrone(EntityType<? extends MilitaryDrone> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new CombatMoveControl.Flying(this, 20, true);
        this.setNoGravity(true);
    }

    public void limitLife(int ticks) {
        this.supportTicks = ticks;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(22.0, 0.54, 1.0, 32.0, 0.20).add(Attributes.FLYING_SPEED, 0.72);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }

    public void setOwner(Player owner) {
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        this.setPersistenceRequired();
        this.setTarget(null);
    }

    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public boolean isOwnedBy(Entity entity) {
        UUID owner = this.getOwnerUUID();
        return owner != null && owner.equals(entity.getUUID());
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (this.isOwnedBy(other) || samePlayerSummon(this, other)) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = this.getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (this.supportTicks > 0) {
            tag.putInt("SupportTicks", this.supportTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("Owner")));
            this.setPersistenceRequired();
        }
        this.supportTicks = tag.contains("SupportTicks") ? tag.getInt("SupportTicks") : -1;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal(this));
        this.goalSelector.addGoal(2, new EscortGoal());
        this.targetSelector.addGoal(1, new GuardTargetGoal());
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected String getMovementAnimation() {
        return HOVER;
    }

    @Override
    protected String getIdleAnimation() {
        return HOVER;
    }

    @Override
    protected float getCombatTurnSpeed() {
        return 28.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return GUN.equals(action) && target.isAlive() && this.hasLineOfSight(target) && this.distanceToSqr(target) <= 18.0 * 18.0;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(GUN, "animation.mechanical_drone.fire");
    }

    @Override
    protected void addExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotors", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop(ROTORS));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared <= 17.0 * 17.0 ? List.of(GUN) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 33;
    }

    @Override
    public int getActionCooldown(String action) {
        return 46;
    }

    @Override
    public double getCombatHoldDistance() {
        return 9.5;
    }

    @Override
    public double getChaseSpeed() {
        return 1.0;
    }

    @Override
    public boolean canMoveDuringAction(String action) {
        return true;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.65F, 1.45F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (GUN.equals(action) && actionTick >= 5 && actionTick <= 29 && actionTick % 3 == 2) {
            this.fireVolley(target);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.supportTicks > 0) {
            this.supportTicks--;
            if (this.supportTicks == 0) {
                this.discard();
                return;
            }
        }
        if (this.level().isClientSide || !(this.getOwner() instanceof Player owner) || !owner.isAlive()) {
            return;
        }
        if (this.distanceToSqr(owner) > RECALL_RANGE * RECALL_RANGE) {
            this.teleportTo(owner.getX(), owner.getY() + 1.2, owner.getZ());
            this.setDeltaMovement(Vec3.ZERO);
            this.getNavigation().stop();
            return;
        }
        if (this.getTarget() == null && this.distanceToSqr(owner) <= 3.5 * 3.5) {
            this.setYRot(owner.getYRot());
            this.yBodyRot = this.yHeadRot = owner.getYRot();
        }
    }

    private void fireVolley(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 forward = targetCenter.subtract(this.getBoundingBox().getCenter());
        if (forward.lengthSqr() < 1.0E-6) {
            forward = this.getLookAngle();
        }
        forward = forward.normalize();
        Vec3 reference = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        double forwardAngle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 0.35 + this.random.nextDouble() * 0.45;
        Vec3 muzzle = WeaponMount.MILITARY_DRONE_GUN.muzzle();
        for (int index = 0; index < BARREL_OFFSETS.length; index++) {
            double angle = forwardAngle + index * (Math.PI * 2.0 / BARREL_OFFSETS.length);
            Vec3 aimPoint = targetCenter.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            this.launchCyanLaser(target, WeaponMount.MILITARY_DRONE_GUN, muzzle.add(BARREL_OFFSETS[index]), aimPoint, 2.8, 0.5F, true);
        }
    }

    private boolean isMonsterTarget(LivingEntity entity) {
        return entity != null
            && entity.isAlive()
            && entity != this
            && !(entity instanceof MilitaryDrone)
            && !(entity instanceof Player)
            && entity instanceof Enemy
            && !this.isAlliedTo(entity);
    }

    private final class EscortGoal extends Goal {
        private EscortGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.canContinueToUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (MilitaryDrone.this.getTarget() != null || !(MilitaryDrone.this.getOwner() instanceof Player owner) || !owner.isAlive()) {
                return false;
            }
            return MilitaryDrone.this.distanceToSqr(owner) > 2.8 * 2.8;
        }

        @Override
        public void tick() {
            if (!(MilitaryDrone.this.getOwner() instanceof Player owner)) {
                return;
            }
            float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
            double x = owner.getX() + Mth.sin(yaw) * 2.0;
            double z = owner.getZ() - Mth.cos(yaw) * 2.0;
            MilitaryDrone.this.getNavigation().stop();
            MilitaryDrone.this.getMoveControl().setWantedPosition(x, owner.getY() + 2.2, z, 1.2);
            MilitaryDrone.this.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        }
    }

    private final class GuardTargetGoal extends Goal {
        private int rescanTicks;

        private GuardTargetGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            return MilitaryDrone.this.getOwner() instanceof Player;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            if (!(MilitaryDrone.this.getOwner() instanceof Player owner)) {
                return;
            }
            LivingEntity current = MilitaryDrone.this.getTarget();
            boolean currentValid = MilitaryDrone.this.isMonsterTarget(current) && owner.distanceToSqr(current) <= GUARD_RANGE * GUARD_RANGE;
            if (currentValid && --this.rescanTicks > 0) {
                return;
            }
            this.rescanTicks = 10;
            LivingEntity next = this.pick(owner);
            if (current != next) {
                MilitaryDrone.this.setTarget(next);
            }
        }

        private LivingEntity pick(Player owner) {
            AABB area = owner.getBoundingBox().inflate(GUARD_RANGE);
            LivingEntity attacker = null;
            double attackerDistance = GUARD_RANGE * GUARD_RANGE;
            LivingEntity lastAttacker = owner.getLastHurtByMob();
            if (MilitaryDrone.this.isMonsterTarget(lastAttacker) && owner.distanceToSqr(lastAttacker) <= attackerDistance) {
                attacker = lastAttacker;
                attackerDistance = owner.distanceToSqr(lastAttacker);
            }
            for (LivingEntity entity : MilitaryDrone.this.level().getEntitiesOfClass(LivingEntity.class, area, MilitaryDrone.this::isMonsterTarget)) {
                if (entity instanceof Mob mob && mob.getTarget() == owner) {
                    double distance = owner.distanceToSqr(entity);
                    if (distance < attackerDistance) {
                        attacker = entity;
                        attackerDistance = distance;
                    }
                }
            }
            if (attacker != null) {
                return attacker;
            }
            LivingEntity attackedByOwner = owner.getLastHurtMob();
            if (MilitaryDrone.this.isMonsterTarget(attackedByOwner) && owner.distanceToSqr(attackedByOwner) <= GUARD_RANGE * GUARD_RANGE) {
                return attackedByOwner;
            }
            LivingEntity nearest = null;
            double nearestDistance = GUARD_RANGE * GUARD_RANGE;
            for (LivingEntity entity : MilitaryDrone.this.level().getEntitiesOfClass(LivingEntity.class, area, MilitaryDrone.this::isMonsterTarget)) {
                double distance = owner.distanceToSqr(entity);
                if (distance < nearestDistance) {
                    nearest = entity;
                    nearestDistance = distance;
                }
            }
            return nearest;
        }
    }
}
