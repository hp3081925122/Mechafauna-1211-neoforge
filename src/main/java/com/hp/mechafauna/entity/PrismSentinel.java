package com.hp.mechafauna.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.hp.mechafauna.entity.ai.CombatMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PrismSentinel extends MechanicalMob {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String LASER = "laser";
    private static final String LASER_SWEEP = "laser_sweep";
    private static final int LASER_FIRE_TICK = 13;
    private static final int SWEEP_FIRE_TICK = 13;
    private static final int LASER_FIRE_DURATION = 120;
    private static final int LASER_END_TICK = LASER_FIRE_TICK + LASER_FIRE_DURATION;
    private static final int SWEEP_END_TICK = SWEEP_FIRE_TICK + LASER_FIRE_DURATION;
    private static final int SHARED_ATTACK_COOLDOWN = LASER_END_TICK + 8 + 60;
    private static final int SWEEP_ROTATION_COUNT = 2;
    private static final double LASER_RANGE = 24.0;
    private static final String FIRE_ANIMATION = "animation.prism_sentinel.fire";
    private static final String SWEEP_ANIMATION = "animation.prism_sentinel.laser_sweep";
    private static final EntityDataAccessor<Integer> DATA_LASER_TICK = SynchedEntityData.defineId(PrismSentinel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_LASER_YAW = SynchedEntityData.defineId(PrismSentinel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LASER_PITCH = SynchedEntityData.defineId(PrismSentinel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BODY_PITCH = SynchedEntityData.defineId(PrismSentinel.class, EntityDataSerializers.FLOAT);
    private final Map<Integer, Integer> lastSweepHitCycle = new HashMap<>();
    private int laserTargetId = -1;
    private int sharedAttackCooldown;
    private int supportTicks = -1;
    private UUID supportOwner;
    private float clientYaw;
    private float clientYawOld;
    private float clientPitch;
    private float clientPitchOld;
    private float clientBodyPitch;
    private float clientBodyPitchOld;
    private boolean clientTracking;

    public PrismSentinel(EntityType<? extends PrismSentinel> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new CombatMoveControl.Flying(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(24.0, 0.32, 2.0, 32.0, 0.25).add(Attributes.FLYING_SPEED, 0.38);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LASER_TICK, 0);
        builder.define(DATA_LASER_YAW, 0.0F);
        builder.define(DATA_LASER_PITCH, 0.0F);
        builder.define(DATA_BODY_PITCH, 0.0F);
    }

    public void assignSupport(Player owner, int lifeTicks) {
        this.supportOwner = owner.getUUID();
        this.supportTicks = lifeTicks;
        this.setPersistenceRequired();
        this.setTarget(null);
    }

    public UUID getSupportOwner() {
        return this.supportOwner;
    }

    private void retargetSupport() {
        LivingEntity current = this.getTarget();
        if (current != null && current.isAlive() && this.canAttack(current)) {
            return;
        }
        if (current != null) {
            this.setTarget(null);
        }
        if (this.tickCount % 10 != 0) {
            return;
        }
        double range = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = this.getBoundingBox().inflate(range, 8.0, range);
        LivingEntity nearest = null;
        double nearestDistance = range * range;
        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class, area, candidate -> candidate != this && this.canAttack(candidate))) {
            double distance = this.distanceToSqr(entity);
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            this.setTarget(nearest);
            LOGGER.debug("Support prism target acquired entity={} target={} type={}", this.getId(), nearest.getId(), nearest.getType());
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return this.supportOwner != null && (this.supportOwner.equals(other.getUUID()) || samePlayerSummon(this, other))
            || super.isAlliedTo(other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.supportOwner != null && (target instanceof Player || !(target instanceof net.minecraft.world.entity.monster.Enemy) || this.isAlliedTo(target))) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.supportOwner != null) {
            tag.putUUID("SupportOwner", this.supportOwner);
        }
        if (this.supportTicks > 0) {
            tag.putInt("SupportTicks", this.supportTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.supportOwner = tag.hasUUID("SupportOwner") ? tag.getUUID("SupportOwner") : null;
        this.supportTicks = tag.contains("SupportTicks") ? tag.getInt("SupportTicks") : -1;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && this.supportOwner != null) {
            this.retargetSupport();
        }
        if (!this.level().isClientSide && this.sharedAttackCooldown > 0) {
            this.sharedAttackCooldown--;
        }
        super.tick();
        if (!this.level().isClientSide && this.supportTicks > 0) {
            this.supportTicks--;
            if (this.supportTicks == 0) {
                this.discard();
                return;
            }
        }
        if (this.level().isClientSide) {
            float yaw = this.entityData.get(DATA_LASER_YAW);
            float pitch = this.entityData.get(DATA_LASER_PITCH);
            float bodyPitch = this.entityData.get(DATA_BODY_PITCH);
            if (!this.clientTracking) {
                this.clientYaw = yaw;
                this.clientPitch = pitch;
                this.clientBodyPitch = bodyPitch;
            }
            this.clientYawOld = this.clientYaw;
            this.clientPitchOld = this.clientPitch;
            this.clientBodyPitchOld = this.clientBodyPitch;
            this.clientYaw = Mth.approachDegrees(this.clientYaw, yaw, 12.0F);
            this.clientPitch = Mth.approachDegrees(this.clientPitch, pitch, 12.0F);
            this.clientBodyPitch = Mth.approachDegrees(this.clientBodyPitch, bodyPitch, 12.0F);
            this.clientTracking = this.isPerformingAction();
        } else if (this.isPerformingAction()) {
            float yaw = this.entityData.get(DATA_LASER_YAW);
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
        } else {
            this.entityData.set(DATA_LASER_TICK, 0);
            this.entityData.set(DATA_BODY_PITCH, Mth.approach(this.entityData.get(DATA_BODY_PITCH), 0.0F, 2.0F));
            this.laserTargetId = -1;
            this.lastSweepHitCycle.clear();
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected String getMovementAnimation() {
        return null;
    }

    @Override
    protected float getCombatTurnSpeed() {
        return this.isPerformingAction() ? 1.5F : 6.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        if (this.supportOwner != null && LASER.equals(action)) {
            if (!target.isAlive() || !this.canAttack(target) || this.distanceToSqr(target) > LASER_RANGE * LASER_RANGE) {
                return false;
            }
            this.laserTargetId = target.getId();
            return true;
        }
        return (LASER.equals(action) || LASER_SWEEP.equals(action))
            && target.isAlive()
            && target.getId() == this.laserTargetId
            && !(target instanceof Player player && (player.isCreative() || player.isSpectator()))
            && this.distanceToSqr(target) <= 32.0 * 32.0;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(LASER, FIRE_ANIMATION, LASER_SWEEP, SWEEP_ANIMATION);
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        if (distanceSquared < 5.0 * 5.0 || distanceSquared > LASER_RANGE * LASER_RANGE) {
            return List.of();
        }
        if (this.supportOwner != null) {
            return List.of(LASER);
        }
        if (this.sharedAttackCooldown > 0) {
            return List.of();
        }
        return this.random.nextInt(5) == 0 ? List.of(LASER_SWEEP) : List.of(LASER);
    }

    @Override
    public int getActionDuration(String action) {
        if (this.supportOwner != null && LASER.equals(action)) {
            return Integer.MAX_VALUE;
        }
        return LASER_END_TICK + 8;
    }

    @Override
    public int getActionCooldown(String action) {
        return this.supportOwner != null ? 0 : SHARED_ATTACK_COOLDOWN;
    }

    @Override
    public double getCombatHoldDistance() {
        return 11.0;
    }

    @Override
    public double getCombatRetreatDistance() {
        return 5.0;
    }

    @Override
    public double getChaseSpeed() {
        return 1.0;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        if (LASER.equals(action) || LASER_SWEEP.equals(action)) {
            this.laserTargetId = target.getId();
            if (this.supportOwner == null) {
                this.sharedAttackCooldown = SHARED_ATTACK_COOLDOWN;
            }
            this.lastSweepHitCycle.clear();
            this.entityData.set(DATA_LASER_TICK, 0);
            this.entityData.set(DATA_LASER_YAW, this.yBodyRot);
            this.entityData.set(DATA_LASER_PITCH, 0.0F);
            this.entityData.set(DATA_BODY_PITCH, 0.0F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 0.75F, 1.65F);
        }
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (!LASER.equals(action) && !LASER_SWEEP.equals(action)) {
            return;
        }
        this.entityData.set(DATA_LASER_TICK, this.supportOwner != null && LASER.equals(action) && actionTick >= LASER_FIRE_TICK
            ? LASER_FIRE_TICK + (actionTick - LASER_FIRE_TICK) % LASER_FIRE_DURATION
            : actionTick);
        Vec3 aimOrigin = this.position().add(0, 12.15 / 16.0 * this.getModelScale(), 0);
        Vec3 offset = target.getBoundingBox().getCenter().subtract(aimOrigin);
        float wantedYaw = (float)Math.toDegrees(Math.atan2(-offset.x, offset.z));
        float wantedPitch = (float)-Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance()));
        float turnSpeed = this.supportOwner != null ? 12.0F : 1.5F;
        this.entityData.set(DATA_LASER_YAW, Mth.approachDegrees(this.entityData.get(DATA_LASER_YAW), wantedYaw, turnSpeed));
        this.entityData.set(DATA_LASER_PITCH, Mth.approachDegrees(this.entityData.get(DATA_LASER_PITCH), wantedPitch, turnSpeed));
        float wantedBodyPitch = Mth.clamp(wantedPitch, 0.0F, 42.0F);
        this.entityData.set(DATA_BODY_PITCH, Mth.approach(this.entityData.get(DATA_BODY_PITCH), wantedBodyPitch, turnSpeed));
        if (!this.isLaserFiring()) {
            return;
        }
        if (LASER.equals(action) && (actionTick - LASER_FIRE_TICK) % 10 == 0) {
            this.damageLaserVictims(this.getLaserRay(0.0F).victims(), 2.0F);
        } else if (LASER_SWEEP.equals(action)) {
            this.damageSweepArc(actionTick);
        }
    }

    private void damageSweepArc(int actionTick) {
        float previousYaw = this.sweepYawAtTick(actionTick - 1.0F);
        float currentYaw = this.sweepYawAtTick(actionTick);
        float yawDelta = Mth.wrapDegrees(currentYaw - previousYaw);
        int steps = Math.max(1, Mth.ceil(Math.abs(yawDelta) / 2.0F));
        int cycle = Math.min(SWEEP_ROTATION_COUNT - 1,
            (int)(this.sweepProgressAtTick(actionTick) * SWEEP_ROTATION_COUNT));
        Set<Integer> hitThisTick = new HashSet<>();
        for (int step = 0; step <= steps; step++) {
            float yaw = previousYaw + yawDelta * step / steps;
            LaserRay ray = this.traceLaser(0.0F, yaw, this.getLaserPitch(0.0F));
            for (LivingEntity victim : ray.victims()) {
                if (hitThisTick.add(victim.getId()) && this.lastSweepHitCycle.getOrDefault(victim.getId(), -1) != cycle
                    && victim.hurt(this.damageSources().mobAttack(this), 8.0F)) {
                    victim.igniteForSeconds(4.0F);
                    this.lastSweepHitCycle.put(victim.getId(), cycle);
                }
            }
        }
    }

    private float sweepYawAtTick(float tick) {
        return Mth.wrapDegrees(this.getLaserModelYaw(0.0F)
            - this.sweepProgressAtTick(tick) * 360.0F * SWEEP_ROTATION_COUNT);
    }

    private float sweepProgressAtTick(float tick) {
        return Mth.clamp((tick - SWEEP_FIRE_TICK) / (LASER_FIRE_DURATION - 1.0F), 0.0F, 1.0F);
    }

    private void damageLaserVictims(List<LivingEntity> victims, float damage) {
        for (LivingEntity victim : victims) {
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.igniteForSeconds(4.0F);
            }
        }
    }

    public boolean isLaserFiring() {
        int tick = this.entityData.get(DATA_LASER_TICK);
        String action = this.getActiveAction();
        return this.isAlive() && ((LASER.equals(action) && tick >= LASER_FIRE_TICK && tick < LASER_END_TICK)
            || (LASER_SWEEP.equals(action) && tick >= SWEEP_FIRE_TICK && tick < SWEEP_END_TICK));
    }

    public float getLaserChargePower(float partialTick) {
        String action = this.getActiveAction();
        if (!this.isAlive() || !this.isPerformingAction() || (!LASER.equals(action) && !LASER_SWEEP.equals(action))) {
            return 0.0F;
        }
        int fireTick = LASER_SWEEP.equals(action) ? SWEEP_FIRE_TICK : LASER_FIRE_TICK;
        float tick = this.entityData.get(DATA_LASER_TICK) + partialTick;
        return tick < fireTick ? Mth.clamp((tick + 1.0F) / fireTick, 0.0F, 1.0F) : 0.0F;
    }

    public float getLaserModelYaw(float partialTick) {
        return this.level().isClientSide && this.clientTracking
            ? Mth.rotLerp(partialTick, this.clientYawOld, this.clientYaw) : this.entityData.get(DATA_LASER_YAW);
    }

    public float getLaserYaw(float partialTick) {
        float baseYaw = this.getLaserModelYaw(partialTick);
        if (!LASER_SWEEP.equals(this.getActiveAction())) {
            return baseYaw;
        }
        float tick = this.entityData.get(DATA_LASER_TICK) + partialTick;
        return Mth.wrapDegrees(baseYaw - this.sweepProgressAtTick(tick) * 360.0F * SWEEP_ROTATION_COUNT);
    }

    public float getLaserPitch(float partialTick) {
        return this.level().isClientSide && this.clientTracking
            ? Mth.lerp(partialTick, this.clientPitchOld, this.clientPitch) : this.entityData.get(DATA_LASER_PITCH);
    }

    public float getBodyTiltRadians(float partialTick) {
        float pitch = this.level().isClientSide && this.clientTracking
            ? Mth.lerp(partialTick, this.clientBodyPitchOld, this.clientBodyPitch) : this.entityData.get(DATA_BODY_PITCH);
        return -pitch * Mth.DEG_TO_RAD;
    }

    public float getLaserPower(float partialTick) {
        float tick = this.entityData.get(DATA_LASER_TICK) + partialTick;
        int fireTick = LASER_SWEEP.equals(this.getActiveAction()) ? SWEEP_FIRE_TICK : LASER_FIRE_TICK;
        int endTick = fireTick + LASER_FIRE_DURATION;
        return this.isLaserFiring() ? Math.min(Mth.clamp((tick - fireTick + 1) / 3.0F, 0, 1),
            Mth.clamp((endTick - tick) / 5.0F, 0, 1)) : 0.0F;
    }

    public WeaponMount.Aim getLaserAim(float partialTick) {
        return this.getLaserAim(partialTick, this.getLaserYaw(partialTick));
    }

    private WeaponMount.Aim getLaserAim(float partialTick, float beamYaw) {
        WeaponPose pose = WeaponPose.forModel("prism_sentinel");
        double seconds = (this.entityData.get(DATA_LASER_TICK) + partialTick) / 20.0;
        String animation = LASER_SWEEP.equals(this.getActiveAction()) ? SWEEP_ANIMATION : FIRE_ANIMATION;
        var parent = pose.parentTransform(WeaponMount.PRISM_LENS,
            WeaponMount.modelToWorld(this.getPosition(partialTick), this.getLaserModelYaw(partialTick), this.getModelScale()),
            animation, seconds, this.getBodyTiltRadians(partialTick));
        Vec3 pivot = WeaponMount.transform(parent, WeaponMount.PRISM_LENS.yawPivot());
        Vec3 direction = Vec3.directionFromRotation(this.getLaserPitch(partialTick), beamYaw);
        return WeaponMount.PRISM_LENS.aim(parent, pivot.add(direction.scale(LASER_RANGE)));
    }

    public LaserRay getLaserRay(float partialTick) {
        return this.traceLaser(partialTick, this.getLaserYaw(partialTick), this.getLaserPitch(partialTick));
    }

    public Vec3 getLaserMuzzle(float partialTick) {
        return this.getLaserMuzzle(partialTick, this.getLaserAim(partialTick));
    }

    private Vec3 getLaserMuzzle(float partialTick, WeaponMount.Aim aim) {
        WeaponPose pose = WeaponPose.forModel("prism_sentinel");
        String animation = LASER_SWEEP.equals(this.getActiveAction()) ? SWEEP_ANIMATION : FIRE_ANIMATION;
        double seconds = (this.entityData.get(DATA_LASER_TICK) + partialTick) / 20.0;
        return aim.muzzle(pose.muzzle(WeaponMount.PRISM_LENS, WeaponMount.PRISM_LENS.muzzle(), animation, seconds));
    }

    private LaserRay traceLaser(float partialTick, float yaw, float pitch) {
        WeaponMount.Aim aim = this.getLaserAim(partialTick, yaw);
        Vec3 origin = this.getLaserMuzzle(partialTick, aim);
        Vec3 direction = Vec3.directionFromRotation(pitch, yaw);
        var blockHit = this.level().clip(new ClipContext(origin, origin.add(direction.scale(LASER_RANGE)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getLocation();
        ArrayList<BeamEntityHit> hits = new ArrayList<>();
        for (LivingEntity candidate : this.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(origin, end).inflate(0.12), entity -> entity != this && entity.isAlive() && this.canLaserHit(entity))) {
            AABB box = candidate.getBoundingBox().inflate(0.09);
            Vec3 hit = box.contains(origin) ? origin : box.clip(origin, end).orElse(null);
            if (hit != null) {
                hits.add(new BeamEntityHit(candidate, hit, origin.distanceToSqr(hit)));
            }
        }
        hits.sort(Comparator.comparingDouble(BeamEntityHit::distanceSquared));
        return new LaserRay(origin, direction, end, hits.stream().map(BeamEntityHit::entity).toList(),
            hits.stream().map(BeamEntityHit::location).toList(), blockHit.getType() != HitResult.Type.MISS);
    }

    private boolean canLaserHit(LivingEntity entity) {
        if (this.supportOwner != null) {
            return this.canAttack(entity);
        }
        if (entity instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return false;
        }
        return !this.isAlliedTo(entity) && (!(entity instanceof MechanicalMob) || playerSummonOwner(entity) != null);
    }

    private record BeamEntityHit(LivingEntity entity, Vec3 location, double distanceSquared) {
    }

    public record LaserRay(Vec3 origin, Vec3 direction, Vec3 end, List<LivingEntity> victims,
                           List<Vec3> hitPoints, boolean blockHit) {
        public boolean hit() {
            return !this.hitPoints.isEmpty() || this.blockHit;
        }
    }
}
