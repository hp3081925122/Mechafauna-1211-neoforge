package com.hp.mechafauna.client;

import com.hp.mechafauna.particle.ExplosionParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public final class ScaledExplosionParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    private ScaledExplosionParticle(ClientLevel level, double x, double y, double z, float radius, SpriteSet sprites) {
        super(level, x, y, z);
        this.lifetime = 6 + this.random.nextInt(4);
        float brightness = this.random.nextFloat() * 0.6F + 0.4F;
        this.rCol = brightness;
        this.gCol = brightness;
        this.bCol = brightness;
        this.quadSize = Math.max(0.1F, radius * 2.0F);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    public static final class Provider implements ParticleProvider<ExplosionParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(ExplosionParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ScaledExplosionParticle(level, x, y, z, options.radius(), this.sprites);
        }
    }
}
