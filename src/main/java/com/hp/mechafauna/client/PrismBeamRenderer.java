package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.PrismBeam;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PrismBeamRenderer extends EntityRenderer<PrismBeam> {
    public PrismBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PrismBeam entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        float fade = 0.82F + 0.18F * (0.5F + 0.5F * Mth.sin(age * 0.5F));
        Vec3 direction = Vec3.directionFromRotation(entity.getBeamPitch(), entity.getBeamYaw());
        Vec3 side = Math.abs(direction.y) > 0.98 ? new Vec3(1, 0, 0)
            : direction.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(direction).normalize();
        VertexConsumer vertices = buffers.getBuffer(PrismSentinelRenderer.LASER);
        Matrix4f matrix = poseStack.last().pose();
        double length = entity.getBeamLength();
        PrismSentinelRenderer.cylinder(vertices, matrix, Vec3.ZERO, direction, side, up, length,
            0.24 * fade, 0.15F, 0.4F, 1.0F, 0.12F * fade);
        PrismSentinelRenderer.cylinder(vertices, matrix, Vec3.ZERO, direction, side, up, length,
            0.12 * fade, 0.08F, 0.78F, 1.0F, 0.36F * fade);
        PrismSentinelRenderer.cylinder(vertices, matrix, Vec3.ZERO, direction, side, up, length,
            0.043 * fade, 0.8F, 1.0F, 1.0F, 0.92F * fade);
        PrismSentinelRenderer.ring(vertices, matrix, direction.scale(length), side, up,
            0.18 * fade, 0.05, age * 0.04, 0.75F * fade);
    }

    @Override
    public ResourceLocation getTextureLocation(PrismBeam entity) {
        return new ResourceLocation("mechafauna", "textures/block/prism_core.png");
    }
}
