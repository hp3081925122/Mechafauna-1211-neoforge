package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.hp.mechafauna.entity.MechanicalSpider;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.geckolib.renderer.GeoEntityRenderer;

public class MechRenderer<T extends AnimatedHostileMob> extends GeoEntityRenderer<T, MechLivingRenderState> {
    public MechRenderer(EntityRendererProvider.Context context, String modelName) {
        super(context, new MechModel<>(modelName));
    }

    @Override
    public MechLivingRenderState createRenderState(T entity, Void context) {
        return new MechLivingRenderState();
    }

    @Override
    public void extractRenderState(T entity, MechLivingRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState state) {
        return state instanceof MechLivingRenderState livingState && livingState.entity instanceof MechanicalSpider
            ? 0.0F : super.getDeathMaxRotation(state);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<MechLivingRenderState> pass, BoneSnapshots bones) {
        ((MechModel<T>)this.model).applyBoneTransforms(pass, bones);
    }
}
