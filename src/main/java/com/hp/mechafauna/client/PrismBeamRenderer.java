package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.PrismBeam;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class PrismBeamRenderer extends EntityRenderer<PrismBeam, PrismBeamRenderState> {
    public PrismBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public PrismBeamRenderState createRenderState() {
        return new PrismBeamRenderState();
    }

    @Override
    public void extractRenderState(PrismBeam entity, PrismBeamRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.age = entity.tickCount + partialTick;
        state.direction = Vec3.directionFromRotation(entity.getBeamPitch(), entity.getBeamYaw());
        state.length = entity.getBeamLength();
    }

    @Override
    public void submit(PrismBeamRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (entry, vertices) -> {
            Matrix4f matrix = new Matrix4f(entry.pose());
            float fade = 0.82F + 0.18F * (0.5F + 0.5F * Mth.sin(state.age * 0.5F));
            Vec3 direction = state.direction;
            Vec3 side = Math.abs(direction.y) > 0.98 ? new Vec3(1, 0, 0)
                : direction.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 up = side.cross(direction).normalize();
            PrismSentinelRenderer.cylinder(vertices, matrix, Vec3.ZERO, direction, side, up, state.length,
                0.24 * fade, 0.15F, 0.4F, 1.0F, 0.12F * fade);
            PrismSentinelRenderer.cylinder(vertices, matrix, Vec3.ZERO, direction, side, up, state.length,
                0.12 * fade, 0.08F, 0.78F, 1.0F, 0.36F * fade);
            PrismSentinelRenderer.cylinder(vertices, matrix, Vec3.ZERO, direction, side, up, state.length,
                0.043 * fade, 0.8F, 1.0F, 1.0F, 0.92F * fade);
            PrismSentinelRenderer.ring(vertices, matrix, direction.scale(state.length), side, up,
                0.18 * fade, 0.05, state.age * 0.04, 0.75F * fade);
        });
    }
}
