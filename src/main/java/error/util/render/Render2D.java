package error.util.render;

import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import error.util.RenderExtend;
import error.util.render.menu.BlurRectRenderState;
import error.util.render.menu.CircleArcRenderState;
import error.util.render.renders.MenuBacks;
import error.util.render.menu.RectRenderState;
import error.util.render.menu.TextureRenderState;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.display.batch.DisplayBatcher;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;

/**
 */
public class Render2D {

    public static final Identifier FROST_CORNER_TEX = Identifier.fromNamespaceAndPath("error", "textures/frost/frost_corner.png");
    public static final Identifier FROST_PATTERN_TEX = Identifier.fromNamespaceAndPath("error", "textures/frost/frost_pattern.png");

    public static void pushScissor(float x, float y, float width, float height) {
        Render2DUtil.pushScissor(x, y, width, height);
    }

    public static void popScissor() {
        Render2DUtil.popScissor();
    }

    public static void drawRect(float x, float y, float width, float height, int color) {
        drawRoundedRect(x, y, width, height, 0.0F, color);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        drawRoundedRectComplex(x, y, width, height, radius, radius, radius, radius, color, color, color, color, 0.0F, 0, 0.0F, 0, false);
    }

    public static void drawRoundedOutline(float x, float y, float width, float height, float radius, float thickness, int borderColor) {
        drawRoundedRectComplex(x, y, width, height, radius, radius, radius, radius, 0, 0, 0, 0, thickness, borderColor, 0.0F, 0, false);
    }

    public static void drawRoundedRectWithOutline(float x, float y, float width, float height, float radius, int fillColor, float thickness, int borderColor) {
        drawRoundedRectComplex(x, y, width, height, radius, radius, radius, radius, fillColor, fillColor, fillColor, fillColor, thickness, borderColor, 0.0F, 0, false);
    }

    public static void drawGradientRound(float x, float y, float width, float height, float radius, int cTL, int cTR, int cBR, int cBL) {
        drawRoundedRectComplex(x, y, width, height, radius, radius, radius, radius, cTL, cTR, cBR, cBL, 0.0F, 0, 0.0F, 0, false);
    }

    public static void drawShadow(float x, float y, float width, float height, float radius, float blur, int shadowColor) {
        drawRoundedRectComplex(x, y, width, height, radius, radius, radius, radius, 0, 0, 0, 0, 0.0F, 0, blur, shadowColor, true);
    }


    public static void drawCircle(float cx, float cy, float radius, int color) {
        drawArc(cx, cy, radius, radius, 0.0F, 360.0F, color);
    }


    public static void drawCircleOutline(float cx, float cy, float radius, float thickness, int color) {
        drawArc(cx, cy, radius, thickness, 0.0F, 360.0F, color);
    }


    public static void drawArc(float cx, float cy, float radius, float thickness, float startAngleDeg, float sweepAngleDeg, int color) {
        if (Render2DUtil.hasEmptyScissor() || Math.abs(sweepAngleDeg) <= 0.01F || radius <= 0.0F) return;
        GuiGraphicsExtractor extractor = RenderExtend.currentGuiGraphicsExtractor();
        if (extractor == null) return;

        Render2DUtil.queue(new CircleArcRenderState(
                extractor.pose(),
                cx, cy, radius, thickness,
                startAngleDeg, sweepAngleDeg,
                color,
                Render2DUtil.currentScissor()
        ));
    }

    public static void drawBlur(float x, float y, float width, float height, float radius, int tintColor, float opacity) {
        drawBlur(x, y, width, height, radius, 16.0F, tintColor, opacity);
    }

    public static void drawBlur(float x, float y, float width, float height, float radius, float blurRadius, int tintColor, float opacity) {
        if (Render2DUtil.hasEmptyScissor()) return;
        GuiGraphicsExtractor extractor = RenderExtend.currentGuiGraphicsExtractor();
        if (extractor == null) return;

        var backdropView = MenuBacks.acquireView();
        if (backdropView != null) {
            int guiScale = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());

            int rawAlpha = (tintColor >>> 24) & 0xFF;
            int tintAlpha = Math.round(rawAlpha * 0.8F);
            int safeTint = (tintColor & 0x00FFFFFF) | (tintAlpha << 24);

            Render2DUtil.queue(new BlurRectRenderState(
                    extractor.pose(),
                    x, y, x + width, y + height,
                    safeTint,
                    radius,
                    blurRadius * guiScale,
                    opacity,
                    backdropView,
                    Render2DUtil.currentScissor()
            ));
        }
    }

    public static void drawTexture(String path, float x, float y, float width, float height) {
        drawTexture(path, x, y, width, height, 0.0F, 0xFFFFFFFF);
    }

    public static void drawTexture(String path, float x, float y, float width, float height, float radius, int color) {
        Identifier id = Identifier.fromNamespaceAndPath("error", path);
        drawTexture(id, x, y, width, height, radius, color);
    }

    public static void drawTexture(Identifier texture, float x, float y, float width, float height) {
        drawTexture(texture, x, y, width, height, 0.0F, 0xFFFFFFFF);
    }

    public static void drawTexture(Identifier texture, float x, float y, float width, float height, int color) {
        drawTexture(texture, x, y, width, height, 0.0F, color);
    }

    public static void drawTexture(Identifier texture, float x, float y, float width, float height, float radius, int color) {
        drawTexture(texture, x, y, width, height, 0.0F, 0.0F, 1.0F, 1.0F, radius, color, FilterMode.LINEAR);
    }

    public static void drawTexture(Identifier texture, float x, float y, float width, float height,
                                   float u0, float v0, float u1, float v1, float radius, int color, FilterMode filterMode) {
        if (Render2DUtil.hasEmptyScissor()) return;
        GuiGraphicsExtractor extractor = RenderExtend.currentGuiGraphicsExtractor();
        if (extractor == null || texture == null) return;

        Render2DUtil.queue(new TextureRenderState(
                extractor.pose(),
                texture,
                x, y, width, height,
                u0, v0, u1, v1,
                color, radius, filterMode,
                Render2DUtil.currentScissor()
        ));
    }

    public static void drawHead(Identifier skin, float x, float y, float size) {
        drawHead(skin, x, y, size, 0.0F, 0xFFFFFFFF);
    }

    public static void drawHead(Identifier skin, float x, float y, float size, float radius) {
        drawHead(skin, x, y, size, radius, 0xFFFFFFFF);
    }

    public static void drawHead(Identifier skin, float x, float y, float size, float radius, float alpha) {
        int tint = error.util.client.clients.ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        drawHead(skin, x, y, size, radius, tint);
    }

    public static void drawHead(net.minecraft.client.player.AbstractClientPlayer player, float x, float y, float size, float radius, float alpha) {
        if (player == null) return;
        Identifier skin = player.getSkin().body().texturePath();
        int tint = error.util.client.clients.ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        drawHead(skin, x, y, size, radius, tint);
    }

    private static final Identifier BUNDLED_AVATAR = Identifier.fromNamespaceAndPath("error", "textures/custom_avatar.png");
    private static Identifier customAvatarIdentifier = null;

    public static Identifier getCustomAvatarTexture() {
        if (customAvatarIdentifier != null) return customAvatarIdentifier;
        customAvatarIdentifier = BUNDLED_AVATAR;
        return customAvatarIdentifier;
    }

    public static void drawCustomAvatar(float x, float y, float size, float radius, float alpha) {
        Identifier avatar = getCustomAvatarTexture();
        int tint = error.util.client.clients.ColorUtil.applyAlpha(0xFFFFFFFF, alpha);
        if (avatar != null) {
            drawTexture(avatar, x, y, size, size, 0.0F, 0.0F, 1.0F, 1.0F, radius, tint, FilterMode.LINEAR);
        } else if (Minecraft.getInstance().player != null) {
            drawHead(Minecraft.getInstance().player, x, y, size, radius, alpha);
        }
    }

    public static void drawHead(Identifier skin, float x, float y, float size, float radius, int tint) {
        if (skin == null || size <= 0) return;

        drawTexture(skin, x, y, size, size,
                8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F,
                radius, tint, FilterMode.NEAREST);

        drawTexture(skin, x, y, size, size,
                40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F,
                radius, tint, FilterMode.NEAREST);
    }

    public static void drawRoundedRectComplex(
            float x, float y, float w, float h,
            float rTL, float rTR, float rBR, float rBL,
            int cTL, int cTR, int cBR, int cBL,
            float borderThickness, int borderColor,
            float shadowBlur, int shadowColor, boolean isShadow
    ) {
        if (Render2DUtil.hasEmptyScissor()) return;
        GuiGraphicsExtractor extractor = RenderExtend.currentGuiGraphicsExtractor();
        if (extractor == null) return;

        Render2DUtil.queue(new RectRenderState(
                extractor.pose(),
                x, y, x + w, y + h,
                cTL, cBL, cBR, cTR,
                rTL, rTR, rBR, rBL,
                borderThickness, borderColor,
                shadowBlur, shadowColor, isShadow,
                Render2DUtil.currentScissor()
        ));
    }

    // ===================== GLASS =====================

    public static void glass(GuiGraphicsExtractor gg, float x, float y, float w, float h, float r,
                             float blurRadius, float alphaFactor, float smoothing, float tintAlpha) {
        glass(gg, x, y, w, h, r, blurRadius, alphaFactor, smoothing, 0f, 0f, 0f, tintAlpha);
    }

    public static void glass(GuiGraphicsExtractor gg, float x, float y, float w, float h, float r,
                             float blurRadius, float alphaFactor, float smoothing,
                             float red, float green, float blue, float tintAlpha) {
        if (alphaFactor <= 0.001f || w <= 0.0f || h <= 0.0f) return;
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.max(1, Math.round(w));
        int ih = Math.max(1, Math.round(h));
        float radius = Math.min(r, Math.min(iw, ih) / 2.0f);
        error.util.render.pipeline.HudBlurPipeline.requestGlass(ix, iy, iw, ih, radius,
                blurRadius > 0.0f ? blurRadius : 40.0f, alphaFactor, smoothing,
                red, green, blue, tintAlpha);
    }

    public static void glass(float x, float y, float width, float height,
                             float alpha, float radius, int tintColor,
                             float distortion, float waveSize, float edgeLight, float shine) {
        glass(null, x, y, width, height, radius, 32.0F, alpha, 26.0F, 0.0F);
    }

    public static void glass(float x, float y, float width, float height,
                             float alpha, float radius,
                             float distortion, float waveSize) {
        glass(null, x, y, width, height, radius, 32.0F, alpha, 26.0F, 0.0F);
    }

    public static void glass(float x, float y, float width, float height,
                             float alpha, float topLeft, float topRight, float bottomRight, float bottomLeft,
                             int tintColor, float distortion, float waveSize, float edgeLight, float shine) {
        glass(null, x, y, width, height, (topLeft + topRight + bottomRight + bottomLeft) / 4.0F, 32.0F, alpha, 26.0F, 0.0F);
    }

    public static void glass(float x1, float y1, float width1, float height1,
                             float x2, float y2, float width2, float height2,
                             float alpha, float radius, int tintColor,
                             float distortion, float waveSize, float edgeLight, float shine,
                             float mergeRadius) {
        float minX = Math.min(x1, x2);
        float minY = Math.min(y1, y2);
        float maxX = Math.max(x1 + width1, x2 + width2);
        float maxY = Math.max(y1 + height1, y2 + height2);
        glass(null, minX, minY, maxX - minX, maxY - minY, radius, 32.0F, alpha, 26.0F, 0.0F);
    }

    // ===================== LIQUID GLASS & HUD CARDS =====================

    public static void drawLiquidGlass(float x, float y, float width, float height, float radius, float alpha) {
        drawLiquidGlass(x, y, width, height, radius, alpha, Theme.getAccentColor());
    }

    public static void drawLiquidGlass(float x, float y, float width, float height, float radius, float alpha, int accentColor) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;

        // If non-liquid theme is selected, route to the appropriate theme card
        if (Theme.isBlack() || Theme.isNewYear()) {
            drawHudCard(null, x, y, width, height, radius, alpha, accentColor);
            return;
        }

        // 1. Exact frosted Kawase Blur & Specular Outline from LiquidClickGui
        GuiGraphicsExtractor extractor = RenderExtend.currentGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            float shellRadius = Math.min(radius, Math.min(width, height) * 0.5F);
            try {
                Blur.of(x, y, width, height)
                        .radius(shellRadius)
                        .type(BlurType.KAWASE)
                        .strength(4)
                        .tint(Color.rgba(0, 0, 0, Math.round(75 * alpha)))
                        .alpha(alpha)
                        .render(extractor);

                Outline.of(x, y, width, height)
                        .radius(shellRadius)
                        .thickness(1.0F)
                        .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 32))
                        .alpha(alpha)
                        .render(extractor);

                DisplayBatcher.flush();
            } catch (Throwable ignored) {}
        }

        // 2. Pure Frosted Glass Fill & Accent Border matching ClickGUI
        int glassFill = ColorUtil.rgba(255, 255, 255, (int) (14 * alpha));
        int glassOutline = ColorUtil.rgba(255, 255, 255, (int) (22 * alpha));

        drawRoundedRect(x, y, width, height, radius, glassFill);
        drawRoundedOutline(x, y, width, height, radius, 0.65F, glassOutline);
    }

    public static void drawHudPill(float x, float y, float width, float height, float alpha) {
        drawHudPill(null, x, y, width, height, height / 2.0F, alpha, Theme.getAccentColor());
    }

    public static void drawHudPill(float x, float y, float width, float height, float alpha, int accentColor) {
        drawHudPill(null, x, y, width, height, height / 2.0F, alpha, accentColor);
    }

    public static void drawHudPill(GuiGraphicsExtractor extractor, float x, float y, float width, float height, float radius, float alpha) {
        drawHudPill(extractor, x, y, width, height, radius, alpha, Theme.getAccentColor());
    }

    public static void drawHudPill(GuiGraphicsExtractor extractor, float x, float y, float width, float height, float radius, float alpha, int accentColor) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;

        if (Theme.isBlack()) {
            // Clean Solid Black Minimal capsule
            Render2D.drawShadow(x, y, width, height, radius, 2.0F, ColorUtil.rgba(0, 0, 0, (int) (40 * alpha)));
            drawRoundedRect(x, y, width, height, radius, ColorUtil.rgba(13, 13, 17, (int) (235 * alpha)));
            drawRoundedOutline(x, y, width, height, radius, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (14 * alpha)));
        } else if (Theme.isNewYear()) {
            // New Year: Liquid Glass with Frost Shader / Ice Crystalline Effect
            Render2D.drawShadow(x, y, width, height, radius, 2.5F, ColorUtil.rgba(0, 0, 0, (int) (25 * alpha)));
            GuiGraphicsExtractor ext = extractor != null ? extractor : RenderExtend.currentGuiGraphicsExtractor();
            if (ext != null) {
                Render2DUtil.flush();
                try {
                    Blur.of(x, y, width, height)
                            .radius(Math.round(radius))
                            .type(BlurType.KAWASE)
                            .strength(3)
                            .tint(Color.rgba(10, 24, 42, (int) (115 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    Outline.of(x, y, width, height)
                            .radius(Math.round(radius))
                            .thickness(0.80F)
                            .verticalGradient(Color.rgba(220, 248, 255, (int) (165 * alpha)), Color.rgba(130, 205, 255, (int) (50 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    DisplayBatcher.flush();
                } catch (Throwable ignored) {}
            }
            drawFrostSheen(x, y, width, height, radius, alpha);
        } else {
            // Liquid Glass (default)
            Render2D.drawShadow(x, y, width, height, radius, 2.2F, ColorUtil.rgba(0, 0, 0, (int) (28 * alpha)));
            GuiGraphicsExtractor ext = extractor != null ? extractor : RenderExtend.currentGuiGraphicsExtractor();
            if (ext != null) {
                Render2DUtil.flush();
                try {
                    Blur.of(x, y, width, height)
                            .radius(Math.round(radius))
                            .type(BlurType.KAWASE)
                            .strength(3)
                            .tint(Color.rgba(14, 16, 22, (int) (110 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    Outline.of(x, y, width, height)
                            .radius(Math.round(radius))
                            .thickness(0.70F)
                            .verticalGradient(Color.rgba(255, 255, 255, (int) (28 * alpha)), Color.rgba(255, 255, 255, (int) (6 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    DisplayBatcher.flush();
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void drawHudCard(float x, float y, float width, float height, float radius, float alpha) {
        drawHudCard(null, x, y, width, height, radius, alpha, Theme.getAccentColor());
    }

    public static void drawHudCard(float x, float y, float width, float height, float radius, float alpha, int accentColor) {
        drawHudCard(null, x, y, width, height, radius, alpha, accentColor);
    }

    public static void drawHudCard(GuiGraphicsExtractor extractor, float x, float y, float width, float height, float radius, float alpha) {
        drawHudCard(extractor, x, y, width, height, radius, alpha, Theme.getAccentColor());
    }

    public static void drawHudCard(GuiGraphicsExtractor extractor, float x, float y, float width, float height, float radius, float alpha, int accentColor) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;

        if (Theme.isBlack()) {
            // Clean Solid Black Minimal card
            Render2D.drawShadow(x, y, width, height, radius, 2.5F, ColorUtil.rgba(0, 0, 0, (int) (48 * alpha)));
            drawRoundedRect(x, y, width, height, radius, ColorUtil.rgba(13, 13, 17, (int) (240 * alpha)));
            drawRoundedOutline(x, y, width, height, radius, 0.70F, ColorUtil.rgba(255, 255, 255, (int) (15 * alpha)));
        } else if (Theme.isNewYear()) {
            // New Year: Liquid Glass with Frost Shader / Ice Crystalline Effect
            Render2D.drawShadow(x, y, width, height, radius, 3.2F, ColorUtil.rgba(0, 0, 0, (int) (30 * alpha)));
            GuiGraphicsExtractor ext = extractor != null ? extractor : RenderExtend.currentGuiGraphicsExtractor();
            if (ext != null) {
                Render2DUtil.flush();
                float shellRadius = Math.min(radius, Math.min(width, height) * 0.5F);
                try {
                    Blur.of(x, y, width, height)
                            .radius(shellRadius)
                            .type(BlurType.KAWASE)
                            .strength(4)
                            .tint(Color.rgba(10, 24, 42, (int) (125 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    Outline.of(x, y, width, height)
                            .radius(shellRadius)
                            .thickness(0.85F)
                            .verticalGradient(Color.rgba(225, 248, 255, (int) (185 * alpha)), Color.rgba(135, 205, 255, (int) (55 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    DisplayBatcher.flush();
                } catch (Throwable ignored) {}
            }
            drawFrostSheen(x, y, width, height, radius, alpha);
        } else {
            // Liquid Glass (default)
            Render2D.drawShadow(x, y, width, height, radius, 3.0F, ColorUtil.rgba(0, 0, 0, (int) (32 * alpha)));
            GuiGraphicsExtractor ext = extractor != null ? extractor : RenderExtend.currentGuiGraphicsExtractor();
            if (ext != null) {
                Render2DUtil.flush();
                float shellRadius = Math.min(radius, Math.min(width, height) * 0.5F);
                try {
                    Blur.of(x, y, width, height)
                            .radius(shellRadius)
                            .type(BlurType.KAWASE)
                            .strength(4)
                            .tint(Color.rgba(14, 16, 22, (int) (120 * alpha)))
                            .alpha(alpha)
                            .render(ext);

                    Outline.of(x, y, width, height)
                            .radius(shellRadius)
                            .thickness(0.8F)
                            .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 30))
                            .alpha(alpha)
                            .render(ext);

                    DisplayBatcher.flush();
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void drawFrostSheen(float x, float y, float width, float height, float radius, float alpha) {
        if (alpha <= 0.01F || width <= 2.0F || height <= 2.0F) return;
        long time = System.currentTimeMillis();

        // 1. Crystalline icy top edge glint (ice rim light along upper bezel)
        float rimW = Math.max(2.0F, width - radius * 1.2F);
        float rimX = x + (width - rimW) / 2.0F;
        drawRoundedRect(rimX, y + 0.6F, rimW, 1.2F, 0.6F, ColorUtil.rgba(240, 252, 255, (int) (165 * alpha)));

        // 2. Secondary soft crystalline gradient glow under top edge
        drawRoundedRect(rimX, y + 1.8F, rimW, 2.2F, 1.0F, ColorUtil.rgba(180, 230, 255, (int) (40 * alpha)));

        // 3. Natural frost feather crystals on corners/borders
        float cornerFrostSize = Math.min(width * 0.45F, Math.min(height * 0.9F, 52.0F));
        if (cornerFrostSize > 12.0F) {
            // Top-Left organic frost crystal plume
            int frostColTL = ColorUtil.rgba(235, 248, 255, (int) (130 * alpha));
            drawTexture(FROST_CORNER_TEX, x, y, cornerFrostSize, cornerFrostSize, frostColTL);

            // Bottom-Right organic frost crystal plume (flipped UV)
            if (width > 60.0F && height > 35.0F) {
                float brSize = cornerFrostSize * 0.85F;
                int frostColBR = ColorUtil.rgba(215, 242, 255, (int) (100 * alpha));
                drawTexture(FROST_CORNER_TEX, x + width - brSize, y + height - brSize, brSize, brSize,
                        1.0F, 1.0F, 0.0F, 0.0F, 0.0F, frostColBR, FilterMode.LINEAR);
            }
        }

        // 4. For large panels (ClickGUI window, modals): diffuse subtle frost pattern across surface
        if (width >= 160.0F && height >= 100.0F) {
            float patW = Math.min(width * 0.65F, 180.0F);
            float patH = Math.min(height * 0.65F, 180.0F);
            float patX = x + width - patW - 8.0F;
            float patY = y + 8.0F;
            int patCol = ColorUtil.rgba(220, 245, 255, (int) (48 * alpha));
            drawTexture(FROST_PATTERN_TEX, patX, patY, patW, patH, patCol);
        }

        // 5. Breathing glacial glint across upper glass edge (soft sine shimmer instead of harsh vertical stripe)
        float shimmerPhase = (float) ((time % 4500L) / 4500.0D);
        float shimmerAlpha = (float) Math.sin(shimmerPhase * Math.PI) * 0.35F * alpha;
        if (shimmerAlpha > 0.01F) {
            float sLen = Math.min(width * 0.5F, 90.0F);
            float sStartX = x + (width - sLen) * shimmerPhase;
            drawRoundedRect(sStartX, y + 0.8F, sLen, 1.4F, 0.7F,
                    ColorUtil.rgba(255, 255, 255, (int) (255 * shimmerAlpha)));
        }

        // 6. Delicate frosty corner sparkles
        float pulse1 = 0.5F + 0.5F * (float) Math.sin(time * 0.0028F);
        int sparkleColor1 = ColorUtil.rgba(225, 248, 255, (int) ((70 + 60 * pulse1) * alpha));
        drawCircle(x + Math.min(radius, 6.0F) + 1.0F, y + Math.min(radius, 6.0F) + 1.0F, 1.2F, sparkleColor1);

        if (width > 80.0F) {
            float pulse2 = 0.5F + 0.5F * (float) Math.cos(time * 0.0033F + 1.5D);
            int sparkleColor2 = ColorUtil.rgba(210, 242, 255, (int) ((50 + 55 * pulse2) * alpha));
            drawCircle(x + width - Math.min(radius, 6.0F) - 1.5F, y + Math.min(radius, 6.0F) + 1.0F, 1.0F, sparkleColor2);
        }
    }
}