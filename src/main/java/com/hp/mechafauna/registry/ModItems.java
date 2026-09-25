package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Mechafauna.MOD_ID);

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

    private ModItems() {
    }

    public static void addSpawnEggsToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(MECHANICAL_WOLF_SPAWN_EGG.get());
            event.accept(MECHANICAL_TANK_SPAWN_EGG.get());
            event.accept(MECHANICAL_DRONE_SPAWN_EGG.get());
            event.accept(MECHANICAL_SPIDER_SPAWN_EGG.get());
            event.accept(HEAVY_MECH_SPAWN_EGG.get());
        }
    }
}
