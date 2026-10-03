package error.util.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.OptionalDouble;

/** Depth-tested textured snow renderer. */
public final class WinterSnowRenderer {
    private static final RenderPipeline SNOW = RenderPipeline.builder()
            .withLocation(Identifier.parse("delta:pipeline/winter_snow"))
            .withVertexShader(Identifier.parse("delta:core/aura_bloom"))
            .withFragmentShader(Identifier.parse("delta:core/aura_bloom"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private static final RenderPipeline SNOW_COVER = RenderPipeline.builder()
            .withLocation(Identifier.parse("delta:pipeline/winter_snow_cover"))
            .withVertexShader(Identifier.parse("delta:core/aura_bloom"))
            .withFragmentShader(Identifier.parse("delta:core/aura_bloom"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private WinterSnowRenderer() { }

    public static void draw(BufferBuilder buffer, int quads, Identifier textureId) {
        draw(buffer, quads, textureId, SNOW, "Render-Winter-Textured-Snow");
    }

    public static void drawCover(BufferBuilder buffer, int quads, Identifier textureId) {
        draw(buffer, quads, textureId, SNOW_COVER, "Render-Winter-Snow-Cover");
    }

    private static void draw(BufferBuilder buffer, int quads, Identifier textureId,
                             RenderPipeline pipeline, String name) {
        if (buffer == null || quads <= 0 || textureId == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.mainRenderTarget() == null) return;
        AbstractTexture texture = mc.getTextureManager().getTexture(textureId);
        if (texture == null) return;
        try (MeshData mesh = buffer.buildOrThrow()) {
            GpuBuffer vertices = RenderSystem.getDevice().createBuffer(
                    () -> name, 32, mesh.vertexBuffer());
            try {
                var target = mc.gameRenderer.mainRenderTarget();
                try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> name,
                        target.getColorTextureView(), Optional.empty(),
                        target.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(pipeline);
                    var projection = LevelProjection.get();
                    pass.setUniform("Projection", projection != null
                            ? projection : RenderSystem.getProjectionMatrixBuffer());
                    pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
                    pass.setVertexBuffer(0, vertices.slice());
                    GpuBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS)
                            .getBuffer(quads * 6);
                    pass.setIndexBuffer(indices,
                            RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).type());
                    pass.drawIndexed(quads * 6, 1, 0, 0, 0);
                }
            } finally {
                vertices.close();
            }
        } catch (Throwable ignored) { }
    }
}
