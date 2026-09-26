package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.entity.FlyingSaucerWeapon;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SaucerBossBarEvents {
    private static final int BAR_WIDTH = 182;
    private static final int GAP = 4;
    private static final int SMALL_BAR_HEIGHT = 3;
    private static final int SECTION_HEIGHT = 16;

    private SaucerBossBarEvents() {
    }

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        // 找到这条 Boss 血条对应的飞碟母舰
        UUID barId = event.getBossEvent().getId();
        FlyingSaucerBoss saucer = null;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof FlyingSaucerBoss candidate && candidate.getBossBarId().filter(barId::equals).isPresent()) {
                saucer = candidate;
                break;
            }
        }
        if (saucer == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        FlyingSaucerWeapon[] weapons = FlyingSaucerWeapon.values();
        int smallWidth = (BAR_WIDTH - GAP * (weapons.length - 1)) / weapons.length;
        int top = event.getY() + 7;
        // 主血条下方横向排列四个武器血条
        for (FlyingSaucerWeapon weapon : weapons) {
            int left = event.getX() + weapon.ordinal() * (smallWidth + GAP);
            float maxHealth = saucer.getWeaponMaxHealth();
            float ratio = maxHealth <= 0.0F ? 0.0F : Mth.clamp(saucer.getWeaponHealth(weapon.ordinal()) / maxHealth, 0.0F, 1.0F);
            boolean destroyed = saucer.isWeaponDestroyed(weapon.ordinal());
            // 武器名称，摧毁后变灰
            Component name = Component.translatable("entity.mechafauna.flying_saucer_boss.part." + weapon.name().toLowerCase(Locale.ROOT));
            int nameColor = 0xFFFFFF;
            if (destroyed) {
                nameColor = 0x777777;
            }
            graphics.pose().pushPose();
            graphics.pose().translate(left + smallWidth / 2.0F, top, 0.0F);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(minecraft.font, name, -minecraft.font.width(name) / 2, 0, nameColor);
            graphics.pose().popPose();
            // 边框、底色和剩余血量
            int barTop = top + 6;
            graphics.fill(left - 1, barTop - 1, left + smallWidth + 1, barTop + SMALL_BAR_HEIGHT + 1, 0xFF000000);
            graphics.fill(left, barTop, left + smallWidth, barTop + SMALL_BAR_HEIGHT, 0xFF3A1010);
            if (!destroyed) {
                int red = (int)Mth.lerp(ratio, 230.0F, 255.0F);
                int green = (int)Mth.lerp(ratio, 40.0F, 150.0F);
                int fillColor = 0xFF000000 | red << 16 | green << 8 | 40;
                graphics.fill(left, barTop, left + Math.round(smallWidth * ratio), barTop + SMALL_BAR_HEIGHT, fillColor);
            }
        }
        // 给下一条 Boss 血条留出空间
        event.setIncrement(event.getIncrement() + SECTION_HEIGHT);
    }
}
