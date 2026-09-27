package com.hp.mechafauna.network;

import com.hp.mechafauna.item.FireSupport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record FireSupportPayload() {
    public static void encode(FireSupportPayload payload, FriendlyByteBuf buffer) {
    }

    public static FireSupportPayload decode(FriendlyByteBuf buffer) {
        return new FireSupportPayload();
    }

    public static void handle(FireSupportPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                FireSupport.activate(player);
            }
        });
        context.setPacketHandled(true);
    }
}
