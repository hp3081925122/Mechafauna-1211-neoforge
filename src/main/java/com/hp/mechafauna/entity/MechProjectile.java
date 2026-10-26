package com.hp.mechafauna.entity;

import com.hp.mechafauna.Mechafauna;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.neoforged.neoforge.entity.PartEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class MechProjectile extends ThrowableItemProjectile implements GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExplosionDamageCalculator PROJECTILE_EXPLOSION_CALCULATOR = new ExplosionDamageCalculator() {
        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.5F;
        }
    };
    private static final ResourceKey<DamageType> MILITARY_DRONE = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "military_drone"));
    private static final ResourceKey<DamageType> REINFORCED_BULLET = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "reinforced_bullet"));
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private float impactDamage;
    private boolean bypassHurtCooldown;
    private boolean reinforcedBulletDamage;
    private boolean igniteOnHit;

    public MechProjectile(EntityType<? extends MechProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public MechProjectile(EntityType<? extends MechProjectile> entityType, Level level, LivingEntity owner, boolean missile, boolean laserTracer, float impactDamage) {
        this(entityType, level, owner, missile, laserTracer, false, impactDamage);
    }

    public MechProjectile(EntityType<? extends MechProjectile> entityType, Level level, LivingEntity owner, boolean missile, boolean laserTracer, boolean prismLaser, float impactDamage) {
        super(entityType, owner, level);
        this.impactDamage = impactDamage;
        this.setItem(new ItemStack(prismLaser ? Items.AMETHYST_SHARD : laserTracer ? Items.REDSTONE : missile ? Items.FIREWORK_ROCKET : Items.IRON_NUGGET));
    }

    public boolean isMissile() {
        return this.getItem().is(Items.FIREWORK_ROCKET);
    }

    public boolean isLaserTracer() {
        return this.isCyanLaserTracer() || this.isPrismLaserTracer();
    }

    public boolean isCyanLaserTracer() {
        return this.getItem().is(Items.REDSTONE);
    }

    public boolean isPrismLaserTracer() {
        return this.getItem().is(Items.AMETHYST_SHARD);
    }

    public void setBypassHurtCooldown(boolean bypassHurtCooldown) {
        this.bypassHurtCooldown = bypassHurtCooldown;
    }

    public void setIgniteOnHit(boolean igniteOnHit) {
        this.igniteOnHit = igniteOnHit;
    }

    public void setReinforcedBulletDamage(boolean reinforcedBulletDamage) {
        this.reinforcedBulletDamage = reinforcedBulletDamage;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("BypassCooldown", this.bypassHurtCooldown);
        tag.putBoolean("ReinforcedBulletDamage", this.reinforcedBulletDamage);
        tag.putBoolean("IgniteOnHit", this.igniteOnHit);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.bypassHurtCooldown = tag.getBoolean("BypassCooldown");
        this.reinforcedBulletDamage = tag.getBoolean("ReinforcedBulletDamage");
        this.igniteOnHit = tag.getBoolean("IgniteOnHit");
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.isRemoved()) {
            float drag = this.isInWater() ? 0.8F : 0.99F;
            this.setDeltaMovement(this.getDeltaMovement().scale(1.0 / drag));
        }
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (this.getOwner() instanceof FlyingSaucerBoss && entity instanceof PartEntity<?> part && part.getParent() == this.getOwner()) {
            return false;
        }
        if (this.getOwner() instanceof MechanicalDrone drone && drone.isAlliedTo(entity)) {
            return false;
        }
        if (this.getOwner() instanceof MilitaryDrone drone
            && (drone.isAlliedTo(entity) || entity instanceof Player || entity instanceof MilitaryDrone)) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_NUGGET;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (this.isLaserTracer()) {
            if (!this.level().isClientSide) {
                boolean damageApplied = false;
                Entity target = hit.getEntity();
                if (target instanceof LivingEntity || target instanceof SaucerDamagePart) {
                    damageApplied = target.hurt(this.impactSource(), this.ownerPercentDamage(target, this.impactDamage));
                }
                if (damageApplied && this.igniteOnHit) {
                    Entity burningTarget = target instanceof PartEntity<?> part ? part.getParent() : target;
                    if (burningTarget instanceof LivingEntity livingTarget) {
                        livingTarget.igniteForSeconds(2.0F);
                    }
                }
                LOGGER.debug("Mech laser collision projectile={} target={} damage={} applied={}", this.getId(), hit.getEntity().getId(), this.impactDamage, damageApplied);
                this.discard();
            }
            return;
        }
        if (!this.level().isClientSide) {
            Entity livingTarget = hit.getEntity();
            if (livingTarget instanceof LivingEntity || livingTarget instanceof SaucerDamagePart) {
                float damage = this.getItem().is(Items.FIREWORK_ROCKET) ? 4.0F : 8.0F;
                livingTarget.hurt(this.impactSource(), this.ownerPercentDamage(livingTarget, damage));
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide) {
            if (this.isLaserTracer()) {
                this.discard();
                return;
            }
            float radius = this.getItem().is(Items.FIREWORK_ROCKET) ? 2.4F : 2.70F;
            net.minecraft.world.entity.Entity explosionSource = this.getOwner() == null ? this : this.getOwner();
            ExplosionEffects.explode(this.level(), explosionSource, this.damageSources().explosion(this, this.getOwner()),
                PROJECTILE_EXPLOSION_CALCULATOR, this.getX(), this.getY(), this.getZ(), radius);
            this.discard();
        }
    }

    private float ownerPercentDamage(Entity target, float damage) {
        return this.getOwner() instanceof FlyingSaucerBoss saucer ? saucer.withPercentDamage(target, damage) : damage;
    }

    private DamageSource impactSource() {
        if (this.bypassHurtCooldown) {
            ResourceKey<DamageType> damageType = this.reinforcedBulletDamage ? REINFORCED_BULLET : MILITARY_DRONE;
            return this.damageSources().source(damageType, this, this.getOwner());
        }
        return this.damageSources().thrown(this, this.getOwner());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }
}
