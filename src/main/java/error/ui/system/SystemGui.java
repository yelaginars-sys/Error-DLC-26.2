package error.ui.system;

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

public class SystemGui extends Screen {

    public static final float W = 680.0F;
    public static final float H = 430.0F;

    public enum SectionGroup {
        CLIENT_CATEGORIES("Client categories"),
        SETTINGS("Settings");

        private final String label;
        SectionGroup(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum Section {
        ALL(Category.COMBAT, "All", Fonts.NURIK_DOTS, SectionGroup.CLIENT_CATEGORIES),
        COMBAT(Category.COMBAT, "Combat", Fonts.NURIK_COMBAT, SectionGroup.CLIENT_CATEGORIES),
        MOVEMENT(Category.MOVEMENT, "Movement", Fonts.NURIK_MOVEMENT, SectionGroup.CLIENT_CATEGORIES),
        PLAYER(Category.PLAYER, "Player", Fonts.NURIK_PLAYER, SectionGroup.CLIENT_CATEGORIES),
        RENDER(Category.RENDER, "Render", Fonts.NURIK_VISUALS, SectionGroup.CLIENT_CATEGORIES),
        VISUAL(Category.RENDER, "Visual", Fonts.NURIK_VISUALS, SectionGroup.CLIENT_CATEGORIES),
        CLIENT(Category.MISC, "Client", Fonts.NURIK_GEAR, SectionGroup.CLIENT_CATEGORIES),

        CONFIGS(null, "Configs", Fonts.NURIK_PRESETS, SectionGroup.SETTINGS),
        FRIENDS(null, "Friends", Fonts.NURIK_ACCOUNTS, SectionGroup.SETTINGS),
        WAYPOINTS(null, "Waypoints", Fonts.NURIK_ANGLES, SectionGroup.SETTINGS),
        COSMETICS(null, "Cosmetics", Fonts.NURIK_PLAYER, SectionGroup.SETTINGS),
        PARTY_SETTINGS(null, "Party settings", Fonts.NURIK_GEAR, SectionGroup.SETTINGS),
        SETTINGS(null, "Settings", Fonts.NURIK_GEAR, SectionGroup.SETTINGS),
        SEARCH(null, "Search", Fonts.NURIK_SEARCH, SectionGroup.SETTINGS);

        private final Category category;
        private final String title;
        private final String iconGlyph;
        private final SectionGroup group;

        Section(Category category, String title, String iconGlyph, SectionGroup group) {
            this.category = category;
            this.title = title;
            this.iconGlyph = iconGlyph;
            this.group = group;
        }

        public Category getCategory() { return category; }
        public String getTitle() { return title; }
        public String getIconGlyph() { return iconGlyph; }
        public SectionGroup getGroup() { return group; }
    }

    public record FontHolder(MsdfFont font, float size) {}

    public Section activeSection = Section.CLIENT;
    public Module selectedModule = null;

    public String searchQuery = "";
    public float scroll = 0.0F;
    public float maxScroll = 0.0F;

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
    public static final Object FOCUS_SEARCH = "system_search";

    public SystemGui() {
        super(Component.literal("System Client GUI"));
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

            // 1. Scene Dimming & Background Glass Blur
            rect(0, 0, screenWidth, screenHeight, 0.0F, ColorUtil.rgba(6, 7, 10, (int) (160 * alpha)));
            darkBlur(x, y, W, H, 16.0F);

            // 2. Window Body Container
            rect(x, y, W, H, 14.0F, SystemTheme.BG_MAIN());
            outline(x, y, W, H, 14.0F, 0.5F, SystemTheme.BORDER());

            // 3. Left System Sidebar
            renderSidebar();

            // 4. Header Bar
            renderHeaderBar();

            // 5. Main Content Masonry Grid (3 Columns)
            float contentX = x + 142.0F;
            float contentY = y + 26.0F;
            float contentW = W - 148.0F;
            float contentH = H - 32.0F;

            SystemModulesPage.renderContent(this, contentX, contentY, contentW, contentH);

            Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();
        }
    }

    private void renderSidebar() {
        float sidebarW = 135.0F;

        // Sidebar Background Panel
        rect(x, y, sidebarW, H, 14.0F, SystemTheme.SIDEBAR());
        rect(x + sidebarW - 0.5F, y + 4.0F, 0.5F, H - 8.0F, 0.0F, SystemTheme.BORDER());

        // Top Logo: ✦ System
        icon("icon", 9.0F, Fonts.NURIK_LOGO, x + 14.0F, y + 14.0F, SystemTheme.TEXT_PRIMARY());
        text(font(9.0F), "System", x + 24.0F, y + 7.5F, SystemTheme.TEXT_PRIMARY());

        float curY = y + 28.0F;
        float itemH = 15.0F;

        SectionGroup currentGroup = null;

        for (Section sec : Section.values()) {
            if (sec.getGroup() != currentGroup) {
                currentGroup = sec.getGroup();
                curY += 4.0F;
                text(font(4.8F), currentGroup.getLabel().toUpperCase(), x + 12.0F, curY, SystemTheme.TEXT_MUTED());
                curY += 9.0F;
            }

            boolean active = (activeSection == sec);
            boolean hov = hovered(x + 8.0F, curY, sidebarW - 16.0F, itemH);

            if (active) {
                rect(x + 8.0F, curY, sidebarW - 16.0F, itemH, 4.0F, SystemTheme.CARD_ACTIVE());
                outline(x + 8.0F, curY, sidebarW - 16.0F, itemH, 4.0F, 0.4F, SystemTheme.BORDER_ACTIVE());
            } else if (hov) {
                rect(x + 8.0F, curY, sidebarW - 16.0F, itemH, 4.0F, SystemTheme.CARD_HOVER());
            }

            int iconCol = active ? SystemTheme.TEXT_PRIMARY() : (hov ? SystemTheme.TEXT_SECONDARY() : SystemTheme.TEXT_MUTED());
            icon("icon", 7.5F, sec.getIconGlyph(), x + 16.0F, curY + 7.5F, iconCol);

            int textCol = active ? SystemTheme.TEXT_PRIMARY() : (hov ? SystemTheme.TEXT_SECONDARY() : SystemTheme.TEXT_MUTED());
            text(font(6.2F), sec.getTitle(), x + 24.0F, textY(curY, itemH, font(6.2F)), textCol);

            // Badge Count for Categories
            if (sec == Section.ALL) {
                int count = getModulesForCategory(null, "").size();
                renderBadge(x + sidebarW - 22.0F, curY + 2.5F, String.valueOf(count));
            } else if (sec == Section.PLAYER) {
                int count = getModulesForCategory(Category.PLAYER, "").size();
                renderBadge(x + sidebarW - 22.0F, curY + 2.5F, String.valueOf(count));
            } else if (sec == Section.CLIENT) {
                renderBadge(x + sidebarW - 24.0F, curY + 2.5F, "9+2");
            }

            hit(x + 8.0F, curY, sidebarW - 16.0F, itemH, button -> {
                activeSection = sec;
                scroll = 0.0F;
                return true;
            });

            curY += itemH + 1.5F;
        }

        // Bottom User Profile Box
        float profileY = y + H - 24.0F;
        rect(x + 8.0F, profileY, sidebarW - 16.0F, 18.0F, 5.0F, ColorUtil.rgba(14, 16, 22, 180));
        rect(x + 12.0F, profileY + 3.0F, 12.0F, 12.0F, 6.0F, ColorUtil.rgba(255, 255, 255, 20));
        textCenter(font(6.5F), "P", x + 18.0F, textY(profileY + 3.0F, 12.0F, font(6.5F)), SystemTheme.TEXT_PRIMARY());

        String username = (minecraft != null && minecraft.getUser() != null) ? minecraft.getUser().getName() : "promo Rimora";
        text(font(5.8F), clip(font(5.8F), username, 70.0F), x + 28.0F, profileY + 2.0F, SystemTheme.TEXT_PRIMARY());
        text(font(4.8F), "funtime.su", x + 28.0F, profileY + 9.5F, SystemTheme.TEXT_MUTED());
    }

    private void renderBadge(float bx, float by, String text) {
        float fontW = font(5.0F).font().getWidth(text, 5.0F);
        float bw = Math.max(12.0F, fontW + 6.0F);
        rect(bx, by, bw, 10.0F, 5.0F, ColorUtil.rgba(255, 255, 255, 15));
        textCenter(font(5.0F), text, bx + bw / 2.0F, textY(by, 10.0F, font(5.0F)), SystemTheme.TEXT_SECONDARY());
    }

    private void renderHeaderBar() {
        // Dragging region header
        hit(x + 135.0F, y, W - 135.0F, 22.0F, button -> {
            if (button == 0) {
                dragging = true;
                dX = mouseX - x;
                dY = mouseY - y;
                return true;
            }
            return false;
        });

        float headerX = x + 142.0F;
        float headerY = y + 6.0F;

        icon("menu", 7.5F, Fonts.NURIK_DOTS, headerX + 4.0F, headerY + 6.0F, SystemTheme.TEXT_SECONDARY());
        text(font(8.0F), activeSection.getTitle(), headerX + 14.0F, textY(headerY, 13.0F, font(8.0F)), SystemTheme.TEXT_PRIMARY());

        // Center Search Bar
        float searchW = 110.0F;
        float searchH = 13.0F;
        float searchX = x + 310.0F;
        float searchY = y + 6.0F;

        boolean searchFocused = (focus == FOCUS_SEARCH);
        rect(searchX, searchY, searchW, searchH, 4.0F, ColorUtil.rgba(18, 20, 26, 160));
        if (searchFocused) {
            outline(searchX, searchY, searchW, searchH, 4.0F, 0.4F, SystemTheme.BORDER_ACTIVE());
        }

        icon("search", 7.0F, Fonts.NURIK_SEARCH, searchX + 6.0F, searchY + 6.5F, searchFocused ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_MUTED());

        String sText = searchFocused ? buffer : (searchQuery.isEmpty() ? "Search" : searchQuery);
        int sCol = searchFocused || !searchQuery.isEmpty() ? SystemTheme.TEXT_PRIMARY() : SystemTheme.TEXT_MUTED();
        text(font(6.0F), clip(font(6.0F), sText, searchW - 32.0F), searchX + 14.0F, textY(searchY, searchH, font(6.0F)), sCol);

        // Keybind badge Tab
        rect(searchX + searchW - 20.0F, searchY + 2.0F, 16.0F, 9.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 10));
        textCenter(font(4.8F), "Tab", searchX + searchW - 12.0F, textY(searchY + 2.0F, 9.0F, font(4.8F)), SystemTheme.TEXT_MUTED());

        hit(searchX, searchY, searchW, searchH, button -> {
            focus = FOCUS_SEARCH;
            buffer = searchQuery;
            return true;
        });

        // Ping Status pill: 109 ms / 375 ms
        float pingX = searchX + searchW + 10.0F;
        rect(pingX, searchY + 1.0F, 36.0F, 11.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 8));
        textCenter(font(5.2F), "109 ms", pingX + 18.0F, textY(searchY + 1.0F, 11.0F, font(5.2F)), SystemTheme.TEXT_MUTED());

        // Header Icons Right: Star, Pin, Sun, Moon, Expand
        float iconStartX = x + W - 75.0F;
        icon("star", 6.5F, "\uEA02", iconStartX, searchY + 6.5F, SystemTheme.TEXT_MUTED());
        icon("pin", 6.5F, "\uEA04", iconStartX + 14.0F, searchY + 6.5F, SystemTheme.TEXT_MUTED());
        icon("sun", 6.5F, "\uEA1D", iconStartX + 28.0F, searchY + 6.5F, SystemTheme.TEXT_MUTED());
        icon("moon", 6.5F, "\uEA1E", iconStartX + 42.0F, searchY + 6.5F, SystemTheme.TEXT_MUTED());
        icon("expand", 6.5F, Fonts.NURIK_DOTS, iconStartX + 56.0F, searchY + 6.5F, SystemTheme.TEXT_MUTED());
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
            } else if (category == null || activeSection == Section.ALL || m.getCategory() == category) {
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
        Render2D.drawBlur(bx, by, bw, bh, radius, ColorUtil.rgba(6, 7, 10, 160), this.alpha);
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
        rect(tx, ty, 15.0F, 8.0F, 4.0F, ColorUtil.lerp(ColorUtil.rgba(35, 38, 48, 255), SystemTheme.WHITE(), t));
        float knobX = tx + 1.0F + 7.0F * t;
        rect(knobX, ty + 1.0F, 6.0F, 6.0F, 3.0F, ColorUtil.lerp(ColorUtil.rgba(110, 115, 125, 255), ColorUtil.rgba(12, 14, 18, 255), t));
    }

    public void checkbox(float cx, float cy, boolean checked, Object key) {
        float t = ModernAnim.value("chk:" + key, checked ? 1.0F : 0.0F, 16.0F);
        rect(cx, cy, 9.0F, 9.0F, 4.5F, ColorUtil.lerp(ColorUtil.rgba(30, 34, 44, 255), SystemTheme.WHITE(), t));
        if (checked) {
            icon("check", 5.0F, Fonts.NURIK_CHECK, cx + 4.5F, cy + 4.5F, ColorUtil.rgba(12, 14, 18, 255));
        }
    }

    public void slider(float x, float y, float w, float fraction, Object key, SliderConsumer action) {
        rect(x, y, w, 3.0F, 1.5F, ColorUtil.rgba(255, 255, 255, 15));
        float fillW = w * Math.max(0.0F, Math.min(1.0F, fraction));
        if (fillW > 0.5F) {
            rect(x, y, fillW, 3.0F, 1.5F, SystemTheme.WHITE());
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
        this.scroll = (float) Math.max(0, Math.min(this.maxScroll, this.scroll - scrollY * 16.0));
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
