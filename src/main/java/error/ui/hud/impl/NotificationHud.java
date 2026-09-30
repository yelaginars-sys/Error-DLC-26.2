package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NotificationHud extends HudElement implements IMinecraft {

    public enum Type { ON, OFF, INFO }

    public static class Note {
        final String title;
        final String message;
        final Type type;
        final long bornAt = System.currentTimeMillis();
        final long duration;
        float animationProgress = 0.0F;

        public Note(String title, String message, Type type, long duration) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.duration = duration;
        }
    }

    private static final ConcurrentLinkedQueue<Note> QUEUE = new ConcurrentLinkedQueue<>();
    private static final List<Note> ACTIVE_NOTES = new ArrayList<>();

    public NotificationHud() {
        super("notifications", "Notifications", 10.0F, 220.0F, 120.0F, 24.0F);
    }

    public static void notify(String title, String message, Type type) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        QUEUE.add(new Note(title, message, type, 3000L));
    }

    public static void onModuleToggle(String moduleName, boolean state) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        String title = moduleName;
        String msg = state ? "включен" : "выключен";
        notify(title, msg, state ? Type.ON : Type.OFF);
    }

    @Override
    public void draw(Render2DEvent event) {
        while (!QUEUE.isEmpty()) {
            Note n = QUEUE.poll();
            if (n != null) {
                ACTIVE_NOTES.add(n);
            }
        }

        if (ACTIVE_NOTES.isEmpty()) return;

        long now = System.currentTimeMillis();
        ACTIVE_NOTES.removeIf(n -> now - n.bornAt > n.duration);

        float drawX = getX();
        float currentY = getY();
        int primaryColor = Theme.getAccentColor();

        for (Note note : ACTIVE_NOTES) {
            long elapsed = now - note.bornAt;
            float progress = Math.min(1.0F, elapsed / 200.0F);
            note.animationProgress = progress;

            float alpha = progress;
            if (elapsed > note.duration - 300L) {
                alpha = Math.max(0.0F, (note.duration - elapsed) / 300.0F);
            }
            if (alpha <= 0.01F) continue;

            IconUse icon = note.type == Type.ON ? IconUse.CHECK : (note.type == Type.OFF ? IconUse.CROSS : IconUse.INFO);
            int iconColor = note.type == Type.ON ? ColorUtil.rgba(65, 220, 120, 255) : (note.type == Type.OFF ? ColorUtil.rgba(235, 75, 75, 255) : primaryColor);

            String textStr = note.title + " " + note.message;
            float textW = Fonts.SF_MEDIUM.getWidth(textStr, 6.0F);
            float noteWidth = Math.max(110.0F, textW + 24.0F);
            float noteHeight = 18.0F;

            int glowColor = ColorUtil.applyAlpha(iconColor, (int) (alpha * 30));
            int borderColor = ColorUtil.applyAlpha(iconColor, (int) (alpha * 60));
            int bgColor = ColorUtil.applyAlpha(ColorUtil.rgba(14, 14, 18, 200), alpha);

            Render2D.drawShadow(drawX, currentY, noteWidth, noteHeight, 4.0F, 6.0F, glowColor);
            Render2D.drawRoundedRect(drawX - 0.5F, currentY - 0.5F, noteWidth + 1.0F, noteHeight + 1.0F, 4.5F, borderColor);
            Render2D.drawRoundedRect(drawX, currentY, noteWidth, noteHeight, 4.0F, bgColor);

            Fonts.drawIcon(icon, drawX + 5.0F, currentY + 4.5F, 8.0F, ColorUtil.applyAlpha(iconColor, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, note.title, drawX + 16.0F, currentY + 5.0F, 6.0F, ColorUtil.applyAlpha(-1, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, note.message, drawX + 16.0F + Fonts.SF_MEDIUM.getWidth(note.title + " ", 6.0F), currentY + 5.0F, 6.0F, ColorUtil.applyAlpha(iconColor, alpha));

            currentY -= (noteHeight + 4.0F);
        }
    }
}