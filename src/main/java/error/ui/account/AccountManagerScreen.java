package error.ui.account;

import error.account.AccountManager;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AccountManagerScreen extends Screen {

    private final Screen parent;
    private EditBox accountInput;
    private float scrollOffset = 0.0F;

    public AccountManagerScreen(Screen parent) {
        super(Component.literal("Account Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int screenW = this.width;
        int screenH = this.height;

        float windowW = 440.0F;
        float windowH = 300.0F;
        float x = (screenW - windowW) / 2.0F;
        float y = (screenH - windowH) / 2.0F;

        // Account Name Input Box
        float inputW = 240.0F;
        float inputH = 22.0F;
        float inputX = x + 20.0F;
        float inputY = y + 55.0F;

        this.accountInput = new EditBox(this.font, (int) inputX, (int) inputY, (int) inputW, (int) inputH, Component.literal("Имя аккаунта"));
        this.accountInput.setMaxLength(32);
        this.addRenderableWidget(this.accountInput);

        // Add Account Button
        float addBtnW = 140.0F;
        float addBtnX = inputX + inputW + 12.0F;
        this.addRenderableWidget(Button.builder(Component.literal("Добавить аккаунт"), b -> {
            String name = accountInput.getValue().trim();
            if (!name.isEmpty()) {
                AccountManager.getInstance().addAccount(name);
                accountInput.setValue("");
            }
        }).bounds((int) addBtnX, (int) inputY, (int) addBtnW, (int) inputH).build());

        // Back Button at bottom
        float backW = 120.0F;
        float backH = 20.0F;
        float backX = x + (windowW - backW) / 2.0F;
        float backY = y + windowH - 32.0F;
        this.addRenderableWidget(Button.builder(Component.literal("Назад"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(this.parent);
            }
        }).bounds((int) backX, (int) backY, (int) backW, (int) backH).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int screenW = this.width;
        int screenH = this.height;

        float windowW = 440.0F;
        float windowH = 300.0F;
        float x = (screenW - windowW) / 2.0F;
        float y = (screenH - windowH) / 2.0F;

        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // Background veil tint
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(6, 8, 14, 180));

            // Container Window Glass
            int glassBg = ColorUtil.rgba(14, 16, 24, 235);
            int borderGlow = ColorUtil.rgba(255, 255, 255, 40);
            int shadowCol = ColorUtil.rgba(0, 0, 0, 200);

            Render2D.drawShadow(x, y, windowW, windowH, 14.0F, 14.0F, shadowCol);
            Render2D.drawRoundedRect(x, y, windowW, windowH, 14.0F, glassBg);
            Render2D.drawRoundedOutline(x, y, windowW, windowH, 14.0F, 0.8F, borderGlow);

            // Title & Subtitle
            Fonts.drawString(Fonts.SF_MEDIUM, "Менеджер аккаунтов", x + 20.0F, y + 16.0F, 11.0F, 0xFFFFFFFF);
            String activeAcc = AccountManager.getInstance().getActiveAccount();
            String activeText = "Текущий аккаунт: " + (activeAcc.isEmpty() ? "Не выбран" : activeAcc);
            Fonts.drawString(Fonts.SF_MEDIUM, activeText, x + 20.0F, y + 32.0F, 6.0F, ColorUtil.rgba(160, 175, 205, 210));

            // Accounts List Header
            float listY = y + 90.0F;
            float listH = windowH - 130.0F;
            float listW = windowW - 40.0F;
            float listX = x + 20.0F;

            Render2D.drawRoundedRect(listX, listY, listW, listH, 8.0F, ColorUtil.rgba(20, 24, 36, 120));
            Render2D.drawRoundedOutline(listX, listY, listW, listH, 8.0F, 0.75F, ColorUtil.rgba(255, 255, 255, 25));

            List<String> accounts = AccountManager.getInstance().getAccounts();
            if (accounts.isEmpty()) {
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Список аккаунтов пуст. Добавьте новый ник в поле выше!", listX + listW / 2.0F, listY + listH / 2.0F - 4.0F, 7.0F, ColorUtil.rgba(150, 165, 190, 180));
            } else {
                float itemY = listY + 6.0F - scrollOffset;
                float itemH = 26.0F;

                Render2DUtil.pushScissor(listX, listY + 4.0F, listW, listH - 8.0F);
                try {
                    for (String acc : accounts) {
                        if (itemY + itemH >= listY && itemY <= listY + listH) {
                            boolean isActive = acc.equalsIgnoreCase(activeAcc);
                            boolean isFav = AccountManager.getInstance().isFavorite(acc);

                            int itemBg = isActive
                                    ? ColorUtil.rgba(37, 117, 252, 60)
                                    : ColorUtil.rgba(255, 255, 255, 12);
                            int itemBorder = isActive
                                    ? ColorUtil.rgba(37, 117, 252, 160)
                                    : ColorUtil.rgba(255, 255, 255, 20);

                            Render2D.drawRoundedRect(listX + 6.0F, itemY, listW - 12.0F, itemH, 6.0F, itemBg);
                            Render2D.drawRoundedOutline(listX + 6.0F, itemY, listW - 12.0F, itemH, 6.0F, 0.7F, itemBorder);

                            // Account Icon & Name
                            String prefix = isFav ? "★ " : "";
                            int textCol = isActive ? 0xFFFFFFFF : ColorUtil.rgba(215, 225, 240, 230);
                            Fonts.drawString(Fonts.SF_MEDIUM, prefix + acc + (isActive ? " (Активен)" : ""), listX + 16.0F, itemY + 8.5F, 7.5F, textCol);

                            // Select Button
                            if (!isActive) {
                                float selectBtnW = 60.0F;
                                float selectBtnH = 16.0F;
                                float selectBtnX = listX + listW - 145.0F;
                                float selectBtnY = itemY + 5.0F;

                                boolean isSelHover = mouseX >= selectBtnX && mouseX <= selectBtnX + selectBtnW && mouseY >= selectBtnY && mouseY <= selectBtnY + selectBtnH;
                                int selBg = ColorUtil.rgba(37, 117, 252, isSelHover ? 220 : 160);

                                Render2D.drawRoundedRect(selectBtnX, selectBtnY, selectBtnW, selectBtnH, 4.0F, selBg);
                                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Выбрать", selectBtnX + selectBtnW / 2.0F, selectBtnY + 4.0F, 6.0F, 0xFFFFFFFF);
                            }

                            // Favorite Button
                            float favW = 16.0F;
                            float favH = 16.0F;
                            float favX = listX + listW - 75.0F;
                            float favY = itemY + 5.0F;
                            int favCol = isFav ? ColorUtil.rgba(255, 200, 50, 240) : ColorUtil.rgba(180, 190, 205, 140);
                            Fonts.drawString(Fonts.ICONS, IconUse.STAR.getGlyph(), favX + 3.0F, favY + 3.5F, 9.0F, favCol);

                            // Delete Button
                            float delX = listX + listW - 45.0F;
                            float delY = itemY + 5.0F;
                            Fonts.drawString(Fonts.ICONS, IconUse.CROSS.getGlyph(), delX + 3.0F, delY + 3.5F, 9.0F, ColorUtil.rgba(255, 80, 80, 200));
                        }
                        itemY += itemH + 4.0F;
                    }
                } finally {
                    Render2DUtil.popScissor();
                }
            }
        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int screenW = this.width;
            int screenH = this.height;

            float windowW = 440.0F;
            float windowH = 300.0F;
            float x = (screenW - windowW) / 2.0F;
            float y = (screenH - windowH) / 2.0F;

            double mouseX = event.x();
            double mouseY = event.y();

            float listY = y + 90.0F;
            float listH = windowH - 130.0F;
            float listW = windowW - 40.0F;
            float listX = x + 20.0F;

            List<String> accounts = AccountManager.getInstance().getAccounts();
            float itemY = listY + 6.0F - scrollOffset;
            float itemH = 26.0F;

            for (String acc : accounts) {
                if (mouseY >= listY && mouseY <= listY + listH && mouseY >= itemY && mouseY <= itemY + itemH) {
                    boolean isActive = acc.equalsIgnoreCase(AccountManager.getInstance().getActiveAccount());

                    // Select Button Click
                    float selectBtnW = 60.0F;
                    float selectBtnH = 16.0F;
                    float selectBtnX = listX + listW - 145.0F;
                    float selectBtnY = itemY + 5.0F;

                    if (!isActive && mouseX >= selectBtnX && mouseX <= selectBtnX + selectBtnW && mouseY >= selectBtnY && mouseY <= selectBtnY + selectBtnH) {
                        AccountManager.getInstance().selectAccount(acc);
                        return true;
                    }

                    // Favorite Button Click
                    float favX = listX + listW - 75.0F;
                    float favY = itemY + 5.0F;
                    if (mouseX >= favX && mouseX <= favX + 20.0F && mouseY >= favY && mouseY <= favY + 16.0F) {
                        AccountManager.getInstance().toggleFavorite(acc);
                        return true;
                    }

                    // Delete Button Click
                    float delX = listX + listW - 45.0F;
                    float delY = itemY + 5.0F;
                    if (mouseX >= delX && mouseX <= delX + 20.0F && mouseY >= delY && mouseY <= delY + 16.0F) {
                        AccountManager.getInstance().removeAccount(acc);
                        return true;
                    }
                }
                itemY += itemH + 4.0F;
            }
        }
        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(this.parent);
            }
            return true;
        }
        return super.keyPressed(event);
    }
}
