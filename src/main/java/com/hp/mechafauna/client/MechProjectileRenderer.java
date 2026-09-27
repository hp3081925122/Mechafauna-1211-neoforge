package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.MechProjectile;
import com.hp.mechafauna.entity.WeaponMount;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import org.joml.Matrix4f;

public final class MechProjectileRenderer extends GeoEntityRenderer<MechProjectile> {
    public MechProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, new MechProjectileModel());
    }

    @Override
    public void render(MechProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isLaserTracer()) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        poseStack.pushPose();
        try {
            float projectileYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
            float projectilePitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
            poseStack.mulPose(WeaponMount.projectileRotation(projectileYaw, projectilePitch));
            VertexConsumer vertices = bufferSource.getBuffer(RenderType.debugQuads());
            Matrix4f matrix = poseStack.last().pose();
            if (entity.isCyanLaserTracer()) {
                renderLaserLayer(vertices, matrix, 0.11F, 0.48F, 0.0F, 0.62F, 0.72F, 0.34F);
                renderLaserLayer(vertices, matrix, 0.035F, 0.46F, 0.63F, 1.0F, 0.96F, 0.98F);
            } else if (entity.isPrismLaserTracer()) {
                renderLaserLayer(vertices, matrix, 0.11F, 0.48F, 0.0F, 0.62F, 0.72F, 0.34F);
                renderLaserLayer(vertices, matrix, 0.035F, 0.46F, 0.63F, 1.0F, 0.96F, 0.98F);
            } else {
                renderLaserLayer(vertices, matrix, 0.09F, 0.22F, 1.0F, 0.015F, 0.0F, 0.26F);
                renderLaserLayer(vertices, matrix, 0.028F, 0.19F, 1.0F, 0.12F, 0.08F, 0.95F);
            }
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    protected void applyRotations(MechProjectile entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        float projectileYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float projectilePitch = Mth.rotLerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(WeaponMount.projectileRotation(projectileYaw, projectilePitch));
    }

    private static void renderLaserLayer(VertexConsumer vertices, Matrix4f matrix, float halfWidth, float halfLength, float red, float green, float blue, float alpha) {
        vertices.vertex(matrix, -halfWidth, 0.0F, -halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, halfWidth, 0.0F, -halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, halfWidth, 0.0F, halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, -halfWidth, 0.0F, halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, 0.0F, -halfWidth, -halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, 0.0F, halfWidth, -halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, 0.0F, halfWidth, halfLength).color(red, green, blue, alpha).endVertex();
        vertices.vertex(matrix, 0.0F, -halfWidth, halfLength).color(red, green, blue, alpha).endVertex();
    }
}
