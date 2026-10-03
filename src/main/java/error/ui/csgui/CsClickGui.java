package error.ui.csgui;

import error.ui.csgui.CsClickGuiModel.*;
import error.util.RenderExtend;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
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
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CsClickGui extends Screen {

    private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("error", "images/logo.png");
    private static int rememberedCategory = 0;

    private final UiDrawList ui = new UiDrawList();
    private final List<CategoryModel> categories = CsClickGuiModel.create();
    private int categoryIndex = rememberedCategory;

    private final CsSettingsPanel settingsPanel = new CsSettingsPanel();
    private final CsRubberSegment categorySegment = new CsRubberSegment();

    private ModuleModel selectedModule = null;
    private KeyModel bindingKey = null;

    private String searchQuery = "";
    private boolean searchFocused = false;

    private float scrollOffset = 0.0F;

    public CsClickGui() {
        super(Component.literal("CS ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        if (categoryIndex >= categories.size()) categoryIndex = 0;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void extractPanorama(GuiGraphicsExtractor extractor, float partialTick) {
    }

    public void render(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width;
        int screenHeight = this.height;

        boolean mouseDown = this.minecraft.mouseHandler.isLeftPressed();

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            int themeAccent = Theme.getAccentColor();

            // 1. Fullscreen Ambient Glass Blur Backdrop
            Render2D.drawRect(0, 0, screenWidth, screenHeight, ColorUtil.rgba(8, 6, 14, 160));
            Render2D.drawBlur(0, 0, screenWidth, screenHeight, 0.0F, 18.0F, ColorUtil.rgba(0, 0, 0, 100), 1.0F);

            // 2. Main CS ClickGUI Window Bounds
            float guiW = Math.min(680.0F, screenWidth - 40.0F);
            float guiH = Math.min(460.0F, screenHeight - 40.0F);
            float guiX = (screenWidth - guiW) / 2.0F;
            float guiY = (screenHeight - guiH) / 2.0F;

            int shadowCol = ColorUtil.rgba(0, 0, 0, 170);
            int glassFill = ColorUtil.rgba(16, 14, 24, 230);
            int glassBorder = ColorUtil.rgba(255, 255, 255, 40);

            // Window Glass Frame
            ui.shadow(guiX, guiY, guiW, guiH, 8.0F, 12.0F, shadowCol)
              .blur(guiX, guiY, guiW, guiH, 8.0F, 18.0F, glassFill, 1.0F)
              .roundedRect(guiX, guiY, guiW, guiH, 8.0F, glassFill)
              .roundedOutline(guiX, guiY, guiW, guiH, 8.0F, 1.0F, glassBorder);

            // Specular top gloss line
            Render2D.drawRoundedRect(guiX + 10.0F, guiY + 1.0F, guiW - 20.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, 30));

            // 3. Top Header Bar: Logo + Rubber Category Segment Tabs + Search Bar
            float topHeaderH = 38.0F;

            // Brand Logo & Title
            float logoSize = 14.0F;
            Render2D.drawTexture(LOGO_TEXTURE, guiX + 12.0F, guiY + 12.0F, logoSize, logoSize, ColorUtil.rgba(255, 255, 255, 245));
            Fonts.drawString(Fonts.SF_MEDIUM, "Error DLC", guiX + 32.0F, guiY + 14.0F, 9.0F, ColorUtil.rgba(255, 255, 255, 245));

            // Category Rubber Tabs
            if (!categories.isEmpty()) {
                float segmentX = guiX + 125.0F;
                float segmentY = guiY + 19.0F;
                this.categoryIndex = categorySegment.draw(ui, categories, categoryIndex, segmentX, segmentY, 1.0F, themeAccent, mouseX, mouseY, mouseDown, false, partialTick);
                rememberedCategory = categoryIndex;
            }

            // Search Box (Right Header)
            float searchW = 120.0F;
            float searchH = 20.0F;
            float searchX = guiX + guiW - searchW - 12.0F;
            float searchY = guiY + 9.0F;

            int searchBg = searchFocused ? ColorUtil.rgba(32, 28, 44, 230) : ColorUtil.rgba(24, 22, 34, 210);
            ui.roundedRect(searchX, searchY, searchW, searchH, 4.0F, searchBg)
              .roundedOutline(searchX, searchY, searchW, searchH, 4.0F, 1.0F, searchFocused ? themeAccent : ColorUtil.rgba(255, 255, 255, 30));

            String searchPlaceholder = searchQuery.isEmpty() ? "Поиск..." : searchQuery;
            ui.text(searchX + 8.0F, searchY + 10.0F, 7.0F, searchQuery.isEmpty() ? 0xFF807890 : 0xFFFFFFFF, searchPlaceholder);

            // 4. Main Body Content Area
            float bodyY = guiY + topHeaderH;
            float bodyH = guiH - topHeaderH;

            CategoryModel curCategory = (categoryIndex >= 0 && categoryIndex < categories.size()) ? categories.get(categoryIndex) : null;

            if (curCategory != null) {
                float moduleListW = (selectedModule != null) ? (guiW - 220.0F) : guiW;

                // Render Module Grid Cards
                float gridPadding = 12.0F;
                float cardW = (moduleListW - gridPadding * 3.0F) / 2.0F;
                float cardH = 34.0F;

                List<ModuleModel> filteredModules = new ArrayList<>();
                for (ModuleModel m : curCategory.modules()) {
                    if (searchQuery.isEmpty() || m.name.toLowerCase().contains(searchQuery.toLowerCase())) {
                        filteredModules.add(m);
                    }
                }

                Render2D.pushScissor(guiX, bodyY, moduleListW, bodyH);

                float curCardX = guiX + gridPadding;
                float curCardY = bodyY + gridPadding - scrollOffset;
                int col = 0;

                for (ModuleModel m : filteredModules) {
                    boolean hovered = (mouseX >= curCardX && mouseX <= curCardX + cardW && mouseY >= curCardY && mouseY <= curCardY + cardH);

                    int cardFill = m.enabled
                            ? ColorUtil.rgba(24, 22, 36, 230)
                            : (hovered ? ColorUtil.rgba(22, 20, 30, 210) : ColorUtil.rgba(18, 16, 26, 190));
                    int cardBorder = m.enabled ? themeAccent : ColorUtil.rgba(255, 255, 255, 25);

                    ui.roundedRect(curCardX, curCardY, cardW, cardH, 5.0F, cardFill)
                      .roundedOutline(curCardX, curCardY, cardW, cardH, 5.0F, 1.0F, cardBorder);

                    // Module Name & Description
                    ui.strongText(curCardX + 10.0F, curCardY + 11.0F, 8.0F, m.enabled ? 0xFFFFFFFF : 0xFFC5C0D0, m.name)
                      .text(curCardX + 10.0F, curCardY + 22.0F, 6.0F, 0xFF858095, m.description);

                    // Toggle Switch Pill
                    float toggleW = 20.0F;
                    float toggleH = 10.0F;
                    float toggleX = curCardX + cardW - toggleW - 8.0F;
                    float toggleY = curCardY + (cardH - toggleH) / 2.0F;

                    int toggleBg = m.enabled ? themeAccent : ColorUtil.rgba(40, 36, 52, 220);
                    ui.roundedRect(toggleX, toggleY, toggleW, toggleH, 5.0F, toggleBg);

                    float knobSize = 8.0F;
                    float knobX = m.enabled ? (toggleX + toggleW - knobSize - 1.0F) : (toggleX + 1.0F);
                    float knobY = toggleY + 1.0F;
                    ui.roundedRect(knobX, knobY, knobSize, knobSize, 4.0F, 0xFFFFFFFF);

                    col++;
                    if (col >= 2) {
                        col = 0;
                        curCardX = guiX + gridPadding;
                        curCardY += cardH + 8.0F;
                    } else {
                        curCardX += cardW + gridPadding;
                    }
                }

                Render2D.popScissor();

                // Render Right Settings Drawer Panel if a module is selected
                if (selectedModule != null) {
                    float settingsW = 210.0F;
                    float settingsX = guiX + guiW - settingsW - 8.0F;
                    float settingsY = bodyY + 8.0F;
                    float settingsH = bodyH - 16.0F;

                    settingsPanel.draw(ui, selectedModule, settingsX, settingsY, settingsW, settingsH, 1.0F, mouseX, mouseY, mouseDown, false, bindingKey);
                }
            }

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        float guiW = Math.min(680.0F, this.width - 40.0F);
        float guiH = Math.min(460.0F, this.height - 40.0F);
        float guiX = (this.width - guiW) / 2.0F;
        float guiY = (this.height - guiH) / 2.0F;

        // Search Bar Focus
        float searchW = 120.0F;
        float searchH = 20.0F;
        float searchX = guiX + guiW - searchW - 12.0F;
        float searchY = guiY + 9.0F;
        this.searchFocused = (mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= searchY && mouseY <= searchY + searchH);

        // Check Module Clicks
        CategoryModel curCategory = (categoryIndex >= 0 && categoryIndex < categories.size()) ? categories.get(categoryIndex) : null;
        if (curCategory != null) {
            float topHeaderH = 38.0F;
            float bodyY = guiY + topHeaderH;
            float moduleListW = (selectedModule != null) ? (guiW - 220.0F) : guiW;
            float gridPadding = 12.0F;
            float cardW = (moduleListW - gridPadding * 3.0F) / 2.0F;
            float cardH = 34.0F;

            List<ModuleModel> filteredModules = new ArrayList<>();
            for (ModuleModel m : curCategory.modules()) {
                if (searchQuery.isEmpty() || m.name.toLowerCase().contains(searchQuery.toLowerCase())) {
                    filteredModules.add(m);
                }
            }

            float curCardX = guiX + gridPadding;
            float curCardY = bodyY + gridPadding - scrollOffset;
            int col = 0;

            for (ModuleModel m : filteredModules) {
                if (mouseX >= curCardX && mouseX <= curCardX + cardW && mouseY >= curCardY && mouseY <= curCardY + cardH) {
                    if (button == 0) {
                        m.toggle();
                        error.util.client.ClientSoundPlayer.playGuiClick();
                    } else if (button == 1) {
                        this.selectedModule = (this.selectedModule == m) ? null : m;
                        error.util.client.ClientSoundPlayer.playGuiClick();
                    }
                    return true;
                }

                col++;
                if (col >= 2) {
                    col = 0;
                    curCardX = guiX + gridPadding;
                    curCardY += cardH + 8.0F;
                } else {
                    curCardX += cardW + gridPadding;
                }
            }
        }

        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (bindingKey != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingKey.setKey(-1);
            } else {
                bindingKey.setKey(keyCode);
            }
            bindingKey = null;
            return true;
        }

        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreenAndShow(null);
            error.util.client.ClientSoundPlayer.playGuiClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char codePoint = (char) event.codepoint();
        if (searchFocused) {
            searchQuery += codePoint;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scrollOffset = Math.max(0.0F, this.scrollOffset - (float) verticalAmount * 16.0F);
        return true;
    }
}
