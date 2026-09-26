package com.hp.mechafauna.entity;

import com.hp.mechafauna.entity.ai.CombatGoal;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MilitaryDrone extends MechanicalDrone {
    private static final double GUARD_RANGE = 24.0;
    private static final double RECALL_RANGE = 32.0;
    private static final Vec3[] BARREL_OFFSETS = {
        new Vec3(-0.624, 1.08, 0.0),
        new Vec3(0.624, 1.08, 0.0),
        new Vec3(-1.248, 0.0, 0.0),
        new Vec3(1.248, 0.0, 0.0),
        new Vec3(-0.624, -1.08, 0.0),
        new Vec3(0.624, -1.08, 0.0)
    };

    public MilitaryDrone(EntityType<? extends MilitaryDrone> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    public void setOwner(Player owner) {
        super.setOwner(owner);
        this.setNoAi(false);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.isOwnedDrone()) {
            this.setNoAi(false);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CombatGoal(this));
        this.goalSelector.addGoal(2, new EscortGoal());
        this.targetSelector.addGoal(1, new GuardTargetGoal());
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean canMoveDuringAction(String action) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
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

    @Override
    protected void fireGun(LivingEntity target) {
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
        Vec3 muzzle = this.gunMount().muzzle();
        for (int index = 0; index < BARREL_OFFSETS.length; index++) {
            double angle = forwardAngle + index * (Math.PI * 2.0 / BARREL_OFFSETS.length);
            Vec3 aimPoint = targetCenter.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            this.launchCyanLaser(target, this.gunMount(), muzzle.add(BARREL_OFFSETS[index]), aimPoint, 2.8, 1.0F, true);
        }
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || !entity.isAlive() || entity == this || this.isAlliedTo(entity)) {
            return false;
        }
        return !(entity instanceof Player player) || (!player.isSpectator() && !player.isCreative());
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
            LivingEntity next = this.pick(owner);
            if (MilitaryDrone.this.getTarget() != next) {
                MilitaryDrone.this.setTarget(next);
            }
        }

        private LivingEntity pick(Player owner) {
            AABB area = owner.getBoundingBox().inflate(GUARD_RANGE);
            LivingEntity attacker = null;
            double attackerDistance = GUARD_RANGE * GUARD_RANGE;
            LivingEntity lastAttacker = owner.getLastHurtByMob();
            if (MilitaryDrone.this.isValidTarget(lastAttacker) && owner.distanceToSqr(lastAttacker) <= attackerDistance) {
                attacker = lastAttacker;
                attackerDistance = owner.distanceToSqr(lastAttacker);
            }
            for (LivingEntity entity : MilitaryDrone.this.level().getEntitiesOfClass(LivingEntity.class, area, MilitaryDrone.this::isValidTarget)) {
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
            if (MilitaryDrone.this.isValidTarget(attackedByOwner) && attackedByOwner instanceof Enemy
                && owner.distanceToSqr(attackedByOwner) <= GUARD_RANGE * GUARD_RANGE) {
                return attackedByOwner;
            }
            LivingEntity nearest = null;
            double nearestDistance = GUARD_RANGE * GUARD_RANGE;
            for (LivingEntity entity : MilitaryDrone.this.level().getEntitiesOfClass(LivingEntity.class, area,
                candidate -> MilitaryDrone.this.isValidTarget(candidate) && candidate instanceof Enemy)) {
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
