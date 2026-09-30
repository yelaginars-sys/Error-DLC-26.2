package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class NotificationHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);

    public enum Type { ON, OFF, INFO }

    public static class Note {
        final String title;
        final String message;
        final Type type;
        final long bornAt = System.currentTimeMillis();
        final long duration;

        public Note(String title, String message, Type type, long duration) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.duration = duration;
        }
    }

    private static final ConcurrentLinkedQueue<Note> QUEUE = new ConcurrentLinkedQueue<>();
    private static final List<Note> ACTIVE_NOTES = new ArrayList<>();

    // Settings Toggles (Photo 2 UI)
    public static boolean moduleState = true;
    public static boolean totemPop = true;
    public static boolean shieldBreak = false;
    public static boolean lowDurability = false;

    private static boolean settingsOpen = false;
    private float settingsAnim = 0.0F;

    public NotificationHud() {
        super("notifications", "Notifications", 10.0F, 220.0F, 180.0F, 24.0F);
    }

    public static void notify(String title, String message, Type type) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        if (type == Type.ON || type == Type.OFF) {
            if (!moduleState) return;
        }
        QUEUE.add(new Note(title, message, type, 3000L));
    }

    public static void post(String title, String description, IconUse icon, int iconColor, long durationMs) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        Type type = Type.INFO;
        if (iconColor == error.util.client.persiki.Notify.COLOR_SUCCESS) type = Type.ON;
        else if (iconColor == error.util.client.persiki.Notify.COLOR_ERROR) type = Type.OFF;
        QUEUE.add(new Note(title, description, type, durationMs));
    }

    public static boolean isNotifyElytraSwapEnabled() {
        return moduleState;
    }

    public static void onModuleToggle(String moduleName, boolean state) {
        if (error.module.impl.misc.UnHook.unhooked || !moduleState) return;
        notify(moduleName, state ? "включен" : "выключен", state ? Type.ON : Type.OFF);
    }

    public com.google.gson.JsonObject writeConfig() {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        obj.addProperty("moduleState", moduleState);
        obj.addProperty("totemPop", totemPop);
        obj.addProperty("shieldBreak", shieldBreak);
        obj.addProperty("lowDurability", lowDurability);
        return obj;
    }

    public void readConfig(com.google.gson.JsonObject json) {
        if (json == null) return;
        if (json.has("moduleState")) moduleState = json.get("moduleState").getAsBoolean();
        if (json.has("totemPop")) totemPop = json.get("totemPop").getAsBoolean();
        if (json.has("shieldBreak")) shieldBreak = json.get("shieldBreak").getAsBoolean();
        if (json.has("lowDurability")) lowDurability = json.get("lowDurability").getAsBoolean();
    }

    public static boolean isSettingsOpen() {
        return settingsOpen;
    }

    public static void toggleSettings() {
        settingsOpen = !settingsOpen;
    }

    @Override
    public void draw(Render2DEvent event) {
        if (error.module.impl.misc.UnHook.unhooked) return;

        while (!QUEUE.isEmpty()) {
            Note n = QUEUE.poll();
            if (n != null) ACTIVE_NOTES.add(n);
        }

        long now = System.currentTimeMillis();
        ACTIVE_NOTES.removeIf(note -> (now - note.bornAt) > note.duration + 400L);

        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);
        if (ACTIVE_NOTES.isEmpty() && editing) {
            ACTIVE_NOTES.add(new Note("Это уведомление", "кликни на меня для настройки", Type.INFO, 5000L));
        }

        float drawX = getX();
        float currentY = getY();

        for (Note note : ACTIVE_NOTES) {
            long elapsed = now - note.bornAt;
            float alpha = 1.0F;
            if (elapsed < 300L) {
                alpha = elapsed / 300.0F;
            } else if (elapsed > note.duration) {
                alpha = Math.max(0.0F, (note.duration + 400L - elapsed) / 400.0F);
            }
            if (alpha <= 0.01F) continue;

            IconUse icon = note.type == Type.ON ? IconUse.CHECK : (note.type == Type.OFF ? IconUse.CROSS : IconUse.INFO);
            int primaryColor = ACCENT_PURPLE;

            String textStr = note.title + (note.message.isEmpty() ? "" : ", " + note.message);
            float textW = Fonts.SF_MEDIUM.getWidth(textStr, 6.5F);
            float noteWidth = Math.max(160.0F, textW + 30.0F);
            float noteHeight = 20.0F;

            int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
            int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
            int bgColor = ColorUtil.rgba(14, 14, 18, (int) (195 * alpha));

            // Capsule Banner (Waper Style)
            Render2D.drawRoundedRect(drawX - 2.0F, currentY - 2.0F, noteWidth + 4.0F, noteHeight + 4.0F, 10.0F, glowColor);
            Render2D.drawRoundedRect(drawX - 0.5F, currentY - 0.5F, noteWidth + 1.0F, noteHeight + 1.0F, 8.5F, borderColor);
            Render2D.drawRoundedRect(drawX, currentY, noteWidth, noteHeight, 8.0F, bgColor);

            Fonts.drawIcon(icon, drawX + 6.0F, currentY + 5.5F, 8.0F, ColorUtil.applyAlpha(primaryColor, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, note.title, drawX + 18.0F, currentY + 5.5F, 6.5F, ColorUtil.applyAlpha(-1, alpha));
            if (!note.message.isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, ", " + note.message, drawX + 18.0F + Fonts.SF_MEDIUM.getWidth(note.title, 6.5F), currentY + 5.5F, 6.5F, ColorUtil.applyAlpha(ColorUtil.rgba(180, 180, 190, 255), alpha));
            }

            currentY -= (noteHeight + 6.0F);
        }

        // Render Settings Window (Photo 2 UI) if open
        settingsAnim = Mth.clamp(settingsAnim + (settingsOpen ? 0.12F : -0.12F), 0.0F, 1.0F);
        if (settingsAnim > 0.001F) {
            renderSettingsModal(drawX, getY() - 110.0F, settingsAnim);
        }
    }

    private void renderSettingsModal(float modalX, float modalY, float alpha) {
        float width = 160.0F;
        float height = 95.0F;
        float radius = 8.0F;

        int primaryColor = ACCENT_PURPLE;
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 20));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 50));
        int bgColor = ColorUtil.rgba(14, 14, 18, (int) (235 * alpha));
        int headerBg = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));

        // Window Frame
        Render2D.drawShadow(modalX, modalY, width, height, radius, 8.0F, ColorUtil.rgba(0, 0, 0, (int) (160 * alpha)));
        Render2D.drawRoundedRect(modalX - 2.0F, modalY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(modalX - 0.5F, modalY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(modalX, modalY, width, height, radius, bgColor);

        // Header "Настройки"
        Render2D.drawRoundedRect(modalX, modalY, width, 16.0F, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, "Настройки", modalX + 8.0F, modalY + 3.5F, 7.5F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Options List (Photo 2 items)
        String[] titles = {"Состояние модулей", "Поп тотема", "Ломание щита", "Низкая прочность"};
        boolean[] states = {moduleState, totemPop, shieldBreak, lowDurability};

        float optY = modalY + 20.0F;
        float optH = 17.0F;

        for (int i = 0; i < titles.length; i++) {
            Fonts.drawString(Fonts.SF_MEDIUM, titles[i], modalX + 8.0F, optY + 3.5F, 6.5F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

            // Switch Pill
            float switchW = 22.0F;
            float switchH = 10.0F;
            float switchX = modalX + width - 12.0F - switchW;
            float switchY = optY + 2.0F;

            boolean active = states[i];
            int switchBg = active ? ColorUtil.applyAlpha(ACCENT_PURPLE, (int) (220 * alpha)) : ColorUtil.rgba(35, 37, 48, (int) (200 * alpha));
            Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 5.0F, switchBg);

            // Circle Knob inside switch
            float knobSize = 8.0F;
            float knobX = active ? (switchX + switchW - knobSize - 1.0F) : (switchX + 1.0F);
            float knobY = switchY + 1.0F;
            Render2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, 4.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

            optY += optH;
        }
    }
}