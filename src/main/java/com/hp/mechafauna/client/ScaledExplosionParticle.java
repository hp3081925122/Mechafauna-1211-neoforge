package com.hp.mechafauna.client;

import com.hp.mechafauna.particle.ExplosionParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.RandomSource;

public final class ScaledExplosionParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    private ScaledExplosionParticle(ClientLevel level, double x, double y, double z, float radius, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
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
    protected int getLightCoords(float partialTick) {
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
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<ExplosionParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public SingleQuadParticle createParticle(ExplosionParticleOptions options, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new ScaledExplosionParticle(level, x, y, z, options.radius(), this.sprites);
        }
    }
}
