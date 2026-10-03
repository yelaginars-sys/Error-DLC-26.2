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
import java.util.Random;

/**
 */
public final class FireflyRenderer {
    private static final float BASE_FLY_SPEED = 2.2f;
    private static final float SPAWN_RADIUS = 30.0f;
    private static final int TRAIL_LENGTH = 24;

    private static final RenderPipeline ADDITIVE_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.parse("error:pipeline/world/firefly_laser_glow"))
            .withVertexShader(Identifier.parse("error:core/lines"))
            .withFragmentShader(Identifier.parse("error:core/lines"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private static class GlowSpark {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        final long spawnTime = System.currentTimeMillis();
        final long maxAge;
        final float baseSize;
        final int color;

        GlowSpark(double x, double y, double z, double vx, double vy, double vz, float baseSize, int color, long maxAge) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.baseSize = baseSize;
            this.color = color;
            this.maxAge = maxAge;
        }

        void update(float dt) {
            prevX = x; prevY = y; prevZ = z;
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;

            vy += 0.05 * dt;
            double damping = Math.pow(0.5, dt);
            vx *= damping;
            vy *= damping;
            vz *= damping;
        }

        boolean isDead() {
            return System.currentTimeMillis() - spawnTime >= maxAge;
        }

        float getProgress() {
            long age = System.currentTimeMillis() - spawnTime;
            return Mth.clamp((float) age / maxAge, 0f, 1f);
        }

        float getAlpha() {
            float p = getProgress();
            if (p <= 0f || p >= 1f) return 0f;
            float fadeIn = Mth.clamp(p / 0.15f, 0f, 1f);
            float fadeOut = 1.0f - p;
            return fadeIn * fadeOut * fadeOut;
        }

        float getCurrentSize() {
            float p = getProgress();
            return baseSize * (float) Math.sin(p * Math.PI);
        }

        double interpX(float td) { return Mth.lerp(td, prevX, x); }
        double interpY(float td) { return Mth.lerp(td, prevY, y); }
        double interpZ(float td) { return Mth.lerp(td, prevZ, z); }
    }

    private static class FireFlyEntity {
        double x, y, z, prevX, prevY, prevZ;
        double velX, velY, velZ;
        double wanderDirX, wanderDirY, wanderDirZ;
        final int baseColor;
        final int idIndex;
        final long spawnTime = System.currentTimeMillis();
        final long maxLifeTime;
        long lastDirectionChange = 0;

        final double[] trailX = new double[TRAIL_LENGTH];
        final double[] trailY = new double[TRAIL_LENGTH];
        final double[] trailZ = new double[TRAIL_LENGTH];
        int trailHead = 0;
        int trailCount = 0;
        float trailTimer = 0f;
        float sparkTimer = 0f;

        boolean isDying = false;
        long deathStartTime = 0;
        final Random rng = new Random();

        FireFlyEntity(double x, double y, double z, int baseColor, int idIndex) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            this.baseColor = baseColor;
            this.idIndex = idIndex;
            this.maxLifeTime = 14000 + rng.nextInt(6000);

            pickNewDirection();
            this.velX = wanderDirX * BASE_FLY_SPEED;
            this.velY = wanderDirY * BASE_FLY_SPEED;
            this.velZ = wanderDirZ * BASE_FLY_SPEED;

            for (int i = 0; i < TRAIL_LENGTH; i++) {
                trailX[i] = x;
                trailY[i] = y;
                trailZ[i] = z;
            }
        }

        private void pickNewDirection() {
            double angle = Math.toRadians(rng.nextDouble() * 360.0);
            double pitch = Math.toRadians((rng.nextDouble() - 0.5) * 40.0);
            wanderDirX = -Math.sin(angle) * Math.cos(pitch);
            wanderDirY = Math.sin(pitch) * 0.4;
            wanderDirZ = Math.cos(angle) * Math.cos(pitch);
            lastDirectionChange = System.currentTimeMillis();
        }

        void startDying() {
            if (!isDying) {
                isDying = true;
                deathStartTime = System.currentTimeMillis();
            }
        }

        void update(float dt, Vec3 playerPos, List<GlowSpark> sparks, int color) {
            prevX = x; prevY = y; prevZ = z;
            long now = System.currentTimeMillis();

            double dx = playerPos.x - x;
            double dy = (playerPos.y + 1.2) - y;
            double dz = playerPos.z - z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (!isDying) {
                if (distSq > (SPAWN_RADIUS + 25) * (SPAWN_RADIUS + 25) || (now - spawnTime > maxLifeTime)) {
                    startDying();
                }
            }

            if (now - lastDirectionChange > 2000 + rng.nextInt(2500)) {
                pickNewDirection();
            }

            double targetVx = wanderDirX * BASE_FLY_SPEED;
            double targetVy = wanderDirY * BASE_FLY_SPEED;
            double targetVz = wanderDirZ * BASE_FLY_SPEED;

            double dist = Math.sqrt(distSq);
            if (dist > SPAWN_RADIUS && dist > 0.001) {
                double steerWeight = Mth.clamp((float)((dist - SPAWN_RADIUS) / 10.0), 0f, 1f);
                double toPlayerX = (dx / dist) * BASE_FLY_SPEED;
                double toPlayerY = (dy / dist) * BASE_FLY_SPEED * 0.5;
                double toPlayerZ = (dz / dist) * BASE_FLY_SPEED;

                targetVx = Mth.lerp(steerWeight, targetVx, toPlayerX);
                targetVy = Mth.lerp(steerWeight, targetVy, toPlayerY);
                targetVz = Mth.lerp(steerWeight, targetVz, toPlayerZ);
            }

            double wave = Math.sin((now + idIndex * 150) * 0.003) * 0.4;
            targetVy += wave;

            double smoothF = 1.0 - Math.exp(-2.8 * dt);
            velX += (targetVx - velX) * smoothF;
            velY += (targetVy - velY) * smoothF;
            velZ += (targetVz - velZ) * smoothF;

            double currentSpeed = Math.sqrt(velX * velX + velY * velY + velZ * velZ);
            double maxAllowedSpeed = BASE_FLY_SPEED * 1.6;
            if (currentSpeed > maxAllowedSpeed) {
                double scale = maxAllowedSpeed / currentSpeed;
                velX *= scale;
                velY *= scale;
                velZ *= scale;
            }

            x += velX * dt;
            y += velY * dt;
            z += velZ * dt;

            trailTimer += dt;
            if (trailTimer >= 0.035f) {
                trailTimer = 0f;
                trailX[trailHead] = x;
                trailY[trailHead] = y;
                trailZ[trailHead] = z;
                trailHead = (trailHead + 1) % TRAIL_LENGTH;
                if (trailCount < TRAIL_LENGTH) trailCount++;
            }

            sparkTimer += dt;
            if (!isDying && trailCount > 2 && sparkTimer >= 0.12f) {
                sparkTimer = 0f;
                int i1 = rng.nextInt(trailCount);
                int i2 = (i1 + 1) % trailCount;

                double lerpFactor = rng.nextDouble();
                double tx = Mth.lerp(lerpFactor, getTrailX(i1), getTrailX(i2));
                double ty = Mth.lerp(lerpFactor, getTrailY(i1), getTrailY(i2));
                double tz = Mth.lerp(lerpFactor, getTrailZ(i1), getTrailZ(i2));

                double angle = rng.nextDouble() * Math.PI * 2;
                double pitch = (rng.nextDouble() - 0.5) * Math.PI;
                double sparkSpeed = 0.2 + rng.nextDouble() * 0.3;

                double svx = Math.cos(angle) * Math.cos(pitch) * sparkSpeed;
                double svy = Math.sin(pitch) * sparkSpeed;
                double svz = Math.sin(angle) * Math.cos(pitch) * sparkSpeed;

                float sparkSize = 0.10f + rng.nextFloat() * 0.08f;
                long life = 500 + rng.nextInt(500);

                sparks.add(new GlowSpark(tx, ty, tz, svx, svy, svz, sparkSize, color, life));
            }
        }

        boolean isFullyDead() {
            return isDying && (System.currentTimeMillis() - deathStartTime >= 1000);
        }

        double interpX(float td) { return Mth.lerp(td, prevX, x); }
        double interpY(float td) { return Mth.lerp(td, prevY, y); }
        double interpZ(float td) { return Mth.lerp(td, prevZ, z); }

        float getAlpha() {
            long now = System.currentTimeMillis();
            if (isDying) {
                float fade = 1.0f - ((float) (now - deathStartTime) / 1000f);
                return Mth.clamp(fade, 0f, 1f);
            }
            long age = now - spawnTime;
            if (age < 800) return age / 800f;
            return 1.0f;
        }

        float pulseAlpha() {
            return (float) (0.90 + 0.10 * Math.sin((System.currentTimeMillis() - spawnTime) / 160.0));
        }

        double getTrailX(int i) { return trailX[(trailHead - 1 - i + TRAIL_LENGTH) % TRAIL_LENGTH]; }
        double getTrailY(int i) { return trailY[(trailHead - 1 - i + TRAIL_LENGTH) % TRAIL_LENGTH]; }
        double getTrailZ(int i) { return trailZ[(trailHead - 1 - i + TRAIL_LENGTH) % TRAIL_LENGTH]; }
    }

    private final List<FireFlyEntity> flies = new ArrayList<>();
    private final List<GlowSpark> sparks = new ArrayList<>();
    private final Random random = new Random();
    private GpuBuffer vertexBuffer;
    private long lastFrameTime = 0;

    public void render(WorldParticles module) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            release();
            return;
        }

        long now = System.nanoTime();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = (float) ((now - lastFrameTime) / 1_000_000_000.0);
        lastFrameTime = now;
        dt = Mth.clamp(dt, 0.001f, 0.05f);

        Vec3 playerPos = mc.player.position();
        int themeCol = Theme.getAccentColor();

        for (FireFlyEntity f : flies) {
            f.update(dt, playerPos, sparks, themeCol);
        }
        flies.removeIf(FireFlyEntity::isFullyDead);

        for (GlowSpark s : sparks) s.update(dt);
        sparks.removeIf(GlowSpark::isDead);

        int target = module.fireflyCount.getValue().intValue();
        int activeCount = 0;
        for (FireFlyEntity f : flies) if (!f.isDying) activeCount++;

        if (activeCount > target) {
            for (FireFlyEntity f : flies) {
                if (!f.isDying) {
                    f.startDying();
                    activeCount--;
                    if (activeCount <= target) break;
                }
            }
        } else {
            while (activeCount < target) {
                spawnFly(playerPos, themeCol);
                activeCount++;
            }
        }

        var targetRT = mc.gameRenderer.mainRenderTarget();
        if (targetRT == null || targetRT.getColorTextureView() == null || targetRT.getDepthTextureView() == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        Matrix4f viewPose = Render3DUtil.cameraViewPose(camera);
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        BufferBuilder builder = new BufferBuilder(
                new ByteBufferBuilder(Math.max((flies.size() * (TRAIL_LENGTH * 12 + 100) + sparks.size() * 36) * DefaultVertexFormat.POSITION_COLOR.getVertexSize(), 8192)),
                PrimitiveTopology.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR
        );

        int drawnVertices = 0;

        for (FireFlyEntity fly : flies) {
            float alpha = fly.getAlpha();
            if (alpha <= 0.001f) continue;

            int segCount = fly.trailCount;
            if (segCount >= 2) {
                double currX = fly.interpX(tickDelta);
                double currY = fly.interpY(tickDelta);
                double currZ = fly.interpZ(tickDelta);

                for (int i = 0; i < segCount - 1; i++) {
                    double nextX = fly.getTrailX(i);
                    double nextY = fly.getTrailY(i);
                    double nextZ = fly.getTrailZ(i);

                    float t1 = (float) i / segCount;
                    float t2 = (float) (i + 1) / segCount;

                    float a1 = (i == 0) ? 0.95f * alpha : (float) Math.pow(1.0f - t1, 2.0) * alpha * 0.95f;
                    float a2 = (float) Math.pow(1.0f - t2, 2.0) * alpha * 0.95f;

                    double dirX = nextX - currX, dirY = nextY - currY, dirZ = nextZ - currZ;
                    double lenSq = dirX * dirX + dirY * dirY + dirZ * dirZ;
                    if (lenSq < 0.00001) continue;

                    double invLen = 1.0 / Math.sqrt(lenSq);
                    dirX *= invLen; dirY *= invLen; dirZ *= invLen;

                    double camDirX = currX - camPos.x, camDirY = currY - camPos.y, camDirZ = currZ - camPos.z;
                    double camLenInv = 1.0 / Math.sqrt(camDirX * camDirX + camDirY * camDirY + camDirZ * camDirZ);
                    camDirX *= camLenInv; camDirY *= camLenInv; camDirZ *= camLenInv;

                    double sideX = dirY * camDirZ - dirZ * camDirY;
                    double sideY = dirZ * camDirX - dirX * camDirZ;
                    double sideZ = dirX * camDirY - dirY * camDirX;

                    float glowW1 = 0.022f * (1.0f - t1 * 0.7f);
                    float glowW2 = 0.022f * (1.0f - t2 * 0.7f);
                    int glowC1 = ColorUtil.multiplyAlpha(themeCol, a1 * 0.45f);
                    int glowC2 = ColorUtil.multiplyAlpha(themeCol, a2 * 0.45f);

                    Vector4f g1 = Render3DUtil.toViewSpace(new Vec3(currX + sideX * glowW1, currY + sideY * glowW1, currZ + sideZ * glowW1), camPos, viewPose);
                    Vector4f g2 = Render3DUtil.toViewSpace(new Vec3(currX - sideX * glowW1, currY - sideY * glowW1, currZ - sideZ * glowW1), camPos, viewPose);
                    Vector4f g3 = Render3DUtil.toViewSpace(new Vec3(nextX - sideX * glowW2, nextY - sideY * glowW2, nextZ - sideZ * glowW2), camPos, viewPose);
                    Vector4f g4 = Render3DUtil.toViewSpace(new Vec3(nextX + sideX * glowW2, nextY + sideY * glowW2, nextZ + sideZ * glowW2), camPos, viewPose);

                    addQuad(builder, g1, g2, g3, g4, glowC1, glowC2);
                    drawnVertices += 6;

                    float coreW1 = 0.006f * (1.0f - t1 * 0.8f);
                    float coreW2 = 0.006f * (1.0f - t2 * 0.8f);
                    int coreC1 = ColorUtil.multiplyAlpha(0xFFFFFFFF, a1 * 0.95f);
                    int coreC2 = ColorUtil.multiplyAlpha(0xFFFFFFFF, a2 * 0.95f);

                    Vector4f c1 = Render3DUtil.toViewSpace(new Vec3(currX + sideX * coreW1, currY + sideY * coreW1, currZ + sideZ * coreW1), camPos, viewPose);
                    Vector4f c2 = Render3DUtil.toViewSpace(new Vec3(currX - sideX * coreW1, currY - sideY * coreW1, currZ - sideZ * coreW1), camPos, viewPose);
                    Vector4f c3 = Render3DUtil.toViewSpace(new Vec3(nextX - sideX * coreW2, nextY - sideY * coreW2, nextZ - sideZ * coreW2), camPos, viewPose);
                    Vector4f c4 = Render3DUtil.toViewSpace(new Vec3(nextX + sideX * coreW2, nextY + sideY * coreW2, nextZ + sideZ * coreW2), camPos, viewPose);

                    addQuad(builder, c1, c2, c3, c4, coreC1, coreC2);
                    drawnVertices += 6;

                    currX = nextX; currY = nextY; currZ = nextZ;
                }
            }
        }

        for (GlowSpark s : sparks) {
            float alpha = s.getAlpha();
            if (alpha <= 0.001f) continue;
            float currentSize = s.getCurrentSize();
            if (currentSize <= 0.0001f) continue;

            Vec3 sPos = new Vec3(s.interpX(tickDelta), s.interpY(tickDelta), s.interpZ(tickDelta));

            drawnVertices += addSoftDisc(builder, sPos, currentSize * 2.2f, ColorUtil.multiplyAlpha(themeCol, alpha * 0.35f), ColorUtil.multiplyAlpha(themeCol, 0.0f), camPos, viewPose);
            drawnVertices += addSoftDisc(builder, sPos, currentSize * 0.8f, ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha * 0.90f), ColorUtil.multiplyAlpha(themeCol, 0.0f), camPos, viewPose);
        }

        for (FireFlyEntity fly : flies) {
            float alpha = fly.getAlpha();
            if (alpha <= 0.001f) continue;

            float p = fly.pulseAlpha() * alpha;
            Vec3 fPos = new Vec3(fly.interpX(tickDelta), fly.interpY(tickDelta), fly.interpZ(tickDelta));

            drawnVertices += addSoftDisc(builder, fPos, 0.50f, ColorUtil.multiplyAlpha(themeCol, p * 0.22f), ColorUtil.multiplyAlpha(themeCol, 0.0f), camPos, viewPose);
            drawnVertices += addSoftDisc(builder, fPos, 0.22f, ColorUtil.multiplyAlpha(themeCol, p * 0.65f), ColorUtil.multiplyAlpha(themeCol, 0.0f), camPos, viewPose);
            drawnVertices += addSoftDisc(builder, fPos, 0.08f, ColorUtil.multiplyAlpha(0xFFFFFFFF, p * 0.98f), ColorUtil.multiplyAlpha(0xFFFFFFFF, 0.0f), camPos, viewPose);
        }

        if (drawnVertices == 0) return;

        MeshData meshData = builder.buildOrThrow();
        var device = RenderSystem.getDevice();
        try {
            ByteBuffer vertexData = meshData.vertexBuffer();
            int remainingBytes = vertexData.remaining();
            ensureVertexCapacity(remainingBytes);

            try (RenderPass pass = device.createCommandEncoder().createRenderPass(
                    () -> "Error Firefly Laser & Bloom",
                    targetRT.getColorTextureView(),
                    java.util.Optional.empty(),
                    targetRT.getDepthTextureView(),
                    java.util.OptionalDouble.empty()
            )) {
                pass.setPipeline(ADDITIVE_PIPELINE);
                pass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                pass.setVertexBuffer(0, this.vertexBuffer.slice(0, remainingBytes));
                pass.draw(drawnVertices, 1, 0, 0);
            }
        } finally {
            meshData.close();
        }
    }

    private static int addSoftDisc(BufferBuilder b, Vec3 worldPos, float radius, int innerColor, int outerColor, Vec3 camPos, Matrix4f viewPose) {
        Vector4f center = Render3DUtil.toViewSpace(worldPos, camPos, viewPose);
        int segments = 12;

        for (int i = 0; i < segments; i++) {
            double a1 = i * (Math.PI * 2.0 / segments);
            double a2 = (i + 1) * (Math.PI * 2.0 / segments);

            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;

            b.addVertex(center.x, center.y, center.z).setColor(innerColor);
            b.addVertex(center.x + x1, center.y + y1, center.z).setColor(outerColor);
            b.addVertex(center.x + x2, center.y + y2, center.z).setColor(outerColor);
        }
        return segments * 3;
    }

    private static void addQuad(BufferBuilder b, Vector4f v1, Vector4f v2, Vector4f v3, Vector4f v4, int c1, int c2) {
        b.addVertex(v1.x, v1.y, v1.z).setColor(c1);
        b.addVertex(v2.x, v2.y, v2.z).setColor(c1);
        b.addVertex(v3.x, v3.y, v3.z).setColor(c2);

        b.addVertex(v1.x, v1.y, v1.z).setColor(c1);
        b.addVertex(v3.x, v3.y, v3.z).setColor(c2);
        b.addVertex(v4.x, v4.y, v4.z).setColor(c2);
    }

    private void spawnFly(Vec3 playerPos, int color) {
        double dist = random.nextDouble() * (SPAWN_RADIUS - 8) + 6;
        double yaw = Math.toRadians(random.nextDouble() * 360);

        flies.add(new FireFlyEntity(
                playerPos.x - Math.sin(yaw) * dist,
                playerPos.y + (random.nextDouble() - 0.3) * 5 + 1.2,
                playerPos.z + Math.cos(yaw) * dist,
                color, random.nextInt(100)
        ));
    }

    private void ensureVertexCapacity(int byteSize) {
        if (this.vertexBuffer != null && this.vertexBuffer.size() >= byteSize) return;
        if (this.vertexBuffer != null) this.vertexBuffer.close();
        int capacity = Math.max(byteSize + byteSize / 2, 64 * 1024);
        this.vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Firefly Laser Strip Buffer",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                capacity
        );
    }

    public void release() {
        this.flies.clear();
        this.sparks.clear();
        this.lastFrameTime = 0;
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
            this.vertexBuffer = null;
        }
    }
}