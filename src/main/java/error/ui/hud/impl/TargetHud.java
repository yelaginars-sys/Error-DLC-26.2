package error.ui.hud.impl;

import com.google.gson.JsonObject;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.module.impl.combat.AuraModule;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import error.util.render.Render2DUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
public final class TargetHud extends HudElement implements IMinecraft {

    public enum Mode { BAR, CIRCLE }
    public enum HeadMode { LEFT, INSIDE_CIRCLE }

    private Mode mode = Mode.BAR;
    private HeadMode headMode = HeadMode.LEFT;

    private boolean showOnHover = true;

    private LivingEntity lastHoveredTarget = null;
    private long lastHoverTime = 0;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 190);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);
    private static final int GOLD_COLOR = ColorUtil.rgba(255, 210, 45, 255);
    private static final int RED_COLOR = ColorUtil.rgba(235, 75, 75, 255);

    private float animatedHp = 20.0F;
    private float animatedAbsorption = 0.0F;

    private final List<DigitAnimation> digitAnimations = new ArrayList<>();
    private String lastHpString = "";

    public TargetHud() {
        super("targethud", "Target HUD", 150.0F, 120.0F, 130.0F, 34.0F);
    }

    public JsonObject writeConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("mode", mode.name());
        obj.addProperty("headMode", headMode.name());
        obj.addProperty("showOnHover", showOnHover);
        return obj;
    }

    public void readConfig(JsonObject obj) {
        if (obj == null) return;
        if (obj.has("mode")) {
            try { this.mode = Mode.valueOf(obj.get("mode").getAsString()); } catch (Exception ignored) {}
        }
        if (obj.has("headMode")) {
            try { this.headMode = HeadMode.valueOf(obj.get("headMode").getAsString()); } catch (Exception ignored) {}
        }
        if (obj.has("showOnHover")) {
            this.showOnHover = obj.get("showOnHover").getAsBoolean();
        }
    }

    private String getTargetName(LivingEntity target) {
        if (target instanceof AbstractClientPlayer player) {
            return player.getGameProfile().name();
        }
        return target.getName().getString();
    }

    private LivingEntity resolveTarget() {
        if (!enabled) return null;

        if (Client.INSTANCE != null && Client.INSTANCE.moduleManager != null) {
            AuraModule aura = Client.INSTANCE.moduleManager.getAuraModule();
            if (aura != null && aura.isEnabled() && aura.getTarget() != null && aura.getTarget().isAlive()) {
                return aura.getTarget();
            }
        }

        if (showOnHover) {
            if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living && living.isAlive() && !living.isDeadOrDying()) {
                lastHoveredTarget = living;
                lastHoverTime = System.currentTimeMillis();
            }
            if (lastHoveredTarget != null && lastHoveredTarget.isAlive() && (System.currentTimeMillis() - lastHoverTime < 1800)) {
                return lastHoveredTarget;
            }
        }

        if (HudManager.getInstance().isDraggableScreenOpen()) {
            return mc.player;
        }

        return null;
    }

    @Override
    public boolean shouldRender() {
        return enabled && resolveTarget() != null;
    }

    @Override
    public void draw(Render2DEvent event) {
        LivingEntity target = resolveTarget();
        if (target == null) return;

        float currentHp = target.getHealth();
        float currentAbsorb = target.getAbsorptionAmount();
        this.animatedHp = Mth.lerp(0.18F, this.animatedHp, currentHp);
        this.animatedAbsorption = Mth.lerp(0.18F, this.animatedAbsorption, currentAbsorb);

        if (mode == Mode.BAR) {
            drawBarMode(event, target);
        } else {
            drawCircleMode(event, target);
        }
    }

    private void drawBarMode(Render2DEvent event, LivingEntity target) {
        this.width = 130.0F;
        this.height = 34.0F;

        Render2D.drawShadow(x, y, width, height, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 5.0F, BG_COLOR, 1.0F);

        float avatarSize = 22.0F;
        float avatarX = x + 5.0F;
        float avatarY = y + 6.0F;

        if (target instanceof AbstractClientPlayer player) {
            Render2D.drawHead(player.getSkin().body().texturePath(), avatarX, avatarY, avatarSize, 3.5F);
        } else {
            Render2D.drawRoundedRect(avatarX, avatarY, avatarSize, avatarSize, 3.5F, ColorUtil.rgba(255, 255, 255, 8));
            Fonts.drawCenteredIcon(IconUse.CROSS, avatarX + avatarSize / 2.0F, avatarY + avatarSize / 2.0F - 3.0F, 7.5F, ColorUtil.rgba(140, 145, 160, 180));
        }

        float contentX = avatarX + avatarSize + 5.0F;
        float contentW = width - (contentX - x) - 5.0F;

        String name = getTargetName(target);
        name = Fonts.SF_MEDIUM.trimToWidth(name, 50.0F, 8.5F);
        Fonts.drawString(Fonts.SF_MEDIUM, name, contentX, y + 4.5F, 8.5F, ColorUtil.rgba(255, 255, 255, 255));

        float textHpX = contentX + Fonts.SF_MEDIUM.getWidth(name, 8.5F) + 3.5F;
        String baseHpStr = String.format("%.1f", target.getHealth());
        Fonts.drawString(Fonts.SF_MEDIUM, baseHpStr, textHpX, y + 5.0F, 7.5F, RED_COLOR);
        textHpX += Fonts.SF_MEDIUM.getWidth(baseHpStr, 7.5F) + 2.0F;

        if (target.getAbsorptionAmount() > 0.0F) {
            String absorbStr = String.format("+%.1f", target.getAbsorptionAmount());
            Fonts.drawString(Fonts.SF_MEDIUM, absorbStr, textHpX, y + 5.0F, 7.5F, GOLD_COLOR);
        }

        drawEquipmentRow(event, target, contentX, y + 14.5F, contentW);
        drawHealthBar(contentX, y + 26.5F, contentW, 2.5F, target);
    }

    private void drawCircleMode(Render2DEvent event, LivingEntity target) {
        this.width = 130.0F;
        this.height = 34.0F;

        Render2D.drawShadow(x, y, width, height, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 5.0F, BG_COLOR, 1.0F);

        float circleR = 10.5F;
        float ringThickness = 2.0F;
        float circleCenterX = x + width - circleR - 6.0F;
        float circleCenterY = y + height / 2.0F;

        boolean isPlayer = target instanceof AbstractClientPlayer;
        float avatarX = x + 5.0F;
        float avatarY = y + 6.0F;
        float contentX = x + 5.0F;

        if (headMode == HeadMode.LEFT) {
            if (isPlayer) {
                Render2D.drawHead(((AbstractClientPlayer) target).getSkin().body().texturePath(), avatarX, avatarY, 22.0F, 3.5F);
            } else {
                Render2D.drawRoundedRect(avatarX, avatarY, 22.0F, 22.0F, 3.5F, ColorUtil.rgba(255, 255, 255, 8));
                Fonts.drawCenteredIcon(IconUse.CROSS, avatarX + 11.0F, avatarY + 8.5F, 7.5F, ColorUtil.rgba(140, 145, 160, 180));
            }
            contentX += 27.0F;
        }

        String name = getTargetName(target);
        name = Fonts.SF_MEDIUM.trimToWidth(name, 44.0F, 8.5F);
        Fonts.drawString(Fonts.SF_MEDIUM, name, contentX, y + 5.0F, 8.5F, ColorUtil.rgba(255, 255, 255, 255));

        float maxEquipW = circleCenterX - circleR - contentX - 4.0F;
        drawEquipmentRow(event, target, contentX, y + 16.5F, maxEquipW);

        Render2D.drawCircleOutline(circleCenterX, circleCenterY, circleR, ringThickness, ColorUtil.rgba(25, 30, 40, 220));

        if (headMode == HeadMode.INSIDE_CIRCLE && isPlayer) {
            AbstractClientPlayer clientPlayer = (AbstractClientPlayer) target;
            Render2D.drawHead(clientPlayer.getSkin().body().texturePath(), circleCenterX - circleR + 2.0F, circleCenterY - circleR + 2.0F, (circleR - 2.0F) * 2, circleR - 2.0F);
        } else {
            int totalInt = (int) Math.ceil(animatedHp + animatedAbsorption);
            drawRollingDigits(String.valueOf(totalInt), circleCenterX, circleCenterY, 8.0F);
        }

        float maxHp = Math.max(1.0F, target.getMaxHealth());
        float effectiveMax = Math.max(maxHp, animatedHp + animatedAbsorption);

        float hpFrac = Math.max(0.0F, Math.min(1.0F, animatedHp / effectiveMax));
        float absFrac = Math.max(0.0F, Math.min(1.0F - hpFrac, animatedAbsorption / effectiveMax));

        float hpSweep = 360.0F * hpFrac;
        float absSweep = 360.0F * absFrac;

        int hpColor = getHealthColor(target);

        if (hpSweep > 0.5F) {
            Render2D.drawArc(circleCenterX, circleCenterY, circleR, ringThickness, -90.0F, hpSweep, hpColor);
        }

        if (absSweep > 0.5F) {
            Render2D.drawArc(circleCenterX, circleCenterY, circleR, ringThickness, -90.0F + hpSweep, absSweep, GOLD_COLOR);
        }
    }

    private void drawEquipmentRow(Render2DEvent event, LivingEntity target, float areaX, float itemY, float areaWidth) {
        EquipmentSlot[] slots = {
                EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND
        };

        float slotSize = 9.0F;
        float gap = 1.5F;
        Render2DUtil.flush();
        var extractor = event.getGuiGraphicsExtractor();

        float startX = areaX;
        for (EquipmentSlot slot : slots) {
            ItemStack stack = target.getItemBySlot(slot);
            Render2D.drawRoundedRect(startX, itemY, slotSize, slotSize, 2.0F, ColorUtil.rgba(255, 255, 255, 10));

            if (stack.isEmpty()) {
                Fonts.drawCenteredIcon(IconUse.CROSS, startX + slotSize / 2.0F, itemY + 1.5F, 5.5F, ColorUtil.rgba(140, 145, 160, 140));
            } else if (extractor != null) {
                Render2DUtil.flush();
                extractor.pose().pushMatrix();
                extractor.pose().translate(startX, itemY);
                float scale = slotSize / 16.0F;
                extractor.pose().scale(scale, scale);
                extractor.item(stack, 0, 0);
                extractor.pose().popMatrix();
            }
            startX += slotSize + gap;
        }
    }

    private void drawHealthBar(float bx, float by, float bw, float bh, LivingEntity target) {
        float maxHp = Math.max(1.0F, target.getMaxHealth());
        float effectiveMax = Math.max(maxHp, animatedHp + animatedAbsorption);

        float hpWidth = Math.max(0.0F, Math.min(bw, (animatedHp / effectiveMax) * bw));
        float totalWidth = Math.max(0.0F, Math.min(bw, ((animatedHp + animatedAbsorption) / effectiveMax) * bw));

        Render2D.drawRoundedRect(bx, by, bw, bh, bh / 2.0F, ColorUtil.rgba(255, 255, 255, 14));

        if (totalWidth > 0.5F) {
            Render2D.drawRoundedRect(bx, by, totalWidth, bh, bh / 2.0F, GOLD_COLOR);
        }

        if (hpWidth > 0.5F) {
            Render2D.drawRoundedRect(bx, by, hpWidth, bh, bh / 2.0F, getHealthColor(target));
        }
    }

    private void drawRollingDigits(String currentStr, float centerX, float centerY, float fontSize) {
        if (!currentStr.equals(lastHpString)) {
            updateDigitAnimations(lastHpString, currentStr);
            lastHpString = currentStr;
        }

        float totalW = Fonts.SF_MEDIUM.getWidth(currentStr, fontSize);
        float curX = centerX - totalW / 2.0F;
        float textY = Fonts.SF_MEDIUM.centeredTextY(centerY, fontSize);

        for (int i = 0; i < currentStr.length(); i++) {
            char targetChar = currentStr.charAt(i);
            float charW = Fonts.SF_MEDIUM.getWidth(String.valueOf(targetChar), fontSize);

            if (i < digitAnimations.size()) {
                DigitAnimation anim = digitAnimations.get(i);
                anim.update();
                float progress = anim.anim.getValue();

                if (anim.isAnimating()) {
                    float offsetOld = anim.direction * progress * 6.0F;
                    float offsetNew = -anim.direction * (1.0F - progress) * 6.0F;

                    int alphaOld = (int) ((1.0F - progress) * 255);
                    int alphaNew = (int) (progress * 255);

                    Fonts.drawString(Fonts.SF_MEDIUM, String.valueOf(anim.oldChar), curX, textY + offsetOld, fontSize, ColorUtil.rgba(255, 255, 255, alphaOld));
                    Fonts.drawString(Fonts.SF_MEDIUM, String.valueOf(targetChar), curX, textY + offsetNew, fontSize, ColorUtil.rgba(255, 255, 255, alphaNew));
                } else {
                    Fonts.drawString(Fonts.SF_MEDIUM, String.valueOf(targetChar), curX, textY, fontSize, ColorUtil.rgba(255, 255, 255, 255));
                }
            } else {
                Fonts.drawString(Fonts.SF_MEDIUM, String.valueOf(targetChar), curX, textY, fontSize, ColorUtil.rgba(255, 255, 255, 255));
            }
            curX += charW;
        }
    }

    private void updateDigitAnimations(String oldStr, String newStr) {
        digitAnimations.clear();
        int oldVal = parseSafeInt(oldStr);
        int newVal = parseSafeInt(newStr);
        int dir = newVal > oldVal ? -1 : 1;

        for (int i = 0; i < newStr.length(); i++) {
            char nCh = newStr.charAt(i);
            char oCh = (i < oldStr.length()) ? oldStr.charAt(i) : ' ';
            DigitAnimation anim = new DigitAnimation(oCh, nCh, dir);
            if (oCh != nCh && oCh != ' ') anim.start();
            digitAnimations.add(anim);
        }
    }

    private int parseSafeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static class DigitAnimation {
        char oldChar, newChar;
        int direction;
        Animation anim = new Animation(0.0F, 0.16F);
        boolean animating = false;

        DigitAnimation(char oldChar, char newChar, int direction) {
            this.oldChar = oldChar; this.newChar = newChar; this.direction = direction;
        }
        void start() { animating = true; anim.setValue(0.0F); anim.setTarget(1.0F); }
        void update() { anim.update(); if (anim.getValue() >= 0.99F) animating = false; }
        boolean isAnimating() { return animating; }
    }

    private int getHealthColor(LivingEntity target) {
        float max = Math.max(1.0F, target.getMaxHealth());
        float percent = Math.max(0.0F, Math.min(1.0F, target.getHealth() / max));
        return ColorUtil.rgba((int) (255 * (1.0F - percent)), (int) (225 * percent + 30), 80, 255);
    }

    @Override
    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        float menuW = 130.0F;
        float menuH = (mode == Mode.CIRCLE ? 56.0F : 40.0F);

        Render2D.drawShadow(menuX, menuY, menuW, menuH, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(menuX, menuY, menuW, menuH, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(14, 14, 18, 240), alpha), 1.0F);

        Fonts.drawString(Fonts.SF_MEDIUM, "Style", menuX + 6.0F, menuY + 6.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        drawChip(menuX + 48.0F, menuY + 5.0F, 34.0F, 11.5F, "Bar", mode == Mode.BAR, alpha);
        drawChip(menuX + 86.0F, menuY + 5.0F, 38.0F, 11.5F, "Circle", mode == Mode.CIRCLE, alpha);

        float curY = menuY + 21.0F;
        if (mode == Mode.CIRCLE) {
            Fonts.drawString(Fonts.SF_MEDIUM, "Head", menuX + 6.0F, curY + 1.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
            drawChip(menuX + 48.0F, curY, 34.0F, 11.5F, "Left", headMode == HeadMode.LEFT, alpha);
            drawChip(menuX + 86.0F, curY, 38.0F, 11.5F, "Inside", headMode == HeadMode.INSIDE_CIRCLE, alpha);
            curY += 16.0F;
        }

        Fonts.drawString(Fonts.SF_MEDIUM, "Hover", menuX + 6.0F, curY + 1.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));
        drawChip(menuX + 48.0F, curY, 76.0F, 11.5F, showOnHover ? "Enabled" : "Disabled", showOnHover, alpha);

        return menuH;
    }

    private void drawChip(float cx, float cy, float cw, float ch, String text, boolean active, float alpha) {
        int bg = active ? Theme.getAccentColor() : 0x351C1F2E;
        int textCol = active ? 0xFFFFFFFF : Theme.TEXT_MUTED;
        Render2D.drawRoundedRect(cx, cy, cw, ch, 2.5F, ColorUtil.multiplyAlpha(bg, alpha));
        Fonts.drawCenteredString(Fonts.SF_MEDIUM, text, cx + cw / 2.0F, cy + 2.0F, 7.5F, ColorUtil.multiplyAlpha(textCol, alpha));
    }

    @Override
    public boolean handleContextMenuClick(float menuX, float menuY, double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float menuW = 130.0F;
        float menuH = (mode == Mode.CIRCLE ? 56.0F : 40.0F);

        if (mouseY >= menuY + 4.0F && mouseY <= menuY + 18.0F) {
            if (mouseX >= menuX + 48.0F && mouseX <= menuX + 82.0F) { this.mode = Mode.BAR; return true; }
            if (mouseX >= menuX + 86.0F && mouseX <= menuX + 124.0F) { this.mode = Mode.CIRCLE; return true; }
        }

        float curY = menuY + 20.0F;

        if (mode == Mode.CIRCLE) {
            if (mouseY >= curY && mouseY <= curY + 14.0F) {
                if (mouseX >= menuX + 48.0F && mouseX <= menuX + 82.0F) { this.headMode = HeadMode.LEFT; return true; }
                if (mouseX >= menuX + 86.0F && mouseX <= menuX + 124.0F) { this.headMode = HeadMode.INSIDE_CIRCLE; return true; }
            }
            curY += 16.0F;
        }

        if (mouseY >= curY && mouseY <= curY + 14.0F && mouseX >= menuX + 48.0F && mouseX <= menuX + 124.0F) {
            this.showOnHover = !this.showOnHover;
            return true;
        }

        return mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + menuH;
    }
}