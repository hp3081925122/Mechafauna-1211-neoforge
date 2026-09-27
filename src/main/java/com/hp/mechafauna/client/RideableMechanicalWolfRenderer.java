package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.RideableMechanicalWolf;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.hp.mechafauna.entity.RideableMechanicalWolf;
import com.geckolib.renderer.base.RenderPassInfo;

public final class RideableMechanicalWolfRenderer extends GeoEntityRenderer<RideableMechanicalWolf, MechLivingRenderState> {
    public RideableMechanicalWolfRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.55F;
    }

    @Override
    public MechLivingRenderState createRenderState(RideableMechanicalWolf entity, Void context) {
        return new MechLivingRenderState();
    }

    @Override
    public void extractRenderState(RideableMechanicalWolf entity, MechLivingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
    }

    private static final class Model extends GeoModel<RideableMechanicalWolf> {
        private static final Identifier GEO = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "geo/mechanical_wolf.geo.json");
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/mechanical_wolf.png");
        private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "animations/mechanical_wolf.animation.json");

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "mechanical_wolf");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return TEXTURE;
        }

        @Override
        public Identifier getAnimationResource(RideableMechanicalWolf entity) {
            return Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "mechanical_wolf");
        }
    }
}
