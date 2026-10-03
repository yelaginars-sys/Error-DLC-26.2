package error.util.render.world.module;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import error.module.impl.render.BlockHighlight;
import error.module.impl.render.BlockOutline;
import error.util.client.clients.ColorUtil;
import error.util.render.Render3DUtil;
import error.util.render.pipeline.PiplinePost;
import error.util.render.pipeline.Post;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 */
public final class BlockHighlightRenderer {
    private static final float BOX_EPSILON = 0.0025F;
    private static final int TRANSFORM_SIZE = new Std140SizeCalculator().putMat4f().get();
    private static final int STYLE_SIZE = new Std140SizeCalculator().putVec4().putVec4().get();

    private final GpuBuffer transformUniforms = uniformBuffer("Block HighLight Transform UBO", TRANSFORM_SIZE);
    private final GpuBuffer styleUniforms = uniformBuffer("Block HighLight Style UBO", STYLE_SIZE);

    private BlockPos selectedPos;
    private BlockState selectedState;
    private long lastFrameNanos;

    private double currentX, currentY, currentZ;
    private float alpha = 0.0F;
    private boolean hasTarget = false;

    private GpuBuffer cachedVertexBuffer;
    private int cachedVertexCount;

    public void render(error.module.Module moduleObj, CameraRenderState cameraState) {
        Minecraft mc = Minecraft.getInstance();
        if (moduleObj == null) return;

        BlockHighlight highlight = moduleObj instanceof BlockHighlight bh ? bh : null;
        BlockOutline outline = moduleObj instanceof BlockOutline bo ? bo : null;
        if (highlight == null && outline == null) return;

        long now = System.nanoTime();
        float deltaSeconds = (this.lastFrameNanos == 0L) ? 0.016F : Math.min(0.1F, (float) ((now - this.lastFrameNanos) / 1.0E9D));
        this.lastFrameNanos = now;

        boolean validHit = mc.level != null
                && mc.gameRenderer != null
                && cameraState != null
                && cameraState.initialized
                && (mc.hitResult instanceof BlockHitResult hit)
                && hit.getType() == HitResult.Type.BLOCK;

        BlockPos blockPos = validHit ? ((BlockHitResult) mc.hitResult).getBlockPos() : null;
        BlockState state = (validHit && blockPos != null) ? mc.level.getBlockState(blockPos) : null;

        if (state != null && state.isAir()) {
            validHit = false;
        }

        float animSpeed = outline != null ? outline.animSpeed.getValue() * 10.0F : (highlight != null ? highlight.animationSpeed.getValue() : 12.0F);
        float factor = 1.0F - (float) Math.exp(-animSpeed * deltaSeconds);

        if (validHit) {
            this.alpha += (1.0F - this.alpha) * factor;

            if (!this.hasTarget || this.alpha < 0.05F) {
                this.currentX = blockPos.getX();
                this.currentY = blockPos.getY();
                this.currentZ = blockPos.getZ();
                this.hasTarget = true;
            } else {
                this.currentX += (blockPos.getX() - this.currentX) * factor;
                this.currentY += (blockPos.getY() - this.currentY) * factor;
                this.currentZ += (blockPos.getZ() - this.currentZ) * factor;
            }

            boolean selectionChanged = !blockPos.equals(this.selectedPos);
            if (selectionChanged || state != this.selectedState || this.cachedVertexBuffer == null) {
                this.selectedPos = blockPos.immutable();
                this.selectedState = state;
                rebuildMesh(mc, blockPos, state);
            }
        } else {
            this.alpha += (0.0F - this.alpha) * factor;
            if (this.alpha <= 0.005F) {
                this.hasTarget = false;
                resetSelection();
                return;
            }
        }

        int color;
        if (outline != null) {
            int col1 = outline.color1.getValue();
            int col2 = outline.color2.getValue();
            float t = (float) (Math.sin((System.currentTimeMillis() * 0.003 * outline.animSpeed.getValue())) * 0.5 + 0.5);
            color = ColorUtil.lerp(col1, col2, t);
        } else {
            color = highlight.getColors();
        }

        if (outline != null && this.hasTarget && this.selectedPos != null && this.selectedState != null) {
            boolean full = outline.fullBlock.getValue();
            List<AABB> boxes = this.selectedState.getShape(mc.level, this.selectedPos).toAabbs();
            if (boxes.isEmpty()) {
                boxes = List.of(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
            }
            int outlineColor = ColorUtil.multiplyAlpha(color, this.alpha);
            int fillColor = ColorUtil.rgba(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), (int) (70 * this.alpha));

            for (AABB box : boxes) {
                AABB animatedBox = box.move(this.currentX - this.selectedPos.getX(), this.currentY - this.selectedPos.getY(), this.currentZ - this.selectedPos.getZ())
                        .move(this.selectedPos.getX(), this.selectedPos.getY(), this.selectedPos.getZ());
                error.util.render.Render3D.drawBox(animatedBox, new java.awt.Color(fillColor, true), new java.awt.Color(outlineColor, true), full, true, false);
            }
        }

        if (this.cachedVertexBuffer == null || this.cachedVertexCount == 0) {
            return;
        }

        var target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.getColorTextureView() == null || target.getDepthTextureView() == null) {
            return;
        }

        Matrix4f projection = Render3DUtil.levelProjectionCopy();
        if (projection == null) {
            return;
        }

        Matrix4f modelViewProjection = projection
                .mul(cameraState.viewRotationMatrix)
                .translate(
                        (float) (this.currentX - cameraState.pos.x() + 0.5D),
                        (float) (this.currentY - cameraState.pos.y() + 0.5D),
                        (float) (this.currentZ - cameraState.pos.z() + 0.5D)
                )
                .translate(-0.5F, -0.5F, -0.5F);

        writeUniforms(moduleObj, modelViewProjection, target.width, target.height, this.alpha, color);

        RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Block HighLight",
                target.getColorTextureView(),
                Optional.empty(),
                target.getDepthTextureView(),
                OptionalDouble.empty()
        );

        try {
            pass.setPipeline(pipeline(moduleObj));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("BlockHighLightTransform", this.transformUniforms);
            pass.setUniform("BlockHighLightStyle", this.styleUniforms);
            pass.setVertexBuffer(0, this.cachedVertexBuffer.slice());
            pass.draw(this.cachedVertexCount, 1, 0, 0);
        } finally {
            pass.close();
        }
    }

    private void rebuildMesh(Minecraft mc, BlockPos blockPos, BlockState state) {
        releaseMesh();
        List<AABB> boxes = state.getShape(mc.level, blockPos).toAabbs();
        if (boxes.isEmpty()) {
            boxes = List.of(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
        }

        MeshData mesh = buildMesh(boxes);
        if (mesh == null) {
            return;
        }

        try (mesh) {
            this.cachedVertexCount = mesh.drawState().vertexCount();
            this.cachedVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "Block HighLight Vertices",
                    GpuBuffer.USAGE_VERTEX,
                    mesh.vertexBuffer()
            );
        }
    }

    private void releaseMesh() {
        if (this.cachedVertexBuffer != null) {
            this.cachedVertexBuffer.close();
            this.cachedVertexBuffer = null;
        }
        this.cachedVertexCount = 0;
    }

    public void resetSelection() {
        this.selectedPos = null;
        this.selectedState = null;
        this.lastFrameNanos = 0L;
        this.hasTarget = false;
        releaseMesh();
    }

    public void release() {
        resetSelection();
        this.transformUniforms.close();
        this.styleUniforms.close();
    }

    private void writeUniforms(error.module.Module moduleObj, Matrix4f modelViewProjection, int width, int height, float alpha, int tint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer transform = Std140Builder.onStack(stack, TRANSFORM_SIZE)
                    .putMat4f(modelViewProjection)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
                    this.transformUniforms.slice(),
                    transform
            );

            float speed = 1.0F;
            float intensity = 1.4F;
            if (moduleObj instanceof BlockHighlight bh) {
                speed = bh.shaderSpeed.getValue();
                intensity = bh.shaderIntensity.getValue();
            } else if (moduleObj instanceof BlockOutline bo) {
                speed = bo.animSpeed.getValue();
                intensity = bo.particlesForce.getValue();
            }

            ByteBuffer style = Std140Builder.onStack(stack, STYLE_SIZE)
                    .putVec4(ColorUtil.red(tint) / 255.0F, ColorUtil.green(tint) / 255.0F, ColorUtil.blue(tint) / 255.0F, alpha * 0.85F)
                    .putVec4((float) width, (float) height, Post.shaderTime() * speed, intensity)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(
                    this.styleUniforms.slice(),
                    style
            );
        }
    }

    private RenderPipeline pipeline(error.module.Module moduleObj) {
        if (moduleObj instanceof BlockOutline bo) {
            String mode = bo.shaderMode.getValue();
            if ("Частицы".equalsIgnoreCase(mode) || "Octgrams".equalsIgnoreCase(mode)) {
                return PiplinePost.BLOCK_HIGHLIGHT_DEEP_SPACE;
            } else if ("Облака".equalsIgnoreCase(mode)) {
                return PiplinePost.BLOCK_HIGHLIGHT_CAUSTICS;
            }
            return PiplinePost.BLOCK_HIGHLIGHT_GLOSSY;
        } else if (moduleObj instanceof BlockHighlight bh) {
            boolean through = bh.ignoreDepth.getValue();
            return switch (bh.modes.getValue()) {
                case BlockHighlight.VARIANT_CAUSTICS -> through ? PiplinePost.BLOCK_HIGHLIGHT_CAUSTICS_THROUGH : PiplinePost.BLOCK_HIGHLIGHT_CAUSTICS;
                case BlockHighlight.VARIANT_DEEP_SPACE -> through ? PiplinePost.BLOCK_HIGHLIGHT_DEEP_SPACE_THROUGH : PiplinePost.BLOCK_HIGHLIGHT_DEEP_SPACE;
                default -> through ? PiplinePost.BLOCK_HIGHLIGHT_GLOSSY_THROUGH : PiplinePost.BLOCK_HIGHLIGHT_GLOSSY;
            };
        }
        return PiplinePost.BLOCK_HIGHLIGHT_GLOSSY;
    }

    private MeshData buildMesh(List<AABB> boxes) {
        int vertexCount = boxes.size() * 36;
        if (vertexCount == 0) {
            return null;
        }

        BufferBuilder builder = new BufferBuilder(
                new ByteBufferBuilder(Math.max(256, vertexCount * DefaultVertexFormat.POSITION.getVertexSize())),
                PrimitiveTopology.TRIANGLES,
                DefaultVertexFormat.POSITION
        );

        for (AABB box : boxes) {
            float minX = (float) box.minX - BOX_EPSILON;
            float minY = (float) box.minY - BOX_EPSILON;
            float minZ = (float) box.minZ - BOX_EPSILON;
            float maxX = (float) box.maxX + BOX_EPSILON;
            float maxY = (float) box.maxY + BOX_EPSILON;
            float maxZ = (float) box.maxZ + BOX_EPSILON;

            face(builder, minX, minY, minZ, maxX, maxY, minZ);
            face(builder, maxX, minY, maxZ, minX, maxY, maxZ);
            face(builder, minX, minY, maxZ, minX, maxY, minZ);
            face(builder, maxX, minY, minZ, maxX, maxY, maxZ);
            face(builder, minX, maxY, minZ, maxX, maxY, maxZ);
            face(builder, minX, minY, maxZ, maxX, minY, minZ);
        }
        return builder.buildOrThrow();
    }

    private void face(BufferBuilder builder, float x1, float y1, float z1, float x2, float y2, float z2) {
        boolean constantX = x1 == x2;
        boolean constantY = y1 == y2;
        if (constantX) {
            vertex(builder, x1, y1, z1);
            vertex(builder, x1, y1, z2);
            vertex(builder, x1, y2, z2);
            vertex(builder, x1, y2, z2);
            vertex(builder, x1, y2, z1);
            vertex(builder, x1, y1, z1);
        } else if (constantY) {
            vertex(builder, x1, y1, z1);
            vertex(builder, x2, y1, z1);
            vertex(builder, x2, y1, z2);
            vertex(builder, x2, y1, z2);
            vertex(builder, x1, y1, z2);
            vertex(builder, x1, y1, z1);
        } else {
            vertex(builder, x1, y1, z1);
            vertex(builder, x2, y1, z1);
            vertex(builder, x2, y2, z1);
            vertex(builder, x2, y2, z1);
            vertex(builder, x1, y2, z1);
            vertex(builder, x1, y1, z1);
        }
    }

    private void vertex(BufferBuilder builder, float x, float y, float z) {
        builder.addVertex(x, y, z);
    }

    private static GpuBuffer uniformBuffer(String label, int size) {
        return RenderSystem.getDevice().createBuffer(
                () -> label,
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                size
        );
    }
}