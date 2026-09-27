package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.registry.ModEntities;
import com.hp.mechafauna.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MECHANICAL_WOLF.get(), context -> new MechRenderer<>(context, "mechanical_wolf"));
        event.registerEntityRenderer(ModEntities.RIDEABLE_MECHANICAL_WOLF.get(), RideableMechanicalWolfRenderer::new);
        event.registerEntityRenderer(ModEntities.MECHANICAL_TANK.get(), context -> new MechRenderer<>(context, "mechanical_tank"));
        event.registerEntityRenderer(ModEntities.MECHANICAL_DRONE.get(), context -> new MechRenderer<>(context, "mechanical_drone"));
        event.registerEntityRenderer(ModEntities.MILITARY_DRONE.get(), context -> new MechRenderer<>(context, "military_drone"));
        event.registerEntityRenderer(ModEntities.MECHANICAL_SPIDER.get(), context -> new MechRenderer<>(context, "mechanical_spider"));
        event.registerEntityRenderer(ModEntities.HEAVY_MECH.get(), context -> new MechRenderer<>(context, "heavy_mech"));
        event.registerEntityRenderer(ModEntities.FLAME_CRAWLER.get(), FlameCrawlerRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISM_SENTINEL.get(), PrismSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SAUCER_BOSS.get(), FlyingSaucerRenderer::new);
        event.registerEntityRenderer(ModEntities.SAUCER_LASER_STRIKE.get(), SaucerLaserStrikeRenderer::new);
        event.registerEntityRenderer(ModEntities.MECH_PROJECTILE.get(), MechProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISM_BEAM.get(), PrismBeamRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SCALED_EXPLOSION.get(), ScaledExplosionParticle.Provider::new);
    }
}
