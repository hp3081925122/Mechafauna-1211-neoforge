package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.SaucerLaserStrike;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class SaucerLaserStrikeRenderer extends EntityRenderer<SaucerLaserStrike, SaucerLaserStrikeRenderState> {
    public SaucerLaserStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public SaucerLaserStrikeRenderState createRenderState() {
        return new SaucerLaserStrikeRenderState();
    }

    @Override
    public void extractRenderState(SaucerLaserStrike entity, SaucerLaserStrikeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.laserTick = entity.getLaserTick();
        state.impact = entity.getImpact().subtract(entity.getPosition(partialTick));
    }

    @Override
    public void submit(SaucerLaserStrikeRenderState state, PoseStack pose, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        Vec3 origin = new Vec3(0.0, SaucerLaserStrike.BEAM_HEIGHT, 0.0);
        collector.submitCustomGeometry(pose, VfxRenderTypes.GLOW, (entry, vertices) -> {
            Matrix4f matrix = new Matrix4f(entry.pose());
            FlyingSaucerRenderer.renderGroundLaser(vertices, matrix, origin, state.impact,
                state.laserTick + state.partialTick);
        });
    }
}
