package error.ui.hud.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render3DUtil;
import error.util.render.font.Fonts;
import error.util.display.batch.DisplayBatcher;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;

public class GpsHud extends HudElement {
    private static final Identifier POINTER_TEX = Identifier.fromNamespaceAndPath("error", "images/ui/pointer.png");

    public static boolean active = false;
    public static String targetName = null;
    public static double targetX = 0;
    public static double targetY = Double.NaN;
    public static double targetZ = 0;

    public GpsHud() {
        super("gps", "GPS", 0, 0, 88.0F, 18.0F, true);
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            this.x = (mc.getWindow().getGuiScaledWidth() - 88.0F) / 2.0F;
            this.y = 28.0F;
        }
    }

    public static void setTarget(String name, double x, double y, double z) {
        targetName = name;
        targetX = x;
        targetY = y;
        targetZ = z;
        active = true;
    }

    public static void clearGps() {
        active = false;
        targetName = null;
        targetY = Double.NaN;
    }

    @Override
    public boolean shouldRender() {
        return enabled && active;
    }

    @Override
    public void draw(Render2DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;

        boolean inChat = mc.gui.screen() instanceof ChatScreen;
        boolean isEditing = inChat || dragging;

        // Visible ONLY when GPS target is active OR when in ChatScreen for HUD positioning
        fadeAnim.setTarget((active || isEditing) && enabled ? 1.0F : 0.0F);
        fadeAnim.update();

        float alpha = fadeAnim.getValue();
        if (alpha <= 0.01F) return;

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor == null) return;

        int primaryAccent = error.module.impl.render.Interface.INSTANCE != null ? error.module.impl.render.Interface.INSTANCE.getHudColor() : Theme.getAccentColor();
        int secondaryAccent = Theme.getSecondaryColor();

        int bgFill = ColorUtil.rgba(16, 18, 28, (int) (210 * alpha));
        int shadowCol = ColorUtil.rgba(0, 0, 0, (int) (140 * alpha));
        int accentGlow = ColorUtil.withAlpha(primaryAccent, (int) (55 * alpha));

        float w = 92.0F;
        float h = 19.0F;
        this.width = w;
        this.height = h;

        Blur.of(x, y, w, h)
                .radius(6)
                .type(BlurType.KAWASE)
                .strength(4)
                .tint(Color.rgba(0, 0, 0, Math.round(75 * alpha)))
                .alpha(alpha)
                .render(extractor);

        Outline.of(x, y, w, h)
                .radius(6)
                .thickness(1.0F)
                .verticalGradient(Color.WHITE, Color.rgba(255, 255, 255, 32))
                .alpha(alpha)
                .render(extractor);

        DisplayBatcher.flush();

        int glassFill = ColorUtil.rgba(14, 18, 28, (int) (140 * alpha));
        int glassBorder = ColorUtil.withAlpha(primaryAccent, (int) (110 * alpha));

        Render2D.drawRoundedRect(x, y, w, h, 6.0F, glassFill);
        Render2D.drawRoundedOutline(x, y, w, h, 6.0F, 0.7F, glassBorder);

        float arrowSize = 9.0F;
        float arrowCenterX = x + 8.0F;
        float arrowCenterY = y + h / 2.0F;

        if (active) {
            float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Vec3 playerPos = Render3DUtil.interpolatedPosition(mc.player, tickDelta);
            float playerYaw = mc.player.getViewYRot(tickDelta);

            double dx = targetX - playerPos.x;
            double dz = targetZ - playerPos.z;

            double yawToPoint = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            float relativeYawRad = (float) Math.toRadians(yawToPoint - playerYaw);

            double dist2D = Math.hypot(dx, dz);
            String distStr = (int) Math.round(dist2D) + "m";

            Matrix3x2fStack pose = extractor.pose();
            pose.pushMatrix();
            pose.translate(arrowCenterX, arrowCenterY);
            pose.rotate(relativeYawRad);

            Render2D.drawTexture(POINTER_TEX, -arrowSize / 2.0F, -arrowSize / 2.0F, arrowSize, arrowSize, 0.0F, ColorUtil.multiplyAlpha(primaryAccent, alpha));
            pose.popMatrix();

            String displayTitle = distStr + ((targetName != null && !targetName.isEmpty()) ? " • " + targetName : "");
            Fonts.drawString(Fonts.SF_MEDIUM, displayTitle, x + 16.0F, y + 2.5F, 6.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

            String coordsStr;
            if (!Double.isNaN(targetY)) {
                coordsStr = (int) targetX + ", " + (int) targetY + ", " + (int) targetZ;
            } else {
                coordsStr = (int) targetX + ", " + (int) targetZ;
            }
            Fonts.drawString(Fonts.SF_MEDIUM, coordsStr, x + 16.0F, y + 9.5F, 5.5F, ColorUtil.rgba(180, 180, 195, (int) (200 * alpha)));
        } else {
            Render2D.drawTexture(POINTER_TEX, arrowCenterX - arrowSize / 2.0F, arrowCenterY - arrowSize / 2.0F, arrowSize, arrowSize, 0.0F, ColorUtil.multiplyAlpha(primaryAccent, alpha));

            String displayTitle = "150m • GPS";
            Fonts.drawString(Fonts.SF_MEDIUM, displayTitle, x + 16.0F, y + 2.5F, 6.5F, ColorUtil.rgba(255, 255, 255, (int) (245 * alpha)));

            String coordsStr = "100, 64, 200";
            Fonts.drawString(Fonts.SF_MEDIUM, coordsStr, x + 16.0F, y + 9.5F, 5.5F, ColorUtil.rgba(180, 180, 195, (int) (180 * alpha)));
        }
    }
}
