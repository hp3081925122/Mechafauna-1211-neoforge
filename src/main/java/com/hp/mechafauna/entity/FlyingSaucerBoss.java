package com.hp.mechafauna.entity;

import com.hp.mechafauna.MechafaunaConfig;
import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.hp.mechafauna.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public final class FlyingSaucerBoss extends AnimatedHostileMob {
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlay("animation.flying_saucer_boss.death");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float BODY_DAMAGE_MULTIPLIER = 0.20F;
    private static final float DESTROYED_WEAPON_DAMAGE_INCREASE = 0.20F;
    private static final float MAX_BODY_DAMAGE_MULTIPLIER = 1.0F;
    private static final float PART_DESTROY_BODY_DAMAGE = 0.10F;
    private static final double BASE_HEALTH = 300.0;
    /** 部件血量占本体最大生命的比例，300 血时每个部件 80。 */
    private static final float WEAPON_HEALTH_RATIO = 80.0F / 300.0F;
    private static final int WEAPON_PART_OFFSET = 1;
    private static final float BODY_WIDTH = 6.6F;
    private static final float BODY_HEIGHT = 2.4F;
    private static final double BODY_CENTER_Y = 3.6;
    private static final double TARGET_RANGE = 72.0;
    private static final int TARGET_UPDATE_INTERVAL = 10;
    private static final int[] GATLING_SHOT_TICKS = {4, 6, 8, 10, 12, 14, 16, 18, 20, 22};
    private static final int[] MISSILE_SHOT_TICKS = {8, 12, 16, 21, 25, 30};
    private static final int GATLING_COOLDOWN = 46;
    private static final int MISSILE_COOLDOWN = 100;
    private static final List<EntityDataAccessor<Float>> DATA_PART_HEALTH = List.of(createFloatAccessor(), createFloatAccessor(), createFloatAccessor(), createFloatAccessor());
    private static final List<EntityDataAccessor<Float>> DATA_WEAPON_YAW = List.of(createFloatAccessor(), createFloatAccessor(), createFloatAccessor(), createFloatAccessor());
    private static final List<EntityDataAccessor<Float>> DATA_WEAPON_PITCH = List.of(createFloatAccessor(), createFloatAccessor(), createFloatAccessor(), createFloatAccessor());
    // 下射激光技能时间轴与数值
    public static final int LASER_CHARGE_TICKS = 30;
    public static final int LASER_FIRE_TICKS = 60;
    public static final int LASER_FADE_TICKS = 10;
    public static final int WAVE_INTERVAL = 12;
    public static final int WAVE_LIFETIME = 30;
    public static final int WAVE_COUNT = LASER_FIRE_TICKS / WAVE_INTERVAL;
    public static final float WAVE_MAX_RADIUS = 21.0F;
    private static final int LASER_TOTAL_TICKS = LASER_CHARGE_TICKS + LASER_FIRE_TICKS + WAVE_LIFETIME;
    private static final int LASER_COOLDOWN = 240;
    static final double LASER_RANGE = 64.0;
    private static final double LASER_ALIGN_DISTANCE = 2.5;
    private static final double LASER_ALIGN_HEIGHT = 3.0;
    static final double LASER_BEAM_RADIUS = 1.3;
    static final float LASER_BEAM_DAMAGE = 8.0F;
    static final float WAVE_DAMAGE = 6.0F;
    private static final EntityDataAccessor<Integer> DATA_LASER_TICK = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_LASER_IMPACT = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.VECTOR3);
    private final Set<Long> waveHits = new HashSet<>();
    private int laserCooldown = 120;
    // 召唤传送光束时间轴与阶段血量阈值
    public static final int SUMMON_NONE = 0;
    public static final int SUMMON_TANK = 1;
    public static final int SUMMON_MECH = 2;
    public static final int SUMMON_CHARGE_TICKS = 20;
    public static final int SUMMON_SPAWN_TICK = 50;
    public static final int SUMMON_FADE_TICKS = 14;
    public static final int SUMMON_TOTAL_TICKS = 90;
    private static final float TANK_SUMMON_HEALTH_RATIO = 0.75F;
    private static final float TANK_SUMMON_HEALTH_FLOOR = 0.74F;
    private static final float MECH_SUMMON_HEALTH_RATIO = 0.25F;
    private static final float MECH_SUMMON_HEALTH_FLOOR = 0.24F;
    private static final EntityDataAccessor<Integer> DATA_SUMMON_TICK = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SUMMON_KIND = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_SUMMON_POINT = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.VECTOR3);
    private boolean tankSummoned;
    private boolean mechSummoned;
    public static final int SKILL_NONE = 0;
    public static final int SKILL_GRAVITY_WELL = 1;
    public static final int SKILL_HALO_SWEEP = 2;
    public static final int SKILL_PLASMA_RAIN = 3;
    public static final int GRAVITY_DROP_TICKS = 20;
    public static final int GRAVITY_PULL_TICKS = 70;
    public static final int GRAVITY_COLLAPSE_TICK = GRAVITY_DROP_TICKS + GRAVITY_PULL_TICKS;
    public static final int GRAVITY_AFTERMATH_TICKS = 22;
    public static final double GRAVITY_PULL_RADIUS = 13.0;
    public static final double GRAVITY_CORE_RADIUS = 2.5;
    public static final double GRAVITY_COLLAPSE_RADIUS = 7.0;
    public static final int HALO_CHARGE_TICKS = 30;
    public static final int HALO_FIRE_TICKS = 80;
    public static final int HALO_BEAMS = 6;
    public static final float HALO_START_RADIUS = 3.0F;
    public static final float HALO_END_RADIUS = 22.0F;
    public static final float HALO_SPIN = 0.045F;
    public static final double HALO_RIM_RADIUS = 4.6;
    private static final double HALO_HIT_RADIUS = 1.5;
    private static final int HALO_HIT_INTERVAL = 10;
    public static final int PLASMA_CHARGE_TICKS = 24;
    public static final int PLASMA_SHOTS = 9;
    public static final int PLASMA_INTERVAL = 5;
    private static final int PLASMA_IMPACT_DELAY = 20;
    private static final float MIN_RAGE_COOLDOWN = 0.4F;
    private static final EntityDataAccessor<Integer> DATA_SKILL = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKILL_TICK = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_SKILL_POINT = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_SKILL_ORIGIN = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.VECTOR3);
    private final int[] skillCooldowns = new int[4];
    private final Map<Integer, Integer> haloHitTicks = new HashMap<>();
    private int skillInterval = 100;
    private boolean crashHandled;
    // 同步 Boss 血条 ID，客户端据此在血条下方绘制武器血条
    private static final EntityDataAccessor<Optional<UUID>> DATA_BOSS_BAR_ID = SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.OPTIONAL_UUID);
    private final SaucerDamagePart[] parts = new SaucerDamagePart[WEAPON_PART_OFFSET + FlyingSaucerWeapon.values().length];
    private final LivingEntity[] weaponTargets = new LivingEntity[FlyingSaucerWeapon.values().length];
    private final LivingEntity[] burstTargets = new LivingEntity[FlyingSaucerWeapon.values().length];
    private final int[] nextBurstTick = new int[FlyingSaucerWeapon.values().length];
    private final int[] burstStartTick = new int[FlyingSaucerWeapon.values().length];
    private final int[] nextShotTick = new int[FlyingSaucerWeapon.values().length];
    private final int[] burstShotIndex = new int[FlyingSaucerWeapon.values().length];
    private final boolean[] burstActive = new boolean[FlyingSaucerWeapon.values().length];
    private final ServerBossEvent bossEvent = new ServerBossEvent(
        net.minecraft.network.chat.Component.translatable("entity.mechafauna.flying_saucer_boss"),
        BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public FlyingSaucerBoss(EntityType<? extends FlyingSaucerBoss> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.noCulling = true;
        this.parts[0] = new SaucerDamagePart(this, -1, BODY_WIDTH, BODY_HEIGHT);
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            this.parts[WEAPON_PART_OFFSET + weapon.ordinal()] = new SaucerDamagePart(this, weapon.ordinal(), 2.4F, 2.0F);
        }
        this.setId(ENTITY_COUNTER.getAndAdd(this.parts.length + 1) + 1);
        this.updatePartPositions();
        if (!level.isClientSide) {
            this.entityData.set(DATA_BOSS_BAR_ID, Optional.of(this.bossEvent.getId()));
            AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(MechafaunaConfig.flyingSaucerMaxHealth());
                this.setHealth(this.getMaxHealth());
            }
            AttributeInstance attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.setBaseValue(MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_ATTACK_DAMAGE));
            }
            this.fillWeaponHealth();
        }
    }

    private static EntityDataAccessor<Float> createFloatAccessor() {
        return SynchedEntityData.defineId(FlyingSaucerBoss.class, EntityDataSerializers.FLOAT);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(BASE_HEALTH, 0.20, 8.0, TARGET_RANGE, 0.75).add(Attributes.ATTACK_DAMAGE, 10.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !AnimatedHostileMob.isIgnoredByOtherMechs(this.mob.getLastHurtByMob()) && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
            target -> target instanceof Enemy && !(target instanceof AnimatedHostileMob)));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        for (int i = 0; i < FlyingSaucerWeapon.values().length; i++) {
            this.entityData.define(DATA_PART_HEALTH.get(i), weaponHealthFor(BASE_HEALTH));
            this.entityData.define(DATA_WEAPON_YAW.get(i), 0.0F);
            this.entityData.define(DATA_WEAPON_PITCH.get(i), 0.0F);
        }
        // 下射激光的同步数据
        this.entityData.define(DATA_LASER_TICK, 0);
        this.entityData.define(DATA_LASER_IMPACT, new Vector3f());
        this.entityData.define(DATA_BOSS_BAR_ID, Optional.empty());
        // 召唤传送光束的同步数据
        this.entityData.define(DATA_SUMMON_TICK, 0);
        this.entityData.define(DATA_SUMMON_KIND, SUMMON_NONE);
        this.entityData.define(DATA_SUMMON_POINT, new Vector3f());
        this.entityData.define(DATA_SKILL, SKILL_NONE);
        this.entityData.define(DATA_SKILL_TICK, 0);
        this.entityData.define(DATA_SKILL_POINT, new Vector3f());
        this.entityData.define(DATA_SKILL_ORIGIN, new Vector3f());
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.parts != null) {
            for (int i = 0; i < this.parts.length; i++) {
                if (this.parts[i] != null) {
                    this.parts[i].setId(id + i + 1);
                }
            }
        }
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (this.parts != null) {
            this.updatePartPositions();
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        // 实体包围盒与主体受击箱重合，不再包住下方空地
        double half = BODY_WIDTH * 0.5;
        double minY = this.getY() + BODY_CENTER_Y - BODY_HEIGHT * 0.5;
        return new AABB(
            this.getX() - half, minY, this.getZ() - half,
            this.getX() + half, minY + BODY_HEIGHT, this.getZ() + half);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // 碰撞箱缩小后仍按原来的外形距离渲染
        double size = (10.4 + 5.6 + 10.4) / 3.0 * 64.0;
        return distance < size * size;
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
        return this.parts;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return this.getSummonTick() > 0 || super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        int destroyedWeapons = this.destroyedWeaponCount();
        float damageMultiplier = Math.min(MAX_BODY_DAMAGE_MULTIPLIER,
            BODY_DAMAGE_MULTIPLIER + destroyedWeapons * DESTROYED_WEAPON_DAMAGE_INCREASE);
        return super.hurt(source, amount * damageMultiplier);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float healthBefore = this.getHealth();
        super.actuallyHurt(source, amount);
        this.keepSummonThreshold(healthBefore);
    }

    private void keepSummonThreshold(float healthBefore) {
        float maxHealth = this.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return;
        }
        float floor = 0.0F;
        if (healthBefore > maxHealth * TANK_SUMMON_HEALTH_RATIO) {
            floor = maxHealth * TANK_SUMMON_HEALTH_FLOOR;
        } else if (healthBefore > maxHealth * MECH_SUMMON_HEALTH_RATIO) {
            floor = maxHealth * MECH_SUMMON_HEALTH_FLOOR;
        }
        if (floor > 0.0F && this.getHealth() < floor) {
            this.setHealth(floor);
            LOGGER.debug("Saucer summon threshold held boss={} healthBefore={} floor={} healthAfter={}",
                this.getId(), healthBefore, floor, this.getHealth());
        }
    }

    public boolean hurt(SaucerDamagePart part, DamageSource source, float amount) {
        if (part.weaponIndex() < 0) {
            return this.hurt(source, amount);
        }
        if (this.isInvulnerableTo(source) || !this.isAlive() || !Float.isFinite(amount) || amount <= 0.0F
            || this.isWeaponDestroyed(part.weaponIndex())) {
            return false;
        }
        int index = part.weaponIndex();
        float remaining = Math.max(0.0F, this.getWeaponHealth(index) - amount);
        this.entityData.set(DATA_PART_HEALTH.get(index), remaining);
        this.level().broadcastEntityEvent(this, (byte)2);
        if (remaining == 0.0F) {
            this.clearWeaponTarget(index);
            float healthBefore = this.getHealth();
            this.setHealth(healthBefore - this.getMaxHealth() * PART_DESTROY_BODY_DAMAGE);
            this.keepSummonThreshold(healthBefore);
            LOGGER.info("Saucer weapon destroyed boss={} weapon={} bodyHealth={}/{}", this.getId(), FlyingSaucerWeapon.values()[index], this.getHealth(), this.getMaxHealth());
            // 部件被摧毁时的爆炸特效
            Vec3 partPos = part.position();
            this.level().playSound(null, partPos.x, partPos.y, partPos.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 2.5F, 0.7F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, partPos.x, partPos.y, partPos.z, 3, 0.4, 0.4, 0.4, 0.0);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, partPos.x, partPos.y, partPos.z, 8, 0.6, 0.6, 0.6, 0.05);
                serverLevel.sendParticles(ParticleTypes.LAVA, partPos.x, partPos.y, partPos.z, 6, 0.5, 0.5, 0.5, 0.0);
            }
            if (this.isDeadOrDying()) {
                this.die(source);
            }
        }
        return true;
    }

    public Optional<UUID> getBossBarId() {
        return this.entityData.get(DATA_BOSS_BAR_ID);
    }

    public static float weaponHealthFor(double bossMaxHealth) {
        return (float)bossMaxHealth * WEAPON_HEALTH_RATIO;
    }

    public float getWeaponMaxHealth() {
        return weaponHealthFor(this.getMaxHealth());
    }

    public float getWeaponHealth(int index) {
        return this.entityData.get(DATA_PART_HEALTH.get(index));
    }

    public boolean isWeaponDestroyed(int index) {
        return index >= 0 && index < FlyingSaucerWeapon.values().length && this.getWeaponHealth(index) <= 0.0F;
    }

    public float getWeaponYawDegrees(FlyingSaucerWeapon weapon) {
        return this.entityData.get(DATA_WEAPON_YAW.get(weapon.ordinal()));
    }

    public float getWeaponPitchDegrees(FlyingSaucerWeapon weapon) {
        return this.entityData.get(DATA_WEAPON_PITCH.get(weapon.ordinal()));
    }

    @Override
    public void tick() {
        // 部件不参与世界 tick，需要手动记录上一刻位置，否则判定箱插值会飞到原点附近
        for (SaucerDamagePart part : this.parts) {
            part.setOldPosAndRot();
        }
        super.tick();
        this.updatePartPositions();
        if (this.level().isClientSide) {
            return;
        }
        this.bossEvent.setProgress(this.getMaxHealth() <= 0.0F ? 0.0F : this.getHealth() / this.getMaxHealth());
        if (this.isDeadOrDying()) {
            this.tickCrashing();
            return;
        }
        if (this.tickCount % TARGET_UPDATE_INTERVAL == 0) {
            this.updateWeaponTargets();
        }
        this.updateWeapons();
        this.updateSummon();
        this.updateLaser();
        this.updateSkills();
        this.emitWreckageSmoke();
        this.updateFlight();
    }

    private void tickCrashing() {
        if (!this.crashHandled) {
            this.crashHandled = true;
            for (int index = 0; index < FlyingSaucerWeapon.values().length; index++) {
                this.clearWeaponTarget(index);
            }
            this.entityData.set(DATA_LASER_TICK, 0);
            this.waveHits.clear();
            if (this.getSkillId() != SKILL_NONE) {
                this.endSkill(this.getSkillId());
            }
            this.entityData.set(DATA_SUMMON_TICK, 0);
            this.entityData.set(DATA_SUMMON_KIND, SUMMON_NONE);
            this.setTarget(null);
            this.getNavigation().stop();
            LOGGER.debug("Saucer crash started boss={} deathTime={} position={}", this.getId(), this.deathTime, this.position());
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
        this.emitWreckageSmoke();
    }

    private void updateWeaponTargets() {
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class,
            this.getBoundingBox().inflate(TARGET_RANGE), this::canTarget);
        Matrix4d model = WeaponMount.modelToWorld(this.position(), this.yBodyRot, this.getModelScale());
        WeaponPose pose = WeaponPose.forModel("flying_saucer_boss");
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            int index = weapon.ordinal();
            if (this.isWeaponDestroyed(index)) {
                this.clearWeaponTarget(index);
                continue;
            }
            Matrix4d parent = pose.parentTransform(weapon.mount(), model, null, 0.0);
            Vec3 pivot = WeaponMount.transform(parent, weapon.mount().yawPivot());
            LivingEntity previous = this.weaponTargets[index];
            LivingEntity next = candidates.stream()
                .filter(target -> target.distanceToSqr(pivot) <= TARGET_RANGE * TARGET_RANGE && this.hasLineOfSight(target))
                .min(Comparator.comparingDouble(target -> target.distanceToSqr(pivot)))
                .orElse(null);
            this.weaponTargets[index] = next;
            if (previous != next) {
                LOGGER.debug("Saucer weapon target changed boss={} weapon={} target={}", this.getId(), weapon, next == null ? -1 : next.getId());
            }
        }
    }

    private boolean canTarget(LivingEntity target) {
        return target != this && target.isAlive() && this.canAttack(target) && !this.isAlliedTo(target)
            && (target instanceof Player || target instanceof MilitaryDrone
                || target instanceof Mob mob && mob instanceof Enemy && !(mob instanceof AnimatedHostileMob));
    }

    private void updateWeapons() {
        WeaponPose pose = WeaponPose.forModel("flying_saucer_boss");
        Matrix4d model = WeaponMount.modelToWorld(this.position(), this.yBodyRot, this.getModelScale());
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            int index = weapon.ordinal();
            if (this.isWeaponDestroyed(index)) {
                this.cancelBurst(index);
                continue;
            }
            LivingEntity target = this.burstActive[index] ? this.burstTargets[index] : this.weaponTargets[index];
            if (target == null || !target.isAlive() || !this.canTarget(target)
                || this.distanceToSqr(target) > TARGET_RANGE * TARGET_RANGE || !this.hasLineOfSight(target)) {
                if (this.burstActive[index]) {
                    this.cancelBurst(index);
                    this.nextBurstTick[index] = this.tickCount + TARGET_UPDATE_INTERVAL;
                }
                continue;
            }
            WeaponMount mount = weapon.mount();
            Matrix4d parent = pose.parentTransform(mount, model, null, 0.0);
            Vec3 muzzlePivot = pose.bonePivot(weapon.muzzleBone(0));
            RadialWeaponAim.Angles desired = RadialWeaponAim.aim(parent, mount.yawPivot(), mount.pitchPivot(), muzzlePivot,
                target.getBoundingBox().getCenter());
            float yaw = net.minecraft.util.Mth.approachDegrees(this.getWeaponYawDegrees(weapon), desired.yawDegrees(), 8.0F);
            float pitch = net.minecraft.util.Mth.approachDegrees(this.getWeaponPitchDegrees(weapon), desired.pitchDegrees(), 5.0F);
            this.entityData.set(DATA_WEAPON_YAW.get(index), yaw);
            this.entityData.set(DATA_WEAPON_PITCH.get(index), pitch);
            if (Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - desired.yawDegrees())) > 4.0F
                || Math.abs(net.minecraft.util.Mth.wrapDegrees(pitch - desired.pitchDegrees())) > 4.0F) {
                continue;
            }
            if (!this.burstActive[index]) {
                if (this.tickCount < this.nextBurstTick[index]) {
                    continue;
                }
                this.burstActive[index] = true;
                this.burstTargets[index] = target;
                this.burstStartTick[index] = this.tickCount;
                this.burstShotIndex[index] = 0;
                int[] schedule = this.shotSchedule(weapon);
                this.nextShotTick[index] = this.tickCount + schedule[0];
                LOGGER.debug("Saucer weapon salvo started boss={} weapon={} target={} shots={}", this.getId(), weapon, target.getId(), weapon.isMissile() ? 6 : 60);
                continue;
            }
            if (this.tickCount < this.nextShotTick[index]) {
                continue;
            }
            int shot = this.burstShotIndex[index];
            if (weapon.isMissile()) {
                Matrix4d aimed = this.aimedWeaponTransform(pose, parent, mount, yaw, pitch, weapon);
                Vec3 muzzle = WeaponMount.transform(aimed, pose.bonePivot(weapon.muzzleBone(shot)));
                this.fireMissile(weapon, target, shot, muzzle);
            } else {
                this.fireMachineGunVolley(pose, parent, mount, weapon, target, yaw, pitch);
            }
            this.burstShotIndex[index]++;
            int[] schedule = this.shotSchedule(weapon);
            if (this.burstShotIndex[index] >= schedule.length) {
                int cooldownReadyTick = this.burstStartTick[index] + (weapon.isMissile() ? MISSILE_COOLDOWN : GATLING_COOLDOWN);
                this.cancelBurst(index);
                this.nextBurstTick[index] = Math.max(this.tickCount, cooldownReadyTick);
            } else {
                int previousShotTick = schedule[shot];
                int nextScheduledShotTick = schedule[shot + 1];
                this.nextShotTick[index] = this.tickCount + nextScheduledShotTick - previousShotTick;
            }
        }
    }

    private int[] shotSchedule(FlyingSaucerWeapon weapon) {
        return weapon.isMissile() ? MISSILE_SHOT_TICKS : GATLING_SHOT_TICKS;
    }

    private Matrix4d aimedWeaponTransform(WeaponPose pose, Matrix4d parent, WeaponMount mount,
                                          float yaw, float pitch, FlyingSaucerWeapon weapon) {
        Matrix4d aimed = RadialWeaponAim.transform(parent, mount.yawPivot(), mount.pitchPivot(), yaw, pitch);
        if (weapon.barrelBone() != null) {
            Vec3 barrelPivot = pose.bonePivot(weapon.barrelBone());
            double spin = this.tickCount * 0.48;
            aimed.translate(barrelPivot.x, barrelPivot.y, barrelPivot.z).rotateX(spin)
                .translate(-barrelPivot.x, -barrelPivot.y, -barrelPivot.z);
        }
        return aimed;
    }

    private void fireMachineGunVolley(WeaponPose pose, Matrix4d parent, WeaponMount mount, FlyingSaucerWeapon weapon,
                                      LivingEntity target, float yaw, float pitch) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 forward = targetCenter.subtract(this.getBoundingBox().getCenter()).normalize();
        if (forward.lengthSqr() < 1.0E-6) {
            forward = this.getLookAngle().normalize();
        }
        Vec3 reference = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        double firstAngle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 0.8 + this.random.nextDouble() * 1.4;
        Matrix4d aimed = this.aimedWeaponTransform(pose, parent, mount, yaw, pitch, weapon);
        for (int muzzleIndex = 0; muzzleIndex < 6; muzzleIndex++) {
            double angle = firstAngle + muzzleIndex * Math.PI / 3.0;
            Vec3 muzzle = WeaponMount.transform(aimed, pose.bonePivot(weapon.muzzleBone(muzzleIndex)));
            Vec3 aimPoint = targetCenter.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            this.fireProjectile(weapon, target, muzzle, aimPoint, muzzleIndex);
        }
    }

    private void fireMissile(FlyingSaucerWeapon weapon, LivingEntity target, int shot, Vec3 muzzle) {
        this.fireProjectile(weapon, target, muzzle, target.getBoundingBox().getCenter(), shot);
    }

    private void fireProjectile(FlyingSaucerWeapon weapon, LivingEntity target, Vec3 muzzle, Vec3 aimPoint, int shot) {
        Vec3 direction = aimPoint.subtract(muzzle).normalize();
        MechProjectile projectile = new MechProjectile(ModEntities.MECH_PROJECTILE.get(), this.level(), this,
            weapon.isMissile(), !weapon.isMissile(), false, weapon.isMissile() ? 5.0F : 2.0F);
        projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
        projectile.shoot(direction.x, direction.y, direction.z, weapon.isMissile() ? 1.8F : 1.45F, 0.0F);
        this.level().addFreshEntity(projectile);
        if (weapon.isMissile()) {
            LOGGER.debug("Saucer missile launch boss={} weapon={} shot={} bone={} spawn=({}, {}, {}) target=({}, {}, {})",
                this.getId(), weapon, shot, weapon.muzzleBone(shot), muzzle.x, muzzle.y, muzzle.z,
                target.getX(), target.getY(), target.getZ());
        }
    }

    private void updateLaser() {
        int tick = this.getLaserTick();
        // 技能空闲时判断是否起手
        if (tick == 0) {
            if (this.laserCooldown > 0) {
                this.laserCooldown--;
                return;
            }
            // 召唤光束进行中或待触发时不释放下射激光
            if (this.getSummonTick() != 0 || this.pendingSummonKind() != SUMMON_NONE || this.getSkillId() != SKILL_NONE) {
                return;
            }
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive() || !this.canTarget(target) || !this.isAboveLaserTarget(target)) {
                return;
            }
            this.entityData.set(DATA_LASER_TICK, 1);
            this.waveHits.clear();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 4.0F, 0.6F);
            LOGGER.debug("Saucer ground laser charging boss={} target={}", this.getId(), target.getId());
            return;
        }
        // 每刻重新计算激光落点
        Vec3 origin = this.getLaserOrigin();
        var hit = this.level().clip(new ClipContext(origin, origin.subtract(0.0, LASER_RANGE, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        Vec3 impact = hit.getLocation();
        this.entityData.set(DATA_LASER_IMPACT, new Vector3f((float)impact.x, (float)impact.y, (float)impact.z));
        int fireTick = tick - LASER_CHARGE_TICKS;
        if (fireTick == 0) {
            this.level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE, 5.0F, 0.55F);
            LOGGER.debug("Saucer ground laser fired boss={} impact=({}, {}, {})", this.getId(), impact.x, impact.y, impact.z);
        }
        // 光柱持续灼烧
        if (fireTick >= 0 && fireTick < LASER_FIRE_TICKS && fireTick % 5 == 0) {
            AABB column = new AABB(origin.x - LASER_BEAM_RADIUS, impact.y - 0.5, origin.z - LASER_BEAM_RADIUS,
                origin.x + LASER_BEAM_RADIUS, origin.y, origin.z + LASER_BEAM_RADIUS);
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, column, this::canTarget)) {
                if (victim.hurt(this.damageSources().mobAttack(this), this.withPercentDamage(victim, LASER_BEAM_DAMAGE))) {
                    victim.setSecondsOnFire(3);
                }
            }
        }
        // 每隔固定时间放出一圈波纹
        if (fireTick >= 0 && fireTick < LASER_FIRE_TICKS && fireTick % WAVE_INTERVAL == 0) {
            this.level().playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 2.2F, 0.5F + fireTick / (float)LASER_FIRE_TICKS * 0.3F);
            if (this.level() instanceof ServerLevel serverLevel) {
                BlockState ground = this.level().getBlockState(BlockPos.containing(impact.x, impact.y - 0.1, impact.z));
                if (!ground.isAir()) {
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        impact.x, impact.y + 0.1, impact.z, 24, 1.2, 0.1, 1.2, 0.25);
                }
            }
        }
        // 波纹扫过地面时造成一次伤害和击退
        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            int age = fireTick - wave * WAVE_INTERVAL;
            if (age < 1 || age >= WAVE_LIFETIME) {
                continue;
            }
            float inner = waveRadius(age - 1);
            float outer = waveRadius(age);
            AABB area = new AABB(impact, impact).inflate(outer + 0.5, 2.5, outer + 0.5);
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, area, this::canTarget)) {
                double dx = victim.getX() - impact.x;
                double dz = victim.getZ() - impact.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                long key = ((long)victim.getId() << 8) | wave;
                if (distance < inner - 0.6 || distance > outer + 0.6 || Math.abs(victim.getY() - impact.y) > 2.5
                    || !this.waveHits.add(key)) {
                    continue;
                }
                if (victim.hurt(this.damageSources().mobAttack(this), this.withPercentDamage(victim, WAVE_DAMAGE))) {
                    victim.knockback(1.1, -dx, -dz);
                    victim.hurtMarked = true;
                }
            }
        }
        if (tick >= LASER_TOTAL_TICKS || !this.isAlive()) {
            this.entityData.set(DATA_LASER_TICK, 0);
            this.laserCooldown = Math.round(LASER_COOLDOWN * this.getRageCooldownFactor());
            this.waveHits.clear();
            LOGGER.debug("Saucer ground laser finished boss={}", this.getId());
            return;
        }
        this.entityData.set(DATA_LASER_TICK, tick + 1);
    }

    private int pendingSummonKind() {
        // 按剩余血量比例决定下一次召唤，先坦克后机甲
        float ratio = 0.0F;
        if (this.getMaxHealth() > 0.0F) {
            ratio = this.getHealth() / this.getMaxHealth();
        }
        if (ratio <= TANK_SUMMON_HEALTH_RATIO && !this.tankSummoned) {
            return SUMMON_TANK;
        }
        if (ratio <= MECH_SUMMON_HEALTH_RATIO && !this.mechSummoned) {
            return SUMMON_MECH;
        }
        return SUMMON_NONE;
    }

    private void updateSummon() {
        int tick = this.getSummonTick();
        // 死亡时立即中断光束
        if (this.isDeadOrDying()) {
            if (tick != 0) {
                this.entityData.set(DATA_SUMMON_TICK, 0);
                this.entityData.set(DATA_SUMMON_KIND, SUMMON_NONE);
            }
            return;
        }
        // 空闲且激光未释放时，按血量阈值起手
        if (tick == 0) {
            int kind = this.pendingSummonKind();
            if (kind == SUMMON_NONE || this.getLaserTick() != 0 || this.getSkillId() != SKILL_NONE) {
                return;
            }
            Vec3 point = this.findSummonPoint();
            this.entityData.set(DATA_SUMMON_POINT, new Vector3f((float)point.x, (float)point.y, (float)point.z));
            this.entityData.set(DATA_SUMMON_KIND, kind);
            this.entityData.set(DATA_SUMMON_TICK, 1);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 4.0F, 0.7F);
            LOGGER.debug("Saucer summon beam started boss={} kind={} point=({}, {}, {}) health={}/{}",
                this.getId(), kind, point.x, point.y, point.z, this.getHealth(), this.getMaxHealth());
            return;
        }
        // 光柱展开的瞬间播放传送音效
        if (tick == SUMMON_CHARGE_TICKS) {
            Vec3 point = this.getSummonPoint();
            this.level().playSound(null, point.x, point.y, point.z, SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 4.0F, 1.4F);
        }
        // 光束中段把增援实体传送到地面
        if (tick == SUMMON_SPAWN_TICK) {
            this.spawnReinforcement(this.getSummonKind(), this.getSummonPoint());
        }
        if (tick >= SUMMON_TOTAL_TICKS) {
            this.entityData.set(DATA_SUMMON_TICK, 0);
            this.entityData.set(DATA_SUMMON_KIND, SUMMON_NONE);
            LOGGER.debug("Saucer summon beam finished boss={}", this.getId());
            return;
        }
        this.entityData.set(DATA_SUMMON_TICK, tick + 1);
    }

    private Vec3 findSummonPoint() {
        // 从光束发射点向下找地面，找不到时退回目标脚下
        Vec3 origin = this.getLaserOrigin();
        var hit = this.level().clip(new ClipContext(origin, origin.subtract(0.0, LASER_RANGE, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            return hit.getLocation();
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            return target.position();
        }
        return this.position();
    }

    private void spawnReinforcement(int kind, Vec3 point) {
        // 标记阶段已完成，失败也不再重复触发
        Mob mob;
        if (kind == SUMMON_TANK) {
            this.tankSummoned = true;
            mob = ModEntities.MECHANICAL_TANK.get().create(this.level());
        } else {
            this.mechSummoned = true;
            mob = ModEntities.HEAVY_MECH.get().create(this.level());
        }
        if (mob == null) {
            LOGGER.warn("Saucer summon failed to create entity boss={} kind={}", this.getId(), kind);
            return;
        }
        // 落地后面朝当前目标
        LivingEntity target = this.getTarget();
        float yaw = this.random.nextFloat() * 360.0F;
        if (target != null && target.isAlive()) {
            yaw = (float)(Mth.atan2(target.getZ() - point.z, target.getX() - point.x) * Mth.RAD_TO_DEG) - 90.0F;
        }
        mob.moveTo(point.x, point.y, point.z, yaw, 0.0F);
        mob.setYHeadRot(yaw);
        mob.yBodyRot = yaw;
        mob.setPersistenceRequired();
        if (target != null && target.isAlive() && !AnimatedHostileMob.isIgnoredByOtherMechs(target)) {
            mob.setTarget(target);
        }
        boolean added = this.level().addFreshEntity(mob);
        // 落点的传送音效与少量辅助闪光
        this.level().playSound(null, point.x, point.y, point.z, SoundEvents.ENDERMAN_TELEPORT,
            SoundSource.HOSTILE, 3.0F, 0.6F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y + mob.getBbHeight() * 0.5, point.z,
                16, mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.4, mob.getBbWidth() * 0.4, 0.04);
        }
        LOGGER.info("Saucer summoned reinforcement boss={} kind={} entity={} added={} target={}",
            this.getId(), kind, mob.getId(), added, target == null ? -1 : target.getId());
    }

    public int getSummonTick() {
        return this.entityData.get(DATA_SUMMON_TICK);
    }

    public int getSummonKind() {
        return this.entityData.get(DATA_SUMMON_KIND);
    }

    public Vec3 getSummonPoint() {
        Vector3f point = this.entityData.get(DATA_SUMMON_POINT);
        return new Vec3(point.x, point.y, point.z);
    }

    public static float summonHeight(int kind) {
        // 渲染扫描环用的增援实体大致高度
        if (kind == SUMMON_MECH) {
            return 3.6F;
        }
        return 2.8F;
    }

    public int getLaserTick() {
        return this.entityData.get(DATA_LASER_TICK);
    }

    public Vec3 getLaserImpact() {
        Vector3f impact = this.entityData.get(DATA_LASER_IMPACT);
        return new Vec3(impact.x, impact.y, impact.z);
    }

    public Vec3 getLaserOrigin() {
        return this.getLaserOrigin(this.position());
    }

    public Vec3 getLaserOrigin(Vec3 position) {
        // 发射点取模型能量核心下方的 beam_origin 骨骼
        Matrix4d model = WeaponMount.modelToWorld(position, this.yBodyRot, this.getModelScale());
        WeaponPose pose = WeaponPose.forModel("flying_saucer_boss");
        return WeaponMount.transform(model, pose.transformBonePoint("beam_origin", pose.bonePivot("beam_origin")));
    }

    private boolean isAboveLaserTarget(LivingEntity target) {
        double horizontal = Math.hypot(target.getX() - this.getX(), target.getZ() - this.getZ());
        double hoverY = target.getY() + target.getBbHeight() * 0.5 + 12.0;
        double verticalGap = Math.abs(hoverY - (this.getY() + this.getBbHeight() * 0.5));
        return horizontal <= LASER_ALIGN_DISTANCE && verticalGap <= LASER_ALIGN_HEIGHT;
    }

    public static float waveRadius(float age) {
        // 波纹半径随时间减速扩张
        float progress = Mth.clamp(age / WAVE_LIFETIME, 0.0F, 1.0F);
        return WAVE_MAX_RADIUS * (1.0F - (1.0F - progress) * (1.0F - progress));
    }

    private void updateFlight() {
        int laserTick = this.getLaserTick();
        // 激光开火后锁定悬停，保证波纹圆心不漂移
        int skill = this.getSkillId();
        if (laserTick > LASER_CHARGE_TICKS || this.getSummonTick() > 0 || skill == SKILL_HALO_SWEEP
            || skill == SKILL_GRAVITY_WELL && this.getSkillTick() < GRAVITY_DROP_TICKS) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
            return;
        }
        Vec3 horizontal = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
        double distance = horizontal.length();
        boolean alignForLaser = laserTick > 0 || this.laserCooldown <= 0 && this.pendingSummonKind() == SUMMON_NONE && skill == SKILL_NONE;
        Vec3 desired = Vec3.ZERO;
        if (alignForLaser) {
            // 冷却结束或蓄力时飞到目标正上方，对齐后才放下射激光
            if (distance > 0.4) {
                double speed = distance > 8.0 ? 0.45 : Math.min(0.36, distance * 0.14);
                desired = horizontal.normalize().scale(speed);
            }
        } else if (distance > 34.0) {
            desired = horizontal.normalize().scale(0.28);
        } else if (distance < 14.0 && distance > 1.0E-4) {
            desired = horizontal.normalize().scale(-0.22);
        } else {
            double orbit = (this.getId() & 1) == 0 ? 1.0 : -1.0;
            desired = new Vec3(-horizontal.z, 0.0, horizontal.x).normalize().scale(0.055 * orbit);
        }
        double targetY = target.getY() + target.getBbHeight() * 0.5 + 12.0;
        double verticalLimit = alignForLaser ? 0.32 : 0.20;
        double vertical = net.minecraft.util.Mth.clamp((targetY - (this.getY() + this.getBbHeight() * 0.5)) * 0.06, -verticalLimit, verticalLimit);
        Vec3 velocity = this.getDeltaMovement().lerp(new Vec3(desired.x, vertical, desired.z), 0.22);
        this.setDeltaMovement(velocity);
    }

    public float withPercentDamage(net.minecraft.world.entity.Entity victim, float damage) {
        if (damage <= 0.0F || !MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_PERCENT_DAMAGE)) {
            return damage;
        }
        LivingEntity living = victim instanceof net.minecraftforge.entity.PartEntity<?> part && part.getParent() instanceof LivingEntity parent
            ? parent : victim instanceof LivingEntity direct ? direct : null;
        double attack = this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (living == null || living instanceof Player || living == this || attack <= 0.0) {
            return damage;
        }
        double multiplier = damage / attack;
        double percent = MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_PERCENT_PER_MULTIPLIER) * multiplier;
        float bonus = (float)(living.getMaxHealth() * percent);
        LOGGER.debug("Saucer percent damage boss={} victim={} type={} baseDamage={} multiplier={} percent={} bonus={}",
            this.getId(), living.getId(), living.getType(), damage, multiplier, percent, bonus);
        return damage + bonus;
    }

    public int destroyedWeaponCount() {
        int destroyed = 0;
        for (int index = 0; index < FlyingSaucerWeapon.values().length; index++) {
            if (this.isWeaponDestroyed(index)) {
                destroyed++;
            }
        }
        return destroyed;
    }

    public float getRageCooldownFactor() {
        double reduction = MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_RAGE_REDUCTION);
        return Math.max(MIN_RAGE_COOLDOWN, 1.0F - (float)reduction * this.destroyedWeaponCount());
    }

    private float skillDamage(ForgeConfigSpec.DoubleValue multiplier) {
        return (float)(this.getAttributeValue(Attributes.ATTACK_DAMAGE) * MechafaunaConfig.get(multiplier));
    }

    public int getSkillId() {
        return this.entityData.get(DATA_SKILL);
    }

    public int getSkillTick() {
        return this.entityData.get(DATA_SKILL_TICK);
    }

    public Vec3 getSkillPoint() {
        Vector3f point = this.entityData.get(DATA_SKILL_POINT);
        return new Vec3(point.x, point.y, point.z);
    }

    public Vec3 getSkillOrigin() {
        Vector3f point = this.entityData.get(DATA_SKILL_ORIGIN);
        return new Vec3(point.x, point.y, point.z);
    }

    public static int skillDuration(int skill) {
        return switch (skill) {
            case SKILL_GRAVITY_WELL -> GRAVITY_COLLAPSE_TICK + GRAVITY_AFTERMATH_TICKS;
            case SKILL_HALO_SWEEP -> HALO_CHARGE_TICKS + HALO_FIRE_TICKS + 8;
            case SKILL_PLASMA_RAIN -> PLASMA_CHARGE_TICKS + PLASMA_SHOTS * PLASMA_INTERVAL + 10;
            default -> 0;
        };
    }

    public static float haloRadius(float fireTime) {
        float progress = Mth.clamp(fireTime / HALO_FIRE_TICKS, 0.0F, 1.0F);
        return HALO_START_RADIUS + (HALO_END_RADIUS - HALO_START_RADIUS) * progress;
    }

    public static double haloAngle(float fireTime, int beam) {
        return beam * Math.PI * 2.0 / HALO_BEAMS + Math.max(0.0F, fireTime) * HALO_SPIN;
    }

    public static Vec3 haloGroundPoint(Vec3 center, float fireTime, int beam) {
        double angle = haloAngle(fireTime, beam);
        double radius = haloRadius(fireTime);
        return center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
    }

    public Vec3 snapToGround(Vec3 point, double above, double below) {
        var hit = this.level().clip(new ClipContext(point.add(0.0, above, 0.0), point.subtract(0.0, below, 0.0),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? point : hit.getLocation();
    }

    private void updateSkills() {
        int skill = this.getSkillId();
        if (this.isDeadOrDying()) {
            if (skill != SKILL_NONE) {
                this.endSkill(skill);
            }
            return;
        }
        if (skill != SKILL_NONE) {
            int tick = this.getSkillTick() + 1;
            this.entityData.set(DATA_SKILL_TICK, tick);
            switch (skill) {
                case SKILL_GRAVITY_WELL -> this.tickGravityWell(tick);
                case SKILL_HALO_SWEEP -> this.tickHaloSweep(tick);
                case SKILL_PLASMA_RAIN -> this.tickPlasmaRain(tick, this.getTarget());
                default -> {
                }
            }
            if (tick >= skillDuration(skill)) {
                this.endSkill(skill);
            }
            return;
        }
        for (int index = 0; index < this.skillCooldowns.length; index++) {
            if (this.skillCooldowns[index] > 0) {
                this.skillCooldowns[index]--;
            }
        }
        if (this.skillInterval > 0) {
            this.skillInterval--;
            return;
        }
        if (this.getLaserTick() != 0 || this.getSummonTick() != 0 || this.pendingSummonKind() != SUMMON_NONE) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !this.canTarget(target)) {
            return;
        }
        double horizontal = Math.hypot(target.getX() - this.getX(), target.getZ() - this.getZ());
        List<Integer> options = new ArrayList<>();
        if (MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_GRAVITY_WELL) && this.skillCooldowns[SKILL_GRAVITY_WELL] == 0
            && horizontal <= 40.0 && this.hasLineOfSight(target)) {
            options.add(SKILL_GRAVITY_WELL);
        }
        if (MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_HALO_SWEEP) && this.skillCooldowns[SKILL_HALO_SWEEP] == 0
            && horizontal <= 20.0) {
            options.add(SKILL_HALO_SWEEP);
        }
        if (MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_PLASMA_RAIN) && this.skillCooldowns[SKILL_PLASMA_RAIN] == 0
            && horizontal <= 48.0) {
            options.add(SKILL_PLASMA_RAIN);
        }
        if (!options.isEmpty()) {
            this.startSkill(options.get(this.random.nextInt(options.size())), target);
        }
    }

    private void startSkill(int skill, LivingEntity target) {
        float rage = this.getRageCooldownFactor();
        int cooldown = switch (skill) {
            case SKILL_GRAVITY_WELL -> MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_GRAVITY_WELL_COOLDOWN);
            case SKILL_HALO_SWEEP -> MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_HALO_SWEEP_COOLDOWN);
            default -> MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_PLASMA_RAIN_COOLDOWN);
        };
        this.skillCooldowns[skill] = Math.round(cooldown * rage);
        Vec3 origin = this.getLaserOrigin();
        Vec3 point;
        if (skill == SKILL_GRAVITY_WELL) {
            point = target.position().add(0.0, target.getBbHeight() * 0.5 + 1.0, 0.0);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 4.0F, 0.5F);
        } else if (skill == SKILL_HALO_SWEEP) {
            point = this.snapToGround(origin, 0.0, LASER_RANGE);
            this.haloHitTicks.clear();
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 1.5F);
        } else {
            point = origin;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 4.0F, 1.9F);
        }
        this.entityData.set(DATA_SKILL_ORIGIN, origin.toVector3f());
        this.entityData.set(DATA_SKILL_POINT, point.toVector3f());
        this.entityData.set(DATA_SKILL_TICK, 0);
        this.entityData.set(DATA_SKILL, skill);
        LOGGER.debug("Saucer skill started boss={} skill={} target={} destroyedWeapons={} rageFactor={} cooldown={}",
            this.getId(), skill, target.getId(), this.destroyedWeaponCount(), rage, this.skillCooldowns[skill]);
    }

    private void endSkill(int skill) {
        this.entityData.set(DATA_SKILL, SKILL_NONE);
        this.entityData.set(DATA_SKILL_TICK, 0);
        this.haloHitTicks.clear();
        this.skillInterval = Math.round(MechafaunaConfig.get(MechafaunaConfig.FLYING_SAUCER_SKILL_INTERVAL) * this.getRageCooldownFactor());
        LOGGER.debug("Saucer skill ended boss={} skill={} nextInterval={}", this.getId(), skill, this.skillInterval);
    }

    private void tickGravityWell(int tick) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = this.getSkillPoint();
        if (tick == GRAVITY_DROP_TICKS) {
            serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.HOSTILE, 4.0F, 0.5F);
        }
        if (tick >= GRAVITY_DROP_TICKS && tick < GRAVITY_COLLAPSE_TICK) {
            float charge = (tick - GRAVITY_DROP_TICKS) / (float)GRAVITY_PULL_TICKS;
            if (tick % 15 == 0) {
                serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.5F + charge);
            }
            if (tick % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 8, 4.0, 2.0, 4.0, 0.15);
            }
            float coreDamage = this.skillDamage(MechafaunaConfig.FLYING_SAUCER_GRAVITY_WELL_MULTIPLIER) * 0.1F;
            int pulled = 0;
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(GRAVITY_PULL_RADIUS), this::canTarget)) {
                Vec3 offset = center.subtract(victim.getBoundingBox().getCenter());
                double distance = offset.length();
                if (distance > GRAVITY_PULL_RADIUS || distance < 0.4) {
                    continue;
                }
                double resistance = 1.0 - Mth.clamp(victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                double strength = (0.045 + 0.09 * (1.0 - distance / GRAVITY_PULL_RADIUS)) * (0.6 + 0.4 * charge) * resistance;
                Vec3 pull = offset.normalize().scale(strength);
                victim.setDeltaMovement(victim.getDeltaMovement().add(pull.x, pull.y * 0.6, pull.z));
                victim.hurtMarked = true;
                victim.fallDistance = 0.0F;
                pulled++;
                if (distance <= GRAVITY_CORE_RADIUS && tick % 10 == 0) {
                    victim.hurt(this.damageSources().mobAttack(this), this.withPercentDamage(victim, coreDamage));
                }
            }
            if (tick % 20 == 0) {
                LOGGER.debug("Saucer gravity well pulling boss={} tick={} pulled={} charge={}", this.getId(), tick, pulled, charge);
            }
        }
        if (tick == GRAVITY_COLLAPSE_TICK) {
            float damage = this.skillDamage(MechafaunaConfig.FLYING_SAUCER_GRAVITY_WELL_MULTIPLIER);
            int hits = 0;
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(GRAVITY_COLLAPSE_RADIUS), this::canTarget)) {
                double distance = victim.getBoundingBox().getCenter().distanceTo(center);
                if (distance > GRAVITY_COLLAPSE_RADIUS) {
                    continue;
                }
                double dx = victim.getX() - center.x;
                double dz = victim.getZ() - center.z;
                if (victim.hurt(this.damageSources().mobAttack(this), this.withPercentDamage(victim, damage * (float)(1.0 - 0.4 * distance / GRAVITY_COLLAPSE_RADIUS)))) {
                    victim.knockback(1.5, -dx, -dz);
                    victim.hurtMarked = true;
                    hits++;
                }
            }
            serverLevel.sendParticles(new ExplosionParticleOptions((float)GRAVITY_COLLAPSE_RADIUS * 0.8F), center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 80, 0.5, 0.5, 0.5, 1.2);
            serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 30, 0.3, 0.3, 0.3, 0.6);
            serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5.0F, 0.6F);
            serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.5F);
            LOGGER.debug("Saucer gravity well collapsed boss={} center={} damage={} hits={}", this.getId(), center, damage, hits);
        }
    }

    private void tickHaloSweep(int tick) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int fire = tick - HALO_CHARGE_TICKS;
        if (fire == 0) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.6F);
        }
        if (fire < 0 || fire >= HALO_FIRE_TICKS) {
            return;
        }
        if (fire % 8 == 0) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 4.0F, 1.7F);
        }
        Vec3 center = this.getSkillPoint();
        float radius = haloRadius(fire);
        float damage = this.skillDamage(MechafaunaConfig.FLYING_SAUCER_HALO_SWEEP_MULTIPLIER);
        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class,
            new AABB(center, center).inflate(radius + HALO_HIT_RADIUS + 1.0, 8.0, radius + HALO_HIT_RADIUS + 1.0), this::canTarget);
        for (int beam = 0; beam < HALO_BEAMS; beam++) {
            Vec3 ground = this.snapToGround(haloGroundPoint(center, fire, beam), 6.0, 10.0);
            if (fire % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME, ground.x, ground.y + 0.1, ground.z, 2, 0.4, 0.05, 0.4, 0.02);
            }
            for (LivingEntity victim : candidates) {
                double dx = victim.getX() - ground.x;
                double dz = victim.getZ() - ground.z;
                if (dx * dx + dz * dz > HALO_HIT_RADIUS * HALO_HIT_RADIUS || Math.abs(victim.getY() - ground.y) > 3.0) {
                    continue;
                }
                Integer last = this.haloHitTicks.get(victim.getId());
                if (last != null && this.tickCount - last < HALO_HIT_INTERVAL) {
                    continue;
                }
                if (victim.hurt(this.damageSources().mobAttack(this), this.withPercentDamage(victim, damage))) {
                    victim.setSecondsOnFire(3);
                    this.haloHitTicks.put(victim.getId(), this.tickCount);
                    LOGGER.debug("Saucer halo sweep hit boss={} victim={} beam={} radius={} damage={}", this.getId(), victim.getId(), beam, radius, damage);
                }
            }
        }
    }

    private void tickPlasmaRain(int tick, LivingEntity target) {
        int shotTick = tick - PLASMA_CHARGE_TICKS;
        if (shotTick < 0 || shotTick % PLASMA_INTERVAL != 0 || shotTick / PLASMA_INTERVAL >= PLASMA_SHOTS
            || target == null || !target.isAlive()) {
            return;
        }
        int shot = shotTick / PLASMA_INTERVAL;
        Vec3 aim;
        if (shot % 3 == 0) {
            Vec3 velocity = new Vec3(target.getX() - target.xOld, 0.0, target.getZ() - target.zOld);
            Vec3 lead = velocity.scale(PLASMA_IMPACT_DELAY);
            if (lead.lengthSqr() > 64.0) {
                lead = lead.normalize().scale(8.0);
            }
            aim = target.position().add(lead);
        } else {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double distance = 2.5 + this.random.nextDouble() * 5.5;
            aim = target.position().add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
        }
        float damage = this.skillDamage(MechafaunaConfig.FLYING_SAUCER_PLASMA_RAIN_MULTIPLIER);
        MechStrikeZone.spawn(this, aim, MechStrikeZone.STYLE_PLASMA, PLASMA_IMPACT_DELAY, 2.4F, damage, 0.6);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 3.0F, 0.6F);
        LOGGER.debug("Saucer plasma rain shot boss={} shot={} aim={} damage={}", this.getId(), shot, aim, damage);
    }

    private void emitWreckageSmoke() {
        if (this.tickCount % 4 != 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            if (!this.isWeaponDestroyed(weapon.ordinal())) {
                continue;
            }
            SaucerDamagePart part = this.parts[WEAPON_PART_OFFSET + weapon.ordinal()];
            Vec3 position = part.position().add(0.0, 1.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, position.x, position.y, position.z, 2, 0.4, 0.2, 0.4, 0.02);
            if (this.random.nextInt(3) == 0) {
                serverLevel.sendParticles(ParticleTypes.LAVA, position.x, position.y, position.z, 1, 0.3, 0.1, 0.3, 0.0);
            }
        }
    }

    private void updatePartPositions() {
        Matrix4d model = WeaponMount.modelToWorld(this.position(), this.yBodyRot, this.getModelScale());
        this.parts[0].setPartPosition(this.getX(), this.getY() + BODY_CENTER_Y, this.getZ());
        WeaponPose pose = WeaponPose.forModel("flying_saucer_boss");
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            Matrix4d parent = pose.parentTransform(weapon.mount(), model, null, 0.0);
            Vec3 center = Vec3.ZERO;
            for (int muzzle = 0; muzzle < 6; muzzle++) {
                center = center.add(pose.bonePivot(weapon.muzzleBone(muzzle)));
            }
            Vec3 world = WeaponMount.transform(parent, center.scale(1.0 / 6.0));
            this.parts[WEAPON_PART_OFFSET + weapon.ordinal()].setPartPosition(world.x, world.y, world.z);
        }
    }

    private void clearWeaponTarget(int index) {
        this.weaponTargets[index] = null;
        this.cancelBurst(index);
    }

    private void cancelBurst(int index) {
        this.burstTargets[index] = null;
        this.burstStartTick[index] = 0;
        this.burstShotIndex[index] = 0;
        this.burstActive[index] = false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            tag.putFloat("WeaponHealth_" + weapon.name(), this.getWeaponHealth(weapon.ordinal()));
        }
        // 记录阶段召唤是否已完成
        tag.putBoolean("TankSummoned", this.tankSummoned);
        tag.putBoolean("MechSummoned", this.mechSummoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        float partMax = this.getWeaponMaxHealth();
        for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
            String key = "WeaponHealth_" + weapon.name();
            float health = tag.contains(key) ? tag.getFloat(key) : partMax;
            this.entityData.set(DATA_PART_HEALTH.get(weapon.ordinal()), Mth.clamp(health, 0.0F, partMax));
        }
        this.tankSummoned = tag.getBoolean("TankSummoned");
        this.mechSummoned = tag.getBoolean("MechSummoned");
    }

    private void fillWeaponHealth() {
        float partMax = this.getWeaponMaxHealth();
        for (int index = 0; index < FlyingSaucerWeapon.values().length; index++) {
            this.entityData.set(DATA_PART_HEALTH.get(index), partMax);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    protected String getMovementAnimation() {
        return null;
    }

    @Override
    protected String getIdleAnimation() {
        return null;
    }

    @Override
    protected java.util.Map<String, String> getActionAnimations() {
        return java.util.Map.of();
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 0;
    }

    @Override
    public int getActionCooldown(String action) {
        return 0;
    }

    @Override
    protected void addExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<FlyingSaucerBoss> death = new AnimationController<>(this, "death", 0, state -> {
            if (this.isDeadOrDying()) {
                state.setAnimation(RawAnimation.begin().thenPlay("animation.flying_saucer_boss.death"));
                return PlayState.CONTINUE;
            }
            return PlayState.STOP;
        });
        controllers.add(death);
    }

    @Override
    public double getCombatHoldDistance() {
        return 28.0;
    }

    @Override
    public double getChaseSpeed() {
        return 0.20;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        // 死亡动画 5 秒后由服务端放爆炸再移除，避免客户端实体先被删掉
        if (this.deathTime == 100) {
            if (this.level() instanceof ServerLevel serverLevel) {
                this.spawnFinalExplosion(serverLevel);
                this.remove(RemovalReason.KILLED);
            }
            return;
        }
        this.updatePartPositions();
    }

    private void spawnFinalExplosion(ServerLevel serverLevel) {
        double x = this.getX();
        double y = this.getY() + 2.0;
        double z = this.getZ();
        serverLevel.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 8.0F, 0.55F);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 40, 3.0, 2.0, 3.0, 0.0);
        serverLevel.sendParticles(ParticleTypes.SMOKE, x, this.getY(), z, 120, 4.0, 2.0, 4.0, 0.08);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return super.getAnimatableInstanceCache();
    }
}
