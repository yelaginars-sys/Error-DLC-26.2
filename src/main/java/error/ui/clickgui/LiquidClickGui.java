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
import java.util.Random;

public class LiquidClickGui extends Screen {

    public static final float WINDOW_W = 670.0F;
    public static final float WINDOW_H = 435.0F;
    public static final float SIDEBAR_W = 165.0F;

    private static final Identifier LOGO_TEX = Identifier.fromNamespaceAndPath("error", "textures/system/logo.png");
    private static final Identifier SEARCH_TEX = Identifier.fromNamespaceAndPath("error", "textures/system/search.png");

    private static final Map<Category, Identifier> CATEGORY_ICONS = Map.of(
            Category.COMBAT, Identifier.fromNamespaceAndPath("error", "textures/system/combat.png"),
            Category.MOVEMENT, Identifier.fromNamespaceAndPath("error", "textures/system/movement.png"),
            Category.RENDER, Identifier.fromNamespaceAndPath("error", "textures/system/visuals.png"),
            Category.PLAYER, Identifier.fromNamespaceAndPath("error", "textures/system/player.png"),
            Category.MISC, Identifier.fromNamespaceAndPath("error", "textures/system/misc.png"),
            Category.CONFIGS, Identifier.fromNamespaceAndPath("error", "textures/system/configs.png"),
            Category.THEMES, Identifier.fromNamespaceAndPath("error", "textures/system/themes.png")
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
        for (int i = 0; i < 45; i++) {
            snowflakes.add(new Snowflake2D(
                    random.nextFloat() * 1000.0F,
                    random.nextFloat() * 700.0F,
                    1.5F + random.nextFloat() * 2.5F,
                    20.0F + random.nextFloat() * 40.0F,
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

        // Update slider drag if active
        if (this.draggingSlider != null && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            updateSliderDrag(mouseX);
        } else {
            this.draggingSlider = null;
        }

        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // 1. Dark Winter Backdrop Overlay
            int veilAlpha = (int) (180 * animVal);
            Render2D.drawRect(0, 0, screenW, screenH, ColorUtil.rgba(4, 7, 14, veilAlpha));

            // 2. 2D Falling Snowflakes Overlay
            renderSnowflakes(screenW, screenH, animVal);

            // 3. Real Frosted Backdrop Blur
            int blurTint = ColorUtil.rgba(10, 14, 26, (int) (135 * animVal));
            Render2D.drawBlur(x, y, WINDOW_W, WINDOW_H, 16.0F, 36.0F, blurTint, animVal);

            // 4. Main Window Glass Drop Shadow & Liquid Ice Gradient Body
            int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (230 * animVal));
            Render2D.drawShadow(x, y, WINDOW_W, WINDOW_H, 16.0F, 26.0F, shadowCol);

            int cTL = ColorUtil.rgba(14, 18, 32, (int) (238 * animVal));
            int cTR = ColorUtil.rgba(10, 15, 26, (int) (234 * animVal));
            int cBL = ColorUtil.rgba(7, 11, 20, (int) (242 * animVal));
            int cBR = ColorUtil.rgba(5, 9, 16, (int) (248 * animVal));
            Render2D.drawGradientRound(x, y, WINDOW_W, WINDOW_H, 16.0F, cTL, cTR, cBL, cBR);

            // 5. Specular Ice Glass Top Reflection
            Render2D.pushScissor(x, y, WINDOW_W, WINDOW_H);
            int shineTop = ColorUtil.rgba(220, 240, 255, (int) (20 * animVal));
            int shineBottom = ColorUtil.rgba(255, 255, 255, 0);
            Render2D.drawGradientRound(x, y, WINDOW_W, 130.0F, 16.0F, shineTop, shineTop, shineBottom, shineBottom);
            Render2D.popScissor();

            // 6. Icy Glow Outlines
            int glassOutline = ColorUtil.rgba(255, 255, 255, (int) (38 * animVal));
            Render2D.drawRoundedOutline(x, y, WINDOW_W, WINDOW_H, 16.0F, 0.8F, glassOutline);

            int accentColor = Theme.getAccentColor();
            int innerAccent = ColorUtil.withAlpha(accentColor, (int) (45 * animVal));
            Render2D.drawRoundedOutline(x + 0.5F, y + 0.5F, WINDOW_W - 1.0F, WINDOW_H - 1.0F, 15.5F, 0.8F, innerAccent);

            // 7. Sidebar Separator Line
            int dividerColor = ColorUtil.rgba(255, 255, 255, (int) (18 * animVal));
            Render2D.drawRoundedRect(x + SIDEBAR_W, y + 14.0F, 1.0F, WINDOW_H - 28.0F, 0.5F, dividerColor);

            // 8. Render Subsections
            renderSidebar(x, y, mouseX, mouseY, animVal);
            renderHeader(x + SIDEBAR_W + 16.0F, y + 16.0F, WINDOW_W - SIDEBAR_W - 32.0F, mouseX, mouseY, animVal);

            // 9. Main Panel Content (Modules vs Special Tabs)
            if (activeCategory == Category.THEMES) {
                renderThemesTab(x + SIDEBAR_W + 16.0F, y + 52.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 66.0F, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.CONFIGS) {
                renderConfigsTab(x + SIDEBAR_W + 16.0F, y + 52.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 66.0F, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.FRIENDS) {
                renderFriendsTab(x + SIDEBAR_W + 16.0F, y + 52.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 66.0F, mouseX, mouseY, animVal);
            } else if (activeCategory == Category.EVENTS || activeCategory == Category.COSMETICS) {
                renderEventsTab(x + SIDEBAR_W + 16.0F, y + 52.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 66.0F, mouseX, mouseY, animVal);
            } else {
                renderModulesGrid(x + SIDEBAR_W + 16.0F, y + 52.0F, WINDOW_W - SIDEBAR_W - 32.0F, WINDOW_H - 66.0F, mouseX, mouseY, animVal);
            }
        } finally {
            Render2DUtil.flush();
            RenderExtend.exit2D();
        }
    }

    private void renderSnowflakes(int screenW, int screenH, float alphaVal) {
        long time = System.currentTimeMillis();
        for (Snowflake2D sf : snowflakes) {
            sf.y += sf.speed * 0.04F;
            sf.x += (float) Math.sin((time * 0.002F) + sf.seed) * 0.4F;

            if (sf.y > screenH + 10.0F) {
                sf.y = -10.0F;
                sf.x = random.nextFloat() * screenW;
            }

            int flakeCol = ColorUtil.rgba(220, 240, 255, (int) (140 * alphaVal));
            Render2D.drawCircle(sf.x, sf.y, sf.size, flakeCol);
        }
    }

    private void renderSidebar(float x, float y, int mouseX, int mouseY, float alphaVal) {
        int whiteCol = ColorUtil.rgba(255, 255, 255, (int) (250 * alphaVal));
        int subCol = ColorUtil.rgba(165, 185, 220, (int) (190 * alphaVal));

        // Logo Image / Fallback Icon
        try {
            Render2D.drawTexture(LOGO_TEX, x + 14.0F, y + 14.0F, 22.0F, 22.0F, 4.0F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
        } catch (Throwable ignored) {
            Fonts.drawString(Fonts.ICONS, IconUse.LOGO.getGlyph(), x + 16.0F, y + 16.0F, 12.0F, ColorUtil.rgba(37, 117, 252, (int) (255 * alphaVal)));
        }

        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", x + 42.0F, y + 14.5F, 10.0F, whiteCol);
        Fonts.drawString(Fonts.SF_MEDIUM, "❆ Winter Edition 26.2", x + 42.0F, y + 27.5F, 5.5F, subCol);

        // Category Pills Stack (Compact height catH = 24.0F so all 10 categories fit cleanly above profile card!)
        Category[] categories = Category.values();
        float catY = y + 46.0F;
        float catH = 24.0F;
        float catW = SIDEBAR_W - 24.0F;
        float catX = x + 12.0F;

        float profileY = y + WINDOW_H - 44.0F;
        float maxCatY = profileY - 6.0F;

        int accentColor = Theme.getAccentColor();

        Render2DUtil.pushScissor(catX - 2.0F, catY, catW + 4.0F, maxCatY - catY);
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
                            ? ColorUtil.withAlpha(accentColor, (int) ((0.22F + hVal * 0.15F) * 255 * alphaVal))
                            : ColorUtil.rgba(255, 255, 255, (int) ((0.05F + hVal * 0.08F) * 255 * alphaVal));

                    int pillOutline = active
                            ? ColorUtil.withAlpha(accentColor, (int) (0.65F * 255 * alphaVal))
                            : ColorUtil.rgba(255, 255, 255, (int) (hVal * 0.25F * 255 * alphaVal));

                    Render2D.drawRoundedRect(catX, catY, catW, catH, 6.0F, pillBg);
                    Render2D.drawRoundedOutline(catX, catY, catW, catH, 6.0F, 0.8F, pillOutline);
                }

                // Category Icon
                int iconCol = active
                        ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                        : ColorUtil.rgba(160, 175, 205, (int) ((0.70F + hVal * 0.30F) * 255 * alphaVal));

                Identifier iconTex = CATEGORY_ICONS.get(cat);
                if (iconTex != null) {
                    Render2D.drawTexture(iconTex, catX + 8.0F, catY + 5.0F, 14.0F, 14.0F, 0.0F, iconCol);
                } else {
                    Fonts.drawString(Fonts.ICONS, getCategoryIcon(cat), catX + 8.0F, catY + 7.0F, 8.0F, iconCol);
                }

                // Category Title Label
                int nameCol = active
                        ? ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal))
                        : ColorUtil.rgba(175, 185, 205, (int) ((0.75F + hVal * 0.25F) * 255 * alphaVal));
                Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 28.0F, catY + 7.5F, 7.0F, nameCol);

                // Enabled Modules Count Badge
                long enabledCount = Client.getInstance().moduleManager.getModules().stream()
                        .filter(m -> m.getCategory() == cat && m.isEnabled())
                        .count();

                if (enabledCount > 0) {
                    String countStr = String.valueOf(enabledCount);
                    float countW = Fonts.SF_MEDIUM.getWidth(countStr, 5.0F);
                    float badgeW = Math.max(12.0F, countW + 6.0F);
                    float badgeX = catX + catW - 8.0F - badgeW;
                    float badgeY = catY + 5.5F;

                    int badgeBg = active
                            ? ColorUtil.withAlpha(accentColor, (int) (210 * alphaVal))
                            : ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal));

                    Render2D.drawRoundedRect(badgeX, badgeY, badgeW, 13.0F, 4.0F, badgeBg);
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, countStr, badgeX + badgeW / 2.0F, badgeY + 3.0F, 5.0F, ColorUtil.rgba(255, 255, 255, (int) (240 * alphaVal)));
                }

                catY += catH + 3.0F;
            }
        } finally {
            Render2DUtil.popScissor();
        }

        // Bottom User Profile Card
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

        // Search Icon
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

        if (modules.isEmpty()) {
            Render2D.drawRoundedRect(x, y, w, 120.0F, 10.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
            Render2D.drawRoundedOutline(x, y, w, 120.0F, 10.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "В этой категории нет доступных модулей", x + w / 2.0F, y + 45.0F, 8.0F, ColorUtil.rgba(170, 185, 210, (int) (200 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Воспользуйтесь поиском или выберите другую категорию в меню", x + w / 2.0F, y + 65.0F, 6.0F, ColorUtil.rgba(130, 145, 170, (int) (160 * alphaVal)));
            return;
        }

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

        // Description
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

    // ===================== SPECIAL TAB VIEWS =====================

    private void renderThemesTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Fonts.drawString(Fonts.SF_MEDIUM, "Выберите тему оформления интерфейса", x, y, 7.5F, ColorUtil.rgba(180, 195, 220, (int) (200 * alphaVal)));

        float cardW = (w - 14.0F) / 2.0F;
        float cardH = 75.0F;

        structThemeOption[] options = new structThemeOption[]{
                new structThemeOption("Winter Special ❄️", "Ледяной циан и зимний ультрафиолет", ColorUtil.rgba(0, 200, 255, 255), ColorUtil.rgba(160, 130, 255, 255), "Static"),
                new structThemeOption("Neon Frost 🧊", "Яркий неон и арктическая свежесть", ColorUtil.rgba(0, 255, 220, 255), ColorUtil.rgba(0, 180, 255, 255), "Static"),
                new structThemeOption("Chroma RGB 🌈", "Динамический перелив всех цветов радуги", ColorUtil.fromHsv(0.5f, 0.85f, 1.0f, 255), ColorUtil.fromHsv(0.7f, 0.85f, 1.0f, 255), "Chroma"),
                new structThemeOption("Christmas Red 🎅", "Новогодний алый и еловый изумруд", ColorUtil.rgba(255, 50, 75, 255), ColorUtil.rgba(40, 210, 120, 255), "Static"),
                new structThemeOption("Deep Violet 💜", "Фиолетовое неоновое стекло", ColorUtil.rgba(170, 70, 255, 255), ColorUtil.rgba(255, 100, 200, 255), "Static")
        };

        for (int i = 0; i < options.length; i++) {
            structThemeOption opt = options[i];
            float cx = (i % 2 == 0) ? x : x + cardW + 14.0F;
            float cy = y + 20.0F + (i / 2) * (cardH + 12.0F);

            boolean isHovered = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;
            boolean isActive = (Theme.getAccentMode().equalsIgnoreCase(opt.mode) && (opt.mode.equalsIgnoreCase("Chroma") || Theme.getAccentColor() == opt.accent));

            int bg = isActive
                    ? ColorUtil.rgba(25, 30, 48, (int) (220 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.12F : 0.06F) * 255 * alphaVal));

            int outline = isActive
                    ? ColorUtil.withAlpha(opt.accent, (int) (240 * alphaVal))
                    : ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 0.30F : 0.15F) * 255 * alphaVal));

            Render2D.drawShadow(cx, cy, cardW, cardH, 8.0F, 8.0F, ColorUtil.rgba(0, 0, 0, (int) (90 * alphaVal)));
            Render2D.drawRoundedRect(cx, cy, cardW, cardH, 8.0F, bg);
            Render2D.drawRoundedOutline(cx, cy, cardW, cardH, 8.0F, 0.8F, outline);

            // Preview Color Orbs
            Render2D.drawCircle(cx + 20.0F, cy + 24.0F, 8.0F, opt.accent);
            Render2D.drawCircle(cx + 34.0F, cy + 24.0F, 8.0F, opt.secondary);

            Fonts.drawString(Fonts.SF_MEDIUM, opt.title, cx + 52.0F, cy + 18.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
            Fonts.drawString(Fonts.SF_MEDIUM, opt.desc, cx + 52.0F, cy + 34.0F, 5.5F, ColorUtil.rgba(140, 160, 190, (int) (180 * alphaVal)));

            if (isActive) {
                Render2D.drawRoundedRect(cx + cardW - 65.0F, cy + 48.0F, 55.0F, 16.0F, 4.0F, ColorUtil.withAlpha(opt.accent, (int) (220 * alphaVal)));
                Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Активна", cx + cardW - 37.5F, cy + 52.0F, 5.5F, ColorUtil.rgba(255, 255, 255, (int) (255 * alphaVal)));
            }
        }
    }

    private void renderConfigsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 10.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 10.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Менеджер Конфигураций", x + 16.0F, y + 16.0F, 10.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Сохраняйте и загружайте ваши идеальные настройки", x + 16.0F, y + 30.0F, 6.0F, ColorUtil.rgba(160, 175, 205, (int) (200 * alphaVal)));

        String[] presets = new String[]{"default", "legit_funtime", "rage_hvh", "skywars_boost"};
        float itemY = y + 55.0F;
        for (String preset : presets) {
            Render2D.drawRoundedRect(x + 16.0F, itemY, w - 32.0F, 28.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
            Render2D.drawRoundedOutline(x + 16.0F, itemY, w - 32.0F, 28.0F, 6.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

            Fonts.drawString(Fonts.SF_MEDIUM, preset + ".json", x + 28.0F, itemY + 9.5F, 7.5F, 0xFFFFFFFF);

            float loadBtnX = x + w - 110.0F;
            Render2D.drawRoundedRect(loadBtnX, itemY + 5.0F, 75.0F, 18.0F, 4.0F, ColorUtil.withAlpha(Theme.getAccentColor(), (int) (200 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + 37.5F, itemY + 9.0F, 6.0F, 0xFFFFFFFF);

            itemY += 34.0F;
        }
    }

    private void renderFriendsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 10.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 10.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        Fonts.drawString(Fonts.SF_MEDIUM, "Список Друзей", x + 16.0F, y + 16.0F, 10.0F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Добавленные игроки игнорируются аимботом и киллаурой", x + 16.0F, y + 30.0F, 6.0F, ColorUtil.rgba(160, 175, 205, (int) (200 * alphaVal)));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Список друзей пуст. Кликните ПКМ по игроку в игре чтобы добавить!", x + w / 2.0F, y + h / 2.0F, 7.0F, ColorUtil.rgba(160, 175, 205, (int) (180 * alphaVal)));
    }

    private void renderEventsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal) {
        Render2D.drawRoundedRect(x, y, w, h, 10.0F, ColorUtil.rgba(255, 255, 255, (int) (10 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 10.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "❆ Новогодний Ивент Error DLC 2026 ❆", x + w / 2.0F, y + h / 2.0F - 12.0F, 11.0F, ColorUtil.withAlpha(Theme.getAccentColor(), (int) (255 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Эксклюзивные праздничные скинпаки и косметические крылья будут доступны в обновлении!", x + w / 2.0F, y + h / 2.0F + 8.0F, 6.5F, ColorUtil.rgba(180, 195, 220, (int) (200 * alphaVal)));
    }

    private record structThemeOption(String title, String desc, int accent, int secondary, String mode) {}

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

        float gridX = x + SIDEBAR_W + 16.0F;
        float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
        float cardW = (gridW - 14.0F) / 2.0F;
        float sliderW = cardW - 24.0F;

        float min = draggingSlider.getMin();
        float max = draggingSlider.getMax();

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
            float catY = y + 46.0F;
            float catH = 24.0F;
            float catW = SIDEBAR_W - 24.0F;
            float catX = x + 12.0F;

            for (Category cat : categories) {
                if (mouseX >= catX && mouseX <= catX + catW && mouseY >= catY && mouseY <= catY + catH) {
                    this.activeCategory = cat;
                    this.searchQuery = "";
                    this.searchFocused = false;
                    return true;
                }
                catY += catH + 3.0F;
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

            // Click inside Themes Tab Options
            if (activeCategory == Category.THEMES) {
                float gridX = x + SIDEBAR_W + 16.0F;
                float gridY = y + 52.0F;
                float gridW = WINDOW_W - SIDEBAR_W - 32.0F;
                float cardW = (gridW - 14.0F) / 2.0F;
                float cardH = 75.0F;

                structThemeOption[] options = new structThemeOption[]{
                        new structThemeOption("Winter Special ❄️", "", ColorUtil.rgba(0, 200, 255, 255), ColorUtil.rgba(160, 130, 255, 255), "Static"),
                        new structThemeOption("Neon Frost 🧊", "", ColorUtil.rgba(0, 255, 220, 255), ColorUtil.rgba(0, 180, 255, 255), "Static"),
                        new structThemeOption("Chroma RGB 🌈", "", ColorUtil.fromHsv(0.5f, 0.85f, 1.0f, 255), ColorUtil.fromHsv(0.7f, 0.85f, 1.0f, 255), "Chroma"),
                        new structThemeOption("Christmas Red 🎅", "", ColorUtil.rgba(255, 50, 75, 255), ColorUtil.rgba(40, 210, 120, 255), "Static"),
                        new structThemeOption("Deep Violet 💜", "", ColorUtil.rgba(170, 70, 255, 255), ColorUtil.rgba(255, 100, 200, 255), "Static")
                };

                for (int i = 0; i < options.length; i++) {
                    float cx = (i % 2 == 0) ? gridX : gridX + cardW + 14.0F;
                    float cy = gridY + 20.0F + (i / 2) * (cardH + 12.0F);

                    if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
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
            float gridX = x + SIDEBAR_W + 16.0F;
            float gridY = y + 52.0F;
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
                    module.toggle();
                    return true;
                }

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
            List<Module> modules = getFilteredModules();
            float gridX = x + SIDEBAR_W + 16.0F;
            float gridY = y + 52.0F;
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
