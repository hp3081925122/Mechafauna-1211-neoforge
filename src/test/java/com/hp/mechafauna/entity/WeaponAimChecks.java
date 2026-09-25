package com.hp.mechafauna.entity;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class WeaponAimChecks {
    private static int checks;

    public static void main(String[] args) {
        Vec3 origin = new Vec3(120, 64, -83);
        for (float bodyYaw : new float[]{0, 90, -90, 180, 179, -179, 370}) {
            for (WeaponMount mount : new WeaponMount[]{WeaponMount.DRONE_GUN, WeaponMount.TANK_CANNON, WeaponMount.MECH_GUN, WeaponMount.MECH_MISSILES, WeaponMount.FLAME_CRAWLER}) {
                double scale = mount == WeaponMount.TANK_CANNON ? 2.0800838 : 1.0;
                Matrix4d base = WeaponMount.modelToWorld(origin, bodyYaw, scale);
                for (int targetYaw = -180; targetYaw <= 180; targetYaw += 30) {
                    for (int height : new int[]{-12, 0, 16}) {
                        Vec3 target = origin.add(-Math.sin(Math.toRadians(targetYaw)) * 24, height, Math.cos(Math.toRadians(targetYaw)) * 24);
                        verifyAim(mount, base, target);
                        Matrix4d animated = new Matrix4d(base).translate(0, 0.3, 1.35)
                            .translate(0, 19, 0).rotateZ(0.15).rotateY(-0.2).rotateX(0.1).translate(0, -19, 0);
                        verifyAim(mount, animated, target);
                    }
                }
            }
        }
        Matrix4d forward = WeaponMount.modelToWorld(Vec3.ZERO, 0, 1);
        Vec3 gun = WeaponMount.transform(forward, WeaponMount.MECH_GUN.muzzle());
        Vec3 missiles = WeaponMount.transform(forward, WeaponMount.MECH_MISSILES.muzzle());
        require(gun.x < 0 && missiles.x > 0, "Gun and missile pod were mirrored");
        require(gun.z > 0 && missiles.z > 0, "Muzzles must be in front of the entity");
        verifyAnimation(WeaponMount.DRONE_GUN, "animation.mechanical_drone.fire", 33);
        verifyAnimation(WeaponMount.TANK_CANNON, "animation.mechanical_tank.fire", 20);
        verifyAnimation(WeaponMount.MECH_GUN, "animation.heavy_mech.machine_gun_fire", 32);
        verifyAnimation(WeaponMount.MECH_MISSILES, "animation.heavy_mech.missile_fire", 54);
        verifyAnimation(WeaponMount.FLAME_CRAWLER, "animation.flame_crawler.flamethrower", 48);
        for (Vec3 velocity : new Vec3[]{new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 0, 1),
            new Vec3(0, 0, -1), new Vec3(1, 1, -1), new Vec3(-1, -1, -1), new Vec3(0, 1, 0), new Vec3(0, -1, 0)}) {
            float yaw = (float)Math.toDegrees(Math.atan2(velocity.x, velocity.z));
            float pitch = (float)Math.toDegrees(Math.atan2(velocity.y, velocity.horizontalDistance()));
            Vector3f nose = WeaponMount.projectileRotation(yaw, pitch).transform(new Vector3f(0, 0, -1));
            Vec3 expected = velocity.normalize();
            require(expected.x * nose.x + expected.y * nose.y + expected.z * nose.z > 0.999999,
                "Projectile nose does not match velocity: " + velocity);
        }
        System.out.println("Weapon aim validation passed: " + checks + " checks");
    }

    private static void verifyAim(WeaponMount mount, Matrix4d parent, Vec3 target) {
        WeaponMount.Aim aim = mount.aim(parent, target);
        Vec3 muzzle = aim.muzzle(mount.muzzle());
        Vector3d forward = aim.transform().transformDirection(new Vector3d(0, 0, -1)).normalize();
        Vec3 desired = target.subtract(muzzle).normalize();
        double agreement = desired.x * forward.x + desired.y * forward.y + desired.z * forward.z;
        require(agreement > 0.999999, mount.yawBone() + " does not point at target: " + agreement);
        WeaponMount.Aim again = mount.aim(parent, target);
        require(aim.yaw() == again.yaw() && aim.pitch() == again.pitch(), "Aim accumulated across frames");
    }

    private static void verifyAnimation(WeaponMount mount, String animation, int ticks) {
        WeaponPose pose = WeaponPose.forModel(mount.modelName());
        for (int tick = 0; tick <= ticks; tick++) {
            Matrix4d parent = pose.parentTransform(mount, WeaponMount.modelToWorld(Vec3.ZERO, 35, 1), animation, tick / 20.0);
            Vec3 target = new Vec3(8, 1, 19);
            verifyAim(mount, parent, target);
            Vec3 muzzle = mount.aim(parent, target).muzzle(pose.muzzle(mount, mount.muzzle(), animation, tick / 20.0));
            require(Double.isFinite(muzzle.x) && Double.isFinite(muzzle.y) && Double.isFinite(muzzle.z), "Invalid animated muzzle");
        }
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
