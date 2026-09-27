package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import java.util.EnumMap;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public final class ModArmorMaterials {
    public static final ArmorMaterial MECH = new MechArmorMaterial();

    private ModArmorMaterials() {
    }

    private static EnumMap<ArmorItem.Type, Integer> defense() {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        return defense;
    }

    private static final class MechArmorMaterial implements ArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return 37 * switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return defense().getOrDefault(type, 0);
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.MECHANICAL_SCRAP.get());
        }

        @Override
        public String getName() {
            return Mechafauna.MOD_ID + ":mech";
        }

        @Override
        public float getToughness() {
            return 2.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.15F;
        }
    }
}
