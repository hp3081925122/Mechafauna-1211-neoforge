package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Mechafauna.MOD_ID);
    public static final RegistryObject<ParticleType<ExplosionParticleOptions>> SCALED_EXPLOSION = PARTICLE_TYPES.register(
        "scaled_explosion", () -> new ParticleType<>(true, ExplosionParticleOptions.DESERIALIZER) {
            @Override
            public Codec<ExplosionParticleOptions> codec() {
                return ExplosionParticleOptions.CODEC;
            }
        }
    );

    private ModParticles() {
    }
}
