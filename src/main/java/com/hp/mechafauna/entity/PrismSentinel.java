package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

public final class PrismSentinel extends MechanicalMob {
    private static final String LASER = "laser";
    private static final int LASER_FIRE_TICK = 13;
    private static final double LASER_RANGE = 24.0;

    public PrismSentinel(EntityType<? extends PrismSentinel> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(24.0, 0.32, 2.0, 32.0, 0.25).add(Attributes.FLYING_SPEED, 0.38);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected String getMovementAnimation() {
        return null;
    }

    @Override
    protected float getCombatTurnSpeed() {
        return 24.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return LASER.equals(action)
            && target.isAlive()
            && this.hasLineOfSight(target)
            && this.distanceToSqr(target) <= (LASER_RANGE + 2.0) * (LASER_RANGE + 2.0);
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of();
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared >= 5.0 * 5.0 && distanceSquared <= LASER_RANGE * LASER_RANGE ? List.of(LASER) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 28;
    }

    @Override
    public int getActionCooldown(String action) {
        return 48;
    }

    @Override
    public double getCombatHoldDistance() {
        return 11.0;
    }

    @Override
    public double getCombatRetreatDistance() {
        return 5.0;
    }

    @Override
    public double getChaseSpeed() {
        return 1.0;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        if (LASER.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 0.75F, 1.65F);
        }
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (LASER.equals(action) && actionTick == LASER_FIRE_TICK) {
            this.launchPrismLaserFromPosition(target, this.position().add(0.0, this.getBbHeight() * 0.7, 0.0), 1.8, 6.0F);
        }
    }
}
