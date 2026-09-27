package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

public final class MechanicalWolf extends MechanicalMob {
    private static final String BITE = "bite";
    private static final String WALK = "walk";
    private static final String RUN = "run";
    private static final int BITE_HIT_TICK = 8;
    private static final double KNOCKBACK_RESISTANCE = 0.15;

    public MechanicalWolf(EntityType<? extends MechanicalWolf> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(26.0, 0.34, 3.0, 28.0, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected String getMovementAnimation() {
        return this.hasCombatTarget() ? RUN : WALK;
    }

    @Override
    protected int getActionTransitionTicks() {
        return 2;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        if (!target.isAlive() || !this.hasLineOfSight(target) || this.distanceToSqr(target) > 2.6 * 2.6) {
            return false;
        }
        return actionTick < BITE_HIT_TICK + 2 || this.distanceToSqr(target) <= 1.9 * 1.9;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(BITE, "attack_bite");
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared <= 2.0 * 2.0 ? List.of(BITE) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 18;
    }

    @Override
    public int getActionCooldown(String action) {
        return 25;
    }

    @Override
    public double getCombatHoldDistance() {
        return 1.7;
    }

    @Override
    public double getChaseSpeed() {
        return 1.18;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.WOLF_SOUNDS.get(net.minecraft.world.entity.animal.wolf.WolfSoundVariants.SoundSet.CLASSIC).adultSounds().growlSound().value(), SoundSource.HOSTILE, 0.7F, 0.8F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (BITE.equals(action) && actionTick == BITE_HIT_TICK) {
            this.damageTarget(target, 5.0F, 2.8 * 2.8);
        }
    }
}
