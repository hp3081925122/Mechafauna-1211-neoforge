package com.hp.mechafauna;

import com.hp.mechafauna.registry.ModEntities;
import com.hp.mechafauna.registry.ModItems;
import com.hp.mechafauna.registry.ModParticles;
import com.hp.mechafauna.registry.ModSounds;
import com.hp.mechafauna.network.DroneControlPayload;
import com.hp.mechafauna.network.FireSupportPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

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
        modEventBus.addListener(FireSupportPayload::register);
        modEventBus.addListener(com.hp.mechafauna.gametest.FortressGameTests::register);
        modEventBus.addListener(com.hp.mechafauna.gametest.TankOutpostGameTests::register);
        if (FMLEnvironment.getDist().isClient()) {
            modEventBus.addListener(com.hp.mechafauna.client.ClientModEvents::registerRenderPipelines);
            modEventBus.addListener(com.hp.mechafauna.client.ClientModEvents::registerRenderers);
            modEventBus.addListener(com.hp.mechafauna.client.ClientModEvents::registerParticleProviders);
            modEventBus.addListener(com.hp.mechafauna.client.ModKeyMappings::register);
            modEventBus.addListener(com.hp.mechafauna.client.HeavyMechMachineGunClientExtensions::registerClientExtensions);
        }
        modContainer.registerConfig(ModConfig.Type.SERVER, MechafaunaConfig.SPEC);
    }
}
