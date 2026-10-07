package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class ReallyWorldExsRotation implements AuraRotation {

    private int snapTicks = 0;
    private float smoothedYaw = 0;
    private float smoothedPitch = 0;
    private boolean smoothInitialized = false;

    private float hitJerkYaw = 0.0f;
    private float hitJerkPitch = 0.0f;

    @Override
    public String getName() {
        return "ReallyWorld (exs)";
    }

    @Override
    public void onAttack() {
        hitJerkYaw = (float) ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
        hitJerkPitch = (float) ThreadLocalRandom.current().nextDouble(-0.4, 0.4);
        snapTicks = ThreadLocalRandom.current().nextInt(1, 3);
    }

    @Override
    public void reset() {
        smoothInitialized = false;
        snapTicks = 0;
        hitJerkYaw = 0.0f;
        hitJerkPitch = 0.0f;
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetEyePos.subtract(eyePos);

        float rawYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))), -89.5F, 89.5F);

        boolean isSnapping = snapTicks > 0;
        if (snapTicks > 0) {
            snapTicks--;
        }

        if (!smoothInitialized) {
            smoothedYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
            smoothedPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();
            smoothInitialized = true;
        }

        float goalYaw = isSnapping ? rawYaw : player.getYRot();
        float goalPitch = isSnapping ? rawPitch : player.getXRot();
        float lerpFactor = isSnapping ? 1.0f : (float) ThreadLocalRandom.current().nextDouble(0.18, 0.28);

        float yawDiff = Mth.wrapDegrees(goalYaw - smoothedYaw);
        smoothedYaw += yawDiff * lerpFactor;
        smoothedPitch += (goalPitch - smoothedPitch) * lerpFactor;
        smoothedPitch = Mth.clamp(smoothedPitch, -89.5F, 89.5F);

        hitJerkYaw *= 0.7f;
        hitJerkPitch *= 0.7f;

        float finalYaw = smoothedYaw + hitJerkYaw;
        float finalPitch = Mth.clamp(smoothedPitch + hitJerkPitch, -89.5F, 89.5F);

        RotationHandler.setRotation(finalYaw, finalPitch);
    }
}
