package com.hp.mechafauna.client.renderer;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.item.HeavyMechMachineGunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class HeavyMechMachineGunRenderer extends GeoItemRenderer<HeavyMechMachineGunItem> {
    public HeavyMechMachineGunRenderer() {
        // 读取 geo/item、animations/item、textures/item 下的同名资源
        super(new DefaultedItemGeoModel<>(new ResourceLocation(Mechafauna.MOD_ID, "heavy_mech_machine_gun")));
    }
}
