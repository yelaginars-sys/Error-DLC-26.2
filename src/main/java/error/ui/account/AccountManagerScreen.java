package error.ui.account;

import error.account.AccountManager;
import error.util.RenderExtend;
import error.util.client.ClientSoundPlayer;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.display.batch.DisplayBatcher;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.UrlImageTexture;
import error.util.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Modern Liquid Glass Alt/Account Manager styled strictly after Error DLC LiquidClickGui.
 * Features Minotar 3D heads, search, smooth animations, star favorites,
 * trash icons, snowflakes in New Year mode, and full sound feedback.
 */
public class AccountManagerScreen extends Screen {

    private static final Identifier LOGO_NONFONE = Identifier.fromNamespaceAndPath("error", "textures/logo_nonfone.png");
    private static final Identifier ICON_TRASH = Identifier.fromNamespaceAndPath("error", "textures/system/trash.png");
    private static final Identifier STAR_FILLED = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/star-filled.png");
    private static final Identifier STAR_OUTLINE = Identifier.fromNamespaceAndPath("error", "nursultan/sprites/star.png");

    private final Screen parent;
    private final Animation openAlpha = new Animation(0.0F, 0.28F);

    private String searchText = "";
    private boolean searchFocused = false;
    private float scrollOffset = 0.0F;
    private float scrollTarget = 0.0F;

    private final Animation searchFocusAnim = new Animation(0.0F, 0.22F);
    private final Animation addBtnHover = new Animation(0.0F, 0.22F);
    private final Animation delBtnHover = new Animation(0.0F, 0.22F);
    private final Animation delAllBtnHover = new Animation(0.0F, 0.22F);
    private final Animation randomBtnHover = new Animation(0.0F, 0.22F);

    private final Map<String, RowAnimState> rowAnims = new HashMap<>();
    private final Set<String> removing = new HashSet<>();

    // Snowflakes for New Year theme
    private final List<Snowflake> snowflakes = new ArrayList<>();
    private static final class Snowflake {
        float x, y, speed, size;
        float seed;
    }

    private static final class RowAnimState {
        final Animation hover = new Animation(0.0F, 0.20F);
        final Animation active = new Animation(0.0F, 0.25F);
        final Animation appear = new Animation(0.0F, 0.28F);
        final Animation headFade = new Animation(0.0F, 0.25F);
        final Animation starHover = new Animation(0.0F, 0.20F);
        float curOff = 0.0F;
        boolean hasOff = false;
    }

    private static final float ROW_H = 32.0F;
    private static final float ROW_GAP = 5.0F;

    private float listX, listY, listW, listH;
    private float delBtnX, delBtnY, delBtnW, delBtnH;
    private float delAllBtnX, delAllBtnY, delAllBtnW, delAllBtnH;
    private float randomBtnX, randomBtnY, randomBtnW, randomBtnH;
    private float searchX, searchY, searchW, searchH;
    private float addBtnX, addBtnY, addBtnW, addBtnH;

    public AccountManagerScreen(Screen parent) {
        super(Component.literal("Account Manager"));
        this.parent = parent;

        Random rnd = new Random();
        for (int i = 0; i < 40; i++) {
            Snowflake s = new Snowflake();
            s.x = rnd.nextFloat() * 1200.0F;
            s.y = rnd.nextFloat() * 800.0F;
            s.speed = 12.0F + rnd.nextFloat() * 24.0F;
            s.size = 1.2F + rnd.nextFloat() * 2.2F;
            s.seed = rnd.nextFloat() * 100.0F;
            snowflakes.add(s);
        }
    }

    @Override
    protected void init() {
        super.init();
        openAlpha.setValue(0.0F);
        openAlpha.setTarget(1.0F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        openAlpha.update();
        searchFocusAnim.update();
        addBtnHover.update();
        delBtnHover.update();
        delAllBtnHover.update();
        randomBtnHover.update();

        float alpha = openAlpha.getValue();
        if (alpha <= 0.01F) return;

        int screenW = this.width;
        int screenH = this.height;

        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // 1. Dark atmospheric backdrop
            int backdropCol = Theme.isNewYear()
                    ? ColorUtil.rgba(6, 14, 28, (int) (185 * alpha))
                    : ColorUtil.rgba(4, 6, 12, (int) (190 * alpha));
            Render2D.drawRect(0, 0, screenW, screenH, backdropCol);

            // Falling snowflakes in New Year mode
            if (Theme.isNewYear()) {
                renderSnowflakes(screenW, screenH, alpha);
            }

            // 2. Main Window Dimensions
            float totalW = 430.0F;
            float totalH = Math.min(345.0F, screenH - 36.0F);
            float startX = (screenW - totalW) / 2.0F;
            float startY = (screenH - totalH) / 2.0F;

            int accent = Theme.getAccentColor();

            // Liquid glass / Blur container matching LiquidClickGui
            if (extractor != null && (Theme.isLiquidGlass() || Theme.isNewYear())) {
                Blur.of(startX, startY, totalW, totalH)
                        .radius(10)
                        .type(BlurType.KAWASE)
                        .strength(4)
                        .tint(Color.rgba(8, 10, 18, Math.round(180 * alpha)))
                        .alpha(alpha)
                        .render(extractor);

                Color topOutline = Theme.isNewYear() ? Color.rgba(160, 230, 255, Math.round(140 * alpha)) : Color.WHITE;
                Color botOutline = Theme.isNewYear() ? Color.rgba(110, 205, 255, Math.round(90 * alpha)) : Color.rgba(255, 255, 255, 28);
                Outline.of(startX, startY, totalW, totalH)
                        .radius(10)
                        .thickness(1.0F)
                        .verticalGradient(topOutline, botOutline)
                        .alpha(alpha)
                        .render(extractor);

                DisplayBatcher.flush();
            }

            Render2D.drawShadow(startX, startY, totalW, totalH, 10.0F, 12.0F, ColorUtil.rgba(0, 0, 0, (int) (130 * alpha)));
            int windowFill = Theme.isNewYear()
                    ? ColorUtil.rgba(10, 24, 44, (int) (230 * alpha))
                    : (Theme.isBlack() ? ColorUtil.rgba(14, 14, 18, (int) (245 * alpha)) : ColorUtil.rgba(12, 14, 22, (int) (230 * alpha)));
            Render2D.drawRoundedRect(startX, startY, totalW, totalH, 10.0F, windowFill);
            Render2D.drawRoundedOutline(startX, startY, totalW, totalH, 10.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int) (25 * alpha)));

            // Header Section: Logo + Title + Subtitle
            float logoX = startX + 14.0F;
            float logoY = startY + 11.0F;
            float logoSize = 22.0F;
            int logoColor = Theme.isGuiBlack()
                    ? ColorUtil.withAlpha(0xFFFFFFFF, (int) (255 * alpha))
                    : ColorUtil.withAlpha(accent, (int) (255 * alpha));
            Render2D.drawTexture(LOGO_NONFONE, logoX, logoY, logoSize, logoSize, logoColor);

            Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", logoX + logoSize + 6.0F, startY + 11.0F, 9.0F, 0xFFFFFFFF);
            Fonts.drawString(Fonts.SF_MEDIUM, "Winter 26.2 • Alt Manager", logoX + logoSize + 6.0F, startY + 22.0F, 5.2F, 0xFFA0B0C4);

            // Active Account Pill in top-right of header
            String active = AccountManager.getInstance().getActiveAccount();
            String activeDisp = active.isEmpty() ? "Не выбран" : active;
            float badgeTextW = Fonts.SF_MEDIUM.getWidth(activeDisp, 6.2F);
            float badgeW = badgeTextW + 24.0F;
            float badgeH = 15.0F;
            float badgeX = startX + totalW - badgeW - 14.0F;
            float badgeY = startY + 14.0F;

            Render2D.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (14 * alpha)));
            Render2D.drawRoundedOutline(badgeX, badgeY, badgeW, badgeH, 4.0F, 0.55F, ColorUtil.rgba(255, 255, 255, (int) (28 * alpha)));
            int dotCol = active.isEmpty() ? ColorUtil.rgba(255, 80, 80, (int) (240 * alpha)) : ColorUtil.rgba(60, 235, 110, (int) (240 * alpha));
            Render2D.drawCircle(badgeX + 7.0F, badgeY + badgeH / 2.0F, 2.3F, dotCol);
            Fonts.drawString(Fonts.SF_MEDIUM, activeDisp, badgeX + 14.0F, Fonts.SF_MEDIUM.centeredTextY(badgeY + badgeH / 2.0F, 6.2F), 6.2F, 0xFFE0E8F5);

            // Left accounts list container
            float leftW = totalW - 122.0F;
            float curY = startY + 42.0F;
            float leftH = totalH - 78.0F;

            listX = startX + 14.0F;
            listY = curY;
            listW = leftW;
            listH = leftH;

            int listBg = Theme.isNewYear() ? ColorUtil.rgba(8, 18, 36, (int) (160 * alpha)) : ColorUtil.rgba(16, 18, 28, (int) (160 * alpha));
            Render2D.drawRoundedRect(listX, listY, listW, listH, 7.0F, listBg);
            Render2D.drawRoundedOutline(listX, listY, listW, listH, 7.0F, 0.55F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

            // Right action buttons container
            float rightX = listX + listW + 8.0F;
            float rightW = 86.0F;
            float btnH = 22.0F;
            float btnGap = 6.0F;

            // 1. Delete Selected button
            delBtnX = rightX;
            delBtnY = curY;
            delBtnW = rightW;
            delBtnH = btnH;
            boolean delHov = isHovered(mouseX, mouseY, delBtnX, delBtnY, delBtnW, delBtnH);
            delBtnHover.setTarget(delHov ? 1.0F : 0.0F);
            float dhp = delBtnHover.getValue();
            Render2D.drawRoundedRect(delBtnX, delBtnY, delBtnW, delBtnH, 5.0F, ColorUtil.rgba(45, 16, 22, (int) ((130 + 60 * dhp) * alpha)));
            Render2D.drawRoundedOutline(delBtnX, delBtnY, delBtnW, delBtnH, 5.0F, 0.65F, ColorUtil.rgba(255, 75, 90, (int) ((140 + 80 * dhp) * alpha)));
            float delTextY = Fonts.SF_MEDIUM.centeredTextY(delBtnY + delBtnH / 2.0F, 6.6F);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Удалить", delBtnX + delBtnW / 2.0F, delTextY, 6.6F, ColorUtil.rgba(255, 190, 200, (int) (245 * alpha)));

            // 2. Delete All button (with Trash vector icon)
            delAllBtnX = rightX;
            delAllBtnY = curY + btnH + btnGap;
            delAllBtnW = rightW;
            delAllBtnH = btnH;
            boolean delAllHov = isHovered(mouseX, mouseY, delAllBtnX, delAllBtnY, delAllBtnW, delAllBtnH);
            delAllBtnHover.setTarget(delAllHov ? 1.0F : 0.0F);
            float dahp = delAllBtnHover.getValue();
            Render2D.drawRoundedRect(delAllBtnX, delAllBtnY, delAllBtnW, delAllBtnH, 5.0F, ColorUtil.rgba(55, 16, 22, (int) ((130 + 70 * dahp) * alpha)));
            Render2D.drawRoundedOutline(delAllBtnX, delAllBtnY, delAllBtnW, delAllBtnH, 5.0F, 0.65F, ColorUtil.rgba(255, 70, 85, (int) ((150 + 90 * dahp) * alpha)));

            float trashSize = 10.0F;
            float trashX = delAllBtnX + 8.0F;
            float trashY = delAllBtnY + (delAllBtnH - trashSize) / 2.0F;
            int trashCol = ColorUtil.rgba(255, 180, 190, (int) (245 * alpha));
            Render2D.drawTexture(ICON_TRASH, trashX, trashY, trashSize, trashSize, trashCol);

            float delAllTextY = Fonts.SF_MEDIUM.centeredTextY(delAllBtnY + delAllBtnH / 2.0F, 6.5F);
            Fonts.drawString(Fonts.SF_MEDIUM, "Очистить", trashX + trashSize + 5.0F, delAllTextY, 6.5F, ColorUtil.rgba(255, 190, 200, (int) (245 * alpha)));

            // 3. Random Button
            randomBtnX = rightX;
            randomBtnY = delAllBtnY + btnH + btnGap;
            randomBtnW = rightW;
            randomBtnH = btnH;
            boolean rndHov = isHovered(mouseX, mouseY, randomBtnX, randomBtnY, randomBtnW, randomBtnH);
            randomBtnHover.setTarget(rndHov ? 1.0F : 0.0F);
            float rhp = randomBtnHover.getValue();
            int rndBg = Theme.isNewYear() ? ColorUtil.rgba(14, 40, 75, (int) ((150 + 50 * rhp) * alpha)) : ColorUtil.rgba(24, 28, 42, (int) ((140 + 50 * rhp) * alpha));
            Render2D.drawRoundedRect(randomBtnX, randomBtnY, randomBtnW, randomBtnH, 5.0F, rndBg);
            Render2D.drawRoundedOutline(randomBtnX, randomBtnY, randomBtnW, randomBtnH, 5.0F, 0.65F, ColorUtil.withAlpha(accent, (int) ((130 + 100 * rhp) * alpha)));
            float rndTextY = Fonts.SF_MEDIUM.centeredTextY(randomBtnY + randomBtnH / 2.0F, 6.6F);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Рандом", randomBtnX + randomBtnW / 2.0F, rndTextY, 6.6F, ColorUtil.rgba(230, 235, 250, (int) (240 * alpha)));

            // Bottom Input bar & Add button
            float bottomY = startY + totalH - 28.0F;
            searchX = listX;
            searchY = bottomY;
            searchW = totalW - 58.0F;
            searchH = 20.0F;

            searchFocusAnim.setTarget(searchFocused ? 1.0F : 0.0F);
            Render2D.drawRoundedRect(searchX, searchY, searchW, searchH, 5.0F, ColorUtil.rgba(20, 24, 34, (int) (190 * alpha)));
            int searchBorder = searchFocused ? ColorUtil.withAlpha(accent, (int) (220 * alpha)) : ColorUtil.rgba(255, 255, 255, (int) (25 * alpha));
            Render2D.drawRoundedOutline(searchX, searchY, searchW, searchH, 5.0F, 0.65F, searchBorder);

            boolean blink = searchFocused && (System.currentTimeMillis() / 480L) % 2L == 0L;
            String placeholder = "Вписать ник / Найти ник...";
            String shownText = searchText.isEmpty() && !searchFocused ? placeholder : searchText + (blink ? "_" : "");
            int inputColor = searchText.isEmpty() && !searchFocused ? ColorUtil.rgba(140, 145, 165, (int) (160 * alpha)) : ColorUtil.rgba(245, 245, 255, (int) (245 * alpha));
            float inputTextY = Fonts.SF_MEDIUM.centeredTextY(searchY + searchH / 2.0F, 6.8F);
            Fonts.drawString(Fonts.SF_MEDIUM, shownText, searchX + 8.0F, inputTextY, 6.8F, inputColor);

            // Add (+) Button
            addBtnX = searchX + searchW + 4.0F;
            addBtnY = searchY;
            addBtnW = 20.0F;
            addBtnH = 20.0F;
            boolean addHov = isHovered(mouseX, mouseY, addBtnX, addBtnY, addBtnW, addBtnH);
            addBtnHover.setTarget(addHov ? 1.0F : 0.0F);
            float ahp = addBtnHover.getValue();
            Render2D.drawRoundedRect(addBtnX, addBtnY, addBtnW, addBtnH, 5.0F, ColorUtil.withAlpha(accent, (int) ((140 + 80 * ahp) * alpha)));
            Render2D.drawRoundedOutline(addBtnX, addBtnY, addBtnW, addBtnH, 5.0F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (140 * alpha)));
            float plusY = Fonts.SF_MEDIUM.centeredTextY(addBtnY + addBtnH / 2.0F, 8.5F);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "+", addBtnX + addBtnW / 2.0F, plusY, 8.5F, 0xFFFFFFFF);

            // Render Accounts List
            renderAccountList(mouseX, mouseY, alpha, accent);

        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderSnowflakes(int screenW, int screenH, float alphaVal) {
        long time = System.currentTimeMillis();
        for (Snowflake sf : snowflakes) {
            sf.y += sf.speed * 0.035F;
            sf.x += (float) Math.sin((time * 0.002F) + sf.seed) * 0.3F;

            if (sf.y > screenH + 8.0F) {
                sf.y = -8.0F;
                sf.x = (float) (Math.random() * screenW);
            }

            int flakeAlpha = (int) (70 * alphaVal);
            Render2D.drawCircle(sf.x, sf.y, sf.size, ColorUtil.rgba(220, 245, 255, flakeAlpha));
        }
    }

    private void renderAccountList(int mouseX, int mouseY, float alpha, int accent) {
        List<String> accounts = AccountManager.getInstance().getSortedAccounts();
        String current = AccountManager.getInstance().getActiveAccount();

        List<String> filtered = new ArrayList<>();
        String query = searchText.trim().toLowerCase();
        for (String acc : accounts) {
            if (query.isEmpty() || acc.toLowerCase().contains(query)) {
                filtered.add(acc);
            }
        }

        float contentH = filtered.size() * (ROW_H + ROW_GAP);
        float maxScroll = Math.max(0.0F, contentH - (listH - 8.0F));
        scrollTarget = Math.max(0.0F, Math.min(scrollTarget, maxScroll));
        scrollOffset += (scrollTarget - scrollOffset) * 0.35F;

        Render2DUtil.pushScissor(listX + 2.0F, listY + 3.0F, listW - 4.0F, listH - 6.0F);
        try {
            float off = 0.0F;
            List<String> finished = null;

            for (String acc : filtered) {
                RowAnimState an = rowAnims.computeIfAbsent(acc, k -> new RowAnimState());
                boolean leaving = removing.contains(acc);

                an.appear.setTarget(leaving ? 0.0F : 1.0F);
                an.appear.update();
                float ap = Math.max(0.0F, Math.min(1.0F, an.appear.getValue()));
                float slotH = (ROW_H + ROW_GAP) * ap;

                if (!an.hasOff) {
                    an.curOff = off;
                    an.hasOff = true;
                } else {
                    an.curOff += (off - an.curOff) * 0.35F;
                }

                float ry = listY + 4.0F - scrollOffset + an.curOff;
                String headUrl = "https://minotar.net/helm/" + acc + "/64.png";
                if (UrlImageTexture.isLoaded(headUrl)) {
                    an.headFade.setTarget(1.0F);
                }
                an.headFade.update();
                float hf = an.headFade.getValue();

                boolean visible = ry + slotH >= listY && ry <= listY + listH;
                boolean inside = mouseY >= listY && mouseY <= listY + listH;
                boolean hovered = !leaving && visible && inside && isHovered(mouseX, mouseY, listX + 4.0F, ry, listW - 8.0F, ROW_H);
                boolean active = acc.equalsIgnoreCase(current);

                an.hover.setTarget(hovered ? 1.0F : 0.0F);
                an.active.setTarget(active ? 1.0F : 0.0F);
                an.hover.update();
                an.active.update();

                float hp = an.hover.getValue();

                if (visible && ap > 0.01F) {
                    float ra = alpha * ap;
                    float rx = listX + 4.0F;
                    float rw = listW - 8.0F;

                    int rowBg = active
                            ? ColorUtil.withAlpha(accent, (int) (60 * ra))
                            : ColorUtil.rgba(255, 255, 255, (int) ((8 + 16 * hp) * ra));
                    Render2D.drawRoundedRect(rx, ry, rw, ROW_H, 6.0F, rowBg);

                    if (active) {
                        Render2D.drawRoundedOutline(rx, ry, rw, ROW_H, 6.0F, 1.0F, ColorUtil.withAlpha(accent, (int) ((200 + 55 * hp) * ra)));
                    } else {
                        Render2D.drawRoundedOutline(rx, ry, rw, ROW_H, 6.0F, 0.55F, ColorUtil.rgba(255, 255, 255, (int) ((15 + 25 * hp) * ra)));
                    }

                    // Head avatar with smooth download fade-in
                    float headSize = 22.0F;
                    float headX = rx + 5.0F;
                    float headY = ry + (ROW_H - headSize) / 2.0F;

                    Render2D.drawRoundedRect(headX, headY, headSize, headSize, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (20 * ra)));

                    Identifier skinId = UrlImageTexture.getIdentifier(headUrl);
                    if (skinId != null) {
                        Render2D.drawTexture(skinId, headX, headY, headSize, headSize, 4.0F, ColorUtil.rgba(255, 255, 255, (int) ((80 + 175 * hf) * ra)));
                    } else {
                        Render2D.drawCustomAvatar(headX, headY, headSize, 4.0F, ra * 0.8F);
                    }

                    // Nickname text
                    float tx = headX + headSize + 8.0F;
                    float nameY = Fonts.SF_MEDIUM.centeredTextY(ry + ROW_H / 2.0F, 7.5F);
                    boolean isFav = AccountManager.getInstance().isFavorite(acc);

                    int nameColor = active
                            ? 0xFFFFFFFF
                            : (isFav ? ColorUtil.rgba(255, 220, 80, (int) (245 * ra)) : ColorUtil.rgba(225, 230, 245, (int) ((200 + 45 * hp) * ra)));
                    Fonts.drawString(Fonts.SF_MEDIUM, acc, tx, nameY, 7.5F, nameColor);

                    // Favorite Star Texture Icon on the right
                    float favSize = 13.0F;
                    float favX = rx + rw - 22.0F;
                    float favY = ry + (ROW_H - favSize) / 2.0F;
                    boolean favHover = hovered && isHovered(mouseX, mouseY, favX - 3.0F, favY - 3.0F, favSize + 6.0F, favSize + 6.0F);
                    an.starHover.setTarget(favHover ? 1.0F : 0.0F);
                    an.starHover.update();
                    float shp = an.starHover.getValue();

                    if (isFav) {
                        int starCol = ColorUtil.rgba(255, (int) (200 + 55 * shp), 50, (int) (245 * ra));
                        Render2D.drawTexture(STAR_FILLED, favX, favY, favSize, favSize, starCol);
                    } else {
                        int starCol = ColorUtil.rgba(180, 190, 210, (int) ((80 + 160 * shp) * ra));
                        Render2D.drawTexture(STAR_OUTLINE, favX, favY, favSize, favSize, starCol);
                    }
                }

                if (leaving && ap <= 0.02F) {
                    if (finished == null) finished = new ArrayList<>();
                    finished.add(acc);
                }

                off += slotH;
            }

            if (finished != null) {
                for (String rem : finished) {
                    AccountManager.getInstance().removeAccount(rem);
                    rowAnims.remove(rem);
                    removing.remove(rem);
                }
            }

            if (filtered.isEmpty()) {
                float emptyY = Fonts.SF_MEDIUM.centeredTextY(listY + listH / 2.0F, 7.5F);
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Аккаунты не найдены", listX + listW / 2.0F, emptyY, 7.5F, ColorUtil.rgba(160, 170, 190, (int) (130 * alpha)));
            }

        } finally {
            Render2DUtil.popScissor();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mx = event.x();
        double my = event.y();

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // 1. Search Box Click
            if (isHovered(mx, my, searchX, searchY, searchW, searchH)) {
                searchFocused = true;
                return true;
            } else {
                searchFocused = false;
            }

            // 2. Add Button Click
            if (isHovered(mx, my, addBtnX, addBtnY, addBtnW, addBtnH)) {
                String toAdd = searchText.trim();
                if (!toAdd.isEmpty()) {
                    AccountManager.getInstance().addAccount(toAdd);
                    AccountManager.getInstance().selectAccount(toAdd);
                    searchText = "";
                    ClientSoundPlayer.playGuiClick();
                }
                return true;
            }

            // 3. Delete Selected Button
            if (isHovered(mx, my, delBtnX, delBtnY, delBtnW, delBtnH)) {
                String active = AccountManager.getInstance().getActiveAccount();
                if (!active.isEmpty()) {
                    removing.add(active);
                    ClientSoundPlayer.playGuiClick();
                }
                return true;
            }

            // 4. Delete All Button
            if (isHovered(mx, my, delAllBtnX, delAllBtnY, delAllBtnW, delAllBtnH)) {
                AccountManager.getInstance().removeAll();
                rowAnims.clear();
                removing.clear();
                ClientSoundPlayer.playGuiClick();
                return true;
            }

            // 5. Random Button
            if (isHovered(mx, my, randomBtnX, randomBtnY, randomBtnW, randomBtnH)) {
                AccountManager.getInstance().randomAccount();
                ClientSoundPlayer.playGuiClick();
                return true;
            }

            // 6. Account list item click
            if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
                List<String> accounts = AccountManager.getInstance().getSortedAccounts();
                String query = searchText.trim().toLowerCase();
                float off = 0.0F;

                for (String acc : accounts) {
                    if (!query.isEmpty() && !acc.toLowerCase().contains(query)) continue;
                    RowAnimState an = rowAnims.get(acc);
                    float curOff = an != null ? an.curOff : off;
                    float ry = listY + 4.0F - scrollOffset + curOff;
                    float rx = listX + 4.0F;
                    float rw = listW - 8.0F;

                    if (isHovered(mx, my, rx, ry, rw, ROW_H)) {
                        // Check if star icon was clicked
                        float favSize = 13.0F;
                        float favX = rx + rw - 22.0F;
                        float favY = ry + (ROW_H - favSize) / 2.0F;
                        if (isHovered(mx, my, favX - 3.0F, favY - 3.0F, favSize + 6.0F, favSize + 6.0F)) {
                            AccountManager.getInstance().toggleFavorite(acc);
                            ClientSoundPlayer.playGuiClick();
                            return true;
                        }

                        // Select active account
                        AccountManager.getInstance().selectAccount(acc);
                        ClientSoundPlayer.playGuiClick();
                        return true;
                    }
                    off += ROW_H + ROW_GAP;
                }
            }
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // Right Click on account row toggles favorite
            if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
                List<String> accounts = AccountManager.getInstance().getSortedAccounts();
                String query = searchText.trim().toLowerCase();
                float off = 0.0F;

                for (String acc : accounts) {
                    if (!query.isEmpty() && !acc.toLowerCase().contains(query)) continue;
                    RowAnimState an = rowAnims.get(acc);
                    float curOff = an != null ? an.curOff : off;
                    float ry = listY + 4.0F - scrollOffset + curOff;
                    float rx = listX + 4.0F;
                    float rw = listW - 8.0F;

                    if (isHovered(mx, my, rx, ry, rw, ROW_H)) {
                        AccountManager.getInstance().toggleFavorite(acc);
                        ClientSoundPlayer.playGuiClick();
                        return true;
                    }
                    off += ROW_H + ROW_GAP;
                }
            }
        }

        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            scrollTarget -= (float) (verticalAmount * 24.0D);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreenAndShow(this.parent);
            return true;
        }

        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ENTER) {
                String toAdd = searchText.trim();
                if (!toAdd.isEmpty()) {
                    AccountManager.getInstance().addAccount(toAdd);
                    AccountManager.getInstance().selectAccount(toAdd);
                    searchText = "";
                    searchFocused = false;
                    ClientSoundPlayer.playGuiClick();
                }
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused) {
            char c = (char) event.codepoint();
            if (c >= 32 && c != 127 && searchText.length() < 24) {
                searchText += c;
                return true;
            }
        }
        return super.charTyped(event);
    }

    private static boolean isHovered(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
