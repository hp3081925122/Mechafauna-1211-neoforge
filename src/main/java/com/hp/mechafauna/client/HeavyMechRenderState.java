package com.hp.mechafauna.client;

import net.minecraft.world.phys.Vec3;

public final class HeavyMechRenderState extends MechLivingRenderState {
    public boolean dying;
    public Vec3 position = Vec3.ZERO;
    public float time;
    public String action;
    public float skillTime;
    public float chargeYaw;
    public Vec3 chargeOrigin = Vec3.ZERO;
    public int overloadTicks;
    public boolean overloaded;
}
