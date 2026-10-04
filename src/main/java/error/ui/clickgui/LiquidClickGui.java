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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LiquidClickGui extends Screen {

    public static final float WINDOW_W = 560.0F;
    public static final float WINDOW_H = 370.0F;
    public static final float SIDEBAR_W = 135.0F;

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

    // 2D Winter Snowflakes Particle System
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
        for (int i = 0; i < 35; i++) {
            snowflakes.add(new Snowflake2D(
                    random.nextFloat() * 900.0F,
                    random.nextFloat() * 600.0F,
                    1.2F + random.nextFloat() * 2.0F,
                    15.0F + random.nextFloat() * 30.0F,
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

        // Slider dragging handling
        if (this.draggingSlider != null && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            updateSliderDrag(mouseX);
        } else {
            this.draggingSlider = null;
        }

        String style = Theme.getUiStyle();
        if (style == null) style = "WINTER_GLASS";

        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // 1. Subtle Dark World Dimmer (keeps Minecraft world visible and blurred!)
            int veilAlpha = (int) (85 * animVal);
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(0, 0, 0, veilAlpha));

            // 2. Winter Snowflakes (falling behind or over glass)
            if ("WINTER_GLASS".equalsIgnoreCase(style) || "CHRISTMAS".equalsIgnoreCase(style)) {
                renderSnowflakes(screenW, screenH, animVal);
            }

            // 3. True Liquid Glass Shader Pass + Backdrop Refraction
            renderWindowFrame(x, y, WINDOW_W, WINDOW_H, animVal, style);

            // 4. Sidebar Glass Separator Line
            int dividerColor = ColorUtil.rgba(255, 255, 255, (int) (22 * animVal));
            Render2D.drawRoundedRect(x + SIDEBAR_W, y + 10.0F, 1.0F, WINDOW_H - 20.0F, 0.5F, dividerColor);

            // 5. Sidebar and Header
            renderSidebar(x, y, mouseX, mouseY, animVal, style);
            renderHeader(x + SIDEBAR_W + 14.0F, y + 14.0F, WINDOW_W - SIDEBAR_W - 28.0F, mouseX, mouseY, animVal);

            // 6. Content Section
            float contentX = x + SIDEBAR_W + 14.0F;
            float contentY = y + 46.0F;
            float contentW = WINDOW_W - SIDEBAR_W - 28.0F;
            float contentH = WINDOW_H - 58.0F;

            if (activeCategory == Category.THEMES) {
                renderThemesTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.CONFIGS) {
                renderConfigsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.FRIENDS) {
                renderFriendsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.EVENTS || activeCategory == Category.COSMETICS) {
                renderEventsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal);
            } else {
                renderModulesGrid(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, style);
            }
        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderWindowFrame(float x, float y, float w, float h, float animVal, String style) {
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (200 * animVal));
        Render2D.drawShadow(x, y, w, h, 14.0F, 24.0F, shadowCol);

        int accentColor = Theme.getAccentColor();

        if ("OBSIDIAN_BLACK".equalsIgnoreCase(style)) {
            // Pure Minimalist Obsidian Black (Frosted Dark Glass)
            Render2D.drawBlur(x, y, w, h, 14.0F, 24.0F, ColorUtil.rgba(4, 4, 6, (int) (160 * animVal)), animVal);
            int cTL = ColorUtil.rgba(18, 18, 22, (int) (180 * animVal));
            int cBR = ColorUtil.rgba(8, 8, 12, (int) (205 * animVal));
            Render2D.drawGradientRound(x, y, w, h, 14.0F, cTL, cTL, cBR, cBR);
            Render2D.drawRoundedOutline(x, y, w, h, 14.0F, 0.9F, ColorUtil.rgba(255, 255, 255, (int) (35 * animVal)));
            Render2D.drawRoundedOutline(x + 0.8F, y + 0.8F, w - 1.6F, h - 1.6F, 13.2F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (14 * animVal)));
        } else if ("CHRISTMAS".equalsIgnoreCase(style)) {
            // Christmas Festive Frosted Glass
            Render2D.drawBlur(x, y, w, h, 14.0F, 28.0F, ColorUtil.rgba(25, 8, 14, (int) (130 * animVal)), animVal);
            int cTL = ColorUtil.rgba(40, 16, 24, (int) (140 * animVal));
            int cBR = ColorUtil.rgba(14, 25, 18, (int) (160 * animVal));
            Render2D.drawGradientRound(x, y, w, h, 14.0F, cTL, cTL, cBR, cBR);
            Render2D.drawRoundedOutline(x, y, w, h, 14.0F, 0.9F, ColorUtil.rgba(255, 80, 100, (int) (75 * animVal)));
        } else if ("RETRO_UI".equalsIgnoreCase(style)) {
            // RetroUI Neobrutalism Frosted Slate
            Render2D.drawBlur(x, y, w, h, 8.0F, 18.0F, ColorUtil.rgba(14, 16, 22, (int) (160 * animVal)), animVal);
            int cBg = ColorUtil.rgba(20, 24, 32, (int) (210 * animVal));
            Render2D.drawRoundedRect(x, y, w, h, 8.0F, cBg);
            Render2D.drawRoundedOutline(x, y, w, h, 8.0F, 1.2F, ColorUtil.rgba(255, 255, 255, (int) (70 * animVal)));
        } else {
            // TRUE LIQUID GLASS SHADER (Winter Glass):
            // 1. Heavy Kawase backdrop blur allows game world to be refracted through glass
            Render2D.drawBlur(x, y, w, h, 14.0F, 34.0F, ColorUtil.rgba(10, 20, 38, (int) (115 * animVal)), animVal);

            // 2. Translucent liquid body (not opaque! lets blur shine through!)
            int cTL = ColorUtil.rgba(22, 38, 68, (int) (125 * animVal));
            int cTR = ColorUtil.rgba(15, 26, 50, (int) (110 * animVal));
            int cBL = ColorUtil.rgba(8, 16, 32, (int) (135 * animVal));
            int cBR = ColorUtil.rgba(6, 12, 24, (int) (150 * animVal));
            Render2D.drawGradientRound(x, y, w, h, 14.0F, cTL, cTR, cBL, cBR);

            // 3. Specular Liquid Light Reflection (Glossy top curved shine)
            Render2D.pushScissor(x, y, w, h);
            int shineTop = ColorUtil.rgba(230, 245, 255, (int) (40 * animVal));
            int shineMid = ColorUtil.rgba(180, 225, 255, (int) (12 * animVal));
            Render2D.drawGradientRound(x, y, w, 110.0F, 14.0F, shineTop, shineTop, shineMid, shineMid);
            Render2D.popScissor();

            // 4. Double Glass Outlines (Crisp Fresnel Outer + Vibrant Inner Accent Glow)
            Render2D.drawRoundedOutline(x, y, w, h, 14.0F, 0.9F, ColorUtil.rgba(255, 255, 255, (int) (55 * animVal)));
            Render2D.drawRoundedOutline(x + 0.8F, y + 0.8F, w - 1.6F, h - 1.6F, 13.2F, 0.8F, ColorUtil.withAlpha(accentColor, (int) (95 * animVal)));
        }
    }

    private void renderSnowflakes(int screenW, int screenH, float alphaVal) {
        long time = System.currentTimeMillis();
        for (Snowflake2D sf : snowflakes) {
            sf.y += sf.speed * 0.04F;
            sf.x += (float) Math.sin((time * 0.002F) + sf.seed) * 0.35F;

            if (sf.y > screenH + 8.0F) {
                sf.y = -8.0F;
                sf.x = random.nextFloat() * screenW;
            }

            int flakeCol = ColorUtil.rgba(220, 240, 255, (int) (130 * alphaVal));
            Render2D.drawCircle(sf.x, sf.y, sf.size, flakeCol);
        }
    }

    private void renderSidebar(float x, float y, int mouseX, int mouseY, float alphaVal, String style) {
        int whiteCol = ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal));
        int subCol = ColorUtil.rgba(180, 205, 240, (int) (190 * alphaVal));
        int accentColor = Theme.getAccentColor();

        // Branding Header
        Fonts.drawString(Fonts.ICONS, IconUse.LOGO.getGlyph(), x + 14.0F, y + 13.0F, 10.5F, accentColor);
        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", x + 34.0F, y + 12.5F, 9.0F, whiteCol);

        String editionText = "OBSIDIAN_BLACK".equalsIgnoreCase(style) ? "Obsidian Edition" : "Winter 26.2";
        Fonts.drawString(Fonts.SF_MEDIUM, editionText, x + 34.0F, y + 23.5F, 5.0F, subCol);

        // Category Pills Stack (Compact: 22px each, perfectly fits all 10 categories)
        Category[] categories = Category.values();
        float catY = y + 42.0F;
        float catH = 22.0F;
        float catW = SIDEBAR_W - 20.0F;
        float catX = x + 10.0F;

        float profileY = y + WINDOW_H - 36.0F;

        Render2DUtil.pushScissor(catX - 2.0F, catY, catW + 4.0F, profileY - catY - 4.0F);
        try {
            for (Category cat : categories) {
                boolean active = (cat == this.activeCategory && searchQuery.isEmpty());
                boolean isHovered = mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH;

                Animation hoverAnim = categoryHoverAnims.computeIfAbsent(cat, k -> new Animation(0.0F, 0.16F));
                hoverAnim.setTarget(active ? 1.0F : (isHovered ? 0.45F : 0.0F));
                hoverAnim.update();
                float hVal = hoverAnim.getValue();

                if (hVal > 0.01F) {
                    int pillBg = active
                            ? ColorUtil.withAlpha(accentColor, (int) ((0.24F + hVal * 0.16F) * 255 * alphaVal))
                            : ColorUtil.rgba(255, 255, 255, (int) ((0.06F + hVal * 0.08F) * 255 * alphaVal));

                    int pillOutline = active
                            ? ColorUtil.withAlpha(accentColor, (int) (0.80F * 255 * alphaVal))
                            : ColorUtil.rgba(255, 255, 255, (int) (hVal * 0.28F * 255 * alphaVal));

                    Render2D.drawRoundedRect(catX, catY, catW, catH, 5.0F, pillBg);
                    Render2D.drawRoundedOutline(catX, catY, catW, catH, 5.0F, 0.75F, pillOutline);
                }

                // Category Icon
                int iconCol = active
                        ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                        : ColorUtil.rgba(170, 190, 220, (int) ((0.70F + hVal * 0.30F) * 255 * alphaVal));
                Fonts.drawString(Fonts.ICONS, getCategoryIcon(cat), catX + 7.0F, catY + 6.0F, 8.0F, iconCol);

                // Category Name
                int nameCol = active
                        ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                        : ColorUtil.rgba(180, 195, 220, (int) ((0.75F + hVal * 0.25F) * 255 * alphaVal));
                Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 24.0F, catY + 6.5F, 6.5F, nameCol);

                // Enabled count badge
                long enabledCount = Client.getInstance().moduleManager.getModules().stream()
                        .filter(m -> m.getCategory() == cat && m.isEnabled())
                        .count();

                if (enabledCount > 0) {
                    String countStr = String.valueOf(enabledCount);
                    float countW = Fonts.SF_MEDIUM.getWidth(countStr, 5.0F);
                    float badgeW = Math.max(12.0F, countW + 5.0F);
                    float badgeX = catX + catW - 7.0F - badgeW;
                    float badgeY = catY + 4.5F;

                    int badgeBg = active
                            ? ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal))
                            : ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal));

                    Render2D.drawRoundedRect(badgeX, badgeY, badgeW, 12.0F, 3.5F, badgeBg);
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, countStr, badgeX + badgeW / 2.0F, badgeY + 2.5F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alphaVal)));
                }

                catY += catH + 2.5F;
            }
        } finally {
            Render2DUtil.popScissor();
        }

        // Bottom User Profile Card (Frosted Glass)
        float profileW = SIDEBAR_W - 20.0F;
        float profileH = 28.0F;
        float profileX = x + 10.0F;

        Render2D.drawRoundedRect(profileX, profileY, profileW, profileH, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
        Render2D.drawRoundedOutline(profileX, profileY, profileW, profileH, 6.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal)));

        Render2D.drawCustomAvatar(profileX + 5.0F, profileY + 4.0F, 20.0F, 5.0F, alphaVal);

        String username = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "User";
        Fonts.drawString(Fonts.SF_MEDIUM, username, profileX + 30.0F, profileY + 5.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal)));
        Fonts.drawString(Fonts.SF_MEDIUM, "Developer", profileX + 30.0F, profileY + 15.0F, 4.5F, ColorUtil.rgba(150, 175, 210, (int) (185 * alphaVal)));
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

    private void renderHeader(float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";
        Fonts.drawString(Fonts.SF_MEDIUM, titleText, x, y + 2.0F, 10.0F, ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal)));

        // Origin UI inspired Search Bar (Liquid Glass Pill)
        float searchW = 140.0F;
        float searchH = 20.0F;
        float searchX = x + w - searchW;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= y && mouseY <= y + searchH;
        int boxBg = ColorUtil.rgba(255, 255, 255, (int) ((searchFocused ? 0.16F : (isHovered ? 0.10F : 0.07F)) * 255 * alphaVal));
        int boxBorder = searchFocused
                ? ColorUtil.withAlpha(Theme.getAccentColor(), (int) (230 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (38 * alphaVal));

        Render2D.drawRoundedRect(searchX, y, searchW, searchH, 5.0F, boxBg);
        Render2D.drawRoundedOutline(searchX, y, searchW, searchH, 5.0F, 0.75F, boxBorder);

        Fonts.drawString(Fonts.ICONS, IconUse.SEARCH.getGlyph(), searchX + 7.0F, y + 5.5F, 7.5F, ColorUtil.rgba(180, 195, 220, (int) (210 * alphaVal)));

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        int textCol = searchQuery.isEmpty() && !searchFocused
                ? ColorUtil.rgba(150, 165, 190, (int) (185 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal));

        Fonts.drawString(Fonts.SF_MEDIUM, displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), searchX + 22.0F, y + 6.0F, 6.0F, textCol);
    }

    private void renderModulesGrid(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, String style) {
        List<Module> modules = getFilteredModules();

        if (modules.isEmpty()) {
            Render2D.drawRoundedRect(x, y, w, 90.0F, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
            Render2D.drawRoundedOutline(x, y, w, 90.0F, 8.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "В этой категории нет модулей", x + w / 2.0F, y + 35.0F, 7.5F, ColorUtil.rgba(180, 195, 220, (int) (210 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Выберите другую категорию в меню слева", x + w / 2.0F, y + 52.0F, 5.5F, ColorUtil.rgba(140, 155, 180, (int) (170 * alphaVal)));
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

                float cardH = 36.0F + (eVal * calculateSettingsHeight(module));

                if (currentY + cardH >= y && currentY <= y + h) {
                    renderModuleCard(module, cardX, currentY, cardW, cardH, eVal, mouseX, mouseY, alphaVal, style);
                }

                if (isRightColumn) {
                    rightY += cardH + 8.0F;
                } else {
                    leftY += cardH + 8.0F;
                }
            }
        } finally {
            Render2DUtil.popScissor();
        }

        float contentH = Math.max(leftY, rightY) - startY;
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0F, Math.max(0.0F, contentH - h));
        this.scrollAnim.setTarget(this.scrollTarget);
    }

    private void renderModuleCard(Module module, float x, float y, float w, float h, float expandVal, int mouseX, int mouseY, float alphaVal, String style) {
        boolean isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 36.0F;

        Animation toggleAnim = moduleToggleAnims.computeIfAbsent(module, k -> new Animation(module.isEnabled() ? 1.0F : 0.0F, 0.18F));
        toggleAnim.setTarget(module.isEnabled() ? 1.0F : 0.0F);
        toggleAnim.update();
        float tVal = toggleAnim.getValue();

        int accentColor = Theme.getAccentColor();

        // Card Drop Shadow
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (85 * alphaVal));
        Render2D.drawShadow(x, y, w, h, 6.0F, 6.0F, shadowCol);

        // Frosted Glass Card Container (Translucent, lets background blur shine through!)
        int cardBg;
        if ("OBSIDIAN_BLACK".equalsIgnoreCase(style)) {
            cardBg = module.isEnabled()
                    ? ColorUtil.rgba(36, 36, 46, (int) (180 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal));
        } else {
            cardBg = module.isEnabled()
                    ? ColorUtil.withAlpha(accentColor, (int) ((0.16F + tVal * 0.12F) * 255 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((0.07F + (isHovered ? 0.05F : 0.0F)) * 255 * alphaVal));
        }

        int outlineCol = module.isEnabled()
                ? ColorUtil.withAlpha(accentColor, (int) ((0.60F + (isHovered ? 0.25F : 0.0F)) * 255 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) ((0.12F + (isHovered ? 0.18F : 0.0F)) * 255 * alphaVal));

        Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
        Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 0.75F, outlineCol);

        // Status Indicator Orb
        int statusDotCol = module.isEnabled()
                ? ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal))
                : ColorUtil.rgba(130, 145, 170, (int) (140 * alphaVal));
        Render2D.drawCircle(x + 11.0F, y + 13.0F, 3.0F, statusDotCol);

        // Module Name
        int titleCol = module.isEnabled()
                ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                : ColorUtil.rgba(200, 210, 230, (int) (220 * alphaVal));
        Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), x + 20.0F, y + 7.5F, 7.0F, titleCol);

        // Module Description
        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            float maxDescW = w - 62.0F;
            Render2DUtil.pushScissor(x + 20.0F, y + 19.0F, maxDescW, 12.0F);
            try {
                Fonts.drawString(Fonts.SF_MEDIUM, desc, x + 20.0F, y + 19.0F, 4.8F, ColorUtil.rgba(145, 165, 195, (int) (180 * alphaVal)));
            } finally {
                Render2DUtil.popScissor();
            }
        }

        // Toggle Switch (iOS / Liquid Glass compact)
        float switchW = 26.0F;
        float switchH = 13.0F;
        float switchX = x + w - switchW - 9.0F;
        float switchY = y + 11.5F;

        int switchTrackBg = tVal > 0.01F
                ? ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal))
                : ColorUtil.rgba(255, 255, 255, (int) (0.16F * 255 * alphaVal));

        Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 6.5F, switchTrackBg);

        // Switch Knob
        float knobSize = 9.0F;
        float knobX = switchX + 2.0F + (switchW - knobSize - 4.0F) * tVal;
        float knobY = switchY + 2.0F;
        Render2D.drawRoundedRect(knobX, knobY, knobSize, knobSize, 4.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));

        // Expanded Settings Section
        if (expandVal > 0.01F) {
            float setY = y + 35.0F;
            List<Setting<?>> settings = module.getSettings();
            for (Setting<?> setting : settings) {
                renderSettingRow(setting, x + 10.0F, setY, w - 20.0F, mouseX, mouseY, expandVal * alphaVal);
                setY += getSettingHeight(setting);
            }
        }
    }

    private void renderSettingRow(Setting<?> setting, float x, float y, float w, int mouseX, int mouseY, float alphaVal) {
        int accentColor = Theme.getAccentColor();

        if (setting instanceof CheckBox cb) {
            Fonts.drawString(Fonts.SF_MEDIUM, cb.getName(), x, y + 2.0F, 5.8F, ColorUtil.rgba(220, 230, 245, (int) (235 * alphaVal)));

            float boxSize = 11.0F;
            float boxX = x + w - boxSize;
            boolean isHovered = mouseX >= boxX && mouseX <= boxX + boxSize && mouseY >= y && mouseY <= y + boxSize;

            int boxBg = cb.getValue()
                    ? ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.22F : 0.10F) * 255 * alphaVal));

            Render2D.drawRoundedRect(boxX, y, boxSize, boxSize, 3.0F, boxBg);
            if (cb.getValue()) {
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✓", boxX + boxSize / 2.0F, y + 1.5F, 6.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
            }
        } else if (setting instanceof SliderSetting sl) {
            float val = sl.getValue();
            float min = sl.getMin();
            float max = sl.getMax();
            float pct = (val - min) / (max - min);

            Fonts.drawString(Fonts.SF_MEDIUM, sl.getName(), x, y, 5.8F, ColorUtil.rgba(220, 230, 245, (int) (235 * alphaVal)));
            String valStr = String.format("%.1f", val);
            float valW = Fonts.SF_MEDIUM.getWidth(valStr, 5.5F);
            Fonts.drawString(Fonts.SF_MEDIUM, valStr, x + w - valW, y, 5.5F, ColorUtil.rgba(180, 195, 220, (int) (215 * alphaVal)));

            float barY = y + 10.0F;
            float barH = 4.0F;
            Render2D.drawRoundedRect(x, barY, w, barH, 2.0F, ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));

            float fillW = Math.max(4.0F, w * pct);
            Render2D.drawRoundedRect(x, barY, fillW, barH, 2.0F, ColorUtil.withAlpha(accentColor, (int) (255 * alphaVal)));

            float handleX = x + fillW - 2.0F;
            Render2D.drawCircle(handleX, barY + 2.0F, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
        } else if (setting instanceof BindSetting b) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Назначение клавиши", x, y + 2.0F, 5.8F, ColorUtil.rgba(220, 230, 245, (int) (235 * alphaVal)));

            String bindText = (activeBindingSetting == b) ? "[...]" : b.getDisplayValue();
            float btnW = 50.0F;
            float btnH = 13.0F;
            float btnX = x + w - btnW;

            Render2D.drawRoundedRect(btnX, y, btnW, btnH, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, bindText, btnX + btnW / 2.0F, y + 3.0F, 5.2F, ColorUtil.rgba(255, 255, 255, (int) (245 * alphaVal)));
        }
    }

    // ===================== THEMES TAB =====================

    private void renderThemesTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Выберите стиль и цветовую тему интерфейса", x, y, 7.0F, ColorUtil.rgba(190, 210, 235, (int) (210 * alphaVal)));

        float cardW = (w - 10.0F) / 2.0F;
        float cardH = 60.0F;

        // Clean names without broken emoji question marks!
        structThemeOption[] options = new structThemeOption[]{
                new structThemeOption("WINTER_GLASS", "Зимнее Жидкое Стекло", "Ледяной циан, снег и сияние стекла", ColorUtil.rgba(0, 190, 255, 255), ColorUtil.rgba(140, 210, 255, 255), "Static"),
                new structThemeOption("OBSIDIAN_BLACK", "Черный Обсидиан", "Глубокий матовый черный, строгий минимализм", ColorUtil.rgba(255, 255, 255, 255), ColorUtil.rgba(180, 190, 205, 255), "Static"),
                new structThemeOption("CHRISTMAS", "Новогодний Карнавал", "Праздничный алый бархат и еловый изумруд", ColorUtil.rgba(255, 55, 75, 255), ColorUtil.rgba(40, 210, 120, 255), "Static"),
                new structThemeOption("NEON_CYBER", "Неоновый Киберпанк", "Динамический радужный перелив Chroma", ColorUtil.fromHsv(0.5f, 0.85f, 1.0f, 255), ColorUtil.fromHsv(0.8f, 0.85f, 1.0f, 255), "Chroma"),
                new structThemeOption("RETRO_UI", "RetroUI 90s", "Ретро-интерфейс, контрастные рамки", ColorUtil.rgba(16, 185, 129, 255), ColorUtil.rgba(59, 130, 246, 255), "Static")
        };

        String currentStyle = Theme.getUiStyle();
        if (currentStyle == null) currentStyle = "WINTER_GLASS";

        for (int i = 0; i < options.length; i++) {
            structThemeOption opt = options[i];
            float cx = (i % 2 == 0) ? x : x + cardW + 10.0F;
            float cy = y + 16.0F + (i / 2) * (cardH + 8.0F);

            boolean isHovered = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;
            boolean isActive = currentStyle.equalsIgnoreCase(opt.styleId);

            int bg = isActive
                    ? ColorUtil.rgba(25, 45, 80, (int) (180 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.14F : 0.07F) * 255 * alphaVal));

            int outline = isActive
                    ? ColorUtil.withAlpha(opt.accent, (int) (245 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.35F : 0.16F) * 255 * alphaVal));

            Render2D.drawShadow(cx, cy, cardW, cardH, 6.0F, 6.0F, ColorUtil.rgba(0, 0, 0, (int) (70 * alphaVal)));
            Render2D.drawRoundedRect(cx, cy, cardW, cardH, 6.0F, bg);
            Render2D.drawRoundedOutline(cx, cy, cardW, cardH, 6.0F, 0.75F, outline);

            // Preview Color Orbs
            Render2D.drawCircle(cx + 16.0F, cy + 18.0F, 6.5F, opt.accent);
            Render2D.drawCircle(cx + 27.0F, cy + 18.0F, 6.5F, opt.secondary);

            Fonts.drawString(Fonts.SF_MEDIUM, opt.title, cx + 40.0F, cy + 13.0F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
            Fonts.drawString(Fonts.SF_MEDIUM, opt.desc, cx + 40.0F, cy + 26.0F, 4.8F, ColorUtil.rgba(150, 175, 210, (int) (190 * alphaVal)));

            if (isActive) {
                Render2D.drawRoundedRect(cx + cardW - 55.0F, cy + 38.0F, 46.0F, 14.0F, 3.5F, ColorUtil.withAlpha(opt.accent, (int) (230 * alphaVal)));
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Активно", cx + cardW - 32.0F, cy + 41.5F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
            }
        }
    }

    private void renderConfigsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 8.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Менеджер Конфигураций", x + 14.0F, y + 14.0F, 9.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Сохраняйте и загружайте настройки клиента", x + 14.0F, y + 26.0F, 5.5F, ColorUtil.rgba(170, 190, 220, (int) (200 * alphaVal)));

        String[] presets = new String[]{"default", "legit_funtime", "rage_hvh", "skywars_boost"};
        float itemY = y + 46.0F;
        for (String preset : presets) {
            Render2D.drawRoundedRect(x + 14.0F, itemY, w - 28.0F, 24.0F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
            Render2D.drawRoundedOutline(x + 14.0F, itemY, w - 28.0F, 24.0F, 5.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (25 * alphaVal)));

            Fonts.drawString(Fonts.SF_MEDIUM, preset + ".json", x + 24.0F, itemY + 8.0F, 6.5F, 0xFFFFFFFF);

            float loadBtnX = x + w - 85.0F;
            Render2D.drawRoundedRect(loadBtnX, itemY + 4.0F, 60.0F, 16.0F, 3.5F, ColorUtil.withAlpha(Theme.getAccentColor(), (int) (220 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + 30.0F, itemY + 7.5F, 5.2F, 0xFFFFFFFF);

            itemY += 28.0F;
        }
    }

    private void renderFriendsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 8.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Список Друзей", x + 14.0F, y + 14.0F, 9.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Друзья автоматически игнорируются аимом и киллаурой", x + 14.0F, y + 26.0F, 5.5F, ColorUtil.rgba(170, 190, 220, (int) (200 * alphaVal)));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Список друзей пуст. Кликните ПКМ по игроку в игре чтобы добавить!", x + w / 2.0F, y + h / 2.0F, 6.5F, ColorUtil.rgba(170, 190, 220, (int) (190 * alphaVal)));
    }

    private void renderEventsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 8.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 8.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Новогодний Ивент Error DLC 2026", x + w / 2.0F, y + h / 2.0F - 10.0F, 9.5F, ColorUtil.withAlpha(Theme.getAccentColor(), (int) (255 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Эксклюзивные праздничные эффекты и косметика будут доступны в обновлении!", x + w / 2.0F, y + h / 2.0F + 8.0F, 5.8F, ColorUtil.rgba(180, 200, 230, (int) (200 * alphaVal)));
    }

    private record structThemeOption(String styleId, String title, String desc, int accent, int secondary, String mode) {}

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

        float gridX = x + SIDEBAR_W + 14.0F;
        float gridW = WINDOW_W - SIDEBAR_W - 28.0F;
        float cardW = (gridW - 10.0F) / 2.0F;
        float sliderW = cardW - 20.0F;

        float min = draggingSlider.getMin();
        float max = draggingSlider.getMax();

        float pct = Math.clamp((mouseX - gridX - 10.0F) / sliderW, 0.0F, 1.0F);
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
            float catY = y + 42.0F;
            float catH = 22.0F;
            float catW = SIDEBAR_W - 20.0F;
            float catX = x + 10.0F;

            for (Category cat : categories) {
                if (mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH) {
                    this.activeCategory = cat;
                    this.searchQuery = "";
                    this.searchFocused = false;
                    return true;
                }
                catY += catH + 2.5F;
            }

            // Search Bar Click
            float searchW = 140.0F;
            float searchH = 20.0F;
            float searchX = x + WINDOW_W - SIDEBAR_W - 14.0F - searchW;
            float searchY = y + 14.0F;

            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Click Themes Tab Cards
            if (activeCategory == Category.THEMES) {
                float gridX = x + SIDEBAR_W + 14.0F;
                float gridY = y + 46.0F;
                float gridW = WINDOW_W - SIDEBAR_W - 28.0F;
                float cardW = (gridW - 10.0F) / 2.0F;
                float cardH = 60.0F;

                structThemeOption[] options = new structThemeOption[]{
                        new structThemeOption("WINTER_GLASS", "", "", ColorUtil.rgba(0, 190, 255, 255), ColorUtil.rgba(140, 210, 255, 255), "Static"),
                        new structThemeOption("OBSIDIAN_BLACK", "", "", ColorUtil.rgba(255, 255, 255, 255), ColorUtil.rgba(180, 190, 205, 255), "Static"),
                        new structThemeOption("CHRISTMAS", "", "", ColorUtil.rgba(255, 55, 75, 255), ColorUtil.rgba(40, 210, 120, 255), "Static"),
                        new structThemeOption("NEON_CYBER", "", "", ColorUtil.fromHsv(0.5f, 0.85f, 1.0f, 255), ColorUtil.fromHsv(0.8f, 0.85f, 1.0f, 255), "Chroma"),
                        new structThemeOption("RETRO_UI", "", "", ColorUtil.rgba(16, 185, 129, 255), ColorUtil.rgba(59, 130, 246, 255), "Static")
                };

                for (int i = 0; i < options.length; i++) {
                    float cx = (i % 2 == 0) ? gridX : gridX + cardW + 10.0F;
                    float cy = gridY + 16.0F + (i / 2) * (cardH + 8.0F);

                    if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                        Theme.setUiStyle(options[i].styleId);
                        Theme.setAccentMode(options[i].mode);
                        if (options[i].mode.equalsIgnoreCase("Static")) {
                            Theme.setAccentColor(options[i].accent);
                            Theme.setSecondaryColor(options[i].secondary);
                        }
                        return true;
                    }
                }
            }

            // Modules Clicks
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 14.0F;
            float gridY = y + 46.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 28.0F;
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
                float cardH = 36.0F + (eVal * calculateSettingsHeight(module));

                // Click module card header to toggle
                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 36.0F) {
                    module.toggle();
                    return true;
                }

                // Click inside settings
                if (eVal > 0.01F && mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY + 35.0F && mouseY <= currentY + cardH) {
                    float setY = currentY + 35.0F;
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
                    rightY += cardH + 8.0F;
                } else {
                    leftY += cardH + 8.0F;
                }
            }
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            // Right-click expands settings
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 14.0F;
            float gridY = y + 46.0F;
            float gridW = WINDOW_W - SIDEBAR_W - 28.0F;
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
                float cardH = 36.0F + (eVal * calculateSettingsHeight(module));

                if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= currentY && mouseY <= currentY + 36.0F) {
                    this.expandedModule = (this.expandedModule == module) ? null : module;
                    return true;
                }

                if (isRightColumn) {
                    rightY += cardH + 8.0F;
                } else {
                    leftY += cardH + 8.0F;
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
