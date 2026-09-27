package com.hp.mechafauna.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public final class MechHumanoidRenderState extends HumanoidRenderState implements GeoRenderState {
    private final Map<DataTicket<?>, Object> geckolibData = new HashMap<>();

    @Override
    public <D> void addGeckolibData(DataTicket<D> ticket, D value) {
        this.geckolibData.put(ticket, value);
    }

    @Override
    public Map<DataTicket<?>, Object> getDataMap() {
        return this.geckolibData;
    }
}
