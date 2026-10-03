package error.ui.zenith;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

public final class ZenithTheme {

    // Zenith Signature Neon Pink / Magenta Accent
    public static int ACCENT() {
        return ColorUtil.rgba(230, 0, 122, 255);
    }

    public static int ACCENT_GLOW() {
        return ColorUtil.rgba(255, 46, 147, 180);
    }

    public static int BG_TOP() {
        return ColorUtil.rgba(20, 22, 32, 220);
    }

    public static int BG_BOTTOM() {
        return ColorUtil.rgba(12, 13, 20, 200);
    }

    public static int SIDEBAR() {
        return ColorUtil.rgba(16, 17, 25, 190);
    }

    public static int CARD() {
        return ColorUtil.rgba(25, 27, 38, 120);
    }

    public static int CARD_ACTIVE() {
        return ColorUtil.rgba(230, 0, 122, 35);
    }

    public static int BORDER() {
        return ColorUtil.rgba(255, 255, 255, 14);
    }

    public static int BORDER_ACTIVE() {
        return ColorUtil.rgba(230, 0, 122, 180);
    }

    public static int TEXT_PRIMARY() {
        return ColorUtil.rgba(255, 255, 255, 255);
    }

    public static int TEXT_SECONDARY() {
        return ColorUtil.rgba(170, 175, 195, 255);
    }

    public static int TEXT_MUTED() {
        return ColorUtil.rgba(110, 115, 130, 255);
    }

    public static int BADGE_GREEN() {
        return ColorUtil.rgba(60, 210, 120, 255);
    }

    private ZenithTheme() {}
}
