package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.FlyingSaucerBoss;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class FlyingSaucerRenderer extends GeoEntityRenderer<FlyingSaucerBoss> {
    // 按透明度叠加发光，保证淡入淡出生效
    static final RenderType GLOW = RenderType.create("mechafauna_saucer_laser",
        DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 8192, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
            .createCompositeState(false));
    private static final Vec3 SIDE = new Vec3(1.0, 0.0, 0.0);
    private static final Vec3 FORWARD = new Vec3(0.0, 0.0, 1.0);
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);

    public FlyingSaucerRenderer(EntityRendererProvider.Context context) {
        super(context, new MechModel<>("flying_saucer_boss"));
    }

    @Override
    protected float getDeathMaxRotation(FlyingSaucerBoss animatable) {
        return 0.0F;
    }

    @Override
    public void render(FlyingSaucerBoss entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        // 死亡爆炸特效
        if (entity.isDeadOrDying()) {
            this.renderDeathExplosion(buffers.getBuffer(GLOW), poseStack.last().pose(), entity, partialTick);
        }
        // 召唤增援的传输光束
        if (entity.getSummonTick() > 0) {
            Vec3 summonPosition = entity.getPosition(partialTick);
            Vec3 summonOrigin = entity.getLaserOrigin(summonPosition).subtract(summonPosition);
            Vec3 summonGround = entity.getSummonPoint().subtract(summonPosition);
            if (summonGround.y < summonOrigin.y) {
                this.renderSummon(buffers.getBuffer(GLOW), poseStack.last().pose(), summonOrigin, summonGround,
                    entity.getSummonTick() + partialTick, entity.getSummonKind());
            }
        }
        int laserTick = entity.getLaserTick();
        if (laserTick <= 0) {
            return;
        }
        // 计算相对渲染原点的发射点与落点
        Vec3 position = entity.getPosition(partialTick);
        Vec3 origin = entity.getLaserOrigin(position).subtract(position);
        Vec3 impact = entity.getLaserImpact().subtract(position);
        renderGroundLaser(buffers.getBuffer(GLOW), poseStack.last().pose(), origin, impact, laserTick + partialTick);
    }

    static void renderGroundLaser(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, Vec3 impact, float time) {
        if (impact.y >= origin.y) {
            return;
        }
        float fireTime = time - FlyingSaucerBoss.LASER_CHARGE_TICKS;
        if (fireTime < 0.0F) {
            renderCharge(vertices, matrix, origin, impact, time, time / FlyingSaucerBoss.LASER_CHARGE_TICKS);
            return;
        }
        if (fireTime < FlyingSaucerBoss.LASER_FIRE_TICKS) {
            renderBeam(vertices, matrix, origin, impact, time, fireTime);
        }
        renderWaves(vertices, matrix, impact, fireTime);
    }

    private static void renderCharge(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, Vec3 impact, float time, float charge) {
        float pulse = 0.85F + 0.15F * Mth.sin(time * 0.9F);
        // 核心下方三圈收缩的聚能环
        for (int i = 0; i < 3; i++) {
            float phase = (time * 0.045F + i / 3.0F) % 1.0F;
            double radius = 0.25 + 3.2 * (1.0 - phase);
            float alpha = charge * phase * 0.7F * pulse;
            flatRing(vertices, matrix, origin.add(0.0, -0.15 - i * 0.12, 0.0), radius, 0.12 + 0.1 * (1.0 - phase),
                32, time * 0.03 * (i + 1), 0.35F, 0.9F, 1.0F, alpha);
        }
        // 螺旋汇聚到发射点的能量流
        for (int lane = 0; lane < 10; lane++) {
            double baseAngle = lane * Math.PI * 2.0 / 10.0 + time * 0.05;
            Vec3 previous = null;
            for (int segment = 0; segment <= 8; segment++) {
                double progress = segment / 8.0;
                double angle = baseAngle + progress * 2.2;
                double radius = 3.6 * (1.0 - progress) + 0.05;
                Vec3 point = origin.add(Math.cos(angle) * radius, -1.2 * (1.0 - progress) * Math.sin(progress * Math.PI + 0.4), Math.sin(angle) * radius);
                if (previous != null) {
                    float flow = (float)Math.max(0.0, 1.0 - Math.abs(progress - (time * 0.07 + lane * 0.13) % 1.0) * 5.0);
                    float alpha = charge * (0.15F + 0.6F * flow) * pulse;
                    ribbon(vertices, matrix, previous, point, 0.07 * (1.0 - progress) + 0.015, 0.3F, 0.85F, 1.0F, alpha);
                }
                previous = point;
            }
        }
        // 发射点逐渐变亮的能量球
        double coreRadius = 0.2 + 0.55 * charge;
        cylinder(vertices, matrix, origin.add(0.0, coreRadius * 0.5, 0.0), DOWN, coreRadius, coreRadius, 0.55F, 0.95F, 1.0F, 0.35F * charge * pulse);
        cylinder(vertices, matrix, origin.add(0.0, coreRadius * 0.3, 0.0), DOWN, coreRadius * 0.6, coreRadius * 0.55, 0.9F, 1.0F, 1.0F, 0.75F * charge);
        // 细瞄准线与地面预警圈
        double length = origin.y - impact.y;
        cylinder(vertices, matrix, origin, DOWN, length, 0.04 + 0.05 * charge, 0.3F, 0.85F, 1.0F, 0.25F * charge * pulse);
        double warn = 2.4 * (1.15 - 0.15 * Mth.sin(time * 0.6F));
        flatRing(vertices, matrix, impact.add(0.0, 0.06, 0.0), warn, 0.18, 40, -time * 0.04, 0.2F, 0.75F, 1.0F, 0.6F * charge);
        flatRing(vertices, matrix, impact.add(0.0, 0.06, 0.0), warn * charge, 0.1, 40, time * 0.05, 0.5F, 0.95F, 1.0F, 0.45F * charge);
    }

    private static void renderBeam(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, Vec3 impact, float time, float fireTime) {
        // 起手迅速展开，结尾收束
        float open = Mth.clamp(fireTime / 3.0F, 0.0F, 1.0F);
        float close = Mth.clamp((FlyingSaucerBoss.LASER_FIRE_TICKS - fireTime) / FlyingSaucerBoss.LASER_FADE_TICKS, 0.0F, 1.0F);
        float power = Math.min(open, close);
        float pulse = 1.0F + 0.08F * Mth.sin(time * 1.3F);
        double length = origin.y - impact.y;
        // 三层光柱：外晕、青色中层、白色光芯
        cylinder(vertices, matrix, origin, DOWN, length, 1.35 * pulse * power, 0.15F, 0.6F, 0.9F, 0.22F * power);
        cylinder(vertices, matrix, origin, DOWN, length, 0.8 * pulse * power, 0.3F, 0.88F, 1.0F, 0.45F * power);
        cylinder(vertices, matrix, origin, DOWN, length, 0.36 * power, 0.9F, 1.0F, 1.0F, 0.95F * power);
        // 沿光柱向下流动的能量环
        for (int i = 0; i < 6; i++) {
            double distance = (fireTime * 1.6 + i * length / 6.0) % length;
            flatRing(vertices, matrix, origin.add(0.0, -distance, 0.0), 1.1 * power, 0.14, 24, time * 0.05, 0.5F, 0.95F, 1.0F, 0.5F * power);
        }
        // 发射口光环
        flatRing(vertices, matrix, origin, 1.6 * pulse * power, 0.35, 32, time * 0.04, 0.4F, 0.9F, 1.0F, 0.8F * power);
        // 落点的地面光盘与溅射光环
        Vec3 ground = impact.add(0.0, 0.05, 0.0);
        disc(vertices, matrix, ground, 2.6 * pulse * power, 32, 0.6F, 0.95F, 1.0F, 0.55F * power);
        flatRing(vertices, matrix, ground, 3.2 * pulse * power, 0.4, 40, -time * 0.06, 0.3F, 0.85F, 1.0F, 0.7F * power);
        cylinder(vertices, matrix, ground.add(0.0, 1.2 * power, 0.0), DOWN, 1.2 * power, 1.9 * pulse * power, 0.4F, 0.9F, 1.0F, 0.3F * power);
    }

    private static void renderWaves(VertexConsumer vertices, Matrix4f matrix, Vec3 impact, float fireTime) {
        Vec3 ground = impact.add(0.0, 0.08, 0.0);
        for (int wave = 0; wave < FlyingSaucerBoss.WAVE_COUNT; wave++) {
            float age = fireTime - wave * FlyingSaucerBoss.WAVE_INTERVAL;
            if (age < 0.0F || age >= FlyingSaucerBoss.WAVE_LIFETIME) {
                continue;
            }
            float progress = age / FlyingSaucerBoss.WAVE_LIFETIME;
            float fade = (1.0F - progress) * (1.0F - progress);
            double radius = FlyingSaucerBoss.waveRadius(age);
            double thickness = 0.35 + 1.1 * (1.0 - progress);
            // 地面扩散的波纹主环与内侧余辉
            flatRing(vertices, matrix, ground, radius, thickness, 64, 0.0, 0.3F, 0.88F, 1.0F, 0.85F * fade);
            flatRing(vertices, matrix, ground, radius * 0.86, thickness * 1.8, 64, 0.0, 0.12F, 0.55F, 0.85F, 0.3F * fade);
            flatRing(vertices, matrix, ground.add(0.0, 0.01, 0.0), radius, 0.1, 64, 0.0, 0.9F, 1.0F, 1.0F, 0.9F * fade);
            // 波纹前沿竖起的冲击墙，在起伏地面上也能看见
            double height = 1.4 * (1.0 - progress) + 0.15;
            wall(vertices, matrix, ground, radius, height, 64, 0.35F, 0.9F, 1.0F, 0.6F * fade);
        }
    }

    private void renderSummon(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, Vec3 ground, float time, int kind) {
        float charge = Mth.clamp(time / FlyingSaucerBoss.SUMMON_CHARGE_TICKS, 0.0F, 1.0F);
        float beamTime = time - FlyingSaucerBoss.SUMMON_CHARGE_TICKS;
        float retract = Mth.clamp((time - (FlyingSaucerBoss.SUMMON_TOTAL_TICKS - FlyingSaucerBoss.SUMMON_FADE_TICKS))
            / FlyingSaucerBoss.SUMMON_FADE_TICKS, 0.0F, 1.0F);
        double baseRadius = 2.6;
        if (kind == FlyingSaucerBoss.SUMMON_MECH) {
            baseRadius = 2.0;
        }
        double height = FlyingSaucerBoss.summonHeight(kind);
        double length = origin.y - ground.y;
        Vec3 floor = ground.add(0.0, 0.06, 0.0);
        // 起手：发射口光环展开，引导线向下延伸
        if (beamTime < 0.0F) {
            float ease = 1.0F - (1.0F - charge) * (1.0F - charge);
            flatRing(vertices, matrix, origin, 0.3 + 1.5 * ease, 0.22, 32, time * 0.08, 0.4F, 0.9F, 1.0F, 0.8F * charge);
            cylinder(vertices, matrix, origin, DOWN, length * ease, 0.05, 0.6F, 0.95F, 1.0F, 0.55F * charge);
            dashedRing(vertices, matrix, floor, baseRadius * 1.35, 0.16, 24, -time * 0.03, 0.3F, 0.85F, 1.0F, 0.6F * charge * charge);
            return;
        }
        float open = Mth.clamp(beamTime / 6.0F, 0.0F, 1.0F);
        float power = open * (1.0F - retract);
        // 收尾时光柱底端向上回收
        double bottomY = Mth.lerp(retract * retract, ground.y, origin.y);
        double beamLength = origin.y - bottomY;
        Vec3 bottom = new Vec3(ground.x, bottomY, ground.z);
        double topRadius = 0.7 * power;
        double bottomRadius = baseRadius * power;
        // 上窄下宽的三层光锥
        cone(vertices, matrix, origin, bottom, topRadius * 1.2, bottomRadius * 1.15, 24, 0.2F, 0.7F, 1.0F, 0.2F * power, 0.12F * power);
        cone(vertices, matrix, origin, bottom, topRadius * 0.7, bottomRadius * 0.6, 24, 0.45F, 0.92F, 1.0F, 0.28F * power, 0.2F * power);
        cylinder(vertices, matrix, origin, DOWN, beamLength, 0.12 * power, 0.9F, 1.0F, 1.0F, 0.55F * power);
        // 沿光锥向下流动的传输环
        for (int i = 0; i < 7; i++) {
            double progress = (beamTime * 0.025 + i / 7.0) % 1.0;
            double radius = Mth.lerp(progress, topRadius, bottomRadius);
            float alpha = 0.5F * power * Mth.sin((float)(progress * Math.PI));
            flatRing(vertices, matrix, origin.add(0.0, -progress * beamLength, 0.0), radius, 0.1, 32, 0.0, 0.55F, 0.95F, 1.0F, alpha);
        }
        // 贴着光锥表面下落的光纹
        for (int lane = 0; lane < 14; lane++) {
            double angle = lane * Math.PI * 2.0 / 14.0 + time * 0.012;
            double start = (beamTime * 0.04 + lane * 0.37) % 1.0;
            double end = Math.min(1.0, start + 0.14);
            Vec3 from = conePoint(origin, beamLength, topRadius * 0.95, bottomRadius * 0.95, angle, start);
            Vec3 to = conePoint(origin, beamLength, topRadius * 0.95, bottomRadius * 0.95, angle, end);
            float alpha = 0.6F * power * Mth.sin((float)(start * Math.PI));
            ribbon(vertices, matrix, from, to, 0.035, 0.75F, 1.0F, 1.0F, alpha);
        }
        // 发射口光环
        flatRing(vertices, matrix, origin, 1.8 * power, 0.3, 32, time * 0.05, 0.4F, 0.9F, 1.0F, 0.75F * power);
        // 地面光斑与反向旋转的传送阵
        disc(vertices, matrix, floor, bottomRadius * 1.25, 32, 0.5F, 0.95F, 1.0F, 0.45F * power);
        dashedRing(vertices, matrix, floor, baseRadius * 1.35, 0.18, 24, -time * 0.03, 0.3F, 0.85F, 1.0F, 0.7F * power);
        dashedRing(vertices, matrix, floor.add(0.0, 0.01, 0.0), baseRadius * 1.65, 0.1, 36, time * 0.02, 0.5F, 0.95F, 1.0F, 0.5F * power);
        flatRing(vertices, matrix, floor, baseRadius * 1.9, 0.08, 48, 0.0, 0.3F, 0.85F, 1.0F, 0.4F * power);
        this.renderMaterialize(vertices, matrix, floor, time, baseRadius, height, power);
    }

    private void renderMaterialize(VertexConsumer vertices, Matrix4f matrix, Vec3 floor, float time,
                                   double baseRadius, double height, float power) {
        float spawnAge = time - FlyingSaucerBoss.SUMMON_SPAWN_TICK;
        float buildStart = FlyingSaucerBoss.SUMMON_CHARGE_TICKS + 6.0F;
        float build = Mth.clamp((time - buildStart) / (FlyingSaucerBoss.SUMMON_SPAWN_TICK - buildStart), 0.0F, 1.0F);
        // 生成前：光壳自下而上填充，扫描环上下往返
        if (spawnAge < 0.0F) {
            if (build <= 0.0F) {
                return;
            }
            Vec3 top = floor.add(0.0, height * build, 0.0);
            cone(vertices, matrix, top, floor, baseRadius * 0.8, baseRadius * 0.8, 24, 0.45F, 0.95F, 1.0F, 0.1F * power, 0.35F * power);
            flatRing(vertices, matrix, top, baseRadius * 0.85, 0.12, 32, 0.0, 0.85F, 1.0F, 1.0F, 0.8F * power);
            double scan = height * build * (0.5 + 0.5 * Mth.sin(time * 0.35F));
            flatRing(vertices, matrix, floor.add(0.0, scan, 0.0), baseRadius * 0.9, 0.08, 32, 0.0, 0.6F, 1.0F, 1.0F, 0.6F * power);
            return;
        }
        // 生成瞬间：光壳闪亮后淡出，地面冲击环向外扩散
        float flash = Mth.clamp(1.0F - spawnAge / 16.0F, 0.0F, 1.0F);
        if (flash > 0.0F) {
            Vec3 top = floor.add(0.0, height * (1.0 + 0.3 * (1.0F - flash)), 0.0);
            cone(vertices, matrix, top, floor, baseRadius * 0.8, baseRadius * 0.85, 24, 0.7F, 1.0F, 1.0F, 0.05F * flash, 0.6F * flash);
            double pulse = baseRadius * (1.0 + 2.2 * (1.0F - flash));
            flatRing(vertices, matrix, floor.add(0.0, 0.02, 0.0), pulse, 0.5 * flash + 0.1, 48, 0.0, 0.8F, 1.0F, 1.0F, 0.85F * flash);
            disc(vertices, matrix, floor.add(0.0, 0.03, 0.0), baseRadius * 1.4, 32, 0.9F, 1.0F, 1.0F, 0.7F * flash * flash);
        }
        // 生成后扫描环向上离开
        float rise = Mth.clamp(spawnAge / 12.0F, 0.0F, 1.0F);
        if (rise < 1.0F) {
            flatRing(vertices, matrix, floor.add(0.0, height * (1.0 + rise), 0.0), baseRadius * 0.85, 0.1, 32, 0.0,
                0.7F, 1.0F, 1.0F, 0.7F * (1.0F - rise) * power);
        }
    }

    private static Vec3 conePoint(Vec3 top, double length, double topRadius, double bottomRadius, double angle, double progress) {
        double radius = Mth.lerp(progress, topRadius, bottomRadius);
        return top.add(Math.cos(angle) * radius, -progress * length, Math.sin(angle) * radius);
    }

    private static void cone(VertexConsumer vertices, Matrix4f matrix, Vec3 top, Vec3 bottom, double topRadius, double bottomRadius,
                             int segments, float red, float green, float blue, float topAlpha, float bottomAlpha) {
        if (topAlpha <= 0.003F && bottomAlpha <= 0.003F) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            // 竖直截面，上下两端可分别设置透明度
            vertex(vertices, matrix, top.add(Math.cos(a) * topRadius, 0.0, Math.sin(a) * topRadius), red, green, blue, topAlpha);
            vertex(vertices, matrix, top.add(Math.cos(b) * topRadius, 0.0, Math.sin(b) * topRadius), red, green, blue, topAlpha);
            vertex(vertices, matrix, bottom.add(Math.cos(b) * bottomRadius, 0.0, Math.sin(b) * bottomRadius), red, green, blue, bottomAlpha);
            vertex(vertices, matrix, bottom.add(Math.cos(a) * bottomRadius, 0.0, Math.sin(a) * bottomRadius), red, green, blue, bottomAlpha);
        }
    }

    private static void dashedRing(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, double thickness, int dashes,
                                   double phase, float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        double outer = radius + thickness * 0.5;
        double inner = radius - thickness * 0.5;
        double step = Math.PI * 2.0 / dashes;
        for (int i = 0; i < dashes; i++) {
            // 每段只画前 60%，形成断续的阵纹
            double a = i * step + phase;
            double b = a + step * 0.6;
            Vec3 va = SIDE.scale(Math.cos(a)).add(FORWARD.scale(Math.sin(a)));
            Vec3 vb = SIDE.scale(Math.cos(b)).add(FORWARD.scale(Math.sin(b)));
            vertex(vertices, matrix, center.add(va.scale(inner)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(va.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(inner)), red, green, blue, alpha);
        }
    }

    private static void cylinder(VertexConsumer vertices, Matrix4f matrix, Vec3 start, Vec3 direction, double length, double radius,
                                 float red, float green, float blue, float alpha) {
        Vec3 end = start.add(direction.scale(length));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            double b = (i + 1) * Math.PI / 6.0;
            Vec3 offsetA = new Vec3(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
            Vec3 offsetB = new Vec3(Math.cos(b) * radius, 0.0, Math.sin(b) * radius);
            vertex(vertices, matrix, start.add(offsetA), red, green, blue, alpha);
            vertex(vertices, matrix, start.add(offsetB), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(offsetB), red, green, blue, alpha);
            vertex(vertices, matrix, end.add(offsetA), red, green, blue, alpha);
        }
    }

    private static void flatRing(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, double thickness, int segments,
                                 double phase, float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F || radius <= 0.0) {
            return;
        }
        double outer = radius + thickness * 0.5;
        double inner = Math.max(0.0, radius - thickness * 0.5);
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments + phase;
            double b = (i + 1) * Math.PI * 2.0 / segments + phase;
            Vec3 va = SIDE.scale(Math.cos(a)).add(FORWARD.scale(Math.sin(a)));
            Vec3 vb = SIDE.scale(Math.cos(b)).add(FORWARD.scale(Math.sin(b)));
            // 内缘透明、外缘不透明形成柔和边
            vertex(vertices, matrix, center.add(va.scale(inner)), red, green, blue, alpha * 0.25F);
            vertex(vertices, matrix, center.add(va.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(outer)), red, green, blue, alpha);
            vertex(vertices, matrix, center.add(vb.scale(inner)), red, green, blue, alpha * 0.25F);
        }
    }

    private static void disc(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, int segments,
                             float red, float green, float blue, float alpha) {
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            // 中心亮、边缘透明的地面光斑
            vertex(vertices, matrix, center, red, green, blue, alpha);
            vertex(vertices, matrix, center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius), red, green, blue, 0.0F);
            vertex(vertices, matrix, center.add(Math.cos(b) * radius, 0.0, Math.sin(b) * radius), red, green, blue, 0.0F);
            vertex(vertices, matrix, center, red, green, blue, alpha);
        }
    }

    private static void wall(VertexConsumer vertices, Matrix4f matrix, Vec3 center, double radius, double height, int segments,
                             float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2.0 / segments;
            double b = (i + 1) * Math.PI * 2.0 / segments;
            Vec3 bottomA = center.add(Math.cos(a) * radius, 0.0, Math.sin(a) * radius);
            Vec3 bottomB = center.add(Math.cos(b) * radius, 0.0, Math.sin(b) * radius);
            // 底部亮、顶部透明的竖直光带
            vertex(vertices, matrix, bottomA, red, green, blue, alpha);
            vertex(vertices, matrix, bottomB, red, green, blue, alpha);
            vertex(vertices, matrix, bottomB.add(0.0, height, 0.0), red, green, blue, 0.0F);
            vertex(vertices, matrix, bottomA.add(0.0, height, 0.0), red, green, blue, 0.0F);
        }
    }

    private static void ribbon(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to, double width,
                               float red, float green, float blue, float alpha) {
        Vec3 along = to.subtract(from);
        if (along.lengthSqr() < 1.0E-8) {
            return;
        }
        // 两片互相垂直的面片，斜视角也可见
        Vec3 horizontal = along.cross(new Vec3(0.0, 1.0, 0.0));
        if (horizontal.lengthSqr() < 1.0E-8) {
            horizontal = SIDE;
        }
        Vec3 first = horizontal.normalize().scale(width);
        Vec3 second = along.cross(first).normalize().scale(width);
        for (Vec3 offset : new Vec3[] {first, second}) {
            vertex(vertices, matrix, from.subtract(offset), red, green, blue, alpha);
            vertex(vertices, matrix, from.add(offset), red, green, blue, alpha);
            vertex(vertices, matrix, to.add(offset), red, green, blue, alpha);
            vertex(vertices, matrix, to.subtract(offset), red, green, blue, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               float red, float green, float blue, float alpha) {
        vertices.addVertex(matrix, (float)point.x, (float)point.y, (float)point.z).setColor(red, green, blue, alpha);
    }

    private void renderDeathExplosion(VertexConsumer vertices, Matrix4f matrix, FlyingSaucerBoss entity, float partialTick) {
        float time = entity.deathTime + partialTick;
        float progress = time / 100.0F;
        if (progress >= 1.0F) {
            return;
        }
        // 从核心向外扩散的能量冲击球
        double coreRadius = 1.5 + 8.0 * progress;
        float coreAlpha = (1.0F - progress) * 0.6F;
        cylinder(vertices, matrix, new Vec3(0.0, 3.0, 0.0), DOWN, coreRadius, coreRadius, 0.4F, 0.85F, 1.0F, coreAlpha);
        cylinder(vertices, matrix, new Vec3(0.0, 3.0 + coreRadius * 0.5, 0.0), DOWN, coreRadius, coreRadius * 0.7, 0.8F, 0.95F, 1.0F, coreAlpha * 1.2F);
        // 向上喷出的能量柱
        if (progress < 0.6F) {
            float jetProgress = progress / 0.6F;
            double height = 12.0 * jetProgress;
            double jetRadius = 1.2 * (1.0F - jetProgress * 0.5F);
            cylinder(vertices, matrix, new Vec3(0.0, 3.0, 0.0), new Vec3(0.0, 1.0, 0.0), height, jetRadius, 0.3F, 0.8F, 1.0F, (1.0F - jetProgress) * 0.5F);
        }
        // 沿装甲板边缘的电弧闪光
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2.0 / 8.0 + time * 0.1;
            double arcProgress = (time * 0.08 + i * 0.15) % 1.0;
            if (arcProgress < 0.7) {
                double x = Math.cos(angle) * (3.5 + arcProgress * 2.0);
                double z = Math.sin(angle) * (3.5 + arcProgress * 2.0);
                double y = 2.5 + Math.sin(arcProgress * Math.PI) * 1.5;
                double nextX = Math.cos(angle) * (3.5 + (arcProgress + 0.1) * 2.0);
                double nextZ = Math.sin(angle) * (3.5 + (arcProgress + 0.1) * 2.0);
                double nextY = 2.5 + Math.sin((arcProgress + 0.1) * Math.PI) * 1.5;
                float arcAlpha = (1.0F - (float)arcProgress) * (1.0F - progress) * 0.8F;
                ribbon(vertices, matrix, new Vec3(x, y, z), new Vec3(nextX, nextY, nextZ), 0.15, 0.5F, 0.9F, 1.0F, arcAlpha);
            }
        }
        // 地面冲击环
        if (progress < 0.5F) {
            float shockProgress = progress / 0.5F;
            double shockRadius = 10.0 * shockProgress;
            float shockAlpha = (1.0F - shockProgress) * 0.7F;
            flatRing(vertices, matrix, new Vec3(0.0, 0.1, 0.0), shockRadius, 0.6, 48, -time * 0.05, 0.3F, 0.85F, 1.0F, shockAlpha);
        }
    }
}
