package com.hp.mechafauna.entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class MechanicalDrone extends MechanicalMob implements OwnableEntity {
    private static final String GUN = "gun";
    private static final String HOVER = "animation.mechanical_drone.hover";
    private static final String ROTORS = "animation.mechanical_drone.rotors";
    private static final double KNOCKBACK_RESISTANCE = 0.20;
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(MechanicalDrone.class, EntityDataSerializers.OPTIONAL_UUID);
    private boolean manualAscend;
    private boolean manualDescend;
    private boolean manualFiring;

    public MechanicalDrone(EntityType<? extends MechanicalDrone> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new AltitudeCappedMoveControl(this);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isOwnedDrone()) {
            if (!this.level().isClientSide) {
                if (!(this.getControllingPassenger() instanceof Player player) || !this.isOwnedBy(player)) {
                    this.setManualControls(false, false, false);
                } else if (this.manualFiring && this.tickCount % 4 == 0) {
                    LivingEntity target = this.getAimedHostile(player);
                    if (target != null) {
                        this.launchProjectile(target, WeaponMount.DRONE_GUN, WeaponMount.DRONE_GUN.muzzle(), false, true, 2.8, 1.0F);
                    }
                }
            }
            return;
        }
        if (!this.level().isClientSide) {
            double ceiling = this.getFlightCeiling();
            if (this.getY() > ceiling) {
                this.getMoveControl().setWantedPosition(this.getX(), ceiling, this.getZ(), 1.0);
            }
        }
    }

    public void setOwner(Player owner) {
        this.entityData.set(DATA_OWNER_UUID, Optional.of(owner.getUUID()));
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.setTarget(null);
    }

    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public boolean isOwnedDrone() {
        return this.entityData.get(DATA_OWNER_UUID).isPresent();
    }

    public boolean isOwnedBy(Entity entity) {
        UUID owner = this.getOwnerUUID();
        return owner != null && owner.equals(entity.getUUID());
    }

    public void setManualControls(boolean ascend, boolean descend, boolean firing) {
        boolean startedFiring = firing && !this.manualFiring;
        this.manualAscend = ascend;
        this.manualDescend = descend;
        this.manualFiring = firing;
        if (startedFiring && !this.level().isClientSide) {
            this.triggerAnim("action", GUN);
        }
    }

    @Override
    public LivingEntity getControllingPassenger() {
        if (this.isOwnedDrone() && this.getFirstPassenger() instanceof Player player && this.isOwnedBy(player)) {
            return player;
        }
        return super.getControllingPassenger();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.isOwnedDrone() && passenger instanceof Player player && this.isOwnedBy(player) && super.canAddPassenger(passenger);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isOwnedBy(player) || !player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        return player.startRiding(this) ? InteractionResult.sidedSuccess(false) : InteractionResult.PASS;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        this.setRot(player.getYRot(), 0.0F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        double vertical = this.manualAscend == this.manualDescend ? 0.0 : this.manualAscend ? 1.0 : -1.0;
        return new Vec3(player.xxa, vertical, player.zza);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float)this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isOwnedDrone() && this.getControllingPassenger() instanceof Player && this.isControlledByLocalInstance()) {
            this.moveRelative(0.085F, input);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.82));
            this.calculateEntityAnimation(true);
            return;
        }
        super.travel(input);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (this.isOwnedBy(other)) {
            return true;
        }
        LivingEntity owner = this.getOwner();
        return owner != null && owner.isAlliedTo(other) || super.isAlliedTo(other);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
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
            this.entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID("Owner")));
            this.setNoAi(true);
        }
    }

    private LivingEntity getAimedHostile(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double range = 48.0;
        AABB searchBounds = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        LivingEntity bestTarget = null;
        double bestDistance = range;
        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, searchBounds,
            entity -> entity != player && entity instanceof Enemy && entity.isAlive() && !this.isAlliedTo(entity))) {
            Vec3 offset = target.getBoundingBox().getCenter().subtract(eye);
            double forwardDistance = offset.dot(look);
            if (forwardDistance <= 0.0 || forwardDistance >= bestDistance || !player.hasLineOfSight(target)) {
                continue;
            }
            double missDistance = offset.subtract(look.scale(forwardDistance)).lengthSqr();
            double hitRadius = target.getBbWidth() * 0.5 + 0.55;
            if (missDistance <= hitRadius * hitRadius) {
                bestTarget = target;
                bestDistance = forwardDistance;
            }
        }
        return bestTarget;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAttributes(22.0, 0.54, 1.0, 32.0, KNOCKBACK_RESISTANCE).add(Attributes.FLYING_SPEED, 0.72);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected String getMovementAnimation() {
        return HOVER;
    }

    @Override
    protected String getIdleAnimation() {
        return HOVER;
    }

    @Override
    protected float getCombatTurnSpeed() {
        return 28.0F;
    }

    @Override
    protected boolean shouldContinueAction(String action, int actionTick, LivingEntity target) {
        return GUN.equals(action)
            && target.isAlive()
            && this.hasLineOfSight(target)
            && this.distanceToSqr(target) <= 18.0 * 18.0;
    }

    @Override
    protected Map<String, String> getActionAnimations() {
        return Map.of(GUN, "animation.mechanical_drone.fire");
    }

    @Override
    protected void addExtraControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "rotors", 0, state -> {
            state.setAnimation(RawAnimation.begin().thenLoop(ROTORS));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public List<String> getAvailableActions(LivingEntity target, double distanceSquared) {
        return distanceSquared <= 17.0 * 17.0 ? List.of(GUN) : List.of();
    }

    @Override
    public int getActionDuration(String action) {
        return 33;
    }

    @Override
    public int getActionCooldown(String action) {
        return 46;
    }

    @Override
    public double getCombatHoldDistance() {
        return 9.5;
    }

    @Override
    public double getChaseSpeed() {
        return 1.0;
    }

    @Override
    protected void onActionStarted(String action, LivingEntity target) {
        this.level().playSound(null, this.blockPosition(), SoundEvents.CROSSBOW_SHOOT, SoundSource.HOSTILE, 0.65F, 1.45F);
    }

    @Override
    protected void onActionTick(String action, int actionTick, LivingEntity target) {
        if (GUN.equals(action) && actionTick >= 5 && actionTick <= 29 && actionTick % 3 == 2) {
            this.launchProjectile(target, WeaponMount.DRONE_GUN, WeaponMount.DRONE_GUN.muzzle(), false, true, 2.8, 1.0F);
        }
    }

    private double getFlightCeiling() {
        int groundHeight = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(this.getX()), Mth.floor(this.getZ()));
        return groundHeight + 3.0;
    }

    private static final class AltitudeCappedMoveControl extends FlyingMoveControl {
        private final MechanicalDrone drone;

        private AltitudeCappedMoveControl(MechanicalDrone drone) {
            super(drone, 20, true);
            this.drone = drone;
        }

        @Override
        public void tick() {
            if (this.hasWanted()) {
                this.wantedY = Math.min(this.wantedY, this.drone.getFlightCeiling());
            } else if (this.drone.getY() > this.drone.getFlightCeiling()) {
                this.setWantedPosition(this.drone.getX(), this.drone.getFlightCeiling(), this.drone.getZ(), 1.0);
            }
            super.tick();
        }
    }
}
