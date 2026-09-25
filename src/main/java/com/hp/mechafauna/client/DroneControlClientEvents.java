package com.hp.mechafauna.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.MechanicalDrone;
import com.hp.mechafauna.network.DroneControlPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DroneControlClientEvents {
    private static int lastDroneId = -1;
    private static boolean lastAscend;
    private static boolean lastDescend;
    private static boolean lastFiring;
    private static boolean mouseFiring;

    private DroneControlClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && player.getVehicle() instanceof MechanicalDrone drone && drone.isOwnedBy(player)) {
            boolean hasFocus = minecraft.screen == null;
            boolean ascend = hasFocus && minecraft.options.keyJump.isDown();
            boolean descend = hasFocus && GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
            boolean firing = hasFocus && (mouseFiring || minecraft.options.keyAttack.isDown());
            drone.setManualControls(ascend, descend, firing);
            if (lastDroneId != drone.getId() || ascend != lastAscend || descend != lastDescend || firing != lastFiring) {
                if (minecraft.getConnection() != null) {
                    PacketDistributor.sendToServer(new DroneControlPayload(drone.getId(), ascend, descend, firing));
                }
                lastDroneId = drone.getId();
                lastAscend = ascend;
                lastDescend = descend;
                lastFiring = firing;
            }
            return;
        }
        if (lastDroneId != -1) {
            if (minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(new DroneControlPayload(lastDroneId, false, false, false));
            }
            lastDroneId = -1;
            lastAscend = false;
            lastDescend = false;
            lastFiring = false;
            mouseFiring = false;
        }
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        boolean ridingOwnedDrone = minecraft.player != null && minecraft.player.getVehicle() instanceof MechanicalDrone drone && drone.isOwnedBy(minecraft.player);
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (!ridingOwnedDrone || minecraft.screen != null) {
                return;
            }
            mouseFiring = true;
            KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), false);
            while (minecraft.options.keyAttack.consumeClick()) {
            }
            event.setCanceled(true);
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            mouseFiring = false;
            if (ridingOwnedDrone && minecraft.screen == null) {
                KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), false);
                while (minecraft.options.keyAttack.consumeClick()) {
                }
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.getVehicle() instanceof MechanicalDrone drone && drone.isOwnedBy(minecraft.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHotbarKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null
            || !(minecraft.player.getVehicle() instanceof MechanicalDrone drone) || !drone.isOwnedBy(minecraft.player)) {
            return;
        }
        for (KeyMapping keyMapping : minecraft.options.keyHotbarSlots) {
            if (keyMapping.matches(event.getKey(), event.getScanCode())) {
                while (keyMapping.consumeClick()) {
                }
            }
        }
        if (minecraft.options.keyAttack.matches(event.getKey(), event.getScanCode())) {
            while (minecraft.options.keyAttack.consumeClick()) {
            }
        }
    }
}
