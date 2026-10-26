package com.hp.mechafauna.client;

import com.hp.mechafauna.registry.ModEntities;
import com.hp.mechafauna.registry.ModParticles;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(VfxRenderTypes.PIPELINE);
        com.mojang.logging.LogUtils.getLogger().debug("Mech VFX pipeline registered cull=false depthWrite=false target=main");
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MECHANICAL_WOLF.get(), context -> new MechRenderer<>(context, "mechanical_wolf"));
        event.registerEntityRenderer(ModEntities.RIDEABLE_MECHANICAL_WOLF.get(), RideableMechanicalWolfRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHANICAL_TANK.get(), MechanicalTankRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHANICAL_DRONE.get(), context -> new MechRenderer<>(context, "mechanical_drone"));
        event.registerEntityRenderer(ModEntities.MILITARY_DRONE.get(), context -> new MechRenderer<>(context, "military_drone"));
        event.registerEntityRenderer(ModEntities.MECHANICAL_SPIDER.get(), context -> new MechRenderer<>(context, "mechanical_spider"));
        event.registerEntityRenderer(ModEntities.HEAVY_MECH.get(), HeavyMechRenderer::new);
        event.registerEntityRenderer(ModEntities.FLAME_CRAWLER.get(), FlameCrawlerRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISM_SENTINEL.get(), PrismSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SAUCER_BOSS.get(), FlyingSaucerRenderer::new);
        event.registerEntityRenderer(ModEntities.SAUCER_LASER_STRIKE.get(), SaucerLaserStrikeRenderer::new);
        event.registerEntityRenderer(ModEntities.MECH_STRIKE_ZONE.get(), MechStrikeZoneRenderer::new);
        event.registerEntityRenderer(ModEntities.MECH_PROJECTILE.get(), MechProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISM_BEAM.get(), PrismBeamRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SCALED_EXPLOSION.get(), ScaledExplosionParticle.Provider::new);
    }
}
