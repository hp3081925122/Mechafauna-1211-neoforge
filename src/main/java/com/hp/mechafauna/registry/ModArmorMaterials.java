package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
    public static final ArmorMaterial MECH = new ArmorMaterial(
        37,
        Map.of(
            ArmorType.HELMET, 3,
            ArmorType.CHESTPLATE, 8,
            ArmorType.LEGGINGS, 6,
            ArmorType.BOOTS, 3,
            ArmorType.BODY, 11
        ),
        12,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        2.0F,
        0.15F,
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "mech_armor_repair")),
        EquipmentAssets.NETHERITE
    );

    private ModArmorMaterials() {
    }
}
