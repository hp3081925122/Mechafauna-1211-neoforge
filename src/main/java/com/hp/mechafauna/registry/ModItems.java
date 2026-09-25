package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.item.MechanicalSpiderThrowableItem;
import com.hp.mechafauna.item.WeaponizedDroneItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mechafauna.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mechafauna.MOD_ID);

    public static final Supplier<? extends Item> MECHANICAL_SCRAP = ITEMS.registerSimpleItem("mechanical_scrap");
    public static final Supplier<? extends Item> WEAPONIZED_DRONE = ITEMS.register("weaponized_drone", WeaponizedDroneItem::new);
    public static final Supplier<? extends Item> MECHANICAL_SPIDER_THROWABLE = ITEMS.register("mechanical_spider_throwable", MechanicalSpiderThrowableItem::new);

    public static final Supplier<? extends Item> MECHANICAL_WOLF_SPAWN_EGG = ITEMS.register(
        "mechanical_wolf_spawn_egg",
        () -> new DeferredSpawnEggItem(ModEntities.MECHANICAL_WOLF, 0x38424A, 0x1ABBC0, new Item.Properties())
    );
    public static final Supplier<? extends Item> MECHANICAL_TANK_SPAWN_EGG = ITEMS.register(
        "mechanical_tank_spawn_egg",
        () -> new DeferredSpawnEggItem(ModEntities.MECHANICAL_TANK, 0x66757C, 0xD45835, new Item.Properties())
    );
    public static final Supplier<? extends Item> MECHANICAL_DRONE_SPAWN_EGG = ITEMS.register(
        "mechanical_drone_spawn_egg",
        () -> new DeferredSpawnEggItem(ModEntities.MECHANICAL_DRONE, 0x53646B, 0x1ABBC0, new Item.Properties())
    );
    public static final Supplier<? extends Item> MECHANICAL_SPIDER_SPAWN_EGG = ITEMS.register(
        "mechanical_spider_spawn_egg",
        () -> new DeferredSpawnEggItem(ModEntities.MECHANICAL_SPIDER, 0x5D3430, 0xD34C2E, new Item.Properties())
    );
    public static final Supplier<? extends Item> HEAVY_MECH_SPAWN_EGG = ITEMS.register(
        "heavy_mech_spawn_egg",
        () -> new DeferredSpawnEggItem(ModEntities.HEAVY_MECH, 0x345477, 0xD6742B, new Item.Properties())
    );
    public static final Supplier<? extends Item> FLAME_CRAWLER_SPAWN_EGG = ITEMS.register(
        "flame_crawler_spawn_egg",
        () -> new DeferredSpawnEggItem(ModEntities.FLAME_CRAWLER, 0x59646A, 0xE55724, new Item.Properties())
    );
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MECHAFAUNA_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.mechafauna.main"))
        .icon(() -> new ItemStack(MECHANICAL_WOLF_SPAWN_EGG.get()))
        .displayItems((parameters, output) -> {
            output.accept(MECHANICAL_WOLF_SPAWN_EGG.get());
            output.accept(MECHANICAL_TANK_SPAWN_EGG.get());
            output.accept(MECHANICAL_DRONE_SPAWN_EGG.get());
            output.accept(MECHANICAL_SPIDER_SPAWN_EGG.get());
            output.accept(HEAVY_MECH_SPAWN_EGG.get());
            output.accept(FLAME_CRAWLER_SPAWN_EGG.get());
            output.accept(MECHANICAL_SCRAP.get());
            output.accept(WEAPONIZED_DRONE.get());
            output.accept(MECHANICAL_SPIDER_THROWABLE.get());
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
            event.accept(MECHANICAL_SCRAP.get());
            event.accept(WEAPONIZED_DRONE.get());
            event.accept(MECHANICAL_SPIDER_THROWABLE.get());
        }
    }
}
