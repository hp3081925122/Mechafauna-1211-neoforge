package com.hp.mechafauna.entity;

import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.hp.mechafauna.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class MechStrikeZone extends Entity {
    public static final int STYLE_ORBITAL = 0;
    public static final int STYLE_MORTAR = 1;
    public static final int STYLE_PLASMA = 2;
    public static final int AFTERMATH_TICKS = 14;
    public static final int FALL_TICKS = 7;
    public static final double FALL_HEIGHT = 26.0;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<Integer> DATA_STYLE = SynchedEntityData.defineId(MechStrikeZone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DELAY = SynchedEntityData.defineId(MechStrikeZone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(MechStrikeZone.class, EntityDataSerializers.FLOAT);
    private UUID ownerId;
    private float damage;
    private double knockback;

    public MechStrikeZone(EntityType<? extends MechStrikeZone> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void spawn(LivingEntity owner, Vec3 position, int style, int delay, float radius, float damage, double knockback) {
        Level level = owner.level();
        MechStrikeZone zone = ModEntities.MECH_STRIKE_ZONE.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (zone == null) {
            return;
        }
        Vec3 top = position.add(0.0, 4.0, 0.0);
        HitResult ground = level.clip(new ClipContext(top, position.subtract(0.0, 12.0, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, owner));
        Vec3 impact = ground.getType() == HitResult.Type.MISS ? position : ground.getLocation();
        zone.setPos(impact);
        zone.ownerId = owner.getUUID();
        zone.damage = damage;
        zone.knockback = knockback;
        zone.entityData.set(DATA_STYLE, style);
        zone.entityData.set(DATA_DELAY, Math.max(FALL_TICKS + 1, delay));
        zone.entityData.set(DATA_RADIUS, radius);
        level.addFreshEntity(zone);
    }

    public int getStyle() {
        return this.entityData.get(DATA_STYLE);
    }

    public int getDelay() {
        return this.entityData.get(DATA_DELAY);
    }

    public float getRadius() {
        return this.entityData.get(DATA_RADIUS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STYLE, STYLE_ORBITAL);
        builder.define(DATA_DELAY, 30);
        builder.define(DATA_RADIUS, 2.5F);
    }

    @Override
    public void tick() {
        super.tick();
        int delay = this.getDelay();
        if (this.level().isClientSide()) {
            if (this.tickCount < delay && this.tickCount % 3 == 0) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double radius = this.getRadius() * (0.6 + this.random.nextDouble() * 0.4);
                this.level().addParticle(this.getStyle() != STYLE_MORTAR ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.SMALL_FLAME,
                    this.getX() + Math.cos(angle) * radius, this.getY() + 0.1, this.getZ() + Math.sin(angle) * radius, 0.0, 0.04, 0.0);
            }
            return;
        }
        if (this.tickCount == delay - FALL_TICKS) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                this.getStyle() != STYLE_MORTAR ? SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR : SoundEvents.ARROW_SHOOT,
                SoundSource.HOSTILE, 1.4F, 0.6F);
        }
        if (this.tickCount == delay) {
            this.impact();
        }
        if (this.tickCount > delay + AFTERMATH_TICKS) {
            this.discard();
        }
    }

    private void impact() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = this.ownerId != null && serverLevel.getEntity(this.ownerId) instanceof LivingEntity living ? living : null;
        float radius = this.getRadius();
        double radiusSquared = radius * radius;
        int hits = 0;
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(radius, 2.5, radius),
            entity -> entity.isAlive() && entity != owner && !AnimatedHostileMob.isIgnoredByOtherMechs(entity)
                && !(owner != null && owner.isAlliedTo(entity)))) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            if (dx * dx + dz * dz > radiusSquared) {
                continue;
            }
            double falloff = 1.0 - 0.4 * Math.sqrt(dx * dx + dz * dz) / radius;
            float dealt = (float)(this.damage * falloff);
            if (owner instanceof FlyingSaucerBoss saucer) {
                dealt = saucer.withPercentDamage(victim, dealt);
            }
            if (victim.hurtOrSimulate(this.damageSources().explosion(this, owner), dealt)) {
                victim.knockback(this.knockback, -dx, -dz);
                hits++;
            }
        }
        serverLevel.sendParticles(new ExplosionParticleOptions(radius * 0.9F), this.getX(), this.getY() + 0.4, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        serverLevel.sendParticles(this.getStyle() != STYLE_MORTAR ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.LAVA,
            this.getX(), this.getY() + 0.3, this.getZ(), 18, radius * 0.4, 0.2, radius * 0.4, 0.4);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 10, radius * 0.35, 0.3, radius * 0.35, 0.03);
        BlockState groundState = serverLevel.getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.1, this.getZ()));
        if (!groundState.isAir()) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState),
                this.getX(), this.getY() + 0.1, this.getZ(), 22, radius * 0.45, 0.1, radius * 0.45, 0.25);
        }
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
            1.6F, 0.85F + this.random.nextFloat() * 0.25F);
        LOGGER.debug("Mech strike zone impact zone={} style={} owner={} radius={} damage={} hits={}",
            this.getId(), this.getStyle(), owner == null ? -1 : owner.getId(), radius, this.damage, hits);
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return this.distanceToSqr(x, y, z) < 96.0 * 96.0;
    }

    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(this.getRadius() + 2.0, FALL_HEIGHT, this.getRadius() + 2.0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
