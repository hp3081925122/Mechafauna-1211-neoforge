package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.MechStrikeZone;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class MechStrikeZoneRenderer extends EntityRenderer<MechStrikeZone, MechStrikeZoneRenderState> {
    public MechStrikeZoneRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public MechStrikeZoneRenderState createRenderState() {
        return new MechStrikeZoneRenderState();
    }

    @Override
    public void extractRenderState(MechStrikeZone entity, MechStrikeZoneRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.time = entity.tickCount + partialTick;
        state.delay = entity.getDelay();
        state.radius = entity.getRadius();
        state.style = entity.getStyle();
    }

    @Override
    protected net.minecraft.world.phys.AABB getBoundingBoxForCulling(MechStrikeZone entity) {
        return entity.getBoundingBoxForCulling();
    }

    @Override
    public void submit(MechStrikeZoneRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        collector.submitCustomGeometry(poseStack, VfxRenderTypes.GLOW, (entry, vertices) -> {
            this.renderEffects(state, vertices, new Matrix4f(entry.pose()));
        });
    }

    private void renderEffects(MechStrikeZoneRenderState entity, VertexConsumer vertices, Matrix4f matrix) {
        float time = entity.time;
        int delay = entity.delay;
        double radius = entity.radius;
        boolean orbital = entity.style != MechStrikeZone.STYLE_MORTAR;
        boolean plasma = entity.style == MechStrikeZone.STYLE_PLASMA;
        // 轨道轰炸为猩红色，迫击炮为橙色，飞碟等离子为青色
        float red = plasma ? 0.3F : 1.0F;
        float green = plasma ? 0.9F : orbital ? 0.18F : 0.52F;
        float blue = plasma ? 1.0F : orbital ? 0.22F : 0.08F;
        Vec3 ground = new Vec3(0.0, 0.06, 0.0);
        if (time < delay) {
            this.renderWarning(vertices, matrix, ground, radius, time, delay, orbital, red, green, blue);
        } else {
            this.renderAftermath(vertices, matrix, ground, radius, time - delay, orbital, red, green, blue);
        }
    }

    private void renderWarning(VertexConsumer vertices, Matrix4f matrix, Vec3 ground, double radius, float time, int delay,
                               boolean orbital, float red, float green, float blue) {
        float progress = Mth.clamp(time / delay, 0.0F, 1.0F);
        float appear = Mth.clamp(time / 5.0F, 0.0F, 1.0F);
        // 越接近落弹闪烁越快
        float blink = 0.65F + 0.35F * Mth.sin(time * (0.5F + progress * 1.6F));
        VfxShapes.flatRing(vertices, matrix, ground, radius, 0.16, 48, 0.0, red, green, blue, 0.85F * appear * blink);
        VfxShapes.dashedRing(vertices, matrix, ground.add(0.0, 0.01, 0.0), radius * 0.82, 0.1, 16, time * 0.06,
            red, green, blue, 0.6F * appear);
        // 内圈填充表示倒计时
        VfxShapes.disc(vertices, matrix, ground.add(0.0, 0.015, 0.0), radius * progress, 32, red, green, blue, 0.4F * appear);
        VfxShapes.flatRing(vertices, matrix, ground.add(0.0, 0.02, 0.0), radius * progress, 0.08, 40, 0.0,
            1.0F, 0.9F, 0.8F, 0.7F * appear);
        // 十字准星
        double gap = radius * 0.25;
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI * 0.5 + Math.PI * 0.25 + time * 0.02;
            Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            VfxShapes.ribbon(vertices, matrix, ground.add(direction.scale(gap)), ground.add(direction.scale(radius * 1.15)), 0.05,
                red, green, blue, 0.75F * appear * blink);
        }
        if (orbital) {
            // 从天而降的细瞄准光柱
            VfxShapes.cylinder(vertices, matrix, ground, new Vec3(0.0, 1.0, 0.0), MechStrikeZone.FALL_HEIGHT, 0.05 + 0.08 * progress,
                red, green, blue, 0.25F * appear * blink);
        } else {
            // 迫击炮落点的竖直警示光墙
            VfxShapes.wall(vertices, matrix, ground, radius, 0.9 + 0.5 * progress, 32, red, green, blue, 0.3F * appear * blink);
        }
        float fallStart = delay - MechStrikeZone.FALL_TICKS;
        if (time >= fallStart) {
            float fall = Mth.clamp((time - fallStart) / MechStrikeZone.FALL_TICKS, 0.0F, 1.0F);
            double headHeight = MechStrikeZone.FALL_HEIGHT * (1.0F - fall * fall);
            Vec3 head = ground.add(0.0, headHeight, 0.0);
            Vec3 tail = head.add(0.0, orbital ? 7.0 : 3.5, 0.0);
            // 下坠的弹体与拖尾
            boolean plasma = blue > 0.9F;
            VfxShapes.tube(vertices, matrix, head, tail, orbital ? 0.38 : 0.28, 0.02, 10, plasma ? 0.8F : 1.0F, plasma ? 1.0F : 0.85F, plasma ? 1.0F : 0.6F, 0.9F, 0.0F);
            VfxShapes.tube(vertices, matrix, head, tail, orbital ? 0.75 : 0.5, 0.05, 10, red, green, blue, 0.45F, 0.0F);
            VfxShapes.sphere(vertices, matrix, head, orbital ? 0.4 : 0.3, 6, 10, plasma ? 0.9F : 1.0F, plasma ? 1.0F : 0.95F, plasma ? 1.0F : 0.85F, 0.9F);
        }
    }

    private void renderAftermath(VertexConsumer vertices, Matrix4f matrix, Vec3 ground, double radius, float age,
                                 boolean orbital, float red, float green, float blue) {
        float progress = Mth.clamp(age / MechStrikeZone.AFTERMATH_TICKS, 0.0F, 1.0F);
        float fade = (1.0F - progress) * (1.0F - progress);
        // 爆点白光与扩散冲击环
        VfxShapes.disc(vertices, matrix, ground.add(0.0, 0.02, 0.0), radius * (1.1 + progress), 32, 1.0F, 0.95F, 0.85F, 0.8F * fade);
        double shock = radius * (0.6 + 1.6 * progress);
        VfxShapes.flatRing(vertices, matrix, ground, shock, 0.5 * (1.0F - progress) + 0.1, 48, 0.0, red, green, blue, 0.9F * fade);
        VfxShapes.wall(vertices, matrix, ground, shock, 1.2 * (1.0F - progress) + 0.2, 48, red, green, blue, 0.55F * fade);
        // 火球向上膨胀
        double ball = radius * (0.35 + 0.65 * Math.sqrt(progress));
        boolean plasma = blue > 0.9F;
        VfxShapes.sphere(vertices, matrix, ground.add(0.0, ball * 0.4, 0.0), ball, 8, 16, plasma ? 0.5F : 1.0F, plasma ? 0.95F : 0.75F, plasma ? 1.0F : 0.4F, 0.4F * fade);
        if (orbital && progress < 0.5F) {
            // 轨道打击的残留光柱
            float column = 1.0F - progress / 0.5F;
            VfxShapes.cylinder(vertices, matrix, ground, new Vec3(0.0, 1.0, 0.0), MechStrikeZone.FALL_HEIGHT, 0.6 * column,
                1.0F, 0.85F, 0.8F, 0.6F * column);
            VfxShapes.cylinder(vertices, matrix, ground, new Vec3(0.0, 1.0, 0.0), MechStrikeZone.FALL_HEIGHT, 1.2 * column,
                red, green, blue, 0.25F * column);
        }
    }

}
