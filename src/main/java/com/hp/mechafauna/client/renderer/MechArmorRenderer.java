package com.hp.mechafauna.client.renderer;

import com.hp.mechafauna.client.model.MechArmorModel;
import com.hp.mechafauna.item.MechArmorItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class MechArmorRenderer extends GeoArmorRenderer<MechArmorItem> {
    public MechArmorRenderer() {
        super(new MechArmorModel());
    }
}
