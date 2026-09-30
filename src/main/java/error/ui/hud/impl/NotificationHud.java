package error.ui.hud.impl;

import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import error.Client;
import error.IMinecraft;
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.IconUse;
import error.util.render.Render2DUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Create by daun kvass
 */
public final class NotificationHud extends HudElement implements IMinecraft {

    public static class Notification {
        final String title;
        final String description;
        final IconUse icon;
        final int iconColor;
        final long durationMs;
        final long startTime;
        final Animation anim = new Animation(0.0F, 0.20F);
        boolean dead = false;

        public Notification(String title, String description, IconUse icon, int iconColor, long durationMs) {
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.iconColor = iconColor;
            this.durationMs = durationMs;
            this.startTime = System.currentTimeMillis();
            this.anim.setValue(0.0F);
            this.anim.setTarget(1.0F);
        }

        public float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0.0F, Math.min(1.0F, 1.0F - ((float) elapsed / (float) durationMs)));
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime >= durationMs;
        }
    }

    private static final List<Notification> NOTIFICATIONS = new CopyOnWriteArrayList<>();

    private boolean notifyModules = true;
    private boolean notifyPotionsExpire = true;
    private boolean notifyTotemPop = true;
    private boolean notifyPlayerPots = true;
    private boolean notifyElytraSwap = true;

    private static final int COLOR_GREEN = ColorUtil.rgba(65, 225, 120, 255);
    private static final int COLOR_RED = ColorUtil.rgba(235, 75, 75, 255);
    private final int СOLOR_THEME = ColorUtil.withAlpha(Theme.getAccentColor(),240);

    private static class TotemData {
        int count;
        long lastPopTime;
        TotemData(int count, long time) { this.count = count; this.lastPopTime = time; }
    }
    private final Map<UUID, TotemData> totemPops = new ConcurrentHashMap<>();

    private final Set<Holder<MobEffect>> warned15Sec = new HashSet<>();
    private final Map<Holder<MobEffect>, Integer> lastBeneficialEffects = new HashMap<>();
    private final Map<String, Float> switchAnims = new HashMap<>();

    private static final float BASE_WIDTH = 110.0F;
    private static final float ITEM_HEIGHT = 13.5F;
    private static final float GAP = 3.5F;
    private static final float PAD_X = 5.0F;
    private static final float ICON_SIZE = 6.5F;
    private static final float ICON_GAP = 3.5F;
    private static final float TEXT_GAP = 3.0F;
    private static final float CIRCLE_GAP = 5.0F;
    private static final float CIRCLE_R = 3.0F;

    private static final int BG_COLOR = ColorUtil.rgba(14, 14, 18, 195);
    private static final int SHADOW_COLOR = ColorUtil.rgba(0, 0, 0, 140);

    public NotificationHud() {
        super("notifications", "Notifications", 10.0F, 220.0F, BASE_WIDTH, ITEM_HEIGHT);
        try {
            if (Client.getInstance() != null && Client.getInstance().getEventManager() != null) {
                Client.getInstance().getEventManager().register(this);
            }
        } catch (Exception ignored) {}
    }

    public JsonObject writeConfig() {
        JsonObject obj = new JsonObject();
        obj.addProperty("notifyModules", notifyModules);
        obj.addProperty("notifyPotionsExpire", notifyPotionsExpire);
        obj.addProperty("notifyTotemPop", notifyTotemPop);
        obj.addProperty("notifyPlayerPots", notifyPlayerPots);
        obj.addProperty("notifyElytraSwap", notifyElytraSwap);
        return obj;
    }

    public void readConfig(JsonObject obj) {
        if (obj == null) return;
        if (obj.has("notifyModules")) this.notifyModules = obj.get("notifyModules").getAsBoolean();
        if (obj.has("notifyPotionsExpire")) this.notifyPotionsExpire = obj.get("notifyPotionsExpire").getAsBoolean();
        if (obj.has("notifyTotemPop")) this.notifyTotemPop = obj.get("notifyTotemPop").getAsBoolean();
        if (obj.has("notifyPlayerPots")) this.notifyPlayerPots = obj.get("notifyPlayerPots").getAsBoolean();
        if (obj.has("notifyElytraSwap")) this.notifyElytraSwap = obj.get("notifyElytraSwap").getAsBoolean();
    }

    public static void post(String title, String description, IconUse icon, int iconColor, long durationMs) {
        NotificationHud inst = getInst();
        if (inst != null && !inst.isEnabled()) return;
        NOTIFICATIONS.add(new Notification(title, description, icon, iconColor, durationMs));
    }

    public static boolean isNotifyElytraSwapEnabled() {
        NotificationHud inst = getInst();
        return inst != null && inst.isEnabled() && inst.notifyElytraSwap;
    }

    public static void onModuleToggle(String moduleName, boolean enabled) {
        NotificationHud instance = getInst();
        if (instance == null || !instance.isEnabled() || !instance.notifyModules) return;

        IconUse icon = enabled ? IconUse.ONMODULE : IconUse.OFFMODULE;
        int iconCol = enabled ? COLOR_GREEN : COLOR_RED;
        String desc = enabled ? "включен" : "выключен";

        post(moduleName, desc, icon, iconCol, 2200L);
    }

    private static NotificationHud getInst() {
        for (HudElement el : HudManager.getInstance().getElements()) {
            if (el instanceof NotificationHud nh) return nh;
        }
        return null;
    }

    @Override
    public boolean shouldRender() {
        if (!enabled) return false;
        checkPotionExpirations();
        if (HudManager.getInstance().isDraggableScreenOpen()) return true;
        return !NOTIFICATIONS.isEmpty();
    }

    @Override
    public void draw(Render2DEvent event) {
        if (!enabled) return;

        if (NOTIFICATIONS.isEmpty() && HudManager.getInstance().isDraggableScreenOpen()) {
            NOTIFICATIONS.add(new Notification("Aura", "включен", IconUse.ONMODULE, COLOR_GREEN, 3000L));
            NOTIFICATIONS.add(new Notification("Скорость II", "скоро закончится", IconUse.POTION, СOLOR_THEME, 3000L));
        }

        int activeCount = 0;
        for (int i = NOTIFICATIONS.size() - 1; i >= 0; i--) {
            Notification n = NOTIFICATIONS.get(i);
            if (!n.dead && !n.isExpired()) {
                activeCount++;
                if (activeCount > 2) {
                    n.dead = true;
                    n.anim.setTarget(0.0F);
                }
            }
        }

        this.width = BASE_WIDTH;
        float anchorCenterX = this.x + (BASE_WIDTH / 2.0F);
        float curY = this.y;
        float totalH = 0.0F;

        int themeAccent = Theme.getAccentColor();
        var extractor = event.getGuiGraphicsExtractor();

        for (Notification n : NOTIFICATIONS) {
            if (n.isExpired() && !n.dead) {
                n.dead = true;
                n.anim.setTarget(0.0F);
            }

            n.anim.update();
            float progress = n.anim.getValue();

            if (n.dead && progress <= 0.015F) {
                NOTIFICATIONS.remove(n);
                continue;
            }

            float titleW = Fonts.SF_MEDIUM.getWidth(n.title, 7.0F);
            float descW = Fonts.SF_MEDIUM.getWidth(n.description, 6.5F);
            float hasIconW = (n.icon != null ? ICON_SIZE + ICON_GAP : 0.0F);
            float cardW = PAD_X + hasIconW + titleW + TEXT_GAP + descW + CIRCLE_GAP + (CIRCLE_R * 2.0F) + PAD_X;

            float cardX = anchorCenterX - (cardW / 2.0F);
            float cardH = ITEM_HEIGHT;
            float centerY = curY + cardH / 2.0F;

            int cardBg = ColorUtil.multiplyAlpha(BG_COLOR, progress);
            int cardShadow = ColorUtil.multiplyAlpha(SHADOW_COLOR, progress);

            if (extractor != null && progress < 0.99F) {
                Render2DUtil.flush();
                float scale = 0.88F + 0.12F * progress;
                extractor.pose().pushMatrix();
                extractor.pose().translate(anchorCenterX, centerY);
                extractor.pose().scale(scale, scale);
                extractor.pose().translate(-anchorCenterX, -centerY);
            }

            Render2D.drawShadow(cardX, curY, cardW, cardH, 4.0F, 5.0F, cardShadow);
            Render2D.drawBlur(cardX, curY, cardW, cardH, 4.0F, cardBg, progress);

            float curContentX = cardX + PAD_X;
            if (n.icon != null) {
                Fonts.drawIcon(n.icon, curContentX, centerY - ICON_SIZE / 2.0F - 0.2F, ICON_SIZE, ColorUtil.multiplyAlpha(n.iconColor, progress));
                curContentX += ICON_SIZE + ICON_GAP;
            }

            float textY = Fonts.SF_MEDIUM.centeredTextY(centerY, 7.0F);
            Fonts.drawString(Fonts.SF_MEDIUM, n.title, curContentX, textY, 7.0F, ColorUtil.rgba(240, 240, 245, (int) (255 * progress)));
            curContentX += titleW + TEXT_GAP;

            Fonts.drawString(Fonts.SF_MEDIUM, n.description, curContentX, textY + 0.1F, 6.5F, ColorUtil.rgba(160, 165, 180, (int) (255 * progress)));

            float circleCX = cardX + cardW - PAD_X - CIRCLE_R;
            float circleCY = centerY;
            float remainingFrac = n.getProgress();

            drawSmoothRing(circleCX, circleCY, CIRCLE_R, 0.9F, remainingFrac, ColorUtil.multiplyAlpha(themeAccent, progress), progress);

            if (extractor != null && progress < 0.99F) {
                extractor.pose().popMatrix();
            }

            curY += (cardH + GAP) * progress;
            totalH += (cardH + GAP) * progress;
        }

        this.height = Math.max(ITEM_HEIGHT, totalH);
    }

    private void drawSmoothRing(float cx, float cy, float radius, float thickness, float progress, int activeColor, float animAlpha) {
        int bgTrackColor = ColorUtil.rgba(255, 255, 255, (int) (22 * animAlpha));
        Render2D.drawCircleOutline(cx, cy, radius, thickness, bgTrackColor);
        float sweepAngle = 360.0F * Math.max(0.0F, Math.min(1.0F, progress));
        if (sweepAngle > 0.5F) {
            Render2D.drawArc(cx, cy, radius, thickness, -90.0F, sweepAngle, activeColor);
        }
    }

    private void checkPotionExpirations() {
        if (!notifyPotionsExpire || mc.player == null) return;

        Map<Holder<MobEffect>, Integer> currentBeneficial = new HashMap<>();
        for (MobEffectInstance inst : mc.player.getActiveEffects()) {
            Holder<MobEffect> holder = inst.getEffect();
            if (holder.value().getCategory() == MobEffectCategory.BENEFICIAL) {
                int duration = inst.getDuration();
                currentBeneficial.put(holder, duration);

                if (duration <= 300 && duration > 20 && !warned15Sec.contains(holder)) {
                    warned15Sec.add(holder);
                    String effName = holder.value().getDisplayName().getString();
                    if (inst.getAmplifier() > 0) effName += " " + (inst.getAmplifier() + 1);
                    post(effName, "скоро закончится", IconUse.POTION, СOLOR_THEME, 3000L);
                } else if (duration > 300) {
                    warned15Sec.remove(holder);
                }
            }
        }

        for (Map.Entry<Holder<MobEffect>, Integer> entry : lastBeneficialEffects.entrySet()) {
            if (!currentBeneficial.containsKey(entry.getKey())) {
                warned15Sec.remove(entry.getKey());
                String effName = entry.getKey().value().getDisplayName().getString();
                post(effName, "закончился", IconUse.POTION, COLOR_RED, 2500L);
            }
        }

        lastBeneficialEffects.clear();
        lastBeneficialEffects.putAll(currentBeneficial);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!enabled || mc.level == null) return;

        if (notifyTotemPop && event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEventId() == 35) {
                Entity entity = packet.getEntity(mc.level);
                if (entity instanceof Player player) {
                    long now = System.currentTimeMillis();
                    TotemData data = totemPops.getOrDefault(player.getUUID(), new TotemData(0, now));

                    if (now - data.lastPopTime > 120_000L) {
                        data.count = 0;
                    }

                    data.count++;
                    data.lastPopTime = now;
                    totemPops.put(player.getUUID(), data);

                    post(player.getGameProfile().name(), "потерял тотем x" + data.count, IconUse.CROSS, СOLOR_THEME, 3000L);
                }
            }
        }

        if (notifyPlayerPots && event.getPacket() instanceof ClientboundUpdateMobEffectPacket packet) {
            Entity entity = mc.level.getEntity(packet.getEntityId());
            if (entity instanceof Player player && player != mc.player) {
                Holder<MobEffect> holder = packet.getEffect();
                String effName = holder.value().getDisplayName().getString();
                int amp = packet.getEffectAmplifier() + 1;
                int sec = packet.getEffectDurationTicks() / 20;
                String timeStr = String.format("%d:%02d", sec / 60, sec % 60);

                post(player.getGameProfile().name(), effName + " " + amp + " (" + timeStr + ")", IconUse.POTION, Theme.getAccentColor(), 3000L);
            }
        }
    }

    @Override
    public float drawContextMenu(float menuX, float menuY, double mouseX, double mouseY, float alpha) {
        float menuW = 140.0F;
        float menuH = 82.0F;

        Render2D.drawShadow(menuX, menuY, menuW, menuH, 5.0F, 8.0F, ColorUtil.multiplyAlpha(SHADOW_COLOR, alpha));
        Render2D.drawBlur(menuX, menuY, menuW, menuH, 5.0F, ColorUtil.multiplyAlpha(ColorUtil.rgba(14, 14, 18, 240), alpha), 1.0F);

        float curY = menuY + 6.0F;
        drawModernSwitch(menuX + 7.0F, curY, "Modules", "mod", notifyModules, alpha);
        curY += 15.0F;
        drawModernSwitch(menuX + 7.0F, curY, "Expire Pots", "exp", notifyPotionsExpire, alpha);
        curY += 15.0F;
        drawModernSwitch(menuX + 7.0F, curY, "Totem Pop", "tot", notifyTotemPop, alpha);
        curY += 15.0F;
        drawModernSwitch(menuX + 7.0F, curY, "Player Pots", "ply", notifyPlayerPots, alpha);
        curY += 15.0F;
        drawModernSwitch(menuX + 7.0F, curY, "Elytra Swap", "ely", notifyElytraSwap, alpha);

        return menuH;
    }

    private void drawModernSwitch(float cx, float cy, String label, String key, boolean active, float alpha) {
        Fonts.drawString(Fonts.SF_MEDIUM, label, cx, cy + 1.5F, 7.5F, ColorUtil.multiplyAlpha(Theme.TEXT_MUTED, alpha));

        float trackW = 19.0F;
        float trackH = 10.0F;
        float trackX = cx + 104.0F;
        float trackY = cy;

        float currentAnim = switchAnims.getOrDefault(key, active ? 1.0F : 0.0F);
        float targetAnim = active ? 1.0F : 0.0F;
        currentAnim += (targetAnim - currentAnim) * 0.22F;
        switchAnims.put(key, currentAnim);

        int inactiveColor = ColorUtil.rgba(40, 42, 54, 255);
        int trackColor = ColorUtil.lerp(inactiveColor, Theme.getAccentColor(), currentAnim);
        trackColor = ColorUtil.multiplyAlpha(trackColor, alpha);

        Render2D.drawRoundedRect(trackX, trackY, trackW, trackH, trackH / 2.0F, trackColor);

        float thumbSize = 7.6F;
        float thumbPad = (trackH - thumbSize) / 2.0F;
        float thumbX = trackX + thumbPad + (trackW - thumbSize - thumbPad * 2.0F) * currentAnim;
        float thumbY = trackY + thumbPad;

        int thumbColor = ColorUtil.multiplyAlpha(0xFFFFFFFF, alpha);
        Render2D.drawRoundedRect(thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2.0F, thumbColor);
    }

    @Override
    public boolean handleContextMenuClick(float menuX, float menuY, double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        float switchX = menuX + 7.0F + 104.0F;
        float curY = menuY + 6.0F;

        if (mouseX >= switchX - 10.0F && mouseX <= switchX + 25.0F) {
            if (mouseY >= curY - 2.0F && mouseY <= curY + 12.0F) { notifyModules = !notifyModules; return true; }
            curY += 15.0F;
            if (mouseY >= curY - 2.0F && mouseY <= curY + 12.0F) { notifyPotionsExpire = !notifyPotionsExpire; return true; }
            curY += 15.0F;
            if (mouseY >= curY - 2.0F && mouseY <= curY + 12.0F) { notifyTotemPop = !notifyTotemPop; return true; }
            curY += 15.0F;
            if (mouseY >= curY - 2.0F && mouseY <= curY + 12.0F) { notifyPlayerPots = !notifyPlayerPots; return true; }
            curY += 15.0F;
            if (mouseY >= curY - 2.0F && mouseY <= curY + 12.0F) { notifyElytraSwap = !notifyElytraSwap; return true; }
        }

        return mouseX >= menuX && mouseX <= menuX + 140.0F && mouseY >= menuY && mouseY <= menuY + 82.0F;
    }
}