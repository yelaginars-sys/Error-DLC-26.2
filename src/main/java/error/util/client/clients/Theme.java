package error.util.client.clients;

import lombok.Getter;
import lombok.Setter;

public final class Theme {
    @Setter
    private static int accentColor = ColorUtil.rgba(0, 160, 255, 255);

    @Setter
    private static int secondaryColor = ColorUtil.rgba(140, 60, 255, 255);

    @Getter @Setter
    private static String accentMode = "Static";

    @Getter @Setter
    private static String backgroundMode = "None";

    @Getter @Setter
    private static String glassStyle = "AURORA";

    @Getter @Setter
    private static String uiStyle = "Жидкое стекло"; // "Жидкое стекло", "Новый Год"

    public static boolean isLiquidGlass() {
        return !"Новый Год".equalsIgnoreCase(uiStyle);
    }

    public static boolean isNewYear() {
        return "Новый Год".equalsIgnoreCase(uiStyle);
    }

    @Getter @Setter
    private static int bgColor1 = ColorUtil.rgba(18, 18, 24, 255);

    @Getter @Setter
    private static int bgColor2 = ColorUtil.rgba(12, 12, 16, 255);

    @Getter @Setter
    private static float panelAlpha = 0.90F;

    public static final int BG_SIDEBAR = ColorUtil.rgba(13, 13, 16, 255);
    public static final int BG_CARD = ColorUtil.rgba(20, 20, 26, 255);
    public static final int BG_CARD_ACTIVE = ColorUtil.rgba(24, 26, 34, 255);
    public static final int BG_ELEMENT = ColorUtil.rgba(26, 26, 33, 255);
    public static final int DIVIDER_COLOR = ColorUtil.rgba(255, 255, 255, 25);
    public static final int BG_COLOR = ColorUtil.rgba(16, 16, 20, 195);
    public static final int TEXT_MAIN = ColorUtil.rgba(245, 245, 250, 255);
    public static final int TEXT_MUTED = ColorUtil.rgba(130, 130, 140, 255);

    public static int getAccentColor() {
        if ("RGB".equalsIgnoreCase(accentMode) || "Chroma".equalsIgnoreCase(accentMode)) {
            float hue = (float) ((System.currentTimeMillis() % 4000L) / 4000.0);
            return ColorUtil.fromHsv(hue, 0.85f, 1.0f, 255);
        }
        return accentColor;
    }

    public static int getSecondaryColor() {
        if ("RGB".equalsIgnoreCase(accentMode) || "Chroma".equalsIgnoreCase(accentMode)) {
            float hue = (float) (((System.currentTimeMillis() + 1000L) % 4000L) / 4000.0);
            return ColorUtil.fromHsv(hue, 0.85f, 1.0f, 255);
        }
        return secondaryColor;
    }

    public static int getAccentWithAlpha(int alpha) {
        return ColorUtil.withAlpha(getAccentColor(), alpha);
    }

    public static int getGradientColor(int indexOffset) {
        int c1 = getAccentColor();
        int c2 = getSecondaryColor();
        if (c1 == c2) {
            c2 = ColorUtil.interpolateColor(c1, 0xFF101018, 0.45F);
        }
        long time = System.currentTimeMillis();
        int wave = (int) ((time / 5L + indexOffset) % 360L);
        if (wave < 0) wave += 360;
        float factor = (wave > 180 ? 360 - wave : wave) / 180.0F;
        return ColorUtil.interpolateColor(c1, c2, factor);
    }
}