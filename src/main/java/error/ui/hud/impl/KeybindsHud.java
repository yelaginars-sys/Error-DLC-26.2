package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.Module;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class KeybindsHud extends HudElement implements IMinecraft {

    public static final int ACCENT_PURPLE = ColorUtil.rgba(166, 130, 255, 255);
    private static final float HEADER_HEIGHT = 14.0F;

    public KeybindsHud() {
        super("keybinds", "Hotkeys", 6.0F, 120.0F, 100.0F, HEADER_HEIGHT + 14.0F);
    }

    private String getKeyName(int key) {
        if (key <= 0) return "NONE";
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "ALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            default -> "K" + key;
        };
    }

    private int getModuleKey(Module m) {
        if (m != null && m.getBind() != null && m.getBind().isBound() && !m.getBind().getValue().isEmpty()) {
            return m.getBind().getValue().get(0);
        }
        return 0;
    }

    @Override
    public void draw(Render2DEvent event) {
        List<Module> bound = new ArrayList<>();
        if (error.Client.INSTANCE != null && error.Client.INSTANCE.moduleManager != null) {
            for (Module m : error.Client.INSTANCE.moduleManager.getModules()) {
                if (m.isEnabled() && getModuleKey(m) > 0) {
                    bound.add(m);
                }
            }
        }

        boolean editing = isDragging() || (mc.gui != null && mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen);
        fadeAnim.setTarget((!bound.isEmpty() || editing) ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        float drawX = getX();
        float drawY = getY();

        float padX = 4.0F;
        float headerH = HEADER_HEIGHT;
        float itemH = 12.0F;
        float radius = 5.0F;

        String title = "Hotkeys";
        float maxNameW = Fonts.SF_MEDIUM.getWidth(title, 6.0F);
        float maxBindW = 0.0F;

        if (bound.isEmpty() && editing) {
            maxNameW = Math.max(maxNameW, Fonts.SF_MEDIUM.getWidth("Elytra Target", 6.0F));
            maxBindW = Math.max(maxBindW, Fonts.SF_MEDIUM.getWidth("X", 6.0F));
        } else {
            for (Module m : bound) {
                maxNameW = Math.max(maxNameW, Fonts.SF_MEDIUM.getWidth(m.getName(), 6.0F));
                maxBindW = Math.max(maxBindW, Fonts.SF_MEDIUM.getWidth(getKeyName(getModuleKey(m)), 6.0F));
            }
        }

        float width = Math.max(90.0F, padX * 2.0F + maxNameW + maxBindW + 18.0F);
        int itemCount = bound.isEmpty() && editing ? 1 : bound.size();
        float height = headerH + itemCount * itemH + 3.0F;

        this.width = width;
        this.height = height;

        int primaryColor = ACCENT_PURPLE;
        int glowColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 15));
        int borderColor = ColorUtil.applyAlpha(primaryColor, (int) (alpha * 40));
        int bgColor = ColorUtil.rgba(14, 14, 18, (int) (160 * alpha));
        int headerBg = ColorUtil.rgba(0, 0, 0, (int) (160 * alpha));

        // Background (Waper Style)
        Render2D.drawRoundedRect(drawX - 2.0F, drawY - 2.0F, width + 4.0F, height + 4.0F, radius + 2.0F, glowColor);
        Render2D.drawRoundedRect(drawX - 0.5F, drawY - 0.5F, width + 1.0F, height + 1.0F, radius + 0.5F, borderColor);
        Render2D.drawRoundedRect(drawX, drawY, width, height, radius, bgColor);

        // Header
        Render2D.drawRoundedRect(drawX, drawY, width, headerH, radius, headerBg);
        Fonts.drawString(Fonts.SF_MEDIUM, title, drawX + padX, drawY + 3.5F, 6.0F, ColorUtil.applyAlpha(primaryColor, alpha));

        // Rows
        float currentY = drawY + headerH + 2.0F;
        if (bound.isEmpty() && editing) {
            renderKeyRow(drawX, currentY, width, "Elytra Target", "X", alpha);
        } else {
            for (Module m : bound) {
                renderKeyRow(drawX, currentY, width, m.getName(), getKeyName(getModuleKey(m)), alpha);
                currentY += itemH;
            }
        }
    }

    private void renderKeyRow(float x, float y, float width, String name, String bind, float alpha) {
        float padX = 4.0F;
        Fonts.drawString(Fonts.SF_MEDIUM, name, x + padX, y + 2.5F, 6.0F, ColorUtil.applyAlpha(ColorUtil.WHITE, alpha));

        float bindW = Fonts.SF_MEDIUM.getWidth(bind, 5.5F) + 6.0F;
        float bindH = 10.0F;
        float bindX = x + width - padX - bindW;
        float bindY = y + 1.0F;

        Render2D.drawRoundedRect(bindX, bindY, bindW, bindH, 3.0F, ColorUtil.rgba(25, 27, 36, (int) (220 * alpha)));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, bind, bindX + bindW / 2.0F, bindY + 2.0F, 5.5F, ColorUtil.applyAlpha(ColorUtil.rgba(200, 200, 210, 255), alpha));
    }
}