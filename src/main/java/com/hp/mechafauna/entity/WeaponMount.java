package com.hp.mechafauna.entity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Matrix4d;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public record WeaponMount(String modelName, String yawBone, String pitchBone, Vec3 yawPivot, Vec3 pitchPivot, Vec3 muzzle) {
    public static final WeaponMount DRONE_GUN = new WeaponMount("mechanical_drone", "gun_yaw", "gun_pitch",
        point(0, 16.2, -2), point(0, 12.9, -2), point(0, 11.2, -13.7));
    public static final WeaponMount MILITARY_DRONE_GUN = new WeaponMount("military_drone", "gun_yaw", "gun_pitch",
        point(-0.44592, 16.2, -2), point(-0.44592, 12.9, -2), point(-0.44592, 12.9, -12.08));
    public static final WeaponMount TANK_CANNON = new WeaponMount("mechanical_tank", "turret", "cannon_pitch",
        point(0, 8.95, 1), point(0, 11.4, -4.9), point(0, 11.4, -22.95));
    public static final WeaponMount MECH_GUN = new WeaponMount("heavy_mech", "gun_mount", "gun_pitch",
        point(-13.8, 38.3, 1), point(-21, 38.3, -1), point(-21, 38.3, -22));
    public static final WeaponMount MECH_MISSILES = new WeaponMount("heavy_mech", "missile_mount", "missile_pitch",
        point(13.8, 38.5, 0.6), point(21, 38.5, -1.8), point(21, 38.5, -10.8));
    public static final WeaponMount FLAME_CRAWLER = new WeaponMount("flame_crawler", "sprayer_yaw", "sprayer_pitch",
        point(0, 10, -8.8), point(0, 10, -10), point(0, 9, -24.15));
    public static final WeaponMount PRISM_LENS = new WeaponMount("prism_sentinel", "lens_yaw", "lens_pitch",
        point(0, 12.15, -3.06), point(0, 12.15, -3.06), point(0, 12.15, -5.13));
    public static Vec3 point(double x, double y, double z) {
        return new Vec3(-x, y, z);
    }

    public static Matrix4d modelToWorld(Vec3 position, float bodyYaw, double scale) {
        return new Matrix4d().translate(position.x, position.y, position.z)
            .rotateY(Math.toRadians(180.0 - bodyYaw)).scale(scale / 16.0).translate(0, 0.16, 0);
    }

    public static Quaternionf projectileRotation(float yaw, float pitch) {
        return new Quaternionf().rotationY((float)Math.toRadians(180.0 + yaw)).rotateX((float)Math.toRadians(pitch));
    }

    public static Vec3 transform(Matrix4d matrix, Vec3 point) {
        Vector3d result = matrix.transformPosition(new Vector3d(point.x, point.y, point.z));
        return new Vec3(result.x, result.y, result.z);
    }

    public Aim aim(Matrix4d parentToWorld, Vec3 target) {
        Vec3 localTarget = transform(new Matrix4d(parentToWorld).invert(), target);
        Vec3 horizontal = localTarget.subtract(this.yawPivot);
        double radius = horizontal.horizontalDistance();
        double yaw = radius < 1.0E-6 ? 0.0 : Math.atan2(-horizontal.x, -horizontal.z)
            + Math.asin(Mth.clamp((this.muzzle.x - this.yawPivot.x) / radius, -1.0, 1.0));
        Matrix4d yawTransform = new Matrix4d().translate(this.yawPivot.x, this.yawPivot.y, this.yawPivot.z)
            .rotateY(yaw).translate(-this.yawPivot.x, -this.yawPivot.y, -this.yawPivot.z);
        Vec3 vertical = transform(new Matrix4d(yawTransform).invert(), localTarget).subtract(this.pitchPivot);
        double verticalRadius = Math.hypot(vertical.y, vertical.z);
        double pitch = verticalRadius < 1.0E-6 ? 0.0 : Math.atan2(vertical.y, -vertical.z)
            - Math.asin(Mth.clamp((this.muzzle.y - this.pitchPivot.y) / verticalRadius, -1.0, 1.0));
        Matrix4d transform = new Matrix4d(parentToWorld).mul(yawTransform)
            .translate(this.pitchPivot.x, this.pitchPivot.y, this.pitchPivot.z).rotateX(pitch)
            .translate(-this.pitchPivot.x, -this.pitchPivot.y, -this.pitchPivot.z);
        return new Aim((float)yaw, (float)pitch, transform);
    }

    public record Aim(float yaw, float pitch, Matrix4d transform) {
        public Vec3 muzzle(Vec3 point) {
            return WeaponMount.transform(this.transform, point);
        }
    }
}
