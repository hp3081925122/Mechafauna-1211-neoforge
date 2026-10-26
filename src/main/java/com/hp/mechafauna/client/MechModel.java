package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.hp.mechafauna.entity.HeavyMech;
import com.hp.mechafauna.entity.FlameCrawler;
import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.entity.FlyingSaucerWeapon;
import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.entity.MilitaryDrone;
import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.entity.MechanicalTank;
import com.hp.mechafauna.entity.PrismSentinel;
import com.hp.mechafauna.entity.WeaponMount;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public final class MechModel<T extends AnimatedHostileMob & GeoAnimatable> extends GeoModel<T> {
    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public MechModel(String modelName) {
        this.model = new ResourceLocation(Mechafauna.MOD_ID, "geo/" + modelName + ".geo.json");
        this.texture = new ResourceLocation(Mechafauna.MOD_ID, "textures/entity/" + modelName + ".png");
        this.animation = new ResourceLocation(Mechafauna.MOD_ID, "animations/" + modelName + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T entity) {
        return this.model;
    }

    @Override
    public ResourceLocation getTextureResource(T entity) {
        if (entity instanceof MechanicalSpider spider && spider.isDeadOrDying()) {
            return new ResourceLocation(Mechafauna.MOD_ID, "textures/entity/mechanical_spider_overload.png");
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
        if (entity instanceof FlyingSaucerBoss saucer) {
            this.getBone("core_rings").ifPresent(bone -> {
                bone.updateRotation(0.0F, (saucer.tickCount + partialTick) * 0.025F, 0.0F);
                bone.resetStateChanges();
            });
            for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
                boolean destroyed = saucer.isWeaponDestroyed(weapon.ordinal());
                this.getBone(weapon.yawBone()).ifPresent(bone -> {
                    bone.setHidden(destroyed);
                    bone.setChildrenHidden(destroyed);
                    bone.updateRotation(0.0F, (float)Math.toRadians(saucer.getWeaponYawDegrees(weapon)), 0.0F);
                    bone.resetStateChanges();
                });
                this.getBone(weapon.pitchBone()).ifPresent(bone -> {
                    bone.updateRotation(0.0F, 0.0F, (float)Math.toRadians(saucer.getWeaponPitchDegrees(weapon)));
                    bone.resetStateChanges();
                });
                if (weapon.barrelBone() != null) {
                    this.getBone(weapon.barrelBone()).ifPresent(bone -> {
                        bone.setRotX((saucer.tickCount + partialTick) * 0.48F);
                        bone.resetStateChanges();
                    });
                }
            }
        } else if (entity instanceof MechanicalTank tank) {
            this.aimWeapon(entity, WeaponMount.TANK_CANNON, partialTick, tank.getCannonAimOverride(partialTick).orElse(null));
        } else if (entity instanceof MilitaryDrone) {
            this.aimWeapon(entity, WeaponMount.MILITARY_DRONE_GUN, partialTick);
            this.getBone("gun_spin").ifPresent(bone -> {
                bone.setRotZ(bone.getInitialSnapshot().getRotZ() + (entity.tickCount + partialTick) * 0.45F);
                bone.resetStateChanges();
            });
        } else if (entity instanceof MechanicalDrone) {
            this.aimWeapon(entity, WeaponMount.DRONE_GUN, partialTick);
        } else if (entity instanceof HeavyMech mech) {
            this.aimWeapon(entity, WeaponMount.MECH_GUN, partialTick);
            this.aimWeapon(entity, WeaponMount.MECH_MISSILES, partialTick, mech.getMissileAimOverride(partialTick));
        } else if (entity instanceof FlameCrawler) {
            this.aimWeapon(entity, WeaponMount.FLAME_CRAWLER, partialTick);
        } else if (entity instanceof PrismSentinel prism && prism.isPerformingAction()) {
            this.getBone("body").ifPresent(body -> {
                body.updateRotation(body.getRotX() + prism.getBodyTiltRadians(partialTick), body.getRotY(), body.getRotZ());
                body.resetStateChanges();
            });
            WeaponMount.Aim aim = prism.getLaserAim(partialTick);
            this.getBone("lens_yaw").ifPresent(bone -> {
                bone.updateRotation(0.0F, aim.yaw(), 0.0F);
                bone.resetStateChanges();
            });
            this.getBone("lens_pitch").ifPresent(bone -> {
                bone.updateRotation(aim.pitch(), 0.0F, 0.0F);
                bone.resetStateChanges();
            });
        }
    }

    private void aimWeapon(T entity, WeaponMount mount, float partialTick) {
        this.aimWeapon(entity, mount, partialTick, null);
    }

    private void aimWeapon(T entity, WeaponMount mount, float partialTick, Vec3 aimOverride) {
        GeoBone yawBone = this.getBone(mount.yawBone()).orElse(null);
        GeoBone pitchBone = this.getBone(mount.pitchBone()).orElse(null);
        if (yawBone == null || pitchBone == null) {
            return;
        }
        Vec3 targetCenter = aimOverride;
        if (targetCenter == null && entity instanceof MechanicalDrone drone && drone.isOwnedDrone()
            && drone.getControllingPassenger() instanceof Player pilot) {
            targetCenter = pilot.getEyePosition().add(pilot.getLookAngle().scale(48.0));
        } else if (targetCenter == null) {
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
