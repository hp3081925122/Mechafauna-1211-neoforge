package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(
        BuiltInRegistries.ARMOR_MATERIAL,
        Mechafauna.MOD_ID
    );

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MECH = ARMOR_MATERIALS.register("mech", () -> new ArmorMaterial(
        defense(),
        12,
        SoundEvents.ARMOR_EQUIP_NETHERITE,
        () -> Ingredient.of(ModItems.MECHANICAL_SCRAP.get()),
        List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "mech"))),
        2.0F,
        0.15F
    ));

    private ModArmorMaterials() {
    }

    private static EnumMap<ArmorItem.Type, Integer> defense() {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return defense;
    }
}
