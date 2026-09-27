package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.registry.ModItems;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class HeavyMechMachineGunClientExtensions {
    // 双手端枪：持枪手平举，另一只手托住护套，跟随头部俯仰
    public static final EnumProxy<HumanoidModel.ArmPose> ARM_POSE_PARAMETERS = new EnumProxy<>(
        HumanoidModel.ArmPose.class, true, true, (IArmPoseTransformer)(model, entity, arm) ->
            AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, arm == HumanoidArm.RIGHT));

    private HeavyMechMachineGunClientExtensions() {
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return ARM_POSE_PARAMETERS.getValue();
            }
        }, ModItems.HEAVY_MECH_MACHINE_GUN.get());
    }
}
