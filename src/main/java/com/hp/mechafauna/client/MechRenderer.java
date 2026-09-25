package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.hp.mechafauna.entity.MechanicalTank;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MechRenderer<T extends AnimatedHostileMob> extends GeoEntityRenderer<T> {
    public MechRenderer(EntityRendererProvider.Context context, String modelName) {
        super(context, new MechModel<>(modelName));
        if ("mechanical_tank".equals(modelName)) {
            this.withScale(MechanicalTank.MODEL_SCALE);
        }
    }
}
