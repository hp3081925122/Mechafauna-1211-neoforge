package com.hp.mechafauna.entity;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.MechafaunaConfig;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.slf4j.Logger;

public final class HeavyMech extends MechanicalMob {
    private static final String MACHINE_GUN = "machine_gun";
    private static final String MISSILE = "missile";
    private static final String STOMP = "stomp";
    private static final String BACKSTEP = "backstep";
    public static final String ORBITAL_BARRAGE = "orbital_barrage";
    public static final String ROCKET_CHARGE = "rocket_charge";
    public static final String OVERLOAD = "overload";
    public static final int CHARGE_LOCK_TICK = 12;
    public static final int CHARGE_DASH_START = 16;
    public static final int CHARGE_DASH_END = 27;
    public static final double CHARGE_SPEED = 1.05;
    public static final double CHARGE_LENGTH = CHARGE_SPEED * (CHARGE_DASH_END - CHARGE_DASH_START + 1);
    public static final int OVERLOAD_PULSE_TICK = 30;
    public static final int OVERLOAD_WAVE_TICKS = 14;
    public static final double OVERLOAD_WAVE_RADIUS = 9.0;
    private static final int[] BARRAGE_LAUNCH_TICKS = {10, 13, 16, 19, 22, 25};
    private static final int BARRAGE_MARK_TICK = 14;
    private static final int[] MISSILE_TICKS = {8, 12, 16, 21, 25, 30};
    private static final int[] BACKSTEP_ATTACK_TICKS = {4, 8, 12, 16, 20, 24};
    private static final int[][] BACKSTEP_MOVE_WINDOWS = {{2, 7}, {11, 16}, {20, 25}};
    private static final Vec3[] MACHINE_GUN_MUZZLES = {
        WeaponMount.point(-19.7, 40.55, -22),
        WeaponMount.point(-22.3, 40.55, -22),
        WeaponMount.point(-18.4, 38.3, -22),
        WeaponMount.point(-23.6, 38.3, -22),
        WeaponMount.point(-19.7, 36.05, -22),
        WeaponMount.point(-22.3, 36.05, -22)
    };
    private static final Vec3[] MISSILE_MUZZLES = {
        WeaponMount.point(23.55, 42.5, -10.8),
        WeaponMount.point(18.45, 42.5, -10.8),
        WeaponMount.point(23.55, 38.5, -10.8),
        WeaponMount.point(18.45, 38.5, -10.8),
        WeaponMount.point(23.55, 34.5, -10.8),
        WeaponMount.point(18.45, 34.5, -10.8)
    };
    private static final double KNOCKBACK_RESISTANCE = 0.85;
    private static final Identifier OVERLOAD_SPEED_ID = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "heavy_mech_overload_speed");
    private static final EntityDataAccessor<Integer> DATA_SKILL_TICK = SynchedEntityData.defineId(HeavyMech.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_CHARGE_YAW = SynchedEntityData.defineId(HeavyMech.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3fc> DATA_CHARGE_ORIGIN = SynchedEntityData.defineId(HeavyMech.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_OVERLOAD_TICKS = SynchedEntityData.defineId(HeavyMech.class, EntityDataSerializers.INT);
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Set<Integer> skillHits = new HashSet<>();
    private int backstepMoveIndex;
    private double backstepMoveProgress;
    private boolean overloadPending;
    private boolean overloadSpent;
    private boolean chargeStopped;

    public HeavyMech(EntityType<? extends HeavyMech> entityType, Level level) {
        super(entityType, level);
        if (!level.isClientSide()) {
            AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_MAX_HEALTH));
                this.setHealth(this.getMaxHealth());
            }
            AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_ATTACK_DAMAGE));
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(180.0, 0.23, 14.0, 40.0, KNOCKBACK_RESISTANCE).add(Attributes.ATTACK_DAMAGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKILL_TICK, 0);
        builder.define(DATA_CHARGE_YAW, 0.0F);
        builder.define(DATA_CHARGE_ORIGIN, new Vector3f());
        builder.define(DATA_OVERLOAD_TICKS, 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("OverloadPending", this.overloadPending);
        output.putBoolean("OverloadSpent", this.overloadSpent);
        output.putInt("OverloadTicks", this.getOverloadTicks());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.overloadPending = input.getBooleanOr("OverloadPending", false);
        this.overloadSpent = input.getBooleanOr("OverloadSpent", false);
        this.entityData.set(DATA_OVERLOAD_TICKS, input.getIntOr("OverloadTicks", 0));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tickClientEffects();
            return;
        }
        if (!this.isAlive()) {
            return;
        }
        if (!this.overloadSpent && !this.overloadPending && MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_OVERLOAD)
            && this.getHealth() <= this.getMaxHealth() * MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_OVERLOAD_HEALTH_RATIO)) {
            this.overloadPending = true;
            LOGGER.debug("Heavy mech overload armed entity={} health={} maxHealth={}", this.getId(), this.getHealth(), this.getMaxHealth());
        }
        int overloadTicks = this.getOverloadTicks();
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (overloadTicks > 0) {
            this.entityData.set(DATA_OVERLOAD_TICKS, overloadTicks - 1);
            if (speed != null && !speed.hasModifier(OVERLOAD_SPEED_ID)) {
                speed.addTransientModifier(new AttributeModifier(OVERLOAD_SPEED_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if (this.tickCount % 4 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2.2, this.getZ(), 3, 0.8, 0.9, 0.8, 0.12);
            }
            if (overloadTicks == 1) {
                LOGGER.debug("Heavy mech overload expired entity={}", this.getId());
            }
        } else if (speed != null && speed.hasModifier(OVERLOAD_SPEED_ID)) {
            speed.removeModifier(OVERLOAD_SPEED_ID);
        }
    }

    private void tickClientEffects() {
        String action = this.getActiveAction();
        int skillTick = this.getSkillTick();
        if (ROCKET_CHARGE.equals(action)) {
            Vec3 back = Vec3.directionFromRotation(0.0F, this.yBodyRot).multiply(-1.0, 0.0, -1.0);
            Vec3 side = new Vec3(-back.z, 0.0, back.x);
            boolean dashing = skillTick >= CHARGE_DASH_START && skillTick <= CHARGE_DASH_END;
            int count = dashing ? 6 : skillTick < CHARGE_DASH_START ? 2 : 0;
            for (int i = 0; i < count; i++) {
                double lateral = (i % 2 == 0 ? 1.0 : -1.0) * 0.7;
                Vec3 nozzle = this.position().add(0.0, 2.5, 0.0).add(back.scale(0.9)).add(side.scale(lateral));
                Vec3 velocity = back.scale(dashing ? 0.45 : 0.15)
                    .add((this.random.nextDouble() - 0.5) * 0.08, -0.02, (this.random.nextDouble() - 0.5) * 0.08);
                this.level().addParticle(ParticleTypes.FLAME, true, false, nozzle.x, nozzle.y, nozzle.z, velocity.x, velocity.y, velocity.z);
                this.level().addParticle(dashing ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, true, false, nozzle.x, nozzle.y, nozzle.z,
                    velocity.x * 0.5, 0.02, velocity.z * 0.5);
            }
            if (dashing) {
                this.level().addParticle(ParticleTypes.CLOUD, this.getX() + (this.random.nextDouble() - 0.5) * 1.6, this.getY() + 0.1,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.6, 0.0, 0.03, 0.0);
            }
        } else if (OVERLOAD.equals(action) && skillTick < OVERLOAD_PULSE_TICK) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = 3.5 + this.random.nextDouble() * 2.0;
            Vec3 start = this.position().add(Math.cos(angle) * radius, 0.6 + this.random.nextDouble() * 3.0, Math.sin(angle) * radius);
            Vec3 velocity = this.position().add(0.0, 2.0, 0.0).subtract(start).scale(0.12);
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, true, false, start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
            this.level().addParticle(ParticleTypes.END_ROD, true, false, start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (OVERLOAD.equals(this.getActiveAction()) && this.getSkillTick() < OVERLOAD_PULSE_TICK) {
            amount *= 0.5F;
        }
        return super.hurtServer(level, damageSource, amount);
    }

    public int getSkillTick() {
        return this.entityData.get(DATA_SKILL_TICK);
    }

    public float getChargeYaw() {
        return this.entityData.get(DATA_CHARGE_YAW);
    }

    public Vec3 getChargeOrigin() {
        Vector3fc origin = this.entityData.get(DATA_CHARGE_ORIGIN);
        return new Vec3(origin.x(), origin.y(), origin.z());
    }

    public Vec3 getMissileAimOverride(float partialTick) {
        if (!ORBITAL_BARRAGE.equals(this.getActiveAction())) {
            return null;
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot));
        return this.getPosition(partialTick).add(forward.scale(6.0)).add(0.0, 40.0, 0.0);
    }

    public int getOverloadTicks() {
        return this.entityData.get(DATA_OVERLOAD_TICKS);
    }

    public boolean isOverloaded() {
        return this.getOverloadTicks() > 0;
    }

    public static double overloadWaveRadius(float age) {
        float progress = Mth.clamp(age / OVERLOAD_WAVE_TICKS, 0.0F, 1.0F);
        return OVERLOAD_WAVE_RADIUS * (1.0 - (1.0 - progress) * (1.0 - progress));
    }

    private float skillDamage(ModConfigSpec.DoubleValue multiplier) {
        return (float)(this.getAttributeValue(Attributes.ATTACK_DAMAGE) * MechafaunaConfig.get(multiplier));
    }

    private boolean canSkillHit(LivingEntity victim) {
        return victim != this && victim.isAlive() && !AnimatedHostileMob.isIgnoredByOtherMechs(victim) && !this.isAlliedTo(victim);
    }

    @Override
    protected String getMovementAnimation() {
        return "animation.heavy_mech.walk";
    }

    @Override
    protected float getCombatTurnSpeed() {
        if (ROCKET_CHARGE.equals(this.getActiveAction()) && this.getSkillTick() >= CHARGE_LOCK_TICK) {
            return 0.0F;
        }
        return OVERLOAD.equals(this.getActiveAction()) ? 4.0F : 12.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }
        double distanceSquared = this.distanceToSqr(target);
        return switch (action) {
            case MACHINE_GUN -> this.hasLineOfSight(target) && distanceSquared <= 18.0 * 18.0;
            case MISSILE -> this.hasLineOfSight(target);
            case STOMP -> distanceSquared <= 4.5 * 4.5;
            case BACKSTEP, ORBITAL_BARRAGE, ROCKET_CHARGE, OVERLOAD -> true;
            default -> false;
        };
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(
            MACHINE_GUN, "animation.heavy_mech.machine_gun_fire",
            MISSILE, "animation.heavy_mech.missile_fire",
            STOMP, "animation.heavy_mech.stomp",
            BACKSTEP, "animation.heavy_mech.backstep",
            ORBITAL_BARRAGE, "animation.heavy_mech.orbital_barrage",
            ROCKET_CHARGE, "animation.heavy_mech.rocket_charge",
            OVERLOAD, "animation.heavy_mech.overload"
        );
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        if (this.overloadPending) {
            return List.of(OVERLOAD);
        }
        List<String> actions = new ArrayList<>();
        if (MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_ROCKET_CHARGE) && distanceSquared >= 5.0 * 5.0 && distanceSquared <= 15.0 * 15.0
            && Math.abs(target.getY() - this.getY()) < 2.5) {
            actions.add(ROCKET_CHARGE);
        }
        if (MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_ORBITAL_BARRAGE) && distanceSquared >= 7.0 * 7.0 && distanceSquared <= 32.0 * 32.0) {
            actions.add(ORBITAL_BARRAGE);
        }
        actions.addAll(this.getBasicActions(distanceSquared));
        return actions;
    }

    private List<String> getBasicActions(double distanceSquared) {
        if (distanceSquared <= 3.6 * 3.6) {
            return List.of(BACKSTEP, STOMP);
        }
        if (distanceSquared <= 4.5 * 4.5) {
            return List.of(STOMP, MACHINE_GUN);
        }
        if (distanceSquared <= 15.0 * 15.0) {
            return List.of(MACHINE_GUN);
        }
        return List.of(MISSILE);
    }

    @Override
    public int getActionDuration(String action) {
        return switch (action) {
            case MACHINE_GUN -> 32;
            case MISSILE -> 54;
            case STOMP -> 52;
            case BACKSTEP -> 28;
            case ORBITAL_BARRAGE -> 60;
            case ROCKET_CHARGE -> 44;
            case OVERLOAD -> 60;
            default -> 1;
        };
    }

    @Override
    public int getActionCooldown(String action) {
        int cooldown = switch (action) {
            case MACHINE_GUN -> 46;
            case MISSILE -> 100;
            case STOMP -> 76;
            case BACKSTEP -> 110;
            case ORBITAL_BARRAGE -> MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_ORBITAL_BARRAGE_COOLDOWN);
            case ROCKET_CHARGE -> MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_ROCKET_CHARGE_COOLDOWN);
            default -> 20;
        };
        return this.isOverloaded() ? Math.max(10, Math.round(cooldown * 0.6F)) : cooldown;
    }

    @Override
    public double getCombatHoldDistance() {
        return 13.0;
    }

    @Override
    public double getCombatRetreatDistance() {
        return ROCKET_CHARGE.equals(this.getActiveAction()) ? 0.0 : 8.0;
    }

    @Override
    public double getChaseSpeed() {
        return 0.9;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.entityData.set(DATA_SKILL_TICK, 0);
        this.skillHits.clear();
        if (ORBITAL_BARRAGE.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.6F, 1.6F);
        } else if (ROCKET_CHARGE.equals(action)) {
            this.chargeStopped = false;
            this.entityData.set(DATA_CHARGE_YAW, this.yBodyRot);
            this.entityData.set(DATA_CHARGE_ORIGIN, this.position().toVector3f());
            this.level().playSound(null, this.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 1.6F, 0.5F);
        } else if (OVERLOAD.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.5F, 0.5F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0F, 0.6F);
        } else if (BACKSTEP.equals(action)) {
            this.backstepMoveIndex = 0;
            this.backstepMoveProgress = 0.0;
        } else if (MACHINE_GUN.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.5F, 0.7F);
        } else if (MISSILE.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 1.5F, 0.55F);
        }
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        this.entityData.set(DATA_SKILL_TICK, actionTick);
        if (ORBITAL_BARRAGE.equals(action)) {
            this.tickOrbitalBarrage(actionTick, target);
        } else if (ROCKET_CHARGE.equals(action)) {
            this.tickRocketCharge(actionTick);
        } else if (OVERLOAD.equals(action)) {
            this.tickOverload(actionTick);
        } else if (MACHINE_GUN.equals(action) && actionTick >= 4 && actionTick <= 22 && actionTick % 2 == 0) {
            this.fireMachineGunVolley(target);
        } else if (MISSILE.equals(action) && this.isMissileLaunchTick(actionTick)) {
            this.launchPodMissileAtTick(target, actionTick);
        } else if (STOMP.equals(action) && actionTick == 21) {
            this.damageNearby(3.8, 9.0F, 0.85);
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.8F, 0.65F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 20, 1.5, 0.12, 1.5, 0.04);
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.15, this.getZ(), 12, 1.1, 0.12, 1.1, 0.08);
            }
        } else if (BACKSTEP.equals(action)) {
            this.tickBackstep(actionTick, target);
        }
    }

    private void tickOrbitalBarrage(int actionTick, LivingEntity target) {
        for (int index = 0; index < BARRAGE_LAUNCH_TICKS.length; index++) {
            if (BARRAGE_LAUNCH_TICKS[index] != actionTick) {
                continue;
            }
            String animation = this.getActionAnimations().get(ORBITAL_BARRAGE);
            double seconds = actionTick / 20.0;
            WeaponPose pose = WeaponPose.forModel("heavy_mech");
            WeaponMount.Aim aim = WeaponMount.MECH_MISSILES.aim(pose.parentTransform(WeaponMount.MECH_MISSILES,
                WeaponMount.modelToWorld(this.position(), this.yBodyRot, this.getModelScale()), animation, seconds), this.getMissileAimOverride(1.0F));
            Vec3 muzzle = aim.muzzle(pose.muzzle(WeaponMount.MECH_MISSILES, MISSILE_MUZZLES[index], animation, seconds));
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 1.0F, 1.0F, 1.0F), muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
                for (int i = 0; i < 8; i++) {
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, muzzle.x, muzzle.y + i * 0.5, muzzle.z, 0,
                        (this.random.nextDouble() - 0.5) * 0.1, 1.0, (this.random.nextDouble() - 0.5) * 0.1, 0.9 + i * 0.05);
                }
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.15, 0.1, 0.15, 0.02);
            }
            this.level().playSound(null, muzzle.x, muzzle.y, muzzle.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 1.6F, 0.6F + index * 0.05F);
        }
        if (actionTick != BARRAGE_MARK_TICK) {
            return;
        }
        int strikes = this.isOverloaded() ? 7 : 5;
        float damage = this.skillDamage(MechafaunaConfig.HEAVY_MECH_ORBITAL_BARRAGE_MULTIPLIER);
        Vec3 lead = target.position().add(target.getDeltaMovement().multiply(12.0, 0.0, 12.0));
        for (int i = 0; i < strikes; i++) {
            Vec3 point = lead;
            if (i > 0) {
                double angle = (i - 1) * Math.PI * 2.0 / (strikes - 1) + this.random.nextDouble() * 0.6;
                double distance = 2.8 + this.random.nextDouble() * 2.6;
                point = lead.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
            }
            MechStrikeZone.spawn(this, point, MechStrikeZone.STYLE_ORBITAL, 30 + i * 5, 2.6F, damage, 0.55);
        }
        this.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
        LOGGER.debug("Heavy mech orbital barrage marked entity={} target={} strikes={} damage={}", this.getId(), target.getId(), strikes, damage);
    }

    private void tickRocketCharge(int actionTick) {
        if (actionTick < CHARGE_LOCK_TICK) {
            this.entityData.set(DATA_CHARGE_YAW, this.yBodyRot);
            this.entityData.set(DATA_CHARGE_ORIGIN, this.position().toVector3f());
            return;
        }
        if (actionTick == CHARGE_LOCK_TICK) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.8F, 0.5F);
        }
        if (actionTick < CHARGE_DASH_START || actionTick > CHARGE_DASH_END || this.chargeStopped) {
            return;
        }
        if (actionTick == CHARGE_DASH_START) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.4F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 1.2F, 0.7F);
        }
        Vec3 direction = Vec3.directionFromRotation(0.0F, this.getChargeYaw()).multiply(1.0, 0.0, 1.0).normalize();
        this.move(MoverType.SELF, direction.scale(CHARGE_SPEED));
        float damage = this.skillDamage(MechafaunaConfig.HEAVY_MECH_ROCKET_CHARGE_MULTIPLIER);
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.8, 0.2, 0.8), this::canSkillHit)) {
            if (this.skillHits.add(victim.getId()) && victim.hurtOrSimulate(this.damageSources().mobAttack(this), damage)) {
                victim.knockback(1.1, -direction.x, -direction.z);
                LOGGER.debug("Heavy mech rocket charge hit entity={} victim={} damage={}", this.getId(), victim.getId(), damage);
            }
        }
        if (this.horizontalCollision && actionTick > CHARGE_DASH_START) {
            this.chargeStopped = true;
            this.chargeImpact(damage * 0.6F);
        } else if (actionTick == CHARGE_DASH_END) {
            this.chargeImpact(damage * 0.4F);
        }
    }

    private void chargeImpact(float damage) {
        double radius = 3.2;
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius, 1.0, radius), this::canSkillHit)) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            if (dx * dx + dz * dz <= radius * radius && victim.hurtOrSimulate(this.damageSources().mobAttack(this), damage)) {
                victim.knockback(0.7, -dx, -dz);
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4F, 0.5F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4F, 0.7F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.6, 0.4, 0.6, 0.0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 24, 1.8, 0.1, 1.8, 0.08);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.5, this.getZ(), 16, 1.4, 0.3, 1.4, 0.2);
        }
        LOGGER.debug("Heavy mech rocket charge impact entity={} stopped={} damage={}", this.getId(), this.chargeStopped, damage);
    }

    private void tickOverload(int actionTick) {
        if (actionTick == OVERLOAD_PULSE_TICK) {
            this.overloadPending = false;
            this.overloadSpent = true;
            this.entityData.set(DATA_OVERLOAD_TICKS, MechafaunaConfig.get(MechafaunaConfig.HEAVY_MECH_OVERLOAD_DURATION));
            this.level().playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 0.7F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.2F, 1.6F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 1.0F, 1.0F, 1.0F), this.getX(), this.getY() + 2.0, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2.0, this.getZ(), 60, 1.2, 1.2, 1.2, 0.8);
            }
            LOGGER.debug("Heavy mech overload pulse entity={} durationTicks={}", this.getId(), this.getOverloadTicks());
        }
        int age = actionTick - OVERLOAD_PULSE_TICK;
        if (age < 1 || age > OVERLOAD_WAVE_TICKS) {
            return;
        }
        double inner = overloadWaveRadius(age - 1) - 0.6;
        double outer = overloadWaveRadius(age) + 0.6;
        float damage = this.skillDamage(MechafaunaConfig.HEAVY_MECH_OVERLOAD_PULSE_MULTIPLIER);
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
            new AABB(this.position(), this.position()).inflate(outer, 3.0, outer), this::canSkillHit)) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < inner || distance > outer || !this.skillHits.add(victim.getId())) {
                continue;
            }
            if (victim.hurtOrSimulate(this.damageSources().mobAttack(this), damage)) {
                victim.knockback(0.9, -dx, -dz);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), this);
                LOGGER.debug("Heavy mech overload wave hit entity={} victim={} damage={} distance={}", this.getId(), victim.getId(), damage, distance);
            }
        }
    }

    private boolean isMissileLaunchTick(int tick) {
        for (int launchTick : MISSILE_TICKS) {
            if (tick == launchTick) {
                return true;
            }
        }
        return false;
    }

    private void launchPodMissileAtTick(LivingEntity target, int tick) {
        int launchIndex = 0;
        while (launchIndex < MISSILE_TICKS.length && MISSILE_TICKS[launchIndex] != tick) {
            launchIndex++;
        }
        if (launchIndex < MISSILE_MUZZLES.length) {
            this.launchPodMissileFromIndex(target, launchIndex);
        }
    }

    private void launchPodMissileFromIndex(LivingEntity target, int launchIndex) {
        this.launchProjectile(target, WeaponMount.MECH_MISSILES, MISSILE_MUZZLES[launchIndex], true, false, 1.65, 0.0F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 0.8F + this.random.nextFloat() * 0.2F);
    }

    private void fireMachineGunVolley(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        double forwardAngle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 0.8 + this.random.nextDouble() * 1.4;
        Vec3 forward = targetCenter.subtract(this.getBoundingBox().getCenter()).normalize();
        if (forward.lengthSqr() < 1.0E-6) {
            forward = this.getLookAngle().normalize();
        }
        Vec3 reference = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        for (int index = 0; index < MACHINE_GUN_MUZZLES.length; index++) {
            Vec3 muzzle = MACHINE_GUN_MUZZLES[index];
            double angle = forwardAngle + index * (Math.PI * 2.0 / MACHINE_GUN_MUZZLES.length);
            Vec3 aimPoint = targetCenter.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            this.launchCyanLaser(target, WeaponMount.MECH_GUN, muzzle, aimPoint, 0.72, 1.1F);
        }
    }

    private void tickBackstep(int actionTick, LivingEntity target) {
        while (this.backstepMoveIndex < BACKSTEP_MOVE_WINDOWS.length) {
            int[] window = BACKSTEP_MOVE_WINDOWS[this.backstepMoveIndex];
            if (actionTick < window[0]) {
                break;
            }
            double phase = Math.min(1.0, (double)(actionTick - window[0] + 1) / (window[1] - window[0] + 1));
            double easedProgress = phase * phase * (3.0 - 2.0 * phase);
            double distance = easedProgress - this.backstepMoveProgress;
            if (distance > 0.0) {
                Vec3 backward = this.getLookAngle().multiply(-1.0, 0.0, -1.0).normalize();
                this.move(MoverType.SELF, backward.scale(distance));
                this.backstepMoveProgress = easedProgress;
            }
            if (phase >= 1.0) {
                this.backstepMoveIndex++;
                this.backstepMoveProgress = 0.0;
            } else {
                break;
            }
        }

        if (this.isBackstepAttackTick(actionTick)) {
            if (this.random.nextInt(4) == 0) {
                this.launchPodMissileFromIndex(target, this.random.nextInt(MISSILE_MUZZLES.length));
            } else {
                this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.65F);
                this.fireMachineGunVolley(target);
            }
        }
    }

    private boolean isBackstepAttackTick(int actionTick) {
        for (int attackTick : BACKSTEP_ATTACK_TICKS) {
            if (actionTick == attackTick) {
                return true;
            }
        }
        return false;
    }
}
