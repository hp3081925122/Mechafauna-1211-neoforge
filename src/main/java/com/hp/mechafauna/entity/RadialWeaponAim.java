package com.hp.mechafauna.entity;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;

public final class RadialWeaponAim {
    private RadialWeaponAim() {
    }

    public static Angles aim(Matrix4d parentToWorld, Vec3 yawPivot, Vec3 pitchPivot, Vec3 muzzle, Vec3 target) {
        Vec3 localTarget = WeaponMount.transform(new Matrix4d(parentToWorld).invert(), target);
        double dx = localTarget.x - yawPivot.x;
        double dz = localTarget.z - yawPivot.z;
        double yaw = Math.atan2(-dz, dx);
        Matrix4d yawTransform = rotationY(yawPivot, yaw);
        Vec3 afterYaw = WeaponMount.transform(new Matrix4d(yawTransform).invert(), localTarget);
        double vertical = afterYaw.y - pitchPivot.y;
        double horizontal = Math.hypot(afterYaw.x - pitchPivot.x, afterYaw.z - pitchPivot.z);
        double radius = Math.hypot(horizontal, vertical);
        double muzzleOffset = Math.clamp((muzzle.y - pitchPivot.y) / Math.max(radius, 1.0E-6), -1.0, 1.0);
        double pitch = Math.atan2(vertical, horizontal) - Math.asin(muzzleOffset);
        return new Angles((float)Math.toDegrees(yaw), (float)Math.toDegrees(pitch));
    }

    public static Matrix4d transform(Matrix4d parentToWorld, Vec3 yawPivot, Vec3 pitchPivot, float yawDegrees, float pitchDegrees) {
        return new Matrix4d(parentToWorld)
            .mul(rotationY(yawPivot, Math.toRadians(yawDegrees)))
            .translate(pitchPivot.x, pitchPivot.y, pitchPivot.z)
            .rotateZ(Math.toRadians(pitchDegrees))
            .translate(-pitchPivot.x, -pitchPivot.y, -pitchPivot.z);
    }

    private static Matrix4d rotationY(Vec3 pivot, double angle) {
        return new Matrix4d().translate(pivot.x, pivot.y, pivot.z)
            .rotateY(angle).translate(-pivot.x, -pivot.y, -pivot.z);
    }

    public record Angles(float yawDegrees, float pitchDegrees) {
    }
}
