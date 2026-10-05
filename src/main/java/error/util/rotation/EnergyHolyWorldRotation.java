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
 * Energy HolyWorld Rotation algorithm port (Util57).
 * Step-wise pitch/yaw alignment designed specifically for HolyWorld AC.
 */
public final class EnergyHolyWorldRotation implements AuraRotation {

    @Override
    public String getName() {
        return "HolyWorld (En)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        AuraModule aura = AuraModule.INSTANCE;
        float dovodka = aura != null ? aura.enDovodka.getValue() : 0.35F;

        Vec3 eyePos = player.getEyePosition();
        Vec3 aimPos = target.getBoundingBox().getCenter().add(0.0, 0.15, 0.0);
        Vec3 delta = aimPos.subtract(eyePos);

        float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        float stepSpeed = 0.07F * (1.0F + dovodka * 1.5F);
        if (Math.abs(deltaYaw) > 50.0F || player.getBoundingBox().intersects(target.getBoundingBox())) {
            stepSpeed /= 1.05F;
        }

        boolean isAimed = RayTraceUtils.isLookingAt(target, aura != null ? aura.attackRange.getValue() : 3.0F, currentYaw, currentPitch);
        if (isAimed) {
            stepSpeed /= 1.25F;
        }

        float maxYawStep = Math.max(12.0F, Math.abs(deltaYaw) * stepSpeed * 10.0F);
        float maxPitchStep = Math.max(8.0F, Math.abs(deltaPitch) * stepSpeed * 8.0F);

        float stepYaw = Mth.clamp(deltaYaw, -maxYawStep, maxYawStep);
        float stepPitch = Mth.clamp(deltaPitch, -maxPitchStep, maxPitchStep);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
