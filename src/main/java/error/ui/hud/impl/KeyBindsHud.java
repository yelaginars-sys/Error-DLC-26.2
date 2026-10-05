package error.ui.hud.impl;

import error.Client;
import error.event.list.Render2DEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.render.Interface;
import error.setting.impl.BindSetting;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.KeyUtil;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.render.Render2DUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.*;

public final class KeyBindsHud extends HudElement implements error.IMinecraft {

    private record Entry(String name, String keyName, String categoryIcon, float alpha) {}

    private final Map<Module, Animation> anims = new HashMap<>();
    private final Animation widthAnim = new Animation(80.0F, 0.22F);
    private final Animation heightAnim = new Animation(18.0F, 0.22F);

    private static final float ROW_H = 13.0F;
    private static final float HEADER_H = 14.5F;
    private static final float PILL_R = 6.25F;
    private static final float GAP_Y = 2.0F;

    public KeyBindsHud() {
        super("keybinds", "Hotkeys", 10.0F, 100.0F, 70.0F, 35.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.binds.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private static String getCategoryIcon(Category category) {
        if (category == null) return "•";
        return switch (category) {
            case COMBAT -> Fonts.NURIK_COMBAT;
            case MOVEMENT -> Fonts.NURIK_MOVEMENT;
            case RENDER -> Fonts.NURIK_VISUALS;
            case PLAYER -> Fonts.NURIK_PLAYER;
            case MISC -> Fonts.NURIK_MISC;
            default -> "•";
        };
    }

    @Override
    public void draw(Render2DEvent event) {
        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;

        List<Module> allModules = Client.getInstance() != null && Client.getInstance().getModuleManager() != null
                ? Client.getInstance().getModuleManager().getModules() : Collections.emptyList();

        for (Module m : allModules) {
            BindSetting bs = m.getBind();
            boolean hasValidBind = bs != null && !bs.getValue().isEmpty() && BindSetting.isValidKey(bs.getValue().get(0));
            boolean shouldShow = hasValidBind && m.isState();

            Animation a = anims.computeIfAbsent(m, k -> new Animation(0.0F, 0.20F));
            a.setTarget(shouldShow ? 1.0F : 0.0F);
            a.update();
        }

        anims.entrySet().removeIf(e -> {
            BindSetting bs = e.getKey().getBind();
            boolean valid = bs != null && !bs.getValue().isEmpty() && BindSetting.isValidKey(bs.getValue().get(0));
            return (!valid || !e.getKey().isState()) && e.getValue().getValue() <= 0.01F;
        });

        List<Entry> entries = new ArrayList<>();
        for (Module m : allModules) {
            Animation a = anims.get(m);
            if (a != null && a.getValue() > 0.01F) {
                int key = m.getBind().getValue().get(0);
                String kName = KeyUtil.getKeyName(key);
                entries.add(new Entry(m.getName(), kName, getCategoryIcon(m.getCategory()), a.getValue()));
            }
        }

        // Preview dummy items in chat if empty
        if (entries.isEmpty() && inChat) {
            entries.add(new Entry("AttackAura", "R", Fonts.NURIK_COMBAT, 1.0F));
            entries.add(new Entry("Velocity", "V", Fonts.NURIK_MOVEMENT, 1.0F));
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Calculate dynamic width
        // Calculate dynamic width
        float maxRowW = 68.0F;
        float headerTitleW = Fonts.SF_MEDIUM.getWidth("Hotkeys", 7.5F);
        float headerMinW = 18.0F + headerTitleW + 6.0F;
        maxRowW = Math.max(maxRowW, headerMinW);

        for (Entry e : entries) {
            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 7.0F);
            float keyW = Fonts.SF_MEDIUM.getWidth(e.keyName, 6.5F);
            float rowTotalW = (13.5F + nameW + 6.0F) + 5.0F + (keyW + 9.0F);
            maxRowW = Math.max(maxRowW, rowTotalW);
        }

        float totalH = HEADER_H;
        for (Entry e : entries) {
            totalH += (ROW_H + GAP_Y) * e.alpha;
        }

        widthAnim.setTarget(maxRowW);
        widthAnim.update();
        heightAnim.setTarget(totalH);
        heightAnim.update();

        this.width = widthAnim.getValue();
        this.height = heightAnim.getValue();

        int accent = Theme.getAccentColor();
        float curX = this.x;
        float curY = this.y;
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();

        // 1. Header Capsule matching Energy HUD in Liquid Glass
        Render2D.drawShadow(curX, curY, this.width, HEADER_H, PILL_R, 5.0F, ColorUtil.rgba(0, 0, 0, 70));
        if (extractor != null) {
            Render2DUtil.flush();
            Blur.of(curX, curY, this.width, HEADER_H)
                    .radius(Math.round(PILL_R))
                    .type(BlurType.KAWASE)
                    .strength(4)
                    .tint(Color.rgba(14, 16, 22, 115))
                    .alpha(1.0F)
                    .render(extractor);

            Outline.of(curX, curY, this.width, HEADER_H)
                    .radius(Math.round(PILL_R))
                    .thickness(0.75F)
                    .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 32))
                    .alpha(1.0F)
                    .render(extractor);
        }

        // Header Energy Icon "p" or NURIK_BIND
        Fonts.drawString(Fonts.ENERGY, "p", curX + 5.0F, curY + 2.0F, 8.5F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Hotkeys", curX + 16.0F, curY + 2.2F, 7.5F, 0xFFFFFFFF);

        curY += HEADER_H + GAP_Y;

        // 2. Entries: Left capsule (Icon + Name) and Right capsule (Keybind)
        for (Entry e : entries) {
            if (e.alpha <= 0.01F) continue;

            int textWhite = ColorUtil.rgba(255, 255, 255, (int) (245 * e.alpha));
            int keyColor = ColorUtil.rgba(215, 225, 240, (int) (235 * e.alpha));

            float nameW = Fonts.SF_MEDIUM.getWidth(e.name, 7.0F);
            float keyW = Fonts.SF_MEDIUM.getWidth(e.keyName, 6.5F);

            float leftPillW = 13.5F + nameW + 6.0F;
            float rightPillW = keyW + 9.0F;
            float rightPillX = curX + this.width - rightPillW;

            // Shadows
            Render2D.drawShadow(curX, curY, leftPillW, ROW_H, PILL_R, 3.5F, ColorUtil.rgba(0, 0, 0, (int) (45 * e.alpha)));
            Render2D.drawShadow(rightPillX, curY, rightPillW, ROW_H, PILL_R, 3.5F, ColorUtil.rgba(0, 0, 0, (int) (45 * e.alpha)));

            // Liquid Glass Kawase Blur & Specular Outlines
            if (extractor != null) {
                Render2DUtil.flush();

                // Left Capsule Blur & Outline
                Blur.of(curX, curY, leftPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .type(BlurType.KAWASE)
                        .strength(3)
                        .tint(Color.rgba(14, 16, 22, (int) (110 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);

                Outline.of(curX, curY, leftPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .thickness(0.65F)
                        .verticalGradient(Color.rgba(255, 255, 255, (int) (28 * e.alpha)), Color.rgba(255, 255, 255, (int) (6 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);

                // Right Capsule Blur & Outline
                Blur.of(rightPillX, curY, rightPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .type(BlurType.KAWASE)
                        .strength(3)
                        .tint(Color.rgba(14, 16, 22, (int) (110 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);

                Outline.of(rightPillX, curY, rightPillW, ROW_H)
                        .radius(Math.round(PILL_R))
                        .thickness(0.65F)
                        .verticalGradient(Color.rgba(255, 255, 255, (int) (28 * e.alpha)), Color.rgba(255, 255, 255, (int) (6 * e.alpha)))
                        .alpha(e.alpha)
                        .render(extractor);
            }

            // Category Icon in left capsule
            if (e.categoryIcon != null && !e.categoryIcon.equals("•")) {
                Fonts.drawString(Fonts.ICONS_NURIK, e.categoryIcon, curX + 4.5F, curY + 1.8F, 6.8F, ColorUtil.withAlpha(accent, (int) (230 * e.alpha)));
            } else {
                Render2D.drawCircle(curX + 6.5F, curY + ROW_H * 0.5F, 1.8F, ColorUtil.withAlpha(accent, (int) (230 * e.alpha)));
            }

            // Module Name
            Fonts.drawString(Fonts.SF_MEDIUM, e.name, curX + 13.5F, curY + 1.8F, 7.0F, textWhite);

            // Key name centered in right capsule
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, e.keyName, rightPillX + rightPillW * 0.5F, curY + 1.8F, 6.5F, keyColor);

            curY += (ROW_H + GAP_Y) * e.alpha;
        }
    }
}
