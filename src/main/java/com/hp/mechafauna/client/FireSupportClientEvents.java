package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.network.FireSupportPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
            PacketDistributor.sendToServer(new FireSupportPayload());
        }
    }
}
