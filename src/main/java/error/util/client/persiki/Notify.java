package error.util.client.persiki;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.font.IconUse;
import error.ui.hud.impl.DynamicIslandHud;

/**
 */
public final class Notify {

    public static final long DEFAULT_DURATION = 2500L;
    public static final int COLOR_SUCCESS = ColorUtil.rgba(65, 225, 120, 255);
    public static final int COLOR_ERROR = ColorUtil.rgba(235, 75, 75, 255);
    public static final int COLOR_WARN = ColorUtil.rgba(245, 190, 45, 255);


    public static void add(String title, String description, IconUse icon, int iconColor, long durationMs) {
        String msg = (title != null && !title.isEmpty()) ? (title + ": " + description) : description;
        DynamicIslandHud.postNotification(msg, iconColor != COLOR_ERROR);
    }
    public static void add(String title, String description, IconUse icon, int iconColor) {
        add(title, description, icon, iconColor, DEFAULT_DURATION);
    }

    public static void info(String title, String description) {
        add(title, description, IconUse.INFO, Theme.getAccentColor(), DEFAULT_DURATION);
    }

    public static void info(String title, String description, long durationMs) {
        add(title, description, IconUse.INFO, Theme.getAccentColor(), durationMs);
    }


    public static void error(String title, String description) {
        add(title, description, IconUse.INFO, COLOR_ERROR, DEFAULT_DURATION);
    }

    public static void error(String title, String description, long durationMs) {
        add(title, description, IconUse.INFO, COLOR_ERROR, durationMs);
    }
    public static void warning(String title, String description) {
        add(title, description, IconUse.INFO, COLOR_WARN, DEFAULT_DURATION);
    }
}