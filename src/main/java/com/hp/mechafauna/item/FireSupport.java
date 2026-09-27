package com.hp.mechafauna.item;

import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.entity.MilitaryDrone;
import com.hp.mechafauna.entity.PrismSentinel;
import com.hp.mechafauna.entity.SaucerLaserStrike;
import com.hp.mechafauna.registry.ModEntities;
import com.hp.mechafauna.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class FireSupport {
    public static final int COOLDOWN_TICKS = 20 * 20;
    public static final int CHEST_COOLDOWN_TICKS = 10 * 20;
    public static final int DURATION_TICKS = 20 * 20;

    private FireSupport() {
    }

    public static void activate(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (helmet.is(ModItems.MECH_HELMET.get())) {
            tryActivate(player, helmet, cooldown(helmet, COOLDOWN_TICKS), () -> summonDrone(player));
        }
        if (chest.is(ModItems.MECH_CHESTPLATE.get())) {
            tryActivate(player, chest, cooldown(chest, CHEST_COOLDOWN_TICKS), () -> summonLaser(player));
        }
        if (legs.is(ModItems.MECH_LEGGINGS.get())) {
            tryActivate(player, legs, cooldown(legs, COOLDOWN_TICKS), () -> summonSpiders(player));
        }
        if (boots.is(ModItems.MECH_BOOTS.get())) {
            tryActivate(player, boots, cooldown(boots, COOLDOWN_TICKS), () -> summonPrisms(player));
        }
    }

    private static int cooldown(ItemStack armorStack, int baseCooldown) {
        return ReinforcementCoreItem.hasArmorCooldownUpgrade(armorStack) ? baseCooldown / 2 : baseCooldown;
    }

    private static void tryActivate(ServerPlayer player, ItemStack stack, int cooldown, java.util.function.BooleanSupplier action) {
        if (player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        if (action.getAsBoolean()) {
            player.getCooldowns().addCooldown(stack, cooldown);
        }
    }

    private static boolean summonDrone(ServerPlayer player) {
        ServerLevel level = (ServerLevel)player.level();
        MilitaryDrone drone = ModEntities.MILITARY_DRONE.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (drone == null) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        drone.setOwner(player);
        drone.limitLife(DURATION_TICKS);
        drone.setPos(player.getX() + look.x * 2.5, player.getY() + 1.2, player.getZ() + look.z * 2.5);
        drone.setYRot(player.getYRot());
        return level.addFreshEntity(drone);
    }

    private static boolean summonLaser(ServerPlayer player) {
        ServerLevel level = (ServerLevel)player.level();
        SaucerLaserStrike strike = ModEntities.SAUCER_LASER_STRIKE.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (strike == null) {
            return false;
        }
        strike.placeAt(player);
        return level.addFreshEntity(strike);
    }

    private static boolean summonSpiders(ServerPlayer player) {
        return FireSupport.<MechanicalSpider>summonPair(player, ModEntities.MECHANICAL_SPIDER.get().create((ServerLevel)player.level(), EntitySpawnReason.SPAWN_ITEM_USE), 0.0, (spider, sign) -> {
            spider.setSummonerUUID(player.getUUID());
            spider.limitLife(DURATION_TICKS);
        });
    }

    private static boolean summonPrisms(ServerPlayer player) {
        return FireSupport.<PrismSentinel>summonPair(player, ModEntities.PRISM_SENTINEL.get().create((ServerLevel)player.level(), EntitySpawnReason.SPAWN_ITEM_USE), 1.6, (prism, sign) -> {
            prism.assignSupport(player, DURATION_TICKS);
        });
    }

    private static <T extends Mob> boolean summonPair(ServerPlayer player, T first, double height, PairSetup<T> setup) {
        if (first == null) {
            return false;
        }
        Vec3 side = sideOf(player);
        boolean spawned = false;
        for (int index = 0; index < 2; index++) {
            T mob = index == 0 ? first : copyType(player, first);
            if (mob == null) {
                continue;
            }
            double sign = index == 0 ? 1.0 : -1.0;
            setup.prepare(mob, sign);
            mob.setPos(player.getX() + side.x * 1.6 * sign, player.getY() + height, player.getZ() + side.z * 1.6 * sign);
            mob.setYRot(player.getYRot());
            spawned |= ((ServerLevel)player.level()).addFreshEntity(mob);
        }
        return spawned;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Mob> T copyType(ServerPlayer player, T sample) {
        return (T)sample.getType().create((ServerLevel)player.level(), EntitySpawnReason.SPAWN_ITEM_USE);
    }

    private static Vec3 sideOf(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0, look.x);
        if (side.lengthSqr() < 1.0E-4) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return side.normalize();
    }

    @FunctionalInterface
    private interface PairSetup<T extends Mob> {
        void prepare(T mob, double sign);
    }
}
