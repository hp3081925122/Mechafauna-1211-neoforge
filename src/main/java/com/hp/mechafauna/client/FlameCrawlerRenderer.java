package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.FlameCrawler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class FlameCrawlerRenderer extends GeoEntityRenderer<FlameCrawler> {
    public FlameCrawlerRenderer(EntityRendererProvider.Context context) {
        super(context, new MechModel<>("flame_crawler"));
    }

    @Override
    public boolean shouldRender(FlameCrawler entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ)
            || (entity.isFlameWindow() && entity.shouldRender(cameraX, cameraY, cameraZ)
                && frustum.isVisible(entity.getBoundingBox().inflate(9.0)));
    }
}
