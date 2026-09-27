package com.hp.mechafauna.network;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.item.FireSupport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record FireSupportPayload() implements CustomPacketPayload {
    public static final Type<FireSupportPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "fire_support"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireSupportPayload> STREAM_CODEC = StreamCodec.unit(new FireSupportPayload());

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TYPE, STREAM_CODEC, FireSupportPayload::handle);
    }

    private static void handle(FireSupportPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            FireSupport.activate(player);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
