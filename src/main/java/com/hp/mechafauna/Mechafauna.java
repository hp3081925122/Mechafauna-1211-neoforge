package com.hp.mechafauna;

import com.hp.mechafauna.registry.ModEntities;
import com.hp.mechafauna.registry.ModItems;
import com.hp.mechafauna.registry.ModParticles;
import com.hp.mechafauna.registry.ModSounds;
import com.hp.mechafauna.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Mechafauna.MOD_ID)
public final class Mechafauna {
    public static final String MOD_ID = "mechafauna";

    public Mechafauna(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_MODE_TABS.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModItems::addSpawnEggsToCreativeTab);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MechafaunaConfig.SPEC);
    }
}
