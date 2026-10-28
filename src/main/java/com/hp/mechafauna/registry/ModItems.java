package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.item.MechArmorItem;
import com.hp.mechafauna.item.HeavyMechMachineGunItem;
import com.hp.mechafauna.item.FlyingSaucerLocatorItem;
import com.hp.mechafauna.item.MechanicalSpiderThrowableItem;
import com.hp.mechafauna.item.MechanicalWolfSummonItem;
import com.hp.mechafauna.item.PrismCoreItem;
import com.hp.mechafauna.item.ReinforcementCoreItem;
import com.hp.mechafauna.item.WeaponizedDroneItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Mechafauna.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mechafauna.MOD_ID);

    public static final Supplier<? extends Item> MECHANICAL_SCRAP = ITEMS.register("mechanical_scrap", () -> new Item(new Item.Properties()));
    public static final Supplier<? extends Item> MECHANICAL_WOLF_SUMMON = ITEMS.register("mechanical_wolf_summon", MechanicalWolfSummonItem::new);
    public static final Supplier<? extends Item> WEAPONIZED_DRONE = ITEMS.register("weaponized_drone", WeaponizedDroneItem::new);
    public static final Supplier<? extends Item> MECHANICAL_SPIDER_THROWABLE = ITEMS.register("mechanical_spider_throwable", MechanicalSpiderThrowableItem::new);
    public static final Supplier<? extends Item> PRISM_CORE = ITEMS.register("prism_core", PrismCoreItem::new);
    public static final Supplier<? extends Item> HEAVY_MECH_MACHINE_GUN = ITEMS.register("heavy_mech_machine_gun", HeavyMechMachineGunItem::new);
    public static final Supplier<ReinforcementCoreItem> REINFORCEMENT_CORE = ITEMS.register("reinforcement_core", ReinforcementCoreItem::new);
    public static final Supplier<? extends Item> FLYING_SAUCER_LOCATOR = ITEMS.register("flying_saucer_locator", FlyingSaucerLocatorItem::new);

    public static final Supplier<? extends Item> MECHANICAL_WOLF_SPAWN_EGG = ITEMS.register(
        "mechanical_wolf_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.MECHANICAL_WOLF, 0xD8DCE8, 0x22C4D4, new Item.Properties())
    );
    public static final Supplier<? extends Item> MECHANICAL_TANK_SPAWN_EGG = ITEMS.register(
        "mechanical_tank_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.MECHANICAL_TANK, 0x1E3434, 0xB4B8D0, new Item.Properties())
    );
    public static final Supplier<? extends Item> MECHANICAL_DRONE_SPAWN_EGG = ITEMS.register(
        "mechanical_drone_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.MECHANICAL_DRONE, 0x3A5656, 0xD4A444, new Item.Properties())
    );
    public static final Supplier<? extends Item> MECHANICAL_SPIDER_SPAWN_EGG = ITEMS.register(
        "mechanical_spider_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.MECHANICAL_SPIDER, 0xB4B8D0, 0xE06424, new Item.Properties())
    );
    public static final Supplier<? extends Item> HEAVY_MECH_SPAWN_EGG = ITEMS.register(
        "heavy_mech_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.HEAVY_MECH, 0x5A7676, 0xE06424, new Item.Properties())
    );
    public static final Supplier<? extends Item> FLAME_CRAWLER_SPAWN_EGG = ITEMS.register(
        "flame_crawler_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.FLAME_CRAWLER, 0x3A3A44, 0xD08030, new Item.Properties())
    );
    public static final Supplier<? extends Item> PRISM_SENTINEL_SPAWN_EGG = ITEMS.register(
        "prism_sentinel_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.PRISM_SENTINEL, 0x343454, 0x44B4C4, new Item.Properties())
    );
    public static final Supplier<MechArmorItem> MECH_HELMET = ITEMS.register(
        "mech_helmet",
        () -> new MechArmorItem(ArmorItem.Type.HELMET)
    );
    public static final Supplier<MechArmorItem> MECH_CHESTPLATE = ITEMS.register(
        "mech_chestplate",
        () -> new MechArmorItem(ArmorItem.Type.CHESTPLATE)
    );
    public static final Supplier<MechArmorItem> MECH_LEGGINGS = ITEMS.register(
        "mech_leggings",
        () -> new MechArmorItem(ArmorItem.Type.LEGGINGS)
    );
    public static final Supplier<MechArmorItem> MECH_BOOTS = ITEMS.register(
        "mech_boots",
        () -> new MechArmorItem(ArmorItem.Type.BOOTS)
    );
    public static final Supplier<? extends Item> FLYING_SAUCER_BOSS_SPAWN_EGG = ITEMS.register(
        "flying_saucer_boss_spawn_egg",
        () -> new ForgeSpawnEggItem(ModEntities.FLYING_SAUCER_BOSS, 0xB4B4B4, 0x22B4D4, new Item.Properties())
    );
    public static final RegistryObject<CreativeModeTab> MECHAFAUNA_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.mechafauna.main"))
        .icon(() -> new ItemStack(MECHANICAL_WOLF_SPAWN_EGG.get()))
        .displayItems((parameters, output) -> {
            output.accept(MECHANICAL_WOLF_SPAWN_EGG.get());
            output.accept(MECHANICAL_TANK_SPAWN_EGG.get());
            output.accept(MECHANICAL_DRONE_SPAWN_EGG.get());
            output.accept(MECHANICAL_SPIDER_SPAWN_EGG.get());
            output.accept(HEAVY_MECH_SPAWN_EGG.get());
            output.accept(FLAME_CRAWLER_SPAWN_EGG.get());
            output.accept(PRISM_SENTINEL_SPAWN_EGG.get());
            output.accept(FLYING_SAUCER_BOSS_SPAWN_EGG.get());
            output.accept(MECHANICAL_SCRAP.get());
            output.accept(MECHANICAL_WOLF_SUMMON.get());
            output.accept(WEAPONIZED_DRONE.get());
            output.accept(MECHANICAL_SPIDER_THROWABLE.get());
            output.accept(PRISM_CORE.get());
            output.accept(HEAVY_MECH_MACHINE_GUN.get());
            output.accept(REINFORCEMENT_CORE.get());
            output.accept(FLYING_SAUCER_LOCATOR.get());
            output.accept(MECH_HELMET.get());
            output.accept(MECH_CHESTPLATE.get());
            output.accept(MECH_LEGGINGS.get());
            output.accept(MECH_BOOTS.get());
        })
        .build());

    private ModItems() {
    }

    public static void addSpawnEggsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(MECHANICAL_WOLF_SPAWN_EGG.get());
            event.accept(MECHANICAL_TANK_SPAWN_EGG.get());
            event.accept(MECHANICAL_DRONE_SPAWN_EGG.get());
            event.accept(MECHANICAL_SPIDER_SPAWN_EGG.get());
            event.accept(HEAVY_MECH_SPAWN_EGG.get());
            event.accept(FLAME_CRAWLER_SPAWN_EGG.get());
            event.accept(PRISM_SENTINEL_SPAWN_EGG.get());
            event.accept(FLYING_SAUCER_BOSS_SPAWN_EGG.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(MECH_HELMET.get());
            event.accept(MECH_CHESTPLATE.get());
            event.accept(MECH_LEGGINGS.get());
            event.accept(MECH_BOOTS.get());
        }
    }
}
