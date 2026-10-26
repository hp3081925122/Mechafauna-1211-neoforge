package com.hp.mechafauna;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MechafaunaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue FLYING_SAUCER_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue FLYING_SAUCER_ATTACK_DAMAGE;
    public static final ModConfigSpec.DoubleValue FLYING_SAUCER_RAGE_REDUCTION;
    public static final ModConfigSpec.IntValue FLYING_SAUCER_SKILL_INTERVAL;
    public static final ModConfigSpec.BooleanValue FLYING_SAUCER_PERCENT_DAMAGE;
    public static final ModConfigSpec.DoubleValue FLYING_SAUCER_PERCENT_PER_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue FLYING_SAUCER_GRAVITY_WELL;
    public static final ModConfigSpec.DoubleValue FLYING_SAUCER_GRAVITY_WELL_MULTIPLIER;
    public static final ModConfigSpec.IntValue FLYING_SAUCER_GRAVITY_WELL_COOLDOWN;
    public static final ModConfigSpec.BooleanValue FLYING_SAUCER_HALO_SWEEP;
    public static final ModConfigSpec.DoubleValue FLYING_SAUCER_HALO_SWEEP_MULTIPLIER;
    public static final ModConfigSpec.IntValue FLYING_SAUCER_HALO_SWEEP_COOLDOWN;
    public static final ModConfigSpec.BooleanValue FLYING_SAUCER_PLASMA_RAIN;
    public static final ModConfigSpec.DoubleValue FLYING_SAUCER_PLASMA_RAIN_MULTIPLIER;
    public static final ModConfigSpec.IntValue FLYING_SAUCER_PLASMA_RAIN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue HEAVY_MECH_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue HEAVY_MECH_ATTACK_DAMAGE;
    public static final ModConfigSpec.BooleanValue HEAVY_MECH_ORBITAL_BARRAGE;
    public static final ModConfigSpec.DoubleValue HEAVY_MECH_ORBITAL_BARRAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue HEAVY_MECH_ORBITAL_BARRAGE_COOLDOWN;
    public static final ModConfigSpec.BooleanValue HEAVY_MECH_ROCKET_CHARGE;
    public static final ModConfigSpec.DoubleValue HEAVY_MECH_ROCKET_CHARGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue HEAVY_MECH_ROCKET_CHARGE_COOLDOWN;
    public static final ModConfigSpec.BooleanValue HEAVY_MECH_OVERLOAD;
    public static final ModConfigSpec.DoubleValue HEAVY_MECH_OVERLOAD_HEALTH_RATIO;
    public static final ModConfigSpec.DoubleValue HEAVY_MECH_OVERLOAD_PULSE_MULTIPLIER;
    public static final ModConfigSpec.IntValue HEAVY_MECH_OVERLOAD_DURATION;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_ATTACK_DAMAGE;
    public static final ModConfigSpec.BooleanValue MECHANICAL_TANK_RAILGUN;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_RAILGUN_MULTIPLIER;
    public static final ModConfigSpec.IntValue MECHANICAL_TANK_RAILGUN_COOLDOWN;
    public static final ModConfigSpec.BooleanValue MECHANICAL_TANK_CLUSTER_MORTAR;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_CLUSTER_MORTAR_MULTIPLIER;
    public static final ModConfigSpec.IntValue MECHANICAL_TANK_CLUSTER_MORTAR_COOLDOWN;
    public static final ModConfigSpec.BooleanValue MECHANICAL_TANK_SHIELD;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_SHIELD_FIRST_RATIO;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_SHIELD_SECOND_RATIO;
    public static final ModConfigSpec.IntValue MECHANICAL_TANK_SHIELD_DURATION;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_SHIELD_REDUCTION;
    public static final ModConfigSpec.DoubleValue MECHANICAL_TANK_SHIELD_BURST_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("机械生物设置").push("flying_saucer_boss");
        FLYING_SAUCER_MAX_HEALTH = builder
            .comment(
                "飞碟母舰最大生命值。",
                "四个独立武器部件的血量各继承本体的 80/300。",
                "本体 300 时每个部件 80；改为 700 时每个部件约为 186.67。"
            )
            .defineInRange("maxHealth", 300, 1, 1_000_000);
        FLYING_SAUCER_ATTACK_DAMAGE = builder.comment("基础攻击力。新技能伤害 = 基础攻击力 × 技能倍率。")
            .defineInRange("attackDamage", 10.0, 0.0, 10_000.0);
        FLYING_SAUCER_RAGE_REDUCTION = builder.comment(
                "残骸暴走：每摧毁一个武器部件，技能与下射激光冷却缩短的比例。",
                "0.15 = 每个部件 -15%，四个全毁时冷却为原来的 40%（最低不低于 40%）。"
            )
            .defineInRange("cooldownReductionPerDestroyedWeapon", 0.15, 0.0, 0.25);
        FLYING_SAUCER_SKILL_INTERVAL = builder.comment("两次特殊技能之间的最短间隔（tick），同样受残骸暴走缩短。")
            .defineInRange("skillIntervalTicks", 80, 0, 72_000);
        builder.push("percent_damage");
        FLYING_SAUCER_PERCENT_DAMAGE = builder.comment(
                "是否启用百分比伤害：飞碟的所有攻击命中非玩家生物时，额外造成目标最大生命值百分比的伤害。",
                "每次命中的百分比 = 该次攻击倍率 × percentPerMultiplier，攻击倍率 = 该次伤害 ÷ 飞碟基础攻击力。"
            )
            .define("enabled", true);
        FLYING_SAUCER_PERCENT_PER_MULTIPLIER = builder.comment("每 1.0 攻击倍率对应的最大生命值百分比（0.04 = 4%）。")
            .defineInRange("percentPerMultiplier", 0.04, 0.0, 1.0);
        builder.pop();
        builder.push("gravity_well");
        FLYING_SAUCER_GRAVITY_WELL = builder.comment("是否启用引力奇点：向目标投下奇点，持续牵引周围单位后坍缩爆发。")
            .define("enabled", true);
        FLYING_SAUCER_GRAVITY_WELL_MULTIPLIER = builder.comment("坍缩爆发伤害倍率，奇点核心的持续伤害为其 10%。")
            .defineInRange("damageMultiplier", 1.8, 0.0, 100.0);
        FLYING_SAUCER_GRAVITY_WELL_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 360, 20, 72_000);
        builder.pop();
        builder.push("halo_sweep");
        FLYING_SAUCER_HALO_SWEEP = builder.comment("是否启用光环扫射：悬停后从舰体边缘射出六道旋转光束，螺旋向外扫过地面。")
            .define("enabled", true);
        FLYING_SAUCER_HALO_SWEEP_MULTIPLIER = builder.comment("每次被光束扫中的伤害倍率。")
            .defineInRange("damageMultiplier", 0.6, 0.0, 100.0);
        FLYING_SAUCER_HALO_SWEEP_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 320, 20, 72_000);
        builder.pop();
        builder.push("plasma_rain");
        FLYING_SAUCER_PLASMA_RAIN = builder.comment("是否启用等离子暴雨：连续标记落点，等离子弹从天而降，部分落点预判目标走位。")
            .define("enabled", true);
        FLYING_SAUCER_PLASMA_RAIN_MULTIPLIER = builder.comment("每发等离子弹的伤害倍率。")
            .defineInRange("damageMultiplier", 1.0, 0.0, 100.0);
        FLYING_SAUCER_PLASMA_RAIN_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 280, 20, 72_000);
        builder.pop();
        builder.pop();

        builder.comment("重装机甲（精英）设置。新技能伤害 = 基础攻击力 × 技能倍率。").push("heavy_mech");
        HEAVY_MECH_MAX_HEALTH = builder.comment("最大生命值，仅对新生成的机甲生效。")
            .defineInRange("maxHealth", 180.0, 1.0, 1_000_000.0);
        HEAVY_MECH_ATTACK_DAMAGE = builder.comment("基础攻击力。")
            .defineInRange("attackDamage", 8.0, 0.0, 10_000.0);
        builder.push("orbital_barrage");
        HEAVY_MECH_ORBITAL_BARRAGE = builder.comment("是否启用轨道导弹齐射：向天发射导弹，在目标周围标记警示圈后集中轰炸。")
            .define("enabled", true);
        HEAVY_MECH_ORBITAL_BARRAGE_MULTIPLIER = builder.comment("每枚落弹的伤害倍率。")
            .defineInRange("damageMultiplier", 1.5, 0.0, 100.0);
        HEAVY_MECH_ORBITAL_BARRAGE_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 260, 20, 72_000);
        builder.pop();
        builder.push("rocket_charge");
        HEAVY_MECH_ROCKET_CHARGE = builder.comment("是否启用火箭冲撞：蓄力后沿地面警示线高速冲刺。")
            .define("enabled", true);
        HEAVY_MECH_ROCKET_CHARGE_MULTIPLIER = builder.comment("冲撞伤害倍率，撞墙震波为其 60%。")
            .defineInRange("damageMultiplier", 1.4, 0.0, 100.0);
        HEAVY_MECH_ROCKET_CHARGE_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 200, 20, 72_000);
        builder.pop();
        builder.push("overload");
        HEAVY_MECH_OVERLOAD = builder.comment("是否启用核心过载阶段：血量降到阈值时释放 EMP 冲击波并进入过载状态。")
            .define("enabled", true);
        HEAVY_MECH_OVERLOAD_HEALTH_RATIO = builder.comment("触发阈值，按最大生命值百分比计算（0.5 = 半血）。")
            .defineInRange("healthRatio", 0.5, 0.0, 1.0);
        HEAVY_MECH_OVERLOAD_PULSE_MULTIPLIER = builder.comment("EMP 冲击波伤害倍率。")
            .defineInRange("pulseDamageMultiplier", 1.2, 0.0, 100.0);
        HEAVY_MECH_OVERLOAD_DURATION = builder.comment("过载状态持续时间（tick），期间移速提升、技能冷却缩短。")
            .defineInRange("durationTicks", 400, 20, 72_000);
        builder.pop();
        builder.pop();

        builder.comment("机械坦克（精英）设置。新技能伤害 = 基础攻击力 × 技能倍率。").push("mechanical_tank");
        MECHANICAL_TANK_MAX_HEALTH = builder.comment("最大生命值，仅对新生成的坦克生效。")
            .defineInRange("maxHealth", 105.0, 1.0, 1_000_000.0);
        MECHANICAL_TANK_ATTACK_DAMAGE = builder.comment("基础攻击力。")
            .defineInRange("attackDamage", 6.0, 0.0, 10_000.0);
        builder.push("railgun");
        MECHANICAL_TANK_RAILGUN = builder.comment("是否启用电磁轨道炮：蓄能并用激光锁定后发射贯穿光束。")
            .define("enabled", true);
        MECHANICAL_TANK_RAILGUN_MULTIPLIER = builder.comment("贯穿光束伤害倍率。")
            .defineInRange("damageMultiplier", 2.4, 0.0, 100.0);
        MECHANICAL_TANK_RAILGUN_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 220, 20, 72_000);
        builder.pop();
        builder.push("cluster_mortar");
        MECHANICAL_TANK_CLUSTER_MORTAR = builder.comment("是否启用集束迫击炮：高抛子母弹，在目标周围连环落弹。")
            .define("enabled", true);
        MECHANICAL_TANK_CLUSTER_MORTAR_MULTIPLIER = builder.comment("每枚子弹的伤害倍率。")
            .defineInRange("damageMultiplier", 1.3, 0.0, 100.0);
        MECHANICAL_TANK_CLUSTER_MORTAR_COOLDOWN = builder.comment("冷却时间（tick）。")
            .defineInRange("cooldownTicks", 240, 20, 72_000);
        builder.pop();
        builder.push("aegis_shield");
        MECHANICAL_TANK_SHIELD = builder.comment("是否启用能量护盾阶段：血量降到阈值时展开护盾，结束时爆裂产生冲击波。")
            .define("enabled", true);
        MECHANICAL_TANK_SHIELD_FIRST_RATIO = builder.comment("第一次展开阈值，按最大生命值百分比计算。")
            .defineInRange("firstHealthRatio", 0.5, 0.0, 1.0);
        MECHANICAL_TANK_SHIELD_SECOND_RATIO = builder.comment("第二次展开阈值，按最大生命值百分比计算，设为 0 关闭第二次。")
            .defineInRange("secondHealthRatio", 0.25, 0.0, 1.0);
        MECHANICAL_TANK_SHIELD_DURATION = builder.comment("护盾持续时间（tick）。")
            .defineInRange("durationTicks", 160, 20, 72_000);
        MECHANICAL_TANK_SHIELD_REDUCTION = builder.comment("护盾期间的减伤比例（0.6 = 减免 60%）。")
            .defineInRange("damageReduction", 0.6, 0.0, 1.0);
        MECHANICAL_TANK_SHIELD_BURST_MULTIPLIER = builder.comment("护盾爆裂冲击波伤害倍率。")
            .defineInRange("burstDamageMultiplier", 1.0, 0.0, 100.0);
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private MechafaunaConfig() {
    }

    public static double flyingSaucerMaxHealth() {
        return get(FLYING_SAUCER_MAX_HEALTH);
    }

    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
}
