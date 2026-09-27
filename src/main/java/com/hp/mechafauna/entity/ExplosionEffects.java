package com.hp.mechafauna.entity;

import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.mojang.logging.LogUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class ExplosionEffects {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ExplosionEffects() {
    }

    public static void explode(Level level, @Nullable Entity source, @Nullable DamageSource damageSource,
                                   @Nullable ExplosionDamageCalculator damageCalculator,
                                   double x, double y, double z, float radius) {
        ExplosionParticleOptions explosionParticle = new ExplosionParticleOptions(radius);
        LOGGER.debug("Creating scaled explosion visual radius={} diameter={}", radius, radius * 2.0F);
        level.explode(source, damageSource, damageCalculator, x, y, z, radius, false, ExplosionInteraction.NONE,
            explosionParticle, explosionParticle, WeightedList.<ExplosionParticleInfo>builder()
                .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
                .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
                .build(), SoundEvents.GENERIC_EXPLODE);
    }
}
