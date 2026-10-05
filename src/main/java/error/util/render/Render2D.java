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

    // ===================== LIQUID GLASS =====================

    public static void drawLiquidGlass(float x, float y, float width, float height, float radius, float alpha) {
        drawLiquidGlass(x, y, width, height, radius, alpha, Theme.getAccentColor());
    }

    public static void drawLiquidGlass(float x, float y, float width, float height, float radius, float alpha, int accentColor) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;

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
        drawHudPill(x, y, width, height, alpha, Theme.getAccentColor());
    }

    public static void drawHudPill(float x, float y, float width, float height, float alpha, int accentColor) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;
        float radius = height / 2.0F;
        Render2D.drawShadow(x, y, width, height, radius, 5.0F, ColorUtil.rgba(0, 0, 0, (int) (60 * alpha)));
        drawLiquidGlass(x, y, width, height, radius, alpha, accentColor);
    }

    public static void drawHudCard(float x, float y, float width, float height, float radius, float alpha) {
        drawHudCard(x, y, width, height, radius, alpha, Theme.getAccentColor());
    }

    public static void drawHudCard(float x, float y, float width, float height, float radius, float alpha, int accentColor) {
        if (alpha <= 0.001F || width <= 0.0F || height <= 0.0F) return;
        Render2D.drawShadow(x, y, width, height, radius, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (65 * alpha)));
        drawLiquidGlass(x, y, width, height, radius, alpha, accentColor);
    }
}