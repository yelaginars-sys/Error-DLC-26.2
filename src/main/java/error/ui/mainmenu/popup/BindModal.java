package error.ui.mainmenu.popup;

import error.module.Module;
import error.setting.BindMode;
import error.setting.impl.BindSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

public class BindModal implements Modal {
    private final Module module;
    private final boolean hadInitialBind;
    private boolean listening = false;
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
        float modalW = 145.0F;
        float modalH = 88.0F;
        float modalX = screenWidth - modalW - 20.0F;
        float modalY = (screenHeight - modalH) / 2.0F;

        int themeAccent = Theme.getAccentColor();
        int ar = ColorUtil.red(themeAccent);
        int ag = ColorUtil.green(themeAccent);
        int ab = ColorUtil.blue(themeAccent);

        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));
        int glassFill = ColorUtil.rgba(18, 14, 24, (int) (220 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (35 * alpha));
        int accentGlow = ColorUtil.rgba(ar, ag, ab, (int) (40 * alpha));

        // Background Backdrop Dimming
        Render2D.drawRect(0, 0, (int) screenWidth, (int) screenHeight, ColorUtil.rgba(0, 0, 0, (int) (60 * alpha)));

        // Modal Frame in Theme / Liquid Glass style
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 7.0F, 10.0F, shadowCol);
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 7.0F, 5.0F, accentGlow);
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 7.0F, 14.0F, glassFill, alpha);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 7.0F, glassFill);
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 7.0F, 1.0F, ColorUtil.multiplyAlpha(themeAccent, 0.7F * alpha));

        // Header Title
        String title = module.getName() + " — Бинд";
        Fonts.drawString(Fonts.SF_MEDIUM, title, modalX + 8.0F, modalY + 6.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        // 1. Key Listen Box
        float boxX = modalX + 8.0F;
        float boxY = modalY + 17.0F;
        float boxW = modalW - 16.0F;
        float boxH = 18.0F;

        int boxBg = listening ? ColorUtil.multiplyAlpha(themeAccent, 0.25F * alpha) : ColorUtil.rgba(25, 20, 32, (int) (160 * alpha));
        int boxBorder = listening ? themeAccent : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));

        Render2D.drawRoundedRect(boxX, boxY, boxW, boxH, 4.0F, boxBg);
        Render2D.drawRoundedOutline(boxX, boxY, boxW, boxH, 4.0F, 1.0F, boxBorder);

        String keyText;
        if (listening) {
            keyText = "Нажмите клавишу...";
        } else if (module.getBind().isBound()) {
            keyText = "Клавиша: " + module.getBind().getDisplayValue();
        } else {
            keyText = "Кликните для бинда";
        }
        int keyColor = listening ? themeAccent : ColorUtil.rgba(240, 240, 255, (int) (240 * alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, keyText, boxX + boxW / 2.0F, boxY + 5.0F, 6.5F, keyColor);

        // 2. Mode Selector (Toggle vs Hold)
        float modeY = boxY + boxH + 9.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Режим:", modalX + 8.0F, modeY + 2.0F, 6.5F, ColorUtil.rgba(190, 190, 205, (int) (200 * alpha)));

        float btnW = 34.0F;
        float btnH = 11.0F;
        float toggleX = modalX + 44.0F;
        float holdX = toggleX + btnW + 4.0F;

        boolean isToggle = currentMode == BindMode.TOGGLE;

        // Toggle Button
        int toggleBg = isToggle ? ColorUtil.multiplyAlpha(themeAccent, 0.50F * alpha) : ColorUtil.rgba(25, 20, 32, (int) (120 * alpha));
        Render2D.drawRoundedRect(toggleX, modeY, btnW, btnH, 3.0F, toggleBg);
        Render2D.drawRoundedOutline(toggleX, modeY, btnW, btnH, 3.0F, 1.0F, isToggle ? themeAccent : ColorUtil.rgba(255, 255, 255, 25));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Toggle", toggleX + btnW / 2.0F, modeY + 2.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));

        // Hold Button
        int holdBg = !isToggle ? ColorUtil.multiplyAlpha(themeAccent, 0.50F * alpha) : ColorUtil.rgba(25, 20, 32, (int) (120 * alpha));
        Render2D.drawRoundedRect(holdX, modeY, btnW, btnH, 3.0F, holdBg);
        Render2D.drawRoundedOutline(holdX, modeY, btnW, btnH, 3.0F, 1.0F, !isToggle ? themeAccent : ColorUtil.rgba(255, 255, 255, 25));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Hold", holdX + btnW / 2.0F, modeY + 2.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));

        // 3. Hide in Keybinds Checkbox
        float hideY = modeY + btnH + 7.0F;
        float cbSize = 8.0F;
        float cbX = modalX + 8.0F;

        int cbBg = hideInKeybinds ? themeAccent : ColorUtil.rgba(255, 255, 255, 20);
        Render2D.drawRoundedRect(cbX, hideY, cbSize, cbSize, 2.0F, ColorUtil.multiplyAlpha(cbBg, alpha));
        Render2D.drawRoundedOutline(cbX, hideY, cbSize, cbSize, 2.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 40));
        if (hideInKeybinds) {
            Fonts.drawString(Fonts.SF_MEDIUM, "v", cbX + 1.5F, hideY + 0.5F, 5.0F, 0xFFFFFFFF);
        }

        Fonts.drawString(Fonts.SF_MEDIUM, "Скрывать в Keybinds", cbX + cbSize + 4.0F, hideY + 0.5F, 6.0F, ColorUtil.rgba(200, 200, 215, (int) (210 * alpha)));

        // 4. Done Button
        float doneW = 38.0F;
        float doneH = 11.0F;
        float doneX = modalX + modalW - doneW - 8.0F;
        float doneY = modalY + modalH - doneH - 6.0F;

        Render2D.drawRoundedRect(doneX, doneY, doneW, doneH, 3.0F, ColorUtil.multiplyAlpha(themeAccent, 0.80F * alpha));
        Render2D.drawRoundedOutline(doneX, doneY, doneW, doneH, 3.0F, 1.0F, themeAccent);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Готово", doneX + doneW / 2.0F, doneY + 2.0F, 6.0F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float modalW = 145.0F;
        float modalH = 88.0F;
        float screenW = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float screenH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float modalX = screenW - modalW - 20.0F;
        float modalY = (screenH - modalH) / 2.0F;

        float boxX = modalX + 8.0F;
        float boxY = modalY + 17.0F;
        float boxW = modalW - 16.0F;
        float boxH = 18.0F;

        // Click Key Listen Box -> Activate listening mode!
        if (mouseX >= boxX && mouseX <= boxX + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
            listening = true;
            return true;
        }

        // If listening and user clicks anywhere with mouse buttons
        if (listening) {
            module.getBind().setSingle(BindSetting.mouse(button));
            module.getBind().setAllModes(currentMode);
            module.getBind().setVisibleAt(0, !hideInKeybinds);
            listening = false;
            return true;
        }

        float modeY = boxY + boxH + 9.0F;
        float btnW = 34.0F;
        float btnH = 11.0F;
        float toggleX = modalX + 44.0F;
        float holdX = toggleX + btnW + 4.0F;

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
        float hideY = modeY + btnH + 7.0F;
        if (mouseX >= modalX + 8.0F && mouseX <= modalX + 130.0F && mouseY >= hideY && mouseY <= hideY + 10.0F) {
            hideInKeybinds = !hideInKeybinds;
            module.getBind().setVisibleAt(0, !hideInKeybinds);
            return true;
        }

        // Done Button
        float doneW = 38.0F;
        float doneH = 11.0F;
        float doneX = modalX + modalW - doneW - 8.0F;
        float doneY = modalY + modalH - doneH - 6.0F;

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
            if (listening) {
                listening = false;
                return true;
            }
            if (hadInitialBind) {
                module.getBind().clear();
            }
            finished = true;
            return true;
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
