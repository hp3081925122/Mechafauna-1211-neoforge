package com.hp.mechafauna.client;

import java.util.List;
import net.minecraft.world.phys.Vec3;

public final class PrismSentinelRenderState extends MechLivingRenderState {
    public boolean laserFiring;
    public boolean blockHit;
    public float time;
    public float charge;
    public float power;
    public double length;
    public Vec3 muzzle = Vec3.ZERO;
    public Vec3 laserDirection = Vec3.ZERO;
    public Vec3 rayOrigin = Vec3.ZERO;
    public List<Vec3> hitPoints = List.of();
}
