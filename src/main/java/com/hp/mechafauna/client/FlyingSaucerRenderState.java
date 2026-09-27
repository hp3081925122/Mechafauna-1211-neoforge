package com.hp.mechafauna.client;

import net.minecraft.world.phys.Vec3;

public final class FlyingSaucerRenderState extends MechLivingRenderState {
    public boolean dying;
    public int laserTick;
    public int summonTick;
    public int summonKind;
    public Vec3 laserOrigin = Vec3.ZERO;
    public Vec3 laserImpact = Vec3.ZERO;
    public Vec3 summonOrigin = Vec3.ZERO;
    public Vec3 summonGround = Vec3.ZERO;
}
