package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Mechafauna.MOD_ID);
    public static final DeferredHolder<ParticleType<?>, ParticleType<ExplosionParticleOptions>> SCALED_EXPLOSION = PARTICLE_TYPES.register(
        "scaled_explosion", () -> new ParticleType<>(true) {
            @Override
            public MapCodec<ExplosionParticleOptions> codec() {
                return ExplosionParticleOptions.CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ExplosionParticleOptions> streamCodec() {
                return ExplosionParticleOptions.STREAM_CODEC;
            }
        }
    );

    private ModParticles() {
    }
}
