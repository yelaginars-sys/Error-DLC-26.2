package error.module.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import error.event.EventTarget;
import error.event.list.Render2DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.render.Render2D;
import error.util.render.Render3DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import error.util.render.Render2DUtil;

import java.util.*;

import static error.util.client.clients.Theme.BG_COLOR;
import static error.util.client.clients.Theme.DIVIDER_COLOR;

/**
 */
public final class FireworkESP extends Module {

    private static final float SCALE = 1.0F;
    private static final float PILL_HEIGHT = 24.0F;
    private static final float RADIUS = 6.0F;
    private static final float PADDING = 6.0F;
    private static final float GAP = 5.0F;
    private static final float ITEM_SIZE = 13.0F;
    private static final float DIVIDER_HEIGHT = 10.0F;
    private static final float TEXT_SIZE = 10.0F;

    public final SliderSetting scale = slider("Scale", 1.0f, 0.5f, 1.5f, 0.05f);

    private record FireworkSpot(Vec3 position, long launchTime) {}

    private final Set<Integer> trackedEntityIds = new HashSet<>();
    private final List<FireworkSpot> spots = Collections.synchronizedList(new ArrayList<>());
    private ItemStack fireworkStack = null;

    public FireworkESP() {
        super("FireworkESP", "Показывает место юзание феера", Category.RENDER);
    }

    private ItemStack getFireworkStack() {
        if (fireworkStack == null) {
            fireworkStack = Items.FIREWORK_ROCKET.getDefaultInstance();
        }
        return fireworkStack;
    }

    @Override
    protected void onDisable() {
        trackedEntityIds.clear();
        spots.clear();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.level == null || mc.player == null) {
            spots.clear();
            trackedEntityIds.clear();
            return;
        }

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float guiScale = (float) mc.getWindow().getGuiScale();
        float unit = (SCALE * this.scale.getValue()) / (guiScale > 0 ? guiScale : 1.0F);
        double maxDistSq = 250.0 * 250.0;
        long now = System.currentTimeMillis();
        long maxLifetimeMs = (long) (10 * 1000.0F);

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof FireworkRocketEntity firework) {
                if (!firework.isRemoved() && trackedEntityIds.add(firework.getId())) {
                    Vec3 launchPos = Render3DUtil.interpolatedPosition(firework, tickDelta);
                    spots.add(new FireworkSpot(launchPos, now));
                }
            }
        }

        synchronized (spots) {
            Iterator<FireworkSpot> iterator = spots.iterator();
            while (iterator.hasNext()) {
                FireworkSpot spot = iterator.next();
                long elapsed = now - spot.launchTime();

                if (elapsed >= maxLifetimeMs) {
                    iterator.remove();
                    continue;
                }

                if (mc.player.distanceToSqr(spot.position()) > maxDistSq) {
                    continue;
                }

                float alpha = 1.0F;
                long fadeTime = 3000L;
                if (elapsed > maxLifetimeMs - fadeTime) {
                    alpha = Math.max(0.0F, (float) (maxLifetimeMs - elapsed) / (float) fadeTime);
                }

                int totalSec = (int) (elapsed / 1000L);
                String timerStr = String.format("%d:%02d", totalSec / 60, totalSec % 60);

                drawFireworkTag(event, spot.position(), timerStr, alpha, unit);
            }
        }
    }

    private void drawFireworkTag(Render2DEvent event, Vec3 worldPos, String timerStr, float alpha, float unit) {
        Minecraft mc = event.getClient();
        Render3DUtil.ScreenPoint anchor = Render3DUtil.projectToScreen(mc, worldPos.add(0.0D, 0.4D, 0.0D));
        if (anchor == null) return;

        MsdfFont font = Fonts.SF_MEDIUM;
        float textSize = TEXT_SIZE * unit;
        float itemSize = ITEM_SIZE * unit;
        float gap = GAP * unit;
        float dividerWidth = Math.max(1.0F, 1.0F * unit);

        String title = "Фейерверк";
        String timerFormatted = "[" + timerStr + "]";

        float titleWidth = font.getWidth(title, textSize);
        float timerWidth = font.getWidth(timerFormatted, textSize);

        float width = PADDING * unit + itemSize + gap + titleWidth + gap + dividerWidth + gap + timerWidth + PADDING * unit;
        float pillHeight = PILL_HEIGHT * unit;

        float pillX = anchor.x() - width / 2.0F;
        float pillY = anchor.y() - pillHeight;
        float centerY = pillY + pillHeight / 2.0F;
        float textY = font.centeredTextY(centerY, textSize);

        int bg = ColorUtil.multiplyAlpha(BG_COLOR, alpha);
        Render2D.drawShadow(pillX, pillY, width, pillHeight, 4.0F * unit, 6.0F * unit, ColorUtil.multiplyAlpha(0x90000000, alpha));
        Render2D.drawBlur(pillX, pillY, width, pillHeight, RADIUS * unit, 1, bg, alpha);

        float cursor = pillX + PADDING * unit;

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor != null) {
            Render2DUtil.flush();
            Matrix3x2fStack pose = extractor.pose();
            float itemScale = itemSize / 16.0F;
            pose.pushMatrix();
            pose.translate(cursor, centerY - itemSize / 2.0F);
            pose.scale(itemScale, itemScale);
            extractor.item(getFireworkStack(), 0, 0);
            pose.popMatrix();
        }
        cursor += itemSize + gap;

        Fonts.drawString(font, title, cursor, textY, textSize, ColorUtil.rgba(240, 240, 245, (int) (255 * alpha)));
        cursor += titleWidth;

        cursor = drawDivider(cursor, centerY, dividerWidth, gap, unit, alpha);

        int timerColor = ColorUtil.rgba(255, 175, 65, (int) (255 * alpha));
        Fonts.drawString(font, timerFormatted, cursor, textY, textSize, timerColor);
    }

    private float drawDivider(float cursor, float centerY, float dividerWidth, float gap, float unit, float alpha) {
        cursor += gap;
        float h = DIVIDER_HEIGHT * unit;
        Render2D.drawRoundedRect(cursor, centerY - h / 2.0F, dividerWidth, h, 0.5F * unit, ColorUtil.multiplyAlpha(DIVIDER_COLOR, alpha));
        return cursor + dividerWidth + gap;
    }
}