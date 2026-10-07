package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public final class ShardExsRotation implements AuraRotation {

    private int tickCounter = 0;
    private long lastAttackTime = 0L;
    private float jitterYaw = 0.0F;
    private float jitterPitch = 0.0F;
    private float noiseYaw = 0.0F;
    private float noisePitch = 0.0F;

    @Override
    public String getName() {
        return "Shard (exs)";
    }

    @Override
    public void reset() {
        tickCounter = 0;
        lastAttackTime = 0L;
        jitterYaw = 0.0F;
        jitterPitch = 0.0F;
        noiseYaw = 0.0F;
        noisePitch = 0.0F;
    }

    @Override
    public void onAttack() {
        lastAttackTime = System.currentTimeMillis();
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) {
            reset();
            return;
        }

        tickCounter++;
        long now = System.currentTimeMillis();
        long timeSinceAttack = now - lastAttackTime;

        Vec3 eyePos = player.getEyePosition();
        Vec3 direction = targetEyePos.subtract(eyePos);

        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float targetPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))), -89.0F, 89.0F);

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        if (timeSinceAttack > 2000L) {
            jitterYaw = (float) (3.0 * Math.sin(now / 85.0));
            jitterPitch = (float) (2.0 * Math.cos(now / 85.0));
        } else {
            jitterYaw = 0.0F;
            jitterPitch = 0.0F;
        }

        noiseYaw = (float) Math.sin(tickCounter * 0.05) * 1.2F;
        noisePitch = (float) Math.cos(tickCounter * 0.07) * 0.6F;

        targetYaw += jitterYaw + noiseYaw;
        targetPitch += jitterPitch + noisePitch;

        float yawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = targetPitch - currentPitch;

        float speed = (float) ThreadLocalRandom.current().nextDouble(0.4, 0.7);
        float lerpSpeed = speed * (float) ThreadLocalRandom.current().nextDouble(0.8, 1.2);

        float newYaw = Mth.lerp(lerpSpeed, currentYaw, currentYaw + yawDiff);
        float newPitch = Mth.lerp(lerpSpeed * 0.5F, currentPitch, Mth.clamp(currentPitch + pitchDiff, -89.0F, 89.0F));

        RotationHandler.setRotation(newYaw, newPitch);
    }
}
