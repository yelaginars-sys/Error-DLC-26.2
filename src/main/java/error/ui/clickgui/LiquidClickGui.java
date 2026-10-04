package error.ui.clickgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LiquidClickGui extends Screen {

    public static final float WINDOW_W = 660.0F;
    public static final float WINDOW_H = 430.0F;
    public static final float SIDEBAR_W = 165.0F;

    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "textures/system/logo.png");
    private static final Identifier SEARCH_TEX = Identifier.fromNamespaceAndPath("error", "textures/system/search.png");

    private static final Map<Category, Identifier> CATEGORY_ICONS = Map.of(
            Category.COMBAT, Identifier.fromNamespaceAndPath("error", "textures/system/combat.png"),
            Category.MOVEMENT, Identifier.fromNamespaceAndPath("error", "textures/system/movement.png"),
            Category.RENDER, Identifier.fromNamespaceAndPath("error", "textures/system/visuals.png"),
            Category.PLAYER, Identifier.fromNamespaceAndPath("error", "textures/system/player.png"),
            Category.MISC, Identifier.fromNamespaceAndPath("error", "textures/system/misc.png"),
            Category.CONFIGS, Identifier.fromNamespaceAndPath("error", "textures/system/configs.png")
    );

    public Category activeCategory = Category.COMBAT;
    public String searchQuery = "";
    public boolean searchFocused = false;

    private final Animation openAnim = new Animation(1.0F, 0.18F);
    private final Animation scrollAnim = new Animation(0.0F, 0.20F);
    private float scrollTarget = 0.0F;
    private long openTime = System.currentTimeMillis();

    private final Map<Module, Animation> moduleExpandAnims = new HashMap<>();
    private final Map<Module, Animation> moduleToggleAnims = new HashMap<>();
    private final Map<Category, Animation> categoryHoverAnims = new HashMap<>();

    public Module expandedModule = null;
    public Setting<?> activeBindingSetting = null;
    private SliderSetting draggingSlider = null;

    public LiquidClickGui() {
        super(Component.literal("ClickGUI"));
        this.openAnim.setValue(1.0F);
        this.openAnim.setTarget(1.0F);
        this.openTime = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        super.init();
        this.openAnim.setValue(1.0F);
        this.openAnim.setTarget(1.0F);
        this.openTime = System.currentTimeMillis();
        this.draggingSlider = null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        openAnim.update();
        scrollAnim.update();

        int screenW = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenH = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

        float animVal = Math.max(0.1F, openAnim.getValue());

        float x = (screenW - WINDOW_W) / 2.0F;
        float y = (screenH - WINDOW_H) / 2.0F;

        // Update slider drag if active
        if (this.draggingSlider != null && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            updateSliderDrag(mouseX);
        } else {
            this.draggingSlider = null;
        }

        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // 1. Dark Backdrop Overlay
            int veilAlpha = (int) (180 * animVal);
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(4, 6, 12, veilAlpha));

            // 2. Real Backdrop Blur
            int blurTint = ColorUtil.rgba(12, 14, 24, (int) (130 * animVal));
            Render2D.drawBlur(x, y, WINDOW_W, WINDOW_H, 16.0F, 32.0F, blurTint, animVal);

            // 3. Main Window Glass Drop Shadow & Liquid Gradient Body
            int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (220 * animVal));
            Render2D.drawShadow(x, y, WINDOW_W, WINDOW_H, 16.0F, 24.0F, shadowCol);

            int cTL = ColorUtil.rgba(16, 19, 30, (int) (235 * animVal));
            int cTR = ColorUtil.rgba(12, 15, 24, (int) (230 * animVal));
            int cBL = ColorUtil.rgba(8, 10, 18, (int) (240 * animVal));
            int cBR = ColorUtil.rgba(6, 8, 14, (int) (245 * animVal));
            Render2D.drawGradientRound(x, y, WINDOW_W, WINDOW_H, 16.0F, cTL, cTR, cBL, cBR);

            // 4. Specular Top Glass Reflection
            Render2D.pushScissor(x, y, WINDOW_W, WINDOW_H);
            int shineTop = ColorUtil.rgba(255, 255, 255, (int) (16 * animVal));
            int shineBottom = ColorUtil.rgba(255, 255, 255, 0);
            Render2D.drawGradientRound(x, y, WINDOW_W, 140.0F, 16.0F, shineTop, shineTop, shineBottom, shineBottom);
            Render2D.popScissor();

            // 5. Glassy Outlines (Outer soft glass outline + inner accent glow)
            int glassOutline = ColorUtil.rgba(255, 255, 255, (int) (35 * animVal));
            Render2D.drawRoundedOutline(x, y, WINDOW_W, WINDOW_H, 16.0F, 0.8F, glassOutline);

            int accentColor = Theme.getAccentColor();
            int innerAccent = ColorUtil.withAlpha(accentColor, (int) (40 * animVal));
            Render2D.drawRoundedOutline(x + 0.5F, y + 0.5F, WINDOW_W - 1.0F, WINDOW_H - 1.0F, 15.5F, 0.8F, innerAccent);

            // 6. Sidebar Separator Line
            int dividerColor = ColorUtil.rgba(255, 255, 255, (int) (18 * animVal));
            Render2D.drawRoundedRect(x + SIDEBAR_W, y + 14.0F, 1.0F, WINDOW_H - 28.0F, 0.5F, dividerColor);

            // 7. Render Subsections
            renderSidebar(x, y, mouseX, mouseY, animVal);
            renderHeader(x + SIDEBAR_W + 16.0F, y + 16.0F, WINDOW_W - SIDEBAR_W - 32.0F, mouseX, mouseY, animVal);
            renderModulesGrid(x + SIDEBAR_W + 16.0F, y + 54.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 68.0F, mouseX, mouseY, animVal);
        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderSidebar(float x, float y, int mouseX, int mouseY, float alphaVal) {
        int whiteCol = ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal));
        int subCol = ColorUtil.rgba(160, 175, 205, (int) (180 * alphaVal));

        // Logo Image / Fallback Icon
        try {
            Render2D.drawTexture(LOGO_TEX, x + 16.0F, y + 14.0F, 22.0F, 22.0F, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
        } catch (Throwable ignored) {
            Fonts.drawString(Fonts.ICONS, IconUse.LOGO.getGlyph(), x + 18.0F, y + 16.0F, 12.0F, ColorUtil.rgba(37, 117, 252, (int) (255 * alphaVal)));
        }

        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", x + 44.0F, y + 15.0F, 10.5F, whiteCol);
        Fonts.drawString(Fonts.SF_MEDIUM, "Liquid Glass UI", x + 44.0F, y + 28.0F, 5.5F, subCol);

        // Category Pills Stack
        Category[] categories = Category.values();
        float catY = y + 52.0F;
        float catH = 28.0F;
        float catW = SIDEBAR_W - 24.0F;
        float catX = x + 12.0F;

        int accentColor = Theme.getAccentColor();

        for (Category cat : categories) {
            boolean active = (cat == this.activeCategory && searchQuery.isEmpty());
            boolean isHovered = mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH;

            Animation hoverAnim = categoryHoverAnims.computeIfAbsent(cat, k -> new Animation(0.0F, 0.16F));
            hoverAnim.setTarget(active ? 1.0F : (isHovered ? 0.45F : 0.0F));
            hoverAnim.update();
            float hVal = hoverAnim.getValue();

            if (hVal > 0.01F) {
                int pillBg = active
                        ? ColorUtil.withAlpha(accentColor, (int) ((0.20F + hVal * 0.15F) * 255 * alphaVal))
                        : ColorUtil.rgba(255, 255, 255, (int) ((0.05F + hVal * 0.08F) * 255 * alphaVal));

                int pillOutline = active
                        ? ColorUtil.withAlpha(accentColor, (int) (0.60F * 255 * alphaVal))
                        : ColorUtil.rgba(255, 255, 255, (int) (hVal * 0.25F * 255 * alphaVal));

                Render2D.drawRoundedRect(catX, catY, catW, catH, 7.0F, pillBg);
                Render2D.drawRoundedOutline(catX, catY, catW, catH, 7.0F, 0.8F, pillOutline);
            }

            // Category Icon (Texture or IconUse glyph fallback)
            int iconCol = active
                    ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                    : ColorUtil.rgba(160, 175, 205, (int) ((0.70F + hVal * 0.30F) * 255 * alphaVal));

            Identifier iconTex = CATEGORY_ICONS.get(cat);
            if (iconTex != null) {
                Render2D.drawTexture(iconTex, catX + 10.0F, catY + 6.0F, 16.0F, 16.0F, 0.0F, iconCol);
            } else {
                Fonts.drawString(Fonts.ICONS, getCategoryIcon(cat), catX + 10.0F, catY + 8.0F, 9.0F, iconCol);
            }

            // Category Title Label
            int nameCol = active
                    ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                    : ColorUtil.rgba(175, 185, 205, (int) ((0.75F + hVal * 0.25F) * 255 * alphaVal));
            Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 32.0F, catY + 9.5F, 7.5F, nameCol);

            // Enabled Modules Count Badge
            long enabledCount = Client.getInstance().moduleManager.getModules().stream()
                    .filter(m -> m.getCategory() == cat && m.isEnabled())
                    .count();

            if (enabledCount > 0) {
                String countStr = String.valueOf(enabledCount);
                float countW = Fonts.SF_MEDIUM.getWidth(countStr, 5.5F);
                float badgeW = Math.max(14.0F, countW + 8.0F);
                float badgeX = catX + catW - 10.0F - badgeW;
                float badgeY = catY + 7.0F;

                int badgeBg = active
                        ? ColorUtil.withAlpha(accentColor, (int) (200 * alphaVal))
                        : ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal));

                Render2D.drawRoundedRect(badgeX, badgeY, badgeW, 14.0F, 4.0F, badgeBg);
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, countStr, badgeX + badgeW / 2.0F, badgeY + 3.5F, 5.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alphaVal)));
            }

            catY += catH + 4.0F;
        }

        // Bottom User Profile Card
        float profileY = y + WINDOW_H - 46.0F;
        float profileW = SIDEBAR_W - 24.0F;
        float profileH = 34.0F;
        float profileX = x + 12.0F;

        int cardBg = ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal));
        int cardBorder = ColorUtil.rgba(255, 255, 255, (int) (25 * alphaVal));

        Render2D.drawRoundedRect(profileX, profileY, profileW, profileH, 8.0F, cardBg);
        Render2D.drawRoundedOutline(profileX, profileY, profileW, profileH, 8.0F, 0.75F, cardBorder);

        Render2D.drawCustomAvatar(profileX + 6.0F, profileY + 5.0F, 24.0F, 6.0F, alphaVal);

        String username = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "User";
        Fonts.drawString(Fonts.SF_MEDIUM, username, profileX + 36.0F, profileY + 7.0F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Developer", profileX + 36.0F, profileY + 19.0F, 5.0F, ColorUtil.rgba(140, 160, 190, (int) (180 * alphaVal)));
    }

    private String getCategoryIcon(Category cat) {
        if (cat == Category.COMBAT) return IconUse.FIGHT.getGlyph();
        if (cat == Category.MOVEMENT) return IconUse.MOVEMENT.getGlyph();
        if (cat == Category.RENDER) return IconUse.RENDER.getGlyph();
        if (cat == Category.PLAYER) return IconUse.PLAYER.getGlyph();
        if (cat == Category.MISC) return IconUse.MISC.getGlyph();
        return IconUse.GEAR.getGlyph();
    }

    private void renderHeader(float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";

        Fonts.drawString(Fonts.SF_MEDIUM, titleText, x, y + 3.0F, 11.5F, ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal)));

        // Search Bar Container
        float searchW = 160.0F;
        float searchH = 24.0F;
        float searchX = x + w - searchW;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y && mouseY <= y + searchH;
        int boxBg = ColorUtil.rgba(255, 255, 255, (int) ((searchFocused ? 0.14F : (isHovered ? 0.09F : 0.06F)) * 255 * alphaVal));
        int boxBorder = searchFocused
                ? ColorUtil.withAlpha(Theme.getAccentColor(), (int) (200 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (35 * alphaVal));

        Render2D.drawRoundedRect(searchX, y, searchW, searchH, 6.0F, boxBg);
        Render2D.drawRoundedOutline(searchX, y, searchW, searchH, 6.0F, 0.8F, boxBorder);

        // Search Icon (Texture or IconUse fallback)
        try {
            Render2D.drawTexture(SEARCH_TEX, searchX + 8.0F, y + 5.0F, 14.0F, 14.0F, 0.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * alphaVal)));
        } catch (Throwable ignored) {
            Fonts.drawString(Fonts.ICONS, IconUse.SEARCH.getGlyph(), searchX + 8.0F, y + 7.0F, 8.0F, ColorUtil.rgba(160, 175, 200, (int) (200 * alphaVal)));
        }

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        int textCol = searchQuery.isEmpty() && !searchFocused
                ? ColorUtil.rgba(140, 150, 170, (int) (180 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), searchX + 26.0F, y + 7.5F, 6.5F, textCol);
    }

    private void renderModulesGrid(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        List<Module> modules = getFilteredModules();

        float cardW = (w - 14.0F) / 2.0F;
        float startY = y - scrollAnim.getValue();

        float leftY = startY;
        float rightY = startY;

        Render2DUtil.pushScissor(x - 2.0F, y, w + 4.0F, h);
        try {
            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);

                float cardX = isRightColumn ? x + cardW + 14.0F : x;
                float currentY = isRightColumn ? rightY : leftY;

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                boolean isExpanded = (expandedModule == module);
                expandAnim.setTarget(isExpanded ? 1.0F : 0.0F);
                expandAnim.update();
                float eVal = expandAnim.getValue();

                float cardH = 44.0F + (eVal * calculateSettingsHeight(module));

                if (currentY + cardH >= y && currentY <= y + h) {
                    renderModuleCard(module, cardX, currentY, cardW, cardH, eVal, mouseX, mouseY, alphaVal);
                }

                if (isRightColumn) {
                    rightY += cardH + 10.0F;
                } else {
                    leftY += cardH + 10.0F;
                }
            }
        } finally {
            Render2DUtil.popScissor();
        }

        float contentH = Math.max(leftY, rightY) - startY;
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0F, Math.max(0.0F, contentH - h));
        this.scrollAnim.setTarget(this.scrollTarget);
    }

    private void renderModuleCard(Module module, float x, float y, float w, float h, float expandVal, int mouseX, int mouseY, float alphaVal) {
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 44.0F;

        Animation toggleAnim = moduleToggleAnims.computeIfAbsent(module, k -> new Animation(module.isEnabled() ? 1.0F : 0.0F, 0.18F));
        toggleAnim.setTarget(module.isEnabled() ? 1.0F : 0.0F);
        toggleAnim.update();
        float tVal = toggleAnim.getValue();

        int accentColor = Theme.getAccentColor();

        // Card Drop Shadow
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (110 * alphaVal));
        Render2D.drawShadow(x, y, w, h, 8.0F, 8.0F, shadowCol);

        // Card Glass Container
        int cardBg = module.isEnabled()
                ? ColorUtil.withAlpha(accentColor, (int) ((0.12F + tVal * 0.08F) * 255 * alphaVal))
                : ColorUtil.rgba(18, 21, 32, (int) (0.75F * 255 * alphaVal));

        int outlineCol = module.isEnabled()
                ? ColorUtil.withAlpha(accentColor, (int) ((0.40F + (isHovered ? 0.20F : 0.0F)) * 255 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) ((0.10F + (isHovered ? 0.15F : 0.0F)) * 255 * alphaVal));

        Render2D.drawRoundedRect(x, y, w, h, 8.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 8.0F, 0.8F, outlineCol);

        // Status Indicator Dot
        int statusDotCol = module.isEnabled()
                ? ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal))
                : ColorUtil.rgba(120, 135, 160, (int) (140 * alphaVal));
        Render2D.drawCircle(x + 14.0F, y + 16.0F, 3.5F, statusDotCol);

        // Module Name
        int titleCol = module.isEnabled()
                ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                : ColorUtil.rgba(190, 200, 220, (int) (210 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), x + 24.0F, y + 9.5F, 8.0F, titleCol);

        // Description (Clipped so it never overlaps the toggle switch)
        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            float maxDescW = w - 75.0F;
            Render2DUtil.pushScissor(x + 24.0F, y + 24.0F, maxDescW, 14.0F);
            try {
                Fonts.drawString(Fonts.SF_MEDIUM, desc, x + 24.0F, y + 24.0F, 5.5F, ColorUtil.rgba(135, 150, 175, (int) (175 * alphaVal)));
            } finally {
                Render2DUtil.popScissor();
            }
        }

        // Toggle Switch Widget
        float switchW = 32.0F;
        float switchH = 16.0F;
        float switchX = x + w - switchW - 12.0F;
        float switchY = y + 14.0F;

        int switchTrackBg = tVal > 0.01F
                ? ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (0.12F * 255 * alphaVal));

        Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 8.0F, switchTrackBg);

        // Switch Knob
        float knobSize = 12.0F;
        float knobX = switchX + 2.0F + (switchW - knobSize - 4.0F) * tVal;
        float knobY = switchY + 2.0F;
        int knobColor = ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal));

        Render2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, 6.0F, knobColor);

        // Expanded Settings Section
        if (expandVal > 0.01F) {
            float setY = y + 42.0F;
            List<Setting<?>> settings = module.getSettings();
            for (Setting<?> setting : settings) {
                renderSettingRow(setting, x + 12.0F, setY, w - 24.0F, mouseX, mouseY, expandVal * alphaVal);
                setY += getSettingHeight(setting);
            }
        }
    }

    private void renderSettingRow(Setting<?> setting, float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        int accentColor = Theme.getAccentColor();

        if (setting instanceof CheckBox cb) {
            Fonts.drawString(Fonts.SF_MEDIUM, cb.getName(), x, y + 3.0F, 6.5F, ColorUtil.rgba(215, 225, 240, (int) (230 * alphaVal)));

            float boxSize = 14.0F;
            float boxX = x + w - boxSize;
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= y && mouseY <= y + boxSize;

            int boxBg = cb.getValue()
                    ? ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.18F : 0.08F) * 255 * alphaVal));

            Render2D.drawRoundedRect(boxX, y, boxSize, boxSize, 4.0F, boxBg);
            if (cb.getValue()) {
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✓", boxX + boxSize / 2.0F, y + 2.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
            }
        } else if (setting instanceof SliderSetting sl) {
            float val = sl.getValue();
            float min = sl.getMin();
            float max = sl.getMax();
            float pct = (val - min) / (max - min);

            Fonts.drawString(Fonts.SF_MEDIUM, sl.getName(), x, y, 6.5F, ColorUtil.rgba(215, 225, 240, (int) (230 * alphaVal)));
            String valStr = String.format("%.1f", val);
            float valW = Fonts.SF_MEDIUM.getWidth(valStr, 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, valStr, x + w - valW, y, 6.0F, ColorUtil.rgba(170, 185, 210, (int) (210 * alphaVal)));

            float barY = y + 12.0F;
            float barH = 5.0F;
            Render2D.drawRoundedRect(x, barY, w, barH, 2.5F, ColorUtil.rgba(255, 255, 255, (int) (25 * alphaVal)));

            float fillW = Math.max(5.0F, w * pct);
            Render2D.drawRoundedRect(x, barY, fillW, barH, 2.5F, ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal)));

            // Knob handle
            float handleX = x + fillW - 2.5F;
            Render2D.drawCircle(handleX, barY + 2.5F, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
        } else if (setting instanceof BindSetting b) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Назначение клавиши", x, y + 3.0F, 6.5F, ColorUtil.rgba(215, 225, 240, (int) (230 * alphaVal)));

            String bindText = (activeBindingSetting == b) ? "[...]" : b.getDisplayValue();
            float btnW = 60.0F;
            float btnH = 15.0F;
            float btnX = x + w - btnW;

            Render2D.drawRoundedRect(btnX, y, btnW, btnH, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, bindText, btnX + btnW / 2.0F, y + 4.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal)));
        }
    }

    private float calculateSettingsHeight(Module module) {
        float h = 6.0F;
        for (Setting<?> s : module.getSettings()) {
            h += getSettingHeight(s);
        }
        return h;
    }

    private float getSettingHeight(Setting<?> setting) {
        if (setting instanceof SliderSetting) return 22.0F;
        return 18.0F;
    }

    private List<Module> getFilteredModules() {
        List<Module> result = new ArrayList<>();
        String q = searchQuery.toLowerCase().trim();
        for (Module m : Client.getInstance().moduleManager.getModules()) {
            if (!q.isEmpty()) {
                if (m.getName().toLowerCase().contains(q) || (m.getDescription() != null && m.getDescription().toLowerCase().contains(q))) {
                    result.add(m);
                }
            } else if (m.getCategory() == activeCategory) {
                result.add(m);
            }
        }
        return result;
    }

    private void updateSliderDrag(int mouseX) {
        if (this.draggingSlider == null) return;

        int screenW = this.width;
        int screenH = this.height;
        float x = (screenW - WINDOW_W) / 2.0F;
        float y = (screenH - WINDOW_H) / 2.0F;

        float gridX = x + SIDEBAR_W + 16.0F;
        float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
        float cardW = (gridW - 14.0F) / 2.0F;
        float sliderW = cardW - 24.0F;

        // Approximate slider bounds offset inside card
        float min = draggingSlider.getMin();
        float max = draggingSlider.getMax();

        // Calculate pct relative to mouse
        float pct = Math.clamp((mouseX - gridX - 12.0F) / sliderW, 0.0F, 1.0F);
        float newVal = min + pct * (max - min);
        draggingSlider.setValue(newVal);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int screenW = this.width;
        int screenH = this.height;

        float x = (screenW - WINDOW_W) / 2.0F;
        float y = (screenH - WINDOW_H) / 2.0F;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Sidebar Category Clicks
            Category[] categories = Category.values();
            float catY = y + 52.0F;
            float catH = 28.0F;
            float catW = SIDEBAR_W - 24.0F;
            float catX = x + 12.0F;

            for (Category cat : categories) {
                if (mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH) {
                    this.activeCategory = cat;
                    this.searchQuery = "";
                    this.searchFocused = false;
                    return true;
                }
                catY += catH + 4.0F;
            }

            // Search Bar Click
            float searchW = 160.0F;
            float searchH = 24.0F;
            float searchX = x + WINDOW_W - SIDEBAR_W - 16.0F - searchW;
            float searchY = y + 16.0F;

            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Modules Clicks
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 16.0F;
            float gridY = y + 54.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
            float cardW = (gridW - 14.0F) / 2.0F;
            float startY = gridY - scrollAnim.getValue();

            float leftY = startY;
            float rightY = startY;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);

                float cardX = isRightColumn ? gridX + cardW + 14.0F : gridX;
                float currentY = isRightColumn ? rightY : leftY;

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                float eVal = expandAnim.getValue();
                float cardH = 44.0F + (eVal * calculateSettingsHeight(module));

                // Click inside card header (44px height)
                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 44.0F) {
                    module.toggle();
                    return true;
                }

                // Click inside expanded settings
                if (eVal > 0.01F && mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY + 42.0F && mouseY <= currentY + cardH) {
                    float setY = currentY + 42.0F;
                    for (Setting<?> setting : module.getSettings()) {
                        float sH = getSettingHeight(setting);
                        if (mouseY >= setY && mouseY <= setY + sH) {
                            if (setting instanceof CheckBox cb) {
                                cb.setValue(!cb.getValue());
                            } else if (setting instanceof SliderSetting sl) {
                                this.draggingSlider = sl;
                                updateSliderDrag((int) mouseX);
                            } else if (setting instanceof BindSetting b) {
                                this.activeBindingSetting = b;
                            }
                            return true;
                        }
                        setY += sH;
                    }
                }

                if (isRightColumn) {
                    rightY += cardH + 10.0F;
                } else {
                    leftY += cardH + 10.0F;
                }
            }
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // Right-click module card to expand/collapse settings
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 16.0F;
            float gridY = y + 54.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
            float cardW = (gridW - 14.0F) / 2.0F;
            float startY = gridY - scrollAnim.getValue();

            float leftY = startY;
            float rightY = startY;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);

                float cardX = isRightColumn ? gridX + cardW + 14.0F : gridX;
                float currentY = isRightColumn ? rightY : leftY;

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                float eVal = expandAnim.getValue();
                float cardH = 44.0F + (eVal * calculateSettingsHeight(module));

                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 44.0F) {
                    this.expandedModule = (this.expandedModule == module) ? null : module;
                    return true;
                }

                if (isRightColumn) {
                    rightY += cardH + 10.0F;
                } else {
                    leftY += cardH + 10.0F;
                }
            }
        }
        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollTarget -= (float) (scrollY * 28.0D);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.activeBindingSetting instanceof BindSetting b) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                b.setValue(List.of());
            } else {
                b.setValue(List.of(event.key()));
            }
            this.activeBindingSetting = null;
            return true;
        }

        if (this.searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.searchQuery.isEmpty()) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                }
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
                return true;
            }
        }

        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            if (System.currentTimeMillis() - this.openTime < 300L) {
                return true;
            }
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.searchFocused) {
            int codePoint = event.codepoint();
            if (codePoint >= 32 && codePoint != 127) {
                this.searchQuery += new String(Character.toChars(codePoint));
                return true;
            }
        }
        return super.charTyped(event);
    }
}
