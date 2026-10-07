package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class SlothExsRotation implements AuraRotation {

    private int snapTicks = 0;
    private float smoothedYaw = 0.0f;
    private float smoothedPitch = 0.0f;
    private boolean initialized = false;

    private float recoilYaw = 0.0f;
    private float recoilPitch = 0.0f;

    @Override
    public String getName() {
        return "Sloth (exs)";
    }

    @Override
    public void onAttack() {
        recoilYaw = (float) ThreadLocalRandom.current().nextDouble(-0.35, 0.35);
        recoilPitch = (float) ThreadLocalRandom.current().nextDouble(-0.25, 0.15);
        snapTicks = 1;
    }

    @Override
    public void reset() {
        initialized = false;
        snapTicks = 0;
        recoilYaw = 0.0f;
        recoilPitch = 0.0f;
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) {
            reset();
            return;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetEyePos.subtract(eyePos);
        double distHoriz = Math.max(Math.hypot(diff.x, diff.z), 0.001);

        float targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(diff.y, distHoriz)), -89.5F, 89.5F);

        float cameraYaw = player.getYRot();
        float cameraPitch = player.getXRot();

        if (!initialized) {
            smoothedYaw = cameraYaw;
            smoothedPitch = cameraPitch;
            initialized = true;
        }

        boolean isSnapping = snapTicks > 0;
        if (snapTicks > 0) {
            snapTicks--;
        }

        recoilYaw *= 0.65f;
        recoilPitch *= 0.65f;

        float goalYaw = isSnapping ? targetYaw : cameraYaw;
        float goalPitch = isSnapping ? targetPitch : cameraPitch;

        float finalYaw;
        float finalPitch;

        if (isSnapping) {
            finalYaw = goalYaw;
            finalPitch = goalPitch;
            smoothedYaw = goalYaw;
            smoothedPitch = goalPitch;
        } else {
            float yawDiff = Mth.wrapDegrees(goalYaw - smoothedYaw);
            float pitchDiff = goalPitch - smoothedPitch;
            smoothedYaw += yawDiff * (float) ThreadLocalRandom.current().nextDouble(0.22, 0.35);
            smoothedPitch += pitchDiff * (float) ThreadLocalRandom.current().nextDouble(0.22, 0.35);
            finalYaw = smoothedYaw + recoilYaw;
            finalPitch = smoothedPitch + recoilPitch;
        }

        finalPitch = Mth.clamp(finalPitch, -89.5F, 89.5F);

        RotationHandler.setRotation(finalYaw, finalPitch);
    }
}
