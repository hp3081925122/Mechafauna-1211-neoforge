package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class FlameCrawler extends MechanicalMob {
    public static final String FLAMETHROWER = "flamethrower";
    private static final String MOVE_ANIMATION = "animation.flame_crawler.move";
    private static final String FLAME_ANIMATION = "animation.flame_crawler.flamethrower";
    private static final EntityDataAccessor<Integer> DATA_FLAME_ACTION_TICK = SynchedEntityData.defineId(FlameCrawler.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DETONATION_FUSE_TICKS = SynchedEntityData.defineId(FlameCrawler.class, EntityDataSerializers.INT);
    private static final int FLAME_START_TICK = 7;
    private static final int FLAME_END_TICK = 38;
    private static final int FLAME_HIT_INTERVAL_TICKS = 12;
    private static final float FLAME_DAMAGE = 2.0F;
    private static final float FLAME_IGNITE_SECONDS = 4.0F;
    private static final double FLAME_BASE_HIT_RADIUS = 0.12;
    private static final double FLAME_TIP_HIT_RADIUS = 1.92;
    private static final int FLAME_PARTICLES_PER_STATION = 3;
    private static final double FLAME_PARTICLE_SPACING = 0.36;
    private static final int DETONATION_FUSE_TICKS = 20;
    private static final float DETONATION_EXPLOSION_RADIUS = 3.0F;
    private static final double MAX_FLAME_LENGTH = 8.5;
    private static final Logger LOGGER = LogUtils.getLogger();

    public FlameCrawler(EntityType<? extends FlameCrawler> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(46.0, 0.23, 5.0, 32.0, 0.45);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLAME_ACTION_TICK, 0);
        this.entityData.define(DATA_DETONATION_FUSE_TICKS, -1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        int fuseTicks = this.entityData.get(DATA_DETONATION_FUSE_TICKS);
        if (fuseTicks >= 0) {
            tag.putInt("DetonationFuseTicks", fuseTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_DETONATION_FUSE_TICKS, tag.contains("DetonationFuseTicks") ? tag.getInt("DetonationFuseTicks") : -1);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        if (!item.is(Items.FLINT_AND_STEEL)) {
            return super.mobInteract(player, hand);
        }
        if (!this.isDetonationPrimed() && !this.isDeadOrDying()) {
            this.level().playSound(player, this.getX(), this.getY(), this.getZ(), SoundEvents.FLINTANDSTEEL_USE, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level().isClientSide && this.primeForDetonation(player)) {
                item.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.isFlameWindow()) {
                this.emitFlameParticles();
            }
            if (this.isDetonationPrimed() && this.tickCount % 4 == 0) {
                Vec3 smokePosition = this.position().add(0.0, this.getBbHeight() * 0.75, 0.0);
                this.level().addParticle(ParticleTypes.SMOKE, smokePosition.x, smokePosition.y, smokePosition.z, 0.0, 0.035, 0.0);
            }
            return;
        }
        int fuseTicks = this.entityData.get(DATA_DETONATION_FUSE_TICKS);
        if (fuseTicks >= 0) {
            if (fuseTicks <= 1) {
                this.detonate();
            } else {
                this.entityData.set(DATA_DETONATION_FUSE_TICKS, fuseTicks - 1);
            }
        }
    }

    @Override
    protected String getMovementAnimation() {
        return MOVE_ANIMATION;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return !this.isDetonationPrimed()
            && FLAMETHROWER.equals(action)
            && target.isAlive()
            && this.hasLineOfSight(target)
            && this.distanceToSqr(target) <= 10.0 * 10.0;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(FLAMETHROWER, FLAME_ANIMATION);
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return !this.isDetonationPrimed() && distanceSquared <= 9.0 * 9.0 ? List.of(FLAMETHROWER) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 48;
    }

    @Override
    public int getActionCooldown(String action) {
        return 58;
    }

    @Override
    public double getCombatHoldDistance() {
        return this.isDetonationPrimed() ? 0.8 : 5.5;
    }

    @Override
    public double getChaseSpeed() {
        return 0.86;
    }

    public boolean isDetonationPrimed() {
        return this.entityData.get(DATA_DETONATION_FUSE_TICKS) >= 0;
    }

    public boolean primeForDetonation(LivingEntity target) {
        if (this.level().isClientSide || !this.isAlive() || this.isDetonationPrimed()) {
            return false;
        }
        if (target != null && target.isAlive() && target != this) {
            this.setTarget(target);
        }
        this.entityData.set(DATA_DETONATION_FUSE_TICKS, DETONATION_FUSE_TICKS);
        this.cancelAction();
        this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.7F);
        this.gameEvent(GameEvent.PRIME_FUSE);
        return true;
    }

    private void detonate() {
        if (this.level().isClientSide || this.isRemoved()) {
            return;
        }
        this.dead = true;
        ExplosionEffects.explode(this.level(), this, null,
            this.getX(), this.getY() + 0.3, this.getZ(), DETONATION_EXPLOSION_RADIUS, ExplosionEffects.DEFAULT);
        this.discard();
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.entityData.set(DATA_FLAME_ACTION_TICK, 0);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (FLAMETHROWER.equals(action)) {
            this.entityData.set(DATA_FLAME_ACTION_TICK, actionTick);
            if (actionTick >= FLAME_START_TICK && actionTick <= FLAME_END_TICK
                && actionTick % FLAME_HIT_INTERVAL_TICKS == 0) {
                this.damageAlongFlameRay(actionTick, target);
            }
        }
    }

    private void damageAlongFlameRay(int actionTick, LivingEntity target) {
        FlameRay ray = this.getFlameRay(1.0F);
        if (ray == null) {
            boolean targetDamaged = this.damageTarget(target, FLAME_DAMAGE, MAX_FLAME_LENGTH * MAX_FLAME_LENGTH);
            if (targetDamaged) {
                target.setSecondsOnFire((int)Math.ceil(FLAME_IGNITE_SECONDS));
            }
            LOGGER.debug("Flamethrower ray unavailable entity={} actionTick={} target={} targetDamaged={} distanceSquared={} hasLineOfSight={}",
                this.getId(), actionTick, target.getId(), targetDamaged, this.distanceToSqr(target), this.hasLineOfSight(target));
            return;
        }
        Vec3 segment = ray.end().subtract(ray.origin());
        double length = segment.length();
        if (length < 1.0E-4) {
            boolean targetDamaged = this.damageTarget(target, FLAME_DAMAGE, MAX_FLAME_LENGTH * MAX_FLAME_LENGTH);
            if (targetDamaged) {
                target.setSecondsOnFire((int)Math.ceil(FLAME_IGNITE_SECONDS));
            }
            LOGGER.debug("Flamethrower ray blocked entity={} actionTick={} target={} targetDamaged={} origin={}",
                this.getId(), actionTick, target.getId(), targetDamaged, ray.origin());
            return;
        }
        Vec3 direction = segment.scale(1.0 / length);
        AABB searchBounds = new AABB(ray.origin(), ray.end()).inflate(FLAME_TIP_HIT_RADIUS);
        int intersections = 0;
        int damaged = 0;
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, searchBounds,
            entity -> entity != this && entity != target && entity.isAlive() && !this.isAlliedTo(entity))) {
            double along = victim.getBoundingBox().getCenter().subtract(ray.origin()).dot(direction);
            if (along < 0.0 || along > length) {
                continue;
            }
            double progress = along / length;
            double radius = FLAME_BASE_HIT_RADIUS + (FLAME_TIP_HIT_RADIUS - FLAME_BASE_HIT_RADIUS) * progress;
            Vec3 nearestPoint = ray.origin().add(direction.scale(along));
            if (!victim.getBoundingBox().inflate(radius).contains(nearestPoint)) {
                continue;
            }
            intersections++;
            if (victim.hurt(this.damageSources().mobAttack(this), FLAME_DAMAGE)) {
                victim.setSecondsOnFire((int)Math.ceil(FLAME_IGNITE_SECONDS));
                damaged++;
            }
        }
        boolean targetDamaged = this.damageTarget(target, FLAME_DAMAGE, MAX_FLAME_LENGTH * MAX_FLAME_LENGTH);
        if (targetDamaged) {
            target.setSecondsOnFire((int)Math.ceil(FLAME_IGNITE_SECONDS));
        }
        LOGGER.debug("Flamethrower damage scan entity={} actionTick={} target={} length={} intersections={} collateralDamaged={} targetDamaged={} distanceSquared={} hasLineOfSight={}",
            this.getId(), actionTick, target.getId(), length, intersections, damaged, targetDamaged,
            this.distanceToSqr(target), this.hasLineOfSight(target));
    }

    public boolean isFlameWindow() {
        int actionTick = this.entityData.get(DATA_FLAME_ACTION_TICK);
        return FLAMETHROWER.equals(this.getActiveAction()) && actionTick >= FLAME_START_TICK && actionTick <= FLAME_END_TICK;
    }

    public FlameRay getFlameRay(float partialTick) {
        return this.getFlameRay(partialTick, false);
    }

    private FlameRay getFlameRay(float partialTick, boolean pierceBlocks) {
        if (!this.isFlameWindow()) {
            return null;
        }
        LivingEntity target = this.getCombatTarget();
        if (target == null) {
            return null;
        }
        int actionTick = this.entityData.get(DATA_FLAME_ACTION_TICK);
        double seconds = actionTick / 20.0;
        WeaponPose pose = WeaponPose.forModel("flame_crawler");
        float bodyYaw = Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot);
        var modelToWorld = WeaponMount.modelToWorld(this.getPosition(partialTick), bodyYaw, this.getModelScale());
        var parent = pose.parentTransform(WeaponMount.FLAME_CRAWLER, modelToWorld, FLAME_ANIMATION, seconds);
        Vec3 targetPoint = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.5, 0.0);
        WeaponMount.Aim aim = WeaponMount.FLAME_CRAWLER.aim(parent, targetPoint);
        Vec3 origin = aim.muzzle(pose.muzzle(WeaponMount.FLAME_CRAWLER, WeaponMount.FLAME_CRAWLER.muzzle(), FLAME_ANIMATION, seconds));
        Vec3 directionPoint = aim.muzzle(WeaponMount.FLAME_CRAWLER.muzzle().add(0.0, 0.0, -1.0));
        Vec3 direction = directionPoint.subtract(origin).normalize();
        double length = MAX_FLAME_LENGTH;
        Vec3 end = origin.add(direction.scale(length));
        if (!pierceBlocks) {
            end = this.level().clip(new ClipContext(origin, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        }
        return new FlameRay(origin, direction, end);
    }

    private void emitFlameParticles() {
        FlameRay ray = this.getFlameRay(1.0F, true);
        if (ray == null) {
            return;
        }
        Vec3 segment = ray.end().subtract(ray.origin());
        double length = segment.length();
        if (length < 0.1) {
            return;
        }
        Vec3 direction = segment.scale(1.0 / length);
        Vec3 side = new Vec3(direction.z, 0.0, -direction.x);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 up = direction.cross(side).normalize();
        int stations = Math.max(6, (int)Math.ceil(length / FLAME_PARTICLE_SPACING));
        double[] radialLayers = {0.08, 0.55, 0.96};
        for (int i = 0; i < stations; i++) {
            double progress = i / (double)(stations - 1);
            Vec3 center = ray.origin().add(direction.scale(length * progress));
            double spread = Math.pow(progress, 1.12);
            double sideRadius = FLAME_TIP_HIT_RADIUS * spread;
            double upRadius = sideRadius * 0.64;
            for (int layer = 0; layer < FLAME_PARTICLES_PER_STATION; layer++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double radius = radialLayers[layer];
                Vec3 offset = side.scale(Math.cos(angle) * sideRadius * radius)
                    .add(up.scale(Math.sin(angle) * upRadius * radius));
                Vec3 particlePosition = center.add(offset);
                double speed = 0.018 + layer * 0.006;
                Vec3 drift = side.scale(Math.cos(angle) * 0.006).add(up.scale(Math.sin(angle) * 0.006));
                Vec3 velocity = direction.scale(speed).add(drift);
                this.level().addParticle(ParticleTypes.FLAME, true, particlePosition.x, particlePosition.y,
                    particlePosition.z, velocity.x, velocity.y, velocity.z);
            }
        }
        Vec3 position = ray.end().subtract(direction.scale(0.12));
        double dx = (this.random.nextDouble() - 0.5) * 0.05;
        double dz = (this.random.nextDouble() - 0.5) * 0.05;
        if (this.tickCount % 5 == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, position.x, position.y + 0.2, position.z, dx, 0.045, dz);
        }
    }

    public record FlameRay(Vec3 origin, Vec3 direction, Vec3 end) {
    }
}
