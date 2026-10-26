package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static com.hp.mechafauna.client.VfxShapes.axisRing;
import static com.hp.mechafauna.client.VfxShapes.cylinder;
import static com.hp.mechafauna.client.VfxShapes.dashedRing;
import static com.hp.mechafauna.client.VfxShapes.disc;
import static com.hp.mechafauna.client.VfxShapes.flatRing;
import static com.hp.mechafauna.client.VfxShapes.ribbon;
import static com.hp.mechafauna.client.VfxShapes.sphere;
import static com.hp.mechafauna.client.VfxShapes.tube;
import static com.hp.mechafauna.client.VfxShapes.wall;

final class SaucerSkillRenderer {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);

    private SaucerSkillRenderer() {
    }

    static void render(VertexConsumer vertices, Matrix4f matrix, FlyingSaucerBoss saucer, float partialTick) {
        Vec3 position = saucer.getPosition(partialTick);
        int destroyed = saucer.destroyedWeaponCount();
        if (destroyed > 0 && saucer.isAlive()) {
            renderRage(vertices, matrix, saucer.getLaserOrigin(position).subtract(position), saucer.tickCount + partialTick, destroyed);
        }
        int skill = saucer.getSkillId();
        if (skill == FlyingSaucerBoss.SKILL_NONE) {
            return;
        }
        float time = saucer.getSkillTick() + partialTick;
        Vec3 origin = saucer.getLaserOrigin(position).subtract(position);
        Vec3 point = saucer.getSkillPoint();
        switch (skill) {
            case FlyingSaucerBoss.SKILL_GRAVITY_WELL -> {
                Vec3 ground = saucer.snapToGround(point, 0.5, 24.0).subtract(position);
                renderGravityWell(vertices, matrix, saucer.getSkillOrigin().subtract(position), point.subtract(position), ground, time);
            }
            case FlyingSaucerBoss.SKILL_HALO_SWEEP -> renderHaloSweep(vertices, matrix, saucer, origin, point, position, time);
            case FlyingSaucerBoss.SKILL_PLASMA_RAIN -> renderPlasmaRain(vertices, matrix, origin, time);
            default -> {
            }
        }
    }

    private static void renderRage(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, float time, int destroyed) {
        float intensity = destroyed / 4.0F;
        float flicker = 0.7F + 0.3F * Mth.sin(time * (0.4F + intensity * 0.9F));
        flatRing(vertices, matrix, origin.add(0.0, 0.2, 0.0), 1.6 + destroyed * 0.5, 0.18 + 0.08 * destroyed, 40, time * 0.06,
            1.0F, 0.25F, 0.12F, 0.35F * intensity * flicker);
        if (destroyed >= 4) {
            sphere(vertices, matrix, origin.add(0.0, 0.4, 0.0), 1.2 + 0.15 * Mth.sin(time * 0.7F), 8, 14, 1.0F, 0.3F, 0.15F, 0.28F * flicker);
            for (int i = 0; i < 3; i++) {
                double tilt = time * 0.05 + i * 2.1;
                Vec3 axis = new Vec3(Math.sin(tilt) * 0.5, 1.0, Math.cos(tilt) * 0.5).normalize();
                axisRing(vertices, matrix, origin.add(0.0, 0.4, 0.0), axis, 2.4 + i * 0.5, 0.1, 32, time * 0.1,
                    1.0F, 0.45F, 0.2F, 0.4F * flicker);
            }
        }
    }

    private static void renderGravityWell(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, Vec3 point, Vec3 ground, float time) {
        if (time < FlyingSaucerBoss.GRAVITY_DROP_TICKS) {
            float progress = time / FlyingSaucerBoss.GRAVITY_DROP_TICKS;
            Vec3 orb = origin.lerp(point, progress * progress);
            tube(vertices, matrix, origin, orb, 0.08, 0.35, 10, 0.6F, 0.3F, 1.0F, 0.1F, 0.6F);
            sphere(vertices, matrix, orb, 0.75, 8, 14, 0.6F, 0.3F, 1.0F, 0.5F);
            sphere(vertices, matrix, orb, 0.35, 6, 10, 1.0F, 0.9F, 1.0F, 0.95F);
            float warn = Mth.clamp(time / 6.0F, 0.0F, 1.0F);
            dashedRing(vertices, matrix, ground.add(0.0, 0.06, 0.0), FlyingSaucerBoss.GRAVITY_PULL_RADIUS, 0.14, 24, time * 0.04,
                0.65F, 0.35F, 1.0F, 0.45F * warn);
            flatRing(vertices, matrix, ground.add(0.0, 0.07, 0.0), FlyingSaucerBoss.GRAVITY_COLLAPSE_RADIUS * progress, 0.18, 40, 0.0,
                1.0F, 0.35F, 0.85F, 0.6F * warn);
            return;
        }
        if (time < FlyingSaucerBoss.GRAVITY_COLLAPSE_TICK) {
            float pull = time - FlyingSaucerBoss.GRAVITY_DROP_TICKS;
            float charge = Mth.clamp(pull / FlyingSaucerBoss.GRAVITY_PULL_TICKS, 0.0F, 1.0F);
            float appear = Mth.clamp(pull / 6.0F, 0.0F, 1.0F);
            float pulse = 0.9F + 0.1F * Mth.sin(time * (0.6F + charge * 1.4F));
            double core = (0.9 + 0.6 * charge) * pulse * appear;
            sphere(vertices, matrix, point, core * 1.8, 10, 18, 0.3F, 0.1F, 0.6F, 0.22F * appear);
            sphere(vertices, matrix, point, core, 10, 18, 0.55F, 0.2F, 0.95F, 0.5F * appear);
            sphere(vertices, matrix, point, core * 0.42, 8, 12, 1.0F, 0.92F, 1.0F, 0.95F * appear);
            axisRing(vertices, matrix, point, UP, core * 1.2, 0.1, 40, time * 0.12, 1.0F, 0.85F, 1.0F, 0.9F * appear);
            for (int i = 0; i < 3; i++) {
                double tilt = time * (0.018 + i * 0.006) + i * 2.1;
                Vec3 axis = new Vec3(Math.sin(tilt) * 0.38, 1.0, Math.cos(tilt) * 0.38).normalize();
                axisRing(vertices, matrix, point, axis, core * (2.1 + i * 0.95), 0.34 - i * 0.08, 56, time * (0.14 - i * 0.035),
                    0.62F, 0.32F, 1.0F, (0.55F - i * 0.13F) * appear);
            }
            for (int lane = 0; lane < 20; lane++) {
                double azimuth = lane * Math.PI * 2.0 / 20.0 + lane * 0.41;
                double elevation = Math.sin(lane * 1.7) * 0.55;
                Vec3 direction = new Vec3(Math.cos(azimuth) * Math.cos(elevation), Math.sin(elevation), Math.sin(azimuth) * Math.cos(elevation));
                float phase = (time * (0.03F + charge * 0.03F) + lane * 0.137F) % 1.0F;
                double distance = FlyingSaucerBoss.GRAVITY_PULL_RADIUS * (1.0 - phase);
                Vec3 from = point.add(direction.scale(distance));
                Vec3 to = point.add(direction.scale(Math.max(core, distance - 2.2)));
                ribbon(vertices, matrix, from, to, 0.07, 0.72F, 0.45F, 1.0F, 0.6F * phase * appear);
            }
            float blink = 0.6F + 0.4F * Mth.sin(time * (0.4F + charge * 1.8F));
            dashedRing(vertices, matrix, ground.add(0.0, 0.06, 0.0), FlyingSaucerBoss.GRAVITY_PULL_RADIUS * (1.0 - 0.04 * Mth.sin(time * 0.3F)),
                0.14, 24, -time * 0.04, 0.65F, 0.35F, 1.0F, 0.45F * appear);
            flatRing(vertices, matrix, ground.add(0.0, 0.07, 0.0), FlyingSaucerBoss.GRAVITY_COLLAPSE_RADIUS, 0.22, 48, 0.0,
                1.0F, 0.3F, 0.8F, (0.35F + 0.5F * charge) * appear * blink);
            disc(vertices, matrix, ground.add(0.0, 0.05, 0.0), FlyingSaucerBoss.GRAVITY_COLLAPSE_RADIUS * charge, 40,
                0.6F, 0.2F, 0.9F, 0.2F * appear);
            wall(vertices, matrix, ground.add(0.0, 0.05, 0.0), FlyingSaucerBoss.GRAVITY_COLLAPSE_RADIUS, 0.5 + 1.0 * charge, 48,
                1.0F, 0.3F, 0.8F, 0.25F * charge * blink);
            cylinder(vertices, matrix, point, DOWN, Math.max(0.0, point.y - ground.y), 0.08 + 0.12 * charge, 0.7F, 0.4F, 1.0F, 0.35F * appear);
            return;
        }
        float age = time - FlyingSaucerBoss.GRAVITY_COLLAPSE_TICK;
        float progress = Mth.clamp(age / FlyingSaucerBoss.GRAVITY_AFTERMATH_TICKS, 0.0F, 1.0F);
        float fade = (1.0F - progress) * (1.0F - progress);
        double radius = FlyingSaucerBoss.GRAVITY_COLLAPSE_RADIUS;
        sphere(vertices, matrix, point, 1.6 * (1.0F - progress) + 0.2, 8, 14, 1.0F, 1.0F, 1.0F, 0.95F * fade);
        sphere(vertices, matrix, point, radius * (0.25 + 0.9 * Math.sqrt(progress)), 10, 20, 0.75F, 0.45F, 1.0F, 0.45F * fade);
        axisRing(vertices, matrix, point, UP, radius * (0.4 + 1.6 * progress), 0.6 * (1.0F - progress) + 0.1, 64, 0.0,
            0.85F, 0.6F, 1.0F, 0.9F * fade);
        Vec3 floor = ground.add(0.0, 0.06, 0.0);
        flatRing(vertices, matrix, floor, radius * (0.5 + 1.5 * progress), 0.5 * (1.0F - progress) + 0.12, 64, 0.0, 0.8F, 0.4F, 1.0F, 0.85F * fade);
        wall(vertices, matrix, floor, radius * (0.5 + 1.5 * progress), 2.2 * (1.0F - progress) + 0.2, 64, 0.7F, 0.35F, 1.0F, 0.5F * fade);
        disc(vertices, matrix, floor, radius * (1.0 + progress * 0.4), 40, 1.0F, 0.85F, 1.0F, 0.4F * fade);
    }

    private static void renderHaloSweep(VertexConsumer vertices, Matrix4f matrix, FlyingSaucerBoss saucer, Vec3 origin,
                                        Vec3 center, Vec3 position, float time) {
        float red = 1.0F;
        float green = 0.28F;
        float blue = 0.62F;
        Vec3 rimCenter = origin.add(0.0, 0.9, 0.0);
        Vec3 relativeCenter = center.subtract(position);
        float charge = Mth.clamp(time / FlyingSaucerBoss.HALO_CHARGE_TICKS, 0.0F, 1.0F);
        float fire = time - FlyingSaucerBoss.HALO_CHARGE_TICKS;
        float rimPulse = 0.8F + 0.2F * Mth.sin(time * 0.9F);
        flatRing(vertices, matrix, rimCenter, FlyingSaucerBoss.HALO_RIM_RADIUS, 0.35, 64, time * 0.05, red, green, blue,
            0.75F * Math.min(1.0F, charge * 1.4F) * rimPulse);
        flatRing(vertices, matrix, rimCenter.add(0.0, 0.02, 0.0), FlyingSaucerBoss.HALO_RIM_RADIUS, 0.1, 64, 0.0, 1.0F, 0.9F, 0.95F,
            0.8F * charge);
        if (fire < 0.0F) {
            for (int beam = 0; beam < FlyingSaucerBoss.HALO_BEAMS; beam++) {
                double angle = FlyingSaucerBoss.haloAngle(0.0F, beam);
                Vec3 rim = rimCenter.add(Math.cos(angle) * FlyingSaucerBoss.HALO_RIM_RADIUS, 0.0, Math.sin(angle) * FlyingSaucerBoss.HALO_RIM_RADIUS);
                sphere(vertices, matrix, rim, 0.2 + 0.3 * charge, 6, 10, 1.0F, 0.85F, 0.95F, 0.85F * charge);
                Vec3 previous = null;
                for (int step = 0; step <= 20; step++) {
                    float sample = step * FlyingSaucerBoss.HALO_FIRE_TICKS / 20.0F;
                    Vec3 groundPoint = FlyingSaucerBoss.haloGroundPoint(relativeCenter, sample, beam).add(0.0, 0.06, 0.0);
                    if (previous != null && step <= 20 * charge) {
                        ribbon(vertices, matrix, previous, groundPoint, 0.12, red, green, blue, 0.35F * charge);
                    }
                    previous = groundPoint;
                }
                Vec3 firstGround = FlyingSaucerBoss.haloGroundPoint(relativeCenter, 0.0F, beam);
                ribbon(vertices, matrix, rim, firstGround, 0.05, red, green, blue, 0.3F * charge * rimPulse);
            }
            dashedRing(vertices, matrix, relativeCenter.add(0.0, 0.07, 0.0), FlyingSaucerBoss.HALO_END_RADIUS, 0.16, 36, time * 0.02,
                red, green, blue, 0.4F * charge);
            return;
        }
        if (fire >= FlyingSaucerBoss.HALO_FIRE_TICKS) {
            float fade = Mth.clamp(1.0F - (fire - FlyingSaucerBoss.HALO_FIRE_TICKS) / 8.0F, 0.0F, 1.0F);
            flatRing(vertices, matrix, relativeCenter.add(0.0, 0.06, 0.0), FlyingSaucerBoss.HALO_END_RADIUS, 0.6, 72, 0.0, red, green, blue, 0.5F * fade);
            return;
        }
        float power = Math.min(Mth.clamp(fire / 3.0F, 0.0F, 1.0F), Mth.clamp((FlyingSaucerBoss.HALO_FIRE_TICKS - fire) / 6.0F, 0.0F, 1.0F));
        float pulse = 1.0F + 0.1F * Mth.sin(time * 1.4F);
        for (int beam = 0; beam < FlyingSaucerBoss.HALO_BEAMS; beam++) {
            double angle = FlyingSaucerBoss.haloAngle(fire, beam);
            Vec3 rim = rimCenter.add(Math.cos(angle) * FlyingSaucerBoss.HALO_RIM_RADIUS, 0.0, Math.sin(angle) * FlyingSaucerBoss.HALO_RIM_RADIUS);
            Vec3 ground = saucer.snapToGround(FlyingSaucerBoss.haloGroundPoint(center, fire, beam), 6.0, 10.0).subtract(position).add(0.0, 0.05, 0.0);
            tube(vertices, matrix, rim, ground, 0.5 * power * pulse, 0.75 * power * pulse, 10, red, green, blue, 0.3F * power, 0.45F * power);
            tube(vertices, matrix, rim, ground, 0.16 * power, 0.24 * power, 8, 1.0F, 0.88F, 0.95F, 0.95F * power, 0.95F * power);
            sphere(vertices, matrix, rim, 0.45 * power * pulse, 6, 10, 1.0F, 0.7F, 0.85F, 0.8F * power);
            disc(vertices, matrix, ground, 1.3 * pulse * power, 24, red, green, blue, 0.55F * power);
            flatRing(vertices, matrix, ground.add(0.0, 0.01, 0.0), 1.8 * pulse * power, 0.35, 32, time * 0.08, 1.0F, 0.75F, 0.85F, 0.75F * power);
            wall(vertices, matrix, ground, 1.1 * power, 1.4 * power * pulse, 24, red, green, blue, 0.45F * power);
            for (int trail = 1; trail <= 12; trail++) {
                float sampleA = fire - trail * 1.5F;
                if (sampleA < 0.0F) {
                    break;
                }
                float fade = 1.0F - trail / 12.0F;
                Vec3 a = FlyingSaucerBoss.haloGroundPoint(relativeCenter, sampleA, beam).add(0.0, 0.05, 0.0);
                Vec3 b = FlyingSaucerBoss.haloGroundPoint(relativeCenter, sampleA + 1.5F, beam).add(0.0, 0.05, 0.0);
                a = new Vec3(a.x, ground.y, a.z);
                b = new Vec3(b.x, ground.y, b.z);
                ribbon(vertices, matrix, a, b, 1.1 * fade + 0.2, 1.0F, 0.42F, 0.18F, 0.5F * fade * power);
            }
        }
        flatRing(vertices, matrix, relativeCenter.add(0.0, 0.07, 0.0), FlyingSaucerBoss.haloRadius(fire), 0.08, 72, 0.0, red, green, blue, 0.35F * power);
    }

    private static void renderPlasmaRain(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, float time) {
        float charge = Mth.clamp(time / FlyingSaucerBoss.PLASMA_CHARGE_TICKS, 0.0F, 1.0F);
        int totalShotTicks = FlyingSaucerBoss.PLASMA_SHOTS * FlyingSaucerBoss.PLASMA_INTERVAL;
        float shotTime = time - FlyingSaucerBoss.PLASMA_CHARGE_TICKS;
        float end = Mth.clamp((totalShotTicks + 8 - shotTime) / 8.0F, 0.0F, 1.0F);
        float power = Math.min(charge, end);
        float pulse = 0.85F + 0.15F * Mth.sin(time * 1.1F);
        Vec3 core = origin.add(0.0, -0.4, 0.0);
        sphere(vertices, matrix, core, (0.3 + 0.9 * power) * pulse, 8, 14, 0.3F, 0.9F, 1.0F, 0.4F * power);
        sphere(vertices, matrix, core, (0.15 + 0.4 * power) * pulse, 6, 10, 0.9F, 1.0F, 1.0F, 0.9F * power);
        for (int i = 0; i < 3; i++) {
            float phase = (time * 0.06F + i / 3.0F) % 1.0F;
            flatRing(vertices, matrix, core.add(0.0, -0.1 * i, 0.0), 0.4 + 3.0 * (1.0F - phase), 0.1 + 0.1 * (1.0F - phase), 32,
                time * 0.04, 0.3F, 0.9F, 1.0F, 0.55F * phase * power);
        }
        if (shotTime < 0.0F || shotTime >= totalShotTicks) {
            return;
        }
        float sinceShot = shotTime % FlyingSaucerBoss.PLASMA_INTERVAL;
        if (sinceShot < 3.0F) {
            float flash = 1.0F - sinceShot / 3.0F;
            cylinder(vertices, matrix, core, UP, 34.0 * Math.min(1.0F, sinceShot / 1.2F + 0.2F), 0.45 * flash, 0.4F, 0.95F, 1.0F, 0.6F * flash);
            cylinder(vertices, matrix, core, UP, 34.0 * Math.min(1.0F, sinceShot / 1.2F + 0.2F), 0.15 * flash, 0.95F, 1.0F, 1.0F, 0.95F * flash);
            flatRing(vertices, matrix, core, 1.2 + 2.0 * (1.0F - flash), 0.3 * flash + 0.05, 32, 0.0, 0.6F, 1.0F, 1.0F, 0.8F * flash);
        }
    }
}
