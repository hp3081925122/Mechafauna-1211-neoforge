package com.hp.mechafauna.entity.ai;

import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.mojang.logging.LogUtils;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class CombatGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double[] RETREAT_ANGLES = {0.0, 35.0, -35.0, 70.0, -70.0, 105.0, -105.0};
    private final AnimatedHostileMob mob;
    private String movementMode = "";
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
            String runningAction = this.mob.getActiveAction();
            this.mob.tickAction(target);
            int interruptedCooldown = this.mob.consumeInterruptedActionCooldown();
            if (interruptedCooldown >= 0 && this.cooldowns.getOrDefault(runningAction, 0) > interruptedCooldown) {
                this.cooldowns.put(runningAction, interruptedCooldown);
                LOGGER.debug("Mech action cooldown shortened after early interrupt entity={} action={} cooldown={}",
                    this.mob.getId(), runningAction, interruptedCooldown);
            }
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
            if (this.pathRefreshTicks-- <= 0 || this.mob.getNavigation().isDone()) {
                Path path = this.findRetreatPath(target, retreatDistance - distance);
                if (path != null) {
                    this.mob.getNavigation().moveTo(path, this.mob.getChaseSpeed() * 1.1);
                    this.setMovementMode("retreat", distance);
                } else {
                    this.mob.getNavigation().stop();
                    this.setMovementMode("retreat_blocked", distance);
                }
                this.pathRefreshTicks = 6;
            }
        } else if (distance > this.mob.getCombatHoldDistance() || !this.mob.hasLineOfSight(target)) {
            if (this.pathRefreshTicks-- <= 0 || this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(target, this.mob.getChaseSpeed());
                this.pathRefreshTicks = 8;
                this.setMovementMode("approach", distance);
            }
        } else {
            this.mob.getNavigation().stop();
            this.setMovementMode("hold", distance);
        }
    }

    private Path findRetreatPath(LivingEntity target, double missingDistance) {
        Vec3 away = this.mob.position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-6) {
            away = this.mob.getLookAngle().multiply(-1.0, 0.0, -1.0);
        }
        away = away.normalize();
        double step = Math.max(3.0, missingDistance + 2.5);
        for (double angle : RETREAT_ANGLES) {
            double radians = Math.toRadians(angle);
            Vec3 direction = new Vec3(away.x * Math.cos(radians) - away.z * Math.sin(radians), 0.0,
                away.x * Math.sin(radians) + away.z * Math.cos(radians));
            Vec3 destination = this.mob.position().add(direction.scale(step));
            Path path = this.mob.getNavigation().createPath(destination.x, destination.y, destination.z, 1);
            if (path != null && path.canReach()) {
                return path;
            }
        }
        return null;
    }

    private void setMovementMode(String mode, double distance) {
        if (!mode.equals(this.movementMode)) {
            LOGGER.debug("Mech combat movement entity={} type={} mode={} distance={} hold={} retreat={}",
                this.mob.getId(), this.mob.getType(), mode, distance, this.mob.getCombatHoldDistance(), this.mob.getCombatRetreatDistance());
            this.movementMode = mode;
        }
    }
}
