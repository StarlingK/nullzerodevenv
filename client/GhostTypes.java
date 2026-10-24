package dev.nullmod.client;

import net.minecraft.client.renderer.RenderType;

public final class GhostTypes extends RenderType {
    public static final RenderType BEAM = RenderType.create(
            "nullmod_beam",
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            1536,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private GhostTypes(String name, com.mojang.blaze3d.vertex.VertexFormat format,
                       com.mojang.blaze3d.vertex.VertexFormat.Mode mode, int size, boolean a, boolean b,
                       Runnable setup, Runnable clear) {
        super(name, format, mode, size, a, b, setup, clear);
    }
}
