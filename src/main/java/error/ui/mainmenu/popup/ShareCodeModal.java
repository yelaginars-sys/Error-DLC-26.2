package error.ui.mainmenu.popup;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.ChatUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;

public class ShareCodeModal implements Modal {
    private final String configName;
    private int selectedUsages = 0; // 0 = Unlimited
    private boolean finished = false;

    private static final int[] USAGE_OPTIONS = {0, 1, 5, 10, 50, 100};
    private static final String[] USAGE_LABELS = {"INF", "1", "5", "10", "50", "100"};

    public ShareCodeModal(String configName) {
        this.configName = configName;
    }

    @Override
    public void render(int mouseX, int mouseY, float screenWidth, float screenHeight, float alpha) {
        float modalW = 220.0F;
        float modalH = 130.0F;
        float modalX = (screenWidth - modalW) / 2.0F;
        float modalY = (screenHeight - modalH) / 2.0F;

        int themeAccent = Theme.getAccentColor();
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (180 * alpha));
        int glassFill = ColorUtil.rgba(22, 18, 28, (int) (235 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (45 * alpha));
        int accentGlow = ColorUtil.multiplyAlpha(themeAccent, 0.25F * alpha);

        // Dimmed backdrop
        Render2D.drawRect(0, 0, (int) screenWidth, (int) screenHeight, ColorUtil.rgba(0, 0, 0, (int) (100 * alpha)));

        // Modal Frame
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 10.0F, 12.0F, shadowCol);
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 10.0F, 6.0F, accentGlow);
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 10.0F, 16.0F, glassFill, alpha);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 10.0F, glassFill);
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 10.0F, 1.0F, glassBorder);

        // Header Title
        String title = "Создать код конфига  —  " + configName;
        Fonts.drawString(Fonts.SF_MEDIUM, title, modalX + 12.0F, modalY + 10.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

        // Subtitle
        Fonts.drawString(Fonts.SF_MEDIUM, "Выберите лимит использований:", modalX + 12.0F, modalY + 28.0F, 7.5F, ColorUtil.rgba(200, 195, 215, (int) (200 * alpha)));

        // Chips for Usage Limits
        float chipY = modalY + 44.0F;
        float chipW = 30.0F;
        float chipH = 16.0F;
        float chipGap = 4.0F;
        float startX = modalX + 12.0F;

        for (int i = 0; i < USAGE_OPTIONS.length; i++) {
            float cx = startX + i * (chipW + chipGap);
            boolean isSelected = (selectedUsages == USAGE_OPTIONS[i]);

            int chipBg = isSelected ? ColorUtil.multiplyAlpha(themeAccent, 0.45F * alpha) : ColorUtil.rgba(45, 38, 56, (int) (140 * alpha));
            int chipBorder = isSelected ? themeAccent : ColorUtil.rgba(255, 255, 255, (int) (30 * alpha));

            Render2D.drawRoundedRect(cx, chipY, chipW, chipH, 4.0F, chipBg);
            Render2D.drawRoundedOutline(cx, chipY, chipW, chipH, 4.0F, 1.0F, chipBorder);

            Fonts.drawCenteredString(Fonts.SF_MEDIUM, USAGE_LABELS[i], cx + chipW / 2.0F, chipY + 3.5F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        }

        // Action Buttons: Cancel & Create
        float btnW = 90.0F;
        float btnH = 18.0F;
        float cancelX = modalX + 12.0F;
        float createX = modalX + modalW - btnW - 12.0F;
        float btnY = modalY + modalH - btnH - 12.0F;

        // Cancel Button
        Render2D.drawRoundedRect(cancelX, btnY, btnW, btnH, 5.0F, ColorUtil.rgba(40, 35, 50, (int) (150 * alpha)));
        Render2D.drawRoundedOutline(cancelX, btnY, btnW, btnH, 5.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int) (30 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Отмена", cancelX + btnW / 2.0F, btnY + 4.5F, 7.5F, ColorUtil.rgba(220, 215, 230, (int) (220 * alpha)));

        // Create & Copy Button
        Render2D.drawRoundedRect(createX, btnY, btnW, btnH, 5.0F, ColorUtil.multiplyAlpha(themeAccent, 0.80F * alpha));
        Render2D.drawRoundedOutline(createX, btnY, btnW, btnH, 5.0F, 1.0F, themeAccent);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Скопировать", createX + btnW / 2.0F, btnY + 4.5F, 7.5F, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float modalW = 220.0F;
        float modalH = 130.0F;
        float modalX = (Minecraft.getInstance().getWindow().getGuiScaledWidth() - modalW) / 2.0F;
        float modalY = (Minecraft.getInstance().getWindow().getGuiScaledHeight() - modalH) / 2.0F;

        float chipY = modalY + 44.0F;
        float chipW = 30.0F;
        float chipH = 16.0F;
        float chipGap = 4.0F;
        float startX = modalX + 12.0F;

        for (int i = 0; i < USAGE_OPTIONS.length; i++) {
            float cx = startX + i * (chipW + chipGap);
            if (mouseX >= cx && mouseX <= cx + chipW && mouseY >= chipY && mouseY <= chipY + chipH) {
                selectedUsages = USAGE_OPTIONS[i];
                return true;
            }
        }

        float btnW = 90.0F;
        float btnH = 18.0F;
        float cancelX = modalX + 12.0F;
        float createX = modalX + modalW - btnW - 12.0F;
        float btnY = modalY + modalH - btnH - 12.0F;

        if (mouseX >= cancelX && mouseX <= cancelX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            finished = true;
            return true;
        }

        if (mouseX >= createX && mouseX <= createX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            String code = Client.INSTANCE.configManager.createShareCode(configName, selectedUsages);
            if (code != null) {
                Minecraft.getInstance().keyboardHandler.setClipboard(code);
                ChatUtil.success("Код конфигурации скопирован! (" + (selectedUsages <= 0 ? "Безлимитно" : selectedUsages + " исп.") + ")");
            } else {
                ChatUtil.error("Не удалось сгенерировать код.");
            }
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
            finished = true;
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
