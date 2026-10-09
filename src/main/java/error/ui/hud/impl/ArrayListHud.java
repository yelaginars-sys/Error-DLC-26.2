package error.ui.hud.impl;

import error.Client;
import error.event.list.Render2DEvent;
import error.module.Module;
import error.module.impl.render.Interface;
import error.setting.Setting;
import error.setting.impl.ModeSetting;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.*;

public final class ArrayListHud extends HudElement implements error.IMinecraft {

    private record Entry(String name, String suffix, float nameW, float suffixW, float fullWidth, float alpha) {}

    private final Map<Module, Animation> anims = new HashMap<>();
    private final Animation widthAnim = new Animation(80.0F, 0.22F);
    private final Animation heightAnim = new Animation(20.0F, 0.22F);

    private static final float ROW_H = 14.0F;
    private static final float ROW_GAP = 2.0F;
    private static final float CARD_R = 3.5F;
    private static final float BAR_W = 2.0F;

    public ArrayListHud() {
        super("arraylist", "ArrayList", 5.0F, 30.0F, 90.0F, 100.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.arrayList.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private static String getModuleSuffix(Module module) {
        if (module == null) return "";
        for (Setting<?> s : module.getSettings()) {
            if (s instanceof ModeSetting ms) {
                return ms.getValue();
            }
        }
        return "";
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc == null || mc.getWindow() == null) return;

        Interface iface = Interface.getInstance();
        boolean showSuffix = iface == null || iface.arrayListSuffix.getValue();
        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;

        List<Module> allModules = Client.getInstance() != null && Client.getInstance().getModuleManager() != null
                ? Client.getInstance().getModuleManager().getModules() : Collections.emptyList();

        for (Module m : allModules) {
            if (m.getName().equalsIgnoreCase("ClickGui") || m.getName().equalsIgnoreCase("Interface")
                    || m.getName().equalsIgnoreCase("HUD") || m.isHiddenFromHud()) {
                continue;
            }

            Animation a = anims.computeIfAbsent(m, k -> new Animation(0.0F, 0.20F));
            a.setTarget(m.isState() ? 1.0F : 0.0F);
            a.update();
        }

        anims.entrySet().removeIf(e -> !e.getKey().isState() && e.getValue().getValue() <= 0.01F);

        List<Entry> entries = new ArrayList<>();
        for (Module m : allModules) {
            Animation a = anims.get(m);
            if (a != null && a.getValue() > 0.01F) {
                String name = m.getName().toLowerCase();
                String suffix = showSuffix ? getModuleSuffix(m).toLowerCase() : "";
                float nameW = Fonts.SF_MEDIUM.getWidth(name, 8.5F);
                float suffixW = suffix.isEmpty() ? 0.0F : Fonts.SF_MEDIUM.getWidth(" " + suffix, 8.0F);
                float totalW = nameW + suffixW + 12.0F; // text + padding
                entries.add(new Entry(name, suffix, nameW, suffixW, totalW, a.getValue()));
            }
        }

        if (entries.isEmpty() && inChat) {
            float aW = Fonts.SF_MEDIUM.getWidth("attackaura", 8.5F);
            float aSw = Fonts.SF_MEDIUM.getWidth(" single", 8.0F);
            entries.add(new Entry("attackaura", "single", aW, aSw, aW + aSw + 12.0F, 1.0F));

            float vW = Fonts.SF_MEDIUM.getWidth("velocity", 8.5F);
            float vSw = Fonts.SF_MEDIUM.getWidth(" packet", 8.0F);
            entries.add(new Entry("velocity", "packet", vW, vSw, vW + vSw + 12.0F, 1.0F));

            float tW = Fonts.SF_MEDIUM.getWidth("targetstrafe", 8.5F);
            float tSw = Fonts.SF_MEDIUM.getWidth(" adaptive", 8.0F);
            entries.add(new Entry("targetstrafe", "adaptive", tW, tSw, tW + tSw + 12.0F, 1.0F));
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Sort descending by full width (longest module at top, matching Energy)
        entries.sort((e1, e2) -> Float.compare(e2.fullWidth, e1.fullWidth));

        float maxW = 50.0F;
        float totalH = 0.0F;
        for (Entry e : entries) {
            if (e.fullWidth > maxW) maxW = e.fullWidth;
            totalH += (ROW_H + ROW_GAP) * e.alpha;
        }

        widthAnim.setTarget(maxW);
        widthAnim.update();
        heightAnim.setTarget(totalH);
        heightAnim.update();

        this.width = widthAnim.getValue();
        this.height = heightAnim.getValue();

        float screenW = mc.getWindow().getGuiScaledWidth();
        boolean rightSide = (this.x + this.width * 0.5F) > (screenW * 0.5F);

        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        float curY = this.y;
        int index = 0;

        for (Entry e : entries) {
            float a = e.alpha;
            if (a <= 0.01F) continue;

            float rowW = e.fullWidth;
            float slide = (1.0F - a) * 12.0F;
            float rowX = rightSide ? (this.x + (this.width - rowW) + slide) : (this.x - slide);

            // Energy wave gradient color per index
            int rowColor = Theme.getGradientColor(index * 30);
            int rowColorWithAlpha = ColorUtil.withAlpha(rowColor, (int) (255 * a));

            // 1. Background Card (respects active theme)
            Render2D.drawHudCard(extractor, rowX, curY, rowW, ROW_H, CARD_R, a, rowColor);

            // 3. Side accent bar (2px width) on outer edge (matching Energy ArrayList)
            float barX = rightSide ? (rowX + rowW - BAR_W) : rowX;
            Render2D.drawRoundedRect(barX, curY, BAR_W, ROW_H, 1.0F, rowColorWithAlpha);

            // 4. Module Text & Suffix
            float textX = rightSide ? (rowX + 5.0F) : (rowX + 6.0F);
            float textY = curY + 2.5F;

            // Module name in lowercase with Energy flowing gradient color
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, textX, textY, 8.5F, rowColorWithAlpha);

            // Suffix in pure white
            if (!e.suffix.isEmpty()) {
                Fonts.drawString(Fonts.SF_MEDIUM, " " + e.suffix, textX + e.nameW, textY, 8.0F, ColorUtil.withAlpha(0xFFFFFFFF, (int) (240 * a)));
            }

            curY += (ROW_H + ROW_GAP) * a;
            index++;
        }
    }
}
