package com.hp.mechafauna.particle;

import com.hp.mechafauna.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ExplosionParticleOptions(float radius) implements ParticleOptions {
    public static final MapCodec<ExplosionParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.FLOAT.fieldOf("radius").forGetter(ExplosionParticleOptions::radius)
    ).apply(instance, ExplosionParticleOptions::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionParticleOptions> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, ExplosionParticleOptions::radius, ExplosionParticleOptions::new
    );

    @Override
    public ParticleType<ExplosionParticleOptions> getType() {
        return ModParticles.SCALED_EXPLOSION.get();
    }
}
