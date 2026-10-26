package com.hp.mechafauna.entity;

import com.hp.mechafauna.MechafaunaConfig;
import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.hp.mechafauna.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import org.joml.Vector3f;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;

public final class MechanicalTank extends MechanicalMob {
    private static final String FIRE = "fire";
    public static final String RAILGUN = "railgun";
    public static final String CLUSTER_MORTAR = "cluster_mortar";
    private static final String MOVE = "animation.mechanical_tank.move";
    private static final String TURRET_TURN = "animation.mechanical_tank.turret_turn";
    private static final String RAILGUN_ANIMATION = "animation.mechanical_tank.railgun";
    private static final String MORTAR_ANIMATION = "animation.mechanical_tank.cluster_mortar";
    public static final int RAIL_LOCK_TICK = 34;
    public static final int RAIL_FIRE_TICK = 42;
    public static final int RAIL_BEAM_TICKS = 14;
    public static final double RAIL_RANGE = 32.0;
    public static final double RAIL_HIT_RADIUS = 0.7;
    public static final int MORTAR_FIRE_TICK = 12;
    private static final float MORTAR_RAISE_TICKS = 10.0F;
    private static final float MORTAR_LOWER_START_TICK = 26.0F;
    private static final float MORTAR_LOWER_END_TICK = 39.0F;
    public static final int SHIELD_INTRO_TICKS = 8;
    public static final int SHIELD_BURST_TICKS = 16;
    public static final double SHIELD_RADIUS = 2.7;
    public static final double SHIELD_BURST_RADIUS = 5.5;
    private static final double MAX_HEALTH = 105.0;
    private static final double KNOCKBACK_RESISTANCE = 0.55;
    private static final int SPIDER_SUMMON_INTERVAL_TICKS = 100;
    private static final int MAX_SUMMONED_SPIDERS = 3;
    private static final double REINFORCEMENT_DISTANCE = 3.2;
    private static final EntityDataAccessor<Integer> DATA_SKILL_TICK = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_RAIL_START = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_RAIL_END = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_SHIELD_TICKS = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHIELD_MAX_TICKS = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHIELD_HIT_TICK = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHIELD_BURST_TICK = SynchedEntityData.defineId(MechanicalTank.class, EntityDataSerializers.INT);
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float MODEL_SCALE = 1.4422496F;
    private int spiderSummonCooldown;
    private boolean halfHealthReinforcementsSummoned;
    private int shieldStage;
    private int clientShieldHitTick = -100;
    private int clientShieldBurstTick = -100;

    public MechanicalTank(EntityType<? extends MechanicalTank> entityType, Level level) {
        super(entityType, level);
        this.spiderSummonCooldown = this.random.nextInt(SPIDER_SUMMON_INTERVAL_TICKS) + 1;
        if (!level.isClientSide) {
            AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_MAX_HEALTH));
                this.setHealth(this.getMaxHealth());
            }
            AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_ATTACK_DAMAGE));
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SKILL_TICK, 0);
        this.entityData.define(DATA_RAIL_START, new Vector3f());
        this.entityData.define(DATA_RAIL_END, new Vector3f());
        this.entityData.define(DATA_SHIELD_TICKS, 0);
        this.entityData.define(DATA_SHIELD_MAX_TICKS, 1);
        this.entityData.define(DATA_SHIELD_HIT_TICK, -100);
        this.entityData.define(DATA_SHIELD_BURST_TICK, -100);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.tickClientEffects();
            return;
        }
        if (this.isAlive() && !this.halfHealthReinforcementsSummoned && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.halfHealthReinforcementsSummoned = true;
            double firstAngle = this.random.nextDouble() * Math.PI * 2.0;
            this.summonMechanicalWolf(firstAngle);
            this.summonMechanicalWolf(firstAngle + Math.PI);
            LOGGER.debug("Mechanical tank half-health reinforcements summoned tank={} wolves=2 target={}", this.getId(), this.getTarget() == null ? -1 : this.getTarget().getId());
        }
        if (this.isAlive()) {
            this.tickShield();
        }
        if (this.isAlive() && --this.spiderSummonCooldown <= 0) {
            int activeSummonedSpiders = 0;
            if (this.level() instanceof ServerLevel serverLevel) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (entity instanceof MechanicalSpider spider && !spider.isRemoved() && spider.isSummonedBy(this.getUUID())) {
                        activeSummonedSpiders++;
                        if (activeSummonedSpiders >= MAX_SUMMONED_SPIDERS) {
                            break;
                        }
                    }
                }
            }
            if (activeSummonedSpiders < MAX_SUMMONED_SPIDERS) {
                this.summonMechanicalSpider();
            } else {
                LOGGER.debug("Mechanical tank spider summon skipped tank={} active={} cap={}", this.getId(), activeSummonedSpiders, MAX_SUMMONED_SPIDERS);
            }
            this.spiderSummonCooldown = SPIDER_SUMMON_INTERVAL_TICKS;
        }
    }

    private void tickShield() {
        if (MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_SHIELD) && !this.isShielded()) {
            double ratio = this.getHealth() / this.getMaxHealth();
            double first = MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_SHIELD_FIRST_RATIO);
            double second = MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_SHIELD_SECOND_RATIO);
            if (this.shieldStage == 0 && ratio <= first) {
                this.shieldStage = second > 0.0 && ratio <= second ? 2 : 1;
                this.raiseShield();
            } else if (this.shieldStage == 1 && second > 0.0 && ratio <= second) {
                this.shieldStage = 2;
                this.raiseShield();
            }
        }
        int shieldTicks = this.entityData.get(DATA_SHIELD_TICKS);
        if (shieldTicks <= 0) {
            return;
        }
        this.entityData.set(DATA_SHIELD_TICKS, shieldTicks - 1);
        if (shieldTicks == 1) {
            this.burstShield();
        }
    }

    private void raiseShield() {
        int duration = MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_SHIELD_DURATION);
        this.entityData.set(DATA_SHIELD_MAX_TICKS, duration);
        this.entityData.set(DATA_SHIELD_TICKS, duration);
        this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 0.8F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2.0F, 0.6F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(), 40, 1.6, 1.0, 1.6, 0.3);
        }
        LOGGER.debug("Mechanical tank shield raised tank={} stage={} durationTicks={} health={} maxHealth={}",
            this.getId(), this.shieldStage, duration, this.getHealth(), this.getMaxHealth());
    }

    private void burstShield() {
        this.entityData.set(DATA_SHIELD_BURST_TICK, this.tickCount);
        float damage = this.skillDamage(MechafaunaConfig.MECHANICAL_TANK_SHIELD_BURST_MULTIPLIER);
        int hits = 0;
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(SHIELD_BURST_RADIUS, 1.5, SHIELD_BURST_RADIUS), this::canSkillHit)) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            if (dx * dx + dz * dz <= SHIELD_BURST_RADIUS * SHIELD_BURST_RADIUS && victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.knockback(1.0, -dx, -dz);
                hits++;
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 1.3F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(), 50, 2.0, 1.0, 2.0, 0.6);
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2, this.getZ(), 24, 1.6, 0.8, 1.6, 0.25);
        }
        LOGGER.debug("Mechanical tank shield burst tank={} damage={} hits={}", this.getId(), damage, hits);
    }

    private void tickClientEffects() {
        String action = this.getActiveAction();
        int skillTick = this.getSkillTick();
        if (RAILGUN.equals(action) && skillTick < RAIL_FIRE_TICK) {
            Vec3 muzzle = this.getCannonMuzzle(1.0F);
            if (muzzle != null) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double radius = 1.4 - skillTick / (double)RAIL_FIRE_TICK;
                Vec3 start = muzzle.add(Math.cos(angle) * radius, (this.random.nextDouble() - 0.5) * radius, Math.sin(angle) * radius);
                Vec3 velocity = muzzle.subtract(start).scale(0.18);
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, true, start.x, start.y, start.z, velocity.x, velocity.y, velocity.z);
            }
        }
        if (this.isShielded() && this.tickCount % 3 == 0) {
            double theta = Math.acos(this.random.nextDouble());
            double phi = this.random.nextDouble() * Math.PI * 2.0;
            Vec3 point = this.position().add(Math.cos(phi) * Math.sin(theta) * SHIELD_RADIUS, 1.0 + Math.cos(theta) * SHIELD_RADIUS,
                Math.sin(phi) * Math.sin(theta) * SHIELD_RADIUS);
            this.level().addParticle(ParticleTypes.END_ROD, point.x, point.y, point.z, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        if (this.isShielded() && !this.level().isClientSide) {
            amount *= (float)(1.0 - MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_SHIELD_REDUCTION));
            if (this.tickCount - this.entityData.get(DATA_SHIELD_HIT_TICK) > 4) {
                this.entityData.set(DATA_SHIELD_HIT_TICK, this.tickCount);
                this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2F, 0.6F);
            }
        }
        return super.hurt(damageSource, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpiderSummonCooldown", this.spiderSummonCooldown);
        tag.putBoolean("HalfHealthReinforcementsSummoned", this.halfHealthReinforcementsSummoned);
        tag.putInt("ShieldStage", this.shieldStage);
        tag.putInt("ShieldTicks", this.entityData.get(DATA_SHIELD_TICKS));
        tag.putInt("ShieldMaxTicks", this.entityData.get(DATA_SHIELD_MAX_TICKS));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpiderSummonCooldown")) {
            this.spiderSummonCooldown = tag.getInt("SpiderSummonCooldown");
        }
        this.halfHealthReinforcementsSummoned = tag.getBoolean("HalfHealthReinforcementsSummoned");
        this.shieldStage = tag.getInt("ShieldStage");
        this.entityData.set(DATA_SHIELD_TICKS, tag.getInt("ShieldTicks"));
        this.entityData.set(DATA_SHIELD_MAX_TICKS, Math.max(1, tag.getInt("ShieldMaxTicks")));
    }

    public int getSkillTick() {
        return this.entityData.get(DATA_SKILL_TICK);
    }

    public Vec3 getRailStart() {
        Vector3f start = this.entityData.get(DATA_RAIL_START);
        return new Vec3(start.x, start.y, start.z);
    }

    public Vec3 getRailEnd() {
        Vector3f end = this.entityData.get(DATA_RAIL_END);
        return new Vec3(end.x, end.y, end.z);
    }

    public boolean isShielded() {
        return this.entityData.get(DATA_SHIELD_TICKS) > 0;
    }

    public int getShieldTicks() {
        return this.entityData.get(DATA_SHIELD_TICKS);
    }

    public int getShieldMaxTicks() {
        return this.entityData.get(DATA_SHIELD_MAX_TICKS);
    }

    public int getShieldHitTick() {
        return this.clientShieldHitTick;
    }

    public int getShieldBurstTick() {
        return this.clientShieldBurstTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!this.level().isClientSide || this.tickCount <= 0) {
            return;
        }
        if (DATA_SHIELD_HIT_TICK.equals(key)) {
            this.clientShieldHitTick = this.tickCount;
        } else if (DATA_SHIELD_BURST_TICK.equals(key)) {
            this.clientShieldBurstTick = this.tickCount;
        }
    }

    public Optional<Vec3> getCannonAimOverride(float partialTick) {
        String action = this.getActiveAction();
        if (RAILGUN.equals(action) && this.getSkillTick() >= RAIL_LOCK_TICK) {
            return Optional.of(this.getRailEnd());
        }
        if (CLUSTER_MORTAR.equals(action)) {
            LivingEntity target = this.getCombatTarget();
            if (target != null) {
                Vec3 targetPosition = target.getPosition(partialTick);
                Vec3 position = this.getPosition(partialTick);
                double distance = targetPosition.subtract(position).horizontalDistance();
                Vec3 direct = targetPosition.add(0.0, target.getBbHeight() * 0.5, 0.0);
                Vec3 lob = position.lerp(targetPosition, 0.35).add(0.0, 4.0 + distance * 1.1, 0.0);
                float tick = this.getSkillTick() + partialTick;
                float raise = Mth.clamp(tick / MORTAR_RAISE_TICKS, 0.0F, 1.0F);
                float lower = Mth.clamp((MORTAR_LOWER_END_TICK - tick) / (MORTAR_LOWER_END_TICK - MORTAR_LOWER_START_TICK), 0.0F, 1.0F);
                float progress = Math.min(raise, lower);
                progress = progress * progress * (3.0F - 2.0F * progress);
                return Optional.of(direct.lerp(lob, progress));
            }
        }
        return Optional.empty();
    }

    public Vec3 getCannonMuzzle(float partialTick) {
        String action = this.getActiveAction();
        Vec3 aimPoint = this.getCannonAimOverride(partialTick).orElse(null);
        if (aimPoint == null) {
            LivingEntity target = this.getCombatTarget();
            if (target == null) {
                return null;
            }
            aimPoint = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.5, 0.0);
        }
        String animation = action == null ? null : this.getActionAnimations().get(action);
        double seconds = this.getSkillTick() / 20.0;
        WeaponPose pose = WeaponPose.forModel("mechanical_tank");
        float bodyYaw = Mth.rotLerp(partialTick, this.yBodyRotO, this.yBodyRot);
        var parent = pose.parentTransform(WeaponMount.TANK_CANNON, WeaponMount.modelToWorld(this.getPosition(partialTick), bodyYaw, this.getModelScale()),
            animation, seconds);
        WeaponMount.Aim aim = WeaponMount.TANK_CANNON.aim(parent, aimPoint);
        return aim.muzzle(pose.muzzle(WeaponMount.TANK_CANNON, WeaponMount.TANK_CANNON.muzzle(), animation, seconds));
    }

    private float skillDamage(ForgeConfigSpec.DoubleValue multiplier) {
        return (float)(this.getAttributeValue(Attributes.ATTACK_DAMAGE) * MechafaunaConfig.get(multiplier));
    }

    private boolean canSkillHit(LivingEntity victim) {
        return victim != this && victim.isAlive() && !AnimatedHostileMob.isIgnoredByOtherMechs(victim) && !this.isAlliedTo(victim);
    }

    private void summonMechanicalWolf(double angle) {
        MechanicalWolf wolf = ModEntities.MECHANICAL_WOLF.get().create(this.level());
        if (wolf == null) {
            return;
        }
        double x = this.getX() + Math.cos(angle) * REINFORCEMENT_DISTANCE;
        double z = this.getZ() + Math.sin(angle) * REINFORCEMENT_DISTANCE;
        wolf.moveTo(x, this.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && !AnimatedHostileMob.isIgnoredByOtherMechs(target)) {
            wolf.setTarget(target);
        }
        if (this.level().addFreshEntity(wolf)) {
            LOGGER.debug("Mechanical tank summoned wolf tank={} wolf={} target={}", this.getId(), wolf.getId(), target == null ? -1 : target.getId());
        }
    }

    private void summonMechanicalSpider() {
        MechanicalSpider spider = ModEntities.MECHANICAL_SPIDER.get().create(this.level());
        if (spider == null) {
            return;
        }
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double x = this.getX() + Math.cos(angle) * REINFORCEMENT_DISTANCE;
        double z = this.getZ() + Math.sin(angle) * REINFORCEMENT_DISTANCE;
        spider.moveTo(x, this.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
        spider.setMechSummoner(this.getUUID());
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && !AnimatedHostileMob.isIgnoredByOtherMechs(target)) {
            spider.setTarget(target);
        }
        if (this.level().addFreshEntity(spider)) {
            LOGGER.debug("Mechanical tank summoned spider tank={} spider={} target={}", this.getId(), spider.getId(), target == null ? -1 : target.getId());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(MAX_HEALTH, 0.38, 10.0, 36.0, KNOCKBACK_RESISTANCE).add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public double getModelScale() {
        return super.getModelScale() * MODEL_SCALE * MODEL_SCALE;
    }

    @Override
    protected String getMovementAnimation() {
        return MOVE;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }
        return switch (action) {
            case FIRE -> this.hasLineOfSight(target) && this.distanceToSqr(target) <= 22.0 * 22.0;
            case RAILGUN -> actionTick >= RAIL_LOCK_TICK || this.distanceToSqr(target) <= (RAIL_RANGE + 4.0) * (RAIL_RANGE + 4.0);
            case CLUSTER_MORTAR -> true;
            default -> false;
        };
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(
            FIRE, "animation.mechanical_tank.fire",
            RAILGUN, RAILGUN_ANIMATION,
            CLUSTER_MORTAR, MORTAR_ANIMATION
        );
    }

    @Override
    protected void addExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<MechanicalTank> turret = new AnimationController<>(this, "turret_scan", 0, state -> {
            if (this.hasCombatTarget() || this.isPerformingAction()) {
                return PlayState.STOP;
            }
            state.setAnimation(RawAnimation.begin().thenLoop(TURRET_TURN));
            return PlayState.CONTINUE;
        });
        controllers.add(turret);
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        List<String> actions = new ArrayList<>();
        if (MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_RAILGUN) && distanceSquared >= 6.0 * 6.0 && distanceSquared <= 28.0 * 28.0) {
            actions.add(RAILGUN);
        }
        if (MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_CLUSTER_MORTAR) && distanceSquared >= 8.0 * 8.0 && distanceSquared <= 30.0 * 30.0) {
            actions.add(CLUSTER_MORTAR);
        }
        if (distanceSquared <= 22.0 * 22.0) {
            actions.add(FIRE);
        }
        return actions;
    }

    @Override
    public int getActionDuration(String action) {
        return switch (action) {
            case RAILGUN -> RAIL_FIRE_TICK + RAIL_BEAM_TICKS;
            case CLUSTER_MORTAR -> 40;
            default -> 20;
        };
    }

    @Override
    public int getActionCooldown(String action) {
        return switch (action) {
            case RAILGUN -> MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_RAILGUN_COOLDOWN);
            case CLUSTER_MORTAR -> MechafaunaConfig.get(MechafaunaConfig.MECHANICAL_TANK_CLUSTER_MORTAR_COOLDOWN);
            default -> 72;
        };
    }

    @Override
    public double getCombatHoldDistance() {
        return 16.0;
    }

    @Override
    public double getCombatRetreatDistance() {
        return 11.0;
    }

    @Override
    public double getChaseSpeed() {
        return 0.86;
    }

    @Override
    public boolean canMoveDuringAction(String action) {
        return FIRE.equals(action);
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.entityData.set(DATA_SKILL_TICK, 0);
        if (RAILGUN.equals(action)) {
            this.entityData.set(DATA_RAIL_END, target.getBoundingBox().getCenter().toVector3f());
            this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0F, 1.4F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.HOSTILE, 1.6F, 1.8F);
            return;
        }
        if (CLUSTER_MORTAR.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 1.4F, 0.6F);
            return;
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        this.entityData.set(DATA_SKILL_TICK, actionTick);
        if (FIRE.equals(action) && actionTick == 10) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.6F, 0.55F);
            this.launchProjectile(target, WeaponMount.TANK_CANNON, WeaponMount.TANK_CANNON.muzzle(), false, false, 2.30, 0.0F);
        } else if (RAILGUN.equals(action)) {
            this.tickRailgun(actionTick, target);
        } else if (CLUSTER_MORTAR.equals(action) && actionTick == MORTAR_FIRE_TICK) {
            this.fireClusterMortar(target);
        }
    }

    private void tickRailgun(int actionTick, LivingEntity target) {
        if (actionTick < RAIL_LOCK_TICK) {
            this.entityData.set(DATA_RAIL_END, target.getBoundingBox().getCenter().toVector3f());
            return;
        }
        if (actionTick == RAIL_LOCK_TICK) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.HOSTILE, 2.0F, 2.0F);
            return;
        }
        if (actionTick != RAIL_FIRE_TICK) {
            return;
        }
        Vec3 start = this.getCannonMuzzle(1.0F);
        if (start == null) {
            start = this.position().add(0.0, 1.6, 0.0);
        }
        Vec3 direction = this.getRailEnd().subtract(start).normalize();
        Vec3 end = this.level().clip(new ClipContext(start, start.add(direction.scale(RAIL_RANGE)),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
        this.entityData.set(DATA_RAIL_START, start.toVector3f());
        this.entityData.set(DATA_RAIL_END, end.toVector3f());
        float damage = this.skillDamage(MechafaunaConfig.MECHANICAL_TANK_RAILGUN_MULTIPLIER);
        int hits = 0;
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(RAIL_HIT_RADIUS + 1.0), this::canSkillHit)) {
            if (victim.getBoundingBox().inflate(RAIL_HIT_RADIUS).clip(start, end).isEmpty()) {
                continue;
            }
            if (victim.hurt(this.damageSources().mobAttack(this), damage)) {
                victim.knockback(0.7, -direction.x, -direction.z);
                hits++;
            }
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.HOSTILE, 1.4F, 1.5F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 2.0F, 0.5F);
        this.level().playSound(null, end.x, end.y, end.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 1.2F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ExplosionParticleOptions(1.4F), end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 30, 0.5, 0.5, 0.5, 0.6);
            serverLevel.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, start.x, start.y, start.z, 8, 0.3, 0.3, 0.3, 0.05);
        }
        LOGGER.debug("Mechanical tank railgun fired tank={} target={} start={} end={} damage={} hits={}",
            this.getId(), target.getId(), start, end, damage, hits);
    }

    private void fireClusterMortar(LivingEntity target) {
        Vec3 muzzle = this.getCannonMuzzle(1.0F);
        if (muzzle == null) {
            muzzle = this.position().add(0.0, 2.0, 0.0);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.6F, 0.5F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 2.0F, 0.5F);
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 up = this.getCannonAimOverride(1.0F).map(point -> point.subtract(this.position().add(0.0, 2.0, 0.0)).normalize()).orElse(new Vec3(0.0, 1.0, 0.0));
            serverLevel.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
            for (int i = 0; i < 10; i++) {
                Vec3 trail = muzzle.add(up.scale(i * 0.6));
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, trail.x, trail.y, trail.z, 1, 0.08, 0.08, 0.08, 0.01);
                serverLevel.sendParticles(ParticleTypes.FLAME, trail.x, trail.y, trail.z, 0, up.x, up.y, up.z, 0.6);
            }
        }
        int shells = 6;
        float damage = this.skillDamage(MechafaunaConfig.MECHANICAL_TANK_CLUSTER_MORTAR_MULTIPLIER);
        Vec3 center = target.position();
        for (int i = 0; i < shells; i++) {
            Vec3 point = center;
            if (i > 0) {
                double angle = (i - 1) * Math.PI * 2.0 / (shells - 1) + this.random.nextDouble() * 0.8;
                double distance = 1.8 + this.random.nextDouble() * 3.2;
                point = center.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
            }
            MechStrikeZone.spawn(this, point, MechStrikeZone.STYLE_MORTAR, 24 + i * 3, 2.2F, damage, 0.45);
        }
        LOGGER.debug("Mechanical tank cluster mortar fired tank={} target={} shells={} damage={}", this.getId(), target.getId(), shells, damage);
    }
}
