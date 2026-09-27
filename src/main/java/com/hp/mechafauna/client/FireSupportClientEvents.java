package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.network.FireSupportPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

@net.neoforged.fml.common.EventBusSubscriber(modid = Mechafauna.MOD_ID, value = Dist.CLIENT)
public final class FireSupportClientEvents {
    private FireSupportClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.getConnection() == null) {
            return;
        }
        while (ModKeyMappings.FIRE_SUPPORT.consumeClick()) {
            ClientPacketDistributor.sendToServer(new FireSupportPayload());
        }
    }
}
