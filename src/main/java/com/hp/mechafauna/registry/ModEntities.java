package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.HeavyMech;
import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.entity.FlameCrawler;
import com.hp.mechafauna.entity.MechProjectile;
import com.hp.mechafauna.entity.MechStrikeZone;
import com.hp.mechafauna.entity.PrismBeam;
import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.entity.MilitaryDrone;
import com.hp.mechafauna.entity.MechanicalSpider;
import com.hp.mechafauna.entity.MechanicalTank;
import com.hp.mechafauna.entity.MechanicalWolf;
import com.hp.mechafauna.entity.PrismSentinel;
import com.hp.mechafauna.entity.SaucerLaserStrike;
import com.hp.mechafauna.entity.RideableMechanicalWolf;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Mechafauna.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<RideableMechanicalWolf>> RIDEABLE_MECHANICAL_WOLF = ENTITY_TYPES.register(
        "rideable_mechanical_wolf",
        () -> EntityType.Builder.of(RideableMechanicalWolf::new, MobCategory.CREATURE)
            .sized(1.25F, 1.2F)
            .passengerAttachments(new Vec3(0.0, 0.9, 0.0))
            .clientTrackingRange(10)
            .build("mechafauna:rideable_mechanical_wolf")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MechanicalWolf>> MECHANICAL_WOLF = ENTITY_TYPES.register(
        "mechanical_wolf",
        () -> EntityType.Builder.of(MechanicalWolf::new, MobCategory.MONSTER).sized(1.25F, 1.2F).clientTrackingRange(10).build("mechafauna:mechanical_wolf")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MechanicalTank>> MECHANICAL_TANK = ENTITY_TYPES.register(
        "mechanical_tank",
        () -> EntityType.Builder.of(MechanicalTank::new, MobCategory.MONSTER).sized(2.4F, 1.9F).clientTrackingRange(12).build("mechafauna:mechanical_tank")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MechanicalDrone>> MECHANICAL_DRONE = ENTITY_TYPES.register(
        "mechanical_drone",
        () -> EntityType.Builder.of(MechanicalDrone::new, MobCategory.MONSTER).sized(2.4F, 1.4F).passengerAttachments(new Vec3(0.0, -0.46, -0.2875)).clientTrackingRange(12).build("mechafauna:mechanical_drone")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MilitaryDrone>> MILITARY_DRONE = ENTITY_TYPES.register(
        "military_drone",
        () -> EntityType.Builder.of(MilitaryDrone::new, MobCategory.MONSTER).sized(2.4F, 1.4F).passengerAttachments(new Vec3(0.0, -0.46, -0.2875)).clientTrackingRange(12).build("mechafauna:military_drone")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MechanicalSpider>> MECHANICAL_SPIDER = ENTITY_TYPES.register(
        "mechanical_spider",
        () -> EntityType.Builder.of(MechanicalSpider::new, MobCategory.MONSTER).sized(1.35F, 0.9F).clientTrackingRange(10).build("mechafauna:mechanical_spider")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<HeavyMech>> HEAVY_MECH = ENTITY_TYPES.register(
        "heavy_mech",
        () -> EntityType.Builder.of(HeavyMech::new, MobCategory.MONSTER).sized(2.0F, 3.5F).clientTrackingRange(16).build("mechafauna:heavy_mech")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<FlameCrawler>> FLAME_CRAWLER = ENTITY_TYPES.register(
        "flame_crawler",
        () -> EntityType.Builder.of(FlameCrawler::new, MobCategory.MONSTER).sized(3.1F, 1.8F).clientTrackingRange(12).build("mechafauna:flame_crawler")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<PrismSentinel>> PRISM_SENTINEL = ENTITY_TYPES.register(
        "prism_sentinel",
        () -> EntityType.Builder.of(PrismSentinel::new, MobCategory.MONSTER).sized(1.45F, 1.55F).clientTrackingRange(12).build("mechafauna:prism_sentinel")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<FlyingSaucerBoss>> FLYING_SAUCER_BOSS = ENTITY_TYPES.register(
        "flying_saucer_boss",
        () -> EntityType.Builder.of(FlyingSaucerBoss::new, MobCategory.MONSTER).sized(10.4F, 5.6F).clientTrackingRange(32).updateInterval(1).build("mechafauna:flying_saucer_boss")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<SaucerLaserStrike>> SAUCER_LASER_STRIKE = ENTITY_TYPES.register(
        "saucer_laser_strike",
        () -> EntityType.Builder.<SaucerLaserStrike>of(SaucerLaserStrike::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(64)
            .updateInterval(1)
            .noSave()
            .build("mechafauna:saucer_laser_strike")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MechStrikeZone>> MECH_STRIKE_ZONE = ENTITY_TYPES.register(
        "mech_strike_zone",
        () -> EntityType.Builder.<MechStrikeZone>of(MechStrikeZone::new, MobCategory.MISC)
            .sized(0.1F, 0.1F)
            .clientTrackingRange(10)
            .updateInterval(20)
            .noSave()
            .noSummon()
            .build("mechafauna:mech_strike_zone")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<MechProjectile>> MECH_PROJECTILE = ENTITY_TYPES.register(
        "mech_projectile",
        () -> EntityType.Builder.<MechProjectile>of(MechProjectile::new, MobCategory.MISC).sized(0.28F, 0.28F).clientTrackingRange(10).updateInterval(1).build("mechafauna:mech_projectile")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<PrismBeam>> PRISM_BEAM = ENTITY_TYPES.register(
        "prism_beam",
        () -> EntityType.Builder.<PrismBeam>of(PrismBeam::new, MobCategory.MISC).noSave().noSummon().sized(0.1F, 0.1F).clientTrackingRange(8).updateInterval(1).build("mechafauna:prism_beam")
    );

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RIDEABLE_MECHANICAL_WOLF.get(), RideableMechanicalWolf.createAttributes().build());
        event.put(MECHANICAL_WOLF.get(), MechanicalWolf.createAttributes().build());
        event.put(MECHANICAL_TANK.get(), MechanicalTank.createAttributes().build());
        event.put(MECHANICAL_DRONE.get(), MechanicalDrone.createAttributes().build());
        event.put(MILITARY_DRONE.get(), MilitaryDrone.createAttributes().build());
        event.put(MECHANICAL_SPIDER.get(), MechanicalSpider.createAttributes().build());
        event.put(HEAVY_MECH.get(), HeavyMech.createAttributes().build());
        event.put(FLAME_CRAWLER.get(), FlameCrawler.createAttributes().build());
        event.put(PRISM_SENTINEL.get(), PrismSentinel.createAttributes().build());
        event.put(FLYING_SAUCER_BOSS.get(), FlyingSaucerBoss.createAttributes().build());
    }
}
