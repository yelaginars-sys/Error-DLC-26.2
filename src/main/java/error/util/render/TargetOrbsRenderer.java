package error.util.render;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.world.particles.ParticlesWorldRenderer;
import error.util.render.world.particles.ParticlesWorldRenderer.Sprite;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class TargetOrbsRenderer {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final int MAX_ORBS = 16;
    private static final int TRAIL_LENGTH = 12;

    @SuppressWarnings("unchecked")
    private final ArrayDeque<Vec3>[] orbTrails = new ArrayDeque[MAX_ORBS];
    @SuppressWarnings("unchecked")
    private final ArrayDeque<Vec3>[] soulTrails = new ArrayDeque[MAX_ORBS];
    private boolean orbTrailsInit = false;

    private final ParticlesWorldRenderer particlesWorldRenderer = new ParticlesWorldRenderer();
    private final List<Sprite> sprites = new ArrayList<>();

    private long lastOrbSampleTime = 0L;
    private long lastSoulSampleTime = 0L;
    private long lastTimeMs = 0L;
    private double stableTimeSec = 0.0D;

    private LivingEntity lastTarget;
    private Vec3 lastTargetPos;
    private float lastTargetHeight = 1.8F;

    public TargetOrbsRenderer() {
        initTrails();
    }

    private void initTrails() {
        if (!orbTrailsInit) {
            for (int i = 0; i < MAX_ORBS; i++) {
                orbTrails[i] = new ArrayDeque<>();
                soulTrails[i] = new ArrayDeque<>();
            }
            orbTrailsInit = true;
        }
    }

    public void reset() {
        for (int i = 0; i < MAX_ORBS; i++) {
            if (orbTrails[i] != null) orbTrails[i].clear();
            if (soulTrails[i] != null) soulTrails[i].clear();
        }
        lastOrbSampleTime = 0L;
        lastSoulSampleTime = 0L;
        lastTimeMs = 0L;
        stableTimeSec = 0.0D;
        lastTarget = null;
        lastTargetPos = null;
        sprites.clear();
    }

    public void renderOrbs(LivingEntity target, float alphaProgress, float tickDelta,
                           int numOrbs, float baseSizePx, float radiusConst, float speed,
                           boolean saturation, boolean colorOnHit, int hitColor, String colorMode, int customColor) {
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            reset();
            return;
        }

        initTrails();

        boolean hasTarget = target != null && target.isAlive() && !target.isRemoved();
        if (hasTarget) {
            lastTarget = target;
            lastTargetPos = Render3DUtil.interpolatedPosition(target, tickDelta);
            lastTargetHeight = target.getBbHeight();
        }

        LivingEntity renderTarget = hasTarget ? target : lastTarget;
        Vec3 vec;
        float height;
        if (renderTarget != null && renderTarget.isAlive()) {
            vec = Render3DUtil.interpolatedPosition(renderTarget, tickDelta);
            height = renderTarget.getBbHeight();
            lastTargetPos = vec;
            lastTargetHeight = height;
        } else if (lastTargetPos != null) {
            vec = lastTargetPos;
            height = lastTargetHeight;
        } else {
            return;
        }

        float aPC = alphaProgress;
        if (aPC <= 0.001f) return;

        long currentMs = System.currentTimeMillis();
        if (lastTimeMs == 0L) lastTimeMs = currentMs;
        double deltaSec = Mth.clamp((currentMs - lastTimeMs) / 1000.0D, 0.0D, 0.1D);
        lastTimeMs = currentMs;
        stableTimeSec += deltaSec * Math.max(0.1F, speed);

        double bx = vec.x;
        double by = vec.y;
        double bz = vec.z;

        final float upperPosition = Math.max(0.4f, height);
        final float lowerPosition = 0.2F;
        final int orbsCount = Mth.clamp(numOrbs, 1, MAX_ORBS);
        final int trailLength = TRAIL_LENGTH;

        double rotPhase = stableTimeSec * 0.7D;
        double vertPhase = stableTimeSec * 0.7D;

        float hurtPC = 0.0F;
        if (colorOnHit && renderTarget != null && renderTarget.hurtTime > 0) {
            hurtPC = (float) Math.sin((double) renderTarget.hurtTime * (Math.PI / 20.0D));
        }

        int baseCol = resolveColor(colorMode, customColor, 0);
        float radius = radiusConst;
        double angleStep = 360.0D / orbsCount;

        for (int k = 0; k < orbsCount; k++) {
            for (int j = 0; j < trailLength; j++) {
                float kf = (float) j / (float) trailLength;
                float ease = 1f - (kf * kf * (3f - 2f * kf));

                final double segPhaseSec = 0.035D;
                final double tj = (rotPhase - j * segPhaseSec) % 1.0D;
                final double tvj = vertPhase - j * segPhaseSec;
                final double cyc = (Math.sin(2.0D * Math.PI * tvj) + 1.0D) * 0.5D;

                float dynRadius = radius * (0.85f + 0.15f * ease);
                final double baseAngleRad = Math.toRadians(k * angleStep + tj * 360.0D);
                double offX = Math.cos(baseAngleRad) * dynRadius;
                double offZ = Math.sin(baseAngleRad) * dynRadius;
                double offY = lowerPosition + (upperPosition - lowerPosition) * cyc;

                double px = bx + offX;
                double py = by + offY;
                double pz = bz + offZ;

                if (j == 0) {
                    if (currentMs - lastOrbSampleTime >= 20) {
                        pushOrbPoint(k, px, py, pz, trailLength);
                        if (k == orbsCount - 1) {
                            lastOrbSampleTime = currentMs;
                        }
                    }
                }
            }
            radius *= -1.0f;
        }

        sprites.clear();
        final float headScale = 1.6f;
        final float tailScale = 0.8f;

        for (int k = 0; k < orbsCount; k++) {
            ArrayDeque<Vec3> q = orbTrails[k];
            if (q == null || q.isEmpty()) continue;

            int idx = 0;
            int qSize = q.size();
            for (Vec3 p : q) {
                float inv = 1f - (float) idx / Math.max(1, qSize - 1);
                float noise = 0.9f + 0.1f * (float) Math.sin(idx * 18.9898 + k * 28.233);
                float sizeScale = tailScale + (headScale - tailScale) * (float) Math.pow(inv, 1.1);
                float dynSize = baseSizePx * 0.35f * sizeScale * 0.5F;

                int finalAlpha = Mth.clamp((int) (255f * aPC * Math.pow(inv, 0.5) * noise), 0, 255);
                int mixed = ColorUtil.interpolateColor(baseCol, hitColor, hurtPC);
                int color = ColorUtil.withAlpha(mixed, finalAlpha);

                sprites.add(new Sprite(p, dynSize, color));
                if (saturation) {
                    sprites.add(new Sprite(p, dynSize * 1.3F, ColorUtil.withAlpha(color, (int) (finalAlpha * 0.5F))));
                }

                if (++idx >= trailLength) break;
            }
        }

        if (!sprites.isEmpty()) {
            particlesWorldRenderer.render(sprites, 1.0F, true, true);
        }
    }

    public void renderSouls(LivingEntity target, float alphaProgress, float tickDelta,
                            int numOrbs, float baseSizePx, float radiusConst, float speed,
                            boolean saturation, boolean colorOnHit, int hitColor, String colorMode, int customColor) {
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            reset();
            return;
        }

        initTrails();

        boolean hasTarget = target != null && target.isAlive() && !target.isRemoved();
        if (hasTarget) {
            lastTarget = target;
            lastTargetPos = Render3DUtil.interpolatedPosition(target, tickDelta);
            lastTargetHeight = target.getBbHeight();
        }

        LivingEntity renderTarget = hasTarget ? target : lastTarget;
        Vec3 vec;
        float height;
        if (renderTarget != null && renderTarget.isAlive()) {
            vec = Render3DUtil.interpolatedPosition(renderTarget, tickDelta);
            height = renderTarget.getBbHeight();
            lastTargetPos = vec;
            lastTargetHeight = height;
        } else if (lastTargetPos != null) {
            vec = lastTargetPos;
            height = lastTargetHeight;
        } else {
            return;
        }

        float aPC = alphaProgress;
        if (aPC <= 0.001f) return;

        long currentMs = System.currentTimeMillis();
        if (lastTimeMs == 0L) lastTimeMs = currentMs;
        double deltaSec = Mth.clamp((currentMs - lastTimeMs) / 1000.0D, 0.0D, 0.1D);
        lastTimeMs = currentMs;
        stableTimeSec += deltaSec * Math.max(0.1F, speed);

        double bx = vec.x;
        double by = vec.y;
        double bz = vec.z;

        final float targetHeight = Math.max(0.6f, height);
        final int soulsCount = 3;
        final int trailLength = 16;

        float hurtPC = 0.0F;
        if (colorOnHit && renderTarget != null && renderTarget.hurtTime > 0) {
            hurtPC = (float) Math.sin((double) renderTarget.hurtTime * (Math.PI / 20.0D));
        }

        float targetWidth = renderTarget != null ? renderTarget.getBbWidth() : 0.6F;
        float bodyRadius = (targetWidth * 0.9F + 0.25F) * (radiusConst / 0.35F);

        double t = stableTimeSec;
        double[] soulX = new double[soulsCount];
        double[] soulY = new double[soulsCount];
        double[] soulZ = new double[soulsCount];

        for (int k = 0; k < soulsCount; k++) {
            double kAngle = k * (2.0D * Math.PI / soulsCount);
            double kOffset1 = k * 2.399963D;
            double kOffset2 = k * 4.188790D;
            double kOffset3 = k * 1.570796D;

            double wanderAngle = Math.sin(t * 0.95D + kOffset1) * 0.38D + Math.cos(t * 0.55D + kOffset2) * 0.22D;
            double orbitAngle = t * 1.5D + kAngle + wanderAngle;

            double radNoise = 0.80D
                    + 0.18D * Math.sin(t * 1.25D + kOffset2)
                    + 0.10D * Math.cos(t * 0.75D + kOffset1);
            double curRadius = bodyRadius * Math.max(0.45D, radNoise);

            double normHeight = 0.50D
                    + 0.38D * Math.sin(t * 1.15D + kAngle + Math.sin(t * 0.6D + kOffset1) * 0.25D)
                    + 0.10D * Math.cos(t * 1.9D + kOffset3);
            normHeight = Mth.clamp(normHeight, 0.05D, 1.05D);
            double baseY = by + 0.08D + (targetHeight * 0.95D) * normHeight;

            double flutterX = bodyRadius * 0.15D * (Math.sin(t * 2.7D + kOffset1) + Math.cos(t * 3.9D + kOffset2));
            double flutterZ = bodyRadius * 0.15D * (Math.cos(t * 2.5D + kOffset2) + Math.sin(t * 4.3D + kOffset3));
            double flutterY = targetHeight * 0.08D * Math.sin(t * 3.1D + kOffset1);

            soulX[k] = bx + Math.cos(orbitAngle) * curRadius + flutterX;
            soulZ[k] = bz + Math.sin(orbitAngle) * curRadius + flutterZ;
            soulY[k] = baseY + flutterY;
        }

        for (int k = 0; k < soulsCount; k++) {
            if (currentMs - lastSoulSampleTime >= 16) {
                pushSoulPoint(k, soulX[k], soulY[k], soulZ[k], trailLength);
                if (k == soulsCount - 1) {
                    lastSoulSampleTime = currentMs;
                }
            }
        }

        sprites.clear();
        final float headScale = 1.9f;
        final float tailScale = 0.5f;

        for (int k = 0; k < soulsCount; k++) {
            ArrayDeque<Vec3> q = soulTrails[k];
            if (q == null || q.isEmpty()) continue;

            int idx = 0;
            int qSize = q.size();
            int baseCol = resolveColor(colorMode, customColor, k * 120);

            for (Vec3 p : q) {
                float inv = 1f - (float) idx / Math.max(1, qSize - 1);
                float ease = (float) Math.pow(inv, 0.7);
                float sizeScale = tailScale + (headScale - tailScale) * ease;
                float dynSize = baseSizePx * 0.38f * sizeScale * 0.5F;

                int finalAlpha = Mth.clamp((int) (255f * aPC * ease), 0, 255);
                int mixed = ColorUtil.interpolateColor(baseCol, hitColor, hurtPC);
                int color = ColorUtil.withAlpha(mixed, finalAlpha);

                sprites.add(new Sprite(p, dynSize, color));
                if (idx <= 2) {
                    sprites.add(new Sprite(p, dynSize * 0.6F, ColorUtil.withAlpha(0xFFFFFFFF, (int) (finalAlpha * 0.85F))));
                }
                if (saturation) {
                    sprites.add(new Sprite(p, dynSize * 1.2F, ColorUtil.withAlpha(color, (int) (finalAlpha * 0.4F))));
                }

                if (++idx >= trailLength) break;
            }
        }

        if (!sprites.isEmpty()) {
            particlesWorldRenderer.render(sprites, 1.0F, true, true);
        }
    }

    private void pushOrbPoint(int index, double x, double y, double z, int maxPoints) {
        if (index < 0 || index >= MAX_ORBS) return;
        ArrayDeque<Vec3> q = orbTrails[index];
        if (q == null) {
            q = new ArrayDeque<>();
            orbTrails[index] = q;
        }
        q.addFirst(new Vec3(x, y, z));
        while (q.size() > maxPoints) {
            q.removeLast();
        }
    }

    private void pushSoulPoint(int index, double x, double y, double z, int maxPoints) {
        if (index < 0 || index >= MAX_ORBS) return;
        ArrayDeque<Vec3> q = soulTrails[index];
        if (q == null) {
            q = new ArrayDeque<>();
            soulTrails[index] = q;
        }
        q.addFirst(new Vec3(x, y, z));
        while (q.size() > maxPoints) {
            q.removeLast();
        }
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
}
