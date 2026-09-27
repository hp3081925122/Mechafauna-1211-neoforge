package com.hp.mechafauna.client.model;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.item.MechArmorItem;
import net.minecraft.resources.Identifier;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.model.GeoModel;

public final class MechArmorModel extends GeoModel<MechArmorItem> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(
        Mechafauna.MOD_ID,
        "armor/mech_armor"
    );
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        Mechafauna.MOD_ID,
        "textures/armor/mech_armor.png"
    );
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(
        Mechafauna.MOD_ID,
        "armor/mech_armor"
    );

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(MechArmorItem animatable) {
        return ANIMATION;
    }
}
