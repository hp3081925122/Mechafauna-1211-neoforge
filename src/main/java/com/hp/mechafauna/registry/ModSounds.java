package com.hp.mechafauna.registry;

import com.hp.mechafauna.Mechafauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Mechafauna.MOD_ID);
    // 飞碟母舰战斗音乐
    public static final RegistryObject<SoundEvent> FLYING_SAUCER_BOSS_MUSIC = SOUND_EVENTS.register(
        "music.flying_saucer_boss",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Mechafauna.MOD_ID, "music.flying_saucer_boss"))
    );

    private ModSounds() {
    }
}
