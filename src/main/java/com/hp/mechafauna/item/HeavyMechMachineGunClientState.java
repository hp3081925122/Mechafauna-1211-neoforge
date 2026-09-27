package com.hp.mechafauna.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class HeavyMechMachineGunClientState {
    private HeavyMechMachineGunClientState() {
    }

    static boolean isFiring(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        // 本地玩家与其他玩家、生物手里同一把枪都要判定，按物品实例匹配正在使用的持有者
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.isUsingItem() && living.getUseItem() == stack) {
                return true;
            }
        }
        return false;
    }
}
