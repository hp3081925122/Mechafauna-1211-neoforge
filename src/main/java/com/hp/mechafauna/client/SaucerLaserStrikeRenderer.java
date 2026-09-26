package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.SaucerLaserStrike;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class SaucerLaserStrikeRenderer extends EntityRenderer<SaucerLaserStrike> {
    public SaucerLaserStrikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SaucerLaserStrike entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        Vec3 position = entity.getPosition(partialTick);
        Vec3 origin = new Vec3(0.0, SaucerLaserStrike.BEAM_HEIGHT, 0.0);
        Vec3 impact = entity.getImpact().subtract(position);
        FlyingSaucerRenderer.renderGroundLaser(buffers.getBuffer(FlyingSaucerRenderer.GLOW), poseStack.last().pose(),
            origin, impact, entity.getLaserTick() + partialTick);
    }

    @Override
    public ResourceLocation getTextureLocation(SaucerLaserStrike entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
