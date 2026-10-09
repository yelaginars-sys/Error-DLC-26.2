package error.util.rotation;

import error.module.impl.combat.AuraModule;
import error.util.AuraRotation;
import error.util.RayTraceUtils;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Error ReallyWorld Rotation algorithm port (Util127).
 * Smooth adaptive leading dovodka, custom through-walls handling.
 */
public final class ErrorReallyWorldRotation implements AuraRotation {

    @Override
    public String getName() {
        return "ReallyWorld (Error)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        AuraModule aura = AuraModule.INSTANCE;
        float dovodka = aura != null ? aura.enDovodka.getValue() : 0.35F;

        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = target.getBoundingBox().getCenter();

        // Lead prediction based on target motion and dovodka
        Vec3 velocity = target.getDeltaMovement();
        Vec3 aimPoint = targetCenter.add(velocity.x * dovodka * 2.2, velocity.y * dovodka * 1.5, velocity.z * dovodka * 2.2);

        Vec3 delta = aimPoint.subtract(eyePos);
        float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        // ReallyWorld adaptive speed curve
        boolean isLooking = RayTraceUtils.isLookingAt(target, aura != null ? aura.attackRange.getValue() : 3.0F, currentYaw, currentPitch);
        float turnSpeed = isLooking ? (25.0F + dovodka * 45.0F) : (75.0F + dovodka * 65.0F);

        float stepYaw = Mth.clamp(deltaYaw * (0.45F + dovodka * 0.45F), -turnSpeed, turnSpeed);
        float stepPitch = Mth.clamp(deltaPitch * (0.45F + dovodka * 0.45F), -turnSpeed, turnSpeed);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
