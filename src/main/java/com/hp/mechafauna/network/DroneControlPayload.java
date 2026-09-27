package com.hp.mechafauna.network;

import com.hp.mechafauna.entity.MechanicalDrone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record DroneControlPayload(int entityId, boolean ascend, boolean descend, boolean firing) {
    public static void encode(DroneControlPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.entityId());
        buffer.writeBoolean(payload.ascend());
        buffer.writeBoolean(payload.descend());
        buffer.writeBoolean(payload.firing());
    }

    public static DroneControlPayload decode(FriendlyByteBuf buffer) {
        return new DroneControlPayload(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(DroneControlPayload payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
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
        });
        context.setPacketHandled(true);
    }
}
