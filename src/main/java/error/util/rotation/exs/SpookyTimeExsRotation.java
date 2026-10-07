package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class SpookyTimeExsRotation implements AuraRotation {

    private float currentSpeedYaw = 40;
    private float currentSpeedPitch = 25;

    @Override
    public String getName() {
        return "SpookyTime (exs)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetEyePos.subtract(eyePos);

        float yawJitter = (float) (Math.cos(System.currentTimeMillis() / 200.0) * ThreadLocalRandom.current().nextDouble(6.0, 8.0));
        float pitchJitter = (float) (Math.sin(System.currentTimeMillis() / 200.0) * ThreadLocalRandom.current().nextDouble(6.0, 8.0));

        float targetSpeedYaw = (float) ThreadLocalRandom.current().nextDouble(52.0, 60.0);
        float targetSpeedPitch = (float) ThreadLocalRandom.current().nextDouble(12.0, 18.0);

        float rawYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z))), -89.5F, 89.5F);

        float smoothFactor = 0.4f;

        currentSpeedYaw += (targetSpeedYaw - currentSpeedYaw) * smoothFactor;
        currentSpeedPitch += (targetSpeedPitch - currentSpeedPitch) * smoothFactor;

        float finalYaw = rawYaw + yawJitter;
        float finalPitch = Mth.clamp(rawPitch + pitchJitter, -89.5F, 89.5F);

        double torsoMiddleY = (target.getY() + target.getBbHeight() * 0.4) - player.getEyeY();
        float maxDownPitch = (float) -Math.toDegrees(Math.atan2(torsoMiddleY, Math.hypot(diff.x, diff.z)));
        finalPitch = Math.min(finalPitch, maxDownPitch);
        finalPitch = Mth.clamp(finalPitch, -89.5F, 89.5F);

        RotationHandler.setRotation(finalYaw, finalPitch);
    }
}
