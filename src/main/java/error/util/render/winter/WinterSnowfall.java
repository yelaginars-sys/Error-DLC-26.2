package error.util.render.winter;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import error.util.render.WinterSnowRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class WinterSnowfall {
    private static final Identifier SOFT_FLAKE = Identifier.parse("delta:pictures/particles/snow_soft.png");
    private static final Identifier DETAILED_FLAKE = Identifier.parse("delta:pictures/particles/snowflake.png");
    private static final int MAX_FLAKES = 4200;
    private static final Identifier SNOW_COVER = Identifier.parse("delta:pictures/particles/snow_cover.png");
    private static final int MAX_GROUND_QUADS = 60000;
    private static final float GRID_REFRESH_SECONDS = 3.0f;
    private static final int GRID_MARGIN = 3;
    private static final int MAX_COLUMN_SCAN = 48;
    private static final int FLOATS_PER_QUAD = 24;
    private static final float EDGE_EPS = 0.006f;
    private static final float LIP_DEPTH = 0.035f;
    private static final float SIDE_V = 0.18f;

    public static final class Config {
        public float radius = 32.0f;
        public float height = 24.0f;
        public float density = 1.2f;
        public float flakeSize = 1.0f;
        public float fallSpeed = 1.0f;
        public float wind = 1.0f;
        public float opacity = 1.0f;
        public float blizzard = 0.0f;
        public boolean skyOnly = true;
        public boolean groundSnow = true;
        public float groundRadius = 40.0f;
        public float groundDepth = 0.09f;
        public int color = 0xFFEAF4FF;
    }

    private static final WinterSnowfall INSTANCE = new WinterSnowfall();

    public static WinterSnowfall getInstance() {
        return INSTANCE;
    }

    private final List<Flake> flakes = new ArrayList<>(1024);
    private final Random random = new Random();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private final Matrix4f sprite = new Matrix4f();
    private final Quaternionf inverseCamera = new Quaternionf();
    private final Vector3f scratchVelocity = new Vector3f();

    private float[] surfaceBase = new float[0];
    private boolean[] surfaceFull = new boolean[0];
    private float[] surfaceLight = new float[0];
    private int[] surfaceY = new int[0];
    private VoxelShape[] surfaceShapes = new VoxelShape[0];
    private float[] mesh = new float[FLOATS_PER_QUAD * 4096];
    private int meshQuads;
    private float builtDepth = -1.0f;
    private int resolvedY;
    private VoxelShape currentShape;
    private int currentWorldX;
    private int currentWorldZ;
    private final float[] segStart = new float[16];
    private final float[] segEnd = new float[16];
    private final float[] segStartNext = new float[16];
    private final float[] segEndNext = new float[16];
    private VoxelShape resolvedShape;
    private int gridRadius = -1;
    private int gridOriginX = Integer.MIN_VALUE;
    private int gridOriginY = Integer.MIN_VALUE;
    private int gridOriginZ = Integer.MIN_VALUE;
    private float gridAge = Float.MAX_VALUE;
    private long lastFrameNanos;
    private float clock;

    private WinterSnowfall() {
    }

    public void reset() {
        flakes.clear();
        meshQuads = 0;
        builtDepth = -1.0f;
        gridRadius = -1;
        gridAge = Float.MAX_VALUE;
        lastFrameNanos = 0L;
    }

    public void render(Camera camera, Config config) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || camera == null || config == null) {
            reset();
            return;
        }

        long now = System.nanoTime();
        float delta = lastFrameNanos == 0L ? 0.016f : Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000.0f);
        lastFrameNanos = now;
        clock += delta;

        Vec3 camPos = camera.position();
        if (config.groundSnow && config.groundDepth > 0.001f) {
            updateSurfaceGrid(mc.level, camPos, config, delta);
            renderGroundMesh(camPos, config);
        }

        updateFlakes(mc.level, camPos, config, delta);
        renderFlakes(camera, camPos, config);
    }

    private void updateFlakes(ClientLevel level, Vec3 camPos, Config config, float delta) {
        int targetCount = Math.min(MAX_FLAKES, Math.max(0, Math.round(config.density * (config.radius * config.radius * 0.45f))));
        while (flakes.size() < targetCount) {
            flakes.add(spawnFlake(level, camPos, config, true));
        }
        while (flakes.size() > targetCount) {
            flakes.remove(flakes.size() - 1);
        }

        float radiusSq = config.radius * config.radius;
        float baseFall = config.fallSpeed * 3.6f;
        float windSpeed = config.wind * (1.2f + config.blizzard * 4.8f);
        float baseRadius = config.radius;
        float heightRange = Math.max(12.0f, config.height);

        for (int i = 0; i < flakes.size(); i++) {
            Flake flake = flakes.get(i);
            flake.age += delta;

            float localWind = windSpeed * (0.75f + 0.5f * (float) Math.sin(clock * 1.7f + flake.seed));
            float fall = (baseFall * flake.fallMult + config.blizzard * 1.8f) * (0.85f + 0.3f * (float) Math.cos(clock * 2.3f + flake.seed * 0.5f));
            float wobbleX = (float) Math.sin(clock * flake.wobbleSpeed + flake.seed) * flake.wobbleAmp;
            float wobbleZ = (float) Math.cos(clock * (flake.wobbleSpeed * 0.9f) + flake.seed * 1.3f) * flake.wobbleAmp;

            flake.x += (localWind + wobbleX) * delta;
            flake.y -= fall * delta;
            flake.z += wobbleZ * delta;
            flake.rot += flake.rotSpeed * delta;

            double dx = flake.x - camPos.x;
            double dy = flake.y - camPos.y;
            double dz = flake.z - camPos.z;

            boolean outOfBounds = (dx * dx + dz * dz) > radiusSq || dy < -6.0 || dy > heightRange + 6.0;
            if (!outOfBounds && config.skyOnly) {
                scratchPos.set(Mth.floor(flake.x), Mth.floor(flake.y), Mth.floor(flake.z));
                if (!level.canSeeSky(scratchPos)) {
                    outOfBounds = true;
                }
            }

            if (outOfBounds) {
                flakes.set(i, spawnFlake(level, camPos, config, false));
            }
        }
    }

    private Flake spawnFlake(ClientLevel level, Vec3 camPos, Config config, boolean randomHeight) {
        float r = (float) (Math.sqrt(random.nextFloat()) * config.radius);
        float angle = random.nextFloat() * ((float) Math.PI * 2.0f);
        float x = (float) (camPos.x + Math.cos(angle) * r);
        float z = (float) (camPos.z + Math.sin(angle) * r);

        float heightRange = Math.max(12.0f, config.height);
        float y;
        if (randomHeight) {
            y = (float) (camPos.y - 4.0 + random.nextFloat() * (heightRange + 8.0));
        } else {
            y = (float) (camPos.y + heightRange * (0.45f + random.nextFloat() * 0.55f));
        }

        if (config.skyOnly) {
            int skyY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            y = Math.max(y, skyY + 0.5f);
        }

        Flake flake = new Flake();
        flake.x = x;
        flake.y = y;
        flake.z = z;
        flake.size = (0.07f + random.nextFloat() * 0.16f) * config.flakeSize;
        flake.fallMult = 0.75f + random.nextFloat() * 0.55f;
        flake.wobbleSpeed = 1.8f + random.nextFloat() * 2.4f;
        flake.wobbleAmp = 0.12f + random.nextFloat() * 0.35f;
        flake.rot = random.nextFloat() * ((float) Math.PI * 2.0f);
        flake.rotSpeed = (random.nextBoolean() ? 1f : -1f) * (0.6f + random.nextFloat() * 2.2f);
        flake.seed = random.nextFloat() * 1000.0f;
        flake.type = random.nextFloat() < (0.18f + config.blizzard * 0.22f) ? 1 : 0;
        return flake;
    }

    private void renderFlakes(Camera camera, Vec3 camPos, Config config) {
        if (flakes.isEmpty()) return;

        int softQuads = 0;
        int detailedQuads = 0;
        for (Flake flake : flakes) {
            if (flake.type == 0) softQuads++;
            else detailedQuads++;
        }

        inverseCamera.set(camera.rotation()).invert();

        if (softQuads > 0) {
            renderFlakeBuffer(camPos, config, 0, softQuads, SOFT_FLAKE);
        }
        if (detailedQuads > 0) {
            renderFlakeBuffer(camPos, config, 1, detailedQuads, DETAILED_FLAKE);
        }
    }

    private void renderFlakeBuffer(Vec3 camPos, Config config, int targetType, int quads, Identifier texture) {
        int capacity = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4 * quads;
        try (ByteBufferBuilder memory = new ByteBufferBuilder(capacity)) {
            BufferBuilder builder = new BufferBuilder(memory, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int count = 0;

            int color = config.color;
            int baseR = (color >> 16) & 255;
            int baseG = (color >> 8) & 255;
            int baseB = color & 255;
            float baseAlpha = ((color >>> 24) & 255) / 255.0f * config.opacity;

            for (Flake flake : flakes) {
                if (flake.type != targetType) continue;

                float relX = (float) (flake.x - camPos.x);
                float relY = (float) (flake.y - camPos.y);
                float relZ = (float) (flake.z - camPos.z);

                sprite.identity();
                sprite.translate(relX, relY, relZ);
                sprite.rotate(inverseCamera);
                sprite.rotateZ(flake.rot);

                float half = flake.size * 0.5f;
                float alpha = baseAlpha * (targetType == 1 ? 0.95f : 0.8f);

                addVertex(builder, sprite, -half, -half, 0f, 0f, 1f, baseR, baseG, baseB, alpha);
                addVertex(builder, sprite, half, -half, 0f, 1f, 1f, baseR, baseG, baseB, alpha);
                addVertex(builder, sprite, half, half, 0f, 1f, 0f, baseR, baseG, baseB, alpha);
                addVertex(builder, sprite, -half, half, 0f, 0f, 0f, baseR, baseG, baseB, alpha);

                count++;
            }

            if (count > 0) {
                WinterSnowRenderer.draw(builder, count, texture);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void addVertex(BufferBuilder builder, Matrix4f mat, float x, float y, float z,
                                  float u, float v, int r, int g, int b, float a) {
        builder.addVertex(mat, x, y, z)
                .setUv(u, v)
                .setColor(r / 255.0f, g / 255.0f, b / 255.0f, a);
    }

    private void updateSurfaceGrid(ClientLevel level, Vec3 camPos, Config config, float delta) {
        int r = Mth.clamp((int) Math.ceil(config.groundRadius), 8, 48);
        int size = r * 2 + 1;
        int centerWorldX = Mth.floor(camPos.x);
        int centerWorldY = Mth.floor(camPos.y);
        int centerWorldZ = Mth.floor(camPos.z);

        gridAge += delta;
        boolean moved = Math.abs(centerWorldX - gridOriginX) > 2
                || Math.abs(centerWorldY - gridOriginY) > 4
                || Math.abs(centerWorldZ - gridOriginZ) > 2;

        if (moved || gridRadius != r || Math.abs(builtDepth - config.groundDepth) > 0.001f || gridAge > GRID_REFRESH_SECONDS) {
            gridRadius = r;
            gridOriginX = centerWorldX;
            gridOriginY = centerWorldY;
            gridOriginZ = centerWorldZ;
            gridAge = 0.0f;
            rebuildGroundMesh(level, camPos, config, r, size, centerWorldX, centerWorldY, centerWorldZ);
        }
    }

    private void rebuildGroundMesh(ClientLevel level, Vec3 camPos, Config config, int r, int size,
                                   int centerWorldX, int centerWorldY, int centerWorldZ) {
        int cells = size * size;
        if (surfaceBase.length < cells) {
            surfaceBase = new float[cells];
            surfaceFull = new boolean[cells];
            surfaceLight = new float[cells];
            surfaceY = new int[cells];
            surfaceShapes = new VoxelShape[cells];
        }

        for (int i = 0; i < cells; i++) {
            surfaceY[i] = Integer.MIN_VALUE;
            surfaceBase[i] = -9999f;
            surfaceFull[i] = false;
            surfaceLight[i] = 1.0f;
            surfaceShapes[i] = null;
        }

        int scanRadius = r + GRID_MARGIN;
        int minX = centerWorldX - scanRadius;
        int maxX = centerWorldX + scanRadius;
        int minZ = centerWorldZ - scanRadius;
        int maxZ = centerWorldZ + scanRadius;

        for (int wz = minZ; wz <= maxZ; wz++) {
            for (int wx = minX; wx <= maxX; wx++) {
                int localX = wx - (centerWorldX - r);
                int localZ = wz - (centerWorldZ - r);
                boolean insideGrid = localX >= 0 && localX < size && localZ >= 0 && localZ < size;
                int cellIdx = insideGrid ? localZ * size + localX : -1;

                int startY = Math.min(level.getMaxY() - 1, centerWorldY + 16);
                int minY = Math.max(level.getMinY(), centerWorldY - MAX_COLUMN_SCAN);

                for (int wy = startY; wy >= minY; wy--) {
                    scratchPos.set(wx, wy, wz);
                    BlockState state = level.getBlockState(scratchPos);
                    if (state.isAir() || state.getBlock() instanceof SnowLayerBlock || state.is(Blocks.SNOW)) continue;

                    RenderShape shape = state.getRenderShape();
                    if (shape == RenderShape.INVISIBLE) continue;

                    VoxelShape collision = state.getCollisionShape(level, scratchPos);
                    if (collision.isEmpty()) continue;

                    double topY = collision.max(Direction.Axis.Y);
                    if (topY <= 0.001) continue;

                    scratchPos.set(wx, wy + 1, wz);
                    BlockState above = level.getBlockState(scratchPos);
                    if (!above.isAir() && above.getRenderShape() != RenderShape.INVISIBLE) {
                        VoxelShape aboveCollision = above.getCollisionShape(level, scratchPos);
                        if (!aboveCollision.isEmpty() && aboveCollision.min(Direction.Axis.Y) <= 0.05) {
                            continue;
                        }
                    }

                    if (insideGrid) {
                        surfaceY[cellIdx] = wy;
                        surfaceBase[cellIdx] = (float) (wy + topY);
                        surfaceFull[cellIdx] = Block.isFaceFull(collision, Direction.UP);
                        surfaceShapes[cellIdx] = collision;

                        scratchPos.set(wx, wy + 1, wz);
                        int skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, scratchPos);
                        int blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, scratchPos);
                        surfaceLight[cellIdx] = Math.max(0.35f, Math.min(1.0f, (skyLight * 0.95f + blockLight * 0.7f) / 15.0f));
                    }
                    break;
                }
            }
        }

        buildMeshQuads(size, r, config, centerWorldX, centerWorldZ);
    }

    private void buildMeshQuads(int size, int r, Config config, int centerWorldX, int centerWorldZ) {
        meshQuads = 0;
        builtDepth = config.groundDepth;
        float depth = config.groundDepth;

        int capacityNeeded = size * size * 6 * FLOATS_PER_QUAD;
        if (mesh.length < capacityNeeded) {
            mesh = new float[capacityNeeded];
        }

        int rSq = r * r;

        for (int lz = 0; lz < size; lz++) {
            int wz = centerWorldZ - r + lz;
            for (int lx = 0; lx < size; lx++) {
                int wx = centerWorldX - r + lx;
                int dx = lx - r;
                int dz = lz - r;
                if (dx * dx + dz * dz > rSq) continue;

                int idx = lz * size + lx;
                float base = surfaceBase[idx];
                if (base < -9000f) continue;

                int wy = surfaceY[idx];
                VoxelShape shape = surfaceShapes[idx];
                float light = surfaceLight[idx];

                resolvedY = wy;
                resolvedShape = shape;
                currentWorldX = wx;
                currentWorldZ = wz;

                if (shape != null && !shape.isEmpty()) {
                    AABB bounds = shape.bounds();
                    float minX = (float) bounds.minX;
                    float maxX = (float) bounds.maxX;
                    float minZ = (float) bounds.minZ;
                    float maxZ = (float) bounds.maxZ;
                    float topY = (float) bounds.maxY;

                    emitTopQuad(wx + minX, wy + topY + depth, wz + minZ,
                            wx + maxX, wy + topY + depth, wz + maxZ, light, depth);

                    if (minX > 0.01f) {
                        emitSideQuad(wx + minX, wy + topY, wz + minZ, wx + minX, wy + topY + depth, wz + maxZ, light, depth);
                    }
                    if (maxX < 0.99f) {
                        emitSideQuad(wx + maxX, wy + topY + depth, wz + minZ, wx + maxX, wy + topY, wz + maxZ, light, depth);
                    }
                } else {
                    emitTopQuad(wx, base + depth, wz, wx + 1.0f, base + depth, wz + 1.0f, light, depth);
                }
            }
        }
    }

    private void emitTopQuad(float x1, float y1, float z1, float x2, float y2, float z2, float light, float depth) {
        if (meshQuads >= MAX_GROUND_QUADS) return;
        int offset = meshQuads * FLOATS_PER_QUAD;

        mesh[offset++] = x1; mesh[offset++] = y1; mesh[offset++] = z1; mesh[offset++] = 0.0f; mesh[offset++] = 0.0f; mesh[offset++] = light;
        mesh[offset++] = x2; mesh[offset++] = y1; mesh[offset++] = z1; mesh[offset++] = 1.0f; mesh[offset++] = 0.0f; mesh[offset++] = light;
        mesh[offset++] = x2; mesh[offset++] = y1; mesh[offset++] = z2; mesh[offset++] = 1.0f; mesh[offset++] = 1.0f; mesh[offset++] = light;
        mesh[offset++] = x1; mesh[offset++] = y1; mesh[offset++] = z2; mesh[offset++] = 0.0f; mesh[offset++] = 1.0f; mesh[offset++] = light;

        meshQuads++;
    }

    private void emitSideQuad(float x1, float y1, float z1, float x2, float y2, float z2, float light, float depth) {
        if (meshQuads >= MAX_GROUND_QUADS) return;
        int offset = meshQuads * FLOATS_PER_QUAD;

        mesh[offset++] = x1; mesh[offset++] = y1; mesh[offset++] = z1; mesh[offset++] = 0.0f; mesh[offset++] = 0.0f; mesh[offset++] = light * 0.85f;
        mesh[offset++] = x2; mesh[offset++] = y1; mesh[offset++] = z2; mesh[offset++] = 1.0f; mesh[offset++] = 0.0f; mesh[offset++] = light * 0.85f;
        mesh[offset++] = x2; mesh[offset++] = y2; mesh[offset++] = z2; mesh[offset++] = 1.0f; mesh[offset++] = SIDE_V; mesh[offset++] = light * 0.85f;
        mesh[offset++] = x1; mesh[offset++] = y2; mesh[offset++] = z1; mesh[offset++] = 0.0f; mesh[offset++] = SIDE_V; mesh[offset++] = light * 0.85f;

        meshQuads++;
    }

    private void renderGroundMesh(Vec3 camPos, Config config) {
        if (meshQuads <= 0) return;

        int capacity = DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4 * meshQuads;
        try (ByteBufferBuilder memory = new ByteBufferBuilder(capacity)) {
            BufferBuilder builder = new BufferBuilder(memory, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            int color = config.color;
            int baseR = (color >> 16) & 255;
            int baseG = (color >> 8) & 255;
            int baseB = color & 255;

            int ptr = 0;
            for (int q = 0; q < meshQuads; q++) {
                for (int v = 0; v < 4; v++) {
                    float vx = mesh[ptr++] - (float) camPos.x;
                    float vy = mesh[ptr++] - (float) camPos.y;
                    float vz = mesh[ptr++] - (float) camPos.z;
                    float u = mesh[ptr++];
                    float tv = mesh[ptr++];
                    float l = mesh[ptr++];

                    builder.addVertex(vx, vy, vz)
                            .setUv(u, tv)
                            .setColor((baseR * l) / 255.0f, (baseG * l) / 255.0f, (baseB * l) / 255.0f, 0.92f);
                }
            }

            WinterSnowRenderer.drawCover(builder, meshQuads, SNOW_COVER);
        } catch (Throwable ignored) {
        }
    }

    private static final class Flake {
        float x, y, z;
        float size;
        float fallMult;
        float wobbleSpeed;
        float wobbleAmp;
        float rot;
        float rotSpeed;
        float seed;
        float age;
        int type;
    }
}
