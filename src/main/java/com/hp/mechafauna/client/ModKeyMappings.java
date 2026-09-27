package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "general"));
    public static final KeyMapping FIRE_SUPPORT = new KeyMapping(
        "key.mechafauna.fire_support",
        GLFW.GLFW_KEY_X,
        CATEGORY
    );

    private ModKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(FIRE_SUPPORT);
    }
}
