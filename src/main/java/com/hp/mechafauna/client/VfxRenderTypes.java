package com.hp.mechafauna.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.hp.mechafauna.Mechafauna;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class VfxRenderTypes {
    static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath(Mechafauna.MOD_ID, "pipeline/glow"))
        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
        .withCull(false)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
        .build();
    public static final RenderType GLOW = RenderType.create("mechafauna_glow",
        RenderSetup.builder(PIPELINE).createRenderSetup());

    private VfxRenderTypes() {
    }
}
