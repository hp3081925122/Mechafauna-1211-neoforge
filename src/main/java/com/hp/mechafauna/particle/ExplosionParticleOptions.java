package com.hp.mechafauna.particle;

import com.hp.mechafauna.registry.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public record ExplosionParticleOptions(float radius) implements ParticleOptions {
    public static final Codec<ExplosionParticleOptions> CODEC = Codec.FLOAT.xmap(ExplosionParticleOptions::new, ExplosionParticleOptions::radius);
    public static final ParticleOptions.Deserializer<ExplosionParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public ExplosionParticleOptions fromCommand(ParticleType<ExplosionParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            return new ExplosionParticleOptions(reader.readFloat());
        }

        @Override
        public ExplosionParticleOptions fromNetwork(ParticleType<ExplosionParticleOptions> type, FriendlyByteBuf buffer) {
            return new ExplosionParticleOptions(buffer.readFloat());
        }
    };

    @Override
    public ParticleType<ExplosionParticleOptions> getType() {
        return ModParticles.SCALED_EXPLOSION.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.radius);
    }

    @Override
    public String writeToString() {
        return BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()) + " " + this.radius;
    }
}
