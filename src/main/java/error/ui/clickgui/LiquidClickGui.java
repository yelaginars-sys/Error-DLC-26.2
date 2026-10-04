package error.ui.clickgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.display.batch.DisplayBatcher;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.display.rounded.RoundedRect;
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
import error.module.impl.render.ClickGui;
import error.util.client.persiki.KeyUtil;
import error.ui.hud.HudManager;
import error.ui.hud.HudElement;
import error.ui.hud.impl.DynamicIslandHud;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LiquidClickGui extends Screen {

    public static final float WINDOW_W = 540.0F;
    public static final float WINDOW_H = 360.0F;
    public static final float SIDEBAR_W = 125.0F;

    private static final Color FADE_WHITE = Color.rgba(255, 255, 255, 32);

    public Category activeCategory = Category.COMBAT;
    public String searchQuery = "";
    public boolean searchFocused = false;

    public boolean settingsModalOpen = false;
    public ColorSetting activeEditingColorSetting = null;
    public boolean bindingClickGuiKey = false;
    public boolean editingSecondaryColor = false;
    public float pickerHue = 0.55F;
    public float pickerSat = 1.0F;
    public float pickerBri = 1.0F;

    private enum DragTarget { NONE, FIELD_2D, HUE_VERT }
    private DragTarget draggingPicker = DragTarget.NONE;

    private final Animation openAnim = new Animation(1.0F, 0.20F);
    private final Animation scrollAnim = new Animation(0.0F, 0.22F);
    private float scrollTarget = 0.0F;
    private long openTime = System.currentTimeMillis();

    private final Map<Module, Animation> moduleExpandAnims = new HashMap<>();
    private final Map<Module, Animation> moduleToggleAnims = new HashMap<>();
    private final Map<Category, Animation> categoryHoverAnims = new HashMap<>();

    public Module expandedModule = null;
    public Setting<?> activeBindingSetting = null;
    private SliderSetting draggingSlider = null;

    private final List<Snowflake2D> snowflakes = new ArrayList<>();
    private final Random random = new Random();

    public LiquidClickGui() {
        super(Component.literal("ClickGUI"));
        this.openAnim.setValue(1.0F);
        this.openAnim.setTarget(1.0F);
        this.openTime = System.currentTimeMillis();
        initSnowflakes();
    }

    private void initSnowflakes() {
        snowflakes.clear();
        for (int i = 0; i < 30; i++) {
            snowflakes.add(new Snowflake2D(
                    random.nextFloat() * 900.0F,
                    random.nextFloat() * 600.0F,
                    1.0F + random.nextFloat() * 1.6F,
                    12.0F + random.nextFloat() * 22.0F,
                    random.nextFloat() * 100.0F
            ));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.openAnim.setValue(1.0F);
        this.openAnim.setTarget(1.0F);
        this.openTime = System.currentTimeMillis();
        this.draggingSlider = null;
        this.draggingPicker = DragTarget.NONE;
        if (this.activeCategory == Category.THEMES) {
            this.activeCategory = Category.COMBAT;
        }
        if (snowflakes.isEmpty()) initSnowflakes();
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

        if (this.draggingPicker != DragTarget.NONE && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            updatePickerDrag(mouseX, mouseY);
        } else {
            this.draggingPicker = DragTarget.NONE;
        }

        if (this.draggingSlider != null && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            updateSliderDrag(mouseX);
        } else {
            this.draggingSlider = null;
        }

        int accentColor = Theme.getAccentColor();

        // 1. Subtle World Dimmer
        RoundedRect.of(0, 0, screenW, screenH)
                .color(Color.rgba(0, 0, 0, Math.round(75 * animVal)))
                .render(extractor);

        // 2. Liquid Glass Window (The exact frosted Kawase Blur from RenderDemo)
        Blur.of(x, y, WINDOW_W, WINDOW_H)
                .radius(12)
                .type(BlurType.KAWASE)
                .strength(4)
                .tint(Color.rgba(0, 0, 0, Math.round(75 * animVal)))
                .alpha(animVal)
                .render(extractor);

        Outline.of(x, y, WINDOW_W, WINDOW_H)
                .radius(12)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(animVal)
                .render(extractor);

        // 3. Settings Modal Liquid Glass Window (Exact same Kawase shader pipeline)
        if (this.settingsModalOpen) {
            float modalW = 340.0F;
            float modalH = (this.activeEditingColorSetting != null) ? 175.0F : 208.0F;
            float modalX = (screenW - modalW) / 2.0F;
            float modalY = (screenH - modalH) / 2.0F;

            Blur.of(modalX, modalY, modalW, modalH)
                    .radius(12)
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(0, 0, 0, Math.round(85 * animVal)))
                    .alpha(animVal)
                    .render(extractor);

            Outline.of(modalX, modalY, modalW, modalH)
                    .radius(12)
                    .thickness(1.0F)
                    .verticalGradient(Color.WHITE, FADE_WHITE)
                    .alpha(animVal)
                    .render(extractor);
        }

        // Flush the glass background first
        DisplayBatcher.flush();

        // 4. Render Cards, Buttons, and Fonts via Render2D & Render2DUtil
        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // Winter snowflakes
            renderSnowflakes(screenW, screenH, animVal);

            // Subtle divider line
            Render2D.drawRoundedRect(x + SIDEBAR_W, y + 10.0F, 1.0F, WINDOW_H - 20.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (16 * animVal)));

            // Sidebar and Header
            renderSidebar(x, y, mouseX, mouseY, animVal, accentColor);
            renderHeader(x + SIDEBAR_W + 12.0F, y + 14.0F, WINDOW_W - SIDEBAR_W - 24.0F, mouseX, mouseY, animVal, accentColor);

            // Content Section
            float contentX = x + SIDEBAR_W + 12.0F;
            float contentY = y + 44.0F;
            float contentW = WINDOW_W - SIDEBAR_W - 24.0F;
            float contentH = WINDOW_H - 54.0F;

            if (activeCategory == Category.CONFIGS) {
                renderConfigsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            } else if (activeCategory == Category.FRIENDS) {
                renderFriendsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.EVENTS || activeCategory == Category.COSMETICS) {
                renderEventsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            } else {
                renderModulesGrid(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            }

            // Settings Modal Popup
            if (this.settingsModalOpen) {
                renderSettingsModal(screenW, screenH, mouseX, mouseY, animVal, accentColor);
            }
        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderSnowflakes(int screenW, int screenH, float alphaVal) {
        long time = System.currentTimeMillis();
        for (Snowflake2D sf : snowflakes) {
            sf.y += sf.speed * 0.035F;
            sf.x += (float) Math.sin((time * 0.002F) + sf.seed) * 0.3F;

            if (sf.y > screenH + 8.0F) {
                sf.y = -8.0F;
                sf.x = random.nextFloat() * screenW;
            }

            Render2D.drawCircle(sf.x, sf.y, sf.size, ColorUtil.rgba(220, 240, 255, (int) (110 * alphaVal)));
        }
    }

    private void renderSidebar(float x, float y, int mouseX, int mouseY, float alphaVal, int accentColor) {
        // Gear Settings Button in the very top-left corner
        float gearX = x + 9.0F;
        float gearY = y + 10.0F;
        float gearSize = 18.0F;
        boolean gearHovered = mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize;
        int gearBg = settingsModalOpen ? ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)) : (gearHovered ? ColorUtil.rgba(255, 255, 255, (int) (35 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
        int gearOutline = settingsModalOpen ? ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal)) : (gearHovered ? ColorUtil.rgba(255, 255, 255, (int) (45 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

        Render2D.drawRoundedRect(gearX, gearY, gearSize, gearSize, 4.5F, gearBg);
        Render2D.drawRoundedOutline(gearX, gearY, gearSize, gearSize, 4.5F, 0.65F, gearOutline);

        // Crisp geometric settings / sliders icon (no broken font/texture dependencies!)
        int iconCol = settingsModalOpen ? 0xFFFFFFFF : (gearHovered ? accentColor : 0xFFB0C0D4);
        float cy = gearY + gearSize / 2.0F;
        Render2D.drawRoundedRect(gearX + 3.5F, cy - 3.8F, 11.0F, 1.4F, 0.7F, iconCol);
        Render2D.drawCircle(gearX + 6.0F, cy - 3.1F, 1.6F, iconCol);

        Render2D.drawRoundedRect(gearX + 3.5F, cy - 0.7F, 11.0F, 1.4F, 0.7F, iconCol);
        Render2D.drawCircle(gearX + 11.0F, cy, 1.6F, iconCol);

        Render2D.drawRoundedRect(gearX + 3.5F, cy + 2.4F, 11.0F, 1.4F, 0.7F, iconCol);
        Render2D.drawCircle(gearX + 7.5F, cy + 3.1F, 1.6F, iconCol);

        // Branding next to the gear button
        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", gearX + gearSize + 7.0F, y + 9.5F, 8.5F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Winter 26.2", gearX + gearSize + 7.0F, y + 20.0F, 4.8F, 0xFFA0B0C4);

        // Category List (Themes removed)
        Category[] categories = Arrays.stream(Category.values()).filter(c -> c != Category.THEMES).toArray(Category[]::new);
        float catY = y + 36.0F;
        float catH = 21.0F;
        float catW = SIDEBAR_W - 16.0F;
        float catX = x + 8.0F;

        for (Category cat : categories) {
            boolean active = (cat == this.activeCategory && searchQuery.isEmpty());
            boolean isHovered = mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH;

            Animation hoverAnim = categoryHoverAnims.computeIfAbsent(cat, k -> new Animation(0.0F, 0.16F));
            hoverAnim.setTarget(active ? 1.0F : (isHovered ? 0.45F : 0.0F));
            hoverAnim.update();
            float hVal = hoverAnim.getValue();

            if (active) {
                // Sleek frosted pill with accent indicator
                Render2D.drawRoundedRect(catX, catY, catW, catH, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));
                Render2D.drawRoundedOutline(catX, catY, catW, catH, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (40 * alphaVal)));
                Render2D.drawRoundedRect(catX + 2.0F, catY + 3.5F, 2.5F, catH - 7.0F, 1.0F, accentColor);
            } else if (hVal > 0.01F) {
                Render2D.drawRoundedRect(catX, catY, catW, catH, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (hVal * 16 * alphaVal)));
            }

            // Clean category indicator dot (no broken font characters!)
            int dotCol = active ? accentColor : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 120 : 50) * alphaVal));
            Render2D.drawCircle(catX + 10.0F, catY + catH / 2.0F, 2.2F, dotCol);

            int nameCol = active ? 0xFFFFFFFF : 0xFFC0D0E0;
            Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 18.0F, catY + 6.5F, 6.2F, nameCol);

            // Badge count
            long enabledCount = Client.getInstance().moduleManager.getModules().stream()
                    .filter(m -> m.getCategory() == cat && m.isEnabled())
                    .count();

            if (enabledCount > 0) {
                String countStr = String.valueOf(enabledCount);
                float countW = Fonts.SF_MEDIUM.getWidth(countStr, 4.8F);
                float badgeW = Math.max(11.0F, countW + 5.0F);
                float badgeX = catX + catW - 6.0F - badgeW;
                float badgeY = catY + 4.5F;

                int badgeBg = active ? ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal));
                Render2D.drawRoundedRect(badgeX, badgeY, badgeW, 11.5F, 3.0F, badgeBg);
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, countStr, badgeX + badgeW / 2.0F, badgeY + 2.5F, 4.8F, 0xFFFFFFFF);
            }

            catY += catH + 2.0F;
        }

        // Bottom User Profile Card
        float profileW = SIDEBAR_W - 16.0F;
        float profileH = 26.0F;
        float profileX = x + 8.0F;
        float profileY = y + WINDOW_H - 34.0F;

        Render2D.drawRoundedRect(profileX, profileY, profileW, profileH, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (12 * alphaVal)));
        Render2D.drawRoundedOutline(profileX, profileY, profileW, profileH, 5.0F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

        Render2D.drawCustomAvatar(profileX + 4.0F, profileY + 4.0F, 18.0F, 4.0F, alphaVal);

        String username = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "User";
        Fonts.drawString(Fonts.SF_MEDIUM, username, profileX + 26.0F, profileY + 4.5F, 5.8F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Developer", profileX + 26.0F, profileY + 14.0F, 4.2F, 0xFFA0B4C8);
    }

    private void renderHeader(float x, float y, float w, int mouseX, int mouseY, float alphaVal, int accentColor) {
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";
        Fonts.drawString(Fonts.SF_MEDIUM, titleText, x, y + 2.0F, 9.5F, 0xFFFFFFFF);

        // Search Bar (Pill) - shifted left by 35px from right margin
        float searchW = 135.0F;
        float searchH = 19.0F;
        float searchX = x + w - searchW - 35.0F;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y && mouseY <= y + searchH;
        int boxBg = ColorUtil.rgba(255, 255, 255, (int) ((searchFocused ? 18 : (isHovered ? 12 : 8)) * alphaVal));
        int boxBorder = searchFocused ? ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));

        Render2D.drawRoundedRect(searchX, y, searchW, searchH, 4.5F, boxBg);
        Render2D.drawRoundedOutline(searchX, y, searchW, searchH, 4.5F, 0.65F, boxBorder);

        // Magnifying glass icon (pure Render2D, no font dependencies)
        int scCol = searchFocused ? accentColor : 0xFF98A8C0;
        Render2D.drawCircleOutline(searchX + 10.0F, y + 8.5F, 3.0F, 0.85F, scCol);
        Render2D.drawRoundedRect(searchX + 12.0F, y + 10.5F, 3.2F, 1.1F, 0.55F, scCol);

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        int textCol = searchQuery.isEmpty() && !searchFocused ? 0xFF8898B0 : 0xFFFFFFFF;
        Fonts.drawString(Fonts.SF_MEDIUM, displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), searchX + 20.0F, y + 5.5F, 5.8F, textCol);
    }

    private void renderModulesGrid(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        List<Module> modules = getFilteredModules();

        if (modules.isEmpty()) {
            Render2D.drawRoundedRect(x, y, w, 80.0F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (8 * alphaVal)));
            Render2D.drawRoundedOutline(x, y, w, 80.0F, 7.0F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "В этой категории нет модулей", x + w / 2.0F, y + 30.0F, 7.0F, 0xFFC0D0E0);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Выберите другую категорию в меню слева", x + w / 2.0F, y + 46.0F, 5.2F, 0xFF8898B0);
            return;
        }

        float cardW = (w - 10.0F) / 2.0F;
        float startY = y - scrollAnim.getValue();

        float leftY = startY;
        float rightY = startY;

        Render2DUtil.pushScissor(x - 2.0F, y, w + 4.0F, h);
        try {
            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);

                float cardX = isRightColumn ? x + cardW + 10.0F : x;
                float currentY = isRightColumn ? rightY : leftY;

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                boolean isExpanded = (expandedModule == module);
                expandAnim.setTarget(isExpanded ? 1.0F : 0.0F);
                expandAnim.update();
                float eVal = expandAnim.getValue();

                float cardH = 34.0F + (eVal * calculateSettingsHeight(module));

                if (currentY + cardH >= y && currentY <= y + h) {
                    renderModuleCard(module, cardX, currentY, cardW, cardH, eVal, mouseX, mouseY, alphaVal, accentColor);
                }

                if (isRightColumn) {
                    rightY += cardH + 7.0F;
                } else {
                    leftY += cardH + 7.0F;
                }
            }
        } finally {
            Render2DUtil.popScissor();
        }

        float contentH = Math.max(leftY, rightY) - startY;
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0F, Math.max(0.0F, contentH - h));
        this.scrollAnim.setTarget(this.scrollTarget);
    }

    private void renderModuleCard(Module module, float x, float y, float w, float h, float expandVal, int mouseX, int mouseY, float alphaVal, int accentColor) {
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 34.0F;

        Animation toggleAnim = moduleToggleAnims.computeIfAbsent(module, k -> new Animation(module.isEnabled() ? 1.0F : 0.0F, 0.18F));
        toggleAnim.setTarget(module.isEnabled() ? 1.0F : 0.0F);
        toggleAnim.update();
        float tVal = toggleAnim.getValue();

        // Distinct styling: noticeable frosted glass card, but NOT a solid blue block!
        if (module.isEnabled()) {
            // Elegant dark graphite glass with soft accent tint
            int cardBg = ColorUtil.rgba(24, 32, 48, (int) (170 * alphaVal));
            int cardOutline = ColorUtil.withAlpha(accentColor, (int) ((120 + (isHovered ? 40 : 0)) * alphaVal));

            Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
            Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 0.85F, cardOutline);
        } else {
            // Inactive clean frosted glass card
            int cardBg = ColorUtil.rgba(20, 24, 34, (int) ((isHovered ? 130 : 90) * alphaVal));
            int cardOutline = ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 28 : 16) * alphaVal));

            Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
            Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 0.75F, cardOutline);
        }

        // Status Indicator Dot
        int dotCol = module.isEnabled() ? accentColor : ColorUtil.rgba(110, 120, 140, (int) (120 * alphaVal));
        Render2D.drawCircle(x + 10.0F, y + 12.0F, 2.6F, dotCol);

        // Module Name
        int titleCol = module.isEnabled() ? 0xFFFFFFFF : 0xFFC0D0E0;
        Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), x + 18.0F, y + 6.5F, 6.8F, titleCol);

        // Module Description
        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            float maxDescW = w - 58.0F;
            Render2DUtil.pushScissor(x + 18.0F, y + 18.0F, maxDescW, 11.0F);
            try {
                Fonts.drawString(Fonts.SF_MEDIUM, desc, x + 18.0F, y + 18.0F, 4.6F, 0xFF8898B0);
            } finally {
                Render2DUtil.popScissor();
            }
        }

        // Toggle Switch Widget
        float switchW = 24.0F;
        float switchH = 12.0F;
        float switchX = x + w - switchW - 8.0F;
        float switchY = y + 11.0F;

        int trackCol = tVal > 0.01F ? ColorUtil.withAlpha(accentColor, (int) (210 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));
        Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 6.0F, trackCol);

        // Knob
        float knobSize = 8.0F;
        float knobX = switchX + 2.0F + (switchW - knobSize - 4.0F) * tVal;
        float knobY = switchY + 2.0F;
        Render2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, 4.0F, 0xFFFFFFFF);

        // Settings section
        if (expandVal > 0.01F) {
            float setY = y + 33.0F;
            List<Setting<?>> settings = module.getSettings();
            for (Setting<?> setting : settings) {
                renderSettingRow(setting, x + 9.0F, setY, w - 18.0F, mouseX, mouseY, expandVal * alphaVal, accentColor);
                setY += getSettingHeight(setting);
            }
        }
    }

    private void renderSettingRow(Setting<?> setting, float x, float y, float w, int mouseX, int mouseY, float alphaVal, int accentColor) {
        if (setting instanceof CheckBox cb) {
            Fonts.drawString(Fonts.SF_MEDIUM, cb.getName(), x, y + 2.0F, 5.6F, 0xFFE0E8F5);

            float boxSize = 10.5F;
            float boxX = x + w - boxSize;
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= y && mouseY <= y + boxSize;

            int boxBg = cb.getValue() ? accentColor : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 35 : 20) * alphaVal));
            Render2D.drawRoundedRect(boxX, y, boxSize, boxSize, 3.0F, boxBg);

            if (cb.getValue()) {
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✓", boxX + boxSize / 2.0F, y + 1.2F, 6.2F, 0xFFFFFFFF);
            }
        } else if (setting instanceof SliderSetting sl) {
            float val = sl.getValue();
            float min = sl.getMin();
            float max = sl.getMax();
            float pct = (val - min) / (max - min);

            Fonts.drawString(Fonts.SF_MEDIUM, sl.getName(), x, y, 5.6F, 0xFFE0E8F5);
            String valStr = String.format("%.1f", val);
            float valW = Fonts.SF_MEDIUM.getWidth(valStr, 5.2F);
            Fonts.drawString(Fonts.SF_MEDIUM, valStr, x + w - valW, y, 5.2F, 0xFFA0B4C8);

            float barY = y + 9.5F;
            float barH = 3.5F;
            Render2D.drawRoundedRect(x, barY, w, barH, 1.8F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

            float fillW = Math.max(3.5F, w * pct);
            Render2D.drawRoundedRect(x, barY, fillW, barH, 1.8F, accentColor);

            float handleX = x + fillW - 2.0F;
            Render2D.drawCircle(handleX, barY + 1.8F, 3.0F, 0xFFFFFFFF);
        } else if (setting instanceof BindSetting b) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Назначение клавиши", x, y + 2.0F, 5.6F, 0xFFE0E8F5);

            String bindText = (activeBindingSetting == b) ? "[...]" : b.getDisplayValue();
            float btnW = 46.0F;
            float btnH = 12.0F;
            float btnX = x + w - btnW;

            Render2D.drawRoundedRect(btnX, y, btnW, btnH, 3.0F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, bindText, btnX + btnW / 2.0F, y + 2.5F, 5.0F, 0xFFFFFFFF);
        } else if (setting instanceof ModeSetting ms) {
            Fonts.drawString(Fonts.SF_MEDIUM, ms.getName(), x, y + 2.0F, 5.6F, 0xFFE0E8F5);

            String val = ms.getValue();
            float valW = Fonts.SF_MEDIUM.getWidth(val, 5.2F);
            float btnW = Math.max(42.0F, valW + 12.0F);
            float btnH = 12.0F;
            float btnX = x + w - btnW;

            boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= y && mouseY <= y + btnH;
            int btnBg = ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 28 : 16) * alphaVal));
            Render2D.drawRoundedRect(btnX, y, btnW, btnH, 3.0F, btnBg);
            Render2D.drawRoundedOutline(btnX, y, btnW, btnH, 3.0F, 0.65F, isHovered ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, val, btnX + btnW / 2.0F, y + 2.5F, 5.0F, 0xFFFFFFFF);
        } else if (setting instanceof ColorSetting cs) {
            Fonts.drawString(Fonts.SF_MEDIUM, cs.getName(), x, y + 2.0F, 5.6F, 0xFFE0E8F5);

            int curCol = cs.getValue();
            String hex = String.format("#%06X", curCol & 0x00FFFFFF);
            float hexW = Fonts.SF_MEDIUM.getWidth(hex, 4.8F);
            float swatchSize = 9.5F;
            float btnW = swatchSize + 5.0F + hexW + 6.0F;
            float btnH = 12.0F;
            float btnX = x + w - btnW;

            boolean isHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= y && mouseY <= y + btnH;
            int btnBg = ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 28 : 16) * alphaVal));
            Render2D.drawRoundedRect(btnX, y, btnW, btnH, 3.0F, btnBg);
            Render2D.drawRoundedOutline(btnX, y, btnW, btnH, 3.0F, 0.65F, isHovered ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal)));

            Render2D.drawRoundedRect(btnX + 3.0F, y + 1.25F, swatchSize, swatchSize, 2.5F, curCol);
            Render2D.drawRoundedOutline(btnX + 3.0F, y + 1.25F, swatchSize, swatchSize, 2.5F, 0.5F, 0xFFFFFFFF);
            Fonts.drawString(Fonts.SF_MEDIUM, hex, btnX + 3.0F + swatchSize + 3.5F, y + 2.5F, 4.8F, 0xFFE0E8F5);
        }
    }

    // ===================== SETTINGS MODAL & COLOR PICKER =====================

    private void openSettingsModal() {
        this.activeEditingColorSetting = null;
        this.settingsModalOpen = true;
        this.bindingClickGuiKey = false;
        syncPickerFromCurrent();
    }

    private void openColorSettingModal(ColorSetting cs) {
        this.activeEditingColorSetting = cs;
        this.settingsModalOpen = true;
        this.bindingClickGuiKey = false;
        float[] hsv = ColorUtil.toHsv(cs.getValue());
        this.pickerHue = hsv[0];
        this.pickerSat = hsv[1];
        this.pickerBri = hsv[2];
    }

    private void syncPickerFromCurrent() {
        int targetColor = editingSecondaryColor ? Theme.getSecondaryColor() : Theme.getAccentColor();
        float[] hsv = ColorUtil.toHsv(targetColor);
        this.pickerHue = hsv[0];
        this.pickerSat = hsv[1];
        this.pickerBri = hsv[2];
    }

    private void setEditingSecondary(boolean secondary) {
        if (this.editingSecondaryColor != secondary) {
            this.editingSecondaryColor = secondary;
            syncPickerFromCurrent();
        }
    }

    private void applyPickerColor() {
        int color = ColorUtil.fromHsv(pickerHue, pickerSat, pickerBri, 255);
        if (activeEditingColorSetting != null) {
            activeEditingColorSetting.setValue(color);
        } else if (editingSecondaryColor) {
            Theme.setSecondaryColor(color);
        } else {
            Theme.setAccentColor(color);
        }
        if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
            Client.INSTANCE.configManager.autoSave();
        }
    }

    private void updatePickerDrag(int mouseX, int mouseY) {
        if (this.draggingPicker == DragTarget.NONE) return;

        int screenW = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenH = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);
        float modalW = 340.0F;
        float modalH = (activeEditingColorSetting != null) ? 175.0F : 208.0F;
        float modalX = (screenW - modalW) / 2.0F;
        float modalY = (screenH - modalH) / 2.0F;

        float fieldX = modalX + 14.0F;
        float fieldY = (activeEditingColorSetting != null) ? (modalY + 32.0F) : (modalY + 68.0F);
        float fieldW = 145.0F;
        float fieldH = 100.0F;

        float hueX = fieldX + fieldW + 10.0F;
        float hueY = fieldY;
        float hueH = fieldH;

        if (this.draggingPicker == DragTarget.FIELD_2D) {
            float sat = Math.clamp((mouseX - fieldX) / fieldW, 0.0F, 1.0F);
            float bri = Math.clamp(1.0F - ((mouseY - fieldY) / fieldH), 0.0F, 1.0F);
            this.pickerSat = sat;
            this.pickerBri = bri;
            applyPickerColor();
        } else if (this.draggingPicker == DragTarget.HUE_VERT) {
            float hue = Math.clamp((mouseY - hueY) / hueH, 0.0F, 1.0F);
            this.pickerHue = hue;
            applyPickerColor();
        }
    }

    private void renderSettingsModal(int screenW, int screenH, int mouseX, int mouseY, float alphaVal, int accentColor) {
        float modalW = 340.0F;
        float modalH = (activeEditingColorSetting != null) ? 175.0F : 208.0F;
        float modalX = (screenW - modalW) / 2.0F;
        float modalY = (screenH - modalH) / 2.0F;

        // Dim background behind modal
        Render2D.drawRoundedRect(0, 0, screenW, screenH, 0.0F, ColorUtil.rgba(0, 0, 0, (int) (120 * alphaVal)));

        // Shadow and subtle glass body (Kawase Blur & Outline are already flushed via DisplayBatcher!)
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 12.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (220 * alphaVal)));
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 12.0F, ColorUtil.rgba(14, 18, 28, (int) (85 * alphaVal)));

        // Header: Settings Icon + Title + Close Button
        float headY = modalY + 8.0F;
        float iconX = modalX + 13.0F;
        float iconY = headY + 2.0F;
        // Crisp geometric sliders icon
        Render2D.drawRoundedRect(iconX, iconY + 1.0F, 10.0F, 1.3F, 0.6F, accentColor);
        Render2D.drawCircle(iconX + 3.0F, iconY + 1.6F, 1.5F, accentColor);
        Render2D.drawRoundedRect(iconX, iconY + 4.5F, 10.0F, 1.3F, 0.6F, accentColor);
        Render2D.drawCircle(iconX + 7.5F, iconY + 5.1F, 1.5F, accentColor);
        Render2D.drawRoundedRect(iconX, iconY + 8.0F, 10.0F, 1.3F, 0.6F, accentColor);
        Render2D.drawCircle(iconX + 4.5F, iconY + 8.6F, 1.5F, accentColor);

        if (activeEditingColorSetting != null) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Выбор цвета: " + activeEditingColorSetting.getName(), iconX + 15.0F, headY + 1.5F, 7.5F, 0xFFFFFFFF);
            Fonts.drawString(Fonts.SF_MEDIUM, "Палитра RGB / HEX", iconX + 140.0F, headY + 2.5F, 5.0F, 0xFFA0B4C8);
        } else {
            Fonts.drawString(Fonts.SF_MEDIUM, "Настройки Клиента", iconX + 15.0F, headY + 1.5F, 7.5F, 0xFFFFFFFF);
            Fonts.drawString(Fonts.SF_MEDIUM, "Бинды и Палитра", iconX + 104.0F, headY + 2.5F, 5.0F, 0xFFA0B4C8);
        }

        // Close Button
        float closeX = modalX + modalW - 22.0F;
        float closeY = modalY + 7.0F;
        float closeSize = 14.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize;
        Render2D.drawRoundedRect(closeX, closeY, closeSize, closeSize, 3.5F, closeHover ? ColorUtil.rgba(240, 70, 70, (int) (200 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✕", closeX + closeSize / 2.0F, closeY + 2.5F, 6.0F, 0xFFFFFFFF);

        // Divider
        Render2D.drawRoundedRect(modalX + 10.0F, modalY + 24.0F, modalW - 20.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (16 * alphaVal)));

        float fieldY;

        if (activeEditingColorSetting == null) {
            // Row 1: ClickGUI Keybind & Mode (Static / Chroma)
            float row1Y = modalY + 29.0F;
            Fonts.drawString(Fonts.SF_MEDIUM, "Бинд GUI:", modalX + 14.0F, row1Y + 2.5F, 5.8F, 0xFFD0E0F0);

            float bindBtnX = modalX + 58.0F;
            float bindBtnW = 60.0F;
            float bindBtnH = 14.0F;
            String keyText;
            if (bindingClickGuiKey) {
                keyText = "[Нажмите...]";
            } else if (ClickGui.INSTANCE != null && !ClickGui.INSTANCE.getBind().isEmpty()) {
                keyText = KeyUtil.getKeyName(ClickGui.INSTANCE.getBind().get(0));
            } else {
                keyText = "NONE";
            }
            boolean bindHover = mouseX >= bindBtnX && mouseX <= bindBtnX + bindBtnW && mouseY >= row1Y && mouseY <= row1Y + bindBtnH;
            int bindBg = bindingClickGuiKey ? ColorUtil.withAlpha(accentColor, (int) (200 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((bindHover ? 26 : 16) * alphaVal));
            Render2D.drawRoundedRect(bindBtnX, row1Y, bindBtnW, bindBtnH, 3.5F, bindBg);
            Render2D.drawRoundedOutline(bindBtnX, row1Y, bindBtnW, bindBtnH, 3.5F, 0.65F, bindingClickGuiKey ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, keyText, bindBtnX + bindBtnW / 2.0F, row1Y + 2.5F, 5.2F, 0xFFFFFFFF);

            // Mode: Static vs Chroma RGB
            boolean isChroma = "Chroma".equalsIgnoreCase(Theme.getAccentMode()) || "RGB".equalsIgnoreCase(Theme.getAccentMode());
            float segW = 56.0F;
            float segH = 14.0F;
            float staticBtnX = modalX + modalW - 14.0F - (segW * 2 + 4.0F);
            float chromaBtnX = staticBtnX + segW + 4.0F;

            boolean sHover = mouseX >= staticBtnX && mouseX <= staticBtnX + segW && mouseY >= row1Y && mouseY <= row1Y + segH;
            int sBg = !isChroma ? ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((sHover ? 24 : 14) * alphaVal));
            Render2D.drawRoundedRect(staticBtnX, row1Y, segW, segH, 3.5F, sBg);
            Render2D.drawRoundedOutline(staticBtnX, row1Y, segW, segH, 3.5F, 0.65F, !isChroma ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Статичный", staticBtnX + segW / 2.0F, row1Y + 2.5F, 5.0F, 0xFFFFFFFF);

            boolean cHover = mouseX >= chromaBtnX && mouseX <= chromaBtnX + segW && mouseY >= row1Y && mouseY <= row1Y + segH;
            int cBg = isChroma ? ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((cHover ? 24 : 14) * alphaVal));
            Render2D.drawRoundedRect(chromaBtnX, row1Y, segW, segH, 3.5F, cBg);
            Render2D.drawRoundedOutline(chromaBtnX, row1Y, segW, segH, 3.5F, 0.65F, isChroma ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "🌈 Chroma", chromaBtnX + segW / 2.0F, row1Y + 2.5F, 5.0F, 0xFFFFFFFF);

            // Row 2: Target Color Selector Tabs (Primary vs Secondary)
            float row2Y = modalY + 47.0F;
            float tabW = (modalW - 32.0F) / 2.0F;
            float tabH = 16.0F;
            float tab1X = modalX + 14.0F;
            float tab2X = tab1X + tabW + 4.0F;

            boolean tab1Active = !editingSecondaryColor;
            boolean tab1Hover = mouseX >= tab1X && mouseX <= tab1X + tabW && mouseY >= row2Y && mouseY <= row2Y + tabH;
            int tab1Bg = tab1Active ? ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((tab1Hover ? 18 : 10) * alphaVal));
            Render2D.drawRoundedRect(tab1X, row2Y, tabW, tabH, 3.5F, tab1Bg);
            Render2D.drawRoundedOutline(tab1X, row2Y, tabW, tabH, 3.5F, 0.65F, tab1Active ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));
            Render2D.drawCircle(tab1X + 8.0F, row2Y + tabH / 2.0F, 3.5F, Theme.getAccentColor());
            Fonts.drawString(Fonts.SF_MEDIUM, "Основной цвет", tab1X + 15.0F, row2Y + 3.0F, 5.4F, tab1Active ? 0xFFFFFFFF : 0xFFB0C0D4);

            boolean tab2Active = editingSecondaryColor;
            boolean tab2Hover = mouseX >= tab2X && mouseX <= tab2X + tabW && mouseY >= row2Y && mouseY <= row2Y + tabH;
            int tab2Bg = tab2Active ? ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((tab2Hover ? 18 : 10) * alphaVal));
            Render2D.drawRoundedRect(tab2X, row2Y, tabW, tabH, 3.5F, tab2Bg);
            Render2D.drawRoundedOutline(tab2X, row2Y, tabW, tabH, 3.5F, 0.65F, tab2Active ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));
            Render2D.drawCircle(tab2X + 8.0F, row2Y + tabH / 2.0F, 3.5F, Theme.getSecondaryColor());
            Fonts.drawString(Fonts.SF_MEDIUM, "Дополнительный цвет", tab2X + 15.0F, row2Y + 3.0F, 5.4F, tab2Active ? 0xFFFFFFFF : 0xFFB0C0D4);

            fieldY = modalY + 68.0F;
        } else {
            fieldY = modalY + 32.0F;
        }

        // 2D Color Picker (Exact style from screenshot media_1791122441486.png)
        float fieldX = modalX + 14.0F;
        float fieldW = 145.0F;
        float fieldH = 100.0F;

        int pureHue = ColorUtil.fromHsv(pickerHue, 1.0F, 1.0F, 255);
        // 2D Gradient Box (Bilinear interpolation: Top-Left White, Top-Right Pure Hue, Bottom-Left & Bottom-Right Black)
        Render2D.drawGradientRound(fieldX, fieldY, fieldW, fieldH, 4.0F, 0xFFFFFFFF, pureHue, 0xFF000000, 0xFF000000);
        Render2D.drawRoundedOutline(fieldX, fieldY, fieldW, fieldH, 4.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int) (45 * alphaVal)));

        // Handle indicator on 2D field
        float handleX = Math.clamp(fieldX + pickerSat * fieldW, fieldX + 1.0F, fieldX + fieldW - 1.0F);
        float handleY = Math.clamp(fieldY + (1.0F - pickerBri) * fieldH, fieldY + 1.0F, fieldY + fieldH - 1.0F);
        Render2D.drawCircleOutline(handleX, handleY, 4.5F, 1.4F, 0xFFFFFFFF);
        Render2D.drawCircleOutline(handleX, handleY, 3.2F, 0.8F, 0xFF000000);

        // Vertical Rainbow Hue Bar
        float hueX = fieldX + fieldW + 10.0F;
        float hueY = fieldY;
        float hueW = 14.0F;
        float hueH = fieldH;

        int steps = 36;
        float stepH = hueH / steps;
        for (int i = 0; i < steps; i++) {
            float h1 = (float) i / steps;
            float h2 = (float) (i + 1) / steps;
            int c1 = ColorUtil.fromHsv(h1, 1.0F, 1.0F, 255);
            int c2 = ColorUtil.fromHsv(h2, 1.0F, 1.0F, 255);
            Render2D.drawGradientRound(hueX, hueY + i * stepH, hueW, stepH + 0.5F, 0.0F, c1, c1, c2, c2);
        }
        Render2D.drawRoundedOutline(hueX, hueY, hueW, hueH, 3.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int) (45 * alphaVal)));

        // Slider Marker on Vertical Hue Bar
        float markerY = Math.clamp(hueY + pickerHue * hueH, hueY, hueY + hueH);
        Render2D.drawRoundedRect(hueX - 2.0F, markerY - 2.0F, hueW + 4.0F, 4.0F, 1.5F, 0xFFFFFFFF);
        Render2D.drawRoundedOutline(hueX - 2.0F, markerY - 2.0F, hueW + 4.0F, 4.0F, 1.5F, 0.6F, 0xFF000000);

        // Right side info panel
        float rightX = hueX + hueW + 12.0F;
        float rightW = modalX + modalW - 14.0F - rightX;

        // Big live Color Swatch with HEX
        int curChosen = ColorUtil.fromHsv(pickerHue, pickerSat, pickerBri, 255);
        String hexStr = String.format("#%06X", curChosen & 0x00FFFFFF);
        Render2D.drawRoundedRect(rightX, fieldY, rightW, 26.0F, 4.0F, curChosen);
        Render2D.drawRoundedOutline(rightX, fieldY, rightW, 26.0F, 4.0F, 0.75F, 0xFFFFFFFF);
        int textCol = (pickerBri > 0.65F && pickerSat < 0.4F) ? 0xFF000000 : 0xFFFFFFFF;
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, hexStr, rightX + rightW / 2.0F, fieldY + 8.5F, 6.5F, textCol);

        // Presets Header
        Fonts.drawString(Fonts.SF_MEDIUM, "Быстрые цвета:", rightX, fieldY + 34.0F, 5.0F, 0xFFB0C0D4);

        int[] presets = new int[]{
                ColorUtil.rgb(0, 180, 255),    // Ice Blue
                ColorUtil.rgb(255, 255, 255),  // Obsidian White
                ColorUtil.rgb(168, 85, 247),   // Neon Purple
                ColorUtil.rgb(255, 55, 75),    // Christmas Red
                ColorUtil.rgb(16, 185, 129),   // Emerald Green
                ColorUtil.rgb(251, 191, 36),   // Gold
                ColorUtil.rgb(251, 113, 133),  // Coral Pink
                ColorUtil.rgb(59, 130, 246)    // Sapphire
        };

        float pSize = 14.0F;
        float pGap = 6.0F;

        for (int i = 0; i < presets.length; i++) {
            float px = rightX + (i % 4) * (pSize + pGap);
            float py = fieldY + 45.0F + (i / 4) * 21.0F;
            boolean pHover = mouseX >= px && mouseX <= px + pSize && mouseY >= py && mouseY <= py + pSize;

            Render2D.drawRoundedRect(px, py, pSize, pSize, 3.5F, presets[i]);
            Render2D.drawRoundedOutline(px, py, pSize, pSize, 3.5F, 0.65F, pHover ? 0xFFFFFFFF : ColorUtil.rgba(255, 255, 255, (int) (40 * alphaVal)));
        }

        // Live HSB info
        Fonts.drawString(Fonts.SF_MEDIUM, "H:" + Math.round(pickerHue * 360) + "° S:" + Math.round(pickerSat * 100) + "% V:" + Math.round(pickerBri * 100) + "%", rightX, fieldY + 89.0F, 4.6F, 0xFFA0B4C8);

        // Footer Divider & Note
        float footY = modalY + modalH - 26.0F;
        Render2D.drawRoundedRect(modalX + 10.0F, footY, modalW - 20.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✦ Кликните по палитре или двигайте ползунок для выбора любого цвета", modalX + modalW / 2.0F, footY + 8.0F, 4.8F, 0xFF8898B0);
    }

    private void renderConfigsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        Render2D.drawRoundedRect(x, y, w, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (90 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Менеджер Конфигураций", x + 14.0F, y + 14.0F, 9.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Сохраняйте и загружайте настройки клиента", x + 14.0F, y + 26.0F, 5.5F, 0xFFA0B0C4);

        String[] presets = new String[]{"default", "legit_funtime", "rage_hvh", "skywars_boost"};
        float itemY = y + 46.0F;
        for (String preset : presets) {
            Render2D.drawRoundedRect(x + 14.0F, itemY, w - 28.0F, 24.0F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
            Render2D.drawRoundedOutline(x + 14.0F, itemY, w - 28.0F, 24.0F, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

            Fonts.drawString(Fonts.SF_MEDIUM, preset + ".json", x + 24.0F, itemY + 8.0F, 6.5F, 0xFFFFFFFF);

            float loadBtnX = x + w - 85.0F;
            Render2D.drawRoundedRect(loadBtnX, itemY + 4.0F, 60.0F, 16.0F, 3.5F, ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + 30.0F, itemY + 7.5F, 5.2F, 0xFFFFFFFF);

            itemY += 28.0F;
        }
    }

    private void renderFriendsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (90 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Список Друзей", x + 14.0F, y + 14.0F, 9.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Друзья автоматически игнорируются аимом и киллаурой", x + 14.0F, y + 26.0F, 5.5F, 0xFFA0B0C4);

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Список друзей пуст. Кликните ПКМ по игроку в игре чтобы добавить!", x + w / 2.0F, y + h / 2.0F, 6.5F, 0xFFA0B0C4);
    }

    private void renderEventsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        Render2D.drawRoundedRect(x, y, w, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (90 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Новогодний Ивент Error DLC 2026", x + w / 2.0F, y + h / 2.0F - 10.0F, 9.5F, accentColor);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Эксклюзивные праздничные эффекты и косметика будут доступны в обновлении!", x + w / 2.0F, y + h / 2.0F + 8.0F, 5.8F, 0xFFA0B0C4);
    }

    private float calculateSettingsHeight(Module module) {
        float h = 4.0F;
        for (Setting<?> s : module.getSettings()) {
            h += getSettingHeight(s);
        }
        return h;
    }

    private float getSettingHeight(Setting<?> setting) {
        if (setting instanceof SliderSetting) return 20.0F;
        return 16.0F;
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
        float x = (screenW - WINDOW_W) / 2.0F;

        float gridX = x + SIDEBAR_W + 12.0F;
        float gridW = WINDOW_W - SIDEBAR_W - 24.0F;
        float cardW = (gridW - 10.0F) / 2.0F;
        float sliderW = cardW - 18.0F;

        float min = draggingSlider.getMin();
        float max = draggingSlider.getMax();

        float pct = Math.clamp((mouseX - gridX - 9.0F) / sliderW, 0.0F, 1.0F);
        float newVal = min + pct * (max - min);
        draggingSlider.setValue(newVal);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int screenW = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenH = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

        float x = (screenW - WINDOW_W) / 2.0F;
        float y = (screenH - WINDOW_H) / 2.0F;

        // Forward click to Dynamic Island if open and clicked
        HudManager hudManager = HudManager.getInstance();
        if (hudManager != null) {
            for (HudElement el : hudManager.getElements()) {
                if (el.isEnabled() && el instanceof DynamicIslandHud island && island.mouseClicked(mouseX, mouseY, event.button())) {
                    return true;
                }
            }
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Handle Settings Modal Clicks if Open
            if (this.settingsModalOpen) {
                float modalW = 340.0F;
                float modalH = (this.activeEditingColorSetting != null) ? 175.0F : 208.0F;
                float modalX = (screenW - modalW) / 2.0F;
                float modalY = (screenH - modalH) / 2.0F;

                // Close Button
                float closeX = modalX + modalW - 22.0F;
                float closeY = modalY + 7.0F;
                if (mouseX >= closeX && mouseX <= closeX + 14.0F && mouseY >= closeY && mouseY <= closeY + 14.0F) {
                    this.settingsModalOpen = false;
                    this.bindingClickGuiKey = false;
                    this.activeEditingColorSetting = null;
                    this.draggingPicker = DragTarget.NONE;
                    return true;
                }

                float fieldY;

                if (activeEditingColorSetting == null) {
                    // ClickGUI Keybind Button
                    float bindBtnX = modalX + 58.0F;
                    float bindBtnW = 60.0F;
                    float bindBtnH = 14.0F;
                    float row1Y = modalY + 29.0F;
                    if (mouseX >= bindBtnX && mouseX <= bindBtnX + bindBtnW && mouseY >= row1Y && mouseY <= row1Y + bindBtnH) {
                        this.bindingClickGuiKey = !this.bindingClickGuiKey;
                        return true;
                    }

                    // Static / Chroma Mode Buttons
                    float segW = 56.0F;
                    float segH = 14.0F;
                    float staticBtnX = modalX + modalW - 14.0F - (segW * 2 + 4.0F);
                    float chromaBtnX = staticBtnX + segW + 4.0F;

                    if (mouseX >= staticBtnX && mouseX <= staticBtnX + segW && mouseY >= row1Y && mouseY <= row1Y + segH) {
                        Theme.setAccentMode("Static");
                        if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                            Client.INSTANCE.configManager.autoSave();
                        }
                        return true;
                    }
                    if (mouseX >= chromaBtnX && mouseX <= chromaBtnX + segW && mouseY >= row1Y && mouseY <= row1Y + segH) {
                        Theme.setAccentMode("Chroma");
                        if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                            Client.INSTANCE.configManager.autoSave();
                        }
                        return true;
                    }

                    // Target Color Tabs (Primary vs Secondary)
                    float row2Y = modalY + 47.0F;
                    float tabW = (modalW - 32.0F) / 2.0F;
                    float tabH = 16.0F;
                    float tab1X = modalX + 14.0F;
                    float tab2X = tab1X + tabW + 4.0F;

                    if (mouseX >= tab1X && mouseX <= tab1X + tabW && mouseY >= row2Y && mouseY <= row2Y + tabH) {
                        setEditingSecondary(false);
                        return true;
                    }
                    if (mouseX >= tab2X && mouseX <= tab2X + tabW && mouseY >= row2Y && mouseY <= row2Y + tabH) {
                        setEditingSecondary(true);
                        return true;
                    }

                    fieldY = modalY + 68.0F;
                } else {
                    fieldY = modalY + 32.0F;
                }

                // 2D Saturation / Brightness Field
                float fieldX = modalX + 14.0F;
                float fieldW = 145.0F;
                float fieldH = 100.0F;

                if (mouseX >= fieldX && mouseX <= fieldX + fieldW && mouseY >= fieldY && mouseY <= fieldY + fieldH) {
                    this.draggingPicker = DragTarget.FIELD_2D;
                    updatePickerDrag((int) mouseX, (int) mouseY);
                    return true;
                }

                // Vertical Hue Bar
                float hueX = fieldX + fieldW + 10.0F;
                float hueY = fieldY;
                float hueW = 14.0F;
                float hueH = fieldH;

                if (mouseX >= hueX - 2.0F && mouseX <= hueX + hueW + 2.0F && mouseY >= hueY && mouseY <= hueY + hueH) {
                    this.draggingPicker = DragTarget.HUE_VERT;
                    updatePickerDrag((int) mouseX, (int) mouseY);
                    return true;
                }

                // Presets
                float rightX = hueX + hueW + 12.0F;
                int[] presets = new int[]{
                        ColorUtil.rgb(0, 180, 255),    // Ice Blue
                        ColorUtil.rgb(255, 255, 255),  // Obsidian White
                        ColorUtil.rgb(168, 85, 247),   // Neon Purple
                        ColorUtil.rgb(255, 55, 75),    // Christmas Red
                        ColorUtil.rgb(16, 185, 129),   // Emerald Green
                        ColorUtil.rgb(251, 191, 36),   // Gold
                        ColorUtil.rgb(251, 113, 133),  // Coral Pink
                        ColorUtil.rgb(59, 130, 246)    // Sapphire
                };
                float pSize = 14.0F;
                float pGap = 6.0F;

                for (int i = 0; i < presets.length; i++) {
                    float px = rightX + (i % 4) * (pSize + pGap);
                    float py = fieldY + 45.0F + (i / 4) * 21.0F;
                    if (mouseX >= px && mouseX <= px + pSize && mouseY >= py && mouseY <= py + pSize) {
                        float[] hsv = ColorUtil.toHsv(presets[i]);
                        this.pickerHue = hsv[0];
                        this.pickerSat = hsv[1];
                        this.pickerBri = hsv[2];
                        applyPickerColor();
                        return true;
                    }
                }

                // If click is anywhere inside modal window, consume it
                if (mouseX >= modalX && mouseX <= modalX + modalW && mouseY >= modalY && mouseY <= modalY + modalH) {
                    return true;
                }

                // If clicked outside modal window, close modal
                this.settingsModalOpen = false;
                this.bindingClickGuiKey = false;
                this.activeEditingColorSetting = null;
                this.draggingPicker = DragTarget.NONE;
                return true;
            }

            // Gear Settings Button (Top-Left corner)
            float gearX = x + 9.0F;
            float gearY = y + 10.0F;
            float gearSize = 18.0F;
            if (mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize) {
                openSettingsModal();
                return true;
            }

            // Sidebar Category Clicks (Themes tab filtered out)
            Category[] categories = Arrays.stream(Category.values()).filter(c -> c != Category.THEMES).toArray(Category[]::new);
            float catY = y + 38.0F;
            float catH = 21.0F;
            float catW = SIDEBAR_W - 16.0F;
            float catX = x + 8.0F;

            for (Category cat : categories) {
                if (mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH) {
                    this.activeCategory = cat;
                    this.searchQuery = "";
                    this.searchFocused = false;
                    return true;
                }
                catY += catH + 2.0F;
            }

            // Search Bar Click
            float searchW = 135.0F;
            float searchH = 19.0F;
            float searchX = x + WINDOW_W - 12.0F - searchW - 35.0F;
            float searchY = y + 14.0F;

            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Modules Clicks
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 12.0F;
            float gridY = y + 44.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 24.0F;
            float cardW = (gridW - 10.0F) / 2.0F;
            float startY = gridY - scrollAnim.getValue();

            float leftY = startY;
            float rightY = startY;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);

                float cardX = isRightColumn ? gridX + cardW + 10.0F : gridX;
                float currentY = isRightColumn ? rightY : leftY;

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                float eVal = expandAnim.getValue();
                float cardH = 34.0F + (eVal * calculateSettingsHeight(module));

                // Click module card header to toggle
                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 34.0F) {
                    module.toggle();
                    return true;
                }

                // Click inside settings
                if (eVal > 0.01F && mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY + 33.0F && mouseY <= currentY + cardH) {
                    float setY = currentY + 33.0F;
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
                            } else if (setting instanceof ModeSetting ms) {
                                ms.cycle();
                            } else if (setting instanceof ColorSetting cs) {
                                openColorSettingModal(cs);
                            }
                            if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                                Client.INSTANCE.configManager.autoSave();
                            }
                            return true;
                        }
                        setY += sH;
                    }
                }

                if (isRightColumn) {
                    rightY += cardH + 7.0F;
                } else {
                    leftY += cardH + 7.0F;
                }
            }
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (this.settingsModalOpen) return true;

            // Right-click expands settings
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 12.0F;
            float gridY = y + 44.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 24.0F;
            float cardW = (gridW - 10.0F) / 2.0F;
            float startY = gridY - scrollAnim.getValue();

            float leftY = startY;
            float rightY = startY;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);

                float cardX = isRightColumn ? gridX + cardW + 10.0F : gridX;
                float currentY = isRightColumn ? rightY : leftY;

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                float eVal = expandAnim.getValue();
                float cardH = 34.0F + (eVal * calculateSettingsHeight(module));

                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 34.0F) {
                    this.expandedModule = (this.expandedModule == module) ? null : module;
                    return true;
                }

                if (isRightColumn) {
                    rightY += cardH + 7.0F;
                } else {
                    leftY += cardH + 7.0F;
                }
            }
        }
        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.settingsModalOpen) return true;
        this.scrollTarget -= (float) (scrollY * 24.0D);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Handle Key Binding for ClickGUI Module
        if (this.bindingClickGuiKey) {
            if (ClickGui.INSTANCE != null) {
                if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                    ClickGui.INSTANCE.getBind().clear();
                } else {
                    ClickGui.INSTANCE.getBind().setSingle(event.key());
                }
                if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    Client.INSTANCE.configManager.autoSave();
                }
            }
            this.bindingClickGuiKey = false;
            return true;
        }

        // Close modal on Escape
        if (this.settingsModalOpen) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.settingsModalOpen = false;
                this.draggingPicker = DragTarget.NONE;
                return true;
            }
        }

        // Handle In-Module Key Binding
        if (this.activeBindingSetting instanceof BindSetting b) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                b.setValue(List.of());
            } else {
                b.setValue(List.of(event.key()));
            }
            this.activeBindingSetting = null;
            return true;
        }

        // Handle Search Bar Input
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

        // Check if pressed key is the bound ClickGUI key
        boolean isClickGuiKey = false;
        if (ClickGui.INSTANCE != null && !ClickGui.INSTANCE.getBind().isEmpty()) {
            isClickGuiKey = ClickGui.INSTANCE.getBind().matches(event.key());
        } else {
            isClickGuiKey = (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT);
        }

        if (isClickGuiKey) {
            if (System.currentTimeMillis() - this.openTime < 300L) {
                return true;
            }
            if (this.minecraft != null) {
                this.minecraft.setScreenAndShow(null);
            }
            return true;
        }

        // Fallback ESC to close ClickGUI
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

    private static class Snowflake2D {
        float x, y, speed, seed, size;
        public Snowflake2D(float x, float y, float size, float speed, float seed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.seed = seed;
        }
    }
}
