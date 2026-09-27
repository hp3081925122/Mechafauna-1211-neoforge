package com.hp.mechafauna.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MechLivingRenderState extends LivingEntityRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();
    public Entity entity;

    @Override
    public <D> void addGeckolibData(DataTicket<D> ticket, D value) {
        this.geckolibData.put(ticket, value);
    }

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolibData;
    }
}
