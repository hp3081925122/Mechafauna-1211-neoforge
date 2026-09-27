package com.hp.mechafauna.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class PrismBeamRenderState extends EntityRenderState {
    public float age;
    public double length;
    public Vec3 direction = Vec3.ZERO;
}
