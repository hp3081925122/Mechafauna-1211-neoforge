package com.hp.mechafauna.entity;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public final class MechanicalSpider extends MechanicalMob {
    private static final double ITEM_THROW_GRAVITY = 0.08;
    private static final String BITE = "bite";
    private static final String DEATH = "overload";
    private static final int BITE_DURATION_TICKS = 20;
    private static final int BITE_HIT_TICK = 10;
    private static final int DEATH_ANIMATION_TICKS = 40;
    private static final int DEATH_EXPLOSION_TICK = 31;
    private static final int DEATH_CLEANUP_TICKS = 40;
    private static final int THROWN_DETONATION_TIMEOUT_TICKS = 40;
    private static final float EXPLOSION_RADIUS = 3.0F;
    private static final float THROWN_EXPLOSION_RADIUS = EXPLOSION_RADIUS;
    private static final float THROWN_EXPLOSION_DAMAGE_MULTIPLIER = 1.5F;
    private static final double KNOCKBACK_RESISTANCE = 0.20;
    private static final double CHAIN_TRIGGER_RADIUS = 5.0;
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean overloadExplosionTriggered;
    private int successfulBites;
    private boolean selfDestructing;
    private LivingEntity overloadTarget;
    private UUID summonerUUID;
    private boolean playerSummon;
    private int supportTicks = -1;
    private boolean thrownItemSpider;
    private int thrownItemTicks;

    public MechanicalSpider(EntityType<? extends MechanicalSpider> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("ThrownItemSpider", this.thrownItemSpider);
        tag.putInt("ThrownItemTicks", this.thrownItemTicks);
        if (this.summonerUUID != null) {
            tag.putUUID("Summoner", this.summonerUUID);
            tag.putBoolean("PlayerSummon", this.playerSummon);
        }
        if (this.supportTicks > 0) {
            tag.putInt("SupportTicks", this.supportTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.thrownItemSpider = tag.getBoolean("ThrownItemSpider");
        this.thrownItemTicks = tag.getInt("ThrownItemTicks");
        this.summonerUUID = tag.hasUUID("Summoner") ? tag.getUUID("Summoner") : null;
        this.playerSummon = this.summonerUUID != null && (!tag.contains("PlayerSummon") || tag.getBoolean("PlayerSummon"));
        this.supportTicks = tag.contains("SupportTicks") ? tag.getInt("SupportTicks") : -1;
    }

    public void launchFromItem(UUID summonerUUID, Vec3 velocity) {
        this.summonerUUID = summonerUUID;
        this.playerSummon = summonerUUID != null;
        this.thrownItemSpider = true;
        this.thrownItemTicks = 0;
        this.setNoAi(true);
        this.setDeltaMovement(velocity);
        LOGGER.debug("Mechanical spider item launch entity={} summoner={} position={} velocity={}", this.getId(), summonerUUID, this.position(), velocity);
    }

    @Override
    public void travel(Vec3 input) {
        if (this.thrownItemSpider && !this.selfDestructing) {
            if (!this.level().isClientSide && this.thrownItemTicks == 0) {
                LOGGER.debug("Mechanical spider item flight started entity={} position={} velocity={} gravity={}", this.getId(), this.position(), this.getDeltaMovement(), ITEM_THROW_GRAVITY);
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 velocity = this.getDeltaMovement();
            this.setDeltaMovement(velocity.x * 0.91, (velocity.y - ITEM_THROW_GRAVITY) * 0.98, velocity.z * 0.91);
            return;
        }
        super.travel(input);
    }

    public void limitLife(int ticks) {
        this.supportTicks = ticks;
        this.setPersistenceRequired();
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && this.supportTicks > 0) {
            this.supportTicks--;
            if (this.supportTicks == 0) {
                this.discard();
                return;
            }
        }
        double previousX = this.getX();
        double previousY = this.getY();
        double previousZ = this.getZ();
        super.tick();
        if (this.level().isClientSide || !this.thrownItemSpider || this.selfDestructing || this.isDeadOrDying()) {
            return;
        }
        this.thrownItemTicks++;
        AABB previousBounds = this.getBoundingBox().move(previousX - this.getX(), previousY - this.getY(), previousZ - this.getZ());
        AABB sweptBounds = previousBounds.minmax(this.getBoundingBox()).inflate(0.05);
        LivingEntity hitTarget = this.level().getEntitiesOfClass(LivingEntity.class, sweptBounds,
            candidate -> candidate != this
                && candidate.isAlive()
                && candidate instanceof Enemy
                && !AnimatedHostileMob.isIgnoredByOtherMechs(candidate)
                && (this.summonerUUID == null || !this.summonerUUID.equals(candidate.getUUID())))
            .stream()
            .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
            .orElse(null);
        if (hitTarget != null) {
            this.setTarget(hitTarget);
            this.primeForDetonation(hitTarget);
            return;
        }
        if (this.onGround() || this.horizontalCollision || this.verticalCollision || this.thrownItemTicks >= THROWN_DETONATION_TIMEOUT_TICKS) {
            this.primeForDetonation(this.getTarget());
        }
    }

    public void setSummonerUUID(UUID summonerUUID) {
        this.summonerUUID = summonerUUID;
        this.playerSummon = summonerUUID != null;
    }

    public void setMechSummoner(UUID summonerUUID) {
        this.summonerUUID = summonerUUID;
        this.playerSummon = false;
    }

    public UUID getSummonerUUID() {
        return this.summonerUUID;
    }

    public UUID getPlayerSummoner() {
        return this.playerSummon ? this.summonerUUID : null;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AnimatedHostileMob.class, 10, true, false,
            target -> this.playerSummon && playerSummonOwner(target) == null));
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return this.summonerUUID != null && (this.summonerUUID.equals(other.getUUID()) || samePlayerSummon(this, other))
            || super.isAlliedTo(other);
    }

    public boolean isSummonedBy(UUID summonerUUID) {
        return summonerUUID != null && summonerUUID.equals(this.summonerUUID);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (this.playerSummon && (target instanceof Player || !(target instanceof Enemy) || this.isAlliedTo(target))) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        if (!item.is(Items.FLINT_AND_STEEL)) {
            return super.mobInteract(player, hand);
        }
        if (!this.selfDestructing && !this.isDeadOrDying()) {
            this.level().playSound(player, this.getX(), this.getY(), this.getZ(), SoundEvents.FLINTANDSTEEL_USE, this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level().isClientSide && this.primeForDetonation(player)) {
                item.hurtAndBreak(1, player, living -> living.broadcastBreakEvent(hand));
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(20.0, 0.31, 2.0, 26.0, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected String getMovementAnimation() {
        return "animation.mechanical_spider.walk";
    }

    @Override
    protected String getIdleAnimation() {
        return "animation.mechanical_spider.idle";
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return BITE.equals(action)
            && target.isAlive()
            && this.hasLineOfSight(target)
            && this.distanceToSqr(target) <= 2.8 * 2.8;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(
            BITE, "animation.mechanical_spider.attack",
            DEATH, "animation.mechanical_spider.explode"
        );
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared <= 2.5 * 2.5 ? List.of(BITE) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return BITE_DURATION_TICKS;
    }

    @Override
    protected int getInterruptedActionCooldown(String action, int actionTick) {
        return actionTick < BITE_HIT_TICK ? 6 : -1;
    }

    @Override
    public boolean canMoveDuringAction(String action) {
        return BITE.equals(action);
    }

    @Override
    public int getActionCooldown(String action) {
        return 27;
    }

    @Override
    public double getCombatHoldDistance() {
        return 1.8;
    }

    @Override
    public double getChaseSpeed() {
        return 1.08;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (BITE.equals(action) && actionTick == BITE_HIT_TICK && this.damageTarget(target, 2.5F, 2.8 * 2.8)) {
            this.successfulBites++;
            if (this.successfulBites >= 2) {
                this.primeForDetonation(target);
            }
        }
    }

    public boolean primeForDetonation(LivingEntity target) {
        if (this.level().isClientSide || this.selfDestructing || this.isDeadOrDying() || this.isRemoved()) {
            return false;
        }
        this.selfDestructing = true;
        this.overloadTarget = target != null && target.isAlive() ? target : this.getTarget();
        this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.65F);
        this.gameEvent(GameEvent.PRIME_FUSE);
        this.setHealth(0.0F);
        LOGGER.debug("Mechanical spider detonation primed entity={} target={}", this.getId(), this.overloadTarget == null ? -1 : this.overloadTarget.getId());
        return true;
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (!this.level().isClientSide && this.selfDestructing && this.overloadTarget != null && this.overloadTarget.isAlive()) {
            Vec3 direction = this.overloadTarget.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (direction.lengthSqr() > 1.0E-6) {
                direction = direction.normalize();
                float targetYaw = (float)(Math.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
                this.setYRot(targetYaw);
                this.setYBodyRot(targetYaw);
                this.setYHeadRot(targetYaw);
                this.move(MoverType.SELF, direction.scale(0.12));
            }
        }
        if (this.deathTime == 1 && !this.level().isClientSide) {
            LOGGER.debug("Mechanical spider death animation triggered entity={} selfDestructing={} animation=animation.mechanical_spider.explode", this.getId(), this.selfDestructing);
            this.triggerAnim("action", DEATH);
        }
        if (!this.level().isClientSide && !this.overloadExplosionTriggered && this.deathTime >= DEATH_EXPLOSION_TICK) {
            this.overloadExplosionTriggered = true;
            double chainRadiusSquared = CHAIN_TRIGGER_RADIUS * CHAIN_TRIGGER_RADIUS;
            for (AnimatedHostileMob nearbyMob : this.level().getEntitiesOfClass(AnimatedHostileMob.class, this.getBoundingBox().inflate(CHAIN_TRIGGER_RADIUS))) {
                if (nearbyMob == this || nearbyMob.distanceToSqr(this) > chainRadiusSquared) {
                    continue;
                }
                if (nearbyMob instanceof MechanicalSpider spider) {
                    if (samePlayerSummon(this, spider)) {
                        continue;
                    }
                    spider.primeForDetonation(this.overloadTarget);
                } else if (nearbyMob instanceof FlameCrawler crawler) {
                    crawler.primeForDetonation(this.overloadTarget);
                }
            }
            float explosionRadius = this.thrownItemSpider ? THROWN_EXPLOSION_RADIUS : EXPLOSION_RADIUS;
            if (this.thrownItemSpider) {
                LOGGER.debug("Thrown mechanical spider detonating entity={} radius={} rangeMultiplier=1.0 damageMultiplier={}", this.getId(), explosionRadius, THROWN_EXPLOSION_DAMAGE_MULTIPLIER);
            }
            UUID summoner = this.summonerUUID;
            ExplosionEffects.Policy policy = this.thrownItemSpider
                ? new ExplosionEffects.Policy(entity -> !(entity instanceof Player),
                    entity -> THROWN_EXPLOSION_DAMAGE_MULTIPLIER, entity -> entity instanceof Player ? 0.0 : 1.0)
                : new ExplosionEffects.Policy(entity -> summoner == null
                    || (!summoner.equals(entity.getUUID()) && !samePlayerSummon(this, entity)),
                    entity -> 1.0, entity -> summoner != null
                        && (summoner.equals(entity.getUUID()) || samePlayerSummon(this, entity)) ? 0.0 : 1.0);
            ExplosionEffects.explode(this.level(), this, null,
                this.getX(), this.getY() + 0.3, this.getZ(), explosionRadius, policy);
        }
        if (this.deathTime >= DEATH_CLEANUP_TICKS && !this.level().isClientSide && !this.isRemoved()) {
            LOGGER.debug("Mechanical spider death animation cleanup entity={} deathTime={} animationLengthTicks={}", this.getId(), this.deathTime, DEATH_ANIMATION_TICKS);
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
    }
}
