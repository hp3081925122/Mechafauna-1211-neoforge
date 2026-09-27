package com.hp.mechafauna.item;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.FORGE)
public final class ReinforcementCoreItem extends Item {
    private static final String ARMOR_COOLDOWN_TAG = "mechafauna_reinforced_armor_cooldown";
    private static final String BULLET_DAMAGE_TAG = "mechafauna_reinforced_bullet_damage";

    public ReinforcementCoreItem() {
        super(new Item.Properties().stacksTo(16));
    }

    public static boolean hasArmorCooldownUpgrade(ItemStack stack) {
        return hasUpgrade(stack, ARMOR_COOLDOWN_TAG);
    }

    public static boolean hasBulletDamageUpgrade(ItemStack stack) {
        return hasUpgrade(stack, BULLET_DAMAGE_TAG);
    }

    private static boolean hasUpgrade(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(key);
    }

    private static void applyUpgrade(ItemStack stack, String key) {
        stack.getOrCreateTag().putBoolean(key, true);
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!event.getRight().is(ModItems.REINFORCEMENT_CORE.get())) {
            return;
        }
        ItemStack base = event.getLeft();
        String upgradeTag;
        if (base.getItem() instanceof MechArmorItem) {
            upgradeTag = ARMOR_COOLDOWN_TAG;
        } else if (base.is(ModItems.HEAVY_MECH_MACHINE_GUN.get())) {
            upgradeTag = BULLET_DAMAGE_TAG;
        } else {
            return;
        }
        if (hasUpgrade(base, upgradeTag)) {
            return;
        }
        ItemStack output = base.copy();
        applyUpgrade(output, upgradeTag);
        event.setOutput(output);
        event.setCost(5);
        event.setMaterialCost(1);
    }

}
