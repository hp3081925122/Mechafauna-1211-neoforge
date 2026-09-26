package com.hp.mechafauna.entity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class SaucerLaserStrike extends Entity {
    public static final double BEAM_HEIGHT = 16.0;
    private static final int DURATION = FlyingSaucerBoss.LASER_CHARGE_TICKS + FlyingSaucerBoss.LASER_FIRE_TICKS + FlyingSaucerBoss.WAVE_LIFETIME;
    private static final EntityDataAccessor<Vector3f> DATA_IMPACT = SynchedEntityData.defineId(SaucerLaserStrike.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_TICK = SynchedEntityData.defineId(SaucerLaserStrike.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(SaucerLaserStrike.class, EntityDataSerializers.OPTIONAL_UUID);
    private final Set<Long> waveHits = new HashSet<>();

    public SaucerLaserStrike(EntityType<? extends SaucerLaserStrike> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void placeAt(Player player) {
        this.setOwner(player);
        this.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        this.refreshImpact();
    }

    public void setOwner(Player owner) {
        this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
    }

    public Vec3 getBeamOrigin() {
        return this.position().add(0.0, BEAM_HEIGHT, 0.0);
    }

    private void refreshImpact() {
        Vec3 origin = this.getBeamOrigin();
        var hit = this.level().clip(new ClipContext(origin, origin.subtract(0.0, FlyingSaucerBoss.LASER_RANGE, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        Vec3 impact = hit.getLocation();
        this.entityData.set(DATA_IMPACT, new Vector3f((float)impact.x, (float)impact.y, (float)impact.z));
    }

    public Vec3 getImpact() {
        Vector3f impact = this.entityData.get(DATA_IMPACT);
        return new Vec3(impact.x, impact.y, impact.z);
    }

    public int getLaserTick() {
        return this.entityData.get(DATA_TICK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IMPACT, new Vector3f());
        builder.define(DATA_TICK, 0);
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount == 1 && !this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY() + BEAM_HEIGHT, this.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 4.0F, 0.6F);
        }
        if (this.tickCount > DURATION) {
            this.discard();
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        this.refreshImpact();
        Vec3 origin = this.getBeamOrigin();
        Vec3 impact = this.getImpact();
        int fireTick = this.tickCount - FlyingSaucerBoss.LASER_CHARGE_TICKS;
        if (fireTick == 0) {
            this.level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 5.0F, 0.55F);
        }
        if (fireTick >= 0 && fireTick < FlyingSaucerBoss.LASER_FIRE_TICKS && fireTick % 5 == 0) {
            AABB column = new AABB(origin.x - FlyingSaucerBoss.LASER_BEAM_RADIUS, impact.y - 0.5, origin.z - FlyingSaucerBoss.LASER_BEAM_RADIUS,
                origin.x + FlyingSaucerBoss.LASER_BEAM_RADIUS, origin.y, origin.z + FlyingSaucerBoss.LASER_BEAM_RADIUS);
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, column, this::canDamage)) {
                if (victim.hurt(this.damageSource(), FlyingSaucerBoss.LASER_BEAM_DAMAGE)) {
                    victim.igniteForSeconds(3.0F);
                }
            }
        }
        if (fireTick >= 0 && fireTick < FlyingSaucerBoss.LASER_FIRE_TICKS && fireTick % FlyingSaucerBoss.WAVE_INTERVAL == 0) {
            this.level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.2F, 0.5F + fireTick / (float)FlyingSaucerBoss.LASER_FIRE_TICKS * 0.3F);
            if (this.level() instanceof ServerLevel serverLevel) {
                BlockState ground = this.level().getBlockState(BlockPos.containing(impact.x, impact.y - 0.1, impact.z));
                if (!ground.isAir()) {
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        impact.x, impact.y + 0.1, impact.z, 24, 1.2, 0.1, 1.2, 0.25);
                }
            }
        }
        for (int wave = 0; wave < FlyingSaucerBoss.WAVE_COUNT; wave++) {
            int age = fireTick - wave * FlyingSaucerBoss.WAVE_INTERVAL;
            if (age < 1 || age >= FlyingSaucerBoss.WAVE_LIFETIME) {
                continue;
            }
            float inner = FlyingSaucerBoss.waveRadius(age - 1);
            float outer = FlyingSaucerBoss.waveRadius(age);
            AABB area = new AABB(impact, impact).inflate(outer + 0.5, 2.5, outer + 0.5);
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area, this::canDamage)) {
                double dx = victim.getX() - impact.x;
                double dz = victim.getZ() - impact.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                long key = ((long)victim.getId() << 8) | wave;
                if (distance < inner - 0.6 || distance > outer + 0.6 || Math.abs(victim.getY() - impact.y) > 2.5
                    || !this.waveHits.add(key)) {
                    continue;
                }
                if (victim.hurt(this.damageSource(), FlyingSaucerBoss.WAVE_DAMAGE)) {
                    victim.knockback(1.1, -dx, -dz);
                    victim.hurtMarked = true;
                }
            }
        }
        this.entityData.set(DATA_TICK, this.tickCount);
    }

    private boolean canDamage(LivingEntity victim) {
        if (!victim.isAlive() || victim instanceof Player) {
            return false;
        }
        UUID owner = this.entityData.get(DATA_OWNER).orElse(null);
        if (owner == null) {
            return true;
        }
        if (victim instanceof MilitaryDrone drone && owner.equals(drone.getOwnerUUID())) {
            return false;
        }
        if (victim instanceof MechanicalSpider spider && spider.isSummonedBy(owner)) {
            return false;
        }
        if (victim instanceof PrismSentinel sentinel && owner.equals(sentinel.getSupportOwner())) {
            return false;
        }
        return true;
    }

    private DamageSource damageSource() {
        UUID ownerId = this.entityData.get(DATA_OWNER).orElse(null);
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.getEntity(ownerId) instanceof Player owner) {
                return this.damageSources().playerAttack(owner);
            }
        }
        return this.damageSources().magic();
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return this.distanceToSqr(x, y, z) < 96.0 * 96.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(FlyingSaucerBoss.WAVE_MAX_RADIUS, BEAM_HEIGHT, FlyingSaucerBoss.WAVE_MAX_RADIUS);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
