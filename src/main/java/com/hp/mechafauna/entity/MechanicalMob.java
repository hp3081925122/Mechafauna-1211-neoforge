package com.hp.mechafauna.entity;

import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class MechanicalMob extends AnimatedHostileMob {
    protected MechanicalMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static UUID playerSummonOwner(Entity entity) {
        if (entity instanceof MilitaryDrone drone) {
            return drone.getOwnerUUID();
        }
        if (entity instanceof MechanicalSpider spider) {
            return spider.getPlayerSummoner();
        }
        if (entity instanceof PrismSentinel prism) {
            return prism.getSupportOwner();
        }
        return null;
    }

    public static boolean samePlayerSummon(Entity first, Entity second) {
        UUID owner = playerSummonOwner(first);
        return owner != null && owner.equals(playerSummonOwner(second));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        return super.hurt(damageSource, damageSource.is(DamageTypeTags.IS_EXPLOSION) ? amount * 0.4F : amount);
    }
}
