package error.ui.clickgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.display.blur.Blur;
import error.util.display.color.Color;
import error.util.display.color.Gradient;
import error.util.display.head.PlayerHead;
import error.util.display.outline.Outline;
import error.util.display.rounded.RoundedRect;
import error.util.display.shadow.Shadow;
import error.util.display.text.Text;
import error.util.display.text.TextAlign;
import error.util.display.text.font.Fonts;
import error.util.math.Animation;
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

public class LiquidClickGui extends Screen {

    public static final float WINDOW_W = 540.0F;
    public static final float WINDOW_H = 350.0F;
    public static final float SIDEBAR_W = 145.0F;

    public Category activeCategory = Category.COMBAT;
    public String searchQuery = "";
    public boolean searchFocused = false;

    private final Animation openAnim = new Animation(0.0F, 0.18F);
    private final Animation scrollAnim = new Animation(0.0F, 0.20F);
    private float scrollTarget = 0.0F;

    private final Map<Module, Animation> moduleExpandAnims = new HashMap<>();
    private final Map<Module, Animation> moduleToggleAnims = new HashMap<>();
    private final Map<Category, Animation> categoryHoverAnims = new HashMap<>();

    public Module expandedModule = null;
    public Setting<?> activeBindingSetting = null;

    public LiquidClickGui() {
        super(Component.literal("ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        this.openAnim.setValue(1.0F);
        this.openAnim.setTarget(1.0F);
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

        float animVal = openAnim.getValue();
        if (animVal <= 0.01F) return;

        float x = (screenW - WINDOW_W) / 2.0F;
        float y = (screenH - WINDOW_H) / 2.0F;

        RenderExtend.enter2D(null, extractor, null);
        try {
            // 1. Fullscreen Kawase Blur Pass on game background (fast 1-step pass)
            Blur.of(0, 0, screenW, screenH)
                    .radius(8)
                    .strength(1)
                    .render(extractor);

            // Background dark veil tint
            int veilAlpha = (int) (130 * animVal);
            extractor.fill(0, 0, screenW, screenH, ColorUtil.rgba(4, 6, 10, veilAlpha));

            // Apply scale animation around center
            extractor.pose().pushMatrix();
            float centerX = screenW / 2.0F;
            float centerY = screenH / 2.0F;
            float scale = 0.88F + 0.12F * animVal;
            extractor.pose().translate(centerX, centerY);
            extractor.pose().scale(scale, scale);
            extractor.pose().translate(-centerX, -centerY);

            // 2. Main Window Container Shadow, Glass Rect & Glowing Outline
            Color glassBg = Color.rgba(14, 16, 24, (int) (215 * animVal));
            Color borderGlow = Color.rgba(255, 255, 255, (int) (35 * animVal));
            Color shadowCol = Color.rgba(0, 0, 0, (int) (190 * animVal));

            Shadow.of(x, y, WINDOW_W, WINDOW_H)
                    .radius(14)
                    .blur(14)
                    .spread(2)
                    .color(shadowCol)
                    .render(extractor);

            RoundedRect.of(x, y, WINDOW_W, WINDOW_H)
                    .radius(14)
                    .color(glassBg)
                    .render(extractor);

            Outline.of(x, y, WINDOW_W, WINDOW_H)
                    .radius(14)
                    .thickness(0.8F)
                    .color(borderGlow)
                    .render(extractor);

            // 3. Sidebar Divider & Sidebar UI
            Color dividerColor = Color.rgba(255, 255, 255, (int) (18 * animVal));
            RoundedRect.of(x + SIDEBAR_W, y + 12, 1.0F, WINDOW_H - 24)
                    .radius(0.5F)
                    .color(dividerColor)
                    .render(extractor);

            renderSidebar(extractor, x, y, mouseX, mouseY, animVal);

            // 4. Header Search Bar & Content Area
            renderHeader(extractor, x + SIDEBAR_W + 16.0F, y + 14.0F, WINDOW_W - SIDEBAR_W - 32.0F, mouseX, mouseY, animVal);
            renderModulesGrid(extractor, x + SIDEBAR_W + 16.0F, y + 50.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 64.0F, mouseX, mouseY, animVal);

            extractor.pose().popMatrix();
        } finally {
            error.util.display.DisplayUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderSidebar(GuiGraphicsExtractor graphics, float x, float y, int mouseX, int mouseY, float alphaVal) {
        // Logo & Title Header
        Color whiteCol = Color.rgba(255, 255, 255, (int) (250 * alphaVal));
        Color subCol = Color.rgba(160, 175, 200, (int) (180 * alphaVal));

        Text.of("Error DLC")
                .font(Fonts.MANROPE_MEDIUM)
                .size(11.0F)
                .color(whiteCol)
                .position(x + 16.0F, y + 16.0F)
                .render(graphics);

        Text.of("Liquid Glass v26.2")
                .font(Fonts.MEDIUM)
                .size(5.5F)
                .color(subCol)
                .position(x + 16.0F, y + 30.0F)
                .render(graphics);

        // Categories Navigation List
        Category[] categories = Category.values();
        float catY = y + 56.0F;
        float catH = 28.0F;
        float catW = SIDEBAR_W - 24.0F;
        float catX = x + 12.0F;

        for (Category cat : categories) {
            boolean active = (cat == this.activeCategory && searchQuery.isEmpty());
            boolean isHovered = mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH;

            Animation hoverAnim = categoryHoverAnims.computeIfAbsent(cat, k -> new Animation(0.0F, 0.16F));
            hoverAnim.setTarget(active ? 1.0F : (isHovered ? 0.45F : 0.0F));
            hoverAnim.update();
            float hVal = hoverAnim.getValue();

            if (hVal > 0.01F) {
                Color pillBg = Color.rgba(255, 255, 255, (int) ((0.06F + hVal * 0.10F) * 255 * alphaVal));
                Color pillOutline = Color.rgba(255, 255, 255, (int) (hVal * 0.20F * 255 * alphaVal));

                RoundedRect.of(catX, catY, catW, catH)
                        .radius(7)
                        .color(pillBg)
                        .render(graphics);

                if (active) {
                    Outline.of(catX, catY, catW, catH)
                            .radius(7)
                            .thickness(0.7F)
                            .color(pillOutline)
                            .render(graphics);
                }
            }

            // Count enabled modules in category
            long enabledCount = Client.getInstance().moduleManager.getModules().stream()
                    .filter(m -> m.getCategory() == cat && m.isEnabled())
                    .count();

            Color nameCol = active
                    ? Color.rgba(255, 255, 255, (int) (255 * alphaVal))
                    : Color.rgba(175, 185, 205, (int) ((0.75F + hVal * 0.25F) * 255 * alphaVal));

            Text.of(cat.getDisplayName())
                    .font(Fonts.MANROPE_MEDIUM)
                    .size(7.5F)
                    .color(nameCol)
                    .position(catX + 12.0F, catY + 9.5F)
                    .render(graphics);

            if (enabledCount > 0) {
                Color countCol = Color.rgba(255, 255, 255, (int) (140 * alphaVal));
                Text.of(String.valueOf(enabledCount))
                        .font(Fonts.MEDIUM)
                        .size(6.0F)
                        .color(countCol)
                        .align(TextAlign.RIGHT)
                        .position(catX + catW - 12.0F, catY + 10.5F)
                        .render(graphics);
            }

            catY += catH + 4.0F;
        }

        // Bottom User Profile Card
        float profileY = y + WINDOW_H - 42.0F;
        float profileW = SIDEBAR_W - 24.0F;
        float profileX = x + 12.0F;

        Color cardBg = Color.rgba(255, 255, 255, (int) (12 * alphaVal));
        Color cardBorder = Color.rgba(255, 255, 255, (int) (22 * alphaVal));

        RoundedRect.of(profileX, profileY, profileW, 30.0F)
                .radius(8)
                .color(cardBg)
                .render(graphics);

        Outline.of(profileX, profileY, profileW, 30.0F)
                .radius(8)
                .thickness(0.7F)
                .color(cardBorder)
                .render(graphics);

        PlayerHead.of(profileX + 5.0F, profileY + 5.0F, 20.0F)
                .self()
                .radius(5)
                .render(graphics);

        String username = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "User";
        Text.of(username)
                .font(Fonts.MANROPE_MEDIUM)
                .size(7.0F)
                .color(Color.rgba(255, 255, 255, (int) (245 * alphaVal)))
                .position(profileX + 30.0F, profileY + 6.0F)
                .render(graphics);

        Text.of("Developer")
                .font(Fonts.MEDIUM)
                .size(5.5F)
                .color(Color.rgba(140, 160, 190, (int) (180 * alphaVal)))
                .position(profileX + 30.0F, profileY + 17.0F)
                .render(graphics);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";

        Text.of(titleText)
                .font(Fonts.MANROPE_MEDIUM)
                .size(11.0F)
                .color(Color.rgba(255, 255, 255, (int) (250 * alphaVal)))
                .position(x, y + 2.0F)
                .render(graphics);

        // Search Bar Box
        float searchW = 130.0F;
        float searchH = 22.0F;
        float searchX = x + w - searchW;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y && mouseY <= y + searchH;
        Color boxBg = Color.rgba(255, 255, 255, (int) ((searchFocused ? 0.12F : (isHovered ? 0.08F : 0.05F)) * 255 * alphaVal));
        Color boxBorder = Color.rgba(255, 255, 255, (int) ((searchFocused ? 0.35F : 0.15F) * 255 * alphaVal));

        RoundedRect.of(searchX, y, searchW, searchH)
                .radius(6)
                .color(boxBg)
                .render(graphics);

        Outline.of(searchX, y, searchW, searchH)
                .radius(6)
                .thickness(0.7F)
                .color(boxBorder)
                .render(graphics);

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        Color textCol = searchQuery.isEmpty() && !searchFocused
                ? Color.rgba(140, 150, 170, (int) (180 * alphaVal))
                : Color.rgba(255, 255, 255, (int) (245 * alphaVal));

        Text.of(displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""))
                .font(Fonts.MEDIUM)
                .size(6.5F)
                .color(textCol)
                .position(searchX + 8.0F, y + 7.0F)
                .render(graphics);
    }

    private void renderModulesGrid(GuiGraphicsExtractor graphics, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        List<Module> modules = getFilteredModules();

        float cardW = (w - 12.0F) / 2.0F;
        float startY = y - scrollAnim.getValue();

        float leftY = startY;
        float rightY = startY;

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            boolean isRightColumn = (i % 2 != 0);

            float cardX = isRightColumn ? x + cardW + 12.0F : x;
            float currentY = isRightColumn ? rightY : leftY;

            Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
            boolean isExpanded = (expandedModule == module);
            expandAnim.setTarget(isExpanded ? 1.0F : 0.0F);
            expandAnim.update();
            float eVal = expandAnim.getValue();

            float cardH = 38.0F + (eVal * calculateSettingsHeight(module));

            // Scissor clip for grid area
            if (currentY + cardH >= y && currentY <= y + h) {
                renderModuleCard(graphics, module, cardX, currentY, cardW, cardH, eVal, mouseX, mouseY, alphaVal);
            }

            if (isRightColumn) {
                rightY += cardH + 10.0F;
            } else {
                leftY += cardH + 10.0F;
            }
        }

        float contentH = Math.max(leftY, rightY) - startY;
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0F, Math.max(0.0F, contentH - h));
        this.scrollAnim.setTarget(this.scrollTarget);
    }

    private void renderModuleCard(GuiGraphicsExtractor graphics, Module module, float x, float y, float w, float h, float expandVal, int mouseX, int mouseY, float alphaVal) {
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 38.0F;

        Animation toggleAnim = moduleToggleAnims.computeIfAbsent(module, k -> new Animation(module.isEnabled() ? 1.0F : 0.0F, 0.18F));
        toggleAnim.setTarget(module.isEnabled() ? 1.0F : 0.0F);
        toggleAnim.update();
        float tVal = toggleAnim.getValue();

        // Card Glass Background & Outline
        Color cardBg = Color.rgba(20, 22, 32, (int) ((0.60F + tVal * 0.15F) * 255 * alphaVal));
        Color outlineCol = Color.rgba(255, 255, 255, (int) ((0.10F + (isHovered ? 0.18F : 0.0F) + tVal * 0.20F) * 255 * alphaVal));

        RoundedRect.of(x, y, w, h)
                .radius(8)
                .color(cardBg)
                .render(graphics);

        Outline.of(x, y, w, h)
                .radius(8)
                .thickness(0.75F)
                .color(outlineCol)
                .render(graphics);

        // Module Name & Subtitle
        Color titleCol = module.isEnabled()
                ? Color.rgba(255, 255, 255, (int) (255 * alphaVal))
                : Color.rgba(185, 195, 215, (int) (210 * alphaVal));

        Text.of(module.getName())
                .font(Fonts.MANROPE_MEDIUM)
                .size(7.5F)
                .color(titleCol)
                .position(x + 12.0F, y + 8.0F)
                .render(graphics);

        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Text.of(desc)
                    .font(Fonts.MEDIUM)
                    .size(5.5F)
                    .color(Color.rgba(130, 145, 170, (int) (170 * alphaVal)))
                    .position(x + 12.0F, y + 21.0F)
                    .render(graphics);
        }

        // Toggle Switch Widget
        float switchW = 28.0F;
        float switchH = 14.0F;
        float switchX = x + w - switchW - 10.0F;
        float switchY = y + 12.0F;

        Color switchTrackBg = Color.rgba(255, 255, 255, (int) ((0.12F + tVal * 0.35F) * 255 * alphaVal));
        Color activeGradient1 = Color.rgb(106, 17, 203);
        Color activeGradient2 = Color.rgb(37, 117, 252);

        if (tVal > 0.01F) {
            RoundedRect.of(switchX, switchY, switchW, switchH)
                    .radius(7)
                    .horizontalGradient(activeGradient1, activeGradient2)
                    .alpha(tVal * alphaVal)
                    .render(graphics);
        } else {
            RoundedRect.of(switchX, switchY, switchW, switchH)
                    .radius(7)
                    .color(switchTrackBg)
                    .render(graphics);
        }

        // Switch Knob
        float knobSize = 10.0F;
        float knobX = switchX + 2.0F + (switchW - knobSize - 4.0F) * tVal;
        float knobY = switchY + 2.0F;
        Color knobColor = Color.rgba(255, 255, 255, (int) (255 * alphaVal));

        RoundedRect.of(knobX, knobY, knobSize, knobSize)
                .radius(5)
                .color(knobColor)
                .render(graphics);

        // Expanded Settings Section
        if (expandVal > 0.01F) {
            float setY = y + 36.0F;
            List<Setting<?>> settings = module.getSettings();
            for (Setting<?> setting : settings) {
                renderSettingRow(graphics, setting, x + 10.0F, setY, w - 20.0F, mouseX, mouseY, expandVal * alphaVal);
                setY += getSettingHeight(setting);
            }
        }
    }

    private void renderSettingRow(GuiGraphicsExtractor graphics, Setting<?> setting, float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        if (setting instanceof CheckBox cb) {
            Text.of(cb.getName())
                    .font(Fonts.MEDIUM)
                    .size(6.5F)
                    .color(Color.rgba(215, 225, 240, (int) (230 * alphaVal)))
                    .position(x, y + 3.0F)
                    .render(graphics);

            float boxSize = 12.0F;
            float boxX = x + w - boxSize;
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= y && mouseY <= y + boxSize;

            Color boxBg = cb.getValue()
                    ? Color.rgb(37, 117, 252)
                    : Color.rgba(255, 255, 255, (int) ((isHovered ? 0.18F : 0.08F) * 255 * alphaVal));

            RoundedRect.of(boxX, y, boxSize, boxSize)
                    .radius(3)
                    .color(boxBg)
                    .render(graphics);

            if (cb.getValue()) {
                Text.of("✓")
                        .font(Fonts.MEDIUM)
                        .size(7.0F)
                        .color(Color.rgba(255, 255, 255, (int) (255 * alphaVal)))
                        .align(TextAlign.CENTER)
                        .position(boxX + boxSize / 2.0F, y + 2.0F)
                        .render(graphics);
            }
        } else if (setting instanceof SliderSetting sl) {
            float val = sl.getValue();
            float min = sl.getMin();
            float max = sl.getMax();
            float pct = (val - min) / (max - min);

            Text.of(sl.getName())
                    .font(Fonts.MEDIUM)
                    .size(6.5F)
                    .color(Color.rgba(215, 225, 240, (int) (230 * alphaVal)))
                    .position(x, y)
                    .render(graphics);

            Text.of(String.format("%.1f", val))
                    .font(Fonts.MEDIUM)
                    .size(6.0F)
                    .color(Color.rgba(170, 185, 210, (int) (210 * alphaVal)))
                    .align(TextAlign.RIGHT)
                    .position(x + w, y)
                    .render(graphics);

            float barY = y + 11.0F;
            float barH = 4.0F;
            RoundedRect.of(x, barY, w, barH)
                    .radius(2)
                    .color(Color.rgba(255, 255, 255, (int) (25 * alphaVal)))
                    .render(graphics);

            float fillW = Math.max(4.0F, w * pct);
            RoundedRect.of(x, barY, fillW, barH)
                    .radius(2)
                    .horizontalGradient(Color.rgb(106, 17, 203), Color.rgb(37, 117, 252))
                    .alpha(alphaVal)
                    .render(graphics);
        } else if (setting instanceof BindSetting b) {
            Text.of("Назначение клавиши")
                    .font(Fonts.MEDIUM)
                    .size(6.5F)
                    .color(Color.rgba(215, 225, 240, (int) (230 * alphaVal)))
                    .position(x, y + 3.0F)
                    .render(graphics);

            String bindText = (activeBindingSetting == b) ? "[...]" : b.getDisplayValue();
            float btnW = 55.0F;
            float btnH = 14.0F;
            float btnX = x + w - btnW;

            RoundedRect.of(btnX, y, btnW, btnH)
                    .radius(4)
                    .color(Color.rgba(255, 255, 255, (int) (20 * alphaVal)))
                    .render(graphics);

            Text.of(bindText)
                    .font(Fonts.MEDIUM)
                    .size(6.0F)
                    .color(Color.rgba(255, 255, 255, (int) (245 * alphaVal)))
                    .align(TextAlign.CENTER)
                    .position(btnX + btnW / 2.0F, y + 3.5F)
                    .render(graphics);
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
            float catY = y + 56.0F;
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
            float searchW = 130.0F;
            float searchH = 22.0F;
            float searchX = x + SIDEBAR_W + 16.0F + (WINDOW_W - SIDEBAR_W - 32.0F) - searchW;
            this.searchFocused = (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y + 14.0F && mouseY <= y + 14.0F + searchH);

            // Module Cards Clicks
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 16.0F;
            float gridY = y + 50.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
            float gridH = WINDOW_H - 64.0F;

            float cardW = (gridW - 12.0F) / 2.0F;
            float startY = gridY - scrollAnim.getValue();
            float leftY = startY;
            float rightY = startY;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);
                float cardX = isRightColumn ? gridX + cardW + 12.0F : gridX;
                float currentY = isRightColumn ? rightY : leftY;
                float expandVal = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F)).getValue();
                float cardH = 38.0F + (expandVal * calculateSettingsHeight(module));

                if (mouseY >= gridY && mouseY <= gridY + gridH) {
                    // Header click toggles module
                    if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 38.0F) {
                        module.toggle();
                        return true;
                    }

                    // Settings clicks inside card
                    if (expandVal > 0.5F) {
                        float setY = currentY + 36.0F;
                        for (Setting<?> setting : module.getSettings()) {
                            float rowH = getSettingHeight(setting);
                            if (mouseY >= setY && mouseY <= setY + rowH && mouseX >= cardX + 10.0F && mouseX <= cardX + cardW - 10.0F) {
                                if (setting instanceof CheckBox cb) {
                                    cb.setValue(!cb.getValue());
                                    return true;
                                } else if (setting instanceof BindSetting b) {
                                    activeBindingSetting = b;
                                    return true;
                                }
                            }
                            setY += rowH;
                        }
                    }
                }

                if (isRightColumn) rightY += cardH + 10.0F;
                else leftY += cardH + 10.0F;
            }
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // Right-click opens module settings
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 16.0F;
            float gridY = y + 50.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
            float cardW = (gridW - 12.0F) / 2.0F;

            float startY = gridY - scrollAnim.getValue();
            float leftY = startY;
            float rightY = startY;

            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                boolean isRightColumn = (i % 2 != 0);
                float cardX = isRightColumn ? gridX + cardW + 12.0F : gridX;
                float currentY = isRightColumn ? rightY : leftY;
                float expandVal = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F)).getValue();
                float cardH = 38.0F + (expandVal * calculateSettingsHeight(module));

                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 38.0F) {
                    this.expandedModule = (expandedModule == module) ? null : module;
                    return true;
                }

                if (isRightColumn) rightY += cardH + 10.0F;
                else leftY += cardH + 10.0F;
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
        if (activeBindingSetting != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_DELETE) {
                if (activeBindingSetting instanceof BindSetting b) {
                    b.clear();
                }
            } else {
                if (activeBindingSetting instanceof BindSetting b) {
                    b.setSingle(event.key());
                }
            }
            activeBindingSetting = null;
            return true;
        }

        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                }
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused) {
            int codePoint = event.codepoint();
            if (codePoint >= 32 && codePoint != 127) {
                searchQuery += new String(Character.toChars(codePoint));
                return true;
            }
        }
        return super.charTyped(event);
    }
}
