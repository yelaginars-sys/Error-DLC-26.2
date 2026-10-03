package error.ui.nova;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.BindSetting;
import error.ui.modern.ModernAnim;
import error.util.RenderExtend;
import error.util.client.ClientSoundPlayer;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * NovaGui — Цельный Liquid Glass ClickGUI с плавной регулировкой RGB цветов без зацепок,
 * единым шейдерным фоном меню настроек и сбросом поиска.
 */
public class NovaGui extends Screen {

    public static final float W          = 520.0F;
    public static final float H          = 320.0F;
    public static final float SIDEBAR_W  = 125.0F;
    public static final float HEADER_H   =  26.0F;

    public enum NavSection {
        COMBAT   (Category.COMBAT,   "Бой",        "textures/system/combat.png",   "ФУНКЦИИ"),
        MOVEMENT (Category.MOVEMENT, "Движение",   "textures/system/movement.png", null),
        RENDER   (Category.RENDER,   "Визуалы",    "textures/system/visuals.png",  null),
        PLAYER   (Category.PLAYER,   "Игрок",      "textures/system/player.png",   null),
        MISC     (Category.MISC,     "Разное",     "textures/system/misc.png",     null),
        PRESETS  (null,              "Пресеты",    "textures/system/presets.png",  "УПРАВЛЕНИЕ"),
        ACCOUNTS (null,              "Аккаунты",   "textures/system/accounts.png", null);

        public final Category category;
        public final String   label;
        public final String   iconTexture;
        public final String   groupHeader;
        NavSection(Category c, String l, String tex, String gh) {
            category = c;
            label = l;
            iconTexture = tex;
            groupHeader = gh;
        }
    }

    public record FontHolder(MsdfFont font, float size) {}

    public NavSection        activeSection     = NavSection.COMBAT;
    public Module            selectedModule    = null;
    public boolean           settingsPanelOpen = false;

    // Контекстное меню модуля на колесико (СКМ)
    public boolean contextMenuOpen = false;
    public Module  contextModule   = null;
    public float   contextX        = 0.0F;
    public float   contextY        = 0.0F;

    public void openContextMenu(Module m, float mx, float my) {
        this.contextModule   = m;
        this.contextX        = mx;
        this.contextY        = my;
        this.contextMenuOpen = true;
    }

    // Состояние Color Picker
    public boolean editingSecondary = false;
    public float   pickerHue        = 0.60F;
    public float   pickerSat        = 0.85F;
    public float   pickerVal        = 0.95F;
    private boolean draggingPickerBox = false;
    private boolean draggingHueBar   = false;

    // Координаты колор-пикера для ультра-плавного драга без зацепок
    private float lastBoxX = 0.0F, lastBoxY = 0.0F, lastBoxW = 145.0F, lastBoxH = 95.0F;
    private float lastHueX = 0.0F, lastHueY = 0.0F, lastHueW = 12.0F,  lastHueH = 95.0F;

    public static String savedSearchQuery = "";
    public String searchQuery = "";
    public float  scroll      = 0.0F;
    public float  maxScroll   = 0.0F;
    public float  rightScroll = 0.0F;
    public float  maxRightScroll = 0.0F;

    public float alpha  = 1.0F;
    public float x      = 0.0F;
    public float y      = 0.0F;
    public float mouseX = 0.0F;
    public float mouseY = 0.0F;

    public Object focus  = null;
    public String buffer = "";

    public Module    bindingModule  = null;
    public Setting<?>bindingSetting = null;

    private float   animProgress = 0.0F;
    private boolean closing      = false;

    private float   dragX = 0.0F, dragY = 0.0F;
    private boolean dragging = false;
    private float   dX = 0.0F,    dY = 0.0F;

    private final List<HitBox> hitBoxes = new ArrayList<>();
    public static final Object FOCUS_SEARCH = "nova_search";

    public NovaGui() {
        super(Component.literal("Error Client ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        this.animProgress = 0.0F;
        this.closing      = false;

        this.searchQuery  = savedSearchQuery;
        this.buffer       = savedSearchQuery;

        syncHsbFromTheme();

        int sw = this.width  > 0 ? this.width  : (minecraft != null ? minecraft.getWindow().getGuiScaledWidth()  : 854);
        int sh = this.height > 0 ? this.height : (minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : 480);

        if (dragX == 0.0F && dragY == 0.0F) {
            dragX = (sw - W) / 2.0F;
            dragY = (sh - H) / 2.0F;
        }

        if (selectedModule == null && Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
            List<Module> mods = getModulesForSection(activeSection);
            if (!mods.isEmpty()) selectedModule = mods.get(0);
        }
    }

    private void syncHsbFromTheme() {
        int color = editingSecondary ? Theme.getSecondaryColor() : Theme.getAccentColor();
        float[] hsb = Color.RGBtoHSB(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), null);
        this.pickerHue = hsb[0];
        this.pickerSat = hsb[1];
        this.pickerVal = hsb[2];
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int sw = this.width  > 0 ? this.width  : (minecraft != null ? minecraft.getWindow().getGuiScaledWidth()  : 854);
        int sh = this.height > 0 ? this.height : (minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : 480);

        ModernAnim.beginFrame();
        this.animProgress = ModernAnim.approach(this.animProgress, closing ? 0.0F : 1.0F, closing ? 15.0F : 11.0F);

        if (closing && animProgress < 0.01F) {
            ClientSoundPlayer.playGuiClose();
            if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                Client.INSTANCE.configManager.autoSave();
            }
            minecraft.setScreenAndShow(null);
            return;
        }

        float ease   = ModernAnim.ease(animProgress);
        this.alpha   = Math.max(0.0F, Math.min(1.0F, ease));
        this.x       = dragX;
        this.y       = dragY;
        this.mouseX  = (float) mouseX;
        this.mouseY  = (float) mouseY;

        if (dragging) {
            dragX = mouseX - dX;
            dragY = mouseY - dY;
            dragX = Math.max(0.0F, Math.min(sw - W, dragX));
            dragY = Math.max(0.0F, Math.min(sh - H, dragY));
            this.x = dragX;
            this.y = dragY;
        }

        // Ультра-плавное перетаскивание колор-пикера без зацепок и задержек
        if (draggingPickerBox && lastBoxW > 0.0F && lastBoxH > 0.0F) {
            pickerSat = Math.max(0.0F, Math.min(1.0F, (this.mouseX - lastBoxX) / lastBoxW));
            pickerVal = Math.max(0.0F, Math.min(1.0F, 1.0F - (this.mouseY - lastBoxY) / lastBoxH));
            applyColorPickerChange();
        }
        if (draggingHueBar && lastHueH > 0.0F) {
            pickerHue = Math.max(0.0F, Math.min(1.0F, (this.mouseY - lastHueY) / lastHueH));
            applyColorPickerChange();
        }

        hitBoxes.clear();

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            // 1. Мягкое затемнение фона
            rect(0, 0, sw, sh, 0.0F, ColorUtil.rgba(4, 5, 8, (int)(150 * alpha)));

            // 2. Внешняя объемная тень вокруг окна с цветом темы (Theme Glow)
            int glowColor = ColorUtil.withAlpha(Theme.getAccentColor(), (int)(45 * alpha));
            Render2D.drawShadow(x, y, W, H, 12.0F, 14.0F, glowColor);

            // 3. Чистое Liquid Glass стекло с идеальным процедурным шейдером
            NovaShader.drawBackdrop(this, x, y, W, H, 12.0F);

            // 4. Внешняя тонкая рамка
            outline(x, y, W, H, 12.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(20 * alpha)));

            // 5. Вертикальный тонкий разделитель сайдбара
            rect(x + SIDEBAR_W - 0.5F, y + 6.0F, 0.5F, H - 12.0F, 0.0F, ColorUtil.rgba(255, 255, 255, (int)(12 * alpha)));

            // 6. Левая панель Sidebar
            renderSidebar();

            // 7. Шапка (поиск + драг + шестерёнка настроек)
            renderHeader();

            // 8. Контентная область модулей и настроек
            float cX = x + SIDEBAR_W + 5.0F;
            float cY = y + HEADER_H + 2.0F;
            float cW = W - SIDEBAR_W - 10.0F;
            float cH = H - HEADER_H - 7.0F;

            if (activeSection == NavSection.PRESETS) {
                NovaPresetsPage.render(this, cX, cY, cW, cH);
            } else if (activeSection == NavSection.ACCOUNTS) {
                NovaAccountsPage.render(this, cX, cY, cW, cH);
            } else {
                NovaModulesPage.render(this, cX, cY, cW, cH);
            }

            // 9. Выпадающее всплывающее меню настроек и RGB Color Picker
            renderGuiSettingsBar();

            // 10. Контекстное меню модуля на колесико мыши (СКМ)
            if (contextMenuOpen) {
                renderModuleContextMenu();
            }

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderSidebar() {
        float time = (System.currentTimeMillis() % 100_000L) / 1000.0F;

        // ── Логотип Error Client из ассетов ──
        float logoY = y + 9.0F;
        try {
            Render2D.drawTexture("textures/error_logo.png", x + 10.0F, logoY, 20.0F, 20.0F, 0.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha));
        } catch (Throwable ignored) {
            icon(10.0F, Fonts.NURIK_LOGO, x + 20.0F, logoY + 10.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha));
        }

        text(font(8.5F), "ERROR", x + 35.0F, logoY + 0.5F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));
        text(font(5.0F), "DLC 26.2", x + 35.0F, logoY + 10.5F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha));

        try {
            Render2D.drawTexture("textures/system/separator.png", x + 8.0F, y + 33.0F, SIDEBAR_W - 16.0F, 2.0F, 0.0F, ColorUtil.rgba(255, 255, 255, (int)(25 * alpha)));
        } catch (Throwable ignored) {
            rect(x + 8.0F, y + 33.0F, SIDEBAR_W - 16.0F, 0.5F, 0.0F, NovaTheme.DIVIDER());
        }

        // ── Разделы навигации ──
        float navY = y + 36.0F;
        float itemH = 22.0F;
        float itemGap = 2.0F;

        for (NavSection sec : NavSection.values()) {
            if (sec.groupHeader != null) {
                var ghf = font(4.8F);
                text(ghf, sec.groupHeader, x + 10.0F, navY + 1.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), alpha));
                navY += 10.0F;
            }

            boolean active  = (activeSection == sec);
            boolean hovered = hovered(x + 6.0F, navY, SIDEBAR_W - 12.0F, itemH);

            float hAnim = ModernAnim.value("nova_nav:" + sec.name(), active ? 1.0F : (hovered ? 0.45F : 0.0F), 13.0F);

            if (hAnim > 0.01F) {
                int bg = active
                        ? ColorUtil.rgba(ColorUtil.red(Theme.getAccentColor()), ColorUtil.green(Theme.getAccentColor()), ColorUtil.blue(Theme.getAccentColor()), (int)(38 * hAnim * alpha))
                        : ColorUtil.rgba(255, 255, 255, (int)(12 * hAnim * alpha));
                rect(x + 6.0F, navY, SIDEBAR_W - 12.0F, itemH, 6.0F, bg);
            }

            if (active) {
                float pulse = NovaShader.accentPulse(time);
                int barCol = ColorUtil.withAlpha(Theme.getAccentColor(), (int)(190 * pulse * alpha));
                rect(x + 6.0F, navY + 3.0F, 2.5F, itemH - 6.0F, 1.2F, barCol);
            }

            int iconCol = active ? Theme.getAccentColor() : (hovered ? NovaTheme.TEXT() : NovaTheme.TEXT_MUTED());
            try {
                Render2D.drawTexture(sec.iconTexture, x + 14.0F, navY + (itemH - 12.0F) / 2.0F, 12.0F, 12.0F, 0.0F, ColorUtil.multiplyAlpha(iconCol, alpha));
            } catch (Throwable ignored) {
                icon(8.0F, Fonts.NURIK_DOTS, x + 20.0F, navY + itemH / 2.0F, ColorUtil.multiplyAlpha(iconCol, alpha));
            }

            int textCol = active ? NovaTheme.TEXT() : (hovered ? NovaTheme.TEXT_SEC() : NovaTheme.TEXT_MUTED());
            text(font(6.8F), sec.label, x + 31.0F, navY + (itemH - 6.8F) / 2.0F, ColorUtil.multiplyAlpha(textCol, alpha));

            if (sec.category != null) {
                int count = getModulesForSection(sec).size();
                renderBadge(x + SIDEBAR_W - 20.0F, navY + (itemH - 10.0F) / 2.0F, String.valueOf(count));
            }

            final NavSection capSec = sec;
            hit(x + 6.0F, navY, SIDEBAR_W - 12.0F, itemH, btn -> {
                activeSection = capSec;
                savedSearchQuery = "";
                searchQuery = "";
                buffer = "";
                focus = null;

                scroll       = 0.0F;
                rightScroll  = 0.0F;
                if (capSec.category != null) {
                    List<Module> mods = getModulesForSection(capSec);
                    if (!mods.isEmpty() && !mods.contains(selectedModule)) {
                        selectedModule = mods.get(0);
                    }
                }
                return true;
            });

            navY += itemH + itemGap;
        }

        // ── Профиль игрока ──
        float profileY = y + H - 24.0F;
        rect(x + 6.0F, profileY, SIDEBAR_W - 12.0F, 18.0F, 5.0F, ColorUtil.rgba(14, 16, 24, (int)(110 * alpha)));
        outline(x + 6.0F, profileY, SIDEBAR_W - 12.0F, 18.0F, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(14 * alpha)));

        Render2D.drawCustomAvatar(x + 9.0F, profileY + 2.5F, 13.0F, 6.5F, alpha);

        String uname = (minecraft != null && minecraft.getUser() != null) ? minecraft.getUser().getName() : "Player";
        text(font(6.0F), clip(font(6.0F), uname, 68.0F), x + 27.0F, profileY + 2.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));
        text(font(4.8F), "funtime.su", x + 27.0F, profileY + 9.8F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), alpha));
    }

    private void renderGuiSettingsBar() {
        float panelAnim = ModernAnim.value("nova_settings_panel", settingsPanelOpen ? 1.0F : 0.0F, 13.0F);
        if (panelAnim <= 0.01F) return;

        float panelW = 210.0F;
        float panelH = 200.0F * panelAnim;
        float panelX = x + W - panelW - 6.0F;
        float panelY = y + HEADER_H + 2.0F;

        // 1. Блокируем абсолютно ВСЕ клики сквозь панель настроек!
        hit(panelX, panelY, panelW, 200.0F, btn -> true);

        Render2DUtil.pushScissor(panelX, panelY, panelW, panelH);

        // 2. Единый Liquid Glass фон для меню настроек
        NovaShader.drawBackdropWithAlpha(alpha * panelAnim, panelX, panelY, panelW, 200.0F, 10.0F);
        rect(panelX, panelY, panelW, 200.0F, 10.0F, ColorUtil.rgba(10, 12, 18, (int)(190 * alpha * panelAnim)));
        Render2D.drawShadow(panelX, panelY, panelW, 200.0F, 10.0F, 14.0F, ColorUtil.withAlpha(Theme.getAccentColor(), (int)(75 * alpha * panelAnim)));
        outline(panelX, panelY, panelW, 200.0F, 10.0F, 0.6F, ColorUtil.withAlpha(Theme.getAccentColor(), (int)(160 * alpha * panelAnim)));

        float rowY = panelY + 6.0F;

        // Заголовок настроек
        text(font(6.5F), "НАСТРОЙКИ ТЕМЫ ЖИДКОГО СТЕКЛА", panelX + 8.0F, rowY, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha * panelAnim));
        rowY += 14.0F;

        // 2. Переключение целевого цвета: Основной vs Доп цвет
        float tabW = (panelW - 20.0F) / 2.0F;
        float tab1X = panelX + 8.0F;
        float tab2X = tab1X + tabW + 4.0F;

        rect(tab1X, rowY, tabW, 14.0F, 4.0F, !editingSecondary ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(140 * alpha * panelAnim)) : ColorUtil.rgba(255, 255, 255, (int)(14 * alpha * panelAnim)));
        textCenter(font(5.5F), "Основной", tab1X + tabW / 2.0F, rowY + 3.0F, ColorUtil.multiplyAlpha(!editingSecondary ? NovaTheme.TEXT() : NovaTheme.TEXT_MUTED(), alpha * panelAnim));
        hit(tab1X, rowY, tabW, 14.0F, btn -> {
            editingSecondary = false;
            syncHsbFromTheme();
            return true;
        });

        rect(tab2X, rowY, tabW, 14.0F, 4.0F, editingSecondary ? ColorUtil.withAlpha(Theme.getSecondaryColor(), (int)(140 * alpha * panelAnim)) : ColorUtil.rgba(255, 255, 255, (int)(14 * alpha * panelAnim)));
        textCenter(font(5.5F), "Доп цвет", tab2X + tabW / 2.0F, rowY + 3.0F, ColorUtil.multiplyAlpha(editingSecondary ? NovaTheme.TEXT() : NovaTheme.TEXT_MUTED(), alpha * panelAnim));
        hit(tab2X, rowY, tabW, 14.0F, btn -> {
            editingSecondary = true;
            syncHsbFromTheme();
            return true;
        });
        rowY += 18.0F;

        // 3. ПОЛНЫЙ 2D RGB COLOR PICKER CANVAS (Sat/Val Box + Hue Bar)
        float boxW = 145.0F;
        float boxH = 95.0F;
        float boxX = panelX + 8.0F;
        float boxY = rowY;

        lastBoxX = boxX; lastBoxY = boxY; lastBoxW = boxW; lastBoxH = boxH;

        int pureHueColor = Color.HSBtoRGB(pickerHue, 1.0F, 1.0F);

        // 2D Градиентное поле выбора Сатурации и Яркости
        Render2D.drawGradientRound(boxX, boxY, boxW, boxH, 4.0F,
                ColorUtil.rgba(255, 255, 255, (int)(255 * alpha * panelAnim)),
                ColorUtil.multiplyAlpha(pureHueColor, alpha * panelAnim),
                ColorUtil.rgba(0, 0, 0, (int)(255 * alpha * panelAnim)),
                ColorUtil.rgba(0, 0, 0, (int)(255 * alpha * panelAnim)));
        outline(boxX, boxY, boxW, boxH, 4.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(50 * alpha * panelAnim)));

        // Курсор кружка внутри 2D Color Picker
        float cursorX = boxX + pickerSat * boxW;
        float cursorY = boxY + (1.0F - pickerVal) * boxH;
        rect(cursorX - 3.0F, cursorY - 3.0F, 6.0F, 6.0F, 3.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha * panelAnim)));
        outline(cursorX - 3.0F, cursorY - 3.0F, 6.0F, 6.0F, 3.0F, 0.6F, ColorUtil.rgba(0, 0, 0, (int)(220 * alpha * panelAnim)));

        hit(boxX, boxY, boxW, boxH, btn -> {
            draggingPickerBox = true;
            pickerSat = Math.max(0.0F, Math.min(1.0F, (mouseX - boxX) / boxW));
            pickerVal = Math.max(0.0F, Math.min(1.0F, 1.0F - (mouseY - boxY) / boxH));
            applyColorPickerChange();
            return true;
        });

        // 4. Вертикальный Радужный Слайдер Тона (Hue Bar)
        float hueBarX = boxX + boxW + 8.0F;
        float hueBarW = 12.0F;
        float hueBarH = boxH;

        lastHueX = hueBarX; lastHueY = boxY; lastHueW = hueBarW; lastHueH = hueBarH;

        for (int i = 0; i < (int) hueBarH; i++) {
            float hFrac = (float) i / hueBarH;
            int stepCol = Color.HSBtoRGB(hFrac, 1.0F, 1.0F);
            rect(hueBarX, boxY + i, hueBarW, 1.0F, 0.0F, ColorUtil.multiplyAlpha(stepCol, alpha * panelAnim));
        }
        outline(hueBarX, boxY, hueBarW, hueBarH, 3.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(50 * alpha * panelAnim)));

        float hueCursorY = boxY + pickerHue * hueBarH;
        rect(hueBarX - 1.0F, hueCursorY - 1.5F, hueBarW + 2.0F, 3.0F, 1.0F, ColorUtil.rgba(255, 255, 255, (int)(255 * alpha * panelAnim)));

        hit(hueBarX, boxY, hueBarW, hueBarH, btn -> {
            draggingHueBar = true;
            pickerHue = Math.max(0.0F, Math.min(1.0F, (mouseY - boxY) / hueBarH));
            applyColorPickerChange();
            return true;
        });

        rowY += boxH + 8.0F;

        // 5. Текстовое отображение HEX и RGB значений
        int curTargetCol = editingSecondary ? Theme.getSecondaryColor() : Theme.getAccentColor();
        int cr = ColorUtil.red(curTargetCol);
        int cg = ColorUtil.green(curTargetCol);
        int cb = ColorUtil.blue(curTargetCol);
        String hexStr = String.format("#%02X%02X%02X", cr, cg, cb);

        text(font(5.8F), "RGB: " + cr + ", " + cg + ", " + cb, panelX + 8.0F, rowY, ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), alpha * panelAnim));
        textRight(font(5.8F), hexStr, panelX + panelW - 8.0F, rowY, ColorUtil.multiplyAlpha(Theme.getAccentColor(), alpha * panelAnim));
        rowY += 14.0F;

        // Кнопка переключения Радужный Chroma RGB
        boolean chromaActive = "RGB".equalsIgnoreCase(Theme.getAccentMode()) || "Chroma".equalsIgnoreCase(Theme.getAccentMode());
        rect(panelX + 8.0F, rowY, panelW - 16.0F, 13.0F, 4.0F, chromaActive ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(140 * alpha * panelAnim)) : ColorUtil.rgba(255, 255, 255, (int)(15 * alpha * panelAnim)));
        textCenter(font(5.5F), "Chroma RGB Радуга", panelX + panelW / 2.0F, rowY + 2.5F, ColorUtil.multiplyAlpha(chromaActive ? NovaTheme.TEXT() : NovaTheme.TEXT_MUTED(), alpha * panelAnim));
        hit(panelX + 8.0F, rowY, panelW - 16.0F, 13.0F, btn -> {
            Theme.setAccentMode(chromaActive ? "Static" : "RGB");
            if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                Client.INSTANCE.configManager.autoSave();
            }
            return true;
        });

        Render2DUtil.popScissor();
    }

    private void applyColorPickerChange() {
        int rgb = Color.HSBtoRGB(pickerHue, pickerSat, pickerVal);
        int color = ColorUtil.rgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
        Theme.setAccentMode("Static");
        if (editingSecondary) {
            Theme.setSecondaryColor(color);
        } else {
            Theme.setAccentColor(color);
        }
        if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
            Client.INSTANCE.configManager.autoSave();
        }
    }

    private void renderHeader() {
        hit(x + SIDEBAR_W, y, W - SIDEBAR_W - 35.0F, HEADER_H, btn -> {
            if (btn == 0) {
                dragging = true;
                dX = mouseX - x;
                dY = mouseY - y;
                return true;
            }
            return false;
        });

        float headX = x + SIDEBAR_W + 10.0F;
        float headY = y + 6.0F;

        text(font(8.5F), activeSection.label, headX, headY, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));

        // ── Поиск ──
        float searchW = 125.0F;
        float searchH = 15.0F;
        float searchX = x + W - searchW - 28.0F;
        float searchY = y + (HEADER_H - searchH) / 2.0F;
        boolean searchFocused = (focus == FOCUS_SEARCH);

        rect(searchX, searchY, searchW, searchH, 4.5F, ColorUtil.rgba(14, 16, 24, (int)(150 * alpha)));
        outline(searchX, searchY, searchW, searchH, 4.5F, 0.4F, searchFocused ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(180 * alpha)) : NovaTheme.BORDER());

        icon(7.0F, Fonts.NURIK_SEARCH, searchX + 7.0F, searchY + 7.5F, ColorUtil.multiplyAlpha(searchFocused ? Theme.getAccentColor() : NovaTheme.TEXT_MUTED(), alpha));

        String sText = searchFocused ? buffer : (searchQuery.isEmpty() ? "Поиск..." : searchQuery);
        int    sCol  = (searchFocused || !searchQuery.isEmpty()) ? NovaTheme.TEXT() : NovaTheme.TEXT_MUTED();
        text(font(6.2F), clip(font(6.2F), sText, searchW - 22.0F), searchX + 16.0F, searchY + (searchH - 6.2F) / 2.0F, ColorUtil.multiplyAlpha(sCol, alpha));

        hit(searchX, searchY, searchW, searchH, btn -> {
            focus  = FOCUS_SEARCH;
            buffer = searchQuery;
            return true;
        });

        // ── КНОПКА-ШЕСТЕРЁНКА НАСТРОЕК ВМЕСТО КРЕСТИКА ──
        float gearX = x + W - 18.0F;
        float gearY = y + 7.0F;
        boolean gearHov = hovered(gearX - 4.0F, gearY - 4.0F, 16.0F, 16.0F);
        int gearCol = settingsPanelOpen ? Theme.getAccentColor() : (gearHov ? NovaTheme.TEXT() : NovaTheme.TEXT_MUTED());
        icon(8.5F, Fonts.NURIK_GEAR, gearX + 3.0F, gearY + 3.0F, ColorUtil.multiplyAlpha(gearCol, alpha));

        hit(gearX - 4.0F, gearY - 4.0F, 16.0F, 16.0F, btn -> {
            settingsPanelOpen = !settingsPanelOpen;
            return true;
        });
    }

    private void renderBadge(float bx, float by, String txt) {
        var f   = font(4.8F);
        float bw = Math.max(10.0F, f.font().getWidth(txt, f.size()) + 5.0F);
        rect(bx, by, bw, 10.0F, 4.0F, ColorUtil.rgba(255, 255, 255, (int)(14 * alpha)));
        textCenter(f, txt, bx + bw / 2.0F, by + (10.0F - 4.8F) / 2.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), alpha));
    }

    public List<Module> getModulesForSection(NavSection sec) {
        List<Module> list = new ArrayList<>();
        if (Client.INSTANCE == null || Client.INSTANCE.moduleManager == null) return list;
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            if (m == null) continue;
            if (!searchQuery.isBlank()) {
                if (m.getName().toLowerCase().contains(searchQuery.toLowerCase())
                        || (m.getDescription() != null && m.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))) {
                    list.add(m);
                }
            } else if (sec.category != null && m.getCategory() == sec.category) {
                list.add(m);
            }
        }
        return list;
    }

    public FontHolder font(float size) { return new FontHolder(Fonts.SF_MEDIUM, size); }

    public void rect(float rx, float ry, float rw, float rh, float radius, int color) {
        Render2D.drawRoundedRect(rx, ry, rw, rh, radius, ColorUtil.multiplyAlpha(color, alpha));
    }
    public void outline(float rx, float ry, float rw, float rh, float radius, float thickness, int color) {
        Render2D.drawRoundedOutline(rx, ry, rw, rh, radius, thickness, ColorUtil.multiplyAlpha(color, alpha));
    }
    public void text(FontHolder f, String s, float tx, float ty, int color) {
        Fonts.drawString(f.font(), s, tx, ty, f.size(), color);
    }
    public void textRight(FontHolder f, String s, float rx, float ty, int color) {
        float w = f.font().getWidth(s, f.size());
        Fonts.drawString(f.font(), s, rx - w, ty, f.size(), color);
    }
    public void textCenter(FontHolder f, String s, float cx, float ty, int color) {
        float w = f.font().getWidth(s, f.size());
        Fonts.drawString(f.font(), s, cx - w / 2.0F, ty, f.size(), color);
    }
    public float textY(float top, float h, FontHolder f) { return top + (h - f.size()) / 2.0F; }
    public String clip(FontHolder f, String s, float maxW) {
        if (s == null) return "";
        if (f.font().getWidth(s, f.size()) <= maxW) return s;
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (f.font().getWidth(sb + "" + c + "..", f.size()) > maxW) break;
            sb.append(c);
        }
        return sb + "..";
    }
    public void icon(float size, String glyph, float cx, float cy, int color) {
        float w = Fonts.NURIK_MENU.getWidth(glyph, size);
        Fonts.drawString(Fonts.NURIK_MENU, glyph, cx - w / 2.0F, cy - size / 2.0F, size, color);
    }
    public void toggle(float tx, float ty, boolean on, Object key) {
        float t = ModernAnim.value("tgl:" + key, on ? 1.0F : 0.0F, 16.0F);
        rect(tx, ty, 16.0F, 8.0F, 4.0F, ColorUtil.lerp(NovaTheme.TOGGLE_TRACK_OFF(), Theme.getAccentColor(), t));
        float kx = tx + 1.0F + 8.0F * t;
        rect(kx, ty + 1.0F, 6.0F, 6.0F, 3.0F, ColorUtil.lerp(NovaTheme.TOGGLE_KNOB_OFF(), ColorUtil.rgba(255, 255, 255, 255), t));
    }
    public void slider(float sx, float sy, float sw, float fraction, Object key, SliderConsumer action) {
        rect(sx, sy, sw, 3.0F, 1.5F, NovaTheme.TRACK());
        float fw = sw * Math.max(0.0F, Math.min(1.0F, fraction));
        if (fw > 0.5F) rect(sx, sy, fw, 3.2F, 1.5F, Theme.getAccentColor());
        hit(sx, sy - 4.0F, sw, 10.0F, btn -> {
            float frac = Math.max(0.0F, Math.min(1.0F, (mouseX - sx) / sw));
            action.accept(frac);
            return true;
        });
    }
    public void pushClip(float cx, float cy, float cw, float ch) { Render2DUtil.pushScissor(cx, cy, cw, ch); }
    public void popClip()                                          { Render2DUtil.popScissor(); }
    public boolean hovered(float hx, float hy, float hw, float hh) {
        return mouseX >= hx && mouseX <= hx + hw && mouseY >= hy && mouseY <= hy + hh;
    }
    public void hit(float hx, float hy, float hw, float hh, ClickAction action) {
        hitBoxes.add(new HitBox(hx, hy, hw, hh, action));
    }

    public record HitBox(float x, float y, float w, float h, ClickAction action) {}
    public interface ClickAction { boolean onClick(int button); }
    public interface SliderConsumer { void accept(double fraction); }

    public boolean isBinding(Object target) {
        if (target instanceof Module m)    return bindingModule == m;
        if (target instanceof Setting<?> s) return bindingSetting == s;
        return false;
    }
    public void startBind(Object target) {
        if (target instanceof Module m)    { bindingModule = m; bindingSetting = null; }
        else if (target instanceof Setting<?> s) { bindingSetting = s; bindingModule = null; }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mx = event.x();
        double my = event.y();
        int btn   = event.button();

        if (bindingModule != null) {
            bindingModule.getBind().setSingle(BindSetting.mouse(btn));
            bindingModule = null;
            return true;
        }
        if (bindingSetting instanceof BindSetting bind) {
            bind.setSingle(BindSetting.mouse(btn));
            bindingSetting = null;
            return true;
        }

        for (int i = hitBoxes.size() - 1; i >= 0; i--) {
            HitBox box = hitBoxes.get(i);
            if (mx >= box.x && mx <= box.x + box.w && my >= box.y && my <= box.y + box.h) {
                if (box.action.onClick(btn)) {
                    ClientSoundPlayer.playGuiClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        draggingPickerBox = false;
        draggingHueBar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (bindingModule != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE)
                bindingModule.getBind().setSingle(BindSetting.UNBOUND);
            else
                bindingModule.getBind().setSingle(key);
            bindingModule = null;
            return true;
        }
        if (bindingSetting instanceof BindSetting bind) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE)
                bind.setSingle(BindSetting.UNBOUND);
            else
                bind.setSingle(key);
            bindingSetting = null;
            return true;
        }

        if (NovaPresetsPage.createModalOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                NovaPresetsPage.createModalOpen = false;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && !NovaPresetsPage.newPresetNameBuffer.isEmpty()) {
                NovaPresetsPage.newPresetNameBuffer = NovaPresetsPage.newPresetNameBuffer.substring(0, NovaPresetsPage.newPresetNameBuffer.length() - 1);
            }
            return true;
        }

        if (NovaAccountsPage.addModalOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                NovaAccountsPage.addModalOpen = false;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && !NovaAccountsPage.nicknameBuffer.isEmpty()) {
                NovaAccountsPage.nicknameBuffer = NovaAccountsPage.nicknameBuffer.substring(0, NovaAccountsPage.nicknameBuffer.length() - 1);
            }
            return true;
        }

        if (focus == FOCUS_SEARCH) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                focus = null;
                searchQuery = buffer;
                savedSearchQuery = searchQuery;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && !buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
                searchQuery = buffer;
                savedSearchQuery = searchQuery;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            closing = true;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (NovaPresetsPage.createModalOpen) {
            NovaPresetsPage.newPresetNameBuffer += (char) event.codepoint();
            return true;
        }
        if (NovaAccountsPage.addModalOpen) {
            NovaAccountsPage.nicknameBuffer += (char) event.codepoint();
            return true;
        }
        if (focus == FOCUS_SEARCH) {
            buffer += (char) event.codepoint();
            searchQuery = buffer;
            savedSearchQuery = searchQuery;
            return true;
        }
        return super.charTyped(event);
    }

    private void renderModuleContextMenu() {
        if (!contextMenuOpen || contextModule == null) return;

        float popW = 210.0F;
        float popH = 125.0F;
        float px = Math.max(x + SIDEBAR_W + 5.0F, Math.min(x + W - popW - 10.0F, contextX));
        float py = Math.max(y + HEADER_H + 5.0F, Math.min(y + H - popH - 10.0F, contextY));

        // 1. Тень и жидкое стекло Liquid Glass
        Render2D.drawShadow(px, py, popW, popH, 10.0F, 12.0F, ColorUtil.rgba(0, 0, 0, (int)(190 * alpha)));
        rect(px, py, popW, popH, 10.0F, ColorUtil.rgba(14, 16, 26, (int)(245 * alpha)));
        outline(px, py, popW, popH, 10.0F, 0.5F, ColorUtil.withAlpha(Theme.getAccentColor(), (int)(160 * alpha)));

        // 2. Шапка: Иконка сетки (::), название модуля, крестик закрытия (✕)
        icon(7.5F, Fonts.NURIK_DOTS, px + 14.0F, py + 12.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), alpha));

        var titleFont = font(7.2F);
        text(titleFont, contextModule.getName(), px + 24.0F, py + 7.5F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));

        // Крестик закрытия справа
        float closeX = px + popW - 18.0F;
        float closeY = py + 7.5F;
        text(font(7.0F), "✕", closeX, closeY, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), alpha));
        hit(closeX - 3.0F, closeY - 3.0F, 14.0F, 14.0F, btn -> {
            contextMenuOpen = false;
            return true;
        });

        // Разделительная полоска под шапкой
        rect(px + 8.0F, py + 24.0F, popW - 16.0F, 0.5F, 0.0F, ColorUtil.rgba(255, 255, 255, (int)(15 * alpha)));

        float rowY = py + 30.0F;
        float rowH = 24.0F;
        var labelFont = font(6.5F);

        // ── ROW 1: "Бинд" ──
        text(labelFont, "Бинд", px + 14.0F, textY(rowY, rowH, labelFont), ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));

        String keyName = isBinding(contextModule) ? "..." : (contextModule.getBind().isEmpty() ? "n/a" : error.util.client.persiki.KeyUtil.getKeyName(contextModule.getBind().get(0)));
        var keyF = font(5.8F);
        float keyTextW = keyF.font().getWidth(keyName, keyF.size());
        float keyPillW = Math.max(34.0F, keyTextW + 14.0F);
        float keyPillX = px + popW - keyPillW - 14.0F;
        float keyPillY = rowY + (rowH - 14.0F) / 2.0F;

        boolean bindHov = hovered(keyPillX, keyPillY, keyPillW, 14.0F);
        int bindBg = isBinding(contextModule) ? Theme.getAccentColor() : (bindHov ? ColorUtil.rgba(255, 255, 255, (int)(30 * alpha)) : ColorUtil.rgba(255, 255, 255, (int)(15 * alpha)));
        rect(keyPillX, keyPillY, keyPillW, 14.0F, 7.0F, bindBg);
        outline(keyPillX, keyPillY, keyPillW, 14.0F, 7.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(25 * alpha)));
        textCenter(keyF, keyName, keyPillX + keyPillW / 2.0F, textY(keyPillY, 14.0F, keyF), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), alpha));

        hit(keyPillX, keyPillY, keyPillW, 14.0F, btn -> {
            startBind(contextModule);
            return true;
        });

        rowY += rowH + 4.0F;

        // ── ROW 2: "Видимость" ──
        text(labelFont, "Видимость", px + 14.0F, textY(rowY, rowH, labelFont), ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));

        float togX = px + popW - 24.0F - 14.0F;
        float togY = rowY + (rowH - 12.0F) / 2.0F;
        boolean visibleInHud = !contextModule.isHiddenFromHud();

        toggle(togX, togY, visibleInHud, "ctx_vis:" + contextModule.getName());
        hit(togX - 2.0F, togY - 2.0F, 28.0F, 16.0F, btn -> {
            contextModule.setHiddenFromHud(!contextModule.isHiddenFromHud());
            return true;
        });

        rowY += rowH + 4.0F;

        // ── ROW 3: "Тип" [ Hold | Toggle ] ──
        text(labelFont, "Тип", px + 14.0F, textY(rowY, rowH, labelFont), ColorUtil.multiplyAlpha(NovaTheme.TEXT(), alpha));

        float segContainerW = 88.0F;
        float segContainerH = 18.0F;
        float segContainerX = px + popW - segContainerW - 14.0F;
        float segContainerY = rowY + (rowH - segContainerH) / 2.0F;

        rect(segContainerX, segContainerY, segContainerW, segContainerH, 6.0F, ColorUtil.rgba(22, 26, 38, (int)(180 * alpha)));
        outline(segContainerX, segContainerY, segContainerW, segContainerH, 6.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(20 * alpha)));

        float segW = (segContainerW - 4.0F) / 2.0F;
        float segH = segContainerH - 4.0F;
        boolean isHold = "Hold".equalsIgnoreCase(contextModule.getBindType());

        // Левый сегмент: Hold
        float holdX = segContainerX + 2.0F;
        float holdY = segContainerY + 2.0F;
        if (isHold) {
            rect(holdX, holdY, segW, segH, 4.5F, Theme.getAccentColor());
        }
        var segF = font(5.8F);
        textCenter(segF, "Hold", holdX + segW / 2.0F, textY(holdY, segH, segF), ColorUtil.rgba(255, 255, 255, (int)((isHold ? 255 : 140) * alpha)));
        hit(holdX, holdY, segW, segH, btn -> {
            contextModule.setBindType("Hold");
            return true;
        });

        // Правый сегмент: Toggle
        float toggleX = segContainerX + 2.0F + segW;
        float toggleY = segContainerY + 2.0F;
        if (!isHold) {
            rect(toggleX, toggleY, segW, segH, 4.5F, Theme.getAccentColor());
        }
        textCenter(segF, "Toggle", toggleX + segW / 2.0F, textY(toggleY, segH, segF), ColorUtil.rgba(255, 255, 255, (int)((!isHold ? 255 : 140) * alpha)));
        hit(toggleX, toggleY, segW, segH, btn -> {
            contextModule.setBindType("Toggle");
            return true;
        });
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        boolean inContent = mx >= this.x + SIDEBAR_W && mx <= this.x + W;
        if (inContent) {
            float midX = this.x + SIDEBAR_W + (W - SIDEBAR_W) * 0.4F;
            if (mx < midX) {
                scroll      = (float) Math.max(0, Math.min(maxScroll,       scroll      - scrollY * 16.0));
            } else {
                rightScroll = (float) Math.max(0, Math.min(maxRightScroll, rightScroll - scrollY * 16.0));
            }
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }
}
