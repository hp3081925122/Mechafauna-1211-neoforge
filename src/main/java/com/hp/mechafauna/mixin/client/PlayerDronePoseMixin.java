package com.hp.mechafauna.mixin.client;

import com.hp.mechafauna.entity.MechanicalDrone;
import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerDronePoseMixin {
    private static final Logger mechafauna$logger = LogUtils.getLogger();
    private static final Set<Integer> mechafauna$loggedDroneRiders = ConcurrentHashMap.newKeySet();

    @Inject(method = "setupAnim", at = @At("TAIL"), remap = false)
    private void mechafauna$setDroneGripPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
        if (!(entity.getVehicle() instanceof MechanicalDrone drone) || !drone.isOwnedBy(entity)) {
            mechafauna$loggedDroneRiders.remove(entity.getId());
            return;
        }
        PlayerModel<?> model = (PlayerModel<?>)(Object)this;
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.rightArm.xRot = -3.1415927F;
        model.rightArm.yRot = 0.0F;
        model.rightArm.zRot = 0.18F;
        model.leftArm.xRot = -3.1415927F;
        model.leftArm.yRot = 0.0F;
        model.leftArm.zRot = -0.18F;
        model.rightLeg.xRot = 0.0F;
        model.rightLeg.yRot = 0.0F;
        model.rightLeg.zRot = 0.0F;
        model.leftLeg.xRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        model.jacket.copyFrom(model.body);
        if (mechafauna$loggedDroneRiders.add(entity.getId())) {
            mechafauna$logger.debug("Applied straight overhead drone grip pose entity={} riderOffset={} rightArmPitch={} rightArmRoll={} leftArmPitch={} leftArmRoll={}", entity.getId(), entity.position().subtract(drone.position()), model.rightArm.xRot, model.rightArm.zRot, model.leftArm.xRot, model.leftArm.zRot);
        }
    }
}
