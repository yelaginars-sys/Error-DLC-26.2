package error.ui.shadertoy;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.BindSetting;
import error.ui.modern.ModernAnim;
import error.ui.modern.ModernTheme;
import error.util.RenderExtend;
import error.util.client.ClientSoundPlayer;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ShaderToyGui extends Screen {

    public static final float W = 540.0F;
    public static final float H = 320.0F;

    public enum ShaderStyle {
        AURORA("Аврора"),
        CYBERPUNK("Киберпанк"),
        NEBULA("Туманность"),
        MIDNIGHT("Полночь"),
        MINIMAL("Минимализм");

        private final String displayName;

        ShaderStyle(String name) {
            this.displayName = name;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum Page {
        MODULES, SETTINGS, THEMES
    }

    public record FontHolder(MsdfFont font, float size) {}

    public Page activePage = Page.MODULES;
    public Category activeCategory = Category.COMBAT;
    public Module selectedModule = null;
    public ShaderStyle currentShaderStyle = ShaderStyle.AURORA;

    public String searchQuery = "";
    public float leftScroll = 0.0F;
    public float maxLeftScroll = 0.0F;
    public float rightScroll = 0.0F;
    public float maxRightScroll = 0.0F;

    public float alpha = 1.0F;
    public float x = 0.0F;
    public float y = 0.0F;
    public float mouseX = 0.0F;
    public float mouseY = 0.0F;

    public Object focus = null;
    public String buffer = "";

    public Module bindingModule = null;
    public Setting<?> bindingSetting = null;

    private float animProgress = 0.0F;
    private boolean closing = false;

    private float dragX = 0.0F;
    private float dragY = 0.0F;
    private boolean dragging = false;
    private float dX = 0.0F;
    private float dY = 0.0F;

    private final List<HitBox> hitBoxes = new ArrayList<>();
    public static final Object FOCUS_SEARCH = "shadertoy_search";

    public ShaderToyGui() {
        super(Component.literal("ShaderToy Next-Gen ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        this.animProgress = 0.0F;
        this.closing = false;

        int screenWidth = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenHeight = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

        if (this.dragX == 0.0F && this.dragY == 0.0F) {
            this.dragX = (screenWidth - W) / 2.0F;
            this.dragY = (screenHeight - H) / 2.0F;
        }

        if (selectedModule == null && Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
            List<Module> mods = getModulesForCategory(activeCategory, "");
            if (!mods.isEmpty()) {
                selectedModule = mods.get(0);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int screenWidth = this.width > 0 ? this.width : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledWidth() : 854);
        int screenHeight = this.height > 0 ? this.height : (this.minecraft != null ? this.minecraft.getWindow().getGuiScaledHeight() : 480);

        ModernAnim.beginFrame();
        this.animProgress = ModernAnim.approach(this.animProgress, this.closing ? 0.0F : 1.0F, this.closing ? 14.0F : 12.0F);

        if (this.closing && this.animProgress < 0.01F) {
            ClientSoundPlayer.playGuiClose();
            this.minecraft.setScreenAndShow(null);
            return;
        }

        float easeAnim = ModernAnim.ease(this.animProgress);
        this.alpha = Math.max(0.0F, Math.min(1.0F, easeAnim));

        this.x = this.dragX;
        this.y = this.dragY;
        this.mouseX = (float) mouseX;
        this.mouseY = (float) mouseY;

        if (this.dragging) {
            this.dragX = mouseX - dX;
            this.dragY = mouseY - dY;
            this.dragX = Math.max(0.0F, Math.min(screenWidth - W, this.dragX));
            this.dragY = Math.max(0.0F, Math.min(screenHeight - H, this.dragY));
            this.x = this.dragX;
            this.y = this.dragY;
        }

        this.hitBoxes.clear();

        RenderExtend.enter2D(null, extractor, null);
        try {
            Render2DUtil.beginFrame();

            // 1. Fullscreen Dimming & Glass Blur
            rect(0, 0, screenWidth, screenHeight, 0.0F, ColorUtil.rgba(6, 7, 12, (int) (150 * alpha)));
            darkBlur(x, y, W, H, 14.0F);

            // 2. Animated ShaderToy Dynamic Background Effect
            renderShaderToyBackdrop(x, y, W, H);

            // 3. Glass Window Border & Outer Glow
            int accentCol = Theme.getAccentColor();
            int glowBorder = ColorUtil.rgba(ColorUtil.red(accentCol), ColorUtil.green(accentCol), ColorUtil.blue(accentCol), (int) (40 * alpha));
            outline(x, y, W, H, 12.0F, 0.5F, glowBorder);

            // 4. Sidebar (Category icons on far left)
            renderSidebar();

            // 5. Top Bar (Search box & User Profile)
            renderTopBar();

            // 6. Main Content Split View
            float leftColX = x + 34.0F;
            float leftColY = y + 8.0F;
            float leftColW = 98.0F;
            float leftColH = H - 16.0F;

            float rightPanelX = x + 138.0F;
            float rightPanelY = y + 28.0F;
            float rightPanelW = W - 146.0F;
            float rightPanelH = H - 34.0F;

            ShaderToyModulesPage.renderLeftColumn(this, leftColX, leftColY, leftColW, leftColH);
            ShaderToyModulesPage.renderRightSettings(this, rightPanelX, rightPanelY, rightPanelW, rightPanelH);

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderShaderToyBackdrop(float bx, float by, float bw, float bh) {
        float time = (System.currentTimeMillis() % 100000L) / 1000.0F;

        int accent = Theme.getAccentColor();
        int r = ColorUtil.red(accent);
        int g = ColorUtil.green(accent);
        int b = ColorUtil.blue(accent);

        int cTL, cTR, cBR, cBL;

        switch (currentShaderStyle) {
            case AURORA -> {
                // Dynamic procedural Aurora wave colors
                float sin1 = (float) Math.sin(time * 0.8F) * 0.5F + 0.5F;
                float cos1 = (float) Math.cos(time * 0.6F) * 0.5F + 0.5F;
                cTL = ColorUtil.rgba((int) (r * 0.8F + sin1 * 50), (int) (g * 0.6F + 60), (int) (b * 0.9F), (int) (180 * alpha));
                cTR = ColorUtil.rgba((int) (30 + cos1 * 80), (int) (140 + sin1 * 80), (int) (220), (int) (170 * alpha));
                cBR = ColorUtil.rgba((int) (12 + sin1 * 20), (int) (15 + cos1 * 20), (int) (30), (int) (190 * alpha));
                cBL = ColorUtil.rgba((int) (r * 0.5F), (int) (g * 0.4F), (int) (b * 0.8F), (int) (175 * alpha));
            }
            case CYBERPUNK -> {
                float pulse = (float) Math.sin(time * 1.5F) * 0.5F + 0.5F;
                cTL = ColorUtil.rgba(230, (int) (40 + pulse * 60), 120, (int) (180 * alpha));
                cTR = ColorUtil.rgba((int) (20 + pulse * 40), 200, 240, (int) (170 * alpha));
                cBR = ColorUtil.rgba(14, 16, 28, (int) (190 * alpha));
                cBL = ColorUtil.rgba(100, 20, 160, (int) (175 * alpha));
            }
            case NEBULA -> {
                float shift = (float) Math.cos(time * 0.5F) * 0.5F + 0.5F;
                cTL = ColorUtil.rgba((int) (120 + shift * 60), 30, (int) (180 + shift * 50), (int) (180 * alpha));
                cTR = ColorUtil.rgba(40, (int) (80 + shift * 80), 200, (int) (170 * alpha));
                cBR = ColorUtil.rgba(10, 12, 22, (int) (190 * alpha));
                cBL = ColorUtil.rgba((int) (80 + shift * 40), 15, (int) (100 + shift * 60), (int) (175 * alpha));
            }
            case MIDNIGHT -> {
                cTL = ColorUtil.rgba(18, 22, 36, (int) (190 * alpha));
                cTR = ColorUtil.rgba(12, 14, 26, (int) (180 * alpha));
                cBR = ColorUtil.rgba(6, 7, 14, (int) (200 * alpha));
                cBL = ColorUtil.rgba(14, 17, 30, (int) (185 * alpha));
            }
            default -> {
                cTL = ColorUtil.rgba(15, 13, 20, (int) (180 * alpha));
                cTR = ColorUtil.rgba(15, 13, 20, (int) (180 * alpha));
                cBR = ColorUtil.rgba(2, 2, 21, (int) (160 * alpha));
                cBL = ColorUtil.rgba(2, 2, 21, (int) (160 * alpha));
            }
        }

        Render2D.drawGradientRound(bx, by, bw, bh, 12.0F, cTL, cTR, cBR, cBL);

        // Glass Overlay Shimmer
        int glassTint = ColorUtil.rgba(255, 255, 255, (int) (6 * alpha));
        rect(bx, by, bw, bh, 12.0F, glassTint);
    }

    private void renderSidebar() {
        float barW = 28.0F;
        rect(x + barW - 0.5F, y + 6.0F, 0.5F, H - 12.0F, 0.0F, ColorUtil.rgba(255, 255, 255, (int) (12 * alpha)));

        // Top Custom Avatar
        Render2D.drawCustomAvatar(x + 7.0F, y + 8.0F, 14.0F, 7.0F, this.alpha);

        Category[] categories = new Category[]{Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
        float startY = y + 40.0F;
        float iconGap = 20.0F;

        for (int i = 0; i < categories.length; i++) {
            Category cat = categories[i];
            float iconY = startY + i * iconGap;
            boolean active = (activePage == Page.MODULES && activeCategory == cat);

            String iconGlyph = categoryGlyph(cat);
            int iconColor = active ? Theme.getAccentColor() : (hovered(x + 2.0F, iconY - 2.0F, barW - 4.0F, 16.0F) ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(120, 125, 140, 255));

            if (active) {
                rect(x + 2.0F, iconY - 2.0F, 2.0F, 14.0F, 1.0F, Theme.getAccentColor());
            }

            icon("icon", 9.0F, iconGlyph, x + 15.0F, iconY + 5.0F, iconColor);

            hit(x + 2.0F, iconY - 2.0F, barW - 4.0F, 16.0F, button -> {
                activePage = Page.MODULES;
                activeCategory = cat;
                searchQuery = "";
                leftScroll = 0.0F;
                rightScroll = 0.0F;
                List<Module> mods = getModulesForCategory(cat, "");
                if (!mods.isEmpty() && !mods.contains(selectedModule)) {
                    selectedModule = mods.get(0);
                }
                return true;
            });
        }

        // Bottom Settings Gear Icon
        float gearY = y + H - 22.0F;
        boolean gearActive = (activePage == Page.THEMES);
        int gearColor = gearActive ? Theme.getAccentColor() : (hovered(x + 2.0F, gearY - 2.0F, barW - 4.0F, 16.0F) ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(120, 125, 140, 255));

        if (gearActive) {
            rect(x + 2.0F, gearY - 2.0F, 2.0F, 14.0F, 1.0F, Theme.getAccentColor());
        }

        icon("icon", 9.0F, Fonts.NURIK_GEAR, x + 15.0F, gearY + 5.0F, gearColor);

        hit(x + 2.0F, gearY - 2.0F, barW - 4.0F, 16.0F, button -> {
            activePage = Page.THEMES;
            return true;
        });
    }

    private void renderTopBar() {
        hit(x, y, W - 120.0F, 24.0F, button -> {
            if (button == 0) {
                dragging = true;
                dX = mouseX - x;
                dY = mouseY - y;
                return true;
            }
            return false;
        });

        float startX = x + 138.0F;
        float startY = y + 7.0F;

        float searchW = 125.0F;
        float searchH = 15.0F;

        boolean searchFocused = (focus == FOCUS_SEARCH);
        rect(startX, startY, searchW, searchH, 4.0F, ColorUtil.rgba(18, 20, 28, 120));
        if (searchFocused) {
            outline(startX, startY, searchW, searchH, 4.0F, 0.5F, Theme.getAccentColor());
        }

        icon("search", 8.0F, Fonts.NURIK_SEARCH, startX + 7.0F, startY + 7.5F, searchFocused ? Theme.getAccentColor() : ColorUtil.rgba(120, 125, 140, 255));

        String sText = searchFocused ? buffer : (searchQuery.isEmpty() ? "Поиск" : searchQuery);
        int sCol = searchFocused || !searchQuery.isEmpty() ? ColorUtil.rgba(255, 255, 255, 255) : ColorUtil.rgba(100, 105, 120, 255);
        text(font(7.5F), clip(font(7.5F), sText, searchW - 20.0F), startX + 16.0F, textY(startY, searchH, font(7.5F)), sCol);

        hit(startX, startY, searchW, searchH, button -> {
            focus = FOCUS_SEARCH;
            buffer = searchQuery;
            return true;
        });

        // Top Right User Profile Badge
        float badgeX = x + W - 90.0F;
        float badgeY = y + 7.0F;
        String userName = (minecraft != null && minecraft.getUser() != null) ? minecraft.getUser().getName() : "Player";
        text(font(8.0F), clip(font(8.0F), userName, 80.0F), badgeX, badgeY + 1.0F, ColorUtil.rgba(255, 255, 255, 255));
        text(font(6.5F), "Error Client", badgeX, badgeY + 9.5F, ColorUtil.rgba(120, 125, 140, 255));
    }

    public List<Module> getModulesForCategory(Category category, String search) {
        List<Module> list = new ArrayList<>();
        if (Client.INSTANCE == null || Client.INSTANCE.moduleManager == null) return list;
        for (Module m : Client.INSTANCE.moduleManager.getModules()) {
            if (m == null) continue;
            if (search != null && !search.isBlank()) {
                if (m.getName().toLowerCase().contains(search.toLowerCase()) || (m.getDescription() != null && m.getDescription().toLowerCase().contains(search.toLowerCase()))) {
                    list.add(m);
                }
            } else if (m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
    }

    public String categoryGlyph(Category cat) {
        if (cat == null) return Fonts.NURIK_COMBAT;
        return switch (cat) {
            case COMBAT -> Fonts.NURIK_COMBAT;
            case MOVEMENT -> Fonts.NURIK_MOVEMENT;
            case RENDER -> Fonts.NURIK_VISUALS;
            case PLAYER -> Fonts.NURIK_PLAYER;
            case MISC -> Fonts.NURIK_MISC;
            default -> Fonts.NURIK_MISC;
        };
    }

    public FontHolder font(float size) {
        return new FontHolder(Fonts.SF_MEDIUM, size);
    }

    public void rect(float rx, float ry, float rw, float rh, float radius, int color) {
        Render2D.drawRoundedRect(rx, ry, rw, rh, radius, ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void outline(float rx, float ry, float rw, float rh, float radius, float thickness, int color) {
        Render2D.drawRoundedOutline(rx, ry, rw, rh, radius, thickness, ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void darkBlur(float bx, float by, float bw, float bh, float radius) {
        Render2D.drawBlur(bx, by, bw, bh, radius, ModernTheme.DARK_BLUR_TINT(), this.alpha);
    }

    public void text(FontHolder f, String s, float tx, float ty, int color) {
        Fonts.drawString(f.font(), s, tx, ty, f.size(), ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void textRight(FontHolder f, String s, float rightX, float ty, int color) {
        float w = f.font().getWidth(s, f.size());
        Fonts.drawString(f.font(), s, rightX - w, ty, f.size(), ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void textCenter(FontHolder f, String s, float centerX, float ty, int color) {
        float w = f.font().getWidth(s, f.size());
        Fonts.drawString(f.font(), s, centerX - w / 2.0F, ty, f.size(), ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public float textY(float top, float height, FontHolder f) {
        return top + (height - f.size()) / 2.0F;
    }

    public String clip(FontHolder f, String s, float maxWidth) {
        if (s == null) return "";
        if (f.font().getWidth(s, f.size()) <= maxWidth) return s;
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (f.font().getWidth(sb.toString() + c + "..", f.size()) > maxWidth) break;
            sb.append(c);
        }
        return sb.toString() + "..";
    }

    public void icon(String fontType, float size, String glyph, float centerX, float centerY, int color) {
        float w = Fonts.NURIK_MENU.getWidth(glyph, size);
        Fonts.drawString(Fonts.NURIK_MENU, glyph, centerX - w / 2.0F, centerY - size / 2.0F, size, ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void chevron(float centerX, float centerY, float size, float rotation, int color) {
        Render2D.drawCircle(centerX, centerY, size, ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void circle(float cx, float cy, float radius, int color) {
        Render2D.drawCircle(cx, cy, radius, ColorUtil.multiplyAlpha(color, this.alpha));
    }

    public void toggle(float tx, float ty, boolean on, Object key) {
        float t = ModernAnim.value("tgl:" + key, on ? 1.0F : 0.0F, 16.0F);
        rect(tx, ty, 17.0F, 8.5F, 4.0F, ColorUtil.lerp(ColorUtil.rgba(35, 40, 55, 255), Theme.getAccentColor(), t));
        float knobX = tx + 1.0F + 8.5F * t;
        rect(knobX, ty + 1.0F, 6.5F, 6.5F, 3.0F, ColorUtil.lerp(ColorUtil.rgba(110, 115, 125, 255), ColorUtil.rgba(255, 255, 255, 255), t));
    }

    public void slider(float x, float y, float w, float fraction, Object key, SliderConsumer action) {
        rect(x, y, w, 3.0F, 1.5F, ColorUtil.rgba(255, 255, 255, 15));
        float fillW = w * Math.max(0.0F, Math.min(1.0F, fraction));
        if (fillW > 0.5F) {
            rect(x, y, fillW, 3.0F, 1.5F, Theme.getAccentColor());
        }
        hit(x, y - 4.0F, w, 10.0F, button -> {
            float frac = Math.max(0.0F, Math.min(1.0F, (mouseX - x) / w));
            action.accept(frac);
            return true;
        });
    }

    public interface SliderConsumer {
        void accept(double fraction);
    }

    public void pill(float px, float py, float pw, float ph, String label, String iconFont, String glyph, boolean active, Object key, ClickAction action) {
        boolean hov = hovered(px, py, pw, ph);
        float t = ModernAnim.value("pill:" + key, active ? 1.0F : (hov ? 0.6F : 0.0F), 15.0F);
        rect(px, py, pw, ph, 4.0F, ColorUtil.lerp(ColorUtil.rgba(255, 255, 255, 10), ColorUtil.withAlpha(Theme.getAccentColor(), 80), t));
        if (active) outline(px, py, pw, ph, 4.0F, 0.5F, Theme.getAccentColor());
        var font = font(7.5F);
        textCenter(font, label, px + pw / 2.0F, textY(py, ph, font), active ? Theme.getAccentColor() : ColorUtil.rgba(160, 165, 180, 255));
        hit(px, py, pw, ph, action);
    }

    public float scroll() {
        return rightScroll;
    }

    public void setContentHeight(float contentH, float viewH) {
        this.maxRightScroll = Math.max(0.0F, contentH - viewH);
    }

    public void pushClip(float cx, float cy, float cw, float ch) {
        Render2DUtil.pushScissor(cx, cy, cw, ch);
    }

    public void popClip() {
        Render2DUtil.popScissor();
    }

    public boolean hovered(float hx, float hy, float hw, float hh) {
        return mouseX >= hx && mouseX <= hx + hw && mouseY >= hy && mouseY <= hy + hh;
    }

    public void hit(float hx, float hy, float hw, float hh, ClickAction action) {
        hitBoxes.add(new HitBox(hx, hy, hw, hh, action));
    }

    public record HitBox(float x, float y, float w, float h, ClickAction action) {}

    public interface ClickAction {
        boolean onClick(int button);
    }

    public boolean isBinding(Object target) {
        if (target instanceof Module m) return bindingModule == m;
        if (target instanceof Setting<?> s) return bindingSetting == s;
        return false;
    }

    public void startBind(Object target) {
        if (target instanceof Module m) {
            this.bindingModule = m;
            this.bindingSetting = null;
        } else if (target instanceof Setting<?> s) {
            this.bindingSetting = s;
            this.bindingModule = null;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isLeftClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (bindingModule != null) {
            int key = BindSetting.mouse(button);
            bindingModule.getBind().setSingle(key);
            bindingModule = null;
            return true;
        }
        if (bindingSetting != null && bindingSetting instanceof BindSetting bind) {
            bind.setSingle(BindSetting.mouse(button));
            bindingSetting = null;
            return true;
        }

        for (int i = hitBoxes.size() - 1; i >= 0; i--) {
            HitBox box = hitBoxes.get(i);
            if (mouseX >= box.x && mouseX <= box.x + box.w && mouseY >= box.y && mouseY <= box.y + box.h) {
                if (box.action.onClick(button)) {
                    ClientSoundPlayer.playGuiClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isLeftClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bindingModule.getBind().setSingle(BindSetting.UNBOUND);
            } else {
                bindingModule.getBind().setSingle(keyCode);
            }
            bindingModule = null;
            return true;
        }
        if (bindingSetting != null && bindingSetting instanceof BindSetting bind) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                bind.setSingle(BindSetting.UNBOUND);
            } else {
                bind.setSingle(keyCode);
            }
            bindingSetting = null;
            return true;
        }

        if (focus == FOCUS_SEARCH) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                focus = null;
                searchQuery = buffer;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!buffer.isEmpty()) {
                    buffer = buffer.substring(0, buffer.length() - 1);
                    searchQuery = buffer;
                }
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.closing = true;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (focus == FOCUS_SEARCH) {
            buffer += (char) event.codepoint();
            searchQuery = buffer;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.x + 34.0F && mouseX <= this.x + 134.0F) {
            this.leftScroll = (float) Math.max(0, Math.min(this.maxLeftScroll, this.leftScroll - scrollY * 16.0));
        } else {
            this.rightScroll = (float) Math.max(0, Math.min(this.maxRightScroll, this.rightScroll - scrollY * 16.0));
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
