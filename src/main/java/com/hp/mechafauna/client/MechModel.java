package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.hp.mechafauna.entity.HeavyMech;
import com.hp.mechafauna.entity.FlameCrawler;
import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.entity.MechanicalTank;
import com.hp.mechafauna.entity.WeaponMount;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class MechModel<T extends AnimatedHostileMob & GeoAnimatable> extends GeoModel<T> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public MechModel(String modelName) {
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
        float partialTick = animationState.getPartialTick();
        if (entity instanceof MechanicalTank) {
            this.aimWeapon(entity, WeaponMount.TANK_CANNON, partialTick);
        } else if (entity instanceof MechanicalDrone) {
            this.aimWeapon(entity, WeaponMount.DRONE_GUN, partialTick);
        } else if (entity instanceof HeavyMech) {
            this.aimWeapon(entity, WeaponMount.MECH_GUN, partialTick);
            this.aimWeapon(entity, WeaponMount.MECH_MISSILES, partialTick);
        } else if (entity instanceof FlameCrawler) {
            this.aimWeapon(entity, WeaponMount.FLAME_CRAWLER, partialTick);
        }
    }

    private void aimWeapon(T entity, WeaponMount mount, float partialTick) {
        GeoBone yawBone = this.getBone(mount.yawBone()).orElse(null);
        GeoBone pitchBone = this.getBone(mount.pitchBone()).orElse(null);
        if (yawBone == null || pitchBone == null) {
            return;
        }
        Vec3 targetCenter = null;
        if (entity instanceof MechanicalDrone drone && drone.isOwnedDrone()
            && drone.getControllingPassenger() instanceof Player pilot) {
            targetCenter = pilot.getEyePosition().add(pilot.getLookAngle().scale(48.0));
        } else {
            LivingEntity target = entity.getCombatTarget();
            if (target != null) {
                targetCenter = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.5, 0.0);
            }
        }
        if (targetCenter == null) {
            if (!(entity instanceof MechanicalTank)) {
                yawBone.setRotY(yawBone.getInitialSnapshot().getRotY());
            }
            pitchBone.setRotX(pitchBone.getInitialSnapshot().getRotX());
        } else {
            float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            Matrix4d parentToWorld = WeaponMount.modelToWorld(entity.getPosition(partialTick), bodyYaw, entity.getModelScale());
            this.applyParentPose(parentToWorld, yawBone.getParent());
            parentToWorld.translate(-yawBone.getPosX(), yawBone.getPosY(), yawBone.getPosZ());
            WeaponMount.Aim aim = mount.aim(parentToWorld, targetCenter);
            yawBone.updateRotation(0.0F, aim.yaw(), 0.0F);
            pitchBone.updateRotation(aim.pitch(), 0.0F, 0.0F);
        }
        yawBone.resetStateChanges();
        pitchBone.resetStateChanges();
    }

    private void applyParentPose(Matrix4d matrix, GeoBone bone) {
        if (bone == null) {
            return;
        }
        this.applyParentPose(matrix, bone.getParent());
        matrix.translate(-bone.getPosX(), bone.getPosY(), bone.getPosZ())
            .translate(bone.getPivotX(), bone.getPivotY(), bone.getPivotZ())
            .rotateZ(bone.getRotZ()).rotateY(bone.getRotY()).rotateX(bone.getRotX())
            .scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
            .translate(-bone.getPivotX(), -bone.getPivotY(), -bone.getPivotZ());
    }
}
