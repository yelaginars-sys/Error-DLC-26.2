package error.ui.mainmenu;

import error.ui.nova.NovaShader;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated Night-Sky Live Menu background renderer.
 * Features: deep space gradient, twinkling stars, procedural shooting stars,
 * nebula glow clouds, aurora shimmer, procedural falling snow.
 */
public final class WinterMenuRenderer {

    private static final int STAR_COUNT   = 160;
    private static final int SNOW_COUNT   = 110;
    private static final int NEBULA_COUNT = 5;

    private static final List<Star>      STARS     = new ArrayList<>();
    private static final List<Snowflake> SNOWFLAKES = new ArrayList<>();
    private static final List<Nebula>    NEBULAE   = new ArrayList<>();
    private static final Random          RANDOM    = new Random();

    private static boolean initialized = false;
    private static long    startTime   = System.currentTimeMillis();

    // ── Shooting star state ──────────────────────────────────────────────────
    private static float  meteorX, meteorY, meteorDX, meteorDY, meteorAlpha;
    private static long   nextMeteorTime = 0L;
    private static boolean meteorActive  = false;

    // ─────────────────────────────────────────────────────────────────────────

    private static class Star {
        float xRatio, yRatio;
        float size;
        float twinkleSpeed;
        float phaseOffset;
        int   baseAlpha;   // 120-230: dim vs bright stars

        Star(float xRatio, float yRatio, float size, float twinkleSpeed, float phaseOffset, int baseAlpha) {
            this.xRatio       = xRatio;
            this.yRatio       = yRatio;
            this.size         = size;
            this.twinkleSpeed = twinkleSpeed;
            this.phaseOffset  = phaseOffset;
            this.baseAlpha    = baseAlpha;
        }
    }

    private static class Snowflake {
        float xRatio, yRatio;
        float speed;
        float swaySpeed, swayWidth;
        float size, alpha;
        float depthScale;  // 0.4-1.0: parallax depth

        Snowflake(float xRatio, float yRatio, float speed, float swaySpeed,
                  float swayWidth, float size, float alpha, float depthScale) {
            this.xRatio     = xRatio;
            this.yRatio     = yRatio;
            this.speed      = speed;
            this.swaySpeed  = swaySpeed;
            this.swayWidth  = swayWidth;
            this.size       = size;
            this.alpha      = alpha;
            this.depthScale = depthScale;
        }

        void update() {
            yRatio += speed * depthScale;
            if (yRatio > 1.05f) {
                yRatio = -0.05f;
                xRatio = RANDOM.nextFloat();
            }
        }
    }

    private static class Nebula {
        float xRatio, yRatio;
        float wRatio, hRatio;
        int r, g, b;

        Nebula(float xRatio, float yRatio, float wRatio, float hRatio, int r, int g, int b) {
            this.xRatio = xRatio; this.yRatio = yRatio;
            this.wRatio = wRatio; this.hRatio = hRatio;
            this.r = r; this.g = g; this.b = b;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    public static void init(int width, int height) {
        if (initialized) return;

        // Stars – two layers: many dim + fewer bright
        STARS.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            boolean bright = RANDOM.nextFloat() < 0.25f;
            STARS.add(new Star(
                    RANDOM.nextFloat(),
                    RANDOM.nextFloat() * 0.75f,
                    bright ? (1.4f + RANDOM.nextFloat() * 1.8f) : (0.6f + RANDOM.nextFloat() * 1.2f),
                    0.0008f + RANDOM.nextFloat() * 0.0025f,
                    RANDOM.nextFloat() * 6.28f,
                    bright ? 180 + RANDOM.nextInt(50) : 80 + RANDOM.nextInt(80)
            ));
        }

        // Snow – two depth layers
        SNOWFLAKES.clear();
        for (int i = 0; i < SNOW_COUNT; i++) {
            float depth = 0.4f + RANDOM.nextFloat() * 0.6f;
            SNOWFLAKES.add(new Snowflake(
                    RANDOM.nextFloat(),
                    RANDOM.nextFloat(),
                    0.0006f + RANDOM.nextFloat() * 0.0018f,
                    0.0008f + RANDOM.nextFloat() * 0.0022f,
                    0.005f  + RANDOM.nextFloat() * 0.015f,
                    (1.2f + RANDOM.nextFloat() * 3.0f) * depth,
                    (0.25f + RANDOM.nextFloat() * 0.65f) * depth,
                    depth
            ));
        }

        // Nebulae – soft glow patches scattered across sky
        NEBULAE.clear();
        int[][] nebColors = {{60,40,120},{30,60,150},{90,30,100},{20,80,140},{70,50,130}};
        for (int i = 0; i < NEBULA_COUNT; i++) {
            NEBULAE.add(new Nebula(
                    RANDOM.nextFloat(),
                    RANDOM.nextFloat() * 0.6f,
                    0.25f + RANDOM.nextFloat() * 0.35f,
                    0.20f + RANDOM.nextFloat() * 0.25f,
                    nebColors[i][0], nebColors[i][1], nebColors[i][2]
            ));
        }

        initialized = true;
    }

    public static void render(int width, int height, float alpha, float cameraYOffset) {
        if (!initialized || width <= 0 || height <= 0) {
            init(width, height);
        }

        long  now     = System.currentTimeMillis();
        float timeSec = (now - startTime) / 1000.0f;

        // ── 1. Animated Liquid Glass Theme Backdrop ───────────────────────────
        error.ui.nova.NovaShader.drawBackdrop(null, 0, cameraYOffset, width, height, 0.0F);

        // ── 5. Twinkling stars ──────────────────────────────────────────────
        for (Star star : STARS) {
            float sx         = star.xRatio * width;
            float sy         = star.yRatio * height + cameraYOffset;
            float brightness = 0.3f + 0.7f * (float) Math.sin(now * star.twinkleSpeed + star.phaseOffset);
            brightness       = Mth.clamp(brightness, 0.05f, 1.0f);

            int starA     = (int)(star.baseAlpha * alpha * brightness);
            int starColor = ColorUtil.rgba(215, 235, 255, starA);
            Render2D.drawRoundedRect(sx, sy, star.size, star.size, star.size * 0.5f, starColor);

            // Bright-star cross flare
            if (star.size > 1.8f && brightness > 0.7f) {
                int flareA   = (int)(60 * alpha * brightness);
                int flareCol = ColorUtil.rgba(200, 225, 255, flareA);
                float fl     = star.size * 2.5f;
                Render2D.drawRoundedRect(sx - fl / 2f, sy + star.size / 2f - 0.4f, fl, 0.8f, 0.4f, flareCol);
                Render2D.drawRoundedRect(sx + star.size / 2f - 0.4f, sy - fl / 2f, 0.8f, fl, 0.4f, flareCol);
            }
        }

        // ── 6. Shooting star / Meteor ───────────────────────────────────────
        if (!meteorActive && now >= nextMeteorTime) {
            meteorActive   = true;
            meteorX        = RANDOM.nextFloat() * width * 0.6f;
            meteorY        = RANDOM.nextFloat() * height * 0.3f + cameraYOffset;
            float angle    = (float)(Math.PI / 4 + RANDOM.nextFloat() * Math.PI / 6);
            float spd      = width * 0.0012f;
            meteorDX       = (float) Math.cos(angle) * spd;
            meteorDY       = (float) Math.sin(angle) * spd;
            meteorAlpha    = 0.0f;
        }

        if (meteorActive) {
            meteorAlpha = Math.min(meteorAlpha + 0.04f, 1.0f);
            meteorX    += meteorDX;
            meteorY    += meteorDY;

            // Tail
            for (int t = 0; t < 10; t++) {
                float tx   = meteorX - meteorDX * t * 2.5f;
                float ty   = meteorY - meteorDY * t * 2.5f;
                float ta   = meteorAlpha * (1.0f - t / 10.0f) * alpha;
                int   tcol = ColorUtil.rgba(200, 225, 255, (int)(180 * ta));
                float ts   = (3.0f - t * 0.25f);
                if (ts > 0) Render2D.drawRoundedRect(tx, ty, ts, ts, ts * 0.5f, tcol);
            }

            // Deactivate when off screen
            if (meteorX > width * 1.1f || meteorY > height * 1.1f + cameraYOffset) {
                meteorActive  = false;
                nextMeteorTime = now + 3500L + (long)(RANDOM.nextFloat() * 6000L);
            }
        }

        // ── 7. Procedural falling snow ──────────────────────────────────────
        for (Snowflake flake : SNOWFLAKES) {
            flake.update();
            float sway = (float) Math.sin(timeSec * flake.swaySpeed * 900.0f) * flake.swayWidth * width;
            float fx   = (flake.xRatio * width + sway) % width;
            if (fx < 0) fx += width;
            float fy = flake.yRatio * height + cameraYOffset;

            int snowA   = (int)(220 * alpha * flake.alpha);
            int snowCol = ColorUtil.rgba(230, 242, 255, snowA);
            Render2D.drawRoundedRect(fx, fy, flake.size, flake.size, flake.size * 0.5f, snowCol);

            if (flake.size > 2.5f) {
                int glowA  = (int)(45 * alpha * flake.alpha);
                int glowCol= ColorUtil.rgba(190, 225, 255, glowA);
                Render2D.drawShadow(fx - 1f, fy - 1f, flake.size + 2f, flake.size + 2f, 2f, 3f, glowCol);
            }
        }
    }
}
