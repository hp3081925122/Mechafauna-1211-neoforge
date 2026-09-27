package com.hp.mechafauna.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.hp.mechafauna.entity.MechProjectile;

public final class MechEntityRenderState extends EntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();
    public MechProjectile projectile;
    public float yaw;
    public float pitch;
    public boolean laser;
    public boolean cyanLaser;
    public boolean prismLaser;

    @Override
    public <D> void addGeckolibData(DataTicket<D> ticket, D value) {
        this.geckolibData.put(ticket, value);
    }

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolibData;
    }
}
