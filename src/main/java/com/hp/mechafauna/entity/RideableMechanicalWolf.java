package com.hp.mechafauna.entity;

import com.hp.mechafauna.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class RideableMechanicalWolf extends Mob implements GeoEntity, OwnableEntity, PlayerRideableJumping {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(
        RideableMechanicalWolf.class, EntityDataSerializers.OPTIONAL_UUID);
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private int jumpPower;

    public RideableMechanicalWolf(EntityType<? extends RideableMechanicalWolf> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.42)
            .add(ForgeMod.STEP_HEIGHT_ADDITION.get(), 2.4)
            .add(Attributes.JUMP_STRENGTH, 0.65)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    public void setOwner(Player owner) {
        this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
        this.setPersistenceRequired();
    }

    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER).orElse(null);
    }

    public boolean isOwnedBy(Entity entity) {
        UUID owner = this.getOwnerUUID();
        return owner != null && owner.equals(entity.getUUID());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                this.recall(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    private void recall(Player player) {
        this.ejectPassengers();
        ItemStack stack = new ItemStack(ModItems.MECHANICAL_WOLF_SUMMON.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        this.discard();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player player && this.isOwnedBy(player) && !this.isVehicle();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (this.getFirstPassenger() instanceof Player player && this.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        this.setRot(player.getYRot(), player.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        if (this.isControlledByLocalInstance() && this.onGround() && this.jumpPower > 0) {
            float scale = this.jumpPower >= 90 ? 1.0F : 0.4F + 0.4F * this.jumpPower / 90.0F;
            this.jumpPower = 0;
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, this.getJumpPower() * scale, motion.z);
            this.hasImpulse = true;
        }
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float sideways = player.xxa * 0.5F;
        float forward = player.zza;
        if (forward <= 0.0F) {
            forward *= 0.25F;
        }
        return new Vec3(sideways, 0.0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public void onPlayerJump(int power) {
        if (power > 0) {
            this.jumpPower = power;
        }
    }

    @Override
    public boolean canJump() {
        return this.getControllingPassenger() instanceof Player;
    }

    @Override
    public void handleStartJump(int power) {
        if (power > 0) {
            this.jumpPower = power;
        }
        this.level().playSound(null, this, SoundEvents.WOLF_AMBIENT, SoundSource.NEUTRAL, 0.5F, 1.5F);
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (state.getLimbSwingAmount() <= 0.05F) {
                return PlayState.STOP;
            }
            state.setAnimation(RawAnimation.begin().thenLoop("run"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = this.getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
            this.setPersistenceRequired();
        }
    }
}
