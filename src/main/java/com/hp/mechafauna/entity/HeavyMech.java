package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class HeavyMech extends AnimatedHostileMob {
    private static final String MACHINE_GUN = "machine_gun";
    private static final String MISSILE = "missile";
    private static final String STOMP = "stomp";
    private static final String BACKSTEP = "backstep";
    private static final int[] MISSILE_TICKS = {8, 12, 16, 21, 25, 30};
    private static final double KNOCKBACK_RESISTANCE = 0.85;
    private double backstepProgress;

    public HeavyMech(EntityType<? extends HeavyMech> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(180.0, 0.23, 14.0, 40.0, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected String getMovementAnimation() {
        return "animation.heavy_mech.walk";
    }

    @Override
    protected float getCombatTurnSpeed() {
        return 12.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }
        double distanceSquared = this.distanceToSqr(target);
        return switch (action) {
            case MACHINE_GUN -> this.hasLineOfSight(target) && distanceSquared <= 18.0 * 18.0;
            case MISSILE -> this.hasLineOfSight(target) && distanceSquared <= 28.0 * 28.0;
            case STOMP -> distanceSquared <= 4.5 * 4.5;
            case BACKSTEP -> distanceSquared <= 6.0 * 6.0;
            default -> false;
        };
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(
            MACHINE_GUN, "animation.heavy_mech.machine_gun_fire",
            MISSILE, "animation.heavy_mech.missile_fire",
            STOMP, "animation.heavy_mech.stomp",
            BACKSTEP, "animation.heavy_mech.backstep"
        );
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        if (distanceSquared <= 3.6 * 3.6) {
            return List.of(BACKSTEP, STOMP);
        }
        if (distanceSquared <= 4.5 * 4.5) {
            return List.of(STOMP, MACHINE_GUN);
        }
        if (distanceSquared <= 15.0 * 15.0) {
            return List.of(MACHINE_GUN);
        }
        if (distanceSquared <= 28.0 * 28.0) {
            return List.of(MISSILE);
        }
        return List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return switch (action) {
            case MACHINE_GUN -> 32;
            case MISSILE -> 54;
            case STOMP -> 52;
            case BACKSTEP -> 28;
            default -> 1;
        };
    }

    @Override
    public int getActionCooldown(String action) {
        return switch (action) {
            case MACHINE_GUN -> 46;
            case MISSILE -> 100;
            case STOMP -> 76;
            case BACKSTEP -> 110;
            default -> 20;
        };
    }

    @Override
    public double getCombatHoldDistance() {
        return 10.0;
    }

    @Override
    public double getChaseSpeed() {
        return 0.9;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        if (BACKSTEP.equals(action)) {
            this.backstepProgress = 0.0;
        } else if (MACHINE_GUN.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.5F, 0.7F);
        } else if (MISSILE.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 1.5F, 0.55F);
        }
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (MACHINE_GUN.equals(action) && actionTick >= 4 && actionTick <= 22 && actionTick % 2 == 0) {
            this.damageTarget(target, 1.1F, 18.0 * 18.0);
            this.emitShotParticles(target);
        } else if (MISSILE.equals(action) && this.isMissileLaunchTick(actionTick)) {
            this.launchPodMissile(target, actionTick);
        } else if (STOMP.equals(action) && actionTick == 21) {
            this.damageNearby(3.8, 9.0F, 0.85);
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.8F, 0.65F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 20, 1.5, 0.12, 1.5, 0.04);
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.15, this.getZ(), 12, 1.1, 0.12, 1.1, 0.08);
            }
        } else if (BACKSTEP.equals(action)) {
            this.tickBackstep(actionTick);
        }
    }

    private boolean isMissileLaunchTick(int tick) {
        for (int launchTick : MISSILE_TICKS) {
            if (tick == launchTick) {
                return true;
            }
        }
        return false;
    }

    private void launchPodMissile(LivingEntity target, int tick) {
        int launchIndex = 0;
        while (launchIndex < MISSILE_TICKS.length && MISSILE_TICKS[launchIndex] != tick) {
            launchIndex++;
        }
        Vec3 forward = this.getLookAngle().normalize();
        Vec3 right = new Vec3(forward.z, 0.0, -forward.x).normalize();
        double side = launchIndex % 2 == 0 ? -0.76 : 0.76;
        Vec3 start = this.position().add(0.0, 2.45, 0.0).add(right.scale(side)).add(forward.scale(0.45));
        Vec3 aim = target.getBoundingBox().getCenter().subtract(start).normalize();
        MechProjectile projectile = new MechProjectile(com.hp.mechafauna.registry.ModEntities.MECH_PROJECTILE.get(), this.level(), this, true);
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(aim.x, aim.y, aim.z, 0.9F, 0.0F);
        this.level().addFreshEntity(projectile);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 0.8F + this.random.nextFloat() * 0.2F);
    }

    private void tickBackstep(int actionTick) {
        double animationTime = actionTick / 20.0;
        double nextProgress = Math.max(0.0, Math.min(1.0, (animationTime - 0.18) / (1.04 - 0.18)));
        double distance = nextProgress - this.backstepProgress;
        if (distance > 0.0) {
            Vec3 backward = this.getLookAngle().multiply(-1.0, 0.0, -1.0).normalize();
            this.move(MoverType.SELF, backward.scale(distance));
            this.backstepProgress = nextProgress;
        }
    }
}
