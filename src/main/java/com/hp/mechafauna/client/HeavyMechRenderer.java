package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.HeavyMech;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import com.geckolib.renderer.GeoEntityRenderer;

public final class HeavyMechRenderer extends GeoEntityRenderer<HeavyMech, HeavyMechRenderState> {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final double CORE_HEIGHT = 2.2;

    public HeavyMechRenderer(EntityRendererProvider.Context context) {
        super(context, new MechModel<>("heavy_mech"));
    }

    @Override
    public boolean shouldRender(HeavyMech entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        if (HeavyMech.ROCKET_CHARGE.equals(entity.getActiveAction()) || HeavyMech.OVERLOAD.equals(entity.getActiveAction())) {
            AABB bounds = entity.getBoundingBox().inflate(HeavyMech.OVERLOAD_WAVE_RADIUS + 2.0, 3.0, HeavyMech.OVERLOAD_WAVE_RADIUS + 2.0);
            return frustum.isVisible(bounds);
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public HeavyMechRenderState createRenderState(HeavyMech entity, Void context) {
        return new HeavyMechRenderState();
    }

    @Override
    public void extractRenderState(HeavyMech entity, HeavyMechRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.entity = entity;
        state.dying = entity.isDeadOrDying();
        state.position = entity.getPosition(partialTick);
        state.time = entity.tickCount + partialTick;
        state.action = entity.getActiveAction();
        state.skillTime = entity.getSkillTick() + partialTick;
        state.chargeYaw = entity.getChargeYaw();
        state.chargeOrigin = entity.getChargeOrigin();
        state.overloadTicks = entity.getOverloadTicks();
        state.overloaded = entity.isOverloaded();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<HeavyMechRenderState> pass, BoneSnapshots bones) {
        ((MechModel<HeavyMech>)this.model).applyBoneTransforms(pass, bones);
    }

    @Override
    public void submit(HeavyMechRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.dying) {
            return;
        }
        collector.submitCustomGeometry(poseStack, VfxRenderTypes.GLOW, (entry, vertices) -> {
            this.renderEffects(state, vertices, new Matrix4f(entry.pose()));
        });
    }

    private void renderEffects(HeavyMechRenderState entity, VertexConsumer vertices, Matrix4f matrix) {
        Vec3 position = entity.position;
        float time = entity.time;
        float partialTick = entity.partialTick;
        String action = entity.action;
        float skillTime = entity.skillTime;
        if (HeavyMech.ROCKET_CHARGE.equals(action)) {
            this.renderCharge(vertices, matrix, entity, position, partialTick, skillTime, time);
        } else if (HeavyMech.OVERLOAD.equals(action)) {
            this.renderOverload(vertices, matrix, skillTime, time);
        }
        if (entity.overloaded) {
            this.renderOverloadAura(vertices, matrix, entity, time);
        }
    }

    private void renderCharge(VertexConsumer vertices, Matrix4f matrix, HeavyMechRenderState entity, Vec3 position, float partialTick,
                              float skillTime, float time) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, entity.chargeYaw).multiply(1.0, 0.0, 1.0).normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        if (skillTime < HeavyMech.CHARGE_DASH_START) {
            // 冲锋路线预警：地面警示带由近向远填充，箭头向前流动
            Vec3 origin = entity.chargeOrigin.subtract(position).add(0.0, 0.07, 0.0);
            double length = HeavyMech.CHARGE_LENGTH + 1.5;
            float appear = Mth.clamp(skillTime / 6.0F, 0.0F, 1.0F);
            float locked = skillTime >= HeavyMech.CHARGE_LOCK_TICK ? 1.0F : 0.0F;
            float blink = 0.7F + 0.3F * Mth.sin(time * (0.6F + locked * 1.2F));
            float fill = Mth.clamp(skillTime / HeavyMech.CHARGE_DASH_START, 0.0F, 1.0F);
            this.groundStrip(vertices, matrix, origin, forward, side, length, 1.15, 1.0F, 0.25F, 0.1F, 0.22F * appear);
            this.groundStrip(vertices, matrix, origin.add(0.0, 0.005, 0.0), forward, side, length * fill, 1.15, 1.0F, 0.45F, 0.15F, 0.35F * appear);
            for (int edge = -1; edge <= 1; edge += 2) {
                Vec3 offset = side.scale(edge * 1.15);
                VfxShapes.ribbon(vertices, matrix, origin.add(offset), origin.add(offset).add(forward.scale(length)), 0.04,
                    1.0F, 0.3F + 0.4F * locked, 0.1F, 0.85F * appear * blink);
            }
            for (int i = 0; i < 6; i++) {
                double along = ((time * 0.35 + i * length / 6.0) % length);
                Vec3 tip = origin.add(forward.scale(along + 0.6));
                Vec3 left = origin.add(forward.scale(along)).add(side.scale(0.7));
                Vec3 right = origin.add(forward.scale(along)).subtract(side.scale(0.7));
                float alpha = 0.8F * appear * Mth.sin((float)(along / length * Math.PI));
                VfxShapes.ribbon(vertices, matrix, left, tip, 0.06, 1.0F, 0.6F, 0.2F, alpha);
                VfxShapes.ribbon(vertices, matrix, right, tip, 0.06, 1.0F, 0.6F, 0.2F, alpha);
            }
            // 背部推进器点火预热
            float heat = Mth.clamp((skillTime - 4.0F) / 10.0F, 0.0F, 1.0F);
            this.thrusters(vertices, matrix, forward, side, 0.6 + 0.6 * heat, heat * 0.6F, time);
            return;
        }
        if (skillTime <= HeavyMech.CHARGE_DASH_END + 1) {
            // 冲刺中：长尾焰、身前激波锥和环绕气流环
            this.thrusters(vertices, matrix, forward, side, 3.2, 1.0F, time);
            Vec3 nose = forward.scale(1.4).add(0.0, 1.6, 0.0);
            VfxShapes.tube(vertices, matrix, nose, nose.subtract(forward.scale(2.6)), 0.3, 1.9, 16, 1.0F, 0.85F, 0.6F, 0.45F, 0.0F);
            for (int i = 0; i < 3; i++) {
                double phase = (time * 0.4 + i / 3.0) % 1.0;
                Vec3 center = forward.scale(1.2 - phase * 4.0).add(0.0, 1.6, 0.0);
                VfxShapes.axisRing(vertices, matrix, center, forward, 1.6 + phase * 0.9, 0.12, 24, 0.0,
                    1.0F, 0.8F, 0.5F, 0.55F * (float)(1.0 - phase));
            }
        }
    }

    private void thrusters(VertexConsumer vertices, Matrix4f matrix, Vec3 forward, Vec3 side, double length, float power, float time) {
        if (power <= 0.01F) {
            return;
        }
        float flicker = 0.85F + 0.15F * Mth.sin(time * 2.3F);
        for (int nozzle = -1; nozzle <= 1; nozzle += 2) {
            Vec3 start = forward.scale(-0.9).add(side.scale(nozzle * 0.7)).add(0.0, 2.5, 0.0);
            Vec3 end = start.subtract(forward.scale(length * flicker)).add(0.0, -0.25 * length, 0.0);
            VfxShapes.tube(vertices, matrix, start, end, 0.22, 0.05, 10, 1.0F, 0.95F, 0.8F, 0.9F * power, 0.0F);
            VfxShapes.tube(vertices, matrix, start, end, 0.42, 0.12, 10, 1.0F, 0.45F, 0.1F, 0.55F * power, 0.0F);
            VfxShapes.axisRing(vertices, matrix, start, forward, 0.32, 0.08, 12, 0.0, 1.0F, 0.6F, 0.2F, 0.8F * power);
        }
    }

    private void groundStrip(VertexConsumer vertices, Matrix4f matrix, Vec3 origin, Vec3 forward, Vec3 side, double length, double halfWidth,
                             float red, float green, float blue, float alpha) {
        if (length <= 0.01 || alpha <= 0.003F) {
            return;
        }
        Vec3 end = origin.add(forward.scale(length));
        VfxShapes.vertex(vertices, matrix, origin.add(side.scale(halfWidth)), red, green, blue, alpha);
        VfxShapes.vertex(vertices, matrix, origin.subtract(side.scale(halfWidth)), red, green, blue, alpha);
        VfxShapes.vertex(vertices, matrix, end.subtract(side.scale(halfWidth)), red, green, blue, alpha * 0.4F);
        VfxShapes.vertex(vertices, matrix, end.add(side.scale(halfWidth)), red, green, blue, alpha * 0.4F);
    }

    private void renderOverload(VertexConsumer vertices, Matrix4f matrix, float skillTime, float time) {
        Vec3 core = new Vec3(0.0, CORE_HEIGHT, 0.0);
        if (skillTime < HeavyMech.OVERLOAD_PULSE_TICK) {
            float charge = Mth.clamp(skillTime / HeavyMech.OVERLOAD_PULSE_TICK, 0.0F, 1.0F);
            float pulse = 0.8F + 0.2F * Mth.sin(time * (0.8F + charge * 2.0F));
            // 地面收缩的聚能阵
            for (int i = 0; i < 3; i++) {
                float phase = (time * 0.05F + i / 3.0F) % 1.0F;
                VfxShapes.flatRing(vertices, matrix, new Vec3(0.0, 0.08 + i * 0.01, 0.0), 0.8 + 6.0 * (1.0F - phase), 0.18,
                    48, time * 0.02 * (i + 1), 0.35F, 0.85F, 1.0F, charge * phase * 0.8F);
            }
            VfxShapes.dashedRing(vertices, matrix, new Vec3(0.0, 0.07, 0.0), 3.6, 0.2, 18, -time * 0.05, 1.0F, 0.3F, 0.2F, 0.7F * charge);
            // 胸口能量核心逐渐膨胀
            VfxShapes.sphere(vertices, matrix, core, 0.25 + 0.75 * charge * pulse, 8, 16, 0.4F, 0.9F, 1.0F, 0.3F + 0.4F * charge);
            VfxShapes.sphere(vertices, matrix, core, 0.15 + 0.35 * charge, 6, 12, 1.0F, 1.0F, 1.0F, 0.8F * charge);
            // 环绕核心的倾斜能量环
            for (int i = 0; i < 3; i++) {
                double tilt = i * Math.PI / 3.0 + time * 0.07;
                Vec3 axis = new Vec3(Math.sin(tilt), Math.cos(tilt * 0.7), Math.cos(tilt));
                VfxShapes.axisRing(vertices, matrix, core, axis, 1.3 + 0.4 * i, 0.09, 32, time * 0.1, 0.4F, 0.9F, 1.0F, 0.6F * charge);
            }
            // 由外向核心汇聚的电弧
            for (int arc = 0; arc < 6; arc++) {
                double seed = Math.floor(time * 0.5) * 7.31 + arc * 13.7;
                double angle = arc * Math.PI / 3.0 + Math.sin(seed) * 0.5;
                Vec3 start = new Vec3(Math.cos(angle) * 3.8, 0.4 + (Math.sin(seed * 1.7) * 0.5 + 0.5) * 3.0, Math.sin(angle) * 3.8);
                this.lightning(vertices, matrix, start, core, seed, 0.06, 0.6F, 0.95F, 1.0F, 0.75F * charge * pulse);
            }
            return;
        }
        float age = skillTime - HeavyMech.OVERLOAD_PULSE_TICK;
        if (age > HeavyMech.OVERLOAD_WAVE_TICKS + 4) {
            return;
        }
        float progress = Mth.clamp(age / (HeavyMech.OVERLOAD_WAVE_TICKS + 4), 0.0F, 1.0F);
        float fade = (1.0F - progress) * (1.0F - progress);
        double radius = HeavyMech.overloadWaveRadius(age);
        // EMP 冲击波：地面主环、竖直冲击墙和半透明能量球壳
        Vec3 ground = new Vec3(0.0, 0.1, 0.0);
        VfxShapes.flatRing(vertices, matrix, ground, radius, 0.5 + 1.2 * (1.0F - progress), 64, 0.0, 0.35F, 0.9F, 1.0F, 0.9F * fade);
        VfxShapes.flatRing(vertices, matrix, ground.add(0.0, 0.01, 0.0), radius, 0.12, 64, 0.0, 1.0F, 1.0F, 1.0F, fade);
        VfxShapes.wall(vertices, matrix, ground, radius, 2.2 * (1.0F - progress) + 0.3, 64, 0.4F, 0.9F, 1.0F, 0.6F * fade);
        VfxShapes.sphere(vertices, matrix, core.subtract(0.0, 1.0, 0.0), radius * 0.85, 10, 24, 0.35F, 0.8F, 1.0F, 0.18F * fade);
        VfxShapes.sphere(vertices, matrix, core, 1.6 * (1.0F - progress) + 0.1, 8, 16, 1.0F, 1.0F, 1.0F, 0.8F * fade);
        for (int arc = 0; arc < 10; arc++) {
            double angle = arc * Math.PI * 2.0 / 10.0 + age * 0.05;
            Vec3 end = new Vec3(Math.cos(angle) * radius, 0.3 + Math.sin(arc * 2.1) * 0.4, Math.sin(angle) * radius);
            this.lightning(vertices, matrix, core, end, arc * 3.3 + Math.floor(age), 0.07, 0.6F, 0.95F, 1.0F, 0.7F * fade);
        }
    }

    private void renderOverloadAura(VertexConsumer vertices, Matrix4f matrix, HeavyMechRenderState entity, float time) {
        float fadeOut = Mth.clamp(entity.overloadTicks / 20.0F, 0.0F, 1.0F);
        float flicker = 0.75F + 0.25F * Mth.sin(time * 1.7F) * Mth.sin(time * 0.31F);
        Vec3 core = new Vec3(0.0, CORE_HEIGHT, 0.0);
        // 过载状态：红色光晕、两道交错旋转的能量环和脚下符文环
        VfxShapes.sphere(vertices, matrix, core.subtract(0.0, 0.3, 0.0), 2.3, 8, 18, 1.0F, 0.2F, 0.12F, 0.1F * flicker * fadeOut);
        VfxShapes.axisRing(vertices, matrix, core, new Vec3(Math.sin(time * 0.05), 1.0, Math.cos(time * 0.05)).normalize().add(0.4, 0.0, 0.0),
            2.0, 0.1, 40, time * 0.12, 1.0F, 0.3F, 0.15F, 0.7F * fadeOut);
        VfxShapes.axisRing(vertices, matrix, core, new Vec3(Math.cos(time * 0.04), 0.6, -Math.sin(time * 0.04)),
            1.7, 0.08, 40, -time * 0.15, 1.0F, 0.55F, 0.2F, 0.6F * fadeOut);
        VfxShapes.dashedRing(vertices, matrix, new Vec3(0.0, 0.06, 0.0), 2.4, 0.16, 12, time * 0.04, 1.0F, 0.25F, 0.15F, 0.6F * flicker * fadeOut);
        VfxShapes.flatRing(vertices, matrix, new Vec3(0.0, 0.05, 0.0), 2.9, 0.08, 48, 0.0, 1.0F, 0.4F, 0.2F, 0.45F * fadeOut);
        if (((int)time / 3) % 2 == 0) {
            double seed = Math.floor(time / 3.0) * 5.17;
            Vec3 start = core.add(Math.sin(seed) * 1.4, Math.cos(seed * 1.3) * 1.2, Math.cos(seed) * 1.4);
            Vec3 end = core.add(Math.sin(seed + 2.4) * 1.6, -1.6, Math.cos(seed + 2.4) * 1.6);
            this.lightning(vertices, matrix, start, end, seed, 0.05, 1.0F, 0.5F, 0.3F, 0.8F * fadeOut);
        }
    }

    private void lightning(VertexConsumer vertices, Matrix4f matrix, Vec3 from, Vec3 to, double seed, double width,
                           float red, float green, float blue, float alpha) {
        if (alpha <= 0.003F) {
            return;
        }
        Vec3 along = to.subtract(from);
        Vec3 reference = Math.abs(along.normalize().y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : UP;
        Vec3 right = along.cross(reference).normalize();
        Vec3 up = right.cross(along).normalize();
        double jitter = along.length() * 0.12;
        Vec3 previous = from;
        int segments = 6;
        // 伪随机折线电弧，外层光晕加内层白芯
        for (int i = 1; i <= segments; i++) {
            double progress = i / (double)segments;
            Vec3 point = from.add(along.scale(progress));
            if (i < segments) {
                point = point.add(right.scale(Math.sin(seed * 3.1 + i * 2.7) * jitter)).add(up.scale(Math.cos(seed * 1.9 + i * 4.3) * jitter));
            }
            VfxShapes.ribbon(vertices, matrix, previous, point, width * 2.2, red, green, blue, alpha * 0.4F);
            VfxShapes.ribbon(vertices, matrix, previous, point, width, 1.0F, 1.0F, 1.0F, alpha);
            previous = point;
        }
    }
}
