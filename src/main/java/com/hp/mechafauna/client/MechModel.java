package com.hp.mechafauna.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.AnimatedHostileMob;
import com.hp.mechafauna.entity.HeavyMech;
import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.entity.FlyingSaucerWeapon;
import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.entity.MechanicalTank;
import com.hp.mechafauna.entity.MilitaryDrone;
import com.hp.mechafauna.entity.PrismSentinel;
import com.hp.mechafauna.entity.WeaponMount;
import com.hp.mechafauna.entity.WeaponPose;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;

public final class MechModel<T extends AnimatedHostileMob> extends GeoModel<T> {
    private final Identifier model;
    private final Identifier texture;
    private final Identifier animation;
    private final String modelName;

    public MechModel(String modelName) {
        this.modelName = modelName;
        this.model = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, modelName);
        this.texture = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/" + modelName + ".png");
        this.animation = Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, modelName);
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return this.model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        if (state instanceof MechLivingRenderState livingState
            && livingState.entity instanceof MechanicalSpider spider && spider.isDeadOrDying()) {
            return Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "textures/entity/mechanical_spider_overload.png");
        }
        return this.texture;
    }

    @Override
    public Identifier getAnimationResource(T entity) {
        return this.animation;
    }

    public void applyBoneTransforms(RenderPassInfo<? extends MechLivingRenderState> pass, BoneSnapshots bones) {
        if (!(pass.renderState().entity instanceof AnimatedHostileMob entity)) {
            return;
        }
        float partialTick = pass.renderState().getPartialTick();
        if (entity instanceof FlyingSaucerBoss saucer) {
            bones.ifPresent("core_rings", bone -> bone.setRotY(bone.getRotY() + (saucer.tickCount + partialTick) * 0.025F));
            for (FlyingSaucerWeapon weapon : FlyingSaucerWeapon.values()) {
                boolean destroyed = saucer.isWeaponDestroyed(weapon.ordinal());
                bones.ifPresent(weapon.yawBone(), bone -> {
                    bone.skipRender(destroyed);
                    bone.skipChildrenRender(destroyed);
                });
                WeaponMount mount = weapon.mount();
                this.aimWeapon(entity, mount, partialTick, bones);
                if (weapon.barrelBone() != null) {
                    bones.ifPresent(weapon.barrelBone(), bone -> bone.setRotX((saucer.tickCount + partialTick) * 0.48F));
                }
            }
        } else if (entity instanceof MechanicalTank tank) {
            this.aimWeapon(entity, WeaponMount.TANK_CANNON, partialTick, bones, tank.getCannonAimOverride(partialTick).orElse(null));
        } else if (entity instanceof MilitaryDrone) {
            this.aimWeapon(entity, WeaponMount.MILITARY_DRONE_GUN, partialTick, bones);
            bones.ifPresent("gun_spin", bone -> bone.setRotZ(bone.getRotZ() + (entity.tickCount + partialTick) * 0.45F));
        } else if (entity instanceof MechanicalDrone) {
            this.aimWeapon(entity, WeaponMount.DRONE_GUN, partialTick, bones);
        } else if (entity instanceof HeavyMech mech) {
            this.aimWeapon(entity, WeaponMount.MECH_GUN, partialTick, bones);
            this.aimWeapon(entity, WeaponMount.MECH_MISSILES, partialTick, bones, mech.getMissileAimOverride(partialTick));
        } else if (entity instanceof com.hp.mechafauna.entity.FlameCrawler) {
            this.aimWeapon(entity, WeaponMount.FLAME_CRAWLER, partialTick, bones);
        } else if (entity instanceof PrismSentinel prism && prism.isPerformingAction()) {
            bones.ifPresent("body", bone -> bone.setRotX(bone.getRotX() + prism.getBodyTiltRadians(partialTick)));
            WeaponMount.Aim aim = prism.getLaserAim(partialTick);
            bones.ifPresent("lens_yaw", bone -> {
                bone.setRotX(0.0F);
                bone.setRotY(aim.yaw());
                bone.setRotZ(0.0F);
            });
            bones.ifPresent("lens_pitch", bone -> {
                bone.setRotX(aim.pitch());
                bone.setRotY(0.0F);
                bone.setRotZ(0.0F);
            });
        }
    }

    private WeaponMount.Aim aimWeapon(AnimatedHostileMob entity, WeaponMount mount, float partialTick, BoneSnapshots bones) {
        return this.aimWeapon(entity, mount, partialTick, bones, null);
    }

    private WeaponMount.Aim aimWeapon(AnimatedHostileMob entity, WeaponMount mount, float partialTick, BoneSnapshots bones, Vec3 aimOverride) {
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
            return null;
        }
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Matrix4d parent = WeaponPose.forModel(this.modelName).parentTransform(mount,
            WeaponMount.modelToWorld(entity.getPosition(partialTick), bodyYaw, entity.getModelScale()),
            this.currentAnimation(entity), this.currentAnimationSeconds(entity, partialTick));
        WeaponMount.Aim aim = mount.aim(parent, targetCenter);
        bones.ifPresent(mount.yawBone(), bone -> {
            bone.setRotX(0.0F);
            bone.setRotY(aim.yaw());
            bone.setRotZ(0.0F);
        });
        bones.ifPresent(mount.pitchBone(), bone -> {
            bone.setRotX(entity instanceof FlyingSaucerBoss ? 0.0F : aim.pitch());
            bone.setRotY(0.0F);
            bone.setRotZ(entity instanceof FlyingSaucerBoss ? aim.pitch() : 0.0F);
        });
        return aim;
    }

    private String currentAnimation(AnimatedHostileMob entity) {
        String action = entity.getActiveAction();
        return action == null ? null : entity.getActionAnimation(action);
    }

    private double currentAnimationSeconds(AnimatedHostileMob entity, float partialTick) {
        return entity.getActionAnimationSeconds(partialTick);
    }
}
