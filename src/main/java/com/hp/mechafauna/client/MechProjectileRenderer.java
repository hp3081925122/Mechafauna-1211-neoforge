package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.MechProjectile;
import com.hp.mechafauna.entity.WeaponMount;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.GeoEntityRenderer;
import org.joml.Matrix4f;

public final class MechProjectileRenderer extends GeoEntityRenderer<MechProjectile, MechEntityRenderState> {
    public MechProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, new MechProjectileModel());
    }

    @Override
    public MechEntityRenderState createRenderState(MechProjectile entity, Void context) {
        return new MechEntityRenderState();
    }

    @Override
    public void extractRenderState(MechProjectile entity, MechEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.projectile = entity;
        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.pitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
        state.laser = entity.isLaserTracer();
        state.cyanLaser = entity.isCyanLaserTracer();
        state.prismLaser = entity.isPrismLaserTracer();
    }

    @Override
    protected void applyRotations(RenderPassInfo<MechEntityRenderState> pass, PoseStack pose, float nativeScale) {
        pose.mulPose(WeaponMount.projectileRotation(pass.renderState().yaw, pass.renderState().pitch));
    }

    @Override
    public void submit(MechEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.laser) {
            super.submit(state, pose, collector, camera);
            return;
        }
        pose.pushPose();
        try {
            pose.mulPose(WeaponMount.projectileRotation(state.yaw, state.pitch));
            collector.submitCustomGeometry(pose, RenderTypes.debugQuads(), (entry, vertices) -> {
                Matrix4f matrix = entry.pose();
                if (state.cyanLaser || state.prismLaser) {
                    renderLaserLayer(vertices, matrix, 0.11F, 0.48F, 0.0F, 0.62F, 0.72F, 0.34F);
                    renderLaserLayer(vertices, matrix, 0.035F, 0.46F, 0.63F, 1.0F, 0.96F, 0.98F);
                } else {
                    renderLaserLayer(vertices, matrix, 0.09F, 0.22F, 1.0F, 0.015F, 0.0F, 0.26F);
                    renderLaserLayer(vertices, matrix, 0.028F, 0.19F, 1.0F, 0.12F, 0.08F, 0.95F);
                }
            });
        } finally {
            pose.popPose();
        }
    }

    private static void renderLaserLayer(VertexConsumer vertices, Matrix4f matrix, float halfWidth, float halfLength, float red, float green, float blue, float alpha) {
        vertices.addVertex(matrix, -halfWidth, 0.0F, -halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, halfWidth, 0.0F, -halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, halfWidth, 0.0F, halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, -halfWidth, 0.0F, halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, 0.0F, -halfWidth, -halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, 0.0F, halfWidth, -halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, 0.0F, halfWidth, halfLength).setColor(red, green, blue, alpha);
        vertices.addVertex(matrix, 0.0F, -halfWidth, halfLength).setColor(red, green, blue, alpha);
    }
}
