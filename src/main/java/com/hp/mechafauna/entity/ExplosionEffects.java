package com.hp.mechafauna.entity;

import com.hp.mechafauna.particle.ExplosionParticleOptions;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class ExplosionEffects {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Policy DEFAULT = new Policy(entity -> true, entity -> 1.0, entity -> 1.0);

    private ExplosionEffects() {
    }

    public static Explosion explode(Level level, @Nullable Entity source, @Nullable DamageSource damageSource,
                                    double x, double y, double z, float radius, Policy policy) {
        Explosion explosion = new Explosion(level, source, damageSource, null, x, y, z, radius, false,
            Explosion.BlockInteraction.KEEP);
        if (ForgeEventFactory.onExplosionStart(level, explosion)) {
            return explosion;
        }
        Vec3 center = new Vec3(x, y, z);
        level.gameEvent(source, GameEvent.EXPLODE, center);
        double diameter = radius * 2.0;
        int minX = net.minecraft.util.Mth.floor(x - diameter - 1.0);
        int maxX = net.minecraft.util.Mth.floor(x + diameter + 1.0);
        int minY = net.minecraft.util.Mth.floor(y - diameter - 1.0);
        int maxY = net.minecraft.util.Mth.floor(y + diameter + 1.0);
        int minZ = net.minecraft.util.Mth.floor(z - diameter - 1.0);
        int maxZ = net.minecraft.util.Mth.floor(z + diameter + 1.0);
        List<Entity> entities = level.getEntities(source, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        ForgeEventFactory.onExplosionDetonate(level, explosion, entities, diameter);
        for (Entity entity : entities) {
            if (entity.ignoreExplosion()) {
                continue;
            }
            double normalizedDistance = Math.sqrt(entity.distanceToSqr(center)) / diameter;
            if (normalizedDistance > 1.0) {
                continue;
            }
            double dx = entity.getX() - x;
            double dy = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - y;
            double dz = entity.getZ() - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance == 0.0) {
                continue;
            }
            dx /= distance;
            dy /= distance;
            dz /= distance;
            double impact = (1.0 - normalizedDistance) * Explosion.getSeenPercent(center, entity);
            if (policy.damageTarget().test(entity)) {
                double baseDamage = (impact * impact + impact) / 2.0 * 7.0 * diameter + 1.0;
                float damage = (float)(baseDamage * policy.damageMultiplier().applyAsDouble(entity));
                if (damage > 0.0F) {
                    entity.hurt(explosion.getDamageSource(), damage);
                }
            }
            double knockback = impact * policy.knockbackMultiplier().applyAsDouble(entity);
            if (entity instanceof LivingEntity living) {
                knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, (float)knockback);
            }
            Vec3 impulse = new Vec3(dx * knockback, dy * knockback, dz * knockback);
            if (impulse.lengthSqr() > 0.0) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
                if (entity instanceof Player player) {
                    explosion.getHitPlayers().put(player, impulse);
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
                    }
                }
            }
        }
        ExplosionParticleOptions particle = new ExplosionParticleOptions(radius);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles((ParticleOptions)particle, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        } else {
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
        float pitch = (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, pitch);
        LOGGER.debug("Created entity-only explosion radius={} affectedEntities={}", radius, entities.size());
        return explosion;
    }

    public record Policy(Predicate<Entity> damageTarget, ToDoubleFunction<Entity> damageMultiplier,
                         ToDoubleFunction<Entity> knockbackMultiplier) {
    }
}
