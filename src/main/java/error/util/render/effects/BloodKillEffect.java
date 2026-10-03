package error.util.render.effects;

import error.IMinecraft;
import error.util.render.LevelProjection;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ThreadLocalRandom;

public final class BloodKillEffect implements IMinecraft {
    private static final int MAX_DROPS = 900;
    private static final int MAX_SPLATS = 700;
    private static final int RING = 6;
    private static final int SPLAT_SEGMENTS = 14;
    private static final double RANGE_SQ = 96.0 * 96.0;
    private static final double GRAVITY = 0.045;
    private static final double DRAG = 0.985;
    private static final int MAX_FLIGHT_TICKS = 120;

    private static final RenderPipeline BLOOD = RenderPipeline.builder()
            .withLocation(Identifier.parse("delta:pipeline/kill_blood"))
            .withVertexShader(Identifier.parse("delta:core/aura_filled"))
            .withFragmentShader(Identifier.parse("delta:core/aura_filled"))
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build();

    private final List<Drop> drops = new ArrayList<>();
    private final List<Splat> splats = new ArrayList<>();
    private int splatCounter;

    public void clear() {
        drops.clear();
        splats.clear();
    }

    public boolean isEmpty() {
        return drops.isEmpty() && splats.isEmpty();
    }

    public void burst(Vec3 center, float width, float height, int count, float power, float scale, int color) {
        if (mc.level == null || mc.player == null) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long now = System.currentTimeMillis();

        drops.clear();

        for (int i = 0; i < count; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = Math.pow(rnd.nextDouble(), 0.7) * (width * 1.4 + power * 1.5);
            double spawnX = center.x + Math.cos(angle) * dist;
            double spawnZ = center.z + Math.sin(angle) * dist;

            Vec3 topPos = new Vec3(spawnX, center.y + 1.5, spawnZ);
            Vec3 bottomPos = new Vec3(spawnX, center.y - 12.0, spawnZ);

            BlockHitResult hit = mc.level.clip(new ClipContext(topPos, bottomPos,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

            Vec3 floorPos;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP) {
                floorPos = hit.getLocation();
            } else {
                floorPos = new Vec3(spawnX, center.y - height * 0.5F, spawnZ);
            }

            float radius = (float) (0.06 + Math.pow(rnd.nextDouble(), 1.8) * 0.24) * scale;
            int splatColor = shade(color, 0.8f + rnd.nextFloat() * 0.35f);
            splats.add(new Splat(floorPos, radius, splatColor, now, rnd.nextLong(), light(floorPos), splatCounter++ % 40));
            while (splats.size() > MAX_SPLATS) {
                splats.remove(0);
            }
        }
    }

    private Drop addDrop(Vec3 origin, Vec3 velocity, float size, int color) {
        Drop drop = new Drop(origin, velocity, size, color);
        if (mc.level != null) drop.light = light(origin);
        drops.add(drop);
        while (drops.size() > MAX_DROPS) drops.remove(0);
        return drop;
    }

    public void tick(float splatSeconds) {
        if (mc.level == null || mc.player == null) {
            clear();
            return;
        }
        long now = System.currentTimeMillis();
        long splatLife = (long) (splatSeconds * 1000f);
        splats.removeIf(s -> now - s.start > splatLife);
        drops.clear();
    }

    public void render(Camera camera, float partialTicks, float splatSeconds) {
        if (camera == null || splats.isEmpty() || mc.gameRenderer == null
                || mc.gameRenderer.mainRenderTarget() == null) return;
        Vec3 cam = camera.position();
        Matrix4f view = camera.getViewRotationMatrix(new Matrix4f())
                .translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
        long now = System.currentTimeMillis();
        float splatLife = splatSeconds * 1000f;

        int maxQuads = splats.size() * SPLAT_SEGMENTS * 2;
        if (maxQuads <= 0) return;
        int capacity = DefaultVertexFormat.POSITION_COLOR.getVertexSize() * 4 * maxQuads;
        try (ByteBufferBuilder memory = new ByteBufferBuilder(capacity)) {
            BufferBuilder buffer = new BufferBuilder(memory, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int quads = 0;
            for (Splat s : splats) {
                if (s.pos.distanceToSqr(cam) > RANGE_SQ) continue;
                quads += appendSplat(buffer, view, s, (now - s.start) / splatLife, now - s.start);
            }
            if (quads > 0) draw(buffer, quads);
        } catch (Throwable ignored) { }
    }

    private static int appendDrop(BufferBuilder b, Matrix4f m, Vec3 p, Drop d) {
        Vec3 dir = d.vel;
        double speed = dir.length();
        if (speed < 1.0E-4) dir = new Vec3(0, -1, 0); else dir = dir.scale(1.0 / speed);
        float r = d.size * 0.5f;
        float head = r * 0.9f;
        float tail = r * (1.6f + (float) Math.min(speed, 0.9) * 5.5f);

        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 s1 = dir.cross(up).normalize();
        Vec3 s2 = dir.cross(s1).normalize();

        Vec3 front = p.add(dir.scale(head));
        Vec3 back = p.subtract(dir.scale(tail));
        Vec3 ringCenter = p.add(dir.scale(head * 0.15));
        Vec3[] ring = new Vec3[RING];
        Vec3[] normals = new Vec3[RING];
        for (int i = 0; i < RING; i++) {
            double a = i * Math.PI * 2.0 / RING;
            Vec3 n = s1.scale(Math.cos(a)).add(s2.scale(Math.sin(a)));
            normals[i] = n;
            ring[i] = ringCenter.add(n.scale(r));
        }
        float light = d.light;
        int quads = 0;
        for (int i = 0; i < RING; i++) {
            int j = (i + 1) % RING;
            Vec3 n = normals[i].add(normals[j]).normalize();
            int cf = lit(d.color, n.add(dir.scale(0.6)).normalize(), light, true);
            int cb = lit(d.color, n.add(dir.scale(-0.3)).normalize(), light, false);
            tri(b, m, front, ring[i], ring[j], cf, cf, cf);
            int tailColor = withAlpha(shade(cb, 0.8f), 150);
            tri(b, m, ring[i], back, ring[j], cb, tailColor, cb);
            quads += 2;
        }
        return quads;
    }

    private static int appendSplat(BufferBuilder b, Matrix4f m, Splat s, float life, long ageMs) {
        if (life >= 1.0f) return 0;
        float grow = 1.0f - (float) Math.pow(1.0f - Math.min(1f, ageMs / 260f), 3);
        float fade = 1.0f - smooth((life - 0.7f) / 0.3f);
        float dry = smooth(life / 0.8f);
        float radius = s.radius * (0.35f + 0.65f * grow);
        int base = shade(s.color, s.light * (1.0f - 0.45f * dry));
        int centerCol = withAlpha(shade(base, 0.72f), Math.round(235 * fade));
        int edgeCol = withAlpha(base, Math.round(205 * fade));
        int rimCol = withAlpha(base, 0);
        if (fade <= 0.01f) return 0;

        double y = s.pos.y + 0.006 + s.layer * 0.0004;
        Vec3 c = new Vec3(s.pos.x, y, s.pos.z);
        float[] radii = new float[SPLAT_SEGMENTS];
        for (int i = 0; i < SPLAT_SEGMENTS; i++) {
            radii[i] = radius * (0.72f + noise(s.seed, i) * 0.5f);
        }
        int quads = 0;
        for (int i = 0; i < SPLAT_SEGMENTS; i++) {
            int j = (i + 1) % SPLAT_SEGMENTS;
            double a1 = i * Math.PI * 2.0 / SPLAT_SEGMENTS;
            double a2 = j * Math.PI * 2.0 / SPLAT_SEGMENTS;
            Vec3 p1 = c.add(Math.cos(a1) * radii[i], 0, Math.sin(a1) * radii[i]);
            Vec3 p2 = c.add(Math.cos(a2) * radii[j], 0, Math.sin(a2) * radii[j]);
            Vec3 o1 = c.add(Math.cos(a1) * radii[i] * 1.18, 0, Math.sin(a1) * radii[i] * 1.18);
            Vec3 o2 = c.add(Math.cos(a2) * radii[j] * 1.18, 0, Math.sin(a2) * radii[j] * 1.18);
            tri(b, m, c, p1, p2, centerCol, edgeCol, edgeCol);
            quad(b, m, p1, o1, o2, p2, edgeCol, rimCol, rimCol, edgeCol);
            quads += 2;
        }
        return quads;
    }

    private static int lit(int color, Vec3 normal, float light, boolean highlight) {
        double lx = 0.35, ly = 0.85, lz = 0.25;
        double dot = Math.max(0.0, normal.x * lx + normal.y * ly + normal.z * lz);
        float k = (float) (0.45 + 0.75 * dot) * light;
        int c = shade(color, k);
        if (highlight && dot > 0.8) c = mix(c, 0xFFFF9A9A, (float) ((dot - 0.8) / 0.2) * 0.45f * light);
        return c;
    }

    private float light(Vec3 pos) {
        try {
            if (mc.level == null) return 1.0f;
            int raw = mc.level.getMaxLocalRawBrightness(BlockPos.containing(pos));
            return 0.3f + 0.7f * (raw / 15.0f);
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    private static void tri(BufferBuilder b, Matrix4f m, Vec3 a, Vec3 c, Vec3 d, int ca, int cc, int cd) {
        vertex(b, m, a, ca);
        vertex(b, m, c, cc);
        vertex(b, m, d, cd);
        vertex(b, m, d, cd);
    }

    private static void quad(BufferBuilder b, Matrix4f m, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                             int c1, int c2, int c3, int c4) {
        vertex(b, m, p1, c1);
        vertex(b, m, p2, c2);
        vertex(b, m, p3, c3);
        vertex(b, m, p4, c4);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 p, int color) {
        buffer.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z).setColor(
                ((color >> 16) & 255) / 255.0f,
                ((color >> 8) & 255) / 255.0f,
                (color & 255) / 255.0f,
                ((color >>> 24) & 255) / 255.0f);
    }

    private static void draw(BufferBuilder buffer, int quads) {
        String name = "KillBlood";
        try (MeshData mesh = buffer.buildOrThrow()) {
            GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> name, 32, mesh.vertexBuffer());
            try {
                if (mc.gameRenderer == null || mc.gameRenderer.mainRenderTarget() == null) return;
                var target = mc.gameRenderer.mainRenderTarget();
                try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                        () -> name, target.getColorTextureView(), Optional.empty(),
                        target.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(BLOOD);
                    var projection = LevelProjection.get();
                    pass.setUniform("Projection",
                            projection != null ? projection : RenderSystem.getProjectionMatrixBuffer());
                    pass.setVertexBuffer(0, vertices.slice());
                    var sequential = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
                    GpuBuffer indices = sequential.getBuffer(quads * 6);
                    pass.setIndexBuffer(indices, sequential.type());
                    pass.drawIndexed(quads * 6, 1, 0, 0, 0);
                }
            } finally {
                vertices.close();
            }
        }
    }

    private static int shade(int color, float k) {
        int a = (color >>> 24) & 255;
        int r = Math.min(255, Math.round(((color >> 16) & 255) * k));
        int g = Math.min(255, Math.round(((color >> 8) & 255) * k));
        int bl = Math.min(255, Math.round((color & 255) * k));
        return (a << 24) | (r << 16) | (g << 8) | bl;
    }

    private static int mix(int first, int second, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (first >>> 24) & 255;
        int r = Math.round(((first >> 16) & 255) * (1 - t) + ((second >> 16) & 255) * t);
        int g = Math.round(((first >> 8) & 255) * (1 - t) + ((second >> 8) & 255) * t);
        int b = Math.round((first & 255) * (1 - t) + (second & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private static float smooth(float v) {
        float t = Math.max(0f, Math.min(1f, v));
        return t * t * (3f - 2f * t);
    }

    private static float noise(long seed, int salt) {
        long value = seed + 0x9E3779B97F4A7C15L * (salt + 1L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return ((value ^ (value >>> 31)) >>> 40) / 16777215.0f;
    }

    private static final class Drop {
        Vec3 pos, prev, vel;
        final float size;
        final int color;
        float light = 1.0f;
        int age;
        boolean child;
        float scaleRef = 1.0f;

        Drop(Vec3 pos, Vec3 vel, float size, int color) {
            this.pos = pos;
            this.prev = pos;
            this.vel = vel;
            this.size = size;
            this.color = color | 0xFF000000;
        }
    }

    private static final class Splat {
        final Vec3 pos;
        final float radius;
        final int color;
        final long start;
        final long seed;
        final float light;
        final int layer;

        Splat(Vec3 pos, float radius, int color, long start, long seed, float light, int layer) {
            this.pos = pos;
            this.radius = radius;
            this.color = color;
            this.start = start;
            this.seed = seed;
            this.light = light;
            this.layer = layer;
        }
    }
}
