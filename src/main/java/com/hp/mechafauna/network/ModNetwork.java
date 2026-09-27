package com.hp.mechafauna.network;

import com.hp.mechafauna.Mechafauna;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(Mechafauna.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, DroneControlPayload.class, DroneControlPayload::encode, DroneControlPayload::decode,
            DroneControlPayload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, FireSupportPayload.class, FireSupportPayload::encode, FireSupportPayload::decode,
            FireSupportPayload::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
