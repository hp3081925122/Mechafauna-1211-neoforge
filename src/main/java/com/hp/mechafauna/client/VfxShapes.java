package com.hp.mechafauna.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class VfxShapes {
    private static final Vec3 SIDE = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private VfxShapes() {
    }

    public static void dashedRing(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, double thickness, int dashes,
                                  double phase, float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        double outer = radius + thickness * 0.5;
        double inner = radius - thickness * 0.5;
        double step = Math.PI * 2.0 / dashes;
        for (int i = 0; i < dashes; i++) {
            // 每段只画前 60%，形成断续的阵纹
            double a = i * step + phase;
            double b = a + step * 0.6;
            Vec3 va = SIDE.scale(Math.cos(a)).add(FORWARD.scale(Math.sin(a)));
            Vec3 vb = SIDE.scale(Math.cos(b)).add(FORWARD.scale(Math.sin(b)));
            vertex(vertices, matrix, center.add(va.scale(inner)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(va.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(inner)), red, green, blue, alpha);
        }
    }

    public static void cylinder(VertexConsumer vertices, Matrix4f matrix, Vec3 start, Vec3 direction, double length, double radius,
                                float red, float green, float blue, float alpha) {
        Vec3 end = start.add(direction.scale(length));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            double b = (i + 1) * Math.PI / 6.0;
            Vec3 offsetA = new Vec3(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
            Vec3 offsetB = new Vec3(Math.cos(b) * radius, 0.0, Math.sin(b) * radius);
            vertex(vertices, matrix, start.add(offsetA), red, green, blue, alpha);
            vertex(vertices, matrix, start.add(offsetB), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(offsetB), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(offsetA), red, green, blue, alpha);
        }
    }

    public static void flatRing(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, double thickness, int segments,
                                double phase, float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F || radius <= 0.0) {
            return;
        }
        double outer = radius + thickness * 0.5;
        double inner = Math.max(0.0, radius - thickness * 0.5);
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments + phase;
            double b = (i + 1) * Math.PI * 2.0 / segments + phase;
            Vec3 va = SIDE.scale(Math.cos(a)).add(FORWARD.scale(Math.sin(a)));
            Vec3 vb = SIDE.scale(Math.cos(b)).add(FORWARD.scale(Math.sin(b)));
            // 内缘透明、外缘不透明形成柔和边
            vertex(vertices, matrix, center.add(va.scale(inner)), red, green, blue, alpha * 0.25F);
            vertex(vertices, matrix, center.add(va.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(inner)), red, green, blue, alpha * 0.25F);
        }
    }

    public static void disc(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, int segments,
                            float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            // 中心亮、边缘透明的地面光斑
            vertex(vertices, matrix, center, red, green, blue, alpha);
            vertex(vertices, matrix, center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius), red, green, blue, 0.0F);
            vertex(vertices, matrix, center.add(Math.cos(b) * radius, 0.0, Math.sin(b) * radius), red, green, blue, 0.0F);
            vertex(vertices, matrix, center, red, green, blue, alpha);
        }
    }

    public static void wall(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, double height, int segments,
                            float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            Vec3 bottomA = center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
            Vec3 bottomB = center.add(Math.cos(b) * radius, 0.0, Math.sin(b) * radius);
            // 底部亮、顶部透明的竖直光带
            vertex(vertices, matrix, bottomA, red, green, blue, alpha);
            vertex(vertices, matrix, bottomB, red, green, blue, alpha);
            vertex(vertices, matrix, bottomB.add(0.0, height, 0.0), red, green, blue, 0.0F);
            vertex(vertices, matrix, bottomA.add(0.0, height, 0.0), red, green, blue, 0.0F);
        }
    }

    public static void ribbon(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to, double width,
                              float red, float green, float blue, float alpha) {
        Vec3 along = to.subtract(from);
        if (alpha <= 0.003F || along.lengthSqr() < 1.0E-8) {
            return;
        }
        // 两片互相垂直的面片，斜视角也可见
        Vec3 horizontal = along.cross(UP);
        if (horizontal.lengthSqr() < 1.0E-8) {
            horizontal = SIDE;
        }
        Vec3 first = horizontal.normalize().scale(width);
        Vec3 second = along.cross(first).normalize().scale(width);
        for (Vec3 offset : new Vec3[] {first, second}) {
            vertex(vertices, matrix, from.subtract(offset), red, green, blue, alpha);
            vertex(vertices, matrix, from.add(offset), red, green, blue, alpha);
            vertex(vertices, matrix, to.add(offset), red, green, blue, alpha);
            vertex(vertices, matrix, to.subtract(offset), red, green, blue, alpha);
        }
    }

    public static void tube(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to, double startRadius, double endRadius,
                            int segments, float red, float green, float blue, float startAlpha, float endAlpha) {
        Vec3 along = to.subtract(from);
        if ((startAlpha <= 0.003F && endAlpha <= 0.003F) || along.lengthSqr() < 1.0E-8) {
            return;
        }
        // 任意方向的圆管，两端半径与透明度可分别设置
        Vec3 axis = along.normalize();
        Vec3 reference = Math.abs(axis.y) > 0.95 ? SIDE : UP;
        Vec3 right = axis.cross(reference).normalize();
        Vec3 up = right.cross(axis).normalize();
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            Vec3 offsetA = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3 offsetB = right.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
            vertex(vertices, matrix, from.add(offsetA.scale(startRadius)), red, green, blue, startAlpha);
            vertex(vertices, matrix, from.add(offsetB.scale(startRadius)), red, green, blue, startAlpha);
            vertex(vertices, matrix, to.add(offsetB.scale(endRadius)), red, green, blue, endAlpha);
            vertex(vertices, matrix, to.add(offsetA.scale(endRadius)), red, green, blue, endAlpha);
        }
    }

    public static void axisRing(VertexConsumer vertices, Matrix4f matrix, Vec3 center, Vec3 axis, double radius, double thickness,
                                int segments, double phase, float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F || radius <= 0.0 || axis.lengthSqr() < 1.0E-8) {
            return;
        }
        // 垂直于指定轴的圆环，用于光束上的螺旋环和炮口光圈
        Vec3 normal = axis.normalize();
        Vec3 reference = Math.abs(normal.y) > 0.95 ? SIDE : UP;
        Vec3 right = normal.cross(reference).normalize();
        Vec3 up = right.cross(normal).normalize();
        double outer = radius + thickness * 0.5;
        double inner = Math.max(0.0, radius - thickness * 0.5);
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments + phase;
            double b = (i + 1) * Math.PI * 2.0 / segments + phase;
            Vec3 va = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3 vb = right.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
            vertex(vertices, matrix, center.add(va.scale(inner)), red, green, blue, alpha * 0.25F);
            vertex(vertices, matrix, center.add(va.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(inner)), red, green, blue, alpha * 0.25F);
        }
    }

    public static void sphere(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, int rings, int segments,
                              float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F || radius <= 0.0) {
            return;
        }
        // 经纬分段的球面，边缘略亮以模拟菲涅尔光晕
        for (int ring = 0; ring < rings; ring++) {
            double theta0 = Math.PI * ring / rings;
            double theta1 = Math.PI * (ring + 1) / rings;
            float alpha0 = alpha * (0.55F + 0.45F * (float)Math.abs(Math.cos(theta0)));
            float alpha1 = alpha * (0.55F + 0.45F * (float)Math.abs(Math.cos(theta1)));
            for (int segment = 0; segment < segments; segment++) {
                double phi0 = Math.PI * 2.0 * segment / segments;
                double phi1 = Math.PI * 2.0 * (segment + 1) / segments;
                vertex(vertices, matrix, spherePoint(center, radius, theta0, phi0), red, green, blue, alpha0);
                vertex(vertices, matrix, spherePoint(center, radius, theta0, phi1), red, green, blue, alpha0);
                vertex(vertices, matrix, spherePoint(center, radius, theta1, phi1), red, green, blue, alpha1);
                vertex(vertices, matrix, spherePoint(center, radius, theta1, phi0), red, green, blue, alpha1);
            }
        }
    }

    public static Vec3 spherePoint(Vec3 center, double radius, double theta, double phi) {
        double ring = Math.sin(theta) * radius;
        return center.add(Math.cos(phi) * ring, Math.cos(theta) * radius, Math.sin(phi) * ring);
    }

    public static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                              float red, float green, float blue, float alpha) {
        vertices.vertex(matrix, (float)point.x, (float)point.y, (float)point.z).color(red, green, blue, alpha);
        vertices.endVertex();
    }
}
