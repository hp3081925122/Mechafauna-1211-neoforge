package com.hp.mechafauna.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class MechProjectile extends ThrowableItemProjectile {
    public MechProjectile(EntityType<? extends MechProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public MechProjectile(EntityType<? extends MechProjectile> entityType, Level level, LivingEntity owner, boolean missile) {
        super(entityType, owner, level);
        this.setItem(new ItemStack(missile ? Items.FIREWORK_ROCKET : Items.IRON_NUGGET));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected Item getDefaultItem() {
        return Items.IRON_NUGGET;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!this.level().isClientSide && hit.getEntity() instanceof LivingEntity livingTarget) {
            float damage = this.getItem().is(Items.FIREWORK_ROCKET) ? 4.0F : 8.0F;
            livingTarget.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide) {
            float radius = this.getItem().is(Items.FIREWORK_ROCKET) ? 2.4F : 1.35F;
            net.minecraft.world.entity.Entity explosionSource = this.getOwner() == null ? this : this.getOwner();
            this.level().explode(explosionSource, this.getX(), this.getY(), this.getZ(), radius, false, ExplosionInteraction.NONE);
            this.discard();
        }
    }
}
