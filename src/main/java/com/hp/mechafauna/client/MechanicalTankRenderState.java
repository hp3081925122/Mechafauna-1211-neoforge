package com.hp.mechafauna.client;

import net.minecraft.world.phys.Vec3;

public final class MechanicalTankRenderState extends MechLivingRenderState {
    public boolean dying;
    public Vec3 position = Vec3.ZERO;
    public float time;
    public String action;
    public float skillTime;
    public Vec3 cannonMuzzle;
    public Vec3 railStart = Vec3.ZERO;
    public Vec3 railEnd = Vec3.ZERO;
    public boolean shielded;
    public int shieldTicks;
    public int shieldMaxTicks;
    public int shieldHitTick;
    public int shieldBurstTick;
}
