package com.hp.mechafauna;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MechafaunaConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FLYING_SAUCER_MAX_HEALTH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("机械生物设置").push("flying_saucer_boss");
        FLYING_SAUCER_MAX_HEALTH = builder
            .comment(
                "飞碟母舰最大生命值。",
                "四个独立武器部件的血量各继承本体的 80/300。",
                "本体 300 时每个部件 80；改为 700 时每个部件约为 186.67。"
            )
            .defineInRange("maxHealth", 300, 1, 1_000_000);
        builder.pop();
        SPEC = builder.build();
    }

    private MechafaunaConfig() {
    }

    public static double flyingSaucerMaxHealth() {
        if (SPEC.isLoaded()) {
            return FLYING_SAUCER_MAX_HEALTH.get();
        }
        return FLYING_SAUCER_MAX_HEALTH.getDefault();
    }
}
