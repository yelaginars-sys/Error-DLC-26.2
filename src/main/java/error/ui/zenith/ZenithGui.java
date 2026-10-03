package error.ui.zenith;

import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.Setting;
import error.setting.impl.BindSetting;
import error.ui.modern.ModernAnim;
import error.util.RenderExtend;
import error.util.client.ClientSoundPlayer;
import error.util.client.clients.ColorUtil;
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

public class ZenithGui extends Screen {

    public static final float W = 570.0F;
    public static final float H = 340.0F;

    public enum Section {
        COMBAT(Category.COMBAT, "Combat", Fonts.NURIK_COMBAT),
        MOVEMENT(Category.MOVEMENT, "Movement", Fonts.NURIK_MOVEMENT),
        MISC(Category.MISC, "Misc", Fonts.NURIK_MISC),
        VISUALS(Category.RENDER, "Visuals", Fonts.NURIK_VISUALS),
        PLAYER(Category.PLAYER, "Player", Fonts.NURIK_PLAYER),

        TOOLS(null, "Tools", Fonts.NURIK_GEAR),
        CLOUD(null, "Cloud", Fonts.NURIK_SEARCH),
        COSMETICS(null, "Cosmetics", Fonts.NURIK_PLAYER),
        INTERFACE(null, "Interface", Fonts.NURIK_VISUALS),
        SCRIPTS(null, "Scripts & Configs", Fonts.NURIK_PRESETS);

        private final Category category;
        private final String title;
        private final String iconGlyph;

        Section(Category category, String title, String iconGlyph) {
            this.category = category;
            this.title = title;
            this.iconGlyph = iconGlyph;
        }

        public Category getCategory() {
            return category;
        }

        public String getTitle() {
            return title;
        }

        public String getIconGlyph() {
            return iconGlyph;
        }
    }

    public record FontHolder(MsdfFont font, float size) {}

    public Section activeSection = Section.COMBAT;
    public Module selectedModule = null;

    public String searchQuery = "";
    public String subTab = "Все";
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
    public static final Object FOCUS_SEARCH = "zenith_search";

    public ZenithGui() {
        super(Component.literal("ZENITH Client GUI"));
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
            this.dragY = (screenHeight - H) / 2.0F + 10.0F;
        }

        if (selectedModule == null && activeSection.getCategory() != null) {
            List<Module> mods = getModulesForCategory(activeSection.getCategory(), "");
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
            rect(0, 0, screenWidth, screenHeight, 0.0F, ColorUtil.rgba(8, 9, 14, (int) (140 * alpha)));

            // 2. Main Glass Window Frame
            darkBlur(x, y, W, H, 14.0F);
            Render2D.drawGradientRound(x, y, W, H, 10.0F, ZenithTheme.BG_TOP(), ZenithTheme.BG_TOP(), ZenithTheme.BG_BOTTOM(), ZenithTheme.BG_BOTTOM());
            outline(x, y, W, H, 10.0F, 0.5F, ZenithTheme.BORDER());

            // 3. Left Zenith Sidebar
            renderSidebar();

            // 4. Header Bar (Search & Title)
            renderHeaderBar();

            // 5. Middle Grid (Modules or Custom Pages) & Right 3D Player Inspector
            float contentX = x + 115.0F;
            float contentY = y + 24.0F;
            float contentW = W - 120.0F;
            float contentH = H - 30.0F;

            ZenithModulesPage.renderContent(this, contentX, contentY, contentW, contentH);

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderTopCapsuleBar(int screenWidth) {
        float capW = 160.0F;
        float capH = 18.0F;
        float capX = (screenWidth - capW) / 2.0F;
        float capY = 6.0F;

        // Floating Capsule Background
        rect(capX, capY, capW, capH, 9.0F, ColorUtil.rgba(16, 18, 26, (int) (200 * alpha)));
        outline(capX, capY, capW, capH, 9.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int) (15 * alpha)));

        // User Avatar + Name
        Render2D.drawCustomAvatar(capX + 4.0F, capY + 3.0F, 12.0F, 6.0F, this.alpha);
        String username = (minecraft != null && minecraft.getUser() != null) ? minecraft.getUser().getName() : "Player";
        text(font(7.0F), clip(font(7.0F), username, 55.0F), capX + 18.0F, textY(capY, capH, font(7.0F)), ZenithTheme.TEXT_PRIMARY());

        // FPS & Ping Indicators
        int fps = minecraft != null ? minecraft.getFps() : 60;
        String statsStr = fps + " fps  |  0 ms";
        textRight(font(6.5F), statsStr, capX + capW - 8.0F, textY(capY, capH, font(6.5F)), ZenithTheme.TEXT_MUTED());
    }

    private void renderSidebar() {
        float sidebarW = 110.0F;

        // Sidebar Background
        rect(x, y, sidebarW, H, 10.0F, ZenithTheme.SIDEBAR());
        rect(x + sidebarW - 0.5F, y + 6.0F, 0.5F, H - 12.0F, 0.0F, ZenithTheme.BORDER());

        // Top ZENITH Logo
        var logoFont = font(9.0F);
        text(logoFont, "ZENITH", x + 10.0F, y + 8.0F, ZenithTheme.TEXT_PRIMARY());

        // Navigation Sections List
        float startY = y + 26.0F;
        float itemH = 16.0F;
        float itemGap = 2.0F;
        float curY = startY;

        Section[] sections = Section.values();
        for (int i = 0; i < sections.length; i++) {
            Section sec = sections[i];

            if (sec == Section.TOOLS) {
                // Section Separator Line
                rect(x + 10.0F, curY + 2.0F, sidebarW - 20.0F, 0.5F, 0.0F, ZenithTheme.BORDER());
                curY += 6.0F;
            }

            boolean active = (activeSection == sec);
            boolean hov = hovered(x + 6.0F, curY, sidebarW - 12.0F, itemH);

            if (active) {
                rect(x + 6.0F, curY, sidebarW - 12.0F, itemH, 4.0F, ZenithTheme.CARD_ACTIVE());
                outline(x + 6.0F, curY, sidebarW - 12.0F, itemH, 4.0F, 0.4F, ZenithTheme.BORDER_ACTIVE());
                rect(x + 6.0F, curY + 3.0F, 2.0F, 10.0F, 1.0F, ZenithTheme.ACCENT());
            }

            int iconCol = active ? ZenithTheme.ACCENT() : (hov ? ZenithTheme.TEXT_PRIMARY() : ZenithTheme.TEXT_MUTED());
            icon("icon", 8.0F, sec.getIconGlyph(), x + 16.0F, curY + 8.0F, iconCol);

            int textCol = active ? ZenithTheme.TEXT_PRIMARY() : (hov ? ZenithTheme.TEXT_SECONDARY() : ZenithTheme.TEXT_MUTED());
            text(font(6.5F), sec.getTitle(), x + 24.0F, textY(curY, itemH, font(6.5F)), textCol);

            // Module Count Badge (for categories)
            if (sec.getCategory() != null) {
                int count = getModulesForCategory(sec.getCategory(), "").size();
                textRight(font(6.0F), String.valueOf(count), x + sidebarW - 10.0F, textY(curY, itemH, font(6.0F)), ZenithTheme.TEXT_MUTED());
            }

            hit(x + 6.0F, curY, sidebarW - 12.0F, itemH, button -> {
                activeSection = sec;
                leftScroll = 0.0F;
                rightScroll = 0.0F;
                if (sec.getCategory() != null) {
                    List<Module> mods = getModulesForCategory(sec.getCategory(), "");
                    if (!mods.isEmpty()) selectedModule = mods.get(0);
                }
                return true;
            });

            curY += itemH + itemGap;
        }

        // Bottom User Card Profile
        float userCardY = y + H - 24.0F;
        rect(x + 6.0F, userCardY, sidebarW - 12.0F, 20.0F, 5.0F, ColorUtil.rgba(20, 22, 32, 160));
        Render2D.drawCustomAvatar(x + 10.0F, userCardY + 4.0F, 12.0F, 6.0F, this.alpha);

        String username = (minecraft != null && minecraft.getUser() != null) ? minecraft.getUser().getName() : "Player";
        text(font(6.5F), clip(font(6.5F), username, 45.0F), x + 25.0F, userCardY + 2.5F, ZenithTheme.TEXT_PRIMARY());
        text(font(5.5F), "2026 | ERROR", x + 25.0F, userCardY + 10.5F, ZenithTheme.TEXT_MUTED());

        // Status Badge Tag "АКТИВЕН"
        textRight(font(5.5F), "АКТИВЕН", x + sidebarW - 10.0F, userCardY + 6.5F, ZenithTheme.BADGE_GREEN());
    }

    private void renderHeaderBar() {
        hit(x + 110.0F, y, W - 110.0F, 22.0F, button -> {
            if (button == 0) {
                dragging = true;
                dX = mouseX - x;
                dY = mouseY - y;
                return true;
            }
            return false;
        });

        float headerX = x + 118.0F;
        float headerY = y + 6.0F;

        icon("menu", 7.5F, Fonts.NURIK_DOTS, headerX + 4.0F, headerY + 6.0F, ZenithTheme.TEXT_SECONDARY());
        text(font(8.5F), activeSection.getTitle(), headerX + 14.0F, textY(headerY, 14.0F, font(8.5F)), ZenithTheme.TEXT_PRIMARY());

        // Search Field Top Right
        float searchW = 120.0F;
        float searchH = 14.0F;
        float searchX = x + W - searchW - 10.0F;
        float searchY = y + 6.0F;

        boolean searchFocused = (focus == FOCUS_SEARCH);
        rect(searchX, searchY, searchW, searchH, 4.0F, ColorUtil.rgba(18, 20, 28, 140));
        if (searchFocused) {
            outline(searchX, searchY, searchW, searchH, 4.0F, 0.4F, ZenithTheme.ACCENT());
        }

        icon("search", 7.5F, Fonts.NURIK_SEARCH, searchX + 6.0F, searchY + 7.0F, searchFocused ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_MUTED());

        String sText = searchFocused ? buffer : (searchQuery.isEmpty() ? "Search for elements..." : searchQuery);
        int sCol = searchFocused || !searchQuery.isEmpty() ? ZenithTheme.TEXT_PRIMARY() : ZenithTheme.TEXT_MUTED();
        text(font(6.5F), clip(font(6.5F), sText, searchW - 18.0F), searchX + 14.0F, textY(searchY, searchH, font(6.5F)), sCol);

        hit(searchX, searchY, searchW, searchH, button -> {
            focus = FOCUS_SEARCH;
            buffer = searchQuery;
            return true;
        });
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
            } else if (category != null && m.getCategory() == category) {
                list.add(m);
            }
        }
        return list;
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
        Render2D.drawBlur(bx, by, bw, bh, radius, ColorUtil.rgba(10, 12, 18, 140), this.alpha);
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

    public void toggle(float tx, float ty, boolean on, Object key) {
        float t = ModernAnim.value("tgl:" + key, on ? 1.0F : 0.0F, 16.0F);
        rect(tx, ty, 17.0F, 8.5F, 4.0F, ColorUtil.lerp(ColorUtil.rgba(35, 40, 55, 255), ZenithTheme.ACCENT(), t));
        float knobX = tx + 1.0F + 8.5F * t;
        rect(knobX, ty + 1.0F, 6.5F, 6.5F, 3.0F, ColorUtil.lerp(ColorUtil.rgba(110, 115, 125, 255), ColorUtil.rgba(255, 255, 255, 255), t));
    }

    public void slider(float x, float y, float w, float fraction, Object key, SliderConsumer action) {
        rect(x, y, w, 3.0F, 1.5F, ColorUtil.rgba(255, 255, 255, 15));
        float fillW = w * Math.max(0.0F, Math.min(1.0F, fraction));
        if (fillW > 0.5F) {
            rect(x, y, fillW, 3.0F, 1.5F, ZenithTheme.ACCENT());
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

    public void pill(float px, float py, float pw, float ph, String label, boolean active, Object key, ClickAction action) {
        boolean hov = hovered(px, py, pw, ph);
        float t = ModernAnim.value("pill:" + key, active ? 1.0F : (hov ? 0.6F : 0.0F), 15.0F);
        rect(px, py, pw, ph, 4.0F, ColorUtil.lerp(ZenithTheme.CARD(), ZenithTheme.CARD_ACTIVE(), t));
        if (active) outline(px, py, pw, ph, 4.0F, 0.4F, ZenithTheme.ACCENT());
        var font = font(6.5F);
        textCenter(font, label, px + pw / 2.0F, textY(py, ph, font), active ? ZenithTheme.ACCENT() : ZenithTheme.TEXT_SECONDARY());
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
        if (mouseX >= this.x + 115.0F && mouseX <= this.x + 420.0F) {
            this.leftScroll = (float) Math.max(0, Math.min(this.maxLeftScroll, this.leftScroll - scrollY * 16.0));
        } else {
            this.rightScroll = (float) Math.max(0, Math.min(this.maxRightScroll, this.rightScroll - scrollY * 16.0));
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
