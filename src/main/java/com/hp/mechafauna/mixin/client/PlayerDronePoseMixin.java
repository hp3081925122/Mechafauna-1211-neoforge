package com.hp.mechafauna.mixin.client;

import com.hp.mechafauna.entity.MechanicalDrone;
import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
    private void mechafauna$setDroneGripPose(AvatarRenderState state, CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(state.id);
        if (!(entity instanceof Player player) || !(player.getVehicle() instanceof MechanicalDrone drone) || !drone.isOwnedBy(player)) {
            mechafauna$loggedDroneRiders.remove(state.id);
            return;
        }
        PlayerModel model = (PlayerModel)(Object)this;
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
        model.rightSleeve.loadPose(model.rightArm.storePose());
        model.leftSleeve.loadPose(model.leftArm.storePose());
        model.rightPants.loadPose(model.rightLeg.storePose());
        model.leftPants.loadPose(model.leftLeg.storePose());
        model.jacket.loadPose(model.body.storePose());
        if (mechafauna$loggedDroneRiders.add(player.getId())) {
            mechafauna$logger.debug("Applied straight overhead drone grip pose entity={} riderOffset={} rightArmPitch={} rightArmRoll={} leftArmPitch={} leftArmRoll={}", player.getId(), player.position().subtract(drone.position()), model.rightArm.xRot, model.rightArm.zRot, model.leftArm.xRot, model.leftArm.zRot);
        }
    }
}
