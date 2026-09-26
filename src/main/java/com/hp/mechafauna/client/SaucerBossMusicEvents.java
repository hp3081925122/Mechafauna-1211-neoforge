package com.hp.mechafauna.client;

import com.hp.mechafauna.Mechafauna;
import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import org.slf4j.Logger;

@EventBusSubscriber(modid = Mechafauna.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class SaucerBossMusicEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MUSIC_RANGE = 96.0;
    private static Music music;
    private static boolean active;

    private SaucerBossMusicEvents() {
    }

    @SubscribeEvent
    public static void onSelectMusic(SelectMusicEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        // 附近存在存活的飞碟母舰时切换为战斗音乐
        boolean nearby = false;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof FlyingSaucerBoss saucer && saucer.isAlive()
                && saucer.distanceToSqr(minecraft.player) <= MUSIC_RANGE * MUSIC_RANGE) {
                nearby = true;
                break;
            }
        }
        if (nearby != active) {
            active = nearby;
            LOGGER.debug("Saucer boss music active={}", active);
        }
        if (!nearby) {
            return;
        }
        // 立即替换当前音乐，播完后无间隔循环
        if (music == null) {
            music = new Music(ModSounds.FLYING_SAUCER_BOSS_MUSIC, 0, 0, true);
        }
        event.overrideMusic(music);
    }
}
