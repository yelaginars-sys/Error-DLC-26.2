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
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.*;

public final class ArrayListHud extends HudElement implements error.IMinecraft {

    private record Entry(String name, String suffix, float fullWidth, float alpha) {}

    private final Map<Module, Animation> anims = new HashMap<>();
    private final Animation widthAnim = new Animation(80.0F, 0.22F);
    private final Animation heightAnim = new Animation(20.0F, 0.22F);

    private static final float ROW_H = 14.0F;
    private static final float ROW_GAP = 2.5F;
    private static final float CARD_R = 4.0F;

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
                String suffix = showSuffix ? getModuleSuffix(m) : "";
                float nameW = Fonts.SF_MEDIUM.getWidth(m.getName(), 8.5F);
                float suffixW = suffix.isEmpty() ? 0.0F : Fonts.SF_MEDIUM.getWidth(" " + suffix, 8.0F);
                float totalW = nameW + suffixW + 14.0F;
                entries.add(new Entry(m.getName(), suffix, totalW, a.getValue()));
            }
        }

        if (entries.isEmpty() && inChat) {
            entries.add(new Entry("AttackAura", "Single", Fonts.SF_MEDIUM.getWidth("AttackAura", 8.5F) + Fonts.SF_MEDIUM.getWidth(" Single", 8.0F) + 14.0F, 1.0F));
            entries.add(new Entry("Velocity", "Packet", Fonts.SF_MEDIUM.getWidth("Velocity", 8.5F) + Fonts.SF_MEDIUM.getWidth(" Packet", 8.0F) + 14.0F, 1.0F));
            entries.add(new Entry("TargetStrafe", "Adaptive", Fonts.SF_MEDIUM.getWidth("TargetStrafe", 8.5F) + Fonts.SF_MEDIUM.getWidth(" Adaptive", 8.0F) + 14.0F, 1.0F));
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Sort descending by width (longest module at top)
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

        int accent = Theme.getAccentColor();
        float curY = this.y;

        for (Entry e : entries) {
            float a = e.alpha;
            if (a <= 0.01F) continue;

            float rowW = e.fullWidth;
            float slide = (1.0F - a) * 14.0F;
            float rowX = rightSide ? (this.x + (this.width - rowW) + slide) : (this.x - slide);

            // ClickGUI frosted card style
            int cardBg = ColorUtil.rgba(255, 255, 255, (int) (14 * a));
            int cardOutline = ColorUtil.withAlpha(accent, (int) (140 * a));

            // Shadow
            Render2D.drawShadow(rowX, curY, rowW, ROW_H, CARD_R, 4.0F, ColorUtil.rgba(0, 0, 0, (int) (55 * a)));

            // Card background & outline
            Render2D.drawRoundedRect(rowX, curY, rowW, ROW_H, CARD_R, cardBg);
            Render2D.drawRoundedOutline(rowX, curY, rowW, ROW_H, CARD_R, 0.75F, cardOutline);

            // Accent indicator bar (outer edge)
            if (rightSide) {
                Render2D.drawRoundedRect(rowX + rowW - 2.2F, curY + 2.0F, 1.8F, ROW_H - 4.0F, 1.0F, ColorUtil.withAlpha(accent, (int) (245 * a)));
            } else {
                Render2D.drawRoundedRect(rowX + 0.4F, curY + 2.0F, 1.8F, ROW_H - 4.0F, 1.0F, ColorUtil.withAlpha(accent, (int) (245 * a)));
            }

            // Text
            float textX = rightSide ? (rowX + 5.0F) : (rowX + 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, textX, curY + 2.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (250 * a)));

            if (!e.suffix.isEmpty()) {
                float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 8.5F);
                Fonts.drawString(Fonts.SF_MEDIUM, " " + e.suffix, textX + nameW, curY + 2.2F, 8.0F, ColorUtil.rgba(160, 180, 200, (int) (225 * a)));
            }

            curY += (ROW_H + ROW_GAP) * a;
        }
    }
}
