package error.ui.mainmenu;

import error.account.AccountManager;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AltManagerScreen extends Screen {

    private static final Identifier BG_TEX = Identifier.fromNamespaceAndPath("client", "textures/mainmenu/background.png");
    private final Screen parent;

    private float screenAlpha = 1.0F;
    private Screen targetScreen = null;

    private String searchText = "";
    private boolean searchFocused = false;
    private float scroll = 0.0F;
    private float scrollTarget = 0.0F;

    private final float[] btnHoverAnims = new float[4]; // 0: Delete, 1: Delete All, 2: Random, 3: Add

    public AltManagerScreen(Screen parent) {
        super(Component.literal("AltManager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.screenAlpha = 1.0F;
        this.targetScreen = null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {}

    @Override
    protected void extractPanorama(GuiGraphicsExtractor extractor, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int screenW = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenH = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

        if (this.targetScreen != null) {
            this.screenAlpha = Math.max(0.0F, this.screenAlpha - 0.12F);
            if (this.screenAlpha <= 0.01F) {
                this.minecraft.setScreenAndShow(this.targetScreen);
                return;
            }
        } else {
            this.screenAlpha = Math.min(1.0F, this.screenAlpha + 0.10F);
        }

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            // 1. Fullscreen dark background texture
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(6, 8, 12, (int) (255 * this.screenAlpha)));
            Render2D.drawTexture(BG_TEX, 0, 0, screenW, screenH, ColorUtil.rgba(240, 245, 255, (int) (255 * this.screenAlpha)));
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(5, 8, 14, (int) (45 * this.screenAlpha)));

            int topFade = ColorUtil.rgba(3, 5, 8, (int) (45 * this.screenAlpha));
            int botFade = ColorUtil.rgba(3, 5, 8, (int) (95 * this.screenAlpha));
            Render2D.drawGradientRound(0, 0, screenW, screenH, 0.0F, topFade, topFade, botFade, botFade);

            // 2. Center Glass Panel
            renderPanel(screenW, screenH, mouseX, mouseY);

            Render2DUtil.flush();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderPanel(int screenW, int screenH, int mouseX, int mouseY) {
        float panelW = 420.0F;
        float panelH = 280.0F;
        float panelX = (screenW - panelW) / 2.0F;
        float panelY = (screenH - panelH) / 2.0F;

        int accent = Theme.getAccentColor();
        int glassFill = ColorUtil.rgba(16, 14, 26, (int) (235 * this.screenAlpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (45 * this.screenAlpha));

        // Shadows & Blur container
        Render2D.drawShadow(panelX, panelY, panelW, panelH, 12.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (240 * this.screenAlpha)));
        Render2D.drawBlur(panelX, panelY, panelW, panelH, 12.0F, 22.0F, glassFill, this.screenAlpha);
        Render2D.drawRoundedRect(panelX, panelY, panelW, panelH, 12.0F, glassFill);
        Render2D.drawRoundedOutline(panelX, panelY, panelW, panelH, 12.0F, 1.0F, glassBorder);

        // Header Title
        float titleY = panelY + 12.0F;
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "AltManager", screenW / 2.0F, titleY, 11.5F, ColorUtil.rgba(255, 255, 255, (int) (250 * this.screenAlpha)));

        float curY = titleY + 22.0F;
        float leftW = panelW - 120.0F;
        float listX = panelX + 14.0F;
        float listY = curY;
        float listH = panelH - (listY - panelY) - 44.0F;

        // Account List Glass Container
        Render2D.drawRoundedRect(listX, listY, leftW, listH, 6.0F, ColorUtil.rgba(22, 26, 42, (int) (180 * this.screenAlpha)));
        Render2D.drawRoundedOutline(listX, listY, leftW, listH, 6.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (30 * this.screenAlpha)));

        renderAccounts(listX, listY, leftW, listH, mouseX, mouseY);

        // Action Buttons on Right
        float rightX = listX + leftW + 10.0F;
        float rightW = panelW - (rightX - panelX) - 14.0F;
        float btnH = 22.0F;
        float btnGap = 6.0F;

        // Button 0: Delete
        float delY = listY;
        boolean delHov = mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= delY && mouseY <= delY + btnH;
        btnHoverAnims[0] = Mth.clamp(btnHoverAnims[0] + (delHov ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderActionButton(rightX, delY, rightW, btnH, "Удалить", btnHoverAnims[0], true);

        // Button 1: Delete All
        float delAllY = delY + btnH + btnGap;
        boolean delAllHov = mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= delAllY && mouseY <= delAllY + btnH;
        btnHoverAnims[1] = Mth.clamp(btnHoverAnims[1] + (delAllHov ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderActionButton(rightX, delAllY, rightW, btnH, "Удалить все", btnHoverAnims[1], true);

        // Button 2: Random
        float randY = delAllY + btnH + btnGap;
        boolean randHov = mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= randY && mouseY <= randY + btnH;
        btnHoverAnims[2] = Mth.clamp(btnHoverAnims[2] + (randHov ? 0.14F : -0.14F), 0.0F, 1.0F);
        renderActionButton(rightX, randY, rightW, btnH, "Рандом", btnHoverAnims[2], false);

        // Input Field & Add Button at Bottom
        float inputY = panelY + panelH - 34.0F;
        float inputX = listX;
        float inputW = panelW - 66.0F;
        float inputH = 22.0F;

        int searchCol = searchFocused ? accent : ColorUtil.rgba(255, 255, 255, 35);
        Render2D.drawRoundedRect(inputX, inputY, inputW, inputH, 5.0F, ColorUtil.rgba(22, 26, 42, (int) (220 * this.screenAlpha)));
        Render2D.drawRoundedOutline(inputX, inputY, inputW, inputH, 5.0F, 0.8F, ColorUtil.multiplyAlpha(searchCol, this.screenAlpha));

        boolean blink = (System.currentTimeMillis() / 450L) % 2 == 0;
        String placeholder = "вписать ник / найти ник...";
        String shownSearch = searchText.isEmpty() && !searchFocused ? placeholder : searchText + (searchFocused && blink ? "|" : "");
        int textCol = searchText.isEmpty() && !searchFocused ? ColorUtil.rgba(140, 145, 165, (int) (180 * this.screenAlpha)) : ColorUtil.rgba(240, 245, 255, (int) (255 * this.screenAlpha));
        Fonts.drawString(Fonts.SF_MEDIUM, shownSearch, inputX + 8.0F, inputY + 6.5F, 6.5F, textCol);

        // Add Button (+)
        float addX = inputX + inputW + 6.0F;
        float addW = 26.0F;
        float addH = 22.0F;

        boolean addHov = mouseX >= addX && mouseX <= addX + addW && mouseY >= inputY && mouseY <= inputY + addH;
        btnHoverAnims[3] = Mth.clamp(btnHoverAnims[3] + (addHov ? 0.14F : -0.14F), 0.0F, 1.0F);

        int addBg = ColorUtil.withAlpha(accent, (int) ((0.60F + 0.35F * btnHoverAnims[3]) * 255 * this.screenAlpha));
        Render2D.drawRoundedRect(addX, inputY, addW, addH, 5.0F, addBg);
        Render2D.drawRoundedOutline(addX, inputY, addW, addH, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (50 * this.screenAlpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "+", addX + addW / 2.0F, inputY + 5.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * this.screenAlpha)));
    }

    private void renderAccounts(float listX, float listY, float listW, float listH, int mouseX, int mouseY) {
        AccountManager manager = AccountManager.getInstance();
        List<String> accs = manager.getSortedAccounts();
        List<String> filtered = new ArrayList<>();

        String query = searchText.trim().toLowerCase(Locale.ROOT);
        for (String acc : accs) {
            if (query.isEmpty() || acc.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(acc);
            }
        }

        float rowH = 26.0F;
        float gap = 3.0F;
        float maxScroll = Math.max(0.0F, filtered.size() * (rowH + gap) - listH);

        scrollTarget = Mth.clamp(scrollTarget, 0.0F, maxScroll);
        scroll += (scrollTarget - scroll) * 0.35F;

        Render2D.pushScissor(listX, listY, listW, listH);
        float itemY = listY - scroll + 3.0F;
        String currentAcc = manager.getActiveAccount();

        for (String acc : filtered) {
            if (itemY + rowH >= listY && itemY <= listY + listH) {
                boolean active = acc.equalsIgnoreCase(currentAcc);
                boolean hov = mouseX >= listX + 3.0F && mouseX <= listX + listW - 3.0F && mouseY >= itemY && mouseY <= itemY + rowH;

                int rowBg = active ? ColorUtil.withAlpha(Theme.getAccentColor(), (int) (170 * this.screenAlpha))
                        : (hov ? ColorUtil.rgba(36, 42, 64, (int) (200 * this.screenAlpha)) : ColorUtil.rgba(24, 28, 44, (int) (160 * this.screenAlpha)));

                float rx = listX + 3.0F;
                float rw = listW - 6.0F;

                Render2D.drawRoundedRect(rx, itemY, rw, rowH, 5.0F, rowBg);
                Render2D.drawRoundedOutline(rx, itemY, rw, rowH, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (35 * this.screenAlpha)));

                // Head Avatar
                UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + acc).getBytes(StandardCharsets.UTF_8));
                Identifier skin = DefaultPlayerSkin.get(uuid).body().texturePath();
                Render2D.drawHead(skin, rx + 4.0F, itemY + 4.0F, 18.0F, 3.5F, this.screenAlpha);

                // Name
                Fonts.drawString(Fonts.SF_MEDIUM, acc, rx + 28.0F, itemY + 8.0F, 7.2F, ColorUtil.rgba(255, 255, 255, (int) (250 * this.screenAlpha)));

                // Favorite Star
                boolean fav = manager.isFavorite(acc);
                int starCol = fav ? ColorUtil.rgba(255, 215, 0, (int) (255 * this.screenAlpha)) : ColorUtil.rgba(130, 140, 160, (int) (180 * this.screenAlpha));
                Fonts.drawString(Fonts.SF_MEDIUM, fav ? "★" : "☆", rx + rw - 16.0F, itemY + 7.5F, 8.0F, starCol);
            }
            itemY += rowH + gap;
        }

        if (filtered.isEmpty()) {
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Аккаунты не найдены", listX + listW / 2.0F, listY + listH / 2.0F - 4.0F, 7.5F, ColorUtil.rgba(180, 185, 205, (int) (180 * this.screenAlpha)));
        }

        Render2D.popScissor();
    }

    private void renderActionButton(float x, float y, float w, float h, String text, float hoverAnim, boolean isDanger) {
        float drawY = y - 1.0F * hoverAnim;
        int bg = isDanger ? ColorUtil.rgba(200, 45, 55, (int) ((0.25F + 0.35F * hoverAnim) * 255 * this.screenAlpha))
                : ColorUtil.rgba(28, 32, 48, (int) ((0.60F + 0.25F * hoverAnim) * 255 * this.screenAlpha));

        int outline = isDanger ? ColorUtil.rgba(240, 70, 80, (int) ((0.20F + 0.40F * hoverAnim) * 255 * this.screenAlpha))
                : ColorUtil.rgba(255, 255, 255, (int) ((0.08F + 0.16F * hoverAnim) * 255 * this.screenAlpha));

        Render2D.drawRoundedRect(x, drawY, w, h, 5.0F, bg);
        Render2D.drawRoundedOutline(x, drawY, w, h, 5.0F, 0.7F, outline);

        int textCol = isDanger ? ColorUtil.rgba(255, 190, 190, (int) (250 * this.screenAlpha))
                : ColorUtil.rgba(245, 245, 255, (int) (250 * this.screenAlpha));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, x + w / 2.0F, drawY + 6.0F, 6.5F, textCol);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int screenW = this.width;
        int screenH = this.height;

        float panelW = 420.0F;
        float panelH = 280.0F;
        float panelX = (screenW - panelW) / 2.0F;
        float panelY = (screenH - panelH) / 2.0F;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float titleY = panelY + 12.0F;
            float curY = titleY + 22.0F;
            float leftW = panelW - 120.0F;
            float listX = panelX + 14.0F;
            float listY = curY;
            float listH = panelH - (listY - panelY) - 44.0F;

            // List Item Selection & Favorite Toggle
            AccountManager manager = AccountManager.getInstance();
            List<String> accs = manager.getSortedAccounts();
            List<String> filtered = new ArrayList<>();

            String query = searchText.trim().toLowerCase(Locale.ROOT);
            for (String acc : accs) {
                if (query.isEmpty() || acc.toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(acc);
                }
            }

            float rowH = 26.0F;
            float gap = 3.0F;
            float itemY = listY - scroll + 3.0F;

            for (String acc : filtered) {
                if (itemY + rowH >= listY && itemY <= listY + listH) {
                    float rx = listX + 3.0F;
                    float rw = leftW - 6.0F;
                    if (mouseX >= rx && mouseX <= rx + rw && mouseY >= itemY && mouseY <= itemY + rowH) {
                        if (mouseX >= rx + rw - 22.0F) {
                            manager.toggleFavorite(acc);
                        } else {
                            manager.setSession(acc);
                        }
                        return true;
                    }
                }
                itemY += rowH + gap;
            }

            // Search Box Focus
            float inputY = panelY + panelH - 34.0F;
            float inputX = listX;
            float inputW = panelW - 66.0F;
            float inputH = 22.0F;

            if (mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH) {
                searchFocused = true;
                return true;
            } else {
                searchFocused = false;
            }

            // Action Buttons
            float rightX = listX + leftW + 10.0F;
            float rightW = panelW - (rightX - panelX) - 14.0F;
            float btnH = 22.0F;
            float btnGap = 6.0F;

            // Delete Button
            float delY = listY;
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= delY && mouseY <= delY + btnH) {
                manager.removeAccount(manager.getActiveAccount());
                return true;
            }

            // Delete All Button
            float delAllY = delY + btnH + btnGap;
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= delAllY && mouseY <= delAllY + btnH) {
                for (String acc : new ArrayList<>(manager.getAccounts())) {
                    manager.removeAccount(acc);
                }
                return true;
            }

            // Random Button
            float randY = delAllY + btnH + btnGap;
            if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= randY && mouseY <= randY + btnH) {
                String randomName = generateRandomName();
                manager.setSession(randomName);
                scrollTarget = Float.MAX_VALUE;
                return true;
            }

            // Add Button (+)
            float addX = inputX + inputW + 6.0F;
            float addW = 26.0F;
            if (mouseX >= addX && mouseX <= addX + addW && mouseY >= inputY && mouseY <= inputY + inputH) {
                if (!searchText.trim().isEmpty()) {
                    manager.setSession(searchText.trim());
                    searchText = "";
                    scrollTarget = Float.MAX_VALUE;
                } else {
                    searchFocused = true;
                }
                return true;
            }
        }

        return super.mouseClicked(event, isLeftClick);
    }

    private String generateRandomName() {
        String pool = "abcdefghijklmnopqrstuvwxyz0123456789";
        java.util.Random r = new java.util.Random();
        StringBuilder sb = new StringBuilder("Player_");
        for (int i = 0; i < 5; i++) {
            sb.append(pool.charAt(r.nextInt(pool.length())));
        }
        return sb.toString();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!searchText.trim().isEmpty()) {
                    AccountManager.getInstance().setSession(searchText.trim());
                    searchText = "";
                    scrollTarget = Float.MAX_VALUE;
                }
                searchFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreenAndShow(this.parent);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char codePoint = (char) event.codepoint();
        if (searchFocused && searchText.length() < 16 && (Character.isLetterOrDigit(codePoint) || codePoint == '_')) {
            searchText += codePoint;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget -= (float) verticalAmount * 22.0F;
        return true;
    }
}
