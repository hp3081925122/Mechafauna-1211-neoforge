package com.hp.mechafauna.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public final class SaucerLaserStrikeRenderState extends EntityRenderState {
    public int laserTick;
    public Vec3 impact = Vec3.ZERO;
}
