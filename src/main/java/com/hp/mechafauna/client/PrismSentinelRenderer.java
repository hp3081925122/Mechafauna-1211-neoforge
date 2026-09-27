package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.PrismSentinel;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class PrismSentinelRenderer extends GeoEntityRenderer<PrismSentinel> {
    static final RenderType LASER = RenderType.create("mechafauna_prism_beam",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(MechRenderStateAccess.POSITION_COLOR_SHADER)
            .setTransparencyState(MechRenderStateAccess.ADDITIVE_TRANSPARENCY)
            .setCullState(MechRenderStateAccess.NO_CULL)
            .setWriteMaskState(MechRenderStateAccess.COLOR_WRITE)
            .setDepthTestState(MechRenderStateAccess.LEQUAL_DEPTH_TEST)
            .createCompositeState(false));

    public PrismSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new MechModel<>("prism_sentinel"));
    }

    @Override
    protected void applyRotations(PrismSentinel entity, PoseStack poseStack, float ageInTicks,
                                  float rotationYaw, float partialTick) {
        super.applyRotations(entity, poseStack, ageInTicks,
            entity.isPerformingAction() ? entity.getLaserModelYaw(partialTick) : rotationYaw, partialTick);
    }

    @Override
    public boolean shouldRender(PrismSentinel entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        if (super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)) {
            return true;
        }
        if (entity.isLaserFiring() && entity.shouldRender(cameraX, cameraY, cameraZ)) {
            PrismSentinel.LaserRay ray = entity.getLaserRay(1.0F);
            return frustum.isVisible(new AABB(ray.origin(), ray.end()).inflate(0.5));
        }
        return false;
    }

    @Override
    public void render(PrismSentinel entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        float time = entity.tickCount + partialTick;
        float charge = entity.getLaserChargePower(partialTick);
        if (charge > 0.0F) {
            renderEnergyGathering(entity, partialTick, poseStack, buffers, time, charge);
        }
        if (!entity.isLaserFiring()) {
            return;
        }
        PrismSentinel.LaserRay ray = entity.getLaserRay(partialTick);
        double length = ray.origin().subtract(ray.end()).length();
        if (length < 0.01) {
            return;
        }
        float power = entity.getLaserPower(partialTick);
        float pulse = (float)(1.0 + 0.045 * Math.sin(time * 0.9));
        Vec3 start = ray.origin().subtract(entity.getPosition(partialTick));
        Vec3 direction = ray.direction();
        Vec3 side = Math.abs(direction.y) > 0.98 ? new Vec3(1, 0, 0)
            : direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(direction).normalize();
        VertexConsumer vertices = buffers.getBuffer(LASER);
        Matrix4f matrix = poseStack.last().pose();
        cylinder(vertices, matrix, start, direction, side, up, length, 0.24 * pulse * power, 0.4F, 0.15F, 1.0F, 0.09F * power);
        cylinder(vertices, matrix, start, direction, side, up, length, 0.12 * pulse * power, 0.08F, 0.78F, 1.0F, 0.32F * power);
        cylinder(vertices, matrix, start, direction, side, up, length, 0.043 * power, 0.8F, 1.0F, 1.0F, 0.85F * power);
        ring(vertices, matrix, start.add(direction.scale(0.025)), side, up, 0.25 * power, 0.035, time * 0.025, power);
        for (int i = 0; i < 4; i++) {
            double distance = ((time * 0.28 + i * 6.0) % 24.0);
            if (distance < length) {
                ring(vertices, matrix, start.add(direction.scale(distance)), side, up,
                    0.16 * power, 0.016, time * 0.015, power * 0.36F);
            }
        }
        if (ray.blockHit()) {
            Vec3 impact = start.add(direction.scale(Math.max(0, length - 0.035)));
            ring(vertices, matrix, impact, side, up, (0.2 + 0.04 * Math.sin(time)) * power,
                0.07, -time * 0.04, power * 0.8F);
            cylinder(vertices, matrix, impact.subtract(direction.scale(0.045)), direction, side, up,
                0.06, 0.11 * power, 0.7F, 1.0F, 1.0F, power * 0.8F);
        }
        for (Vec3 hitPoint : ray.hitPoints()) {
            Vec3 impact = hitPoint.subtract(entity.getPosition(partialTick));
            ring(vertices, matrix, impact, side, up, 0.16 * power, 0.045, -time * 0.04, power * 0.7F);
        }
    }

    private static void renderEnergyGathering(PrismSentinel entity, float partialTick, PoseStack poseStack,
                                              MultiBufferSource buffers, float time, float charge) {
        Vec3 origin = entity.getLaserMuzzle(partialTick).subtract(entity.getPosition(partialTick));
        Vec3 direction = Vec3.directionFromRotation(entity.getLaserPitch(partialTick), entity.getLaserYaw(partialTick));
        Vec3 side = Math.abs(direction.y) > 0.98 ? new Vec3(1, 0, 0)
            : direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(direction).normalize();
        VertexConsumer vertices = buffers.getBuffer(LASER);
        Matrix4f matrix = poseStack.last().pose();
        float pulse = (float)(0.82 + 0.18 * Math.sin(time * 0.55));
        ring(vertices, matrix, origin.subtract(direction.scale(0.08)), side, up,
            0.12 + 0.34 * (1.0 - charge), 0.025, time * 0.04, charge * 0.48F * pulse);
        ring(vertices, matrix, origin.subtract(direction.scale(0.28)), side, up,
            0.10 + 0.25 * (1.0 - charge), 0.018, -time * 0.035, charge * 0.28F * pulse);
        for (int lane = 0; lane < 8; lane++) {
            double startAngle = lane * Math.PI / 4.0;
            Vec3 previous = convergingPoint(origin, direction, side, up, 0.0, startAngle, time);
            for (int segment = 0; segment < 8; segment++) {
                double startProgress = segment / 8.0;
                double endProgress = (segment + 1) / 8.0;
                double angleStart = startAngle + startProgress * 2.4 + time * 0.018;
                double angleEnd = startAngle + endProgress * 2.4 + time * 0.018;
                Vec3 next = convergingPoint(origin, direction, side, up, endProgress, lane * Math.PI / 4.0, time);
                Vec3 tangentStart = side.scale(-Math.sin(angleStart)).add(up.scale(Math.cos(angleStart))).normalize();
                Vec3 tangentEnd = side.scale(-Math.sin(angleEnd)).add(up.scale(Math.cos(angleEnd))).normalize();
                double widthStart = 0.018 * (1.0 - startProgress) + 0.002;
                double widthEnd = 0.018 * (1.0 - endProgress) + 0.002;
                float phase = (time * 0.055F + lane * 0.17F) % 1.0F;
                float middle = (float)((startProgress + endProgress) * 0.5);
                float travelingPulse = Math.max(0.0F, 1.0F - Math.abs(middle - phase) * 7.0F);
                float alpha = charge * (0.16F + 0.48F * travelingPulse) * pulse;
                float red = 0.18F + 0.4F * middle;
                vertex(vertices, matrix, previous.subtract(tangentStart.scale(widthStart)), red, 0.72F, 1.0F, alpha);
                vertex(vertices, matrix, previous.add(tangentStart.scale(widthStart)), red, 0.72F, 1.0F, alpha);
                vertex(vertices, matrix, next.add(tangentEnd.scale(widthEnd)), 0.72F, 0.96F, 1.0F, alpha);
                vertex(vertices, matrix, next.subtract(tangentEnd.scale(widthEnd)), 0.72F, 0.96F, 1.0F, alpha);
                previous = next;
            }
        }
        double coreRadius = 0.045 + 0.035 * charge;
        cylinder(vertices, matrix, origin.subtract(direction.scale(0.025)), direction, side, up,
            0.08, coreRadius, 0.45F, 0.92F, 1.0F, charge * 0.62F * pulse);
    }

    private static Vec3 convergingPoint(Vec3 origin, Vec3 direction, Vec3 side, Vec3 up,
                                        double progress, double startAngle, float time) {
        double angle = startAngle + progress * 2.4 + time * 0.018;
        double radius = 0.52 * (1.0 - progress) + 0.015;
        return origin.subtract(direction.scale(0.28 * (1.0 - progress)))
            .add(side.scale(Math.cos(angle) * radius))
            .add(up.scale(Math.sin(angle) * radius));
    }

    static void cylinder(VertexConsumer vertices, Matrix4f matrix, Vec3 start, Vec3 direction,
                                 Vec3 side, Vec3 up, double length, double radius,
                                 float red, float green, float blue, float alpha) {
        Vec3 end = start.add(direction.scale(length));
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            double b = (i + 1) * Math.PI / 4.0;
            Vec3 offsetA = side.scale(Math.cos(a) * radius).add(up.scale(Math.sin(a) * radius));
            Vec3 offsetB = side.scale(Math.cos(b) * radius).add(up.scale(Math.sin(b) * radius));
            vertex(vertices, matrix, start.add(offsetA), red, green, blue, alpha);
            vertex(vertices, matrix, start.add(offsetB), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(offsetB), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(offsetA), red, green, blue, alpha);
        }
    }

    static void ring(VertexConsumer vertices, Matrix4f matrix, Vec3 center, Vec3 side, Vec3 up,
                             double radius, double thickness, double phase, float alpha) {
        double inner = Math.max(0, radius - thickness);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0 + phase;
            double b = (i + 1) * Math.PI / 8.0 + phase;
            Vec3 va = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            Vec3 vb = side.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
            vertex(vertices, matrix, center.add(va.scale(inner)), 0.25F, 0.85F, 1.0F, alpha);
            vertex(vertices, matrix, center.add(va.scale(radius)), 0.25F, 0.85F, 1.0F, alpha);
            vertex(vertices, matrix, center.add(vb.scale(radius)), 0.25F, 0.85F, 1.0F, alpha);
            vertex(vertices, matrix, center.add(vb.scale(inner)), 0.25F, 0.85F, 1.0F, alpha);
        }
    }

    static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               float red, float green, float blue, float alpha) {
        vertices.vertex(matrix, (float)point.x, (float)point.y, (float)point.z).color(red, green, blue, alpha);
        vertices.endVertex();
    }
}
