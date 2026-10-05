package error.util.rotation;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Energy Snap Rotation algorithm port (Util140).
 * Instant 1-tick snap on attack, full free view during cooldown.
 */
public final class EnergySnapRotation implements AuraRotation {

    @Override
    public String getName() {
        return "Snap (En)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        if (attackLikely) {
            Vec3 eyePos = player.getEyePosition();
            Vec3 aimPos = target.getBoundingBox().getCenter();
            Vec3 delta = aimPos.subtract(eyePos);

            float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
            float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

            RotationHandler.setRotation(rawYaw, Mth.clamp(rawPitch, -89.5F, 89.5F));
        } else {
            // Keep current rotation or return to free look
            if (RotationHandler.isActive()) {
                RotationHandler.disengage("Smooth");
            }
        }
    }
}
