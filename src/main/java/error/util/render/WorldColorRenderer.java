package error.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Optional;

public final class WorldColorRenderer {
    private static final WorldColorRenderer INSTANCE = new WorldColorRenderer();
    private static final BindGroupLayout LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler")
            .withSampler("DepthTexture")
            .withUniform("WorldColorUniforms", UniformType.UNIFORM_BUFFER).build();
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("delta:pipeline/atmosphere_world_color"))
            .withVertexShader(Identifier.parse("delta:core/world_color/world_color"))
            .withFragmentShader(Identifier.parse("delta:core/world_color/world_color"))
            .withBindGroupLayout(LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).build();
    private static final int UBO = new Std140SizeCalculator()
            .putVec4().putVec4().putVec4().get();

    private TextureTarget copy;
    private GpuBuffer uniforms;
    private GpuBuffer quad;
    private int vertices;
    private GpuSampler sampler;
    private long lastError;

    private WorldColorRenderer() {
    }

    public static WorldColorRenderer getInstance() {
        return INSTANCE;
    }

    public static RenderPipeline getPipeline() {
        return PIPELINE;
    }

    public void render(int color, float saturation, float brightness,
                       float strength, float contrast, float snowMask,
                       float hazeStrength, float hazeDistance) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null
                || RenderSystem.tryGetDevice() == null) return;
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.getColorTexture() == null
                || target.getColorTextureView() == null || target.width <= 1 || target.height <= 1) return;
        ensure(target.width, target.height);
        if (copy == null || copy.getColorTexture() == null || copy.getColorTextureView() == null) return;
        if (sampler == null) sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                    target.getColorTexture(), copy.getColorTexture(), 0, 0, 0, 0, 0,
                    target.width, target.height);
            float r = ((color >> 16) & 255) / 255.0f;
            float g = ((color >> 8) & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = Std140Builder.onStack(stack, UBO)
                        .putVec4(r, g, b, 1.0f)
                        .putVec4(clamp(saturation, 0, 2), clamp(brightness, .1f, 2),
                                clamp(strength, 0, 1), clamp(contrast, .5f, 1.5f))
                        .putVec4(clamp(snowMask, 0, 1), clamp(hazeStrength, 0, 1),
                                clamp(hazeDistance, 1, 256), 0.0f)
                        .get();
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniforms.slice(), data);
            }
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "WorldTweaks Color Grade", target.getColorTextureView(), Optional.empty())) {
                pass.setPipeline(PIPELINE);
                pass.setUniform("WorldColorUniforms", uniforms);
                pass.bindTexture("SceneSampler", copy.getColorTextureView(), sampler);
                pass.bindTexture("DepthTexture", target.getDepthTextureView(), sampler);
                pass.setVertexBuffer(0, quad.slice());
                pass.draw(vertices, 1, 0, 0);
            }
        } catch (Throwable e) {
            long now = System.currentTimeMillis();
            if (now - lastError > 5000) {
                lastError = now;
                System.err.println("[WorldTweaks] color grading failed: " + e.getMessage());
            }
        }
    }

    private void ensure(int width, int height) {
        if (copy == null || copy.width != width || copy.height != height) {
            if (copy != null) copy.destroyBuffers();
            copy = new TextureTarget("WorldTweaks Scene", width, height, false, GpuFormat.RGBA8_UNORM);
        }
        if (uniforms == null) {
            uniforms = RenderSystem.getDevice().createBuffer(() -> "WorldTweaks Color UBO",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, UBO);
        }
        if (quad == null) {
            BufferBuilder builder = new BufferBuilder(
                    ByteBufferBuilder.exactlySized(6 * DefaultVertexFormat.POSITION_TEX.getVertexSize()),
                    PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(-1, -1, 0).setUv(0, 0);
            builder.addVertex(1, -1, 0).setUv(1, 0);
            builder.addVertex(1, 1, 0).setUv(1, 1);
            builder.addVertex(1, 1, 0).setUv(1, 1);
            builder.addVertex(-1, 1, 0).setUv(0, 1);
            builder.addVertex(-1, -1, 0).setUv(0, 0);
            try (MeshData mesh = builder.buildOrThrow()) {
                vertices = mesh.drawState().vertexCount();
                quad = RenderSystem.getDevice().createBuffer(() -> "WorldTweaks Color Quad",
                        GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
