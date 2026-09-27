package com.hp.mechafauna.client;

import net.minecraft.client.renderer.RenderStateShard;

final class MechRenderStateAccess extends RenderStateShard {
    static final ShaderStateShard POSITION_COLOR_SHADER = RenderStateShard.POSITION_COLOR_SHADER;
    static final TransparencyStateShard LIGHTNING_TRANSPARENCY = RenderStateShard.LIGHTNING_TRANSPARENCY;
    static final TransparencyStateShard ADDITIVE_TRANSPARENCY = RenderStateShard.ADDITIVE_TRANSPARENCY;
    static final CullStateShard NO_CULL = RenderStateShard.NO_CULL;
    static final WriteMaskStateShard COLOR_WRITE = RenderStateShard.COLOR_WRITE;
    static final DepthTestStateShard LEQUAL_DEPTH_TEST = RenderStateShard.LEQUAL_DEPTH_TEST;

    private MechRenderStateAccess() {
        super("mechafauna_render_state_access", () -> {}, () -> {});
    }
}
