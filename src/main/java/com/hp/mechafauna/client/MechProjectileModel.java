package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.MechProjectile;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MechProjectileModel extends GeoModel<MechProjectile> {
    private static final ResourceLocation MISSILE_MODEL = new ResourceLocation(Mechafauna.MOD_ID, "geo/mech_missile.geo.json");
    private static final ResourceLocation SHELL_MODEL = new ResourceLocation(Mechafauna.MOD_ID, "geo/mech_shell.geo.json");
    private static final ResourceLocation MISSILE_TEXTURE = new ResourceLocation(Mechafauna.MOD_ID, "textures/entity/mech_missile.png");
    private static final ResourceLocation SHELL_TEXTURE = new ResourceLocation(Mechafauna.MOD_ID, "textures/entity/mech_shell.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(Mechafauna.MOD_ID, "animations/mech_projectile.animation.json");

    @Override
    public ResourceLocation getModelResource(MechProjectile entity) {
        return entity.isMissile() ? MISSILE_MODEL : SHELL_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MechProjectile entity) {
        return entity.isMissile() ? MISSILE_TEXTURE : SHELL_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MechProjectile entity) {
        return ANIMATION;
    }

    @Override
    public RenderType getRenderType(MechProjectile entity, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
