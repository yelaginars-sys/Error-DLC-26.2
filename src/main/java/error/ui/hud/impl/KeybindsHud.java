package error.ui.hud.impl;

import error.Client;
import error.IMinecraft;
import error.util.client.clients.Theme;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.module.Module;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Create by daun kvass
 */
public final class KeybindsHud extends HudElement implements IMinecraft {

    private static final float HEADER_HEIGHT = 14.0F;
    private static final float ROW_HEIGHT = 11.5F;
    private static final float PADDING_X = 5.0F;
    private static final float PADDING_BOTTOM = 4.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 195);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);

    private final List<AnimatedBind> animatedBinds = new ArrayList<>();
    private long lastTime = System.currentTimeMillis();
    private float animatedWidth = 85.0F;

    public KeybindsHud() {
        super("hotkeys", "HotKeys", 10.0F, 80.0F, 85.0F, HEADER_HEIGHT + PADDING_BOTTOM);
    }

    private static class AnimatedBind {
        final String name;
        String keyName;
        float progress = 0.0F;
        boolean removing = false;

        AnimatedBind(String name, String keyName) {
            this.name = name;
            this.keyName = keyName;
        }
    }

    private record RawBind(String name, String keyName) {}

    private List<RawBind> getCurrentBinds() {
        List<RawBind> list = new ArrayList<>();
        if (Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
            for (Module m : Client.INSTANCE.moduleManager.getModules()) {
                if (m.isEnabled() && m.getBind() != null && m.getBind().isBound()) {
                    String display = m.getBind().getDisplayValue();
                    if (!display.equalsIgnoreCase("NONE")) {
                        list.add(new RawBind(m.getName(), display));
                    }
                }
            }
        }

        if (list.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            list.add(new RawBind("Attack Aura", "R"));
        }

        return list;
    }

    private void updateAnimations(float delta) {
        List<RawBind> current = getCurrentBinds();

        for (AnimatedBind anim : animatedBinds) {
            RawBind match = current.stream()
                    .filter(b -> b.name.equalsIgnoreCase(anim.name))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                anim.keyName = match.keyName;
                anim.removing = false;
            } else {
                anim.removing = true;
            }
        }

        for (RawBind b : current) {
            boolean exists = animatedBinds.stream().anyMatch(a -> a.name.equalsIgnoreCase(b.name));
            if (!exists) {
                animatedBinds.add(new AnimatedBind(b.name, b.keyName));
            }
        }

        float speed = 12.0F;
        Iterator<AnimatedBind> it = animatedBinds.iterator();
        while (it.hasNext()) {
            AnimatedBind anim = it.next();
            float target = anim.removing ? 0.0F : 1.0F;
            anim.progress += (target - anim.progress) * Math.min(1.0F, delta * speed);

            if (anim.removing && anim.progress <= 0.01F) {
                it.remove();
            }
        }
    }

    @Override
    public boolean shouldRender() {
        return enabled && (!animatedBinds.isEmpty() || !getCurrentBinds().isEmpty() || HudManager.getInstance().isDraggableScreenOpen());
    }

    @Override
    public List<Box> getCollisionBoxes() {
        if (width <= 0 || height <= 0) return List.of();
        return List.of(new Box(x, y, width, height, this));
    }

    @Override
    public void draw(Render2DEvent event) {
        long now = System.currentTimeMillis();
        float delta = Math.min(1.0F, (now - lastTime) / 1000.0F);
        lastTime = now;

        updateAnimations(delta);

        if (animatedBinds.isEmpty() && !HudManager.getInstance().isDraggableScreenOpen()) {
            return;
        }

        float titleW = Fonts.SF_MEDIUM.getWidth("HotKeys", 8.5F) + 16.0F;
        float maxContentW = titleW;
        float totalRowsHeight = 0.0F;

        for (AnimatedBind b : animatedBinds) {
            if (b.progress > 0.05F) {
                float rowW = Fonts.SF_MEDIUM.getWidth(b.name, 8.0F) + Fonts.SF_MEDIUM.getWidth("[" + b.keyName + "]", 8.0F) + 14.0F;
                maxContentW = Math.max(maxContentW, rowW);
            }
            totalRowsHeight += ROW_HEIGHT * b.progress;
        }

        float targetWidth = maxContentW + PADDING_X * 2.0F;
        float targetHeight = HEADER_HEIGHT + totalRowsHeight + (totalRowsHeight > 0.5F ? PADDING_BOTTOM : 0.0F);

        animatedWidth += (targetWidth - animatedWidth) * Math.min(1.0F, delta * 14.0F);

        this.width = animatedWidth;
        this.height = targetHeight;

        Render2D.drawShadow(x, y, width, height, 4.0F, 7.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 4.0F, BG_COLOR, 1.0F);

        Fonts.drawIcon(IconUse.KEYBIND, x + PADDING_X, y + 3.5F, 8.0F, ColorUtil.withAlpha(Theme.getAccentColor(),240));
        Fonts.drawString(Fonts.SF_MEDIUM, "HotKeys", x + PADDING_X + 10.5F, y + 3.5F, 8.5F, 0xFFFFFFFF);

        float curY = y + HEADER_HEIGHT + 1.0F;
        for (AnimatedBind b : animatedBinds) {
            float rowH = ROW_HEIGHT * b.progress;
            if (b.progress <= 0.02F) continue;

            int alpha = (int) (Math.max(0.0F, Math.min(1.0F, b.progress)) * 255.0F);
            int textColor = ColorUtil.rgba(224, 224, 224, alpha);
            int keyColor = ColorUtil.rgba(255, 255, 255, alpha);

            float slideOffset = (1.0F - b.progress) * -4.0F;

            float rowCenterY = curY + (rowH - 8.0F) / 2.0F;

            Fonts.drawString(Fonts.SF_MEDIUM, b.name, x + PADDING_X + slideOffset, rowCenterY, 8.0F, textColor);

            String keyStr = "[" + b.keyName + "]";
            float kw = Fonts.SF_MEDIUM.getWidth(keyStr, 8.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, keyStr, x + width - PADDING_X - kw, rowCenterY, 8.0F, keyColor);

            curY += rowH;
        }
    }
}