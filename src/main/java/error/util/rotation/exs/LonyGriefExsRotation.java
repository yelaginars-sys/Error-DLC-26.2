package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class LonyGriefExsRotation implements AuraRotation {

    private int ticksSinceLastJerk = 0;
    private int nextJerkTick = 8;
    private float yawSpeedMultiplier = 0.0f;
    private float pitchSpeedMultiplier = 0.0f;

    @Override
    public String getName() {
        return "LonyGrief (exs)";
    }

    @Override
    public void reset() {
        ticksSinceLastJerk = 0;
        nextJerkTick = 8;
        yawSpeedMultiplier = 0.0f;
        pitchSpeedMultiplier = 0.0f;
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
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        targetPitch = Mth.clamp(targetPitch, -89.0f, 89.0f);

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDelta = targetPitch - currentPitch;

        float accelSpeed = (Math.abs(yawDelta) > 15.0f) ? (float) ThreadLocalRandom.current().nextDouble(0.18, 0.50) : 0.12f;

        yawSpeedMultiplier = Mth.lerp(accelSpeed, yawSpeedMultiplier, 1.0f);
        pitchSpeedMultiplier = Mth.lerp(accelSpeed, pitchSpeedMultiplier, 1.0f);

        float baseYawSpeed = (float) ThreadLocalRandom.current().nextDouble(58.0, 72.0);
        float basePitchSpeed = (float) ThreadLocalRandom.current().nextDouble(22.0, 32.0);

        float distanceFactor = Mth.clamp(Math.abs(yawDelta) / 15.0f, 0.35f, 1.0f);

        float yawSpeed = baseYawSpeed * yawSpeedMultiplier * distanceFactor;
        float pitchSpeed = basePitchSpeed * pitchSpeedMultiplier;

        ticksSinceLastJerk++;
        if (ticksSinceLastJerk >= nextJerkTick) {
            float jerkYaw = (float) ThreadLocalRandom.current().nextDouble(0.4, 0.8);
            float jerkPitch = (float) ThreadLocalRandom.current().nextDouble(0.5, 0.9);
            yawDelta *= jerkYaw;
            pitchDelta *= jerkPitch;
            ticksSinceLastJerk = 0;
            nextJerkTick = ThreadLocalRandom.current().nextInt(4, 11);
        }

        float clampedYawDelta = Mth.clamp(yawDelta, -yawSpeed, yawSpeed);
        float clampedPitchDelta = Mth.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        float finalYaw = currentYaw + clampedYawDelta;
        float finalPitch = Mth.clamp(currentPitch + clampedPitchDelta, -89.0f, 89.0f);

        RotationHandler.setRotation(finalYaw, finalPitch);
    }
}
