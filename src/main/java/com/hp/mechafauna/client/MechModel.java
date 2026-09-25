package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.hp.mechafauna.entity.HeavyMech;
import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.entity.MechanicalTank;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class MechModel<T extends AnimatedHostileMob & GeoAnimatable> extends GeoModel<T> {
    private final String modelName;
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public MechModel(String modelName) {
        this.modelName = modelName;
        this.model = ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "geo/" + modelName + ".geo.json");
        this.texture = ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/" + modelName + ".png");
        this.animation = ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "animations/" + modelName + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        if (entity instanceof MechanicalSpider spider && spider.isDeadOrDying()) {
            return ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/mechanical_spider_overload.png");
        }
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(T entity) {
        return this.animation;
    }

    @Override
    public void setCustomAnimations(T entity, long instanceId, AnimationState<T> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        float bodyAnimationYaw = this.getBone("body").map(bone -> bone.getRotY()).orElse(0.0F);
        float relativeYaw = Mth.wrapDegrees(entity.getYHeadRot() - entity.yBodyRot) * Mth.DEG_TO_RAD - bodyAnimationYaw;
        if (entity instanceof MechanicalTank) {
            this.getBone("turret").ifPresent(bone -> bone.setRotY(bone.getRotY() + relativeYaw));
        } else if (entity instanceof MechanicalDrone) {
            this.getBone("gun_yaw").ifPresent(bone -> bone.setRotY(bone.getRotY() + relativeYaw));
            this.getBone("gun_pitch").ifPresent(bone -> bone.setRotX(bone.getRotX() - entity.getXRot() * Mth.DEG_TO_RAD));
        } else if (entity instanceof HeavyMech) {
            this.getBone("gun_mount").ifPresent(bone -> bone.setRotY(bone.getRotY() + relativeYaw));
            this.getBone("missile_mount").ifPresent(bone -> bone.setRotY(bone.getRotY() + relativeYaw));
            this.getBone("gun_pitch").ifPresent(bone -> bone.setRotX(bone.getRotX() - entity.getXRot() * Mth.DEG_TO_RAD));
            this.getBone("missile_pitch").ifPresent(bone -> bone.setRotX(bone.getRotX() - entity.getXRot() * Mth.DEG_TO_RAD));
        }
    }
}
