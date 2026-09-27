package com.hp.mechafauna.mixin.client;

import com.hp.mechafauna.client.SaucerBossMusicEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class SaucerBossMusicMixin {
    @Inject(method = "getSituationalMusic", at = @At("RETURN"), cancellable = true)
    private void mechafauna$selectBossMusic(CallbackInfoReturnable<Music> callbackInfo) {
        callbackInfo.setReturnValue(SaucerBossMusicEvents.overrideMusic(callbackInfo.getReturnValue()));
    }
}
