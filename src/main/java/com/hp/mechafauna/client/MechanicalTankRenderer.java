package com.hp.mechafauna.client;

import com.hp.mechafauna.entity.MechanicalTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class MechanicalTankRenderer extends GeoEntityRenderer<MechanicalTank> {
    private static final double SHIELD_CENTER_HEIGHT = 1.0;

    public MechanicalTankRenderer(EntityRendererProvider.Context context) {
        super(context, new MechModel<>("mechanical_tank"));
        this.withScale(MechanicalTank.MODEL_SCALE);
    }

    @Override
    public boolean shouldRender(MechanicalTank entity, Frustum frustum, double x, double y, double z) {
        if (MechanicalTank.RAILGUN.equals(entity.getActiveAction())) {
            return frustum.isVisible(new AABB(entity.getRailEnd(), entity.position()).inflate(3.0)) || super.shouldRender(entity, frustum, x, y, z);
        }
        return super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void render(MechanicalTank entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
        if (entity.isDeadOrDying()) {
            return;
        }
        VertexConsumer vertices = buffers.getBuffer(FlyingSaucerRenderer.GLOW);
        Matrix4f matrix = poseStack.last().pose();
        Vec3 position = entity.getPosition(partialTick);
        float time = entity.tickCount + partialTick;
        if (MechanicalTank.RAILGUN.equals(entity.getActiveAction())) {
            this.renderRailgun(vertices, matrix, entity, position, partialTick, entity.getSkillTick() + partialTick, time);
        }
        if (entity.isShielded()) {
            this.renderShield(vertices, matrix, entity, time);
        }
        float burstAge = time - entity.getShieldBurstTick();
        if (burstAge >= 0.0F && burstAge < MechanicalTank.SHIELD_BURST_TICKS) {
            this.renderShieldBurst(vertices, matrix, burstAge);
        }
    }

    private void renderRailgun(VertexConsumer vertices, Matrix4f matrix, MechanicalTank entity, Vec3 position, float partialTick,
                               float skillTime, float time) {
        if (skillTime < MechanicalTank.RAIL_FIRE_TICK) {
            Vec3 muzzleWorld = entity.getCannonMuzzle(partialTick);
            if (muzzleWorld == null) {
                return;
            }
            Vec3 muzzle = muzzleWorld.subtract(position);
            Vec3 aim = entity.getRailEnd().subtract(position);
            Vec3 direction = aim.subtract(muzzle).normalize();
            float charge = Mth.clamp(skillTime / MechanicalTank.RAIL_FIRE_TICK, 0.0F, 1.0F);
            boolean locked = skillTime >= MechanicalTank.RAIL_LOCK_TICK;
            float blink = locked ? (Mth.sin(time * 2.2F) > 0.0F ? 1.0F : 0.35F) : 0.6F + 0.4F * Mth.sin(time * 0.7F);
            // 瞄准激光：跟踪时为暗红色细线，锁定后变亮并闪烁
            Vec3 laserEnd = muzzle.add(direction.scale(MechanicalTank.RAIL_RANGE));
            VfxShapes.tube(vertices, matrix, muzzle, laserEnd, locked ? 0.05 : 0.025, locked ? 0.05 : 0.025, 6,
                1.0F, locked ? 0.35F : 0.1F, locked ? 0.4F : 0.12F, 0.7F * blink, 0.15F * blink);
            if (locked) {
                VfxShapes.sphere(vertices, matrix, aim, 0.25 + 0.1 * blink, 6, 10, 1.0F, 0.3F, 0.3F, 0.7F * blink);
                VfxShapes.axisRing(vertices, matrix, aim, direction, 0.9 - 0.3 * blink, 0.06, 24, time * 0.2, 1.0F, 0.3F, 0.3F, 0.8F);
            }
            // 炮口聚能球与向炮口收缩的电磁环
            double core = 0.12 + 0.45 * charge * charge;
            VfxShapes.sphere(vertices, matrix, muzzle, core * 1.8, 8, 14, 0.3F, 0.8F, 1.0F, 0.35F * charge);
            VfxShapes.sphere(vertices, matrix, muzzle, core, 6, 12, 0.85F, 1.0F, 1.0F, 0.9F * charge);
            for (int i = 0; i < 4; i++) {
                float phase = (time * 0.09F + i / 4.0F) % 1.0F;
                Vec3 center = muzzle.subtract(direction.scale(2.2 * phase));
                VfxShapes.axisRing(vertices, matrix, center, direction, 0.25 + 0.9 * (1.0F - phase) * charge, 0.07, 20, time * 0.15,
                    0.35F, 0.9F, 1.0F, 0.75F * charge * phase);
            }
            return;
        }
        float age = skillTime - MechanicalTank.RAIL_FIRE_TICK;
        float progress = Mth.clamp(age / MechanicalTank.RAIL_BEAM_TICKS, 0.0F, 1.0F);
        float open = Mth.clamp(age / 2.0F, 0.0F, 1.0F);
        float power = open * (1.0F - progress) * (1.0F - progress);
        if (power <= 0.003F) {
            return;
        }
        Vec3 start = entity.getRailStart().subtract(position);
        Vec3 end = entity.getRailEnd().subtract(position);
        Vec3 along = end.subtract(start);
        double length = along.length();
        if (length < 0.1) {
            return;
        }
        Vec3 direction = along.scale(1.0 / length);
        // 贯穿光束：白色光芯、青色中层和外晕，随时间收细
        VfxShapes.tube(vertices, matrix, start, end, 0.22 * power + 0.03, 0.18 * power + 0.03, 12, 1.0F, 1.0F, 1.0F, 0.95F * power, 0.85F * power);
        VfxShapes.tube(vertices, matrix, start, end, 0.55 * power, 0.45 * power, 12, 0.35F, 0.9F, 1.0F, 0.5F * power, 0.4F * power);
        VfxShapes.tube(vertices, matrix, start, end, 1.0 * open * (1.0F - progress * 0.5F), 0.8 * open, 12, 0.2F, 0.55F, 1.0F, 0.2F * power, 0.12F * power);
        // 沿光束螺旋分布的电磁轨道环
        int rings = Math.max(4, (int)(length / 1.6));
        for (int i = 0; i < rings; i++) {
            double t = (i + 0.5) / rings;
            double offset = (age * 0.06 + t) % 1.0;
            Vec3 center = start.add(direction.scale(offset * length));
            float alpha = 0.7F * power * (float)Math.sin(offset * Math.PI);
            VfxShapes.axisRing(vertices, matrix, center, direction, 0.5 + 0.6 * progress, 0.08, 18, i * 0.7 + age * 0.3,
                0.5F, 0.95F, 1.0F, alpha);
        }
        // 炮口冲击环
        VfxShapes.axisRing(vertices, matrix, start.add(direction.scale(0.4 + progress * 1.6)), direction, 0.6 + 2.4 * progress, 0.3 * (1.0F - progress) + 0.05,
            28, 0.0, 0.7F, 1.0F, 1.0F, 0.8F * (1.0F - progress));
        VfxShapes.axisRing(vertices, matrix, start.add(direction.scale(0.2 + progress * 0.8)), direction, 0.4 + 1.4 * progress, 0.2 * (1.0F - progress) + 0.04,
            24, 0.0, 1.0F, 1.0F, 1.0F, 0.6F * (1.0F - progress));
        // 命中点闪光
        VfxShapes.sphere(vertices, matrix, end, 0.4 + 1.6 * progress, 8, 16, 0.6F, 0.95F, 1.0F, 0.6F * (1.0F - progress));
        VfxShapes.axisRing(vertices, matrix, end, direction, 0.6 + 2.8 * progress, 0.25, 32, 0.0, 0.4F, 0.9F, 1.0F, 0.7F * (1.0F - progress));
    }

    private void renderShield(VertexConsumer vertices, Matrix4f matrix, MechanicalTank entity, float time) {
        float age = entity.getShieldMaxTicks() - entity.getShieldTicks();
        float intro = Mth.clamp(age / MechanicalTank.SHIELD_INTRO_TICKS, 0.0F, 1.0F);
        float ease = 1.0F - (1.0F - intro) * (1.0F - intro) * (1.0F - intro);
        int remaining = entity.getShieldTicks();
        float ending = remaining < 30 ? (Mth.sin(time * 1.6F) > -0.2F ? 1.0F : 0.25F) : 1.0F;
        float hit = Mth.clamp(1.0F - (time - entity.getShieldHitTick()) / 8.0F, 0.0F, 1.0F);
        double radius = MechanicalTank.SHIELD_RADIUS * (0.4 + 0.6 * ease) * (1.0 + 0.04 * hit);
        Vec3 center = new Vec3(0.0, SHIELD_CENTER_HEIGHT, 0.0);
        float alpha = ease * ending;
        float red = 0.3F + 0.7F * hit;
        float green = 0.85F + 0.15F * hit;
        // 半透明护盾壳体，受击时整体闪白
        VfxShapes.sphere(vertices, matrix, center, radius, 12, 24, red, green, 1.0F, (0.12F + 0.25F * hit) * alpha);
        // 经纬交错的六边形蜂窝纹路，缓慢旋转
        double spin = time * 0.012;
        int latitudes = 6;
        for (int ring = 1; ring < latitudes; ring++) {
            double theta = Math.PI * ring / latitudes;
            int cells = 12;
            for (int cell = 0; cell < cells; cell++) {
                double shift = (ring % 2) * Math.PI / cells;
                double phi0 = cell * Math.PI * 2.0 / cells + shift + spin;
                double phi1 = phi0 + Math.PI * 2.0 / cells * 0.62;
                Vec3 a = VfxShapes.spherePoint(center, radius * 1.002, theta, phi0);
                Vec3 b = VfxShapes.spherePoint(center, radius * 1.002, theta, phi1);
                Vec3 c = VfxShapes.spherePoint(center, radius * 1.002, theta + Math.PI / latitudes * 0.5, (phi0 + phi1) * 0.5 + Math.PI / cells * 0.5);
                float shimmer = 0.5F + 0.5F * Mth.sin((float)(time * 0.15 + ring * 1.3 + cell * 0.9));
                float lineAlpha = (0.35F + 0.4F * shimmer + 0.3F * hit) * alpha;
                VfxShapes.ribbon(vertices, matrix, a, b, 0.025, red, green, 1.0F, lineAlpha);
                VfxShapes.ribbon(vertices, matrix, b, c, 0.025, red, green, 1.0F, lineAlpha * 0.8F);
            }
        }
        // 赤道能量环与地面投影
        VfxShapes.axisRing(vertices, matrix, center, new Vec3(0.0, 1.0, 0.0), radius, 0.1, 48, spin * 3.0, 0.5F, 0.95F, 1.0F, 0.7F * alpha);
        VfxShapes.flatRing(vertices, matrix, new Vec3(0.0, 0.06, 0.0), radius * 0.95, 0.2, 48, 0.0, 0.3F, 0.85F, 1.0F, 0.6F * alpha);
        VfxShapes.dashedRing(vertices, matrix, new Vec3(0.0, 0.07, 0.0), radius * 1.15, 0.1, 20, -time * 0.03, 0.4F, 0.9F, 1.0F, 0.5F * alpha);
        if (intro < 1.0F) {
            // 展开瞬间的扫描环
            VfxShapes.axisRing(vertices, matrix, center.add(0.0, radius * (intro * 2.0 - 1.0), 0.0), new Vec3(0.0, 1.0, 0.0),
                radius * Math.sqrt(Math.max(0.0, 1.0 - (intro * 2.0 - 1.0) * (intro * 2.0 - 1.0))), 0.15, 40, 0.0,
                0.9F, 1.0F, 1.0F, 0.9F * (1.0F - intro));
        }
    }

    private void renderShieldBurst(VertexConsumer vertices, Matrix4f matrix, float age) {
        float progress = age / MechanicalTank.SHIELD_BURST_TICKS;
        float fade = (1.0F - progress) * (1.0F - progress);
        double radius = MechanicalTank.SHIELD_RADIUS + (MechanicalTank.SHIELD_BURST_RADIUS - MechanicalTank.SHIELD_RADIUS) * Math.sqrt(progress);
        Vec3 center = new Vec3(0.0, SHIELD_CENTER_HEIGHT, 0.0);
        // 护盾碎裂：外扩球壳、地面冲击环和四散碎片
        VfxShapes.sphere(vertices, matrix, center, radius, 10, 20, 0.5F, 0.95F, 1.0F, 0.25F * fade);
        VfxShapes.flatRing(vertices, matrix, new Vec3(0.0, 0.08, 0.0), radius * 1.05, 0.6 * (1.0F - progress) + 0.1, 48, 0.0,
            0.4F, 0.9F, 1.0F, 0.85F * fade);
        VfxShapes.wall(vertices, matrix, new Vec3(0.0, 0.08, 0.0), radius * 1.05, 1.4 * (1.0F - progress) + 0.2, 48, 0.4F, 0.9F, 1.0F, 0.5F * fade);
        for (int shard = 0; shard < 18; shard++) {
            double theta = Math.PI * (0.15 + 0.5 * ((shard * 0.618) % 1.0));
            double phi = shard * 2.399;
            Vec3 from = VfxShapes.spherePoint(center, radius * 0.92, theta, phi);
            Vec3 to = VfxShapes.spherePoint(center, radius * 1.12, theta, phi + 0.05);
            VfxShapes.ribbon(vertices, matrix, from, to, 0.06, 0.8F, 1.0F, 1.0F, 0.9F * fade);
        }
    }
}
