package com.hp.mechafauna.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PrismBeam extends Entity {
    private static final int KEEP_ALIVE_TICKS = 3;
    private static final EntityDataAccessor<Float> DATA_BEAM_YAW = SynchedEntityData.defineId(PrismBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_PITCH = SynchedEntityData.defineId(PrismBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BEAM_LENGTH = SynchedEntityData.defineId(PrismBeam.class, EntityDataSerializers.FLOAT);

    private UUID ownerId;
    private int life = KEEP_ALIVE_TICKS;

    public PrismBeam(EntityType<? extends PrismBeam> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BEAM_YAW, 0.0F);
        builder.define(DATA_BEAM_PITCH, 0.0F);
        builder.define(DATA_BEAM_LENGTH, 0.0F);
    }

    public void configure(Player owner, Vec3 origin, float yaw, float pitch, float length) {
        this.ownerId = owner.getUUID();
        this.life = KEEP_ALIVE_TICKS;
        this.setPos(origin);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.entityData.set(DATA_BEAM_YAW, yaw);
        this.entityData.set(DATA_BEAM_PITCH, pitch);
        this.entityData.set(DATA_BEAM_LENGTH, Math.max(0.05F, length));
    }

    public boolean ownedBy(Player player) {
        return player.getUUID().equals(this.ownerId);
    }

    public float getBeamYaw() {
        return this.entityData.get(DATA_BEAM_YAW);
    }

    public float getBeamPitch() {
        return this.entityData.get(DATA_BEAM_PITCH);
    }

    public float getBeamLength() {
        return this.entityData.get(DATA_BEAM_LENGTH);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && --this.life <= 0) {
            this.discard();
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        Vec3 end = this.position().add(Vec3.directionFromRotation(this.getBeamPitch(), this.getBeamYaw()).scale(this.getBeamLength()));
        return new AABB(this.position(), end).inflate(0.4);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
