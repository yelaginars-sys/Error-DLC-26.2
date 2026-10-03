package error.ui.modern;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

import java.awt.Color;

public final class ModernTheme {
    public static final int[] ACCENTS = new int[]{
        ColorUtil.rgba(177, 140, 255, 255),
        ColorUtil.rgba(125, 217, 250, 255),
        ColorUtil.rgba(222, 155, 53, 255),
        ColorUtil.rgba(75, 105, 255, 255),
        ColorUtil.rgba(226, 87, 76, 255),
        ColorUtil.rgba(105, 205, 130, 255)
    };

    private static int cachedAccent = 0;
    private static float cachedHue = 0.0F;

    public static final int DANGER = ColorUtil.rgba(214, 106, 128, 255);
    public static final float FONT_SCALE = 1.3F;
    private static boolean enableAnimations = true;
    private static boolean enableShadow = true;

    private ModernTheme() {
    }

    public static int accent() {
        return Theme.getAccentColor();
    }

    public static boolean animations() {
        return enableAnimations;
    }

    public static void setAnimations(boolean value) {
        enableAnimations = value;
    }

    public static boolean shadow() {
        return enableShadow;
    }

    public static void setShadow(boolean value) {
        enableShadow = value;
    }

    public static float windowOpacity() {
        return 0.92F;
    }

    public static float surfaceAlpha() {
        return 0.85F;
    }

    public static float blur() {
        return 20.0F;
    }

    public static int DARK_BLUR_TINT() {
        return ColorUtil.rgba(10, 12, 18, 140);
    }

    public static int LIGHT_BLUR_TINT() {
        return ColorUtil.rgba(20, 24, 36, 100);
    }

    private static float getHue() {
        int color = accent();
        if (cachedAccent != color) {
            float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
            cachedHue = hsb[0];
            cachedAccent = color;
        }
        return cachedHue;
    }

    private static int resolveColor(float saturation, float brightness) {
        int rgb = Color.HSBtoRGB(getHue(), saturation, brightness);
        return ColorUtil.rgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
    }

    public static int BG() {
        return resolveColor(0.28F, 0.11F);
    }

    public static int BG_BORDER() {
        return resolveColor(0.25F, 0.28F);
    }

    public static int SIDEBAR() {
        return resolveColor(0.32F, 0.08F);
    }

    public static int LINE() {
        return resolveColor(0.20F, 0.22F);
    }

    public static int LINE_SOFT() {
        return resolveColor(0.20F, 0.25F);
    }

    public static int CARD() {
        return resolveColor(0.24F, 0.16F);
    }

    public static int CARD_HOVER() {
        return resolveColor(0.24F, 0.22F);
    }

    public static int CARD_ON() {
        return resolveColor(0.30F, 0.21F);
    }

    public static int CARD_ON_HOVER() {
        return resolveColor(0.30F, 0.27F);
    }

    public static int CARD_BORDER() {
        return resolveColor(0.20F, 0.26F);
    }

    public static int CARD_BORDER_ON() {
        return ColorUtil.withAlpha(accent(), 180);
    }

    public static int TEXT() {
        return ColorUtil.rgba(245, 248, 255, 255);
    }

    public static int TEXT_DIM() {
        return ColorUtil.rgba(200, 208, 225, 255);
    }

    public static int TEXT_MUTED() {
        return ColorUtil.rgba(125, 134, 155, 255);
    }

    public static int TEXT_SOFT() {
        return ColorUtil.rgba(165, 175, 195, 255);
    }

    public static int TEXT_FAINT() {
        return ColorUtil.rgba(90, 98, 118, 255);
    }

    public static int ICON_IDLE() {
        return ColorUtil.rgba(140, 148, 170, 255);
    }

    public static int NAV_HOVER() {
        return resolveColor(0.25F, 0.18F);
    }

    public static int NAV_ACTIVE() {
        return resolveColor(0.35F, 0.22F);
    }

    public static int PANEL() {
        return resolveColor(0.28F, 0.12F);
    }

    public static int PANEL_BORDER() {
        return resolveColor(0.20F, 0.26F);
    }

    public static int FIELD() {
        return resolveColor(0.20F, 0.14F);
    }

    public static int TRACK() {
        return resolveColor(0.18F, 0.18F);
    }

    public static int TOGGLE_OFF() {
        return ColorUtil.rgba(35, 40, 55, 255);
    }

    public static int KNOB_OFF() {
        return ColorUtil.rgba(140, 148, 168, 255);
    }

    public static int PILL() {
        return resolveColor(0.22F, 0.15F);
    }

    public static float R_PANEL() {
        return 9.0F;
    }

    public static float R_CARD() {
        return 6.0F;
    }

    public static float R_ROW() {
        return 4.5F;
    }

    public static float radius(float base) {
        return base;
    }
}
