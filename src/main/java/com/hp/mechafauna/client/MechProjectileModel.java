package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.MechProjectile;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public final class MechProjectileModel extends GeoModel<MechProjectile> {
    private static final Identifier MISSILE_MODEL = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "mech_missile");
    private static final Identifier SHELL_MODEL = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "mech_shell");
    private static final Identifier MISSILE_TEXTURE = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/mech_missile.png");
    private static final Identifier SHELL_TEXTURE = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/mech_shell.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "mech_projectile");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        MechProjectile entity = ((MechEntityRenderState)renderState).projectile;
        return entity.isMissile() ? MISSILE_MODEL : SHELL_MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        MechProjectile entity = ((MechEntityRenderState)renderState).projectile;
        return entity.isMissile() ? MISSILE_TEXTURE : SHELL_TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MechProjectile entity) {
        return ANIMATION;
    }
}
