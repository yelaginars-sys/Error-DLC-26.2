package error.ui.nova;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

/**
 * NovaTheme — цветовая палитра нового ClickGUI "Nova".
 * Liquid-glass стиль: тёмный полупрозрачный фон, акцент через Theme.getAccentColor().
 */
public final class NovaTheme {

    private NovaTheme() {}

    // ─── Базовый фон окна ────────────────────────────────────────
    public static int BG_WINDOW()    { return ColorUtil.rgba( 9,  10,  15, 210); }
    public static int BG_SIDEBAR()   { return ColorUtil.rgba( 7,   8,  13, 195); }
    public static int BG_CONTENT()   { return ColorUtil.rgba(11,  12,  18, 180); }
    public static int BG_CARD()      { return ColorUtil.rgba(18,  20,  28,  90); }
    public static int BG_CARD_HOV()  { return ColorUtil.rgba(24,  26,  36, 115); }
    public static int BG_HEADER()    { return ColorUtil.rgba(14,  15,  22, 200); }
    public static int BG_SETTINGS()  { return ColorUtil.rgba( 8,   9,  14, 220); }

    // ─── Границы / линии ─────────────────────────────────────────
    public static int BORDER()        { return ColorUtil.rgba(255, 255, 255,  14); }
    public static int BORDER_ACTIVE() { return ColorUtil.withAlpha(accent(),  90); }
    public static int DIVIDER()       { return ColorUtil.rgba(255, 255, 255,  10); }

    // ─── Текст ───────────────────────────────────────────────────
    public static int TEXT()          { return ColorUtil.rgba(238, 240, 252, 255); }
    public static int TEXT_SEC()      { return ColorUtil.rgba(175, 180, 200, 255); }
    public static int TEXT_MUTED()    { return ColorUtil.rgba(100, 108, 130, 255); }
    public static int TEXT_FAINT()    { return ColorUtil.rgba( 65,  72,  92, 255); }

    // ─── Акцент (делегируем в Theme) ─────────────────────────────
    public static int accent()        { return Theme.getAccentColor(); }
    public static int ACCENT_DIM()    { return ColorUtil.withAlpha(accent(),  70); }
    public static int ACCENT_GLOW()   { return ColorUtil.withAlpha(accent(),  40); }

    // ─── Элементы управления ─────────────────────────────────────
    public static int TOGGLE_TRACK_OFF() { return ColorUtil.rgba(35, 40, 55, 255); }
    public static int TOGGLE_KNOB_OFF()  { return ColorUtil.rgba(110, 115, 130, 255); }
    public static int PILL()             { return ColorUtil.rgba(255, 255, 255,  10); }
    public static int FIELD()            { return ColorUtil.rgba(14,  16,  24, 150); }
    public static int TRACK()            { return ColorUtil.rgba(255, 255, 255,  15); }

    // ─── Sidebar-item активный фон ────────────────────────────────
    public static int SIDEBAR_ACTIVE()   {
        int a = accent();
        return ColorUtil.rgba(ColorUtil.red(a), ColorUtil.green(a), ColorUtil.blue(a), 28);
    }
    public static int SIDEBAR_HOVER()    { return ColorUtil.rgba(255, 255, 255, 10); }
}
