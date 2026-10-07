package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class FunTimeExsRotation implements AuraRotation {

    private float smoothedYaw = 0;
    private float smoothedPitch = 0;
    private boolean initialized = false;

    private float microNoiseYaw = 0;
    private float microNoisePitch = 0;
    private long lastNoiseUpdate = 0;

    private float recoilYaw = 0;
    private float recoilPitch = 0;

    @Override
    public String getName() {
        return "FunTime (exs)";
    }

    @Override
    public void onAttack() {
        recoilYaw = (float) ThreadLocalRandom.current().nextDouble(-0.35, 0.35);
        recoilPitch = (float) ThreadLocalRandom.current().nextDouble(-0.25, 0.15);
    }

    @Override
    public void reset() {
        initialized = false;
        recoilYaw = 0;
        recoilPitch = 0;
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) {
            initialized = false;
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetEyePos.subtract(eyePos);
        double distHoriz = Math.max(Math.hypot(diff.x, diff.z), 0.01);

        float targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(diff.y, distHoriz)), -89.5F, 89.5F);

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        if (!initialized) {
            smoothedYaw = currentYaw;
            smoothedPitch = currentPitch;
            initialized = true;
        }

        recoilYaw *= 0.65f;
        recoilPitch *= 0.65f;

        long now = System.currentTimeMillis();
        if (now - lastNoiseUpdate > 45) {
            microNoiseYaw = (float) (Math.sin(now / 120.0) * 0.35 + Math.cos(now / 55.0) * 0.15);
            microNoisePitch = (float) (Math.cos(now / 140.0) * 0.25 + Math.sin(now / 70.0) * 0.10);
            lastNoiseUpdate = now;
        }

        float yawDelta = Mth.wrapDegrees(targetYaw - smoothedYaw);
        float pitchDelta = targetPitch - smoothedPitch;
        float totalDelta = (float) Math.hypot(yawDelta, pitchDelta);

        float lerpFactor;
        if (totalDelta > 45.0f) {
            lerpFactor = (float) ThreadLocalRandom.current().nextDouble(0.60, 0.75);
        } else if (totalDelta > 15.0f) {
            lerpFactor = (float) ThreadLocalRandom.current().nextDouble(0.45, 0.60);
        } else if (totalDelta > 5.0f) {
            lerpFactor = (float) ThreadLocalRandom.current().nextDouble(0.35, 0.50);
        } else {
            lerpFactor = (float) ThreadLocalRandom.current().nextDouble(0.25, 0.38);
        }

        smoothedYaw += yawDelta * lerpFactor;
        smoothedPitch += pitchDelta * lerpFactor;
        smoothedPitch = Mth.clamp(smoothedPitch, -89.5F, 89.5F);

        float finalYaw = smoothedYaw + microNoiseYaw + recoilYaw;
        float finalPitch = Mth.clamp(smoothedPitch + microNoisePitch + recoilPitch, -89.5F, 89.5F);

        RotationHandler.setRotation(finalYaw, finalPitch);
    }
}
