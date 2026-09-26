package com.hp.mechafauna;

import com.hp.mechafauna.registry.ModEntities;
import com.hp.mechafauna.registry.ModItems;
import com.hp.mechafauna.registry.ModParticles;
import com.hp.mechafauna.registry.ModSounds;
import com.hp.mechafauna.network.DroneControlPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Mechafauna.MOD_ID)
public final class Mechafauna {
    public static final String MOD_ID = "mechafauna";

    public Mechafauna(IEventBus modEventBus, ModContainer modContainer) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModItems::addSpawnEggsToCreativeTab);
        modEventBus.addListener(DroneControlPayload::register);
    }
}
