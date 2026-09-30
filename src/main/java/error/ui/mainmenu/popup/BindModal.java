package error.ui.mainmenu.popup;

import error.module.Module;
import error.setting.BindMode;
import error.setting.impl.BindSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import org.lwjgl.glfw.GLFW;

public class BindModal implements Modal {
    private final Module module;
    private final boolean hadInitialBind;
    private boolean listening = true;
    private boolean finished = false;
    private BindMode currentMode;
    private boolean hideInKeybinds;

    public BindModal(Module module) {
        this.module = module;
        this.hadInitialBind = module.getBind().isBound();
        this.currentMode = module.getBind().getMode(0, BindMode.TOGGLE);
        this.hideInKeybinds = !module.getBind().isVisibleAt(0);
    }

    @Override
    public void render(int mouseX, int mouseY, float screenWidth, float screenHeight, float alpha) {
        float modalW = 230.0F;
        float modalH = 145.0F;
        float modalX = (screenWidth - modalW) / 2.0F;
        float modalY = (screenHeight - modalH) / 2.0F;

        int themeAccent = Theme.getAccentColor();
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));
        int glassFill = ColorUtil.rgba(24, 20, 32, (int) (235 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (55 * alpha));
        int accentGlow = ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), (int) (40 * alpha));

        // Background Backdrop Dimming
        Render2D.drawRect(0, 0, (int) screenWidth, (int) screenHeight, ColorUtil.rgba(0, 0, 0, (int) (100 * alpha)));

        // Modal Frame
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 12.0F, 14.0F, shadowCol);
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 12.0F, 8.0F, accentGlow);
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 12.0F, 18.0F, glassFill, alpha);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 12.0F, glassFill);
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 12.0F, 1.0F, glassBorder);

        // Header Title
        String title = module.getName() + "  —  Бинд";
        Fonts.drawString(Fonts.SF_MEDIUM, title, modalX + 14.0F, modalY + 12.0F, 9.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        // 1. Key Listen Box
        float boxX = modalX + 12.0F;
        float boxY = modalY + 28.0F;
        float boxW = modalW - 24.0F;
        float boxH = 28.0F;

        int boxBg = listening ? ColorUtil.rgba(50, 42, 68, (int) (160 * alpha)) : ColorUtil.rgba(36, 30, 48, (int) (140 * alpha));
        int boxBorder = listening ? Theme.getAccentColor() : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));

        Render2D.drawRoundedRect(boxX, boxY, boxW, boxH, 6.0F, boxBg);
        Render2D.drawRoundedOutline(boxX, boxY, boxW, boxH, 6.0F, 1.0F, boxBorder);

        String keyText;
        if (listening) {
            keyText = "Нажмите клавишу или мышь...";
        } else if (module.getBind().isBound()) {
            keyText = "Клавиша:  " + module.getBind().getDisplayValue();
        } else {
            keyText = "Бинд не назначен";
        }
        int keyColor = listening ? Theme.getAccentColor() : ColorUtil.rgba(240, 240, 255, (int) (240 * alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, keyText, boxX + boxW / 2.0F, boxY + 8.5F, 8.5F, keyColor);

        // Subtext hint for ESC
        String escHint = hadInitialBind ? "[ESC] Удалить бинд" : "[ESC] Отмена";
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, escHint, boxX + boxW / 2.0F, boxY + boxH + 4.0F, 6.5F, ColorUtil.rgba(170, 165, 185, (int) (160 * alpha)));

        // 2. Mode Selector (Toggle vs Hold)
        float modeY = boxY + boxH + 16.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Режим:", modalX + 14.0F, modeY + 3.0F, 8.0F, ColorUtil.rgba(200, 195, 215, (int) (200 * alpha)));

        float btnW = 52.0F;
        float btnH = 15.0F;
        float toggleX = modalX + 75.0F;
        float holdX = toggleX + btnW + 6.0F;

        boolean isToggle = currentMode == BindMode.TOGGLE;
        int accent = Theme.getAccentColor();

        // Toggle Button
        int toggleBg = isToggle ? ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (140 * alpha)) : ColorUtil.rgba(45, 38, 56, (int) (120 * alpha));
        Render2D.drawRoundedRect(toggleX, modeY, btnW, btnH, 4.0F, toggleBg);
        Render2D.drawRoundedOutline(toggleX, modeY, btnW, btnH, 4.0F, 1.0F, isToggle ? accent : ColorUtil.rgba(255, 255, 255, 30));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Toggle", toggleX + btnW / 2.0F, modeY + 3.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));

        // Hold Button
        int holdBg = !isToggle ? ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (140 * alpha)) : ColorUtil.rgba(45, 38, 56, (int) (120 * alpha));
        Render2D.drawRoundedRect(holdX, modeY, btnW, btnH, 4.0F, holdBg);
        Render2D.drawRoundedOutline(holdX, modeY, btnW, btnH, 4.0F, 1.0F, !isToggle ? accent : ColorUtil.rgba(255, 255, 255, 30));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Hold", holdX + btnW / 2.0F, modeY + 3.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));

        // 3. Hide in Keybinds Checkbox
        float hideY = modeY + btnH + 10.0F;
        float cbSize = 10.0F;
        float cbX = modalX + 14.0F;

        int cbBg = hideInKeybinds ? accent : ColorUtil.rgba(255, 255, 255, 20);
        Render2D.drawRoundedRect(cbX, hideY, cbSize, cbSize, 2.5F, ColorUtil.multiplyAlpha(cbBg, alpha));
        Render2D.drawRoundedOutline(cbX, hideY, cbSize, cbSize, 2.5F, 1.0F, ColorUtil.rgba(255, 255, 255, 40));
        if (hideInKeybinds) {
            Fonts.drawString(Fonts.SF_MEDIUM, "v", cbX + 2.5F, hideY + 1.0F, 6.0F, 0xFFFFFFFF);
        }

        Fonts.drawString(Fonts.SF_MEDIUM, "Скрывать в Keybinds", cbX + cbSize + 6.0F, hideY + 1.0F, 7.5F, ColorUtil.rgba(210, 205, 225, (int) (210 * alpha)));

        // 4. Done Button
        float doneW = 60.0F;
        float doneH = 16.0F;
        float doneX = modalX + modalW - doneW - 12.0F;
        float doneY = modalY + modalH - doneH - 10.0F;

        Render2D.drawRoundedRect(doneX, doneY, doneW, doneH, 4.0F, ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), (int) (180 * alpha)));
        Render2D.drawRoundedOutline(doneX, doneY, doneW, doneH, 4.0F, 1.0F, accent);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Готово", doneX + doneW / 2.0F, doneY + 3.5F, 8.0F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float modalW = 230.0F;
        float modalH = 145.0F;
        float modalX = (net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth() - modalW) / 2.0F;
        float modalY = (net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight() - modalH) / 2.0F;

        float boxX = modalX + 12.0F;
        float boxY = modalY + 28.0F;
        float boxW = modalW - 24.0F;
        float boxH = 28.0F;

        // Click Key Listen Box -> Toggle listening
        if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
            listening = true;
            return true;
        }

        // If listening and user clicks anywhere in modal with MB2/MB3/MB4/MB5 -> bind to mouse button!
        if (listening) {
            module.getBind().setSingle(BindSetting.mouse(button));
            module.getBind().setAllModes(currentMode);
            module.getBind().setVisibleAt(0, !hideInKeybinds);
            listening = false;
            return true;
        }

        float modeY = boxY + boxH + 16.0F;
        float btnW = 52.0F;
        float btnH = 15.0F;
        float toggleX = modalX + 75.0F;
        float holdX = toggleX + btnW + 6.0F;

        // Mode Toggle Button
        if (mouseX >= toggleX && mouseX <= toggleX + btnW && mouseY >= modeY && mouseY <= modeY + btnH) {
            currentMode = BindMode.TOGGLE;
            module.getBind().setAllModes(BindMode.TOGGLE);
            return true;
        }

        // Mode Hold Button
        if (mouseX >= holdX && mouseX <= holdX + btnW && mouseY >= modeY && mouseY <= modeY + btnH) {
            currentMode = BindMode.HOLD;
            module.getBind().setAllModes(BindMode.HOLD);
            return true;
        }

        // Hide in Keybinds Checkbox
        float hideY = modeY + btnH + 10.0F;
        if (mouseX >= modalX + 14.0F && mouseX <= modalX + 160.0F && mouseY >= hideY && mouseY <= hideY + 14.0F) {
            hideInKeybinds = !hideInKeybinds;
            module.getBind().setVisibleAt(0, !hideInKeybinds);
            return true;
        }

        // Done Button
        float doneW = 60.0F;
        float doneH = 16.0F;
        float doneX = modalX + modalW - doneW - 12.0F;
        float doneY = modalY + modalH - doneH - 10.0F;

        if (mouseX >= doneX && mouseX <= doneX + doneW && mouseY >= doneY && mouseY <= doneY + doneH) {
            finished = true;
            return true;
        }

        return true;
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (hadInitialBind) {
                module.getBind().clear();
            }
            finished = true;
            return true; // CONSUME ESC so ClickGUI stays open!
        }

        if (listening) {
            module.getBind().setSingle(keyCode);
            module.getBind().setAllModes(currentMode);
            module.getBind().setVisibleAt(0, !hideInKeybinds);
            listening = false;
            return true;
        }

        return true;
    }

    @Override
    public void charTyped(int codePoint) {}

    @Override
    public boolean isFinished() {
        return finished;
    }
}
