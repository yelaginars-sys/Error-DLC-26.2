package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class LegitExsRotation implements AuraRotation {

    private float smoothedSpeed = 0.0f;

    @Override
    public String getName() {
        return "Legit (exs)";
    }

    @Override
    public void reset() {
        smoothedSpeed = 0.0f;
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) {
            reset();
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetEyePos.subtract(eyePos);

        float targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))), -89.5F, 89.5F);

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float absDelta = Math.abs(yawDelta);

        float randomFactor = (float) ThreadLocalRandom.current().nextDouble(0.07, 0.12);
        float randomMaxSpeed = (float) ThreadLocalRandom.current().nextDouble(0.9, 1.5);
        float targetSpeed = Mth.clamp(absDelta * randomFactor, 0.01f, randomMaxSpeed);

        smoothedSpeed = Mth.lerp(0.12f, smoothedSpeed, targetSpeed);
        if (smoothedSpeed < 0.0005f) {
            smoothedSpeed = 0.0f;
        }

        float stepYaw = Mth.clamp(yawDelta * 0.85f, -smoothedSpeed * 60.0f, smoothedSpeed * 60.0f);
        float finalYaw = currentYaw + stepYaw;

        RotationHandler.setRotation(finalYaw, targetPitch);
    }
}
