package com.hp.mechafauna.client;

import net.minecraft.world.phys.Vec3;

public final class FlyingSaucerRenderState extends MechLivingRenderState {
    public boolean dying;
    public boolean alive;
    public int destroyedWeapons;
    public int skill;
    public float skillTime;
    public float time;
    public Vec3 position = Vec3.ZERO;
    public Vec3 skillPoint = Vec3.ZERO;
    public Vec3 skillOrigin = Vec3.ZERO;
    public Vec3 gravityGround = Vec3.ZERO;
    public Vec3[] haloGround = new Vec3[6];
    public int laserTick;
    public int summonTick;
    public int summonKind;
    public Vec3 laserOrigin = Vec3.ZERO;
    public Vec3 laserImpact = Vec3.ZERO;
    public Vec3 summonOrigin = Vec3.ZERO;
    public Vec3 summonGround = Vec3.ZERO;
}
