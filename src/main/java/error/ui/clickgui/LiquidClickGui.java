package error.ui.clickgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.client.clients.Theme;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.head.PlayerHead;
import error.util.display.outline.Outline;
import error.util.display.rounded.RoundedRect;
import error.util.display.shadow.Shadow;
import error.util.math.Animation;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
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

        if (this.draggingSlider != null && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            updateSliderDrag(mouseX);
        } else {
            this.draggingSlider = null;
        }

        int accentArgb = Theme.getAccentColor();
        Color accentColor = Color.of(accentArgb);

        // 1. Subtle World Dimmer (allows world to remain visible and blurred)
        RoundedRect.of(0, 0, screenW, screenH)
                .color(Color.rgba(0, 0, 0, Math.round(75 * animVal)))
                .render(extractor);

        // 2. Winter Snowflakes
        renderSnowflakes(extractor, screenW, screenH, animVal);

        // 3. Main Liquid Glass Window (1:1 from RenderDemo)
        // Soft drop shadow
        Shadow.of(x, y, WINDOW_W, WINDOW_H)
                .radius(12)
                .blur(20)
                .color(Color.rgba(0, 0, 0, Math.round(180 * animVal)))
                .render(extractor);

        // Authentic Kawase Blur with subtle dark tint (NO opaque paint!)
        Blur.of(x, y, WINDOW_W, WINDOW_H)
                .radius(12)
                .type(BlurType.KAWASE)
                .strength(4)
                .tint(Color.rgba(0, 0, 0, Math.round(80 * animVal)))
                .alpha(animVal)
                .render(extractor);

        // Subtle dark glass acrylic wash
        RoundedRect.of(x, y, WINDOW_W, WINDOW_H)
                .radius(12)
                .color(Color.rgba(14, 16, 22, Math.round(140 * animVal)))
                .render(extractor);

        // Vertical gradient white outline (from RenderDemo row 7)
        Outline.of(x, y, WINDOW_W, WINDOW_H)
                .radius(12)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(animVal)
                .render(extractor);

        // 4. Sidebar Separator
        RoundedRect.of(x + SIDEBAR_W, y + 10.0F, 1.0F, WINDOW_H - 20.0F)
                .radius(0.5F)
                .color(Color.rgba(255, 255, 255, Math.round(16 * animVal)))
                .render(extractor);

        // 5. Sidebar and Header
        renderSidebar(extractor, x, y, mouseX, mouseY, animVal, accentColor);
        renderHeader(extractor, x + SIDEBAR_W + 12.0F, y + 14.0F, WINDOW_W - SIDEBAR_W - 24.0F, mouseX, mouseY, animVal, accentColor);

        // 6. Content Section
        float contentX = x + SIDEBAR_W + 12.0F;
        float contentY = y + 44.0F;
        float contentW = WINDOW_W - SIDEBAR_W - 24.0F;
        float contentH = WINDOW_H - 54.0F;

        if (activeCategory == Category.THEMES) {
            renderThemesTab(extractor, contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
        } else if (activeCategory == Category.CONFIGS) {
            renderConfigsTab(extractor, contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
        } else if (activeCategory == Category.FRIENDS) {
            renderFriendsTab(extractor, contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
        } else if (activeCategory == Category.EVENTS || activeCategory == Category.COSMETICS) {
            renderEventsTab(extractor, contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
        } else {
            renderModulesGrid(extractor, contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
        }
    }

    private void renderSnowflakes(GuiGraphicsExtractor extractor, int screenW, int screenH, float alphaVal) {
        long time = System.currentTimeMillis();
        for (Snowflake2D sf : snowflakes) {
            sf.y += sf.speed * 0.035F;
            sf.x += (float) Math.sin((time * 0.002F) + sf.seed) * 0.3F;

            if (sf.y > screenH + 8.0F) {
                sf.y = -8.0F;
                sf.x = random.nextFloat() * screenW;
            }

            RoundedRect.of(sf.x, sf.y, sf.size, sf.size)
                    .radius(sf.size * 0.5F)
                    .color(Color.rgba(220, 240, 255, Math.round(110 * alphaVal)))
                    .render(extractor);
        }
    }

    private void renderSidebar(GuiGraphicsExtractor extractor, float x, float y, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        int accentArgb = accentColor.argb();

        // Branding
        Fonts.drawString(Fonts.ICONS, IconUse.LOGO.getGlyph(), x + 12.0F, y + 13.0F, 9.5F, accentArgb);
        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", x + 28.0F, y + 12.0F, 8.5F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Winter 26.2", x + 28.0F, y + 22.0F, 4.8F, 0xFFA0B0C8);

        // Category List
        Category[] categories = Category.values();
        float catY = y + 38.0F;
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
                // Glow shadow directly from RenderDemo row 7
                Shadow.of(catX, catY, catW, catH)
                        .radius(6)
                        .blur(6)
                        .offset(0, 2)
                        .strength(2.0F)
                        .color(accentColor)
                        .alpha(alphaVal)
                        .render(extractor);

                RoundedRect.of(catX, catY, catW, catH)
                        .radius(6)
                        .color(Color.rgba(28, 32, 44, Math.round(230 * alphaVal)))
                        .render(extractor);

                Outline.of(catX, catY, catW, catH)
                        .radius(6)
                        .thickness(1.0F)
                        .verticalGradient(Color.WHITE, FADE_WHITE)
                        .alpha(alphaVal)
                        .render(extractor);
            } else if (hVal > 0.01F) {
                RoundedRect.of(catX, catY, catW, catH)
                        .radius(6)
                        .color(Color.rgba(255, 255, 255, Math.round(hVal * 16 * alphaVal)))
                        .render(extractor);
            }

            int iconCol = active ? 0xFFFFFFFF : 0xFFB0C0D4;
            Fonts.drawString(Fonts.ICONS, getCategoryIcon(cat), catX + 9.0F, catY + 6.0F, 7.5F, iconCol);

            int nameCol = active ? 0xFFFFFFFF : 0xFFC0D0E0;
            Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 23.0F, catY + 6.5F, 6.2F, nameCol);

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

                RoundedRect.of(badgeX, badgeY, badgeW, 11.5F)
                        .radius(3.0F)
                        .color(active ? accentColor : Color.rgba(255, 255, 255, 20))
                        .alpha(alphaVal)
                        .render(extractor);

                Fonts.drawCenteredString(Fonts.SF_MEDIUM, countStr, badgeX + badgeW / 2.0F, badgeY + 2.5F, 4.8F, 0xFFFFFFFF);
            }

            catY += catH + 2.0F;
        }

        // Bottom User Profile Card with PlayerHead from RenderDemo
        float profileW = SIDEBAR_W - 16.0F;
        float profileH = 26.0F;
        float profileX = x + 8.0F;
        float profileY = y + WINDOW_H - 34.0F;

        RoundedRect.of(profileX, profileY, profileW, profileH)
                .radius(6)
                .color(Color.rgba(255, 255, 255, Math.round(12 * alphaVal)))
                .render(extractor);

        Outline.of(profileX, profileY, profileW, profileH)
                .radius(6)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(alphaVal)
                .render(extractor);

        PlayerHead.of(profileX + 4.0F, profileY + 4.0F, 18.0F)
                .radius(4.0F)
                .alpha(alphaVal)
                .render(extractor);

        String username = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "User";
        Fonts.drawString(Fonts.SF_MEDIUM, username, profileX + 26.0F, profileY + 4.5F, 5.8F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Developer", profileX + 26.0F, profileY + 14.0F, 4.2F, 0xFFA0B4C8);
    }

    private String getCategoryIcon(Category cat) {
        if (cat == Category.COMBAT) return IconUse.FIGHT.getGlyph();
        if (cat == Category.MOVEMENT) return IconUse.MOVEMENT.getGlyph();
        if (cat == Category.RENDER) return IconUse.RENDER.getGlyph();
        if (cat == Category.COSMETICS) return IconUse.POTION.getGlyph();
        if (cat == Category.PLAYER) return IconUse.PLAYER.getGlyph();
        if (cat == Category.MISC) return IconUse.MISC.getGlyph();
        if (cat == Category.THEMES) return IconUse.GLOBE.getGlyph();
        if (cat == Category.EVENTS) return IconUse.SPUTNIK.getGlyph();
        if (cat == Category.CONFIGS) return IconUse.GEAR.getGlyph();
        if (cat == Category.FRIENDS) return IconUse.GROUP.getGlyph();
        return IconUse.GEAR.getGlyph();
    }

    private void renderHeader(GuiGraphicsExtractor extractor, float x, float y, float w, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";
        Fonts.drawString(Fonts.SF_MEDIUM, titleText, x, y + 2.0F, 9.5F, 0xFFFFFFFF);

        // Search Bar (Pill)
        float searchW = 135.0F;
        float searchH = 19.0F;
        float searchX = x + w - searchW;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y && mouseY <= y + searchH;

        RoundedRect.of(searchX, y, searchW, searchH)
                .radius(5)
                .color(Color.rgba(255, 255, 255, Math.round((searchFocused ? 18 : (isHovered ? 12 : 8)) * alphaVal)))
                .render(extractor);

        Outline.of(searchX, y, searchW, searchH)
                .radius(5)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(alphaVal)
                .render(extractor);

        Fonts.drawString(Fonts.ICONS, IconUse.SEARCH.getGlyph(), searchX + 6.5F, y + 5.0F, 7.0F, 0xFFB0C0D4);

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        int textCol = searchQuery.isEmpty() && !searchFocused ? 0xFF8898B0 : 0xFFFFFFFF;
        Fonts.drawString(Fonts.SF_MEDIUM, displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), searchX + 20.0F, y + 5.5F, 5.8F, textCol);
    }

    private void renderModulesGrid(GuiGraphicsExtractor extractor, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        List<Module> modules = getFilteredModules();

        if (modules.isEmpty()) {
            RoundedRect.of(x, y, w, 80.0F)
                    .radius(8)
                    .color(Color.rgba(255, 255, 255, Math.round(8 * alphaVal)))
                    .render(extractor);

            Outline.of(x, y, w, 80.0F)
                    .radius(8)
                    .thickness(1.0F)
                    .verticalGradient(Color.WHITE, FADE_WHITE)
                    .alpha(alphaVal)
                    .render(extractor);

            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "В этой категории нет модулей", x + w / 2.0F, y + 30.0F, 7.0F, 0xFFC0D0E0);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Выберите другую категорию в меню слева", x + w / 2.0F, y + 46.0F, 5.2F, 0xFF8898B0);
            return;
        }

        float cardW = (w - 10.0F) / 2.0F;
        float startY = y - scrollAnim.getValue();

        float leftY = startY;
        float rightY = startY;

        extractor.enableScissor(Math.round(x - 2.0F), Math.round(y), Math.round(x + w + 2.0F), Math.round(y + h));
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
                    renderModuleCard(extractor, module, cardX, currentY, cardW, cardH, eVal, mouseX, mouseY, alphaVal, accentColor);
                }

                if (isRightColumn) {
                    rightY += cardH + 7.0F;
                } else {
                    leftY += cardH + 7.0F;
                }
            }
        } finally {
            extractor.disableScissor();
        }

        float contentH = Math.max(leftY, rightY) - startY;
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0F, Math.max(0.0F, contentH - h));
        this.scrollAnim.setTarget(this.scrollTarget);
    }

    private void renderModuleCard(GuiGraphicsExtractor extractor, Module module, float x, float y, float w, float h, float expandVal, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 34.0F;

        Animation toggleAnim = moduleToggleAnims.computeIfAbsent(module, k -> new Animation(module.isEnabled() ? 1.0F : 0.0F, 0.18F));
        toggleAnim.setTarget(module.isEnabled() ? 1.0F : 0.0F);
        toggleAnim.update();
        float tVal = toggleAnim.getValue();

        int accentArgb = accentColor.argb();

        // 1. Shadow: EXACTLY from RenderDemo row 7:
        // Shadow.of(x, y, WIDTH, HEIGHT).radius(8).blur(6).offset(0, 3).strength(2.0F).color(MINT).render(graphics);
        if (module.isEnabled()) {
            Shadow.of(x, y, w, h)
                    .radius(8)
                    .blur(6)
                    .offset(0, 2)
                    .strength(2.0F)
                    .color(accentColor)
                    .alpha(alphaVal)
                    .render(extractor);

            RoundedRect.of(x, y, w, h)
                    .radius(8)
                    .color(Color.rgba(26, 28, 38, Math.round(220 * alphaVal)))
                    .render(extractor);

            Outline.of(x, y, w, h)
                    .radius(8)
                    .thickness(1.0F)
                    .verticalGradient(Color.WHITE, FADE_WHITE)
                    .alpha(alphaVal)
                    .render(extractor);
        } else {
            Shadow.of(x, y, w, h)
                    .radius(8)
                    .blur(6)
                    .color(Color.rgba(0, 0, 0, Math.round(80 * alphaVal)))
                    .render(extractor);

            RoundedRect.of(x, y, w, h)
                    .radius(8)
                    .color(Color.rgba(18, 20, 26, Math.round((isHovered ? 160 : 120) * alphaVal)))
                    .render(extractor);

            Outline.of(x, y, w, h)
                    .radius(8)
                    .thickness(1.0F)
                    .color(Color.rgba(255, 255, 255, Math.round((isHovered ? 30 : 16) * alphaVal)))
                    .render(extractor);
        }

        // Status Indicator Dot
        int dotCol = module.isEnabled() ? accentArgb : 0xFF708098;
        RoundedRect.of(x + 9.0F, y + 11.5F, 5.0F, 5.0F)
                .radius(2.5F)
                .color(Color.of(dotCol))
                .alpha(alphaVal)
                .render(extractor);

        // Module Name
        int titleCol = module.isEnabled() ? 0xFFFFFFFF : 0xFFC0D0E0;
        Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), x + 18.0F, y + 6.5F, 6.8F, titleCol);

        // Module Description
        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, desc, x + 18.0F, y + 18.0F, 4.6F, 0xFF8898B0);
        }

        // Toggle Switch Widget
        float switchW = 24.0F;
        float switchH = 12.0F;
        float switchX = x + w - switchW - 8.0F;
        float switchY = y + 11.0F;

        Color trackCol = tVal > 0.01F ? accentColor : Color.rgba(255, 255, 255, 30);
        RoundedRect.of(switchX, switchY, switchW, switchH)
                .radius(6)
                .color(trackCol)
                .alpha(alphaVal)
                .render(extractor);

        // Knob
        float knobSize = 8.0F;
        float knobX = switchX + 2.0F + (switchW - knobSize - 4.0F) * tVal;
        float knobY = switchY + 2.0F;
        RoundedRect.of(knobX, knobY, knobSize, knobSize)
                .radius(4)
                .color(Color.WHITE)
                .alpha(alphaVal)
                .render(extractor);

        // Settings section
        if (expandVal > 0.01F) {
            float setY = y + 33.0F;
            List<Setting<?>> settings = module.getSettings();
            for (Setting<?> setting : settings) {
                renderSettingRow(extractor, setting, x + 9.0F, setY, w - 18.0F, mouseX, mouseY, expandVal * alphaVal, accentColor);
                setY += getSettingHeight(setting);
            }
        }
    }

    private void renderSettingRow(GuiGraphicsExtractor extractor, Setting<?> setting, float x, float y, float w, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        if (setting instanceof CheckBox cb) {
            Fonts.drawString(Fonts.SF_MEDIUM, cb.getName(), x, y + 2.0F, 5.6F, 0xFFE0E8F5);

            float boxSize = 10.5F;
            float boxX = x + w - boxSize;
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= y && mouseY <= y + boxSize;

            RoundedRect.of(boxX, y, boxSize, boxSize)
                    .radius(3)
                    .color(cb.getValue() ? accentColor : Color.rgba(255, 255, 255, isHovered ? 40 : 20))
                    .alpha(alphaVal)
                    .render(extractor);

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
            RoundedRect.of(x, barY, w, barH)
                    .radius(1.8F)
                    .color(Color.rgba(255, 255, 255, 25))
                    .alpha(alphaVal)
                    .render(extractor);

            float fillW = Math.max(3.5F, w * pct);
            RoundedRect.of(x, barY, fillW, barH)
                    .radius(1.8F)
                    .color(accentColor)
                    .alpha(alphaVal)
                    .render(extractor);

            float handleX = x + fillW - 2.0F;
            RoundedRect.of(handleX, barY + 0.2F, 3.5F, 3.5F)
                    .radius(1.75F)
                    .color(Color.WHITE)
                    .alpha(alphaVal)
                    .render(extractor);
        } else if (setting instanceof BindSetting b) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Назначение клавиши", x, y + 2.0F, 5.6F, 0xFFE0E8F5);

            String bindText = (activeBindingSetting == b) ? "[...]" : b.getDisplayValue();
            float btnW = 46.0F;
            float btnH = 12.0F;
            float btnX = x + w - btnW;

            RoundedRect.of(btnX, y, btnW, btnH)
                    .radius(3)
                    .color(Color.rgba(255, 255, 255, 24))
                    .alpha(alphaVal)
                    .render(extractor);

            Fonts.drawCenteredString(Fonts.SF_MEDIUM, bindText, btnX + btnW / 2.0F, y + 2.5F, 5.0F, 0xFFFFFFFF);
        }
    }

    // ===================== THEMES TAB =====================

    private void renderThemesTab(GuiGraphicsExtractor extractor, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Выберите цветовую палитру интерфейса", x, y, 6.8F, 0xFFB0C0D4);

        float cardW = (w - 10.0F) / 2.0F;
        float cardH = 56.0F;

        structThemeOption[] options = new structThemeOption[]{
                new structThemeOption("WINTER_GLASS", "Зимнее Жидкое Стекло", "Ледяной циан, снег и сияние стекла", Color.rgb(0, 180, 255), Color.rgb(120, 200, 255), "Static"),
                new structThemeOption("OBSIDIAN_BLACK", "Черный Обсидиан", "Глубокий матовый черный, строгий минимализм", Color.rgb(255, 255, 255), Color.rgb(180, 190, 205), "Static"),
                new structThemeOption("CHRISTMAS", "Новогодний Карнавал", "Праздничный алый бархат и еловый изумруд", Color.rgb(255, 55, 75), Color.rgb(40, 210, 120), "Static"),
                new structThemeOption("NEON_CYBER", "Неоновый Киберпанк", "Динамический радужный перелив Chroma", Color.rgb(168, 85, 247), Color.rgb(6, 182, 212), "Chroma"),
                new structThemeOption("RETRO_UI", "RetroUI 90s", "Ретро-интерфейс, контрастные рамки", Color.rgb(16, 185, 129), Color.rgb(59, 130, 246), "Static")
        };

        String currentStyle = Theme.getUiStyle();
        if (currentStyle == null) currentStyle = "WINTER_GLASS";

        for (int i = 0; i < options.length; i++) {
            structThemeOption opt = options[i];
            float cx = (i % 2 == 0) ? x : x + cardW + 10.0F;
            float cy = y + 15.0F + (i / 2) * (cardH + 8.0F);

            boolean isHovered = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;
            boolean isActive = currentStyle.equalsIgnoreCase(opt.styleId);

            if (isActive) {
                // RenderDemo Row 7 shadow glow:
                Shadow.of(cx, cy, cardW, cardH)
                        .radius(8)
                        .blur(6)
                        .offset(0, 2)
                        .strength(2.0F)
                        .color(opt.accent)
                        .alpha(alphaVal)
                        .render(extractor);

                RoundedRect.of(cx, cy, cardW, cardH)
                        .radius(8)
                        .color(Color.rgba(26, 28, 38, Math.round(225 * alphaVal)))
                        .render(extractor);

                Outline.of(cx, cy, cardW, cardH)
                        .radius(8)
                        .thickness(1.0F)
                        .verticalGradient(Color.WHITE, FADE_WHITE)
                        .alpha(alphaVal)
                        .render(extractor);
            } else {
                Shadow.of(cx, cy, cardW, cardH)
                        .radius(8)
                        .blur(5)
                        .color(Color.rgba(0, 0, 0, Math.round(70 * alphaVal)))
                        .render(extractor);

                RoundedRect.of(cx, cy, cardW, cardH)
                        .radius(8)
                        .color(Color.rgba(18, 20, 26, Math.round((isHovered ? 160 : 120) * alphaVal)))
                        .render(extractor);

                Outline.of(cx, cy, cardW, cardH)
                        .radius(8)
                        .thickness(1.0F)
                        .color(Color.rgba(255, 255, 255, Math.round((isHovered ? 30 : 16) * alphaVal)))
                        .render(extractor);
            }

            // Orbs
            RoundedRect.of(cx + 12.0F, cy + 14.0F, 8.0F, 8.0F)
                    .radius(4)
                    .color(opt.accent)
                    .render(extractor);

            RoundedRect.of(cx + 22.0F, cy + 14.0F, 8.0F, 8.0F)
                    .radius(4)
                    .color(opt.secondary)
                    .render(extractor);

            Fonts.drawString(Fonts.SF_MEDIUM, opt.title, cx + 36.0F, cy + 11.5F, 7.0F, 0xFFFFFFFF);
            Fonts.drawString(Fonts.SF_MEDIUM, opt.desc, cx + 36.0F, cy + 24.0F, 4.5F, 0xFFA0B0C4);

            if (isActive) {
                RoundedRect.of(cx + cardW - 50.0F, cy + 35.0F, 42.0F, 13.0F)
                        .radius(3)
                        .color(opt.accent)
                        .alpha(alphaVal)
                        .render(extractor);
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Активно", cx + cardW - 29.0F, cy + 38.5F, 4.8F, 0xFFFFFFFF);
            }
        }
    }

    private void renderConfigsTab(GuiGraphicsExtractor extractor, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        RoundedRect.of(x, y, w, h)
                .radius(8)
                .color(Color.rgba(255, 255, 255, Math.round(8 * alphaVal)))
                .render(extractor);

        Outline.of(x, y, w, h)
                .radius(8)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(alphaVal)
                .render(extractor);

        Fonts.drawString(Fonts.SF_MEDIUM, "Менеджер Конфигураций", x + 14.0F, y + 14.0F, 9.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Сохраняйте и загружайте настройки клиента", x + 14.0F, y + 26.0F, 5.5F, 0xFFA0B0C4);

        String[] presets = new String[]{"default", "legit_funtime", "rage_hvh", "skywars_boost"};
        float itemY = y + 46.0F;
        for (String preset : presets) {
            RoundedRect.of(x + 14.0F, itemY, w - 28.0F, 24.0F)
                    .radius(6)
                    .color(Color.rgba(255, 255, 255, Math.round(12 * alphaVal)))
                    .render(extractor);

            Outline.of(x + 14.0F, itemY, w - 28.0F, 24.0F)
                    .radius(6)
                    .thickness(1.0F)
                    .verticalGradient(Color.WHITE, FADE_WHITE)
                    .alpha(alphaVal)
                    .render(extractor);

            Fonts.drawString(Fonts.SF_MEDIUM, preset + ".json", x + 24.0F, itemY + 8.0F, 6.5F, 0xFFFFFFFF);

            float loadBtnX = x + w - 85.0F;
            RoundedRect.of(loadBtnX, itemY + 4.0F, 60.0F, 16.0F)
                    .radius(4)
                    .color(accentColor)
                    .alpha(alphaVal)
                    .render(extractor);

            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + 30.0F, itemY + 7.5F, 5.2F, 0xFFFFFFFF);

            itemY += 28.0F;
        }
    }

    private void renderFriendsTab(GuiGraphicsExtractor extractor, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        RoundedRect.of(x, y, w, h)
                .radius(8)
                .color(Color.rgba(255, 255, 255, Math.round(8 * alphaVal)))
                .render(extractor);

        Outline.of(x, y, w, h)
                .radius(8)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(alphaVal)
                .render(extractor);

        Fonts.drawString(Fonts.SF_MEDIUM, "Список Друзей", x + 14.0F, y + 14.0F, 9.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Друзья автоматически игнорируются аимом и киллаурой", x + 14.0F, y + 26.0F, 5.5F, 0xFFA0B0C4);

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Список друзей пуст. Кликните ПКМ по игроку в игре чтобы добавить!", x + w / 2.0F, y + h / 2.0F, 6.5F, 0xFFA0B0C4);
    }

    private void renderEventsTab(GuiGraphicsExtractor extractor, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, Color accentColor) {
        RoundedRect.of(x, y, w, h)
                .radius(8)
                .color(Color.rgba(255, 255, 255, Math.round(8 * alphaVal)))
                .render(extractor);

        Outline.of(x, y, w, h)
                .radius(8)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, FADE_WHITE)
                .alpha(alphaVal)
                .render(extractor);

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Новогодний Ивент Error DLC 2026", x + w / 2.0F, y + h / 2.0F - 10.0F, 9.5F, accentColor.argb());
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Эксклюзивные праздничные эффекты и косметика будут доступны в обновлении!", x + w / 2.0F, y + h / 2.0F + 8.0F, 5.8F, 0xFFA0B0C4);
    }

    private record structThemeOption(String styleId, String title, String desc, Color accent, Color secondary, String mode) {}

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
        int screenW = this.width;
        int screenH = this.height;

        float x = (screenW - WINDOW_W) / 2.0F;
        float y = (screenH - WINDOW_H) / 2.0F;

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            // Sidebar Category Clicks
            Category[] categories = Category.values();
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
            float searchX = x + WINDOW_W - 12.0F - searchW;
            float searchY = y + 14.0F;

            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Click Themes Tab Cards
            if (activeCategory == Category.THEMES) {
                float gridX = x + SIDEBAR_W + 12.0F;
                float gridY = y + 44.0F;
                float gridW = WINDOW_W - SIDEBAR_W - 24.0F;
                float cardW = (gridW - 10.0F) / 2.0F;
                float cardH = 56.0F;

                structThemeOption[] options = new structThemeOption[]{
                        new structThemeOption("WINTER_GLASS", "", "", Color.rgb(0, 180, 255), Color.rgb(120, 200, 255), "Static"),
                        new structThemeOption("OBSIDIAN_BLACK", "", "", Color.rgb(255, 255, 255), Color.rgb(180, 190, 205), "Static"),
                        new structThemeOption("CHRISTMAS", "", "", Color.rgb(255, 55, 75), Color.rgb(40, 210, 120), "Static"),
                        new structThemeOption("NEON_CYBER", "", "", Color.rgb(168, 85, 247), Color.rgb(6, 182, 212), "Chroma"),
                        new structThemeOption("RETRO_UI", "", "", Color.rgb(16, 185, 129), Color.rgb(59, 130, 246), "Static")
                };

                for (int i = 0; i < options.length; i++) {
                    float cx = (i % 2 == 0) ? gridX : gridX + cardW + 10.0F;
                    float cy = gridY + 15.0F + (i / 2) * (cardH + 8.0F);

                    if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                        Theme.setUiStyle(options[i].styleId);
                        Theme.setAccentMode(options[i].mode);
                        if (options[i].mode.equalsIgnoreCase("Static")) {
                            Theme.setAccentColor(options[i].accent.argb());
                            Theme.setSecondaryColor(options[i].secondary.argb());
                        }
                        return true;
                    }
                }
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
        this.scrollTarget -= (float) (scrollY * 24.0D);
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
