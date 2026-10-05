package error.util.render.world.particles;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render3DUtil;
import error.module.impl.render.WorldParticles;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 */
public final class CubeParticleRenderer {
    private static final float LINE_WIDTH = 0.0022F;

    private static final RenderPipeline TRANSLUCENT_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/cube_particles"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private static final RenderPipeline ADDITIVE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/cube_particles_additive"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private final List<CubeParticle> particles = new ArrayList<>();
    private GpuBuffer vertexBuffer;
    private long lastFrameNanos = 0L;

    public void render(WorldParticles module, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            this.particles.clear();
            return;
        }

        long nowNanos = System.nanoTime();
        float dt = this.lastFrameNanos == 0L ? 0.016F : (float) ((nowNanos - this.lastFrameNanos) / 1_000_000_000.0);
        this.lastFrameNanos = nowNanos;
        dt = Mth.clamp(dt, 0.001F, 0.05F);

        Vec3 playerPos = mc.player.position();
        float radius = module.radius.getValue();
        float lifetime = module.lifetime.getValue();
        int targetCount = module.count.getValue().intValue();

        float finalDt = dt;
        this.particles.removeIf(p -> {
            p.update(finalDt, playerPos, radius);
            return p.isDead();
        });

        while (this.particles.size() < targetCount) {
            this.particles.add(new CubeParticle(
                    playerPos,
                    radius,
                    module.size.getValue(),
                    lifetime
            ));
        }

        if (this.particles.isEmpty()) return;

        var target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.getColorTextureView() == null || target.getDepthTextureView() == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);

        int baseColor = Theme.getAccentColor();
        boolean glow = module.glow.getValue();
        boolean innerCore = module.innerGlow.getValue();
        boolean diagonals = module.diagonals.getValue();
        boolean cornerOnly = module.cornerEdges.getValue();

        BufferBuilder builder = new BufferBuilder(
                new ByteBufferBuilder(Math.max(this.particles.size() * 300 * DefaultVertexFormat.POSITION_COLOR.getVertexSize(), 2048)),
                PrimitiveTopology.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR
        );

        int drawnVertices = 0;
        Matrix4f rotMatrix = new Matrix4f();
        Vector4f[] v = new Vector4f[8];
        Vector4f[] coreV = new Vector4f[8];
        for (int i = 0; i < 8; i++) {
            v[i] = new Vector4f();
            coreV[i] = new Vector4f();
        }

        for (CubeParticle p : this.particles) {
            float alpha = p.getAlphaProgress();
            if (alpha <= 0.005F) continue;

            Vector4f centerV = Render3DUtil.toViewSpace(p.pos, cameraPos, viewPose);
            if (centerV.z >= -0.25F) continue;

            float h = p.size * (0.4F + alpha * 0.6F) * 0.5F;
            float coreH = h * 0.38F;

            rotMatrix.identity().rotateXYZ(
                    (float) Math.toRadians(p.pitch),
                    (float) Math.toRadians(p.yaw),
                    (float) Math.toRadians(p.roll)
            );

            transformVertex(v[0], -h, -h, -h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[1],  h, -h, -h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[2],  h,  h, -h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[3], -h,  h, -h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[4], -h, -h,  h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[5],  h, -h,  h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[6],  h,  h,  h, rotMatrix, p.pos, cameraPos, viewPose);
            transformVertex(v[7], -h,  h,  h, rotMatrix, p.pos, cameraPos, viewPose);

            float mainAlpha = (ColorUtil.alpha(baseColor) / 255.0F) * alpha;

            int cTop = applyShade(baseColor, 1.0F, mainAlpha * 0.22F);
            int cSide = applyShade(baseColor, 0.75F, mainAlpha * 0.18F);
            int cBottom = applyShade(baseColor, 0.5F, mainAlpha * 0.15F);

            addQuad(builder, v[0], v[3], v[2], v[1], cSide);
            addQuad(builder, v[5], v[6], v[7], v[4], cSide);
            addQuad(builder, v[3], v[7], v[6], v[2], cTop);
            addQuad(builder, v[4], v[0], v[1], v[5], cBottom);
            addQuad(builder, v[4], v[7], v[3], v[0], cSide);
            addQuad(builder, v[1], v[2], v[6], v[5], cSide);
            drawnVertices += 36;

            if (innerCore) {
                transformVertex(coreV[0], -coreH, -coreH, -coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[1],  coreH, -coreH, -coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[2],  coreH,  coreH, -coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[3], -coreH,  coreH, -coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[4], -coreH, -coreH,  coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[5],  coreH, -coreH,  coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[6],  coreH,  coreH,  coreH, rotMatrix, p.pos, cameraPos, viewPose);
                transformVertex(coreV[7], -coreH,  coreH,  coreH, rotMatrix, p.pos, cameraPos, viewPose);

                int cCore = applyShade(baseColor, 1.4F, mainAlpha * 0.85F);
                addQuad(builder, coreV[0], coreV[3], coreV[2], coreV[1], cCore);
                addQuad(builder, coreV[5], coreV[6], coreV[7], coreV[4], cCore);
                addQuad(builder, coreV[3], coreV[7], coreV[6], coreV[2], cCore);
                addQuad(builder, coreV[4], coreV[0], coreV[1], coreV[5], cCore);
                addQuad(builder, coreV[4], coreV[7], coreV[3], coreV[0], cCore);
                addQuad(builder, coreV[1], coreV[2], coreV[6], coreV[5], cCore);
                drawnVertices += 36;
            }

            if (diagonals) {
                int cDiag = applyShade(baseColor, 1.1F, mainAlpha * 0.45F);
                drawnVertices += addLine(builder, v[0], v[6], LINE_WIDTH * 0.8F, cDiag);
                drawnVertices += addLine(builder, v[1], v[7], LINE_WIDTH * 0.8F, cDiag);
                drawnVertices += addLine(builder, v[2], v[4], LINE_WIDTH * 0.8F, cDiag);
                drawnVertices += addLine(builder, v[3], v[5], LINE_WIDTH * 0.8F, cDiag);
            }

            int cEdge = applyShade(baseColor, 1.35F, mainAlpha * 0.95F);
            float cornerRatio = cornerOnly ? 0.3F : 1.0F;

            drawnVertices += renderEdgeSegment(builder, v[0], v[1], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[1], v[2], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[2], v[3], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[3], v[0], cornerRatio, cEdge);

            drawnVertices += renderEdgeSegment(builder, v[4], v[5], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[5], v[6], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[6], v[7], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[7], v[4], cornerRatio, cEdge);

            drawnVertices += renderEdgeSegment(builder, v[0], v[4], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[1], v[5], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[2], v[6], cornerRatio, cEdge);
            drawnVertices += renderEdgeSegment(builder, v[3], v[7], cornerRatio, cEdge);
        }

        if (drawnVertices == 0) return;

        MeshData meshData = builder.buildOrThrow();
        var device = RenderSystem.getDevice();
        try {
            ByteBuffer vertexData = meshData.vertexBuffer();
            int remainingBytes = vertexData.remaining();
            ensureVertexCapacity(remainingBytes);

            device.createCommandEncoder().writeToBuffer(this.vertexBuffer.slice(0, remainingBytes), vertexData);

            try (RenderPass pass = device.createCommandEncoder().createRenderPass(
                    () -> "Error World Cube Particles",
                    target.getColorTextureView(),
                    Optional.empty(),
                    target.getDepthTextureView(),
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(glow ? ADDITIVE_PIPELINE : TRANSLUCENT_PIPELINE);
                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                pass.setVertexBuffer(0, this.vertexBuffer.slice(0, remainingBytes));
                pass.draw(drawnVertices, 1, 0, 0);
            }
        } finally {
            meshData.close();
        }
    }

    private static int renderEdgeSegment(BufferBuilder builder, Vector4f a, Vector4f b, float ratio, int color) {
        if (ratio >= 0.99F) {
            return addLine(builder, a, b, LINE_WIDTH, color);
        }
        Vector4f aEnd = new Vector4f(
                a.x + (b.x - a.x) * ratio,
                a.y + (b.y - a.y) * ratio,
                a.z + (b.z - a.z) * ratio,
                1.0F
        );
        Vector4f bStart = new Vector4f(
                b.x + (a.x - b.x) * ratio,
                b.y + (a.y - b.y) * ratio,
                b.z + (a.z - b.z) * ratio,
                1.0F
        );
        return addLine(builder, a, aEnd, LINE_WIDTH, color) + addLine(builder, bStart, b, LINE_WIDTH, color);
    }

    private static int addLine(BufferBuilder builder, Vector4f a, Vector4f b, float width, int color) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float len = (float) Math.hypot(dx, dy);
        float nx = len > 0.0001F ? -dy / len * width : width;
        float ny = len > 0.0001F ? dx / len * width : 0.0F;

        builder.addVertex(a.x + nx, a.y + ny, a.z).setColor(color);
        builder.addVertex(a.x - nx, a.y - ny, a.z).setColor(color);
        builder.addVertex(b.x - nx, b.y - ny, b.z).setColor(color);
        builder.addVertex(a.x + nx, a.y + ny, a.z).setColor(color);
        builder.addVertex(b.x - nx, b.y - ny, b.z).setColor(color);
        builder.addVertex(b.x + nx, b.y + ny, b.z).setColor(color);
        return 6;
    }

    private static void transformVertex(Vector4f out, float lx, float ly, float lz, Matrix4f rot, Vec3 center, Vec3 cameraPos, Matrix4f viewPose) {
        out.set(lx, ly, lz, 1.0F);
        rot.transform(out);
        Vec3 world = center.add(out.x, out.y, out.z);
        Vector4f view = Render3DUtil.toViewSpace(world, cameraPos, viewPose);
        out.set(view.x, view.y, view.z, 1.0F);
    }

    private static void addQuad(BufferBuilder builder, Vector4f v1, Vector4f v2, Vector4f v3, Vector4f v4, int color) {
        builder.addVertex(v1.x, v1.y, v1.z).setColor(color);
        builder.addVertex(v2.x, v2.y, v2.z).setColor(color);
        builder.addVertex(v3.x, v3.y, v3.z).setColor(color);
        builder.addVertex(v1.x, v1.y, v1.z).setColor(color);
        builder.addVertex(v3.x, v3.y, v3.z).setColor(color);
        builder.addVertex(v4.x, v4.y, v4.z).setColor(color);
    }

    private static int applyShade(int color, float shade, float alpha) {
        int r = Math.min(255, (int) (ColorUtil.red(color) * shade));
        int g = Math.min(255, (int) (ColorUtil.green(color) * shade));
        int b = Math.min(255, (int) (ColorUtil.blue(color) * shade));
        int a = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void ensureVertexCapacity(int byteSize) {
        if (this.vertexBuffer != null && this.vertexBuffer.size() >= byteSize) return;
        if (this.vertexBuffer != null) this.vertexBuffer.close();
        int capacity = Math.max(byteSize + byteSize / 2, 64 * 1024);
        this.vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Cube Particles Buffer",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                capacity
        );
    }

    public void release() {
        this.particles.clear();
        this.lastFrameNanos = 0L;
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }
}