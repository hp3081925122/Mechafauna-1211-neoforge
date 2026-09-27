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

public final class HeavyMech extends MechanicalMob {
    private static final String MACHINE_GUN = "machine_gun";
    private static final String MISSILE = "missile";
    private static final String STOMP = "stomp";
    private static final String BACKSTEP = "backstep";
    private static final int[] MISSILE_TICKS = {8, 12, 16, 21, 25, 30};
    private static final int[] BACKSTEP_ATTACK_TICKS = {4, 8, 12, 16, 20, 24};
    private static final int[][] BACKSTEP_MOVE_WINDOWS = {{2, 7}, {11, 16}, {20, 25}};
    private static final Vec3[] MACHINE_GUN_MUZZLES = {
        WeaponMount.point(-19.7, 40.55, -22),
        WeaponMount.point(-22.3, 40.55, -22),
        WeaponMount.point(-18.4, 38.3, -22),
        WeaponMount.point(-23.6, 38.3, -22),
        WeaponMount.point(-19.7, 36.05, -22),
        WeaponMount.point(-22.3, 36.05, -22)
    };
    private static final Vec3[] MISSILE_MUZZLES = {
        WeaponMount.point(23.55, 42.5, -10.8),
        WeaponMount.point(18.45, 42.5, -10.8),
        WeaponMount.point(23.55, 38.5, -10.8),
        WeaponMount.point(18.45, 38.5, -10.8),
        WeaponMount.point(23.55, 34.5, -10.8),
        WeaponMount.point(18.45, 34.5, -10.8)
    };
    private static final double KNOCKBACK_RESISTANCE = 0.85;
    private int backstepMoveIndex;
    private double backstepMoveProgress;

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
            case MISSILE -> this.hasLineOfSight(target);
            case STOMP -> distanceSquared <= 4.5 * 4.5;
            case BACKSTEP -> true;
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
        return List.of(MISSILE);
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
            this.backstepMoveIndex = 0;
            this.backstepMoveProgress = 0.0;
        } else if (MACHINE_GUN.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.5F, 0.7F);
        } else if (MISSILE.equals(action)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.HOSTILE, 1.5F, 0.55F);
        }
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (MACHINE_GUN.equals(action) && actionTick >= 4 && actionTick <= 22 && actionTick % 2 == 0) {
            this.fireMachineGunVolley(target);
        } else if (MISSILE.equals(action) && this.isMissileLaunchTick(actionTick)) {
            this.launchPodMissileAtTick(target, actionTick);
        } else if (STOMP.equals(action) && actionTick == 21) {
            this.damageNearby(3.8, 9.0F, 0.85);
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, 0.65F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1, this.getZ(), 20, 1.5, 0.12, 1.5, 0.04);
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.15, this.getZ(), 12, 1.1, 0.12, 1.1, 0.08);
            }
        } else if (BACKSTEP.equals(action)) {
            this.tickBackstep(actionTick, target);
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

    private void launchPodMissileAtTick(LivingEntity target, int tick) {
        int launchIndex = 0;
        while (launchIndex < MISSILE_TICKS.length && MISSILE_TICKS[launchIndex] != tick) {
            launchIndex++;
        }
        if (launchIndex < MISSILE_MUZZLES.length) {
            this.launchPodMissileFromIndex(target, launchIndex);
        }
    }

    private void launchPodMissileFromIndex(LivingEntity target, int launchIndex) {
        this.launchProjectile(target, WeaponMount.MECH_MISSILES, MISSILE_MUZZLES[launchIndex], true, false, 1.65, 0.0F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 0.8F + this.random.nextFloat() * 0.2F);
    }

    private void fireMachineGunVolley(LivingEntity target) {
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        double forwardAngle = this.random.nextDouble() * Math.PI * 2.0;
        double radius = 0.8 + this.random.nextDouble() * 1.4;
        Vec3 forward = targetCenter.subtract(this.getBoundingBox().getCenter()).normalize();
        if (forward.lengthSqr() < 1.0E-6) {
            forward = this.getLookAngle().normalize();
        }
        Vec3 reference = Math.abs(forward.y) > 0.95 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(reference).normalize();
        Vec3 up = right.cross(forward).normalize();
        for (int index = 0; index < MACHINE_GUN_MUZZLES.length; index++) {
            Vec3 muzzle = MACHINE_GUN_MUZZLES[index];
            double angle = forwardAngle + index * (Math.PI * 2.0 / MACHINE_GUN_MUZZLES.length);
            Vec3 aimPoint = targetCenter.add(right.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            this.launchCyanLaser(target, WeaponMount.MECH_GUN, muzzle, aimPoint, 0.72, 1.1F);
        }
    }

    private void tickBackstep(int actionTick, LivingEntity target) {
        while (this.backstepMoveIndex < BACKSTEP_MOVE_WINDOWS.length) {
            int[] window = BACKSTEP_MOVE_WINDOWS[this.backstepMoveIndex];
            if (actionTick < window[0]) {
                break;
            }
            double phase = Math.min(1.0, (double)(actionTick - window[0] + 1) / (window[1] - window[0] + 1));
            double easedProgress = phase * phase * (3.0 - 2.0 * phase);
            double distance = easedProgress - this.backstepMoveProgress;
            if (distance > 0.0) {
                Vec3 backward = this.getLookAngle().multiply(-1.0, 0.0, -1.0).normalize();
                this.move(MoverType.SELF, backward.scale(distance));
                this.backstepMoveProgress = easedProgress;
            }
            if (phase >= 1.0) {
                this.backstepMoveIndex++;
                this.backstepMoveProgress = 0.0;
            } else {
                break;
            }
        }

        if (this.isBackstepAttackTick(actionTick)) {
            if (this.random.nextInt(4) == 0) {
                this.launchPodMissileFromIndex(target, this.random.nextInt(MISSILE_MUZZLES.length));
            } else {
                this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 1.0F, 0.65F);
                this.fireMachineGunVolley(target);
            }
        }
    }

    private boolean isBackstepAttackTick(int actionTick) {
        for (int attackTick : BACKSTEP_ATTACK_TICKS) {
            if (actionTick == attackTick) {
                return true;
            }
        }
        return false;
    }
}
