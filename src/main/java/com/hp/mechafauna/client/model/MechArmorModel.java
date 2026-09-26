package com.hp.mechafauna.client.model;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.item.MechArmorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class MechArmorModel extends GeoModel<MechArmorItem> {
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
        Mechafauna.MOD_ID,
        "geo/armor/mech_armor.geo.json"
    );
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Mechafauna.MOD_ID,
        "textures/armor/mech_armor.png"
    );
    private static final ResourceLocation ANIMATION = ResourceLocation.fromNamespaceAndPath(
        Mechafauna.MOD_ID,
        "animations/armor/mech_armor.animation.json"
    );

    @Override
    public ResourceLocation getModelResource(MechArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MechArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MechArmorItem animatable) {
        return ANIMATION;
    }
}
