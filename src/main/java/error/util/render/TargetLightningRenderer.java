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
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Random;

public final class TargetLightningRenderer {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final int MAX_STRANDS = 16;
    private static final int SEGMENTS_PER_STRAND = 20;
    private static final int WHITE = 0xFFFFFFFF;

    private static final RenderPipeline LIGHTNING_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/target_lightning"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private final Animation appear = new Animation(0.0F, 0.25F);
    private final List<LineSegment> lineList = new ArrayList<>();
    private final Random random = new Random();

    private final Strand[] strands = new Strand[MAX_STRANDS];
    private boolean strandsInitialized = false;

    private LivingEntity lastTarget;
    private Vec3 lastTargetPos;
    private float lastTargetWidth = 0.6F;
    private float lastTargetHeight = 1.8F;
    private boolean animationTarget;
    private GpuBuffer vertexBuffer;

    public TargetLightningRenderer() {
        initStrands();
    }

    private void initStrands() {
        if (!strandsInitialized) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < MAX_STRANDS; i++) {
                strands[i] = new Strand();
                respawnStrand(strands[i], now, 1.0F);
                strands[i].spawnTime = now - (long) (random.nextDouble() * strands[i].lifetimeMs);
            }
            strandsInitialized = true;
        }
    }

    public void render(LivingEntity activeTarget, float alphaProgress, float tickDelta,
                       float lengthFactor, float thickness, float radius, float speed,
                       boolean enableGlow, boolean colorOnHit, int hitColor,
                       String colorMode, int customColor) {
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            reset();
            return;
        }

        initStrands();

        long nowMillis = System.currentTimeMillis();

        boolean present = valid(activeTarget);
        if (present) {
            this.lastTarget = activeTarget;
            this.lastTargetPos = Render3DUtil.interpolatedPosition(activeTarget, tickDelta);
            this.lastTargetWidth = activeTarget.getBbWidth();
            this.lastTargetHeight = activeTarget.getBbHeight();
        }

        if (dead(this.lastTarget)) {
            present = false;
        }
        updateAnimations(present);

        this.appear.update();
        float appearValue = this.appear.getValue();
        float totalAlpha = appearValue * alphaProgress;

        if (this.lastTarget == null || totalAlpha <= 0.005F) {
            if (!present && appearValue <= 0.01F) {
                this.lastTarget = null;
                this.lineList.clear();
            }
            return;
        }

        LivingEntity renderTarget = present ? activeTarget : this.lastTarget;
        Vec3 basePos;
        float width;
        float height;
        if (renderTarget != null && renderTarget.isAlive()) {
            basePos = Render3DUtil.interpolatedPosition(renderTarget, tickDelta);
            width = renderTarget.getBbWidth();
            height = renderTarget.getBbHeight();
            this.lastTargetPos = basePos;
            this.lastTargetWidth = width;
            this.lastTargetHeight = height;
        } else if (this.lastTargetPos != null) {
            basePos = this.lastTargetPos;
            width = this.lastTargetWidth;
            height = this.lastTargetHeight;
        } else {
            return;
        }

        float hurtFactor = 0.0F;
        if (colorOnHit && renderTarget != null && renderTarget.hurtTime > 0) {
            hurtFactor = (float) Math.sin(renderTarget.hurtTime * (Math.PI / 10.0D));
        }

        float spd = Math.max(0.1F, speed);
        double timeSec = (nowMillis % 100000000L) * 0.001D * spd;
        double baseRadius = (width / 0.6D) * Math.max(0.05F, radius);

        this.lineList.clear();

        int activeStrands = Mth.clamp((int) (6 * Math.max(0.4F, lengthFactor)), 3, MAX_STRANDS);
        long electricStep = nowMillis / 40L;

        for (int i = 0; i < activeStrands; i++) {
            Strand strand = this.strands[i];
            if (strand == null) {
                strand = new Strand();
                this.strands[i] = strand;
                respawnStrand(strand, nowMillis, spd);
            }

            if (nowMillis - strand.spawnTime >= strand.lifetimeMs) {
                respawnStrand(strand, nowMillis, spd);
            }

            float life = (float) (nowMillis - strand.spawnTime) / (float) strand.lifetimeMs;
            life = Mth.clamp(life, 0.0F, 1.0F);

            float lifeFade;
            if (life < 0.15F) {
                lifeFade = life / 0.15F;
            } else if (life > 0.75F) {
                lifeFade = (1.0F - life) / 0.25F;
            } else {
                lifeFade = 1.0F;
            }
            lifeFade = Mth.clamp(lifeFade, 0.0F, 1.0F);

            float flicker = 0.90F + 0.10F * (float) Math.sin((nowMillis * 0.024D) + i * 4.3D);
            float strandAlpha = totalAlpha * lifeFade * flicker;
            if (strandAlpha <= 0.01F) {
                continue;
            }

            double elapsedSec = (nowMillis - strand.spawnTime) / 1000.0D;
            double curOrbit = strand.baseAngle + elapsedSec * strand.rotSpeed * spd;

            Vec3[] points = new Vec3[SEGMENTS_PER_STRAND + 1];
            int[] segColors = new int[SEGMENTS_PER_STRAND + 1];

            for (int seg = 0; seg <= SEGMENTS_PER_STRAND; seg++) {
                double t = (double) seg / SEGMENTS_PER_STRAND;
                double angle = curOrbit + strand.angleSpan * t;
                double yNorm = strand.yStartNorm + (strand.yEndNorm - strand.yStartNorm) * t;
                double y = yNorm * height;

                double env = Math.sin(Math.PI * t);
                double envTaper = Math.pow(Math.max(0.0D, env), 0.6D);
                double r = baseRadius * strand.radiusMult * (1.0D + 0.14D * env);

                double waveX = Math.sin(t * 18.0D + timeSec * 12.0D + i * 2.5D) * 0.05D;
                double waveY = Math.cos(t * 16.0D - timeSec * 9.0D + i * 1.8D) * 0.04D;
                double waveZ = Math.cos(t * 18.0D + timeSec * 12.0D + i * 2.5D) * 0.05D;

                double randSeed = Math.sin(seg * 17.3D + i * 43.1D + electricStep * 73.9D);
                double crackleX = Math.sin(randSeed * 43758.5453D) * 0.05D;
                double crackleY = Math.cos(randSeed * 22578.1459D) * 0.03D;
                double crackleZ = Math.sin(randSeed * 63145.8912D) * 0.05D;

                int jitterIdx = Mth.clamp((int) (t * (strand.jitter.length - 1)), 0, strand.jitter.length - 1);
                double jx = (strand.jitter[jitterIdx].x * 0.6D + waveX + crackleX) * envTaper;
                double jy = (strand.jitter[jitterIdx].y * 0.6D + waveY + crackleY) * envTaper;
                double jz = (strand.jitter[jitterIdx].z * 0.6D + waveZ + crackleZ) * envTaper;

                points[seg] = basePos.add(Math.cos(angle) * r + jx, y + jy, Math.sin(angle) * r + jz);

                int strandHueIndex = (int) (i * 40 + seg * 5 + Math.toDegrees(curOrbit) + timeSec * 120.0D);
                int segColor = resolveColor(colorMode, customColor, strandHueIndex);
                if (hurtFactor > 0.01F) {
                    segColor = ColorUtil.interpolateColor(segColor, hitColor, hurtFactor);
                }
                segColors[seg] = segColor;
            }

            float outerHalfThick = (enableGlow ? 0.030F : 0.018F) * thickness;
            float midHalfThick = 0.015F * thickness;
            float coreHalfThick = 0.006F * thickness;

            for (int seg = 0; seg < SEGMENTS_PER_STRAND; seg++) {
                Vec3 p1 = points[seg];
                Vec3 p2 = points[seg + 1];

                double segT = (seg + 0.5D) / SEGMENTS_PER_STRAND;
                double segEnv = Math.pow(Math.max(0.0D, Math.sin(Math.PI * segT)), 0.5D);
                float segAlpha = strandAlpha * (float) (0.35D + 0.65D * segEnv);
                if (segAlpha <= 0.005F) continue;

                int brightCol1 = segColors[seg];
                int brightCol2 = segColors[seg + 1];

                int midCol1 = ColorUtil.withAlpha(brightCol1, (int) (250 * segAlpha));
                int midCol2 = ColorUtil.withAlpha(brightCol2, (int) (250 * segAlpha));
                int core1 = ColorUtil.withAlpha(WHITE, (int) (255 * segAlpha));
                int core2 = ColorUtil.withAlpha(WHITE, (int) (255 * segAlpha));

                if (enableGlow) {
                    int softCol1 = ColorUtil.withAlpha(segColors[seg], (int) (90 * segAlpha));
                    int softCol2 = ColorUtil.withAlpha(segColors[seg + 1], (int) (90 * segAlpha));
                    this.lineList.add(new LineSegment(p1, p2, softCol1, softCol2, outerHalfThick));
                }

                this.lineList.add(new LineSegment(p1, p2, midCol1, midCol2, midHalfThick));
                this.lineList.add(new LineSegment(p1, p2, core1, core2, coreHalfThick));
            }
        }

        if (this.lineList.isEmpty()) return;

        var target = mc.gameRenderer.mainRenderTarget();
        if (target == null || target.getColorTextureView() == null || target.getDepthTextureView() == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cameraPos = camera.position();
        Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);

        BufferBuilder builder = new BufferBuilder(
                new ByteBufferBuilder(Math.max(this.lineList.size() * 6 * DefaultVertexFormat.POSITION_COLOR.getVertexSize(), 2048)),
                PrimitiveTopology.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (LineSegment line : this.lineList) {
            drawBillboardTriangles(builder, viewPose, cameraPos,
                    (float) line.start.x, (float) line.start.y, (float) line.start.z,
                    (float) line.end.x, (float) line.end.y, (float) line.end.z,
                    line.halfThickness, line.color1, line.color2);
        }

        MeshData meshData = builder.buildOrThrow();
        var device = RenderSystem.getDevice();
        try {
            ByteBuffer vertexData = meshData.vertexBuffer();
            int remainingBytes = vertexData.remaining();
            int vertexCount = meshData.drawState().vertexCount();
            if (vertexCount == 0) return;

            ensureVertexCapacity(remainingBytes);

            try (RenderPass pass = device.createCommandEncoder().createRenderPass(
                    () -> "Error Target Lightning Pass",
                    target.getColorTextureView(),
                    Optional.empty(),
                    target.getDepthTextureView(),
                    OptionalDouble.empty()
            )) {
                pass.setPipeline(LIGHTNING_PIPELINE);
                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                pass.setVertexBuffer(0, this.vertexBuffer.slice(0, remainingBytes));
                pass.draw(vertexCount, 1, 0, 0);
            }
        } finally {
            meshData.close();
        }
    }

    private void drawBillboardTriangles(BufferBuilder builder, Matrix4f viewPose, Vec3 cameraPos,
                                         float x1, float y1, float z1,
                                         float x2, float y2, float z2,
                                         float halfThickness, int color1, int color2) {
        if (((color1 >> 24) & 0xFF) <= 0 && ((color2 >> 24) & 0xFF) <= 0) {
            return;
        }

        float ax = (float) (x1 - cameraPos.x);
        float ay = (float) (y1 - cameraPos.y);
        float az = (float) (z1 - cameraPos.z);

        float bx = (float) (x2 - cameraPos.x);
        float by = (float) (y2 - cameraPos.y);
        float bz = (float) (z2 - cameraPos.z);

        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;

        float midX = (ax + bx) * 0.5F;
        float midY = (ay + by) * 0.5F;
        float midZ = (az + bz) * 0.5F;

        float nx = dy * midZ - dz * midY;
        float ny = dz * midX - dx * midZ;
        float nz = dx * midY - dy * midX;

        float nLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nLen < 1.0E-6F) {
            nx = 0.0F;
            ny = halfThickness;
            nz = 0.0F;
        } else {
            float inv = halfThickness / nLen;
            nx *= inv;
            ny *= inv;
            nz *= inv;
        }

        Vector4f v0 = Render3DUtil.toViewSpace(new Vec3(x1 - nx, y1 - ny, z1 - nz), cameraPos, viewPose);
        Vector4f v1 = Render3DUtil.toViewSpace(new Vec3(x1 + nx, y1 + ny, z1 + nz), cameraPos, viewPose);
        Vector4f v2 = Render3DUtil.toViewSpace(new Vec3(x2 + nx, y2 + ny, z2 + nz), cameraPos, viewPose);
        Vector4f v3 = Render3DUtil.toViewSpace(new Vec3(x2 - nx, y2 - ny, z2 - nz), cameraPos, viewPose);

        builder.addVertex(v0.x, v0.y, v0.z).setColor(color1);
        builder.addVertex(v1.x, v1.y, v1.z).setColor(color1);
        builder.addVertex(v2.x, v2.y, v2.z).setColor(color2);

        builder.addVertex(v0.x, v0.y, v0.z).setColor(color1);
        builder.addVertex(v2.x, v2.y, v2.z).setColor(color2);
        builder.addVertex(v3.x, v3.y, v3.z).setColor(color2);
    }

    private void respawnStrand(Strand strand, long now, float speed) {
        strand.spawnTime = now;
        long baseLifetime = (long) (340L / Math.max(0.2F, speed));
        strand.lifetimeMs = baseLifetime + random.nextInt((int) Math.max(40L, (long) (200L / Math.max(0.2F, speed))));

        strand.baseAngle = random.nextDouble() * Math.PI * 2.0D;
        double spanDeg = 65.0D + random.nextDouble() * 85.0D;
        strand.angleSpan = Math.toRadians(random.nextBoolean() ? spanDeg : -spanDeg);

        strand.yStartNorm = 0.06D + random.nextDouble() * 0.76D;
        double heightDelta = (0.22D + random.nextDouble() * 0.46D) * (random.nextBoolean() ? 1.0D : -1.0D);
        strand.yEndNorm = Mth.clamp(strand.yStartNorm + heightDelta, 0.05D, 0.95D);

        strand.radiusMult = 0.94D + random.nextDouble() * 0.26D;
        strand.rotSpeed = (0.35D + random.nextDouble() * 0.50D) * (random.nextBoolean() ? 1.0D : -1.0D);

        double jitterAmp = 0.065D + random.nextDouble() * 0.04D;
        for (int i = 0; i < strand.jitter.length; i++) {
            strand.jitter[i] = new Vec3(
                    (random.nextDouble() * 2.0D - 1.0D) * jitterAmp,
                    (random.nextDouble() * 2.0D - 1.0D) * jitterAmp * 0.5D,
                    (random.nextDouble() * 2.0D - 1.0D) * jitterAmp
            );
        }
    }

    public void reset() {
        this.lastTarget = null;
        this.lastTargetPos = null;
        this.animationTarget = false;
        this.lineList.clear();
        this.appear.setTarget(0.0F);
    }

    private void updateAnimations(boolean present) {
        if (present == this.animationTarget) {
            return;
        }
        this.animationTarget = present;
        this.appear.setTarget(present ? 1.0F : 0.0F);
    }

    private int resolveColor(String mode, int customColor, int index) {
        if ("Кастом".equalsIgnoreCase(mode) || "Кастомный".equalsIgnoreCase(mode)) {
            int c1 = customColor;
            int c2 = ColorUtil.withAlpha(customColor, 180);
            float f = (float) ((System.currentTimeMillis() + index * 50L) % 2000L) / 2000.0F;
            if (f > 0.5F) f = 1.0F - f;
            return ColorUtil.interpolateColor(c1, c2, f * 2.0F);
        }
        int c1 = Theme.getAccentColor();
        int c2 = Theme.getSecondaryColor();
        float f = (float) ((System.currentTimeMillis() + index * 50L) % 2000L) / 2000.0F;
        if (f > 0.5F) f = 1.0F - f;
        return ColorUtil.interpolateColor(c1, c2, f * 2.0F);
    }

    private static boolean valid(LivingEntity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    private static boolean dead(LivingEntity entity) {
        return entity != null && (entity.isDeadOrDying() || entity.deathTime > 0 || entity.getHealth() <= 0.0F);
    }

    private void ensureVertexCapacity(int byteSize) {
        if (this.vertexBuffer != null && this.vertexBuffer.size() >= byteSize) return;
        if (this.vertexBuffer != null) this.vertexBuffer.close();
        this.vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Error Target Lightning Vertices",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                Math.max(byteSize + 1024, 64 * 1024)
        );
    }

    private static final class Strand {
        long spawnTime;
        long lifetimeMs;
        double baseAngle;
        double angleSpan;
        double yStartNorm;
        double yEndNorm;
        double radiusMult;
        double rotSpeed;
        final Vec3[] jitter = new Vec3[SEGMENTS_PER_STRAND + 1];
    }

    private record LineSegment(Vec3 start, Vec3 end, int color1, int color2, float halfThickness) {}
}
