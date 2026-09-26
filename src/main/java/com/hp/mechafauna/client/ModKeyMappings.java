package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final KeyMapping FIRE_SUPPORT = new KeyMapping(
        "key.mechafauna.fire_support",
        GLFW.GLFW_KEY_X,
        "key.categories.mechafauna"
    );

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(FIRE_SUPPORT);
    }
}
