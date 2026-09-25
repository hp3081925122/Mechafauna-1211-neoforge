package com.hp.mechafauna.entity.ai;

import com.hp.mechafauna.entity.AnimatedHostileMob;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public final class CombatGoal extends Goal {
    private final AnimatedHostileMob mob;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    private int pathRefreshTicks;

    public CombatGoal(AnimatedHostileMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && !this.mob.isDeadOrDying();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.pathRefreshTicks = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.mob.cancelAction();
        this.pathRefreshTicks = 0;
    }

    @Override
    public void tick() {
        this.cooldowns.replaceAll((action, cooldown) -> Math.max(0, cooldown - 1));
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            this.mob.getNavigation().stop();
            this.mob.cancelAction();
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.mob.updateCombatAim();
        double distanceSquared = this.mob.distanceToSqr(target);
        if (this.mob.isPerformingAction()) {
            this.mob.tickAction(target);
            if (this.mob.isPerformingAction()) {
                if (this.mob.canMoveDuringAction(this.mob.getActiveAction())) {
                    this.updateCombatMovement(target, distanceSquared);
                } else {
                    this.mob.getNavigation().stop();
                }
                return;
            }
        }
        if (this.mob.hasLineOfSight(target)) {
            for (String action : this.mob.getAvailableActions(target, distanceSquared)) {
                if (this.cooldowns.getOrDefault(action, 0) == 0) {
                    if (!this.mob.canMoveDuringAction(action)) {
                        this.mob.getNavigation().stop();
                    }
                    this.mob.beginAction(action, target);
                    this.cooldowns.put(action, this.mob.getActionCooldown(action));
                    return;
                }
            }
        }
        this.updateCombatMovement(target, distanceSquared);
    }

    private void updateCombatMovement(LivingEntity target, double distanceSquared) {
        double distance = Math.sqrt(distanceSquared);
        double retreatDistance = this.mob.getCombatRetreatDistance();
        if (retreatDistance > 0.0 && distance < retreatDistance) {
            net.minecraft.world.phys.Vec3 away = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-6) {
                away = this.mob.getLookAngle().multiply(-1.0, 0.0, -1.0);
            }
            double retreatLength = Math.max(1.0, retreatDistance - away.horizontalDistance() + 0.5);
            net.minecraft.world.phys.Vec3 destination = this.mob.position().add(away.normalize().scale(retreatLength));
            if (this.pathRefreshTicks-- <= 0 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 0, this.mob.getChaseSpeed());
                this.pathRefreshTicks = 8;
            }
        } else if (distance > this.mob.getCombatHoldDistance() || !this.mob.hasLineOfSight(target)) {
            if (this.pathRefreshTicks-- <= 0 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(target, this.mob.getChaseSpeed());
                this.pathRefreshTicks = 8;
            }
        } else {
            this.mob.getNavigation().stop();
        }
    }
}
