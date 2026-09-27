package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.hp.mechafauna.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.Music;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

public final class SaucerBossMusicEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MUSIC_RANGE = 96.0;
    private static Music music;
    private static boolean active;

    private SaucerBossMusicEvents() {
    }

    public static Music overrideMusic(Music fallback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            if (active) {
                active = false;
                LOGGER.debug("Saucer boss music active=false");
            }
            return fallback;
        }
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
            return fallback;
        }
        if (music == null) {
            music = new Music(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.FLYING_SAUCER_BOSS_MUSIC.get()), 0, 0, true);
        }
        return music;
    }
}
