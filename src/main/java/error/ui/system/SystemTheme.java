package error.ui.system;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

public final class SystemTheme {

    public static int BG_MAIN() {
        return ColorUtil.rgba(13, 14, 18, 245);
    }

    public static int SIDEBAR() {
        return ColorUtil.rgba(9, 10, 14, 250);
    }

    public static int CARD() {
        return ColorUtil.rgba(18, 20, 26, 215);
    }

    public static int CARD_HOVER() {
        return ColorUtil.rgba(24, 27, 36, 230);
    }

    public static int CARD_ACTIVE() {
        return ColorUtil.rgba(28, 31, 42, 240);
    }

    public static int BORDER() {
        return ColorUtil.rgba(255, 255, 255, 14);
    }

    public static int BORDER_ACTIVE() {
        return ColorUtil.rgba(255, 255, 255, 40);
    }

    public static int TEXT_PRIMARY() {
        return ColorUtil.rgba(242, 244, 248, 255);
    }

    public static int TEXT_SECONDARY() {
        return ColorUtil.rgba(155, 160, 172, 255);
    }

    public static int TEXT_MUTED() {
        return ColorUtil.rgba(95, 100, 112, 255);
    }

    public static int ACCENT() {
        return Theme.getAccentColor();
    }

    public static int WHITE() {
        return ColorUtil.rgba(255, 255, 255, 255);
    }

    public static int CHECKMARK_BG() {
        return ColorUtil.rgba(255, 255, 255, 240);
    }

    public static int CHECKMARK_ICON() {
        return ColorUtil.rgba(12, 14, 18, 255);
    }

    public static int PILL_BG() {
        return ColorUtil.rgba(255, 255, 255, 12);
    }
}
