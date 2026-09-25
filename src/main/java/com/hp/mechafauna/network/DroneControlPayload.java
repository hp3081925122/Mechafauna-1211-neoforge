package com.hp.mechafauna.network;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.MechanicalDrone;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record DroneControlPayload(int entityId, boolean ascend, boolean descend, boolean firing) implements CustomPacketPayload {
    public static final Type<DroneControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mechafauna.MOD_ID, "drone_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DroneControlPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeVarInt(payload.entityId());
            buffer.writeBoolean(payload.ascend());
            buffer.writeBoolean(payload.descend());
            buffer.writeBoolean(payload.firing());
        },
        buffer -> new DroneControlPayload(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean())
    );

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TYPE, STREAM_CODEC, DroneControlPayload::handle);
    }

    private static void handle(DroneControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Entity entity = player.serverLevel().getEntity(payload.entityId());
        if (!(entity instanceof MechanicalDrone drone) || !drone.isOwnedBy(player)) {
            return;
        }
        if (player.getVehicle() == drone && drone.getControllingPassenger() == player) {
            drone.setManualControls(payload.ascend(), payload.descend(), payload.firing());
        } else if (!payload.ascend() && !payload.descend() && !payload.firing()) {
            drone.setManualControls(false, false, false);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
