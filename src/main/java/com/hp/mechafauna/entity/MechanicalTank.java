package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class MechanicalTank extends AnimatedHostileMob {
    private static final String FIRE = "fire";
    private static final String MOVE = "animation.mechanical_tank.move";
    private static final String TURRET_TURN = "animation.mechanical_tank.turret_turn";
    private static final double MAX_HEALTH = 210.0;
    private static final double KNOCKBACK_RESISTANCE = 0.55;
    public static final float MODEL_SCALE = 1.4422496F;

    public MechanicalTank(EntityType<? extends MechanicalTank> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(MAX_HEALTH, 0.19, 10.0, 36.0, KNOCKBACK_RESISTANCE).add(Attributes.SCALE, MODEL_SCALE);
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
        return 14.0;
    }

    @Override
    public double getChaseSpeed() {
        return 0.86;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (FIRE.equals(action) && actionTick == 10) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.6F, 0.55F);
            this.launchProjectile(target, false, 1.15);
        }
    }
}
