package error.module.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.event.list.Render2DEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.Render3DUtil;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;

/**
 * Create by daun kvass
 */
public final class Arrows extends Module {

    private static final Identifier POINTER_TEXTURE = Identifier.fromNamespaceAndPath("error", "images/ui/pointer.png");
    private static final int FRIEND_COLOR = ColorUtil.rgba(85, 255, 85, 255);

    public final SliderSetting radius = slider("Distance from Crosshair", 70.0F, 25.0F, 250.0F, 1.0F);
    public final SliderSetting size = slider("Scale", 16.0F, 8.0F, 32.0F, 1.0F);
    public final CheckBox distance = checkbox("Show Distance", true);
    public final CheckBox highlightTarget = checkbox("Highlight target", true);
    public final CheckBox ignoreNaked = checkbox("Ignore Naked", false);

    private int targetId = -1;
    private long firstAttackTime = 0L;
    private long lastAttackTime = 0L;

    public Arrows() {
        super("Arrows", "Стрелочки смешные типо ни хаха ни хохо", Category.RENDER);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof Player targetPlayer && targetPlayer != mc.player) {
            long now = System.currentTimeMillis();

            if (this.targetId != targetPlayer.getId() || (now - this.lastAttackTime) > 5000L) {
                this.firstAttackTime = now;
            }

            this.targetId = targetPlayer.getId();
            this.lastAttackTime = now;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        Minecraft mc = event.getClient();
        if (mc == null || mc.level == null || mc.player == null) {
            return;
        }

        var extractor = event.getGuiGraphicsExtractor();
        if (extractor == null) {
            return;
        }

        float tickDelta = event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0F;
        float centerY = mc.getWindow().getGuiScaledHeight() / 2.0F;

        Vec3 playerPos = Render3DUtil.interpolatedPosition(mc.player, tickDelta);
        float playerYaw = mc.player.getViewYRot(tickDelta);

        float currentRadius = this.radius.getValue();
        float baseSize = this.size.getValue();
        int baseColor = Theme.getAccentColor();

        MsdfFont font = Fonts.SF_MEDIUM;
        Matrix3x2fStack pose = extractor.pose();
        long currentTime = System.currentTimeMillis();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player target)) {
                continue;
            }
            if (target == mc.player) {
                continue;
            }
            if (target.isRemoved() || !target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (this.ignoreNaked.getValue() && isNaked(target)) {
                continue;
            }

            boolean isFriend = FriendManager.getInstance().isFriend(target);

            float targetFactor = 0.0F;
            if (!isFriend && this.highlightTarget.getValue() && target.getId() == this.targetId) {
                long elapsedSinceLast = currentTime - this.lastAttackTime;

                if (elapsedSinceLast <= 5000L) {
                    long elapsedSinceFirst = currentTime - this.firstAttackTime;

                    if (elapsedSinceFirst < 200L) {
                        targetFactor = elapsedSinceFirst / 200.0F;
                    } else if (elapsedSinceLast > 4000L) {
                        targetFactor = 1.0F - ((elapsedSinceLast - 4000.0F) / 1000.0F);
                    } else {
                        targetFactor = 1.0F;
                    }

                    targetFactor = Math.max(0.0F, Math.min(1.0F, targetFactor));
                }
            }

            float currentSize = baseSize + (baseSize * 0.35f * targetFactor);
            float halfSize = currentSize / 2;

            int arrowColor = isFriend
                    ? FRIEND_COLOR
                    : ColorUtil.interpolateColor(baseColor, ColorUtil.rgba(255, 55, 55, 255), targetFactor);

            float textSize = Math.max(7.0F, currentSize * 0.15f);
            Vec3 targetPos = Render3DUtil.interpolatedPosition(target, tickDelta);
            double dx = targetPos.x - playerPos.x;
            double dz = targetPos.z - playerPos.z;

            double yawToEntity = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
            double relativeYawRad = Math.toRadians(yawToEntity - playerYaw);

            float sin = (float) Math.sin(relativeYawRad);
            float cos = (float) Math.cos(relativeYawRad);
            float arrowX = centerX + (sin * currentRadius);
            float arrowY = centerY - (cos * currentRadius);

            pose.pushMatrix();
            pose.translate(arrowX, arrowY);
            pose.rotate((float) relativeYawRad);

            Render2D.drawTexture(POINTER_TEXTURE, -halfSize, -halfSize, currentSize, currentSize, 0.0F, arrowColor);

            pose.popMatrix();

            if (this.distance.getValue()) {
                double dist = playerPos.distanceTo(targetPos);
                String distText = (int) dist + "m";

                float distOffset = halfSize + 6.0F;
                float textCenterX = centerX + (sin * (currentRadius + distOffset));
                float textCenterY = centerY - (cos * (currentRadius + distOffset));

                float textWidth = font.getWidth(distText, textSize);
                float textX = textCenterX - textWidth / 2.0F;
                float textY = font.centeredTextY(textCenterY, textSize);

                Fonts.drawString(font, distText, textX, textY, textSize, ColorUtil.rgba(255, 255, 255, 240));
            }
        }
    }

    private static boolean isNaked(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}