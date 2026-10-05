package error.util.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import error.IMinecraft;
import error.util.client.clients.ColorUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ThreadLocalRandom;

public class TargetLightningV2 implements IMinecraft {

    private static final Identifier BLOOM_TEX = Identifier.fromNamespaceAndPath("error", "textures/targetesp/bloom.png");

    private static final RenderPipeline LIGHTNING_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_lightning_v2"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private static final RenderPipeline BLOOM_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_lightning_v2_bloom"))
            .withVertexShader(Identifier.parse("error:core/aura_bloom"))
            .withFragmentShader(Identifier.parse("error:core/aura_bloom"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private static final class Bolt {
        final List<Vec3> points;
        final List<List<Vec3>> branches;
        final long spawn;
        final long life;
        final float flicker;

        Bolt(List<Vec3> points, List<List<Vec3>> branches, long life, float flicker) {
            this.points = points;
            this.branches = branches;
            this.spawn = System.currentTimeMillis();
            this.life = life;
            this.flicker = flicker;
        }
    }

    private final ArrayList<Bolt> bolts = new ArrayList<>();
    private long lastBoltSpawn = 0L;
    private float appearValue = 0.0F;
    private LivingEntity lastTarget = null;
    private Vec3 lastTargetPos = null;

    public void clear() {
        this.bolts.clear();
        this.appearValue = 0.0F;
        this.lastTarget = null;
        this.lastTargetPos = null;
    }

    public void render(LivingEntity currentTarget, float tickDelta, int baseColor, boolean hurtColor, float widthScale) {
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) return;

        boolean hasTarget = currentTarget != null && currentTarget.isAlive();
        float targetAppear = hasTarget ? 1.0F : 0.0F;
        this.appearValue = Mth.lerp(0.08F, this.appearValue, targetAppear);

        if (hasTarget) {
            this.lastTarget = currentTarget;
            this.lastTargetPos = new Vec3(
                    Mth.lerp(tickDelta, currentTarget.xOld, currentTarget.getX()),
                    Mth.lerp(tickDelta, currentTarget.yOld, currentTarget.getY()),
                    Mth.lerp(tickDelta, currentTarget.zOld, currentTarget.getZ())
            );
        }

        if (this.appearValue <= 0.005F) {
            this.bolts.clear();
            if (!hasTarget) {
                this.lastTarget = null;
                this.lastTargetPos = null;
            }
            return;
        }

        renderLightning(tickDelta, hasTarget, baseColor, hurtColor, widthScale);
    }

    private void renderLightning(float partialTicks, boolean hasTarget, int baseColor, boolean hurtColor, float widthScale) {
        if (this.lastTarget == null && this.lastTargetPos == null) return;

        long now = System.currentTimeMillis();
        int maxBolts = 8;
        long spawnInterval = 35L;
        if (hasTarget && this.lastTarget != null && this.lastTarget.isAlive() && now - this.lastBoltSpawn > spawnInterval && this.bolts.size() < maxBolts) {
            this.bolts.add(spawnBolt(this.lastTarget));
            this.lastBoltSpawn = now;
        }

        this.bolts.removeIf(b -> now - b.spawn > b.life);
        if (this.bolts.isEmpty()) return;

        Vec3 basePos;
        if (this.lastTarget != null && this.lastTarget.isAlive()) {
            basePos = new Vec3(
                    Mth.lerp(partialTicks, this.lastTarget.xOld, this.lastTarget.getX()),
                    Mth.lerp(partialTicks, this.lastTarget.yOld, this.lastTarget.getY()),
                    Mth.lerp(partialTicks, this.lastTarget.zOld, this.lastTarget.getZ())
            );
        } else {
            basePos = this.lastTargetPos;
        }
        if (basePos == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        Matrix4f viewMatrix = camera.getViewRotationMatrix(new Matrix4f())
                .translate((float) -cam.x, (float) -cam.y, (float) -cam.z);

        float hurtFactor = 0.0F;
        if (hurtColor && this.lastTarget != null && this.lastTarget.hurtTime > 0) {
            hurtFactor = (float) Math.sin(this.lastTarget.hurtTime * (Math.PI / 10.0D));
        }

        int redColor = ColorUtil.rgba(255, 60, 60, 255);
        int glowColor = hurtFactor > 0.01F ? ColorUtil.interpolateColor(baseColor, redColor, hurtFactor) : baseColor;
        int coreColor = hurtFactor > 0.01F ? ColorUtil.interpolateColor(0xFFFFFFFF, redColor, hurtFactor) : 0xFFFFFFFF;

        var target = mc.gameRenderer.mainRenderTarget();
        if (target == null) return;

        // Build Ribbons (Triangles)
        try (ByteBufferBuilder ribbonMemory = new ByteBufferBuilder(4096 * DefaultVertexFormat.POSITION_COLOR.getVertexSize())) {
            BufferBuilder ribbonBuilder = new BufferBuilder(ribbonMemory, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            int ribbonTriCount = 0;

            for (Bolt bolt : this.bolts) {
                float alpha = boltAlpha(bolt, now) * this.appearValue;
                if (alpha <= 0.02F) continue;

                // Outer Ribbon
                int outerA = (int) (alpha * 60.0F);
                if (outerA > 0) {
                    ribbonTriCount += appendRibbon(ribbonBuilder, viewMatrix, basePos, cam, bolt.points, 0.045F * widthScale, ColorUtil.withAlpha(glowColor, outerA));
                    for (List<Vec3> branch : bolt.branches) {
                        ribbonTriCount += appendRibbon(ribbonBuilder, viewMatrix, basePos, cam, branch, 0.030F * widthScale, ColorUtil.withAlpha(glowColor, (int) (outerA * 0.85F)));
                    }
                }

                // Mid Glow Ribbon
                int glowA = (int) (alpha * 120.0F);
                if (glowA > 0) {
                    ribbonTriCount += appendRibbon(ribbonBuilder, viewMatrix, basePos, cam, bolt.points, 0.018F * widthScale, ColorUtil.withAlpha(glowColor, glowA));
                    for (List<Vec3> branch : bolt.branches) {
                        ribbonTriCount += appendRibbon(ribbonBuilder, viewMatrix, basePos, cam, branch, 0.012F * widthScale, ColorUtil.withAlpha(glowColor, (int) (glowA * 0.8F)));
                    }
                }

                // Core Bright Ribbon
                int coreA = (int) (alpha * 240.0F);
                if (coreA > 0) {
                    ribbonTriCount += appendRibbon(ribbonBuilder, viewMatrix, basePos, cam, bolt.points, 0.007F * widthScale, ColorUtil.withAlpha(coreColor, coreA));
                    for (List<Vec3> branch : bolt.branches) {
                        ribbonTriCount += appendRibbon(ribbonBuilder, viewMatrix, basePos, cam, branch, 0.004F * widthScale, ColorUtil.withAlpha(coreColor, (int) (coreA * 0.85F)));
                    }
                }
            }

            if (ribbonTriCount > 0) {
                try (MeshData mesh = ribbonBuilder.buildOrThrow()) {
                    GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
                            () -> "TargetLightningV2 Triangles",
                            GpuBuffer.USAGE_VERTEX,
                            mesh.vertexBuffer()
                    );
                    try {
                        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                                () -> "TargetLightningV2 Pass",
                                target.getColorTextureView(),
                                Optional.empty(),
                                target.getDepthTextureView(),
                                OptionalDouble.empty()
                        )) {
                            pass.setPipeline(LIGHTNING_PIPELINE);
                            pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                            pass.setVertexBuffer(0, vertexBuffer.slice());
                            pass.draw(mesh.drawState().vertexCount(), 1, 0, 0);
                        }
                    } finally {
                        vertexBuffer.close();
                    }
                }
            }
        }

        // Bloom Node Billboards (Quads)
        AbstractTexture bloomTex = mc.getTextureManager().getTexture(BLOOM_TEX);
        if (bloomTex != null) {
            try (ByteBufferBuilder bloomMemory = new ByteBufferBuilder(2048 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
                BufferBuilder bloomBuilder = new BufferBuilder(bloomMemory, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                int bloomQuadCount = 0;

                for (Bolt bolt : this.bolts) {
                    float alpha = boltAlpha(bolt, now) * this.appearValue;
                    if (alpha <= 0.03F) continue;

                    int nodeA = (int) (alpha * 130.0F);
                    if (nodeA > 0) {
                        int nodeColor = ColorUtil.withAlpha(glowColor, nodeA);
                        for (int i = 0; i < bolt.points.size(); i += 2) {
                            Vec3 p = basePos.add(bolt.points.get(i));
                            bloomQuadCount += addBillboardQuad(bloomBuilder, viewMatrix, p, 0.12F * widthScale, nodeColor);
                        }
                    }
                }

                if (bloomQuadCount > 0) {
                    try (MeshData mesh = bloomBuilder.buildOrThrow()) {
                        GpuBuffer vertexBuffer = RenderSystem.getDevice().createBuffer(
                                () -> "TargetLightningV2 Bloom",
                                GpuBuffer.USAGE_VERTEX,
                                mesh.vertexBuffer()
                        );
                        try {
                            GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                                    () -> "TargetLightningV2 Bloom Pass",
                                    target.getColorTextureView(),
                                    Optional.empty(),
                                    target.getDepthTextureView(),
                                    OptionalDouble.empty()
                            )) {
                                pass.setPipeline(BLOOM_PIPELINE);
                                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                                pass.bindTexture("Sampler0", bloomTex.getTextureView(), sampler);
                                pass.setVertexBuffer(0, vertexBuffer.slice());
                                GpuBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(bloomQuadCount * 6);
                                pass.setIndexBuffer(indices, RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).type());
                                pass.drawIndexed(bloomQuadCount * 6, 1, 0, 0, 0);
                            }
                        } finally {
                            vertexBuffer.close();
                        }
                    }
                }
            }
        }
    }

    private Bolt spawnBolt(LivingEntity target) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float radius = target.getBbWidth() * 0.55F;
        float height = target.getBbHeight();
        double startAngle = random.nextDouble() * Math.PI * 2.0;
        double startY = 0.15 + random.nextDouble() * Math.max(0.2, height - 0.4);
        Vec3 start = new Vec3(Math.cos(startAngle) * radius * 0.7, startY, Math.sin(startAngle) * radius * 0.7);

        double theta = random.nextDouble() * Math.PI * 2.0;
        double phi = Math.toRadians((random.nextDouble() - 0.5) * 150.0);
        Vec3 dir = new Vec3(Math.cos(phi) * Math.cos(theta), Math.sin(phi) * 0.8 + 0.25, Math.cos(phi) * Math.sin(theta)).normalize();
        double length = 0.35 + random.nextDouble() * 0.45;
        Vec3 end = start.add(dir.scale(length));

        ArrayList<Vec3> points = new ArrayList<>(List.of(start, end));
        points = displacePoints(points, 0.09, 3);
        ArrayList<List<Vec3>> branches = new ArrayList<>();
        int branchCount = 1 + random.nextInt(3);

        for (int i = 0; i < branchCount; i++) {
            int anchorIndex = 1 + random.nextInt(Math.max(1, points.size() - 2));
            Vec3 anchor = points.get(anchorIndex);
            Vec3 branchDir = randomPerpendicular(dir);
            if (random.nextBoolean()) branchDir = branchDir.scale(-1.0);

            double branchLength = 0.12 + random.nextDouble() * 0.18;
            Vec3 branchEnd = anchor.add(branchDir.scale(branchLength));
            ArrayList<Vec3> branchPoints = new ArrayList<>(List.of(anchor, branchEnd));
            branches.add(displacePoints(branchPoints, 0.05, 2));
        }

        return new Bolt(points, branches, 140L + random.nextLong(160L), random.nextFloat() * 6.28F);
    }

    private static ArrayList<Vec3> displacePoints(ArrayList<Vec3> input, double amount, int passes) {
        ArrayList<Vec3> points = new ArrayList<>(input);
        double amt = amount;

        for (int pass = 0; pass < passes; pass++) {
            ArrayList<Vec3> next = new ArrayList<>(points.size() * 2);

            for (int i = 0; i < points.size() - 1; i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                next.add(a);
                Vec3 dir = b.subtract(a);
                if (dir.lengthSqr() < 1.0E-7) {
                    next.add(a.add(b).scale(0.5));
                } else {
                    Vec3 perp = randomPerpendicular(dir.normalize());
                    double offset = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2.0 * amt;
                    next.add(a.add(b).scale(0.5).add(perp.scale(offset)));
                }
            }

            next.add(points.get(points.size() - 1));
            points = next;
            amt *= 0.5;
        }

        return points;
    }

    private static Vec3 randomPerpendicular(Vec3 dir) {
        Vec3 arbitrary = Math.abs(dir.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 perp1 = dir.cross(arbitrary).normalize();
        Vec3 perp2 = dir.cross(perp1).normalize();
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        return perp1.scale(Math.cos(angle)).add(perp2.scale(Math.sin(angle)));
    }

    private static int appendRibbon(BufferBuilder buffer, Matrix4f viewMatrix, Vec3 base, Vec3 cam, List<Vec3> points, float width, int color) {
        int a = (color >> 24) & 0xFF;
        if (a <= 0 || points.size() < 2) return 0;

        float half = width * 0.5F;
        int triCount = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = base.add(points.get(i));
            Vec3 p2 = base.add(points.get(i + 1));
            Vec3 segDir = p2.subtract(p1);
            if (segDir.lengthSqr() < 1.0E-8) continue;

            Vec3 viewDir = p1.add(p2).scale(0.5).subtract(cam);
            if (viewDir.lengthSqr() < 1.0E-8) continue;

            Vec3 side = segDir.cross(viewDir);
            double sideLen = side.length();
            if (sideLen < 1.0E-8 || !Double.isFinite(sideLen)) continue;
            side = side.scale(1.0 / sideLen);

            Vec3 offset = side.scale(half);

            Vector4f v0 = viewMatrix.transform(new Vector4f((float) (p1.x - offset.x), (float) (p1.y - offset.y), (float) (p1.z - offset.z), 1.0F));
            Vector4f v1 = viewMatrix.transform(new Vector4f((float) (p1.x + offset.x), (float) (p1.y + offset.y), (float) (p1.z + offset.z), 1.0F));
            Vector4f v2 = viewMatrix.transform(new Vector4f((float) (p2.x + offset.x), (float) (p2.y + offset.y), (float) (p2.z + offset.z), 1.0F));
            Vector4f v3 = viewMatrix.transform(new Vector4f((float) (p2.x - offset.x), (float) (p2.y - offset.y), (float) (p2.z - offset.z), 1.0F));

            if (v0.z >= -0.05F && v1.z >= -0.05F) continue;

            // Two triangles for quad
            buffer.addVertex(v0.x, v0.y, v0.z).setColor(color);
            buffer.addVertex(v1.x, v1.y, v1.z).setColor(color);
            buffer.addVertex(v2.x, v2.y, v2.z).setColor(color);

            buffer.addVertex(v0.x, v0.y, v0.z).setColor(color);
            buffer.addVertex(v2.x, v2.y, v2.z).setColor(color);
            buffer.addVertex(v3.x, v3.y, v3.z).setColor(color);

            triCount += 2;
        }

        return triCount;
    }

    private static int addBillboardQuad(BufferBuilder buffer, Matrix4f viewMatrix, Vec3 pos, float halfSize, int color) {
        Vector4f cv = viewMatrix.transform(new Vector4f((float) pos.x, (float) pos.y, (float) pos.z, 1.0F));
        if (cv.z >= -0.05F) return 0;

        buffer.addVertex(cv.x - halfSize, cv.y - halfSize, cv.z).setUv(0.0F, 1.0F).setColor(color);
        buffer.addVertex(cv.x + halfSize, cv.y - halfSize, cv.z).setUv(1.0F, 1.0F).setColor(color);
        buffer.addVertex(cv.x + halfSize, cv.y + halfSize, cv.z).setUv(1.0F, 0.0F).setColor(color);
        buffer.addVertex(cv.x - halfSize, cv.y + halfSize, cv.z).setUv(0.0F, 0.0F).setColor(color);
        return 1;
    }

    private float boltAlpha(Bolt bolt, long now) {
        float life = (float) (now - bolt.spawn) / (float) bolt.life;
        if (life < 0.0F || life > 1.0F) return 0.0F;
        float fade = (float) Math.sin(life * Math.PI);
        float flicker = 0.8F + 0.2F * (float) Math.sin((double) (life * 32.0F + bolt.flicker));
        return Math.max(0.0F, Math.min(1.0F, fade * flicker));
    }
}
