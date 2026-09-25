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
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class MechanicalDrone extends AnimatedHostileMob {
    private static final String GUN = "gun";
    private static final String HOVER = "animation.mechanical_drone.hover";
    private static final String ROTORS = "animation.mechanical_drone.rotors";
    private static final double KNOCKBACK_RESISTANCE = 0.20;

    public MechanicalDrone(EntityType<? extends MechanicalDrone> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(22.0, 0.27, 1.0, 32.0, KNOCKBACK_RESISTANCE).add(Attributes.FLYING_SPEED, 0.36);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected String getMovementAnimation() {
        return HOVER;
    }

    @Override
    protected String getIdleAnimation() {
        return HOVER;
    }

    @Override
    protected float getCombatTurnSpeed() {
        return 28.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return GUN.equals(action)
            && target.isAlive()
            && this.hasLineOfSight(target)
            && this.distanceToSqr(target) <= 18.0 * 18.0;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(GUN, "animation.mechanical_drone.fire");
    }

    @Override
    protected void addExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotors", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop(ROTORS));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared <= 17.0 * 17.0 ? List.of(GUN) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 33;
    }

    @Override
    public int getActionCooldown(String action) {
        return 46;
    }

    @Override
    public double getCombatHoldDistance() {
        return 9.5;
    }

    @Override
    public double getChaseSpeed() {
        return 1.0;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.65F, 1.45F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (GUN.equals(action) && actionTick >= 5 && actionTick <= 29 && actionTick % 3 == 2) {
            this.damageTarget(target, 1.0F, 18.0 * 18.0);
            this.emitShotParticles(target);
        }
    }
}
