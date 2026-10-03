package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.Module;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import net.minecraft.client.gui.screens.ChatScreen;

import java.util.ArrayList;
import java.util.List;

public final class HotkeysHud extends HudElement implements IMinecraft {
    private final Animation heightAnim = new Animation(0.0F, 0.22F);
    private final Animation chatOffsetAnim = new Animation(0.0F, 0.22F);

    public HotkeysHud() {
        super("hotkeys", "Hotkeys", 10.0F, 200.0F, 125.0F, 30.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (!isEnabled() || mc.player == null) return;

        List<Module> activeKeys = new ArrayList<>();
        if (Client.getInstance() != null && Client.getInstance().getModuleManager() != null) {
            for (Module m : Client.getInstance().getModuleManager().getModules()) {
                if (m.isEnabled() && m.getBind() != null && m.getBind().isBound()) {
                    activeKeys.add(m);
                }
            }
        }

        fadeAnim.setTarget(activeKeys.isEmpty() ? 0.0F : 1.0F);
        fadeAnim.update();
        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float headerH = 18.0F;
        float itemH = 12.0F;
        float targetH = headerH + (activeKeys.size() * itemH) + 4.0F;

        heightAnim.setTarget(targetH);
        heightAnim.update();
        this.height = heightAnim.getValue();

        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
        chatOffsetAnim.setTarget(chatOpen ? -20.0F : 0.0F);
        chatOffsetAnim.update();

        float renderY = (dragging ? getY() : getY()) + chatOffsetAnim.getValue();
        float renderX = getX();

        int accent = Theme.getAccentColor();
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));
        int glassFill = ColorUtil.rgba(16, 18, 26, (int) (205 * alpha));
        int glassBorder = ColorUtil.rgba(255, 255, 255, (int) (35 * alpha));

        Render2D.drawShadow(renderX, renderY, width, height, 8.0F, 8.0F, shadowCol);
        Render2D.drawBlur(renderX, renderY, width, height, 8.0F, 14.0F, glassFill, alpha);
        Render2D.drawRoundedRect(renderX, renderY, width, height, 8.0F, glassFill);
        Render2D.drawRoundedOutline(renderX, renderY, width, height, 8.0F, 1.0F, glassBorder);

        // Header: Hotkeys Title + Keyboard Icon
        Fonts.drawString(Fonts.SF_MEDIUM, "Hotkeys", renderX + 8.0F, renderY + 4.5F, 7.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * alpha)));
        Fonts.drawIcon(IconUse.KEYBOARD, renderX + width - 16.0F, renderY + 4.5F, 7.5F, ColorUtil.multiplyAlpha(accent, alpha));

        // Line Divider
        Render2D.drawRoundedRect(renderX + 6.0F, renderY + headerH, width - 12.0F, 1.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int) (20 * alpha)));

        float curY = renderY + headerH + 3.0F;
        Render2D.pushScissor(renderX, renderY + headerH, width, height - headerH);
        for (Module m : activeKeys) {
            IconUse icon = getCategoryIcon(m.getCategory());
            Fonts.drawIcon(icon, renderX + 8.0F, curY + 1.0F, 6.5F, ColorUtil.multiplyAlpha(accent, alpha));
            Fonts.drawString(Fonts.SF_MEDIUM, m.getName(), renderX + 18.0F, curY + 1.0F, 6.5F, ColorUtil.rgba(240, 240, 255, (int) (230 * alpha)));

            String keyText = m.getBind().getDisplayValue();
            float keyW = Fonts.SF_MEDIUM.getWidth(keyText, 6.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, keyText, renderX + width - keyW - 8.0F, curY + 1.0F, 6.0F, ColorUtil.rgba(180, 185, 205, (int) (190 * alpha)));
            curY += itemH;
        }
        Render2D.popScissor();
    }

    private IconUse getCategoryIcon(error.module.Category category) {
        return switch (category) {
            case COMBAT -> IconUse.FIGHT;
            case MOVEMENT -> IconUse.MOVEMENT;
            case RENDER -> IconUse.RENDER;
            case PLAYER -> IconUse.PLAYER;
            case MISC -> IconUse.MISC;
            default -> IconUse.SCRIPT;
        };
    }
}
