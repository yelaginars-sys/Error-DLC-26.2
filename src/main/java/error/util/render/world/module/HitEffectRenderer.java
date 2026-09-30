package error.util.render.world.module;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import error.module.impl.render.HitEffect;
import error.util.client.clients.ColorUtil;
import error.util.render.Render3DUtil;
import error.util.render.pipeline.FullscreenQuad;
import error.util.render.pipeline.PiplinePost;
import error.util.render.pipeline.Post;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Create by daun kvass
 */
public final class HitEffectRenderer {
    private static final int MAX_HITS = 4;

    private static final int UBO_SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putMat4f()
            .putMat4f()
            .putVec4()
            .putVec4()
            .putVec4().putVec4().putVec4()
            .putVec4().putVec4().putVec4()
            .putVec4().putVec4().putVec4()
            .putVec4().putVec4().putVec4()
            .get();

    private GpuBuffer uniformsBuffer;
    private TextureTarget sceneCopy;

    private final List<HitInstance> activeHits = new ArrayList<>();

    public static class HitInstance {
        public final Vec3 pos;
        public final long startTime;
        public final float duration;
        public final float maxRadius;
        public final float distortion;
        public final float glow;
        public final int color;
        public final boolean isWorld;

        public HitInstance(Vec3 pos, float duration, float maxRadius, float distortion, float glow, int color, boolean isWorld) {
            this.pos = pos;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
            this.maxRadius = maxRadius;
            this.distortion = distortion;
            this.glow = glow;
            this.color = color;
            this.isWorld = isWorld;
        }

        public float getProgress() {
            float elapsed = (System.currentTimeMillis() - this.startTime) / 1000.0F;
            return Math.min(1.0F, elapsed / this.duration);
        }

        public boolean isDead() {
            return getProgress() >= 1.0F;
        }
    }

    public synchronized void addHit(Vec3 pos, float radius, boolean isWorld, HitEffect module) {
        if (this.activeHits.size() >= MAX_HITS * 2) {
            this.activeHits.removeFirst();
        }
        this.activeHits.add(new HitInstance(
                pos,
                module.duration.getValue().floatValue(),
                radius,
                module.distortion.getValue().floatValue(),
                module.glow.getValue().floatValue(),
                module.getColor(),
                isWorld
        ));
    }

    public synchronized boolean hasHits() {
        this.activeHits.removeIf(HitInstance::isDead);
        return !this.activeHits.isEmpty();
    }

    public synchronized void render(HitEffect module, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.gameRenderer.mainRenderTarget();
        if (!valid(mainTarget) || cameraState == null || !cameraState.initialized) {
            return;
        }

        this.activeHits.removeIf(HitInstance::isDead);
        if (this.activeHits.isEmpty()) {
            return;
        }

        Matrix4f projection = Render3DUtil.levelProjectionCopy();
        if (projection == null) {
            return;
        }

        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f viewMatrix = new Matrix4f(cameraState.viewRotationMatrix);
        Matrix4f invViewMatrix = new Matrix4f(viewMatrix).invert();

        ensureSceneCopy(mainTarget.width, mainTarget.height);

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                mainTarget.getColorTexture(),
                this.sceneCopy.getColorTexture(),
                0, 0, 0, 0, 0,
                mainTarget.width,
                mainTarget.height
        );

        writeUniforms(cameraState, projection, invProjection, viewMatrix, invViewMatrix, mainTarget.width, mainTarget.height);

        GpuSampler linearSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        GpuSampler nearestSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "hit shockwave pass",
                mainTarget.getColorTextureView(),
                Optional.empty()
        )) {
            pass.setPipeline(PiplinePost.WORLD_HIT_EFFECT);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("HitEffectUniforms", getUniformBuffer());
            pass.bindTexture("SceneSampler", this.sceneCopy.getColorTextureView(), linearSampler);
            pass.bindTexture("DepthSampler", mainTarget.getDepthTextureView(), nearestSampler);
            drawFullscreen(pass);
        }
    }

    private void writeUniforms(CameraRenderState cameraState, Matrix4f proj, Matrix4f invProj, Matrix4f view, Matrix4f invView, int width, int height) {
        int count = Math.min(MAX_HITS, this.activeHits.size());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, UBO_SIZE)
                    .putMat4f(proj)
                    .putMat4f(invProj)
                    .putMat4f(view)
                    .putMat4f(invView)
                    .putVec4((float) cameraState.pos.x, (float) cameraState.pos.y, (float) cameraState.pos.z, 1.0F)
                    .putVec4(1.0F / width, 1.0F / height, (float) count, Post.shaderTime());

            for (int i = 0; i < MAX_HITS; i++) {
                if (i < count) {
                    HitInstance hit = this.activeHits.get(this.activeHits.size() - 1 - i);
                    float progress = hit.getProgress();
                    builder.putVec4((float) hit.pos.x, (float) hit.pos.y, (float) hit.pos.z, progress)
                            .putVec4(hit.maxRadius, hit.distortion, hit.glow, hit.isWorld ? 1.0F : 0.0F)
                            .putVec4(ColorUtil.red(hit.color) / 255.0F, ColorUtil.green(hit.color) / 255.0F, ColorUtil.blue(hit.color) / 255.0F, 1.0F - progress);
                } else {
                    builder.putVec4(0, 0, 0, 1)
                            .putVec4(0, 0, 0, 0)
                            .putVec4(0, 0, 0, 0);
                }
            }

            ByteBuffer data = builder.get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(getUniformBuffer().slice(), data);
        }
    }

    private GpuBuffer getUniformBuffer() {
        if (this.uniformsBuffer == null) {
            this.uniformsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "hit effect ubo",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UBO_SIZE
            );
        }
        return this.uniformsBuffer;
    }

    private void ensureSceneCopy(int width, int height) {
        if (this.sceneCopy != null && this.sceneCopy.width == width && this.sceneCopy.height == height) {
            return;
        }
        if (this.sceneCopy != null) this.sceneCopy.destroyBuffers();
        this.sceneCopy = new TextureTarget("hit scene copy", width, height, false, PiplinePost.EFFECT_FORMAT);
    }

    private static void drawFullscreen(RenderPass pass) {
        pass.setVertexBuffer(0, FullscreenQuad.buffer().slice());
        pass.draw(FullscreenQuad.vertexCount(), 1, 0, 0);
    }

    private static boolean valid(RenderTarget target) {
        return target != null && target.width > 0 && target.height > 0 && target.getColorTexture() != null && target.getDepthTexture() != null;
    }

    public synchronized void clear() {
        this.activeHits.clear();
    }

    public void release() {
        clear();
        if (this.sceneCopy != null) {
            this.sceneCopy.destroyBuffers();
            this.sceneCopy = null;
        }
        if (this.uniformsBuffer != null) {
            this.uniformsBuffer.close();
            this.uniformsBuffer = null;
        }
    }
}