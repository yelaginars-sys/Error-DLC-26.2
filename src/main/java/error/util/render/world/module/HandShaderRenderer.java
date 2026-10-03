package error.util.render.world.module;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import error.module.impl.render.HandShader;
import error.util.client.clients.ColorUtil;
import error.util.render.pipeline.FullscreenQuad;
import error.util.render.pipeline.PiplinePost;
import error.util.render.pipeline.Post;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 */
public final class HandShaderRenderer {
    private static final Vector4f CLEAR_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);

    private static final int MASK_SIZE = new Std140SizeCalculator().putVec2().putVec2().get();
    private static final int FILL_SIZE = new Std140SizeCalculator().putVec4().putVec4().get();
    private static final int GLASS_SIZE = new Std140SizeCalculator().putVec4().putFloat().get();
    private static final int OUTLINE_SIZE = new Std140SizeCalculator().putVec4().putFloat().get();
    private static final int HALO_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().get();
    private static final int TRAIL_SIZE = new Std140SizeCalculator().putVec2().putFloat().putFloat().putFloat().putFloat().get();
    private static final int FLAME_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().get();
    private static final int BLUR_SIZE = new Std140SizeCalculator().putVec2().putFloat().putFloat().get();

    private GpuBuffer maskUniforms;
    private GpuBuffer fillUniforms;
    private GpuBuffer glassUniforms;
    private GpuBuffer outlineUniforms;
    private GpuBuffer haloUniforms;
    private GpuBuffer trailUniforms;
    private GpuBuffer flameUniforms;
    private GpuBuffer blurUniforms;

    private TextureTarget beforeTarget;
    private TextureTarget maskRawTarget;
    private TextureTarget maskTarget;
    private TextureTarget trailA;
    private TextureTarget trailB;
    private TextureTarget blurTargetA;
    private TextureTarget blurTargetB;

    private boolean trailUsesA = false;
    private boolean flameHistoryActive = false;

    public void render(HandShader module, Runnable handDraw) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
        if (!valid(mainTarget)) {
            handDraw.run();
            return;
        }

        initBuffers();
        ensureTargets(mainTarget.width, mainTarget.height);

        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(
                mainTarget.getColorTexture(),
                this.beforeTarget.getColorTexture(),
                0, 0, 0, 0, 0,
                mainTarget.width,
                mainTarget.height
        );
        encoder.copyTextureToTexture(
                mainTarget.getDepthTexture(),
                this.beforeTarget.getDepthTexture(),
                0, 0, 0, 0, 0,
                mainTarget.width,
                mainTarget.height
        );

        handDraw.run();

        GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler depthSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        writeMaskUniforms(module, mainTarget.width, mainTarget.height);
        renderMask(mainTarget, sampler, depthSampler);

        if (module.hasFill()) {
            if (module.fillType.is(HandShader.GLASS)) {
                writeGlassUniforms(module);
                renderGlass(mainTarget, sampler);
            } else {
                writeFillUniforms(module);
                renderFill(module, mainTarget, sampler);
            }
        }

        if (module.hasOutline()) {
            writeOutlineUniforms(module);
            renderOutline(mainTarget, sampler);
        }

        if (module.hasGlow()) {
            int passes = (int) module.blurPasses.getValue().floatValue();
            GpuTextureView blurredMask = blurMask(this.maskTarget.getColorTextureView(), mainTarget.width, mainTarget.height, module.glowRadius.getValue().floatValue(), passes, sampler);
            if (module.hasFlame()) {
                float time = Post.shaderTime() * module.flameSpeed.getValue().floatValue();
                writeTrailUniforms(module, mainTarget.width, mainTarget.height, time);
                GpuTextureView flame = renderTrail(blurredMask, sampler);

                writeFlameUniforms(module, time);
                renderFlame(mainTarget, flame, sampler);
                this.flameHistoryActive = true;
            } else {
                clearFlameHistoryIfNeeded();
                writeHaloUniforms(module);
                renderHalo(mainTarget, blurredMask, sampler);
            }
        } else {
            clearFlameHistoryIfNeeded();
        }
    }

    private void renderMask(RenderTarget mainTarget, GpuSampler linearSampler, GpuSampler depthSampler) {
        try (RenderPass pass = renderPass("hand mask raw", this.maskRawTarget)) {
            pass.setPipeline(PiplinePost.HAND_MASK);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandMaskUniforms", this.maskUniforms);
            pass.bindTexture("BeforeTexture", this.beforeTarget.getColorTextureView(), linearSampler);
            pass.bindTexture("AfterTexture", mainTarget.getColorTextureView(), linearSampler);
            pass.bindTexture("BeforeDepth", this.beforeTarget.getDepthTextureView(), depthSampler);
            pass.bindTexture("AfterDepth", mainTarget.getDepthTextureView(), depthSampler);
            draw(pass);
        }

        try (RenderPass pass = renderPass("hand mask smooth", this.maskTarget)) {
            pass.setPipeline(PiplinePost.HAND_MASK_SMOOTH);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandMaskUniforms", this.maskUniforms);
            pass.bindTexture("RawMask", this.maskRawTarget.getColorTextureView(), linearSampler);
            draw(pass);
        }
    }

    private void renderFill(HandShader module, RenderTarget mainTarget, GpuSampler sampler) {
        try (RenderPass pass = renderPass("hand fill", mainTarget)) {
            if (module.fillType.is(HandShader.PLASMA)) {
                pass.setPipeline(PiplinePost.HAND_PLASMA);
            } else if (module.fillType.is(HandShader.NOISE)) {
                pass.setPipeline(PiplinePost.HAND_NOISE);
            } else if (module.fillType.is(HandShader.HOLOGRAM)) {
                pass.setPipeline(PiplinePost.HAND_HOLOGRAM);
            } else {
                pass.setPipeline(PiplinePost.HAND_FILL);
            }
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandFillUniforms", this.fillUniforms);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private void renderGlass(RenderTarget mainTarget, GpuSampler sampler) {
        try (RenderPass pass = renderPass("hand glass", mainTarget)) {
            pass.setPipeline(PiplinePost.HAND_GLASS);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandGlassUniforms", this.glassUniforms);
            pass.bindTexture("SceneSampler", this.beforeTarget.getColorTextureView(), sampler);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private void renderOutline(RenderTarget mainTarget, GpuSampler sampler) {
        try (RenderPass pass = renderPass("hand outline", mainTarget)) {
            pass.setPipeline(PiplinePost.HAND_OUTLINE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandOutlineUniforms", this.outlineUniforms);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private void renderHalo(RenderTarget mainTarget, GpuTextureView blurredMask, GpuSampler sampler) {
        try (RenderPass pass = renderPass("hand halo", mainTarget)) {
            pass.setPipeline(PiplinePost.HAND_HALO);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandHaloUniforms", this.haloUniforms);
            pass.bindTexture("BlurredSampler", blurredMask, sampler);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private GpuTextureView renderTrail(GpuTextureView injectTexture, GpuSampler sampler) {
        TextureTarget src = this.trailUsesA ? this.trailA : this.trailB;
        TextureTarget dst = this.trailUsesA ? this.trailB : this.trailA;

        try (RenderPass pass = renderPass("hand trail accum", dst)) {
            pass.setPipeline(PiplinePost.HAND_TRAIL);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandTrailUniforms", this.trailUniforms);
            pass.bindTexture("PrevSampler", src.getColorTextureView(), sampler);
            pass.bindTexture("InjectSampler", injectTexture, sampler);
            draw(pass);
        }

        this.trailUsesA = !this.trailUsesA;
        return dst.getColorTextureView();
    }

    private void renderFlame(RenderTarget mainTarget, GpuTextureView flame, GpuSampler sampler) {
        try (RenderPass pass = renderPass("hand flame composite", mainTarget)) {
            pass.setPipeline(PiplinePost.SHADER_HANDS);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HandCompositeUniforms", this.flameUniforms);
            pass.bindTexture("BlurredSampler", flame, sampler);
            pass.bindTexture("MaskSampler", this.maskTarget.getColorTextureView(), sampler);
            draw(pass);
        }
    }

    private GpuTextureView blurMask(GpuTextureView source, int width, int height, float radius, int passes, GpuSampler sampler) {
        GpuTextureView currentInput = source;
        float baseOffset = radius / 16.0F;

        for (int i = 0; i < passes; i++) {
            float passOffset = baseOffset * (i + 1);
            writeBlurUniforms(width, height, passOffset);

            TextureTarget target = (i % 2 == 0) ? this.blurTargetA : this.blurTargetB;
            try (RenderPass pass = renderPass("hand blur down " + i, target)) {
                pass.setPipeline(PiplinePost.HAND_BLUR_DOWN);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("HandBlurUniforms", this.blurUniforms);
                pass.bindTexture("CurrentInput", currentInput, sampler);
                draw(pass);
            }
            currentInput = target.getColorTextureView();
        }

        for (int i = passes - 1; i >= 1; i--) {
            float passOffset = baseOffset * (i + 0.5f);
            writeBlurUniforms(width, height, passOffset);

            TextureTarget target = (i % 2 == 0) ? this.blurTargetB : this.blurTargetA;
            try (RenderPass pass = renderPass("hand blur up " + i, target)) {
                pass.setPipeline(PiplinePost.HAND_BLUR_UP);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("HandBlurUniforms", this.blurUniforms);
                pass.bindTexture("CurrentInput", currentInput, sampler);
                draw(pass);
            }
            currentInput = target.getColorTextureView();
        }

        return currentInput;
    }

    private void writeMaskUniforms(HandShader module, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        boolean left = module.bothHands.getValue();
        boolean right = module.bothHands.getValue();
        if (!module.bothHands.getValue() && mc.player != null) {
            left = mc.player.getMainArm() == HumanoidArm.LEFT;
            right = mc.player.getMainArm() == HumanoidArm.RIGHT;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, MASK_SIZE).putVec2(1.0F / width, 1.0F / height).putVec2(left ? 1.0F : 0.0F, right ? 1.0F : 0.0F).get();
            write(this.maskUniforms, data);
        }
    }

    private void writeFillUniforms(HandShader module) {
        int color = module.getBaseColor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, FILL_SIZE)
                    .putVec4(ColorUtil.red(color) / 255.0F, ColorUtil.green(color) / 255.0F, ColorUtil.blue(color) / 255.0F, module.fillOpacity.getValue().floatValue())
                    .putVec4(Post.shaderTime() * module.speeds.getValue().floatValue(), 0.0F, 0.0F, 0.0F)
                    .get();
            write(this.fillUniforms, data);
        }
    }

    private void writeGlassUniforms(HandShader module) {
        int color = module.getBaseColor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, GLASS_SIZE).putVec4(ColorUtil.red(color) / 255.0F, ColorUtil.green(color) / 255.0F, ColorUtil.blue(color) / 255.0F, module.fillOpacity.getValue().floatValue()).putFloat(1.0F).get();
            write(this.glassUniforms, data);
        }
    }

    private void writeOutlineUniforms(HandShader module) {
        int color = module.getBaseColor();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, OUTLINE_SIZE).putVec4(ColorUtil.red(color) / 255.0F, ColorUtil.green(color) / 255.0F, ColorUtil.blue(color) / 255.0F, 1.0F).putFloat(module.outlineThickness.getValue().floatValue()).get();
            write(this.outlineUniforms, data);
        }
    }

    private void writeHaloUniforms(HandShader module) {
        int leftCol = module.getLeftGlowColor();
        int rightCol = module.getRightGlowColor();
        float strength = module.glowStrength.getValue().floatValue();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, HALO_SIZE)
                    .putVec4(ColorUtil.red(leftCol) / 255.0F, ColorUtil.green(leftCol) / 255.0F, ColorUtil.blue(leftCol) / 255.0F, strength)
                    .putVec4(ColorUtil.red(rightCol) / 255.0F, ColorUtil.green(rightCol) / 255.0F, ColorUtil.blue(rightCol) / 255.0F, strength)
                    .putVec4(module.glowRadius.getValue().floatValue() / 12.0F, 0.0F, 0.0F, 0.0F)
                    .get();
            write(this.haloUniforms, data);
        }
    }

    private void writeTrailUniforms(HandShader module, int width, int height, float time) {
        float dirCode = 2.0F;
        if (module.flameDirection.is(HandShader.FLAME_UP)) {
            dirCode = 0.0F;
        } else if (module.flameDirection.is(HandShader.FLAME_DOWN)) {
            dirCode = 1.0F;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, TRAIL_SIZE)
                    .putVec2(1.0F / width, -1.0F / height)
                    .putFloat(module.flameTrail.getValue().floatValue())
                    .putFloat(module.flameSpeed.getValue().floatValue())
                    .putFloat(time)
                    .putFloat(dirCode)
                    .get();
            write(this.trailUniforms, data);
        }
    }

    private void writeFlameUniforms(HandShader module, float time) {
        int leftCol = module.getLeftGlowColor();
        int rightCol = module.getRightGlowColor();
        float strength = module.glowStrength.getValue().floatValue();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, FLAME_SIZE)
                    .putVec4(ColorUtil.red(leftCol) / 255.0F, ColorUtil.green(leftCol) / 255.0F, ColorUtil.blue(leftCol) / 255.0F, strength)
                    .putVec4(ColorUtil.red(rightCol) / 255.0F, ColorUtil.green(rightCol) / 255.0F, ColorUtil.blue(rightCol) / 255.0F, strength)
                    .putVec4(time, 0.0F, 0.0F, 0.0F)
                    .get();
            write(this.flameUniforms, data);
        }
    }

    private void writeBlurUniforms(int width, int height, float offset) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, BLUR_SIZE).putVec2(1.0F / width, 1.0F / height).putFloat(offset).putFloat(1.0F).get();
            write(this.blurUniforms, data);
        }
    }

    private void initBuffers() {
        if (this.maskUniforms == null) this.maskUniforms = ubo("Arm Mask UBO", MASK_SIZE);
        if (this.fillUniforms == null) this.fillUniforms = ubo("Arm Fill UBO", FILL_SIZE);
        if (this.glassUniforms == null) this.glassUniforms = ubo("Arm Glass UBO", GLASS_SIZE);
        if (this.outlineUniforms == null) this.outlineUniforms = ubo("Arm Outline UBO", OUTLINE_SIZE);
        if (this.haloUniforms == null) this.haloUniforms = ubo("Arm Halo UBO", HALO_SIZE);
        if (this.trailUniforms == null) this.trailUniforms = ubo("Arm Trail UBO", TRAIL_SIZE);
        if (this.flameUniforms == null) this.flameUniforms = ubo("Arm Flame UBO", FLAME_SIZE);
        if (this.blurUniforms == null) this.blurUniforms = ubo("Arm Blur UBO", BLUR_SIZE);
    }

    private void ensureTargets(int width, int height) {
        boolean resetTrail = this.trailA == null || this.trailA.width != width || this.trailA.height != height;

        this.beforeTarget = ensureTarget(this.beforeTarget, "arm before", width, height, true, PiplinePost.EFFECT_FORMAT);
        this.maskRawTarget = ensureTarget(this.maskRawTarget, "arm mask raw", width, height, false, PiplinePost.MASK_RAW_FORMAT);
        this.maskTarget = ensureTarget(this.maskTarget, "arm mask smooth", width, height, false, PiplinePost.EFFECT_FORMAT);
        this.trailA = ensureTarget(this.trailA, "arm trail A", width, height, false, PiplinePost.EFFECT_FORMAT);
        this.trailB = ensureTarget(this.trailB, "arm trail B", width, height, false, PiplinePost.EFFECT_FORMAT);
        this.blurTargetA = ensureTarget(this.blurTargetA, "arm blur A", Math.max(1, width / 2), Math.max(1, height / 2), false, PiplinePost.EFFECT_FORMAT);
        this.blurTargetB = ensureTarget(this.blurTargetB, "arm blur B", Math.max(1, width / 2), Math.max(1, height / 2), false, PiplinePost.EFFECT_FORMAT);

        if (resetTrail) {
            clearHistory();
        }
    }

    private TextureTarget ensureTarget(TextureTarget target, String label, int width, int height, boolean useDepth, GpuFormat format) {
        if (target != null && target.width == width && target.height == height) return target;
        if (target != null) target.destroyBuffers();
        return new TextureTarget(label, width, height, useDepth, format);
    }

    private static RenderPass renderPass(String label, RenderTarget target) {
        return RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> label, target.getColorTextureView(), Optional.empty());
    }

    private static void draw(RenderPass pass) {
        pass.setVertexBuffer(0, FullscreenQuad.buffer().slice());
        pass.draw(FullscreenQuad.vertexCount(), 1, 0, 0);
    }

    private static void write(GpuBuffer buffer, ByteBuffer data) {
        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data);
    }

    private static GpuBuffer ubo(String label, int size) {
        return RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, size);
    }

    private static boolean valid(RenderTarget target) {
        return target != null && target.width > 0 && target.height > 0 && target.getColorTexture() != null && target.getDepthTexture() != null;
    }

    public void clearHistory() {
        this.trailUsesA = false;
        this.flameHistoryActive = false;
        if (this.trailA != null && this.trailA.getColorTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.trailA.getColorTexture(), CLEAR_COLOR);
        }
        if (this.trailB != null && this.trailB.getColorTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.trailB.getColorTexture(), CLEAR_COLOR);
        }
    }

    private void clearFlameHistoryIfNeeded() {
        if (this.flameHistoryActive) clearHistory();
    }

    public void release() {
        clearHistory();
        if (this.beforeTarget != null) this.beforeTarget.destroyBuffers();
        if (this.maskRawTarget != null) this.maskRawTarget.destroyBuffers();
        if (this.maskTarget != null) this.maskTarget.destroyBuffers();
        if (this.trailA != null) this.trailA.destroyBuffers();
        if (this.trailB != null) this.trailB.destroyBuffers();
        if (this.blurTargetA != null) this.blurTargetA.destroyBuffers();
        if (this.blurTargetB != null) this.blurTargetB.destroyBuffers();
        if (this.maskUniforms != null) this.maskUniforms.close();
        if (this.fillUniforms != null) this.fillUniforms.close();
        if (this.glassUniforms != null) this.glassUniforms.close();
        if (this.outlineUniforms != null) this.outlineUniforms.close();
        if (this.haloUniforms != null) this.haloUniforms.close();
        if (this.trailUniforms != null) this.trailUniforms.close();
        if (this.flameUniforms != null) this.flameUniforms.close();
        if (this.blurUniforms != null) this.blurUniforms.close();
    }
}