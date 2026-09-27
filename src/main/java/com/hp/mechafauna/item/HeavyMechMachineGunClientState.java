package com.hp.mechafauna.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.geckolib.animatable.GeoItem;

final class HeavyMechMachineGunClientState {
    private HeavyMechMachineGunClientState() {
    }

    static boolean isFiring(long instanceId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        if (instanceId == Long.MAX_VALUE && minecraft.player != null && minecraft.player.isUsingItem()
            && minecraft.player.getUseItem().getItem() instanceof HeavyMechMachineGunItem) {
            return true;
        }
        // 本地玩家与其他玩家、生物手里同一把枪都要判定，按物品实例匹配正在使用的持有者
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.isUsingItem() && GeoItem.getId(living.getUseItem()) == instanceId) {
                return true;
            }
        }
        return false;
    }

    static void spawnFirstPersonMuzzleFlash(Level level, Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != player) {
            return;
        }
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 muzzle = HeavyMechMachineGunItem.muzzlePosition(level, player, direction);
        level.addParticle(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z,
            direction.x * 0.08, direction.y * 0.08, direction.z * 0.08);
        for (int i = 0; i < 2; i++) {
            double spreadX = (level.getRandom().nextDouble() - 0.5) * 0.18;
            double spreadY = (level.getRandom().nextDouble() - 0.5) * 0.18;
            double spreadZ = (level.getRandom().nextDouble() - 0.5) * 0.18;
            level.addParticle(ParticleTypes.CRIT, muzzle.x, muzzle.y, muzzle.z,
                direction.x * 0.45 + spreadX, direction.y * 0.45 + spreadY, direction.z * 0.45 + spreadZ);
        }
    }
}
