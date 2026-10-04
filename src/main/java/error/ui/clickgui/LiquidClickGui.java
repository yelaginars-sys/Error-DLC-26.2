package error.ui.clickgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
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

    private final Animation openAnim = new Animation(1.0F, 0.18F);
    private final Animation scrollAnim = new Animation(0.0F, 0.20F);
    private float scrollTarget = 0.0F;

    private final Map<Module, Animation> moduleExpandAnims = new HashMap<>();
    private final Map<Module, Animation> moduleToggleAnims = new HashMap<>();
    private final Map<Category, Animation> categoryHoverAnims = new HashMap<>();

    public Module expandedModule = null;
    public Setting<?> activeBindingSetting = null;

    private long openTime = System.currentTimeMillis();

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

        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // 1. Background dark veil tint
            int veilAlpha = (int) (160 * animVal);
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(4, 6, 10, veilAlpha));

            // 2. Main Window Container Shadow, Glass Rect & Glowing Outline
            int glassBg = ColorUtil.rgba(14, 16, 24, (int) (230 * animVal));
            int borderGlow = ColorUtil.rgba(255, 255, 255, (int) (40 * animVal));
            int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (200 * animVal));

            Render2D.drawShadow(x, y, WINDOW_W, WINDOW_H, 14.0F, 14.0F, shadowCol);
            Render2D.drawRoundedRect(x, y, WINDOW_W, WINDOW_H, 14.0F, glassBg);
            Render2D.drawRoundedOutline(x, y, WINDOW_W, WINDOW_H, 14.0F, 0.8F, borderGlow);

            // 3. Sidebar Divider & Sidebar UI
            int dividerColor = ColorUtil.rgba(255, 255, 255, (int) (22 * animVal));
            Render2D.drawRoundedRect(x + SIDEBAR_W, y + 12, 1.0F, WINDOW_H - 24, 0.5F, dividerColor);

            renderSidebar(x, y, mouseX, mouseY, animVal);

            // 4. Header Search Bar & Content Area
            renderHeader(x + SIDEBAR_W + 16.0F, y + 14.0F, WINDOW_W - SIDEBAR_W - 32.0F, mouseX, mouseY, animVal);
            renderModulesGrid(x + SIDEBAR_W + 16.0F, y + 50.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 64.0F, mouseX, mouseY, animVal);
        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderSidebar(float x, float y, int mouseX, int mouseY, float alphaVal) {
        // Logo & Title Header
        int whiteCol = ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal));
        int subCol = ColorUtil.rgba(160, 175, 200, (int) (180 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", x + 16.0F, y + 16.0F, 11.0F, whiteCol);
        Fonts.drawString(Fonts.SF_MEDIUM, "Liquid Glass v26.2", x + 16.0F, y + 30.0F, 5.5F, subCol);

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
                int pillBg = ColorUtil.rgba(255, 255, 255, (int) ((0.06F + hVal * 0.10F) * 255 * alphaVal));
                int pillOutline = ColorUtil.rgba(255, 255, 255, (int) (hVal * 0.20F * 255 * alphaVal));

                Render2D.drawRoundedRect(catX, catY, catW, catH, 7.0F, pillBg);
                if (active) {
                    Render2D.drawRoundedOutline(catX, catY, catW, catH, 7.0F, 0.7F, pillOutline);
                }
            }

            // Count enabled modules in category
            long enabledCount = Client.getInstance().moduleManager.getModules().stream()
                    .filter(m -> m.getCategory() == cat && m.isEnabled())
                    .count();

            int nameCol = active
                    ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                    : ColorUtil.rgba(175, 185, 205, (int) ((0.75F + hVal * 0.25F) * 255 * alphaVal));

            Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 12.0F, catY + 9.5F, 7.5F, nameCol);

            if (enabledCount > 0) {
                int countCol = ColorUtil.rgba(255, 255, 255, (int) (140 * alphaVal));
                String countStr = String.valueOf(enabledCount);
                float countW = Fonts.SF_MEDIUM.getWidth(countStr, 6.0F);
                Fonts.drawString(Fonts.SF_MEDIUM, countStr, catX + catW - 12.0F - countW, catY + 10.5F, 6.0F, countCol);
            }

            catY += catH + 4.0F;
        }

        // Bottom User Profile Card
        float profileY = y + WINDOW_H - 42.0F;
        float profileW = SIDEBAR_W - 24.0F;
        float profileX = x + 12.0F;

        int cardBg = ColorUtil.rgba(255, 255, 255, (int) (12 * alphaVal));
        int cardBorder = ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal));

        Render2D.drawRoundedRect(profileX, profileY, profileW, 30.0F, 8.0F, cardBg);
        Render2D.drawRoundedOutline(profileX, profileY, profileW, 30.0F, 8.0F, 0.7F, cardBorder);

        String username = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "User";
        Fonts.drawString(Fonts.SF_MEDIUM, username, profileX + 12.0F, profileY + 6.0F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Developer", profileX + 12.0F, profileY + 17.0F, 5.5F, ColorUtil.rgba(140, 160, 190, (int) (180 * alphaVal)));
    }

    private void renderHeader(float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";

        Fonts.drawString(Fonts.SF_MEDIUM, titleText, x, y + 2.0F, 11.0F, ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal)));

        // Search Bar Box
        float searchW = 130.0F;
        float searchH = 22.0F;
        float searchX = x + w - searchW;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y && mouseY <= y + searchH;
        int boxBg = ColorUtil.rgba(255, 255, 255, (int) ((searchFocused ? 0.12F : (isHovered ? 0.08F : 0.05F)) * 255 * alphaVal));
        int boxBorder = ColorUtil.rgba(255, 255, 255, (int) ((searchFocused ? 0.35F : 0.15F) * 255 * alphaVal));

        Render2D.drawRoundedRect(searchX, y, searchW, searchH, 6.0F, boxBg);
        Render2D.drawRoundedOutline(searchX, y, searchW, searchH, 6.0F, 0.75F, boxBorder);

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        int textCol = searchQuery.isEmpty() && !searchFocused
                ? ColorUtil.rgba(140, 150, 170, (int) (180 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), searchX + 8.0F, y + 7.0F, 6.5F, textCol);
    }

    private void renderModulesGrid(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
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

            if (currentY + cardH >= y && currentY <= y + h) {
                renderModuleCard(module, cardX, currentY, cardW, cardH, eVal, mouseX, mouseY, alphaVal);
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

    private void renderModuleCard(Module module, float x, float y, float w, float h, float expandVal, int mouseX, int mouseY, float alphaVal) {
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 38.0F;

        Animation toggleAnim = moduleToggleAnims.computeIfAbsent(module, k -> new Animation(module.isEnabled() ? 1.0F : 0.0F, 0.18F));
        toggleAnim.setTarget(module.isEnabled() ? 1.0F : 0.0F);
        toggleAnim.update();
        float tVal = toggleAnim.getValue();

        // Card Glass Background & Outline
        int cardBg = ColorUtil.rgba(20, 22, 32, (int) ((0.60F + tVal * 0.15F) * 255 * alphaVal));
        int outlineCol = ColorUtil.rgba(255, 255, 255, (int) ((0.10F + (isHovered ? 0.18F : 0.0F) + tVal * 0.20F) * 255 * alphaVal));

        Render2D.drawRoundedRect(x, y, w, h, 8.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 8.0F, 0.75F, outlineCol);

        // Module Name & Subtitle
        int titleCol = module.isEnabled()
                ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                : ColorUtil.rgba(185, 195, 215, (int) (210 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), x + 12.0F, y + 8.0F, 7.5F, titleCol);

        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, desc, x + 12.0F, y + 21.0F, 5.5F, ColorUtil.rgba(130, 145, 170, (int) (170 * alphaVal)));
        }

        // Toggle Switch Widget
        float switchW = 28.0F;
        float switchH = 14.0F;
        float switchX = x + w - switchW - 10.0F;
        float switchY = y + 12.0F;

        int switchTrackBg = tVal > 0.01F
                ? ColorUtil.rgba(37, 117, 252, (int) (255 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (0.12F * 255 * alphaVal));

        Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 7.0F, switchTrackBg);

        // Switch Knob
        float knobSize = 10.0F;
        float knobX = switchX + 2.0F + (switchW - knobSize - 4.0F) * tVal;
        float knobY = switchY + 2.0F;
        int knobColor = ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal));

        Render2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, 5.0F, knobColor);

        // Expanded Settings Section
        if (expandVal > 0.01F) {
            float setY = y + 36.0F;
            List<Setting<?>> settings = module.getSettings();
            for (Setting<?> setting : settings) {
                renderSettingRow(setting, x + 10.0F, setY, w - 20.0F, mouseX, mouseY, expandVal * alphaVal);
                setY += getSettingHeight(setting);
            }
        }
    }

    private void renderSettingRow(Setting<?> setting, float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        if (setting instanceof CheckBox cb) {
            Fonts.drawString(Fonts.SF_MEDIUM, cb.getName(), x, y + 3.0F, 6.5F, ColorUtil.rgba(215, 225, 240, (int) (230 * alphaVal)));

            float boxSize = 12.0F;
            float boxX = x + w - boxSize;
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= y && mouseY <= y + boxSize;

            int boxBg = cb.getValue()
                    ? ColorUtil.rgba(37, 117, 252, (int) (255 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.18F : 0.08F) * 255 * alphaVal));

            Render2D.drawRoundedRect(boxX, y, boxSize, boxSize, 3.0F, boxBg);
            if (cb.getValue()) {
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✓", boxX + boxSize / 2.0F, y + 2.0F, 7.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
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

            float barY = y + 11.0F;
            float barH = 4.0F;
            Render2D.drawRoundedRect(x, barY, w, barH, 2.0F, ColorUtil.rgba(255, 255, 255, (int) (25 * alphaVal)));

            float fillW = Math.max(4.0F, w * pct);
            Render2D.drawRoundedRect(x, barY, fillW, barH, 2.0F, ColorUtil.rgba(37, 117, 252, (int) (255 * alphaVal)));
        } else if (setting instanceof BindSetting b) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Назначение клавиши", x, y + 3.0F, 6.5F, ColorUtil.rgba(215, 225, 240, (int) (230 * alphaVal)));

            String bindText = (activeBindingSetting == b) ? "[...]" : b.getDisplayValue();
            float btnW = 55.0F;
            float btnH = 14.0F;
            float btnX = x + w - btnW;

            Render2D.drawRoundedRect(btnX, y, btnW, btnH, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, bindText, btnX + btnW / 2.0F, y + 3.5F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal)));
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
            float searchX = x + WINDOW_W - SIDEBAR_W - 16.0F - searchW;
            float searchY = y + 14.0F;

            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Modules Clicks
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

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                float eVal = expandAnim.getValue();
                float cardH = 38.0F + (eVal * calculateSettingsHeight(module));

                // Check click inside card header (38px height)
                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 38.0F) {
                    // Check toggle switch click
                    float switchW = 28.0F;
                    float switchH = 14.0F;
                    float switchX = cardX + cardW - switchW - 10.0F;
                    float switchY = currentY + 12.0F;

                    if (mouseX >= switchX && mouseX <= switchX + switchW && mouseY >= switchY && mouseY <= switchY + switchH) {
                        module.toggle();
                    }
                    return true;
                }

                // Check click inside expanded settings
                if (eVal > 0.01F && mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY + 36.0F && mouseY <= currentY + cardH) {
                    float setY = currentY + 36.0F;
                    for (Setting<?> setting : module.getSettings()) {
                        float sH = getSettingHeight(setting);
                        if (mouseY >= setY && mouseY <= setY + sH) {
                            if (setting instanceof CheckBox cb) {
                                cb.setValue(!cb.getValue());
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
            // Right click module card to expand/collapse settings
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

                Animation expandAnim = moduleExpandAnims.computeIfAbsent(module, k -> new Animation(0.0F, 0.20F));
                float eVal = expandAnim.getValue();
                float cardH = 38.0F + (eVal * calculateSettingsHeight(module));

                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 38.0F) {
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
