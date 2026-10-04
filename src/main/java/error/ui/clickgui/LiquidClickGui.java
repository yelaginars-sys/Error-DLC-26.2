package error.ui.clickgui;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.*;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.KeyUtil;
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
import error.ui.hud.HudManager;
import error.ui.hud.HudElement;
import error.ui.hud.impl.DynamicIslandHud;
import error.module.impl.render.ClickGui;
import error.module.impl.render.CustomModels;
import error.cosmetic.CosmeticsManager;
import error.cosmetic.CosmeticItem;
import error.cosmetic.CosmeticType;
import error.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class LiquidClickGui extends Screen {

    public static final float WINDOW_W = 540.0F;
    public static final float WINDOW_H = 360.0F;
    public static final float SIDEBAR_W = 125.0F;

    private static final Color FADE_WHITE = Color.rgba(255, 255, 255, 32);
    private static final Identifier LOGO_NONFONE = Identifier.fromNamespaceAndPath("error", "textures/logo_nonfone.png");

    public Category activeCategory = Category.COMBAT;
    public String searchQuery = "";
    public boolean searchFocused = false;

    // Settings Modal (Top-Right Gear)
    public boolean settingsModalOpen = false;
    public ColorSetting activeEditingColorSetting = null;
    public boolean bindingClickGuiKey = false;
    public boolean editingSecondaryColor = false;
    public float pickerHue = 0.55F;
    public float pickerSat = 1.0F;
    public float pickerBri = 1.0F;

    private enum DragTarget { NONE, FIELD_2D, HUE_VERT }
    private DragTarget draggingPicker = DragTarget.NONE;

    // Configs Tab States
    public String newConfigInput = "";
    public boolean newConfigFocused = false;
    public String shareKeyInput = "";
    public boolean shareKeyFocused = false;
    private final Animation configsScrollAnim = new Animation(0.0F, 0.22F);
    private float configsScrollTarget = 0.0F;

    // Friends Tab States
    public String friendInput = "";
    public boolean friendInputFocused = false;
    private final Animation friendsScrollAnim = new Animation(0.0F, 0.22F);
    private float friendsScrollTarget = 0.0F;

    // Cosmetics Tab States
    public String cosmeticFilter = "Все";
    private final Animation cosmeticsScrollAnim = new Animation(0.0F, 0.22F);
    private float cosmeticsScrollTarget = 0.0F;
    private float cosmeticsPlayerYaw = 0.0F;
    private boolean draggingCosmeticsPlayer = false;
    private double lastCosmeticsMouseX = 0;

    // Module Middle-Click Bind Modal States
    public Module moduleModalModule = null;
    public boolean moduleModalOpen = false;
    public boolean moduleModalBinding = false;

    // Mode Setting Dropdown Popup States
    public ModeSetting activeDropdownMode = null;
    public float dropdownPopupX = 0.0F;
    public float dropdownPopupY = 0.0F;
    public float dropdownPopupW = 85.0F;
    public float dropdownPopupH = 0.0F;

    // General UI Animations & State
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
        error.util.client.ClientSoundPlayer.playGuiOpen();
        this.openAnim.setValue(1.0F);
        this.openAnim.setTarget(1.0F);
        this.openTime = System.currentTimeMillis();
        this.draggingSlider = null;
        this.draggingPicker = DragTarget.NONE;
        this.moduleModalOpen = false;
        this.moduleModalBinding = false;
        this.activeDropdownMode = null;
        if (this.activeCategory == Category.THEMES) {
            this.activeCategory = Category.COMBAT;
        }
        if (snowflakes.isEmpty()) initSnowflakes();
    }

    @Override
    public void onClose() {
        error.util.client.ClientSoundPlayer.playGuiClose();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static Identifier safeId(String path) {
        try {
            return Identifier.fromNamespaceAndPath("error", path);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Identifier getCategoryIcon(Category cat) {
        return switch (cat) {
            case COMBAT -> safeId("textures/system/combat.png");
            case MOVEMENT -> safeId("textures/system/movement.png");
            case RENDER -> safeId("textures/system/visuals.png");
            case COSMETICS -> safeId("textures/system/folder2.png");
            case PLAYER -> safeId("textures/system/player.png");
            case MISC -> safeId("textures/system/misc.png");
            case THEMES -> safeId("textures/system/themes.png");
            case EVENTS -> safeId("textures/system/heartbeat1.png");
            case CONFIGS -> safeId("textures/system/configs.png");
            case FRIENDS -> safeId("textures/system/player1.png");
            default -> null;
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        openAnim.update();
        scrollAnim.update();
        configsScrollAnim.update();
        friendsScrollAnim.update();
        cosmeticsScrollAnim.update();

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

        if (this.draggingCosmeticsPlayer && GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            float deltaX = (float) (mouseX - lastCosmeticsMouseX);
            this.cosmeticsPlayerYaw += deltaX * 1.5F;
            this.lastCosmeticsMouseX = mouseX;
        } else {
            this.draggingCosmeticsPlayer = false;
        }

        int accentColor = Theme.getAccentColor();

        // 1. Subtle World Dimmer
        RoundedRect.of(0, 0, screenW, screenH)
                .color(Color.rgba(0, 0, 0, Math.round(75 * animVal)))
                .render(extractor);

        // 2. Liquid Glass Window (Exact frosted Kawase Blur from RenderDemo)
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

        // 3. Settings Modal Liquid Glass Window
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

        // 4. Module Middle-Click Bind Modal Liquid Glass Window
        if (this.moduleModalOpen && this.moduleModalModule != null) {
            float mModalW = 180.0F;
            float mModalH = 98.0F;
            float mModalX = (screenW - mModalW) / 2.0F;
            float mModalY = (screenH - mModalH) / 2.0F;

            Blur.of(mModalX, mModalY, mModalW, mModalH)
                    .radius(10)
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(0, 0, 0, Math.round(90 * animVal)))
                    .alpha(animVal)
                    .render(extractor);

            Outline.of(mModalX, mModalY, mModalW, mModalH)
                    .radius(10)
                    .thickness(1.0F)
                    .verticalGradient(Color.WHITE, FADE_WHITE)
                    .alpha(animVal)
                    .render(extractor);
        }

        // Flush the glass background first
        DisplayBatcher.flush();

        // 5. Render Cards, Buttons, and Fonts via Render2D & Render2DUtil
        RenderExtend.enter2D(null, extractor, null);
        Render2DUtil.beginFrame();
        try {
            // Winter snowflakes
            renderSnowflakes(screenW, screenH, animVal);

            // Subtle divider line between sidebar and content
            Render2D.drawRoundedRect(x + SIDEBAR_W, y + 10.0F, 1.0F, WINDOW_H - 20.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (16 * animVal)));

            // Sidebar (with Logo tinted by theme color and system asset icons)
            renderSidebar(x, y, mouseX, mouseY, animVal, accentColor);

            // Header (with Settings Gear at top-right and Search bar to its left)
            renderHeader(x, y, mouseX, mouseY, animVal, accentColor);

            // Content Section
            float contentX = x + SIDEBAR_W + 12.0F;
            float contentY = y + 44.0F;
            float contentW = WINDOW_W - SIDEBAR_W - 24.0F;
            float contentH = WINDOW_H - 54.0F;

            if (activeCategory == Category.CONFIGS) {
                renderConfigsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            } else if (activeCategory == Category.FRIENDS) {
                renderFriendsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            } else if (activeCategory == Category.COSMETICS) {
                renderCosmeticsTab(extractor, contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            } else if (activeCategory == Category.EVENTS) {
                renderEventsTab(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            } else {
                renderModulesGrid(contentX, contentY, contentW, contentH, mouseX, mouseY, animVal, accentColor);
            }

            // Settings Modal Popup
            if (this.settingsModalOpen) {
                renderSettingsModal(screenW, screenH, mouseX, mouseY, animVal, accentColor);
            }

            // Module Middle-Click Bind Modal Popup
            if (this.moduleModalOpen && this.moduleModalModule != null) {
                renderModuleModal(screenW, screenH, mouseX, mouseY, animVal, accentColor);
            }

            // Dropdown Menu Popup (floating on top of everything)
            if (this.activeDropdownMode != null) {
                renderModeDropdown(screenW, screenH, mouseX, mouseY, animVal, accentColor);
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
        // Logo from "D:\Error logo nonfone.png" placed in top-left sidebar header
        // Tinted with theme accent color (+ зависимость цвета от темы)
        float logoX = x + 9.0F;
        float logoY = y + 8.5F;
        float logoSize = 21.0F;
        int logoTint = ColorUtil.withAlpha(accentColor, (int) (240 * alphaVal));
        Render2D.drawTexture(LOGO_NONFONE, logoX, logoY, logoSize, logoSize, logoTint);

        // Branding next to the logo
        Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", logoX + logoSize + 6.0F, y + 9.5F, 8.5F, 0xFFFFFFFF);
        Fonts.drawString(Fonts.SF_MEDIUM, "Winter 26.2", logoX + logoSize + 6.0F, y + 20.0F, 4.8F, 0xFFA0B0C4);

        // Category List (Themes tab filtered out as requested)
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

            // Real Category Icon from system assets
            Identifier catIcon = getCategoryIcon(cat);
            int iconCol = active ? 0xFFFFFFFF : (isHovered ? 0xFFFFFFFF : ColorUtil.rgba(160, 180, 200, (int) (180 * alphaVal)));
            if (catIcon != null) {
                Render2D.drawTexture(catIcon, catX + 8.0F, catY + 5.0F, 11.0F, 11.0F, iconCol);
            } else {
                Render2D.drawCircle(catX + 13.0F, catY + catH / 2.0F, 2.2F, active ? accentColor : 0xFF8090A0);
            }

            int nameCol = active ? 0xFFFFFFFF : 0xFFC0D0E0;
            Fonts.drawString(Fonts.SF_MEDIUM, cat.getDisplayName(), catX + 24.0F, catY + 6.5F, 6.2F, nameCol);

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

    private void renderHeader(float x, float y, int mouseX, int mouseY, float alphaVal, int accentColor) {
        float headerX = x + SIDEBAR_W + 12.0F;
        float headerY = y + 14.0F;

        // Title on the left
        String titleText = searchQuery.isEmpty() ? activeCategory.getDisplayName() : "Поиск: \"" + searchQuery + "\"";
        Fonts.drawString(Fonts.SF_MEDIUM, titleText, headerX, headerY + 2.0F, 9.5F, 0xFFFFFFFF);

        // Gear Settings Button strictly at the TOP-RIGHT corner
        float gearSize = 18.0F;
        float gearX = x + WINDOW_W - 14.0F - gearSize;
        float gearY = headerY;

        boolean gearHovered = mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize;
        int gearBg = settingsModalOpen ? ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)) : (gearHovered ? ColorUtil.rgba(255, 255, 255, (int) (35 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));
        int gearOutline = settingsModalOpen ? ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal)) : (gearHovered ? ColorUtil.rgba(255, 255, 255, (int) (45 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

        Render2D.drawRoundedRect(gearX, gearY, gearSize, gearSize, 4.5F, gearBg);
        Render2D.drawRoundedOutline(gearX, gearY, gearSize, gearSize, 4.5F, 0.65F, gearOutline);

        // Geometric sliders / settings icon
        int iconCol = settingsModalOpen ? 0xFFFFFFFF : (gearHovered ? accentColor : 0xFFB0C0D4);
        float cy = gearY + gearSize / 2.0F;
        Render2D.drawRoundedRect(gearX + 3.5F, cy - 3.8F, 11.0F, 1.4F, 0.7F, iconCol);
        Render2D.drawCircle(gearX + 6.0F, cy - 3.1F, 1.6F, iconCol);

        Render2D.drawRoundedRect(gearX + 3.5F, cy - 0.7F, 11.0F, 1.4F, 0.7F, iconCol);
        Render2D.drawCircle(gearX + 11.0F, cy, 1.6F, iconCol);

        Render2D.drawRoundedRect(gearX + 3.5F, cy + 2.4F, 11.0F, 1.4F, 0.7F, iconCol);
        Render2D.drawCircle(gearX + 7.5F, cy + 3.1F, 1.6F, iconCol);

        // Search Bar placed to the left of the Settings button ("в поиске чуть левее")
        float searchW = 125.0F;
        float searchH = 18.0F;
        float searchX = gearX - 10.0F - searchW;

        boolean isHovered = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= gearY && mouseY <= gearY + searchH;
        int boxBg = ColorUtil.rgba(255, 255, 255, (int) ((searchFocused ? 18 : (isHovered ? 12 : 8)) * alphaVal));
        int boxBorder = searchFocused ? ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));

        Render2D.drawRoundedRect(searchX, gearY, searchW, searchH, 4.5F, boxBg);
        Render2D.drawRoundedOutline(searchX, gearY, searchW, searchH, 4.5F, 0.65F, boxBorder);

        // Magnifying glass icon
        int scCol = searchFocused ? accentColor : 0xFF98A8C0;
        Render2D.drawCircleOutline(searchX + 9.5F, gearY + 8.5F, 3.0F, 0.85F, scCol);
        Render2D.drawRoundedRect(searchX + 11.5F, gearY + 10.5F, 3.2F, 1.1F, 0.55F, scCol);

        String displayText = searchQuery.isEmpty() ? (searchFocused ? "" : "Поиск...") : searchQuery;
        int textCol = searchQuery.isEmpty() && !searchFocused ? 0xFF8898B0 : 0xFFFFFFFF;
        Fonts.drawString(Fonts.SF_MEDIUM, displayText + (searchFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), searchX + 19.0F, gearY + 5.0F, 5.6F, textCol);
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

        if (module.isEnabled()) {
            int cardBg = ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 24 : 14) * alphaVal));
            int cardOutline = ColorUtil.withAlpha(accentColor, (int) ((isHovered ? 180 : 130) * alphaVal));

            Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
            Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 0.85F, cardOutline);
        } else {
            int cardBg = ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 14 : 7) * alphaVal));
            int cardOutline = ColorUtil.rgba(255, 255, 255, (int) ((isHovered ? 22 : 12) * alphaVal));

            Render2D.drawRoundedRect(x, y, w, h, 6.0F, cardBg);
            Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 0.65F, cardOutline);
        }

        // Module Name
        int titleCol = module.isEnabled() ? 0xFFFFFFFF : 0xFFB8C8D8;
        Fonts.drawString(Fonts.SF_MEDIUM, module.getName(), x + 10.0F, y + 8.5F, 6.8F, titleCol);

        // Subtitle / Description
        String desc = module.getDescription();
        if (desc == null || desc.isEmpty()) desc = "Нажмите чтобы включить";
        if (desc.length() > 28) desc = desc.substring(0, 25) + "...";
        Fonts.drawString(Fonts.SF_MEDIUM, desc, x + 10.0F, y + 20.0F, 4.6F, 0xFF7A8B9E);

        // Switch Toggle Button (Right side)
        float switchW = 24.0F;
        float switchH = 13.0F;
        float switchX = x + w - switchW - 9.0F;
        float switchY = y + 10.5F;

        int switchBg = module.isEnabled() ? ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal));
        Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 6.5F, switchBg);
        Render2D.drawRoundedOutline(switchX, switchY, switchW, switchH, 6.5F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (35 * alphaVal)));

        float knobX = module.isEnabled() ? switchX + switchW - 6.5F : switchX + 6.5F;
        Render2D.drawCircle(knobX, switchY + switchH / 2.0F, 4.5F, 0xFFFFFFFF);

        // Expanded Settings Section
        if (expandVal > 0.01F) {
            float settingsY = y + 33.0F;
            Render2D.drawRoundedRect(x + 6.0F, settingsY, w - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (expandVal * 14 * alphaVal)));

            Render2DUtil.pushScissor(x, settingsY, w, h - 33.0F);
            try {
                float curSetY = settingsY + 3.0F;
                for (Setting<?> setting : module.getSettings()) {
                    float sH = getSettingHeight(setting);
                    renderSetting(setting, x + 8.0F, curSetY, w - 16.0F, sH, mouseX, mouseY, alphaVal * expandVal, accentColor);
                    curSetY += sH;
                }
            } finally {
                Render2DUtil.popScissor();
            }
        }
    }

    private void renderSetting(Setting<?> setting, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        if (setting instanceof CheckBox cb) {
            Fonts.drawString(Fonts.SF_MEDIUM, cb.getName(), x + 4.0F, y + 4.5F, 5.2F, 0xFFD0E0F0);

            float switchW = 20.0F;
            float switchH = 11.0F;
            float switchX = x + w - switchW - 4.0F;
            float switchY = y + 2.5F;

            int switchBg = cb.getValue() ? ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal));
            int switchOutline = cb.getValue() ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));
            Render2D.drawRoundedRect(switchX, switchY, switchW, switchH, 5.5F, switchBg);
            Render2D.drawRoundedOutline(switchX, switchY, switchW, switchH, 5.5F, 0.6F, switchOutline);

            float knobX = cb.getValue() ? (switchX + switchW - 5.5F) : (switchX + 5.5F);
            Render2D.drawCircle(knobX, switchY + switchH / 2.0F, 3.8F, 0xFFFFFFFF);
        } else if (setting instanceof SliderSetting slider) {
            Fonts.drawString(Fonts.SF_MEDIUM, slider.getName(), x + 4.0F, y + 2.0F, 5.0F, 0xFFD0E0F0);

            String valStr = String.format(Locale.US, "%.1f", slider.getValue());
            float valW = Fonts.SF_MEDIUM.getWidth(valStr, 4.8F);
            Fonts.drawString(Fonts.SF_MEDIUM, valStr, x + w - 4.0F - valW, y + 2.0F, 4.8F, 0xFFA0B4C8);

            float trackY = y + 11.5F;
            float trackH = 3.5F;
            Render2D.drawRoundedRect(x + 4.0F, trackY, w - 8.0F, trackH, 1.75F, ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal)));

            float pct = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
            pct = Math.clamp(pct, 0.0F, 1.0F);
            float fillW = (w - 8.0F) * pct;

            if (fillW > 0.5F) {
                Render2D.drawRoundedRect(x + 4.0F, trackY, fillW, trackH, 1.75F, ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal)));
            }

            Render2D.drawCircle(x + 4.0F + fillW, trackY + trackH / 2.0F, 3.5F, 0xFFFFFFFF);
        } else if (setting instanceof ModeSetting mode) {
            Fonts.drawString(Fonts.SF_MEDIUM, mode.getName(), x + 4.0F, y + 4.5F, 5.2F, 0xFFD0E0F0);

            String val = mode.getValue();
            float valW = Fonts.SF_MEDIUM.getWidth(val, 5.0F);
            float btnW = Math.max(36.0F, valW + 14.0F);
            float btnX = x + w - btnW - 4.0F;
            float btnY = y + 2.5F;

            boolean isOpen = (this.activeDropdownMode == mode);
            int bg = isOpen ? ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal));
            Render2D.drawRoundedRect(btnX, btnY, btnW, 11.5F, 3.0F, bg);
            Render2D.drawRoundedOutline(btnX, btnY, btnW, 11.5F, 3.0F, 0.6F, isOpen ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));
            Fonts.drawString(Fonts.SF_MEDIUM, val, btnX + 4.0F, btnY + 2.5F, 4.8F, 0xFFFFFFFF);
            Fonts.drawString(Fonts.ICONS, IconUse.DOWN.glyph, btnX + btnW - 8.0F, btnY + 2.8F, 4.2F, isOpen ? 0xFFFFFFFF : 0xFF90A4B8);
        } else if (setting instanceof BindSetting bind) {
            Fonts.drawString(Fonts.SF_MEDIUM, bind.getName(), x + 4.0F, y + 4.5F, 5.2F, 0xFFD0E0F0);

            String keyText = (this.activeBindingSetting == bind) ? "[...]" : (bind.getValue().isEmpty() ? "NONE" : KeyUtil.getKeyName(bind.getValue().get(0)));
            float valW = Fonts.SF_MEDIUM.getWidth(keyText, 5.0F);
            float btnW = Math.max(26.0F, valW + 8.0F);
            float btnX = x + w - btnW - 4.0F;
            float btnY = y + 2.5F;

            int bg = (this.activeBindingSetting == bind) ? ColorUtil.withAlpha(accentColor, (int) (200 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal));
            Render2D.drawRoundedRect(btnX, btnY, btnW, 11.5F, 3.0F, bg);
            Render2D.drawRoundedOutline(btnX, btnY, btnW, 11.5F, 3.0F, 0.6F, ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, keyText, btnX + btnW / 2.0F, btnY + 2.5F, 4.8F, 0xFFFFFFFF);
        } else if (setting instanceof ColorSetting color) {
            Fonts.drawString(Fonts.SF_MEDIUM, color.getName(), x + 4.0F, y + 4.5F, 5.2F, 0xFFD0E0F0);

            float pillW = 22.0F;
            float pillH = 10.0F;
            float pillX = x + w - pillW - 4.0F;
            float pillY = y + 3.0F;

            Render2D.drawRoundedRect(pillX, pillY, pillW, pillH, 3.0F, color.getValue());
            Render2D.drawRoundedOutline(pillX, pillY, pillW, pillH, 3.0F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (50 * alphaVal)));
        }
    }

    public void openSettingsModal() {
        this.settingsModalOpen = true;
        this.activeEditingColorSetting = null;
        this.bindingClickGuiKey = false;
        syncPickerFromCurrent();
    }

    public void openColorSettingModal(ColorSetting setting) {
        this.settingsModalOpen = true;
        this.activeEditingColorSetting = setting;
        this.bindingClickGuiKey = false;
        float[] hsv = ColorUtil.toHsv(setting.getValue());
        this.pickerHue = hsv[0];
        this.pickerSat = hsv[1];
        this.pickerBri = hsv[2];
    }

    private void syncPickerFromCurrent() {
        int currentColor = editingSecondaryColor ? Theme.getSecondaryColor() : Theme.getAccentColor();
        float[] hsv = ColorUtil.toHsv(currentColor);
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

        // Dim background behind modal (soft subtle veil, not opaque black)
        Render2D.drawRoundedRect(0, 0, screenW, screenH, 0.0F, ColorUtil.rgba(0, 0, 0, (int) (45 * alphaVal)));

        // Shadow and translucent liquid glass body
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 12.0F, 24.0F, ColorUtil.rgba(0, 0, 0, (int) (150 * alphaVal)));
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 12.0F, 24.0F, ColorUtil.rgba(14, 18, 28, (int) (130 * alphaVal)), alphaVal);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 12.0F, ColorUtil.rgba(14, 18, 28, (int) (135 * alphaVal)));
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 12.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 12.0F, 0.6F, ColorUtil.withAlpha(accentColor, (int) (60 * alphaVal)));

        // Header: Settings Icon + Title + Close Button
        float headY = modalY + 8.0F;
        float iconX = modalX + 13.0F;
        float iconY = headY + 2.0F;

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

        // 2D Color Picker (Saturation / Brightness Field)
        float fieldX = modalX + 14.0F;
        float fieldW = 145.0F;
        float fieldH = 100.0F;

        int pureHue = ColorUtil.fromHsv(pickerHue, 1.0F, 1.0F, 255);
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
            Render2D.drawGradientRound(hueX, hueY + i * stepH, hueW, stepH + 0.5F, 1.5F, c1, c1, c2, c2);
        }
        Render2D.drawRoundedOutline(hueX, hueY, hueW, hueH, 3.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int) (40 * alphaVal)));

        // Handle indicator on Hue Bar
        float hueHandleY = Math.clamp(hueY + pickerHue * hueH, hueY + 2.0F, hueY + hueH - 2.0F);
        Render2D.drawRoundedRect(hueX - 1.5F, hueHandleY - 1.5F, hueW + 3.0F, 3.0F, 1.5F, 0xFFFFFFFF);
        Render2D.drawRoundedOutline(hueX - 1.5F, hueHandleY - 1.5F, hueW + 3.0F, 3.0F, 1.5F, 0.65F, 0xFF000000);

        // Right side info & Quick Presets
        float rightX = hueX + hueW + 12.0F;
        float rightW = modalW - (rightX - modalX) - 14.0F;

        int finalColor = ColorUtil.fromHsv(pickerHue, pickerSat, pickerBri, 255);

        // Color Preview Box
        Render2D.drawRoundedRect(rightX, fieldY, rightW, 22.0F, 4.0F, finalColor);
        Render2D.drawRoundedOutline(rightX, fieldY, rightW, 22.0F, 4.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int) (40 * alphaVal)));

        String hexText = String.format("#%06X", (finalColor & 0xFFFFFF));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, hexText, rightX + rightW / 2.0F, fieldY + 6.0F, 6.2F, 0xFFFFFFFF);

        // Quick Swatches
        Fonts.drawString(Fonts.SF_MEDIUM, "Быстрые цвета:", rightX, fieldY + 31.0F, 5.0F, 0xFFA0B4C8);

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
            Render2D.drawRoundedRect(px, py, pSize, pSize, 3.0F, presets[i]);
            Render2D.drawRoundedOutline(px, py, pSize, pSize, 3.0F, 0.6F, ColorUtil.rgba(255, 255, 255, (int) (40 * alphaVal)));
        }
    }

    private void renderModuleModal(int screenW, int screenH, int mouseX, int mouseY, float alphaVal, int accentColor) {
        if (moduleModalModule == null) return;

        float modalW = 180.0F;
        float modalH = 98.0F;
        float modalX = (screenW - modalW) / 2.0F;
        float modalY = (screenH - modalH) / 2.0F;

        // Dim background behind modal (soft subtle veil, not opaque black)
        Render2D.drawRoundedRect(0, 0, screenW, screenH, 0.0F, ColorUtil.rgba(0, 0, 0, (int) (40 * alphaVal)));

        // Shadow and translucent liquid glass body
        Render2D.drawShadow(modalX, modalY, modalW, modalH, 10.0F, 20.0F, ColorUtil.rgba(0, 0, 0, (int) (150 * alphaVal)));
        Render2D.drawBlur(modalX, modalY, modalW, modalH, 10.0F, 20.0F, ColorUtil.rgba(14, 18, 26, (int) (130 * alphaVal)), alphaVal);
        Render2D.drawRoundedRect(modalX, modalY, modalW, modalH, 10.0F, ColorUtil.rgba(14, 18, 26, (int) (135 * alphaVal)));
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 10.0F, 0.8F, ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));
        Render2D.drawRoundedOutline(modalX, modalY, modalW, modalH, 10.0F, 0.6F, ColorUtil.withAlpha(accentColor, (int) (60 * alphaVal)));

        // Header: :: dots + Module Name + Close button
        float headY = modalY + 8.0F;
        float dotX = modalX + 11.0F;
        float dotY = headY + 2.0F;
        int dotCol = 0xFF7A8B9E;
        Render2D.drawCircle(dotX, dotY, 1.2F, dotCol);
        Render2D.drawCircle(dotX + 3.2F, dotY, 1.2F, dotCol);
        Render2D.drawCircle(dotX, dotY + 3.2F, 1.2F, dotCol);
        Render2D.drawCircle(dotX + 3.2F, dotY + 3.2F, 1.2F, dotCol);
        Render2D.drawCircle(dotX, dotY + 6.4F, 1.2F, dotCol);
        Render2D.drawCircle(dotX + 3.2F, dotY + 6.4F, 1.2F, dotCol);

        Fonts.drawString(Fonts.SF_MEDIUM, moduleModalModule.getName(), dotX + 9.0F, headY + 1.5F, 6.2F, 0xFFFFFFFF);

        // Close Button ✕
        float closeX = modalX + modalW - 20.0F;
        float closeY = modalY + 6.0F;
        float closeSize = 13.0F;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize;
        Render2D.drawRoundedRect(closeX, closeY, closeSize, closeSize, 3.0F, closeHover ? ColorUtil.rgba(240, 70, 70, (int) (190 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (12 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✕", closeX + closeSize / 2.0F, closeY + 2.0F, 5.5F, 0xFFFFFFFF);

        // Divider
        Render2D.drawRoundedRect(modalX + 10.0F, modalY + 22.0F, modalW - 20.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));

        // Row 1: Бинд
        float r1Y = modalY + 28.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Бинд", modalX + 12.0F, r1Y + 3.0F, 5.5F, 0xFFD0E0F0);

        float bindBtnW = 44.0F;
        float bindBtnH = 15.0F;
        float bindBtnX = modalX + modalW - 12.0F - bindBtnW;
        boolean bindHover = mouseX >= bindBtnX && mouseX <= bindBtnX + bindBtnW && mouseY >= r1Y && mouseY <= r1Y + bindBtnH;

        int bindBg = moduleModalBinding ? ColorUtil.withAlpha(accentColor, (int) (210 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((bindHover ? 22 : 14) * alphaVal));
        Render2D.drawRoundedRect(bindBtnX, r1Y, bindBtnW, bindBtnH, 4.0F, bindBg);
        Render2D.drawRoundedOutline(bindBtnX, r1Y, bindBtnW, bindBtnH, 4.0F, 0.6F, moduleModalBinding ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));

        String bindText;
        if (moduleModalBinding) {
            bindText = "[...]";
        } else if (moduleModalModule.getBind() != null && !moduleModalModule.getBind().isEmpty()) {
            bindText = KeyUtil.getKeyName(moduleModalModule.getBind().get(0));
        } else {
            bindText = "n/a";
        }
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, bindText, bindBtnX + bindBtnW / 2.0F, r1Y + 3.5F, 4.8F, 0xFFFFFFFF);

        // Row 2: Видимость (Hidden from HUD)
        float r2Y = modalY + 50.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Видимость", modalX + 12.0F, r2Y + 3.0F, 5.5F, 0xFFD0E0F0);

        boolean visibleOnHud = !moduleModalModule.isHiddenFromHud();
        float visW = 22.0F;
        float visH = 12.0F;
        float visX = modalX + modalW - 12.0F - visW;
        float visY = r2Y + 1.5F;

        int visBg = visibleOnHud ? ColorUtil.withAlpha(accentColor, (int) (220 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal));
        Render2D.drawRoundedRect(visX, visY, visW, visH, 6.0F, visBg);
        Render2D.drawRoundedOutline(visX, visY, visW, visH, 6.0F, 0.6F, visibleOnHud ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal)));

        float visKnobX = visibleOnHud ? (visX + visW - 6.0F) : (visX + 6.0F);
        Render2D.drawCircle(visKnobX, visY + visH / 2.0F, 4.2F, 0xFFFFFFFF);

        // Row 3: Тип (Hold / Toggle)
        float r3Y = modalY + 72.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, "Тип", modalX + 12.0F, r3Y + 3.5F, 5.5F, 0xFFD0E0F0);

        boolean isHold = "Hold".equalsIgnoreCase(moduleModalModule.getBindType());
        float segContainerW = 76.0F;
        float segContainerH = 16.0F;
        float segX = modalX + modalW - 12.0F - segContainerW;
        float segY = r3Y;

        Render2D.drawRoundedRect(segX, segY, segContainerW, segContainerH, 4.5F, ColorUtil.rgba(255, 255, 255, (int) (12 * alphaVal)));
        Render2D.drawRoundedOutline(segX, segY, segContainerW, segContainerH, 4.5F, 0.6F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        float segBtnW = 37.0F;
        float holdX = segX + 1.0F;
        float toggleX = segX + 1.0F + segBtnW;

        // Hold segment
        if (isHold) {
            Render2D.drawRoundedRect(holdX, segY + 1.0F, segBtnW, segContainerH - 2.0F, 3.5F, ColorUtil.withAlpha(accentColor, (int) (210 * alphaVal)));
        }
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Hold", holdX + segBtnW / 2.0F, segY + 3.5F, 4.8F, isHold ? 0xFFFFFFFF : 0xFF8090A4);

        // Toggle segment
        if (!isHold) {
            Render2D.drawRoundedRect(toggleX, segY + 1.0F, segBtnW, segContainerH - 2.0F, 3.5F, ColorUtil.withAlpha(accentColor, (int) (210 * alphaVal)));
        }
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Toggle", toggleX + segBtnW / 2.0F, segY + 3.5F, 4.8F, !isHold ? 0xFFFFFFFF : 0xFF8090A4);
    }

    private void renderModeDropdown(int screenW, int screenH, int mouseX, int mouseY, float alphaVal, int accentColor) {
        if (this.activeDropdownMode == null) return;

        float dx = this.dropdownPopupX;
        float dy = this.dropdownPopupY;
        float dw = this.dropdownPopupW;
        float dh = this.dropdownPopupH;

        // Soft drop shadow
        Render2D.drawShadow(dx, dy, dw, dh, 6.0F, 12.0F, ColorUtil.rgba(0, 0, 0, (int) (140 * alphaVal)));
        // Translucent liquid glass body (slightly darker than GUI, but clearly translucent)
        Render2D.drawBlur(dx, dy, dw, dh, 6.0F, 16.0F, ColorUtil.rgba(14, 18, 28, (int) (140 * alphaVal)), alphaVal);
        Render2D.drawRoundedRect(dx, dy, dw, dh, 6.0F, ColorUtil.rgba(14, 18, 28, (int) (145 * alphaVal)));
        Render2D.drawRoundedOutline(dx, dy, dw, dh, 6.0F, 0.75F, ColorUtil.rgba(255, 255, 255, (int) (28 * alphaVal)));
        Render2D.drawRoundedOutline(dx, dy, dw, dh, 6.0F, 0.5F, ColorUtil.withAlpha(accentColor, (int) (55 * alphaVal)));

        float rowY = dy + 3.0F;
        float rowH = 15.0F;

        for (int i = 0; i < this.activeDropdownMode.getModes().size(); i++) {
            String opt = this.activeDropdownMode.getModes().get(i);
            boolean isSelected = opt.equalsIgnoreCase(this.activeDropdownMode.getValue());
            boolean isHovered = mouseX >= dx + 2.0F && mouseX <= dx + dw - 2.0F && mouseY >= rowY && mouseY <= rowY + rowH;

            if (isHovered) {
                Render2D.drawRoundedRect(dx + 2.5F, rowY, dw - 5.0F, rowH, 3.5F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));
            } else if (isSelected) {
                Render2D.drawRoundedRect(dx + 2.5F, rowY, dw - 5.0F, rowH, 3.5F, ColorUtil.withAlpha(accentColor, (int) (40 * alphaVal)));
            }

            int textColor = isSelected ? 0xFFFFFFFF : (isHovered ? 0xFFE0EBF8 : 0xFFA0B4C8);
            Fonts.drawString(Fonts.SF_MEDIUM, opt, dx + 6.0F, rowY + 3.5F, 5.0F, textColor);

            if (isSelected) {
                Fonts.drawString(Fonts.ICONS, IconUse.CHECK.glyph, dx + dw - 12.0F, rowY + 3.5F, 5.2F, accentColor);
            }

            rowY += 16.0F;
        }
    }

    private void renderConfigsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        Render2D.drawRoundedRect(x, y, w, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (90 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        // Top Action Bar: Create Config Input + Buttons
        float row1Y = y + 8.0F;
        float inputW = w - 145.0F;
        float inputH = 19.0F;

        // New Config Input Field
        int inputBg = ColorUtil.rgba(255, 255, 255, (int) ((newConfigFocused ? 18 : 10) * alphaVal));
        int inputBorder = newConfigFocused ? ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));
        Render2D.drawRoundedRect(x + 10.0F, row1Y, inputW, inputH, 4.0F, inputBg);
        Render2D.drawRoundedOutline(x + 10.0F, row1Y, inputW, inputH, 4.0F, 0.65F, inputBorder);

        String newCfgDisplay = newConfigInput.isEmpty() ? (newConfigFocused ? "" : "Имя нового конфига...") : newConfigInput;
        int newCfgColor = newConfigInput.isEmpty() && !newConfigFocused ? 0xFF8090A4 : 0xFFFFFFFF;
        Fonts.drawString(Fonts.SF_MEDIUM, newCfgDisplay + (newConfigFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), x + 16.0F, row1Y + 5.0F, 5.5F, newCfgColor);

        // "+ Создать" Button
        float createBtnX = x + 10.0F + inputW + 6.0F;
        float createBtnW = 64.0F;
        boolean createHover = mouseX >= createBtnX && mouseX <= createBtnX + createBtnW && mouseY >= row1Y && mouseY <= row1Y + inputH;
        int createBg = ColorUtil.withAlpha(accentColor, (int) ((createHover ? 220 : 180) * alphaVal));
        Render2D.drawRoundedRect(createBtnX, row1Y, createBtnW, inputH, 4.0F, createBg);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "+ Создать", createBtnX + createBtnW / 2.0F, row1Y + 5.0F, 5.4F, 0xFFFFFFFF);

        // "Папка" Button
        float folderBtnX = createBtnX + createBtnW + 5.0F;
        float folderBtnW = 50.0F;
        boolean folderHover = mouseX >= folderBtnX && mouseX <= folderBtnX + folderBtnW && mouseY >= row1Y && mouseY <= row1Y + inputH;
        int folderBg = ColorUtil.rgba(255, 255, 255, (int) ((folderHover ? 26 : 14) * alphaVal));
        Render2D.drawRoundedRect(folderBtnX, row1Y, folderBtnW, inputH, 4.0F, folderBg);
        Render2D.drawRoundedOutline(folderBtnX, row1Y, folderBtnW, inputH, 4.0F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (30 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Папка", folderBtnX + folderBtnW / 2.0F, row1Y + 5.0F, 5.4F, 0xFFD0E0F0);

        // Row 2: Share Key Import Field + "Импорт по ключу" Button
        float row2Y = row1Y + 24.0F;
        float keyInputW = w - 110.0F;
        float keyInputH = 18.0F;

        int keyBg = ColorUtil.rgba(255, 255, 255, (int) ((shareKeyFocused ? 18 : 10) * alphaVal));
        int keyBorder = shareKeyFocused ? ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));
        Render2D.drawRoundedRect(x + 10.0F, row2Y, keyInputW, keyInputH, 4.0F, keyBg);
        Render2D.drawRoundedOutline(x + 10.0F, row2Y, keyInputW, keyInputH, 4.0F, 0.65F, keyBorder);

        String keyDisplay = shareKeyInput.isEmpty() ? (shareKeyFocused ? "" : "Вставьте ключ (ERR$...)...") : shareKeyInput;
        int keyColor = shareKeyInput.isEmpty() && !shareKeyFocused ? 0xFF8090A4 : 0xFFFFFFFF;
        Fonts.drawString(Fonts.SF_MEDIUM, keyDisplay + (shareKeyFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), x + 16.0F, row2Y + 4.5F, 5.2F, keyColor);

        // "Импорт" Button
        float importBtnX = x + 10.0F + keyInputW + 6.0F;
        float importBtnW = w - 20.0F - keyInputW - 6.0F;
        boolean importHover = mouseX >= importBtnX && mouseX <= importBtnX + importBtnW && mouseY >= row2Y && mouseY <= row2Y + keyInputH;
        int importBg = ColorUtil.rgba(255, 255, 255, (int) ((importHover ? 26 : 14) * alphaVal));
        Render2D.drawRoundedRect(importBtnX, row2Y, importBtnW, keyInputH, 4.0F, importBg);
        Render2D.drawRoundedOutline(importBtnX, row2Y, importBtnW, keyInputH, 4.0F, 0.65F, ColorUtil.withAlpha(accentColor, (int) (160 * alphaVal)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Импорт", importBtnX + importBtnW / 2.0F, row2Y + 4.5F, 5.2F, 0xFFFFFFFF);

        // Divider
        float divY = row2Y + 23.0F;
        Render2D.drawRoundedRect(x + 10.0F, divY, w - 20.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));

        // Configs List Section
        List<String> configs = (Client.INSTANCE != null && Client.INSTANCE.configManager != null) ? Client.INSTANCE.configManager.getAvailableConfigs() : List.of("default");
        String curConfig = (Client.INSTANCE != null && Client.INSTANCE.configManager != null) ? Client.INSTANCE.configManager.getCurrentConfig() : "default";

        Fonts.drawString(Fonts.SF_MEDIUM, "Сохраненные конфигурации (" + configs.size() + ")", x + 12.0F, divY + 5.0F, 5.5F, 0xFFA0B4C8);

        float listY = divY + 16.0F;
        float listH = (y + h) - listY - 8.0F;

        Render2DUtil.pushScissor(x + 8.0F, listY, w - 16.0F, listH);
        try {
            float startItemY = listY - configsScrollAnim.getValue();
            float itemY = startItemY;

            for (String cfg : configs) {
                boolean isCurrent = cfg.equalsIgnoreCase(curConfig);
                float itemH = 25.0F;
                float itemW = w - 24.0F;
                float itemX = x + 12.0F;

                if (itemY + itemH >= listY && itemY <= listY + listH) {
                    boolean itemHover = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + itemH;
                    int cardBg = isCurrent ? ColorUtil.rgba(28, 36, 52, (int) (180 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((itemHover ? 18 : 10) * alphaVal));
                    int cardBorder = isCurrent ? ColorUtil.withAlpha(accentColor, (int) (140 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal));

                    Render2D.drawRoundedRect(itemX, itemY, itemW, itemH, 4.5F, cardBg);
                    Render2D.drawRoundedOutline(itemX, itemY, itemW, itemH, 4.5F, 0.65F, cardBorder);

                    // Name & Badge
                    Fonts.drawString(Fonts.SF_MEDIUM, cfg + ".json", itemX + 10.0F, itemY + 8.0F, 6.2F, 0xFFFFFFFF);
                    if (isCurrent) {
                        float curTagX = itemX + 16.0F + Fonts.SF_MEDIUM.getWidth(cfg + ".json", 6.2F);
                        Render2D.drawRoundedRect(curTagX, itemY + 6.0F, 36.0F, 12.0F, 3.0F, ColorUtil.withAlpha(accentColor, (int) (180 * alphaVal)));
                        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Текущий", curTagX + 18.0F, itemY + 8.0F, 4.5F, 0xFFFFFFFF);
                    }

                    // Action Buttons: [Загрузить] [Сохранить] [Поделиться] [✕]
                    float btnH = 15.0F;
                    float btnY = itemY + 5.0F;

                    // Delete [✕]
                    float delBtnW = 18.0F;
                    float delBtnX = itemX + itemW - delBtnW - 6.0F;
                    boolean delHover = mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
                    Render2D.drawRoundedRect(delBtnX, btnY, delBtnW, btnH, 3.0F, delHover ? ColorUtil.rgba(240, 70, 70, (int) (200 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (16 * alphaVal)));
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✕", delBtnX + delBtnW / 2.0F, btnY + 3.0F, 5.2F, 0xFFFFFFFF);

                    // Share Key [Поделиться]
                    float shareBtnW = 44.0F;
                    float shareBtnX = delBtnX - shareBtnW - 4.0F;
                    boolean shareHover = mouseX >= shareBtnX && mouseX <= shareBtnX + shareBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
                    Render2D.drawRoundedRect(shareBtnX, btnY, shareBtnW, btnH, 3.0F, ColorUtil.rgba(255, 255, 255, (int) ((shareHover ? 26 : 14) * alphaVal)));
                    Render2D.drawRoundedOutline(shareBtnX, btnY, shareBtnW, btnH, 3.0F, 0.6F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Ключ", shareBtnX + shareBtnW / 2.0F, btnY + 3.5F, 4.8F, 0xFFB0C4DE);

                    // Save [Сохранить]
                    float saveBtnW = 48.0F;
                    float saveBtnX = shareBtnX - saveBtnW - 4.0F;
                    boolean saveHover = mouseX >= saveBtnX && mouseX <= saveBtnX + saveBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
                    Render2D.drawRoundedRect(saveBtnX, btnY, saveBtnW, btnH, 3.0F, ColorUtil.rgba(255, 255, 255, (int) ((saveHover ? 26 : 14) * alphaVal)));
                    Render2D.drawRoundedOutline(saveBtnX, btnY, saveBtnW, btnH, 3.0F, 0.6F, ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal)));
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Сохранить", saveBtnX + saveBtnW / 2.0F, btnY + 3.5F, 4.8F, 0xFFB0C4DE);

                    // Load [Загрузить]
                    float loadBtnW = 48.0F;
                    float loadBtnX = saveBtnX - loadBtnW - 4.0F;
                    boolean loadHover = mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= btnY && mouseY <= btnY + btnH;
                    Render2D.drawRoundedRect(loadBtnX, btnY, loadBtnW, btnH, 3.0F, ColorUtil.withAlpha(accentColor, (int) ((loadHover ? 220 : 180) * alphaVal)));
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Загрузить", loadBtnX + loadBtnW / 2.0F, btnY + 3.5F, 4.8F, 0xFFFFFFFF);
                }

                itemY += itemH + 4.0F;
            }

            float totalH = itemY - startItemY;
            this.configsScrollTarget = Math.clamp(this.configsScrollTarget, 0.0F, Math.max(0.0F, totalH - listH));
            this.configsScrollAnim.setTarget(this.configsScrollTarget);
        } finally {
            Render2DUtil.popScissor();
        }
    }

    private void renderFriendsTab(float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        Render2D.drawRoundedRect(x, y, w, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (90 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, w, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        // Top Row: Input field for friend nickname + "+ Добавить" button
        float row1Y = y + 8.0F;
        float inputW = w - 96.0F;
        float inputH = 20.0F;

        int inputBg = ColorUtil.rgba(255, 255, 255, (int) ((friendInputFocused ? 18 : 10) * alphaVal));
        int inputBorder = friendInputFocused ? ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal));
        Render2D.drawRoundedRect(x + 10.0F, row1Y, inputW, inputH, 4.0F, inputBg);
        Render2D.drawRoundedOutline(x + 10.0F, row1Y, inputW, inputH, 4.0F, 0.65F, inputBorder);

        String friendDisplay = friendInput.isEmpty() ? (friendInputFocused ? "" : "Введите никнейм игрока...") : friendInput;
        int friendColor = friendInput.isEmpty() && !friendInputFocused ? 0xFF8090A4 : 0xFFFFFFFF;
        Fonts.drawString(Fonts.SF_MEDIUM, friendDisplay + (friendInputFocused && (System.currentTimeMillis() % 1000 > 500) ? "_" : ""), x + 16.0F, row1Y + 5.5F, 5.6F, friendColor);

        // "+ Добавить" Button
        float addBtnX = x + 10.0F + inputW + 6.0F;
        float addBtnW = 70.0F;
        boolean addHover = mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= row1Y && mouseY <= row1Y + inputH;
        int addBg = ColorUtil.withAlpha(accentColor, (int) ((addHover ? 220 : 180) * alphaVal));
        Render2D.drawRoundedRect(addBtnX, row1Y, addBtnW, inputH, 4.0F, addBg);
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "+ Добавить", addBtnX + addBtnW / 2.0F, row1Y + 5.5F, 5.4F, 0xFFFFFFFF);

        // Divider
        float divY = row1Y + 26.0F;
        Render2D.drawRoundedRect(x + 10.0F, divY, w - 20.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (14 * alphaVal)));

        Set<String> friends = FriendManager.getInstance().getFriends();
        Fonts.drawString(Fonts.SF_MEDIUM, "Список друзей (" + friends.size() + ")", x + 12.0F, divY + 6.0F, 5.6F, 0xFFA0B4C8);

        float listY = divY + 18.0F;
        float listH = (y + h) - listY - 8.0F;

        if (friends.isEmpty()) {
            Render2D.drawRoundedRect(x + 12.0F, listY + 10.0F, w - 24.0F, 65.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int) (8 * alphaVal)));
            Render2D.drawRoundedOutline(x + 12.0F, listY + 10.0F, w - 24.0F, 65.0F, 6.0F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal)));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Список друзей пуст", x + w / 2.0F, listY + 28.0F, 7.0F, 0xFFD0E0F0);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Введите никнейм выше или нажмите ПКМ по игроку в игре", x + w / 2.0F, listY + 44.0F, 5.0F, 0xFF8898B0);
            return;
        }

        Render2DUtil.pushScissor(x + 8.0F, listY, w - 16.0F, listH);
        try {
            float startItemY = listY - friendsScrollAnim.getValue();
            float itemY = startItemY;

            for (String f : friends) {
                float itemH = 26.0F;
                float itemW = w - 24.0F;
                float itemX = x + 12.0F;

                if (itemY + itemH >= listY && itemY <= listY + listH) {
                    boolean itemHover = mouseX >= itemX && mouseX <= itemX + itemW && mouseY >= itemY && mouseY <= itemY + itemH;
                    int cardBg = ColorUtil.rgba(255, 255, 255, (int) ((itemHover ? 18 : 10) * alphaVal));

                    Render2D.drawRoundedRect(itemX, itemY, itemW, itemH, 4.5F, cardBg);
                    Render2D.drawRoundedOutline(itemX, itemY, itemW, itemH, 4.5F, 0.65F, ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal)));

                    // Avatar head icon
                    Render2D.drawCustomAvatar(itemX + 5.0F, itemY + 4.0F, 18.0F, 3.5F, alphaVal);

                    // Nickname
                    Fonts.drawString(Fonts.SF_MEDIUM, f, itemX + 28.0F, itemY + 8.5F, 6.2F, 0xFFFFFFFF);

                    // Badge "Друг"
                    float badgeX = itemX + 32.0F + Fonts.SF_MEDIUM.getWidth(f, 6.2F);
                    Render2D.drawRoundedRect(badgeX, itemY + 7.0F, 28.0F, 12.0F, 3.0F, ColorUtil.withAlpha(accentColor, (int) (160 * alphaVal)));
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Друг", badgeX + 14.0F, itemY + 8.5F, 4.5F, 0xFFFFFFFF);

                    // Delete Button [✕ Удалить]
                    float delBtnW = 54.0F;
                    float delBtnH = 15.0F;
                    float delBtnX = itemX + itemW - delBtnW - 6.0F;
                    float delBtnY = itemY + 5.5F;
                    boolean delHover = mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= delBtnY && mouseY <= delBtnY + delBtnH;
                    Render2D.drawRoundedRect(delBtnX, delBtnY, delBtnW, delBtnH, 3.0F, delHover ? ColorUtil.rgba(240, 70, 70, (int) (200 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (16 * alphaVal)));
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, "✕ Удалить", delBtnX + delBtnW / 2.0F, delBtnY + 3.5F, 4.8F, 0xFFFFFFFF);
                }

                itemY += itemH + 4.0F;
            }

            float totalH = itemY - startItemY;
            this.friendsScrollTarget = Math.clamp(this.friendsScrollTarget, 0.0F, Math.max(0.0F, totalH - listH));
            this.friendsScrollAnim.setTarget(this.friendsScrollTarget);
        } finally {
            Render2DUtil.popScissor();
        }
    }

    private void renderCosmeticsTab(GuiGraphicsExtractor extractor, float x, float y, float w, float h, int mouseX, int mouseY, float alphaVal, int accentColor) {
        // Layout: Left side -> Cosmetics List with Filter Pills; Right side -> 3D Character Preview
        float previewW = 138.0F;
        float listW = w - previewW - 8.0F;

        // --- Left Container: Cosmetics ---
        Render2D.drawRoundedRect(x, y, listW, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (90 * alphaVal)));
        Render2D.drawRoundedOutline(x, y, listW, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (20 * alphaVal)));

        // Category Filter Pills
        String[] filters = new String[]{"Все", "Модели", "Крылья", "Шапки", "Маски", "Питомцы"};
        float pillX = x + 8.0F;
        float pillY = y + 7.0F;
        float pillH = 15.0F;

        for (String fName : filters) {
            boolean isSel = fName.equals(this.cosmeticFilter);
            float fTextW = Fonts.SF_MEDIUM.getWidth(fName, 5.0F);
            float fPillW = fTextW + 10.0F;

            boolean pHover = mouseX >= pillX && mouseX <= pillX + fPillW && mouseY >= pillY && mouseY <= pillY + pillH;
            int pBg = isSel ? ColorUtil.withAlpha(accentColor, (int) (190 * alphaVal)) : (pHover ? ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (12 * alphaVal)));
            int pOutline = isSel ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (24 * alphaVal));

            Render2D.drawRoundedRect(pillX, pillY, fPillW, pillH, 3.5F, pBg);
            Render2D.drawRoundedOutline(pillX, pillY, fPillW, pillH, 3.5F, 0.6F, pOutline);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, fName, pillX + fPillW / 2.0F, pillY + 3.0F, 4.8F, isSel ? 0xFFFFFFFF : 0xFFB0C0D0);

            pillX += fPillW + 4.0F;
        }

        // Cosmetics Items List
        float listY = y + 27.0F;
        float listH = h - 33.0F;

        List<CosmeticItem> allItems = CosmeticsManager.getInstance().getCosmetics();
        List<CosmeticItem> filtered = new ArrayList<>();
        for (CosmeticItem it : allItems) {
            if ("Все".equals(cosmeticFilter)) {
                filtered.add(it);
            } else if ("Модели".equals(cosmeticFilter) && it.getType() == CosmeticType.MODEL) {
                filtered.add(it);
            } else if ("Крылья".equals(cosmeticFilter) && it.getType() == CosmeticType.WINGS) {
                filtered.add(it);
            } else if ("Шапки".equals(cosmeticFilter) && it.getType() == CosmeticType.HAT) {
                filtered.add(it);
            } else if ("Маски".equals(cosmeticFilter) && it.getType() == CosmeticType.MASK) {
                filtered.add(it);
            } else if ("Питомцы".equals(cosmeticFilter) && it.getType() == CosmeticType.PET) {
                filtered.add(it);
            }
        }

        Render2DUtil.pushScissor(x + 4.0F, listY, listW - 8.0F, listH);
        try {
            float startItemY = listY - cosmeticsScrollAnim.getValue();
            float itemY = startItemY;

            for (CosmeticItem item : filtered) {
                float itemH = 26.0F;
                float itemCardW = listW - 16.0F;
                float itemX = x + 8.0F;

                if (itemY + itemH >= listY && itemY <= listY + listH) {
                    boolean isHover = mouseX >= itemX && mouseX <= itemX + itemCardW && mouseY >= itemY && mouseY <= itemY + itemH;
                    int cardBg = item.isEnabled() ? ColorUtil.rgba(24, 34, 50, (int) (180 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((isHover ? 18 : 10) * alphaVal));
                    int cardBorder = item.isEnabled() ? ColorUtil.withAlpha(accentColor, (int) (140 * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) (18 * alphaVal));

                    Render2D.drawRoundedRect(itemX, itemY, itemCardW, itemH, 4.5F, cardBg);
                    Render2D.drawRoundedOutline(itemX, itemY, itemCardW, itemH, 4.5F, 0.65F, cardBorder);

                    // Indicator color circle
                    Render2D.drawCircle(itemX + 10.0F, itemY + itemH / 2.0F, 3.2F, item.getColor());

                    // Name & Type
                    Fonts.drawString(Fonts.SF_MEDIUM, item.getName(), itemX + 18.0F, itemY + 4.5F, 5.8F, item.isEnabled() ? 0xFFFFFFFF : 0xFFC0D0E0);
                    Fonts.drawString(Fonts.SF_MEDIUM, item.getType().getDisplayName(), itemX + 18.0F, itemY + 14.5F, 4.2F, 0xFF8090A4);

                    // Toggle Button [ Надеть / Снять ]
                    float btnW = item.isEnabled() ? 46.0F : 42.0F;
                    float btnH = 14.5F;
                    float btnX = itemX + itemCardW - btnW - 6.0F;
                    float btnY = itemY + 5.5F;
                    boolean bHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;

                    int bBg = item.isEnabled() ? ColorUtil.withAlpha(accentColor, (int) ((bHover ? 230 : 190) * alphaVal)) : ColorUtil.rgba(255, 255, 255, (int) ((bHover ? 24 : 14) * alphaVal));
                    Render2D.drawRoundedRect(btnX, btnY, btnW, btnH, 3.5F, bBg);
                    Render2D.drawRoundedOutline(btnX, btnY, btnW, btnH, 3.5F, 0.6F, item.isEnabled() ? accentColor : ColorUtil.rgba(255, 255, 255, (int) (26 * alphaVal)));

                    String btnLabel = item.isEnabled() ? "Снять" : "Надеть";
                    Fonts.drawCenteredString(Fonts.SF_MEDIUM, btnLabel, btnX + btnW / 2.0F, btnY + 3.0F, 4.8F, 0xFFFFFFFF);
                }

                itemY += itemH + 4.0F;
            }

            float totalH = itemY - startItemY;
            this.cosmeticsScrollTarget = Math.clamp(this.cosmeticsScrollTarget, 0.0F, Math.max(0.0F, totalH - listH));
            this.cosmeticsScrollAnim.setTarget(this.cosmeticsScrollTarget);
        } finally {
            Render2DUtil.popScissor();
        }

        // --- Right Container: 3D Character Preview Panel ---
        float charX = x + listW + 8.0F;
        Render2D.drawRoundedRect(charX, y, previewW, h, 7.0F, ColorUtil.rgba(20, 24, 34, (int) (95 * alphaVal)));
        Render2D.drawRoundedOutline(charX, y, previewW, h, 7.0F, 0.7F, ColorUtil.rgba(255, 255, 255, (int) (22 * alphaVal)));

        // Header inside preview
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "3D Персонаж", charX + previewW / 2.0F, y + 8.0F, 6.5F, 0xFFFFFFFF);

        String activeModelName = (CustomModels.INSTANCE != null && CustomModels.INSTANCE.isEnabled() && !CustomModels.NONE.equalsIgnoreCase(CustomModels.INSTANCE.model.getValue()))
                ? CustomModels.INSTANCE.model.getValue() : "Стив";
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Модель: " + activeModelName, charX + previewW / 2.0F, y + 19.0F, 4.6F, accentColor);

        // Flush 2D rendering before extracting 3D entity
        Render2DUtil.flush();

        // Extract 3D Player Entity standing still (rotated freely via drag, whole body aligned without separate head spinning)
        if (this.minecraft != null && this.minecraft.player != null) {
            int pX1 = (int) (charX + 8.0F);
            int pY1 = (int) (y + 30.0F);
            int pX2 = (int) (charX + previewW - 8.0F);
            int pY2 = (int) (y + h - 24.0F);

            try {
                net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher = this.minecraft.getEntityRenderDispatcher();
                net.minecraft.client.renderer.entity.EntityRenderer renderer = dispatcher.getRenderer(this.minecraft.player);
                net.minecraft.client.renderer.entity.state.EntityRenderState renderState = renderer.createRenderState(this.minecraft.player, 1.0F);
                renderState.shadowPieces.clear();
                renderState.outlineColor = 0;
                if (renderState instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingState) {
                    livingState.bodyRot = 180.0F + this.cosmeticsPlayerYaw;
                    livingState.yRot = 0.0F;
                    livingState.xRot = 0.0F;
                    livingState.walkAnimationPos = 0.0F;
                    livingState.walkAnimationSpeed = 0.0F;
                    livingState.boundingBoxWidth /= livingState.scale;
                    livingState.boundingBoxHeight /= livingState.scale;
                    livingState.scale = 1.0F;
                }
                org.joml.Quaternionf q1 = new org.joml.Quaternionf().rotateZ((float) Math.PI);
                org.joml.Quaternionf q2 = new org.joml.Quaternionf();
                org.joml.Vector3f translation = new org.joml.Vector3f(0.0F, renderState.boundingBoxHeight / 2.0F + 0.0625F, 0.0F);
                extractor.entity(renderState, 52.0F, translation, q1, q2, pX1, pY1, pX2, pY2);
            } catch (Exception ignored) {}
        }

        // Bottom rotation hint
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, "Зажмите ЛКМ для вращения", charX + previewW / 2.0F, y + h - 13.0F, 4.4F, 0xFF7A8B9E);
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

        // Handle Active Mode Dropdown Popup Clicks
        if (this.activeDropdownMode != null) {
            float dx = this.dropdownPopupX;
            float dy = this.dropdownPopupY;
            float dw = this.dropdownPopupW;
            float dh = this.dropdownPopupH;

            if (mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + dh) {
                float rowY = dy + 3.0F;
                float rowH = 15.0F;
                for (String opt : this.activeDropdownMode.getModes()) {
                    if (mouseY >= rowY && mouseY <= rowY + rowH) {
                        this.activeDropdownMode.setValue(opt);
                        error.util.client.ClientSoundPlayer.playModePreview(opt);
                        this.activeDropdownMode = null;
                        if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                            Client.INSTANCE.configManager.autoSave();
                        }
                        return true;
                    }
                    rowY += 16.0F;
                }
                return true;
            }
            // Click outside closes dropdown
            this.activeDropdownMode = null;
        }

        // Handle Module Middle-Click Bind Modal Clicks if Open
        if (this.moduleModalOpen && this.moduleModalModule != null) {
            float mModalW = 180.0F;
            float mModalH = 98.0F;
            float mModalX = (screenW - mModalW) / 2.0F;
            float mModalY = (screenH - mModalH) / 2.0F;

            // Close button [✕]
            float closeX = mModalX + mModalW - 20.0F;
            float closeY = mModalY + 6.0F;
            float closeSize = 13.0F;
            if (mouseX >= closeX && mouseX <= closeX + closeSize && mouseY >= closeY && mouseY <= closeY + closeSize) {
                this.moduleModalOpen = false;
                this.moduleModalBinding = false;
                return true;
            }

            // Row 1: Бинд button
            float r1Y = mModalY + 28.0F;
            float bindBtnW = 44.0F;
            float bindBtnH = 15.0F;
            float bindBtnX = mModalX + mModalW - 12.0F - bindBtnW;
            if (mouseX >= bindBtnX && mouseX <= bindBtnX + bindBtnW && mouseY >= r1Y && mouseY <= r1Y + bindBtnH) {
                this.moduleModalBinding = !this.moduleModalBinding;
                return true;
            }

            // Row 2: Видимость (Hidden from HUD) toggle switch
            float r2Y = mModalY + 50.0F;
            float visW = 22.0F;
            float visH = 12.0F;
            float visX = mModalX + mModalW - 12.0F - visW;
            float visY = r2Y + 1.5F;
            if (mouseX >= visX && mouseX <= visX + visW && mouseY >= visY && mouseY <= visY + visH) {
                this.moduleModalModule.setHiddenFromHud(!this.moduleModalModule.isHiddenFromHud());
                if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    Client.INSTANCE.configManager.autoSave();
                }
                return true;
            }

            // Row 3: Тип (Hold / Toggle) buttons
            float r3Y = mModalY + 72.0F;
            float segContainerW = 76.0F;
            float segContainerH = 16.0F;
            float segX = mModalX + mModalW - 12.0F - segContainerW;
            float segY = r3Y;
            float segBtnW = 37.0F;
            float holdX = segX + 1.0F;
            float toggleX = segX + 1.0F + segBtnW;
            if (mouseX >= holdX && mouseX <= holdX + segBtnW && mouseY >= segY && mouseY <= segY + segContainerH) {
                this.moduleModalModule.setBindType("Hold");
                if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    Client.INSTANCE.configManager.autoSave();
                }
                return true;
            }
            if (mouseX >= toggleX && mouseX <= toggleX + segBtnW && mouseY >= segY && mouseY <= segY + segContainerH) {
                this.moduleModalModule.setBindType("Toggle");
                if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    Client.INSTANCE.configManager.autoSave();
                }
                return true;
            }

            // Consume click inside modal
            if (mouseX >= mModalX && mouseX <= mModalX + mModalW && mouseY >= mModalY && mouseY <= mModalY + mModalH) {
                return true;
            }

            // Click outside closes modal
            this.moduleModalOpen = false;
            this.moduleModalBinding = false;
            return true;
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

                // Consume click if inside modal window
                if (mouseX >= modalX && mouseX <= modalX + modalW && mouseY >= modalY && mouseY <= modalY + modalH) {
                    return true;
                }

                // Close modal if click outside
                this.settingsModalOpen = false;
                this.bindingClickGuiKey = false;
                this.activeEditingColorSetting = null;
                this.draggingPicker = DragTarget.NONE;
                return true;
            }

            // Gear Settings Button strictly at the TOP-RIGHT corner
            float gearSize = 18.0F;
            float gearX = x + WINDOW_W - 14.0F - gearSize;
            float gearY = y + 14.0F;
            if (mouseX >= gearX && mouseX <= gearX + gearSize && mouseY >= gearY && mouseY <= gearY + gearSize) {
                openSettingsModal();
                return true;
            }

            // Search Bar Click (Left of Settings Button)
            float searchW = 125.0F;
            float searchH = 18.0F;
            float searchX = gearX - 10.0F - searchW;
            float searchY = y + 14.0F;

            if (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH) {
                this.searchFocused = true;
                this.newConfigFocused = false;
                this.shareKeyFocused = false;
                this.friendInputFocused = false;
                return true;
            } else {
                this.searchFocused = false;
            }

            // Sidebar Category Clicks
            Category[] categories = Arrays.stream(Category.values()).filter(c -> c != Category.THEMES).toArray(Category[]::new);
            float catY = y + 36.0F;
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

            float contentX = x + SIDEBAR_W + 12.0F;
            float contentY = y + 44.0F;
            float contentW = WINDOW_W - SIDEBAR_W - 24.0F;
            float contentH = WINDOW_H - 54.0F;

            // Handle Interactions for CONFIGS Tab
            if (activeCategory == Category.CONFIGS) {
                float row1Y = contentY + 8.0F;
                float inputW = contentW - 145.0F;
                float inputH = 19.0F;

                // New Config Input Field Click
                if (mouseX >= contentX + 10.0F && mouseX <= contentX + 10.0F + inputW && mouseY >= row1Y && mouseY <= row1Y + inputH) {
                    this.newConfigFocused = true;
                    this.shareKeyFocused = false;
                    this.friendInputFocused = false;
                    return true;
                } else {
                    this.newConfigFocused = false;
                }

                // "+ Создать" Button Click
                float createBtnX = contentX + 10.0F + inputW + 6.0F;
                float createBtnW = 64.0F;
                if (mouseX >= createBtnX && mouseX <= createBtnX + createBtnW && mouseY >= row1Y && mouseY <= row1Y + inputH) {
                    if (!newConfigInput.trim().isEmpty() && Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                        String name = newConfigInput.trim();
                        Client.INSTANCE.configManager.saveConfig(name, true);
                        newConfigInput = "";
                    }
                    return true;
                }

                // "Папка" Button Click
                float folderBtnX = createBtnX + createBtnW + 5.0F;
                float folderBtnW = 50.0F;
                if (mouseX >= folderBtnX && mouseX <= folderBtnX + folderBtnW && mouseY >= row1Y && mouseY <= row1Y + inputH) {
                    if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                        Client.INSTANCE.configManager.openFolder();
                    }
                    return true;
                }

                // Key Input Field Click
                float row2Y = row1Y + 24.0F;
                float keyInputW = contentW - 110.0F;
                float keyInputH = 18.0F;

                if (mouseX >= contentX + 10.0F && mouseX <= contentX + 10.0F + keyInputW && mouseY >= row2Y && mouseY <= row2Y + keyInputH) {
                    this.shareKeyFocused = true;
                    this.newConfigFocused = false;
                    this.friendInputFocused = false;
                    return true;
                } else {
                    this.shareKeyFocused = false;
                }

                // "Импорт" Button Click
                float importBtnX = contentX + 10.0F + keyInputW + 6.0F;
                float importBtnW = contentW - 20.0F - keyInputW - 6.0F;
                if (mouseX >= importBtnX && mouseX <= importBtnX + importBtnW && mouseY >= row2Y && mouseY <= row2Y + keyInputH) {
                    handleImportConfigKey();
                    return true;
                }

                // Config Rows Action Buttons
                float divY = row2Y + 23.0F;
                float listY = divY + 16.0F;
                float listH = (contentY + contentH) - listY - 8.0F;

                if (mouseY >= listY && mouseY <= listY + listH && Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    List<String> configs = Client.INSTANCE.configManager.getAvailableConfigs();
                    float itemY = listY - configsScrollAnim.getValue();

                    for (String cfg : configs) {
                        float itemH = 25.0F;
                        float itemW = contentW - 24.0F;
                        float itemX = contentX + 12.0F;

                        if (itemY + itemH >= listY && itemY <= listY + listH) {
                            float btnH = 15.0F;
                            float btnY = itemY + 5.0F;

                            // Delete [✕]
                            float delBtnW = 18.0F;
                            float delBtnX = itemX + itemW - delBtnW - 6.0F;
                            if (mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                                Client.INSTANCE.configManager.deleteConfig(cfg);
                                ChatUtil.info("Конфиг '" + cfg + "' удален.");
                                return true;
                            }

                            // Share Key [Ключ]
                            float shareBtnW = 44.0F;
                            float shareBtnX = delBtnX - shareBtnW - 4.0F;
                            if (mouseX >= shareBtnX && mouseX <= shareBtnX + shareBtnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                                handleShareConfig(cfg);
                                return true;
                            }

                            // Save [Сохранить]
                            float saveBtnW = 48.0F;
                            float saveBtnX = shareBtnX - saveBtnW - 4.0F;
                            if (mouseX >= saveBtnX && mouseX <= saveBtnX + saveBtnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                                Client.INSTANCE.configManager.saveConfig(cfg, true);
                                return true;
                            }

                            // Load [Загрузить]
                            float loadBtnW = 48.0F;
                            float loadBtnX = saveBtnX - loadBtnW - 4.0F;
                            if (mouseX >= loadBtnX && mouseX <= loadBtnX + loadBtnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                                Client.INSTANCE.configManager.loadConfig(cfg, true);
                                return true;
                            }
                        }

                        itemY += itemH + 4.0F;
                    }
                }
                return true;
            }

            // Handle Interactions for FRIENDS Tab
            if (activeCategory == Category.FRIENDS) {
                float row1Y = contentY + 8.0F;
                float inputW = contentW - 96.0F;
                float inputH = 20.0F;

                if (mouseX >= contentX + 10.0F && mouseX <= contentX + 10.0F + inputW && mouseY >= row1Y && mouseY <= row1Y + inputH) {
                    this.friendInputFocused = true;
                    this.newConfigFocused = false;
                    this.shareKeyFocused = false;
                    return true;
                } else {
                    this.friendInputFocused = false;
                }

                // "+ Добавить" Button Click
                float addBtnX = contentX + 10.0F + inputW + 6.0F;
                float addBtnW = 70.0F;
                if (mouseX >= addBtnX && mouseX <= addBtnX + addBtnW && mouseY >= row1Y && mouseY <= row1Y + inputH) {
                    if (!friendInput.trim().isEmpty()) {
                        String name = friendInput.trim();
                        FriendManager.getInstance().addFriend(name);
                        ChatUtil.success("Игрок '" + name + "' добавлен в друзья!");
                        friendInput = "";
                    }
                    return true;
                }

                // Friends List Delete Clicks
                float divY = row1Y + 26.0F;
                float listY = divY + 18.0F;
                float listH = (contentY + contentH) - listY - 8.0F;

                if (mouseY >= listY && mouseY <= listY + listH) {
                    Set<String> friends = FriendManager.getInstance().getFriends();
                    float itemY = listY - friendsScrollAnim.getValue();

                    for (String f : friends) {
                        float itemH = 26.0F;
                        float itemW = contentW - 24.0F;
                        float itemX = contentX + 12.0F;

                        if (itemY + itemH >= listY && itemY <= listY + listH) {
                            float delBtnW = 54.0F;
                            float delBtnH = 15.0F;
                            float delBtnX = itemX + itemW - delBtnW - 6.0F;
                            float delBtnY = itemY + 5.5F;

                            if (mouseX >= delBtnX && mouseX <= delBtnX + delBtnW && mouseY >= delBtnY && mouseY <= delBtnY + delBtnH) {
                                FriendManager.getInstance().removeFriend(f);
                                ChatUtil.info("Игрок '" + f + "' удален из друзей.");
                                return true;
                            }
                        }

                        itemY += itemH + 4.0F;
                    }
                }
                return true;
            }

            // Handle Interactions for COSMETICS Tab
            if (activeCategory == Category.COSMETICS) {
                float previewW = 138.0F;
                float listW = contentW - previewW - 8.0F;

                // Category Filter Pills Clicks
                String[] filters = new String[]{"Все", "Модели", "Крылья", "Шапки", "Маски", "Питомцы"};
                float pillX = contentX + 8.0F;
                float pillY = contentY + 7.0F;
                float pillH = 15.0F;

                for (String fName : filters) {
                    float fTextW = Fonts.SF_MEDIUM.getWidth(fName, 5.0F);
                    float fPillW = fTextW + 10.0F;

                    if (mouseX >= pillX && mouseX <= pillX + fPillW && mouseY >= pillY && mouseY <= pillY + pillH) {
                        this.cosmeticFilter = fName;
                        return true;
                    }

                    pillX += fPillW + 4.0F;
                }

                // Items List Toggle Clicks
                float listY = contentY + 27.0F;
                float listH = contentH - 33.0F;

                if (mouseX >= contentX && mouseX <= contentX + listW && mouseY >= listY && mouseY <= listY + listH) {
                    List<CosmeticItem> allItems = CosmeticsManager.getInstance().getCosmetics();
                    List<CosmeticItem> filtered = new ArrayList<>();
                    for (CosmeticItem it : allItems) {
                        if ("Все".equals(cosmeticFilter)) {
                            filtered.add(it);
                        } else if ("Модели".equals(cosmeticFilter) && it.getType() == CosmeticType.MODEL) {
                            filtered.add(it);
                        } else if ("Крылья".equals(cosmeticFilter) && it.getType() == CosmeticType.WINGS) {
                            filtered.add(it);
                        } else if ("Шапки".equals(cosmeticFilter) && it.getType() == CosmeticType.HAT) {
                            filtered.add(it);
                        } else if ("Маски".equals(cosmeticFilter) && it.getType() == CosmeticType.MASK) {
                            filtered.add(it);
                        } else if ("Питомцы".equals(cosmeticFilter) && it.getType() == CosmeticType.PET) {
                            filtered.add(it);
                        }
                    }

                    float itemY = listY - cosmeticsScrollAnim.getValue();

                    for (CosmeticItem item : filtered) {
                        float itemH = 26.0F;
                        float itemCardW = listW - 16.0F;
                        float itemX = contentX + 8.0F;

                        if (itemY + itemH >= listY && itemY <= listY + listH) {
                            float btnW = item.isEnabled() ? 46.0F : 42.0F;
                            float btnH = 14.5F;
                            float btnX = itemX + itemCardW - btnW - 6.0F;
                            float btnY = itemY + 5.5F;

                            // Clicking toggle button or card row toggles cosmetic
                            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                                item.setEnabled(!item.isEnabled());
                                CosmeticsManager.getInstance().onToggleCosmetic(item);
                                return true;
                            }
                        }

                        itemY += itemH + 4.0F;
                    }
                }

                // Character Preview Drag Rotation Click
                float charX = contentX + listW + 8.0F;
                if (mouseX >= charX && mouseX <= charX + previewW && mouseY >= contentY && mouseY <= contentY + contentH) {
                    this.draggingCosmeticsPlayer = true;
                    this.lastCosmeticsMouseX = mouseX;
                    return true;
                }

                return true;
            }

            // Modules Clicks (Default module grid)
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
                                if (this.activeDropdownMode == ms) {
                                    this.activeDropdownMode = null;
                                } else {
                                    this.activeDropdownMode = ms;
                                    float valW = Fonts.SF_MEDIUM.getWidth(ms.getValue(), 5.0F);
                                    float btnW = Math.max(36.0F, valW + 14.0F);
                                    float setW = cardW - 16.0F;
                                    float btnX = cardX + 8.0F + setW - btnW - 4.0F;
                                    float btnY = setY + 2.5F;

                                    float maxOptW = 65.0F;
                                    for (String opt : ms.getModes()) {
                                        maxOptW = Math.max(maxOptW, Fonts.SF_MEDIUM.getWidth(opt, 5.0F) + 26.0F);
                                    }
                                    this.dropdownPopupW = Math.max(btnW + 10.0F, maxOptW);
                                    this.dropdownPopupX = Math.min(screenW - this.dropdownPopupW - 8.0F, Math.max(8.0F, btnX + btnW - this.dropdownPopupW));
                                    this.dropdownPopupH = ms.getModes().size() * 16.0F + 6.0F;
                                    this.dropdownPopupY = btnY + 13.0F;
                                    if (this.dropdownPopupY + this.dropdownPopupH > screenH - 8.0F) {
                                        this.dropdownPopupY = Math.max(8.0F, btnY - this.dropdownPopupH - 2.0F);
                                    }
                                    error.util.client.ClientSoundPlayer.playGuiClick();
                                }
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

            // Right-click expands module settings
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
        } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (this.settingsModalOpen || this.moduleModalOpen) return true;

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
                    this.moduleModalModule = module;
                    this.moduleModalOpen = true;
                    this.moduleModalBinding = false;
                    return true;
                }

                if (isRightColumn) {
                    rightY += cardH + 7.0F;
                } else {
                    leftY += cardH + 7.0F;
                }
            }
            return true;
        }
        return super.mouseClicked(event, isLeftClick);
    }

    private void handleShareConfig(String cfgName) {
        try {
            if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                File file = Client.INSTANCE.configManager.getConfigFile(cfgName);
                if (file.exists()) {
                    String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    String key = "ERR$" + Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                    GLFW.glfwSetClipboardString(Minecraft.getInstance().getWindow().handle(), key);
                    ChatUtil.success("Ключ конфига '" + cfgName + "' скопирован в буфер обмена!");
                }
            }
        } catch (Exception e) {
            ChatUtil.error("Не удалось скопировать ключ: " + e.getMessage());
        }
    }

    private void handleImportConfigKey() {
        if (shareKeyInput == null || shareKeyInput.trim().isEmpty()) {
            ChatUtil.error("Сначала вставьте ключ конфигурации!");
            return;
        }

        String raw = shareKeyInput.trim();
        try {
            if (raw.startsWith("ERR$")) {
                raw = raw.substring(4);
            }
            byte[] bytes = Base64.getUrlDecoder().decode(raw);
            String json = new String(bytes, StandardCharsets.UTF_8);

            String importName = "shared_" + (System.currentTimeMillis() % 10000);
            if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                File file = Client.INSTANCE.configManager.getConfigFile(importName);
                Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
                Client.INSTANCE.configManager.loadConfig(importName, true);
                shareKeyInput = "";
                ChatUtil.success("Конфиг успешно импортирован как '" + importName + "'!");
            }
        } catch (Exception e) {
            ChatUtil.error("Неверный формат ключа конфигурации!");
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.settingsModalOpen) return true;

        if (activeCategory == Category.CONFIGS) {
            this.configsScrollTarget -= (float) (scrollY * 24.0D);
            return true;
        } else if (activeCategory == Category.FRIENDS) {
            this.friendsScrollTarget -= (float) (scrollY * 24.0D);
            return true;
        } else if (activeCategory == Category.COSMETICS) {
            this.cosmeticsScrollTarget -= (float) (scrollY * 24.0D);
            return true;
        } else {
            this.scrollTarget -= (float) (scrollY * 24.0D);
            return true;
        }
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

        // Handle Module Middle-Click Bind Modal Key Handler
        if (this.moduleModalOpen && this.moduleModalModule != null) {
            if (this.moduleModalBinding) {
                if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                    this.moduleModalModule.getBind().clear();
                } else {
                    this.moduleModalModule.getBind().setSingle(event.key());
                }
                this.moduleModalBinding = false;
                if (Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    Client.INSTANCE.configManager.autoSave();
                }
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.moduleModalOpen = false;
                return true;
            }
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

        // Clipboard Paste Support (Ctrl+V)
        boolean isPaste = (event.key() == GLFW.GLFW_KEY_V && (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0);

        // Handle Configs Text Inputs
        if (this.newConfigFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.newConfigInput.isEmpty()) {
                    this.newConfigInput = this.newConfigInput.substring(0, this.newConfigInput.length() - 1);
                }
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ENTER) {
                if (!newConfigInput.trim().isEmpty() && Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                    Client.INSTANCE.configManager.saveConfig(newConfigInput.trim(), true);
                    newConfigInput = "";
                }
                return true;
            } else if (isPaste) {
                String clip = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
                if (clip != null) newConfigInput += clip.trim();
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.newConfigFocused = false;
                return true;
            }
        }

        if (this.shareKeyFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.shareKeyInput.isEmpty()) {
                    this.shareKeyInput = this.shareKeyInput.substring(0, this.shareKeyInput.length() - 1);
                }
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ENTER) {
                handleImportConfigKey();
                return true;
            } else if (isPaste) {
                String clip = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
                if (clip != null) shareKeyInput += clip.trim();
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.shareKeyFocused = false;
                return true;
            }
        }

        // Handle Friends Text Input
        if (this.friendInputFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.friendInput.isEmpty()) {
                    this.friendInput = this.friendInput.substring(0, this.friendInput.length() - 1);
                }
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ENTER) {
                if (!friendInput.trim().isEmpty()) {
                    FriendManager.getInstance().addFriend(friendInput.trim());
                    ChatUtil.success("Игрок '" + friendInput.trim() + "' добавлен в друзья!");
                    friendInput = "";
                }
                return true;
            } else if (isPaste) {
                String clip = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
                if (clip != null) friendInput += clip.trim();
                return true;
            } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                this.friendInputFocused = false;
                return true;
            }
        }

        // Handle Search Bar Input
        if (this.searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.searchQuery.isEmpty()) {
                    this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
                }
                return true;
            } else if (isPaste) {
                String clip = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
                if (clip != null) searchQuery += clip.trim();
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
        int codePoint = event.codepoint();
        if (codePoint >= 32 && codePoint != 127) {
            String ch = new String(Character.toChars(codePoint));
            if (this.newConfigFocused) {
                this.newConfigInput += ch;
                return true;
            } else if (this.shareKeyFocused) {
                this.shareKeyInput += ch;
                return true;
            } else if (this.friendInputFocused) {
                this.friendInput += ch;
                return true;
            } else if (this.searchFocused) {
                this.searchQuery += ch;
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
