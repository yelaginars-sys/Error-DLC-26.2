package error.util.render.pipeline;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class HudBlurPipeline {
    private static final int BLUR_UNIFORM_SIZE = 256;
    private static final int MAX_GLASS_RECTS = 64;
    private static final int GLASS_UNIFORM_SIZE = 64 + 5 * 16 + MAX_GLASS_RECTS * 3 * 16;
    private static final int SEPARABLE_UNIFORM_SIZE = 256;
    private static final int BLUR_DOWNSCALE = 2;
    private static final int BLUR_ITERATIONS = 1;
    private static final float GLASS_INTERIOR_ALPHA = 1.0F;
    private static final float GLASS_RIM_STRENGTH = 0.0F;
    private static final int MAX_QUEUED = 64;

    private static RenderPipeline blurPipeline;
    private static RenderPipeline glassPipeline;
    private static GpuBuffer blurUniform;
    private static GpuBuffer glassUniform;
    private static ByteBuffer scratchBuffer;
    private static GpuTexture copyTexture;
    private static GpuTextureView copyTextureView;
    private static GpuTexture blurTextureA;
    private static GpuTextureView blurTextureAView;
    private static GpuTexture blurTextureB;
    private static GpuTextureView blurTextureBView;
    private static RenderPipeline separablePipeline;
    private static GpuBuffer separableUniformH;
    private static GpuBuffer separableUniformV;
    private static int lastWidth;
    private static int lastHeight;
    private static int lastBlurWidth;
    private static int lastBlurHeight;

    private static final List<BlurRect> BLUR_QUEUE = new ArrayList<>();
    private static final List<GlassRect> GLASS_QUEUE = new ArrayList<>();

    private HudBlurPipeline() {
    }

    public static void request(float x, float y, float width, float height, float radius,
                               float blurRadius, float smoothness, float alpha,
                               float red, float green, float blue, float tintAlpha) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;
        if (BLUR_QUEUE.size() >= MAX_QUEUED) BLUR_QUEUE.remove(0);
        BLUR_QUEUE.add(new BlurRect(x, y, width, height,
                Math.min(radius, Math.min(width, height) * 0.5F),
                blurRadius, smoothness, alpha, red, green, blue, tintAlpha));
    }

    public static void requestGlass(float x, float y, float width, float height, float radius,
                                    float blurRadius, float alpha, float smoothing,
                                    float red, float green, float blue, float tintAlpha) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;
        if (GLASS_QUEUE.size() >= MAX_GLASS_RECTS) GLASS_QUEUE.remove(0);
        GLASS_QUEUE.add(new GlassRect(x, y, width, height,
                Math.min(radius, Math.min(width, height) * 0.5F),
                blurRadius, alpha, smoothing, red, green, blue, tintAlpha));
    }

    public static void flush() {
        if (BLUR_QUEUE.isEmpty() && GLASS_QUEUE.isEmpty()) return;

        List<BlurRect> blurs = new ArrayList<>(BLUR_QUEUE);
        List<GlassRect> glass = new ArrayList<>(GLASS_QUEUE);
        BLUR_QUEUE.clear();
        GLASS_QUEUE.clear();

        if (blurPipeline == null || glassPipeline == null || blurUniform == null || glassUniform == null
                || separablePipeline == null || separableUniformH == null || separableUniformV == null) {
            init();
            if (blurPipeline == null || glassPipeline == null
                    || blurUniform == null || glassUniform == null) {
                return;
            }
        }

        Minecraft client = Minecraft.getInstance();
        RenderTarget framebuffer = client.gameRenderer.mainRenderTarget();
        if (framebuffer == null || framebuffer.getColorTexture() == null) {
            return;
        }

        int fbWidth = framebuffer.width;
        int fbHeight = framebuffer.height;
        ensureCopyTexture(fbWidth, fbHeight);
        if (copyTexture == null || copyTextureView == null) return;

        int blurWidth = Math.max(1, fbWidth / BLUR_DOWNSCALE);
        int blurHeight = Math.max(1, fbHeight / BLUR_DOWNSCALE);
        ensureBlurTextures(blurWidth, blurHeight);

        int screenWidth = client.getWindow().getWidth();
        int screenHeight = client.getWindow().getHeight();
        Matrix4f projection = new Matrix4f().setOrtho(0.0F, (float) screenWidth,
                (float) screenHeight, 0.0F, -1000.0F, 1000.0F);
        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        try {
            encoder.copyTextureToTexture(framebuffer.getColorTexture(), copyTexture,
                    0, 0, 0, 0, 0, fbWidth, fbHeight);

            if (!blurs.isEmpty()) {
                drawBlurPass(encoder, framebuffer, copyTextureView, sampler, projection,
                        screenWidth, screenHeight, blurs);
            }
            if (!glass.isEmpty()) {
                float maxGlassBlur = 0.0F;
                for (GlassRect rect : glass) maxGlassBlur = Math.max(maxGlassBlur, rect.blurRadius());
                runSeparableBlur(encoder, copyTextureView, sampler, blurWidth, blurHeight, maxGlassBlur);

                GpuTextureView glassSource = blurTextureBView != null ? blurTextureBView : copyTextureView;
                drawGlassPass(encoder, framebuffer, glassSource, sampler, projection,
                        fbWidth, fbHeight, glass);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                encoder.submit();
            } catch (Exception ignored) {
            }
        }
    }

    private static void drawBlurPass(CommandEncoder encoder, RenderTarget framebuffer,
                                     GpuTextureView copyView, GpuSampler sampler,
                                     Matrix4f projection, int screenWidth, int screenHeight,
                                     List<BlurRect> rects) throws Exception {
        for (BlurRect rect : rects) {
            ByteBuffer buffer = ensureScratch();
            buffer.clear();
            projection.get(buffer);
            buffer.position(64);
            buffer.putFloat(rect.x()).putFloat(rect.y()).putFloat(rect.width()).putFloat(rect.height());
            buffer.putFloat(screenWidth).putFloat(screenHeight).putFloat(0f).putFloat(0f);
            buffer.putFloat(rect.radius()).putFloat(rect.radius()).putFloat(rect.radius()).putFloat(rect.radius());
            buffer.putFloat(rect.blurRadius()).putFloat(rect.smoothness())
                    .putFloat(clamp(rect.alpha(), 0f, 1f)).putFloat(rect.tintAlpha());
            buffer.putFloat(rect.red()).putFloat(rect.green()).putFloat(rect.blue()).putFloat(0f);
            buffer.putFloat(0f).putFloat(0f).putFloat(0f).putFloat(0f);
            buffer.flip();
            encoder.writeToBuffer(blurUniform.slice(), buffer);

            try (RenderPass pass = encoder.createRenderPass(
                    () -> "ErrorHudBlur",
                    framebuffer.getColorTextureView(),
                    Optional.empty(),
                    framebuffer.getDepthTextureView(),
                    OptionalDouble.empty())) {
                pass.setPipeline(blurPipeline);
                pass.setUniform("Uniforms", blurUniform);
                pass.bindTexture("Sampler0", copyView, sampler);
                pass.draw(6, 1, 0, 0);
            }
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private static void drawGlassPass(CommandEncoder encoder, RenderTarget framebuffer,
                                      GpuTextureView copyView, GpuSampler sampler,
                                      Matrix4f projection, int fbWidth, int fbHeight,
                                      List<GlassRect> rects) throws Exception {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float maxBlur = 0.0F;
        for (GlassRect rect : rects) {
            minX = Math.min(minX, rect.x());
            minY = Math.min(minY, rect.y());
            maxX = Math.max(maxX, rect.x() + rect.width());
            maxY = Math.max(maxY, rect.y() + rect.height());
            maxBlur = Math.max(maxBlur, rect.blurRadius());
        }
        float pad = maxBlur + 4.0F;
        float regionX = Math.max(0.0F, minX - pad);
        float regionY = Math.max(0.0F, minY - pad);
        float regionW = Math.min(fbWidth, maxX + pad) - regionX;
        float regionH = Math.min(fbHeight, maxY + pad) - regionY;
        if (regionW <= 0.0F || regionH <= 0.0F) return;

        float smoothing = 0.0F;
        for (GlassRect rect : rects) smoothing = Math.max(smoothing, Math.abs(rect.smoothing()));

        ByteBuffer buffer = ensureScratch(GLASS_UNIFORM_SIZE);
        buffer.clear();
        projection.get(buffer);
        buffer.position(64);
        buffer.putFloat(regionX).putFloat(regionY).putFloat(regionW).putFloat(regionH);
        buffer.putFloat(fbWidth).putFloat(fbHeight).putFloat(1.0F / fbWidth).putFloat(1.0F / fbHeight);
        buffer.putFloat(1.0F).putFloat(smoothing).putFloat(GLASS_RIM_STRENGTH).putFloat(9.0F);
        buffer.putFloat(rects.size()).putFloat(1.0F).putFloat(0.0F).putFloat(0f);
        buffer.putFloat(GLASS_INTERIOR_ALPHA).putFloat(1.0F).putFloat(0.35F).putFloat(0f);

        writeArray(buffer, rects, rect -> put4(buffer, rect.x(), rect.y(), rect.width(), rect.height()));
        writeArray(buffer, rects, rect -> put4(buffer, rect.radius(), rect.blurRadius(), rect.alpha(), rect.smoothing()));
        writeArray(buffer, rects, rect -> put4(buffer, rect.red(), rect.green(), rect.blue(), rect.tintAlpha()));
        buffer.flip();
        encoder.writeToBuffer(glassUniform.slice(), buffer);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "ErrorLiquidGlass",
                framebuffer.getColorTextureView(),
                Optional.empty(),
                framebuffer.getDepthTextureView(),
                OptionalDouble.empty())) {
            pass.setPipeline(glassPipeline);
            pass.setUniform("Uniforms", glassUniform);
            pass.bindTexture("Sampler0", copyView, sampler);
            pass.draw(6, 1, 0, 0);
        }
    }

    private static void writeArray(ByteBuffer buffer, List<GlassRect> rects,
                                   java.util.function.Consumer<GlassRect> writer) {
        for (int i = 0; i < MAX_GLASS_RECTS; i++) {
            if (i < rects.size()) {
                writer.accept(rects.get(i));
            } else {
                put4(buffer, 0f, 0f, 0f, 0f);
            }
        }
    }

    private static void put4(ByteBuffer buffer, float a, float b, float c, float d) {
        buffer.putFloat(a).putFloat(b).putFloat(c).putFloat(d);
    }

    private static ByteBuffer ensureScratch() {
        return ensureScratch(Math.max(BLUR_UNIFORM_SIZE, GLASS_UNIFORM_SIZE));
    }

    private static ByteBuffer ensureScratch(int size) {
        if (scratchBuffer == null || scratchBuffer.capacity() < size) {
            if (scratchBuffer != null) MemoryUtil.memFree(scratchBuffer);
            scratchBuffer = MemoryUtil.memAlloc(size);
        }
        return scratchBuffer;
    }

    private static void ensureCopyTexture(int width, int height) {
        if (copyTexture != null && width == lastWidth && height == lastHeight) return;

        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }

        copyTexture = RenderSystem.getDevice().createTexture(
                () -> "error:hud_blur_copy",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM,
                width, height, 1, 1);
        copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);
        lastWidth = width;
        lastHeight = height;
    }

    private static void ensureBlurTextures(int width, int height) {
        if (blurTextureA != null && blurTextureB != null
                && width == lastBlurWidth && height == lastBlurHeight) {
            return;
        }
        closeBlurTextures();

        int usage = GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        blurTextureA = RenderSystem.getDevice().createTexture(
                () -> "error:hud_blur_sep_a", usage, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        blurTextureAView = RenderSystem.getDevice().createTextureView(blurTextureA);
        blurTextureB = RenderSystem.getDevice().createTexture(
                () -> "error:hud_blur_sep_b", usage, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
        blurTextureBView = RenderSystem.getDevice().createTextureView(blurTextureB);
        lastBlurWidth = width;
        lastBlurHeight = height;
    }

    private static void closeBlurTextures() {
        if (blurTextureAView != null) {
            blurTextureAView.close();
            blurTextureAView = null;
        }
        if (blurTextureBView != null) {
            blurTextureBView.close();
            blurTextureBView = null;
        }
        if (blurTextureA != null) {
            blurTextureA.close();
            blurTextureA = null;
        }
        if (blurTextureB != null) {
            blurTextureB.close();
            blurTextureB = null;
        }
    }

    private static void runSeparableBlur(CommandEncoder encoder, GpuTextureView source,
                                         GpuSampler sampler, int width, int height,
                                         float radius) throws Exception {
        if (separablePipeline == null || separableUniformH == null || separableUniformV == null
                || blurTextureAView == null || blurTextureBView == null) {
            return;
        }
        float halfRadius = Math.max(radius, 0.0F) / BLUR_DOWNSCALE;
        if (halfRadius < 0.5F) halfRadius = 0.5F;

        for (int i = 0; i < BLUR_ITERATIONS; i++) {
            GpuTextureView src = i == 0 ? source : blurTextureBView;
            blurSeparable(encoder, src, blurTextureAView, sampler, width, height, halfRadius,
                    1.0F, 0.0F, separableUniformH);
            blurSeparable(encoder, blurTextureAView, blurTextureBView, sampler, width, height,
                    halfRadius, 0.0F, 1.0F, separableUniformV);
        }
    }

    private static void blurSeparable(CommandEncoder encoder, GpuTextureView src,
                                      GpuTextureView dst, GpuSampler sampler,
                                      int width, int height, float radius,
                                      float dirX, float dirY, GpuBuffer uniformBuffer) throws Exception {
        ByteBuffer buffer = ensureScratch(SEPARABLE_UNIFORM_SIZE);
        buffer.clear();
        buffer.putFloat(1.0F / width).putFloat(1.0F / height).putFloat(dirX).putFloat(dirY);
        buffer.putFloat(radius).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
        buffer.flip();
        encoder.writeToBuffer(uniformBuffer.slice(), buffer);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "ErrorSeparableBlur",
                dst,
                Optional.<Vector4fc>empty())) {
            pass.setPipeline(separablePipeline);
            pass.setUniform("Uniforms", uniformBuffer.slice());
            pass.bindTexture("Sampler0", src, sampler);
            pass.draw(6, 1, 0, 0);
        }
    }

    public static void init() {
        if (blurPipeline != null && glassPipeline != null) return;

        try {
            blurPipeline = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("error", "hud_blur"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("error", "core/blur_rect"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("error", "core/blur_rect"))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withBindGroupLayout(BindGroupLayout.builder().withUniform("Uniforms", UniformType.UNIFORM_BUFFER).build())
                    .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withCull(false)
                    .build();

            glassPipeline = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("error", "liquid_glass"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("error", "core/liquid_glass"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("error", "core/liquid_glass"))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withBindGroupLayout(BindGroupLayout.builder().withUniform("Uniforms", UniformType.UNIFORM_BUFFER).build())
                    .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withCull(false)
                    .build();

            separablePipeline = RenderPipeline.builder()
                    .withLocation(Identifier.fromNamespaceAndPath("error", "blur_separable"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("error", "core/blur_separable"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("error", "core/blur_separable"))
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withBindGroupLayout(BindGroupLayout.builder().withUniform("Uniforms", UniformType.UNIFORM_BUFFER).build())
                    .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .build();

            separableUniformH = RenderSystem.getDevice().createBuffer(
                    () -> "ErrorSeparableBlur H Uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    SEPARABLE_UNIFORM_SIZE);

            separableUniformV = RenderSystem.getDevice().createBuffer(
                    () -> "ErrorSeparableBlur V Uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    SEPARABLE_UNIFORM_SIZE);

            blurUniform = RenderSystem.getDevice().createBuffer(
                    () -> "ErrorHudBlur Uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    BLUR_UNIFORM_SIZE);

            glassUniform = RenderSystem.getDevice().createBuffer(
                    () -> "ErrorLiquidGlass Uniforms",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    GLASS_UNIFORM_SIZE);

            ensureScratch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void shutdown() {
        if (blurUniform != null) {
            blurUniform.close();
            blurUniform = null;
        }
        if (glassUniform != null) {
            glassUniform.close();
            glassUniform = null;
        }
        if (separableUniformH != null) {
            separableUniformH.close();
            separableUniformH = null;
        }
        if (separableUniformV != null) {
            separableUniformV.close();
            separableUniformV = null;
        }
        closeBlurTextures();
        if (scratchBuffer != null) {
            MemoryUtil.memFree(scratchBuffer);
            scratchBuffer = null;
        }
        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }
        BLUR_QUEUE.clear();
        GLASS_QUEUE.clear();
        lastWidth = 0;
        lastHeight = 0;
    }

    private record BlurRect(float x, float y, float width, float height, float radius,
                            float blurRadius, float smoothness, float alpha,
                            float red, float green, float blue, float tintAlpha) {
    }

    private record GlassRect(float x, float y, float width, float height, float radius,
                             float blurRadius, float alpha, float smoothing,
                             float red, float green, float blue, float tintAlpha) {
    }
}
