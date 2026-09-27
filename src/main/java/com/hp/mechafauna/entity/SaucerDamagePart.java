package com.hp.mechafauna.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.neoforged.neoforge.entity.PartEntity;

public final class SaucerDamagePart extends PartEntity<FlyingSaucerBoss> {
    private final int weaponIndex;
    private final EntityDimensions dimensions;

    public SaucerDamagePart(FlyingSaucerBoss parent, int weaponIndex, float width, float height) {
        super(parent);
        this.weaponIndex = weaponIndex;
        this.dimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    public int weaponIndex() {
        return this.weaponIndex;
    }

    public void setPartPosition(double x, double centerY, double z) {
        this.setPos(x, centerY - this.dimensions.height() * 0.5, z);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean isPickable() {
        return this.getParent().isAlive()
            && (this.weaponIndex < 0 || !this.getParent().isWeaponDestroyed(this.weaponIndex));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return this.getParent().hurt(this, level, source, amount);
    }

    @Override
    public boolean is(net.minecraft.world.entity.Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.dimensions;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.isPickable();
    }
}
