package com.hp.mechafauna.entity;

import com.hp.mechafauna.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class MechanicalTank extends MechanicalMob {
    private static final String FIRE = "fire";
    private static final String MOVE = "animation.mechanical_tank.move";
    private static final String TURRET_TURN = "animation.mechanical_tank.turret_turn";
    private static final double MAX_HEALTH = 105.0;
    private static final double KNOCKBACK_RESISTANCE = 0.55;
    private static final int SPIDER_SUMMON_INTERVAL_TICKS = 100;
    private static final int MAX_SUMMONED_SPIDERS = 3;
    private static final double REINFORCEMENT_DISTANCE = 3.2;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float MODEL_SCALE = 1.4422496F;
    private int spiderSummonCooldown;
    private boolean halfHealthReinforcementsSummoned;

    public MechanicalTank(EntityType<? extends MechanicalTank> entityType, Level level) {
        super(entityType, level);
        this.spiderSummonCooldown = this.random.nextInt(SPIDER_SUMMON_INTERVAL_TICKS) + 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.isAlive() && !this.halfHealthReinforcementsSummoned && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.halfHealthReinforcementsSummoned = true;
            double firstAngle = this.random.nextDouble() * Math.PI * 2.0;
            this.summonMechanicalWolf(firstAngle);
            this.summonMechanicalWolf(firstAngle + Math.PI);
            LOGGER.debug("Mechanical tank half-health reinforcements summoned tank={} wolves=2 target={}", this.getId(), this.getTarget() == null ? -1 : this.getTarget().getId());
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpiderSummonCooldown", this.spiderSummonCooldown);
        tag.putBoolean("HalfHealthReinforcementsSummoned", this.halfHealthReinforcementsSummoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpiderSummonCooldown")) {
            this.spiderSummonCooldown = tag.getInt("SpiderSummonCooldown");
        }
        this.halfHealthReinforcementsSummoned = tag.getBoolean("HalfHealthReinforcementsSummoned");
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
        if (target != null && target.isAlive() && !(target instanceof AnimatedHostileMob)) {
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
        spider.setSummonerUUID(this.getUUID());
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && !(target instanceof AnimatedHostileMob)) {
            spider.setTarget(target);
        }
        if (this.level().addFreshEntity(spider)) {
            LOGGER.debug("Mechanical tank summoned spider tank={} spider={} target={}", this.getId(), spider.getId(), target == null ? -1 : target.getId());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(MAX_HEALTH, 0.38, 10.0, 36.0, KNOCKBACK_RESISTANCE).add(Attributes.SCALE, MODEL_SCALE);
    }

    @Override
    public double getModelScale() {
        return super.getModelScale() * MODEL_SCALE;
    }

    @Override
    protected String getMovementAnimation() {
        return MOVE;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return FIRE.equals(action)
            && target.isAlive()
            && this.hasLineOfSight(target)
            && this.distanceToSqr(target) <= 22.0 * 22.0;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(FIRE, "animation.mechanical_tank.fire");
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
        return distanceSquared <= 22.0 * 22.0 ? List.of(FIRE) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 20;
    }

    @Override
    public int getActionCooldown(String action) {
        return 72;
    }

    @Override
    public double getCombatHoldDistance() {
        return 8.5;
    }

    @Override
    public double getCombatRetreatDistance() {
        return 7.5;
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
        this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (FIRE.equals(action) && actionTick == 10) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.6F, 0.55F);
            this.launchProjectile(target, WeaponMount.TANK_CANNON, WeaponMount.TANK_CANNON.muzzle(), false, false, 2.30, 0.0F);
        }
    }
}
