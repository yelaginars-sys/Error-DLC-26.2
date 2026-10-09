package error.util.rotation;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Error Ares/FT Rotation algorithm port (Util151).
 * Distance-adaptive timing with fast attack snapping and organic decay.
 */
public final class ErrorAresRotation implements AuraRotation {

    @Override
    public String getName() {
        return "Ares/FT (Error)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 aimPos = target.getBoundingBox().getCenter().add(0.0, 0.15, 0.0);
        Vec3 delta = aimPos.subtract(eyePos);

        float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        double dist = player.distanceTo(target);
        float adaptSpeed = (float) Math.max(40.0, 120.0 - dist * 12.0);

        if (attackLikely) {
            adaptSpeed *= 1.4F;
        }

        float stepYaw = Mth.clamp(deltaYaw * 0.88F, -adaptSpeed, adaptSpeed);
        float stepPitch = Mth.clamp(deltaPitch * 0.88F, -adaptSpeed, adaptSpeed);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
