package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.impl.render.Notification;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationHud implements IMinecraft {
    private static final List<Toast> toasts = new ArrayList<>();

    public static class Toast {
        public String title;
        public String description;
        public IconUse icon;
        public int iconColor;
        public long durationMs;
        public long createdTime;
        public Animation alphaAnim = new Animation(0.0F, 0.25F);

        public Toast(String title, String description, IconUse icon, int iconColor, long durationMs) {
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.iconColor = iconColor;
            this.durationMs = durationMs;
            this.createdTime = System.currentTimeMillis();
        }

        public boolean isExpired(long now) {
            return now - createdTime > durationMs;
        }
    }

    public static boolean isNotifyElytraSwapEnabled() {
        return true;
    }

    public static void post(String title, String description, IconUse icon, int iconColor, long durationMs) {
        if (Notification.INSTANCE != null && !Notification.INSTANCE.isEnabled()) {
            String fullText = (title + " " + description).trim();
            DynamicIslandHud.showNotification(fullText, iconColor != ColorUtil.rgba(235, 75, 75, 255), durationMs);
            return;
        }

        Toast toast = new Toast(title, description, icon, iconColor, durationMs);
        toasts.add(toast);
    }

    public static void onModuleToggle(String name, boolean state) {
        String msg = name + (state ? " enabled" : " disabled");
        IconUse icon = state ? IconUse.CHECK : IconUse.CROSS;
        int col = state ? ColorUtil.rgba(85, 255, 135, 255) : ColorUtil.rgba(255, 80, 100, 255);
        post(msg, "", icon, col, 2200L);
    }

    public static void renderToasts(Render2DEvent event) {
        if (toasts.isEmpty() || IMinecraft.mc.player == null) return;

        long now = System.currentTimeMillis();
        int screenW = IMinecraft.mc.getWindow().getGuiScaledWidth();
        int screenH = IMinecraft.mc.getWindow().getGuiScaledHeight();

        boolean chatOpen = IMinecraft.mc.gui.screen() instanceof ChatScreen;
        float startY = screenH - (chatOpen ? 60.0F : 35.0F);

        Iterator<Toast> it = toasts.iterator();
        float currentY = startY;

        while (it.hasNext()) {
            Toast t = it.next();
            if (t.isExpired(now)) {
                t.alphaAnim.setTarget(0.0F);
            } else {
                t.alphaAnim.setTarget(1.0F);
            }
            t.alphaAnim.update();

            float alpha = t.alphaAnim.getValue();
            if (alpha <= 0.01F && t.isExpired(now)) {
                it.remove();
                continue;
            }

            String text = (t.title + " " + t.description).trim();
            float fontW = Fonts.SF_MEDIUM.getWidth(text, 7.5F);
            float toastW = Math.max(120.0F, fontW + 28.0F);
            float toastH = 18.0F;

            float toastX = (screenW - toastW) / 2.0F;
            float toastY = currentY;

            int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));
            int glassFill = ColorUtil.rgba(20, 22, 32, (int) (215 * alpha));
            int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (40 * alpha));

            Render2D.drawShadow(toastX, toastY, toastW, toastH, 6.0F, 6.0F, shadowCol);
            Render2D.drawBlur(toastX, toastY, toastW, toastH, 6.0F, 12.0F, glassFill, alpha);
            Render2D.drawRoundedRect(toastX, toastY, toastW, toastH, 6.0F, glassFill);
            Render2D.drawRoundedOutline(toastX, toastY, toastW, toastH, 6.0F, 1.0F, glassBorder);

            Fonts.drawIcon(t.icon == null ? IconUse.INFO : t.icon, toastX + 8.0F, toastY + 4.5F, 7.5F, ColorUtil.multiplyAlpha(t.iconColor, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, text, toastX + 22.0F, toastY + 4.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

            currentY -= (toastH + 4.0F) * alpha;
        }
    }

    private NotificationHud() {}
}
