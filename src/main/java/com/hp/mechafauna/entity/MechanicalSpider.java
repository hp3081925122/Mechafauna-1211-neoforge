package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;

public final class MechanicalSpider extends AnimatedHostileMob {
    private static final String BITE = "bite";
    private static final String DEATH = "overload";
    private static final double KNOCKBACK_RESISTANCE = 0.20;
    private boolean overloadExplosionTriggered;

    public MechanicalSpider(EntityType<? extends MechanicalSpider> entityType, Level level) {
        super(entityType, level);
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
            BITE, "animation.mechanical_spider.bite",
            DEATH, "animation.mechanical_spider.death_overload"
        );
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared <= 2.5 * 2.5 ? List.of(BITE) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 13;
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
        if (BITE.equals(action) && actionTick == 7) {
            this.damageTarget(target, 2.5F, 2.8 * 2.8);
        }
    }

    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime == 1 && !this.level().isClientSide) {
            this.triggerAnim("action", DEATH);
        }
        if (!this.level().isClientSide && !this.overloadExplosionTriggered && this.deathTime >= 22) {
            this.overloadExplosionTriggered = true;
            this.level().explode(this, this.getX(), this.getY() + 0.3, this.getZ(), 2.5F, false, ExplosionInteraction.NONE);
        }
        if (this.deathTime >= 24 && !this.level().isClientSide && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(RemovalReason.KILLED);
        }
    }
}
