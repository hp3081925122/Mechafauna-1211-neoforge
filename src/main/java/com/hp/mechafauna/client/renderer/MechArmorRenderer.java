package com.hp.mechafauna.client.renderer;

import com.hp.mechafauna.client.model.MechArmorModel;
import com.hp.mechafauna.item.MechArmorItem;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import com.geckolib.renderer.GeoArmorRenderer;

public final class MechArmorRenderer extends GeoArmorRenderer<MechArmorItem, HumanoidRenderState> {
    public MechArmorRenderer() {
        super(new MechArmorModel());
    }
}
