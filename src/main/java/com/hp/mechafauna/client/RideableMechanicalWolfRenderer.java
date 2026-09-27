package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.RideableMechanicalWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RideableMechanicalWolfRenderer extends GeoEntityRenderer<RideableMechanicalWolf> {
    public RideableMechanicalWolfRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.55F;
    }

    private static final class Model extends GeoModel<RideableMechanicalWolf> {
        private static final ResourceLocation GEO = new ResourceLocation(Mechafauna.MOD_ID, "geo/mechanical_wolf.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(Mechafauna.MOD_ID, "textures/entity/mechanical_wolf.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation(Mechafauna.MOD_ID, "animations/mechanical_wolf.animation.json");

        @Override
        public ResourceLocation getModelResource(RideableMechanicalWolf entity) {
            return GEO;
        }

        @Override
        public ResourceLocation getTextureResource(RideableMechanicalWolf entity) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(RideableMechanicalWolf entity) {
            return ANIMATION;
        }
    }
}
