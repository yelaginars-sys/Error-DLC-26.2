package error.ui.hud.impl;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.lwjgl.glfw.GLFW;
import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import error.util.render.Render2DUtil;

import java.util.*;

/**
 * Create by daun kvass
 */
public final class PotionsHud extends HudElement implements IMinecraft {

    public enum Mode { CARDS, LIST }

    private Mode mode = Mode.LIST;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 190);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 150);
    private static final int BAR_TRACK = ColorUtil.rgba(255, 255, 255, 16);
    private static final int HEADER_DIVIDER = ColorUtil.rgba(255, 255, 255, 12);

    private static final int COLOR_RED = ColorUtil.rgba(235, 75, 75, 255);
    private static final int COLOR_YELLOW = ColorUtil.rgba(245, 190, 45, 255);
    private static final int COLOR_GREEN = ColorUtil.rgba(65, 220, 120, 255);

    private static class AnimatedPotion {
        final Identifier texture;
        String name;
        String durationStr;
        boolean harmful;
        float fraction;
        float progress = 0.0F;
        boolean removing = false;

        AnimatedPotion(Identifier texture, String name, String durationStr, boolean harmful, float fraction) {
            this.texture = texture;
            this.name = name;
            this.durationStr = durationStr;
            this.harmful = harmful;
            this.fraction = fraction;
        }
    }

    private final List<AnimatedPotion> animatedCards = new ArrayList<>();
    private final Map<Identifier, Integer> maxDurations = new HashMap<>();

    private long lastTime = System.currentTimeMillis();
    private float animatedWidth = 96.0F;
    private float animatedHeight = 48.0F;

    public PotionsHud() {
        super("potionshud", "Potions", 10.0F, 160.0F, 96.0F, 48.0F);
    }

    public JsonObject writeConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("mode", mode.name());
        return obj;
    }

    public void readConfig(JsonObject obj) {
        if (obj == null) return;
        if (obj.has("mode")) {
            try { this.mode = Mode.valueOf(obj.get("mode").getAsString()); } catch (Exception ignored) {}
        }
    }

    private void updateCards(float delta) {
        Map<Identifier, Integer> seen = new HashMap<>();
        List<AnimatedPotion> current = new ArrayList<>();

        if (mc.player != null) {
            Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
            for (MobEffectInstance instance : effects) {
                Holder<MobEffect> holder = instance.getEffect();
                MobEffect effect = holder.value();

                Identifier effectId = holder.unwrapKey().map(k -> k.identifier()).orElse(null);
                if (effectId == null) continue;

                Identifier iconTex = effectId.withPath(path -> "textures/mob_effect/" + path + ".png");
                boolean harmful = effect.getCategory() == MobEffectCategory.HARMFUL;

                float fraction;
                String timeStr;
                if (instance.isInfiniteDuration()) {
                    fraction = 1.0F;
                    timeStr = "∞";
                } else {
                    int duration = instance.getDuration();
                    int max = Math.max(duration, maxDurations.getOrDefault(iconTex, 0));
                    seen.put(iconTex, max);
                    fraction = max > 0 ? (float) duration / max : 0.0F;

                    int totalSec = duration / 20;
                    timeStr = String.format("%d:%02d", totalSec / 60, totalSec % 60);
                }

                String name = effect.getDisplayName().getString();
                if (instance.getAmplifier() > 0) {
                    name += " " + (instance.getAmplifier() + 1);
                }

                current.add(new AnimatedPotion(iconTex, name, timeStr, harmful, fraction));
            }
        }

        if (current.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            current.add(new AnimatedPotion(effectIcon("speed"), "Speed 2", "1:30", false, 0.75F));
            current.add(new AnimatedPotion(effectIcon("strength"), "Strength 1", "0:45", false, 0.40F));
            current.add(new AnimatedPotion(effectIcon("slowness"), "Slowness 1", "0:12", true, 0.20F));
        }

        maxDurations.clear();
        maxDurations.putAll(seen);

        for (AnimatedPotion anim : animatedCards) {
            AnimatedPotion match = current.stream()
                    .filter(c -> c.texture.equals(anim.texture) && c.name.equals(anim.name))
                    .findFirst()
                    .orElse(null);

            if (match != null) {
                anim.durationStr = match.durationStr;
                anim.fraction = match.fraction;
                anim.harmful = match.harmful;
                anim.removing = false;
            } else {
                anim.removing = true;
            }
        }

        for (AnimatedPotion c : current) {
            boolean exists = animatedCards.stream().anyMatch(a -> a.texture.equals(c.texture) && a.name.equals(c.name));
            if (!exists) {
                animatedCards.add(c);
            }
        }

        float animSpeed = 10.0F;
        Iterator<AnimatedPotion> iterator = animatedCards.iterator();
        while (iterator.hasNext()) {
            AnimatedPotion anim = iterator.next();
            float target = anim.removing ? 0.0F : 1.0F;
            anim.progress += (target - anim.progress) * Math.min(1.0F, delta * animSpeed);

            if (anim.removing && anim.progress <= 0.01F) {
                iterator.remove();
            }
        }
    }

    private static Identifier effectIcon(String name) {
        return Identifier.withDefaultNamespace("textures/mob_effect/" + name + ".png");
    }

    @Override
    public boolean shouldRender() {
        if (!enabled) return false;
        if (HudManager.getInstance().isDraggableScreenOpen()) return true;
        return !animatedCards.isEmpty() || (mc.player != null && !mc.player.getActiveEffects().isEmpty());
    }

    @Override
    public void draw(Render2DEvent event) {
        long now = System.currentTimeMillis();
        float delta = Math.min(1.0F, (now - lastTime) / 1000.0F);
        lastTime = now;

        updateCards(delta);

        if (animatedCards.isEmpty() && !HudManager.getInstance().isDraggableScreenOpen()) {
            this.width = 0;
            this.height = 0;
            return;
        }

        if (mode == Mode.CARDS) {
            drawCardsMode(event, delta);
        } else {
            drawListMode(event, delta);
        }
    }

    private void drawCardsMode(Render2DEvent event, float delta) {
        float padding = 4.5F;
        float headerH = 16.0F;
        float cardW = 27.0F;
        float cardH = 28.0F;
        float gap = 3.5F;
        float iconSize = 11.5F;
        float barH = 1.5F;
        float barW = 15.0F;

        float totalCardsW = 0.0F;
        int visibleCount = 0;
        for (AnimatedPotion card : animatedCards) {
            if (card.progress > 0.01F) {
                totalCardsW += (cardW * card.progress);
                visibleCount++;
            }
        }
        if (visibleCount > 1) {
            totalCardsW += (visibleCount - 1) * gap;
        }

        float calculatedW = (padding * 2.0F) + totalCardsW;
        float targetW = Math.max(76.0F, calculatedW);
        float targetH = headerH + cardH + padding + 2.0F;

        animatedWidth += (targetW - animatedWidth) * Math.min(1.0F, delta * 12.0F);
        animatedHeight += (targetH - animatedHeight) * Math.min(1.0F, delta * 12.0F);

        this.width = animatedWidth;
        this.height = animatedHeight;

        Render2D.drawShadow(x, y, width, height, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 5.0F, BG_COLOR, 1.0F);

        drawHeader();

        var extractor = event.getGuiGraphicsExtractor();
        float curX = x + padding;
        float cardY = y + headerH + 1.5F;

        for (int i = 0; i < animatedCards.size(); i++) {
            AnimatedPotion card = animatedCards.get(i);
            if (card.progress <= 0.02F) continue;

            float currentCardW = cardW * card.progress;
            float cardCenterX = curX + currentCardW / 2.0F;
            float alphaMul = card.progress;

            if (i > 0 && currentCardW > 5.0F) {
                int divCol = ColorUtil.multiplyAlpha(HEADER_DIVIDER, alphaMul);
                Render2D.drawRoundedRect(curX - (gap / 2.0F) - 0.25F, cardY + 3.0F, 0.5F, cardH - 6.0F, 0.25F, divCol);
            }

            float curIconSize = iconSize * card.progress;
            float iconX = cardCenterX - (curIconSize / 2.0F);
            float iconY = cardY + 1.5F + (iconSize - curIconSize) / 2.0F;

            if (extractor != null && curIconSize > 2.0F) {
                Render2DUtil.flush();
                extractor.pose().pushMatrix();
                extractor.pose().translate(iconX, iconY);
                float scale = curIconSize / 18.0F;
                extractor.pose().scale(scale, scale);
                extractor.blit(card.texture, 0, 0, 18, 18, 0.0F, 1.0F, 0.0F, 1.0F);
                extractor.pose().popMatrix();
            }

            int textColor = card.harmful ? COLOR_RED : ColorUtil.rgba(230, 235, 245, 255);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, card.durationStr, cardCenterX, cardY + 14.5F, 7.0F * card.progress, ColorUtil.multiplyAlpha(textColor, alphaMul));

            float curBarW = barW * card.progress;
            float bx = cardCenterX - (curBarW / 2.0F);
            float by = cardY + cardH - barH - 2.0F;
            Render2D.drawRoundedRect(bx, by, curBarW, barH, barH / 2.0F, ColorUtil.multiplyAlpha(BAR_TRACK, alphaMul));

            float fillW = Math.max(0.0F, curBarW * card.fraction);
            if (fillW > 0.4F) {
                int barColor = getBarColor(card);
                Render2D.drawRoundedRect(bx, by, fillW, barH, barH / 2.0F, ColorUtil.multiplyAlpha(barColor, alphaMul));
            }

            curX += currentCardW + (gap * card.progress);
        }
    }

    private void drawListMode(Render2DEvent event, float delta) {
        float padding = 4.0F;
        float headerH = 16.0F;
        float rowH = 15.0F;
        float iconSize = 10.5F;

        float maxW = 75.0F;
        float totalListH = 0.0F;

        for (AnimatedPotion c : animatedCards) {
            if (c.progress > 0.05F) {
                float tw = Fonts.SF_MEDIUM.getWidth(c.name, 7.5F) + Fonts.SF_MEDIUM.getWidth(c.durationStr, 7.0F) + 22.0F;
                maxW = Math.max(maxW, tw);
            }
            totalListH += (rowH * c.progress);
        }

        float targetW = maxW + padding * 2.0F;
        float targetH = headerH + totalListH + (totalListH > 0.5F ? padding : 0.0F);

        animatedWidth += (targetW - animatedWidth) * Math.min(1.0F, delta * 12.0F);
        animatedHeight += (targetH - animatedHeight) * Math.min(1.0F, delta * 12.0F);

        this.width = animatedWidth;
        this.height = animatedHeight;

        Render2D.drawShadow(x, y, width, height, 5.0F, 8.0F, SHADOW_COLOR);
        Render2D.drawBlur(x, y, width, height, 5.0F, BG_COLOR, 1.0F);

        drawHeader();

        var extractor = event.getGuiGraphicsExtractor();
        float curY = y + headerH + 2.0F;

        for (AnimatedPotion card : animatedCards) {
            float currentRowH = rowH * card.progress;
            if (card.progress <= 0.02F) continue;

            float alphaMul = card.progress;
            float slideX = (1.0F - card.progress) * -5.0F;

            float curIconSize = iconSize * card.progress;
            float iconX = x + padding + 1.0F + slideX;
            float iconY = curY + (currentRowH - curIconSize) / 2.0F;

            if (extractor != null && curIconSize > 2.0F) {
                Render2DUtil.flush();
                extractor.pose().pushMatrix();
                extractor.pose().translate(iconX, iconY);
                float scale = curIconSize / 18.0F;
                extractor.pose().scale(scale, scale);
                extractor.blit(card.texture, 0, 0, 18, 18, 0.0F, 1.0F, 0.0F, 1.0F);
                extractor.pose().popMatrix();
            }

            int nameColor = card.harmful ? COLOR_RED : ColorUtil.rgba(235, 235, 240, 255);
            Fonts.drawString(Fonts.SF_MEDIUM, card.name, iconX + curIconSize + 4.0F, curY + (currentRowH - 7.5F) / 2.0F, 7.5F, ColorUtil.multiplyAlpha(nameColor, alphaMul));

            float durW = Fonts.SF_MEDIUM.getWidth(card.durationStr, 7.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, card.durationStr, x + width - padding - durW - 2.0F, curY + (currentRowH - 7.0F) / 2.0F, 7.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(150, 155, 170, 255), alphaMul));

            curY += currentRowH;
        }
    }

    private void drawHeader() {
        float headerY = y + 4.5F;

        Fonts.drawIcon(IconUse.POTION, x + 5.0F, headerY + 0.5F, 7.5F, ColorUtil.withAlpha(Theme.getAccentColor(),240));
        Fonts.drawString(Fonts.SF_MEDIUM, "Potions", x + 15.0F, headerY, 8.0F, ColorUtil.rgba(240, 240, 245, 255));

        Render2D.drawRoundedRect(x + 4.0F, y + 15.5F, width - 8.0F, 0.5F, 0.25F, HEADER_DIVIDER);
    }

    private int getBarColor(AnimatedPotion card) {
        if (card.harmful) return COLOR_RED;
        if (card.fraction > 0.50F) return COLOR_GREEN;
        if (card.fraction > 0.25F) return COLOR_YELLOW;
        return COLOR_RED;
    }

    @Override
    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        float menuW = 125.0F;
        float menuH = 26.0F;

        Render2D.drawShadow(menuX, menuY, menuW, menuH, 5.0F, 8.0F, ColorUtil.multiplyAlpha(SHADOW_COLOR, alpha));
        Render2D.drawBlur(menuX, menuY, menuW, menuH, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(14, 14, 18, 240), alpha), 1.0F);

        Fonts.drawString(Fonts.SF_MEDIUM, "Style", menuX + 6.0F, menuY + 6.5F, 8.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));

        drawChip(menuX + 42.0F, menuY + 5.0F, 36.0F, 11.5F, "Cards", mode == Mode.CARDS, alpha);
        drawChip(menuX + 82.0F, menuY + 5.0F, 36.0F, 11.5F, "List", mode == Mode.LIST, alpha);

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

        float menuW = 125.0F;
        float menuH = 26.0F;

        if (mouseY >= menuY + 4.0F && mouseY <= menuY + 18.0F) {
            if (mouseX >= menuX + 42.0F && mouseX <= menuX + 78.0F) {
                this.mode = Mode.CARDS;
                return true;
            }
            if (mouseX >= menuX + 82.0F && mouseX <= menuX + 118.0F) {
                this.mode = Mode.LIST;
                return true;
            }
        }

        return mouseX >= menuX && mouseX <= menuX + menuW && mouseY >= menuY && mouseY <= menuY + menuH;
    }
}