package error.ui.mainmenu.popup;

import error.setting.SettingRenderer;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public class SettingsPopup {
    private final ModeSetting language = new ModeSetting("Language", "ENG", "ENG", "RU");
    private final ModeSetting bgMode = new ModeSetting("Background", Theme.getBackgroundMode(), "Blur", "None");
    private final ColorSetting accent = new ColorSetting("Accent", Theme.getAccentColor());

    private final List<SettingRenderer<?>> renderers = new ArrayList<>();
    private float cachedHeight = 135.0F;
    private final float width = 125.0F;

    private int lastSyncedAccent = -1;
    private String lastSyncedBgMode = "";

    public SettingsPopup() {
        renderers.add(language.createRenderer());
        renderers.add(bgMode.createRenderer());
        renderers.add(accent.createRenderer());
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return cachedHeight;
    }

    public void syncFromTheme() {
        if (!bgMode.getValue().equalsIgnoreCase(Theme.getBackgroundMode())) {
            bgMode.setValue(Theme.getBackgroundMode());
        }
        if (accent.getValue() != Theme.getAccentColor()) {
            accent.setValue(Theme.getAccentColor());
        }
        this.lastSyncedAccent = Theme.getAccentColor();
        this.lastSyncedBgMode = Theme.getBackgroundMode();
    }

    private void updateConfigs() {
        Localization.setLanguage(language.getValue());
        Theme.setBackgroundMode(bgMode.getValue());
        Theme.setAccentColor(accent.getValue());
        this.lastSyncedAccent = Theme.getAccentColor();
        this.lastSyncedBgMode = Theme.getBackgroundMode();
    }

    public void render(float x, float y, int mouseX, int mouseY, float alpha) {
        if (alpha <= 0.01F) return;

        if (Theme.getAccentColor() != lastSyncedAccent || !Theme.getBackgroundMode().equalsIgnoreCase(lastSyncedBgMode)) {
            syncFromTheme();
        }

        updateConfigs();

        float totalSettingsH = 0.0F;
        for (SettingRenderer<?> sr : renderers) {
            sr.updateVisibility();
            float sH = sr.getHeight();
            if (sH > 0.1F) {
                totalSettingsH += sH + 2.5F;
            }
        }
        this.cachedHeight = 20.0F + totalSettingsH + 5.0F;

        int popupBgColor = 0xFF0D0E12;
        float popupAlpha = alpha * 0.95F;

        if (Theme.getBackgroundMode().equalsIgnoreCase("Blur")) {
            Render2D.drawBlur(x, y, width, cachedHeight, 6.0F, ColorUtil.multiplyAlpha(popupBgColor, popupAlpha), alpha);
        }
        Render2D.drawRoundedRect(x, y, width, cachedHeight, 6.0F, ColorUtil.multiplyAlpha(popupBgColor, popupAlpha));

        Fonts.drawString(Fonts.SF_MEDIUM, Localization.get("GUI Settings"), x + 7, y + 6.0F, 9.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));

        float curY = y + 19.0F;
        for (SettingRenderer<?> sr : renderers) {
            float sH = sr.getHeight();
            if (sH > 0.05F) {
                sr.render(x + 3, curY, width - 6, sH, mouseX, mouseY, alpha);
                curY += sH + (2.5F * sr.getVisAnim().getValue());
            }
        }
    }

    public boolean mouseClicked(float x, float y, double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + cachedHeight) {
            float curY = y + 19.0F;
            for (SettingRenderer<?> sr : renderers) {
                float sH = sr.getHeight();
                if (sH > 0.05F) {
                    if (mouseY >= curY && mouseY <= curY + sH) {
                        sr.mouseClicked(mouseX, mouseY, button);
                        updateConfigs();
                        return true;
                    }
                    curY += sH + (2.5F * sr.getVisAnim().getValue());
                }
            }
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (SettingRenderer<?> sr : renderers) {
            sr.mouseReleased(mouseX, mouseY, button);
        }
        updateConfigs();
    }
}