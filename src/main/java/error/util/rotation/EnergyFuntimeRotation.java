package error.util.rotation;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Energy FunTime Rotation algorithm port (Util14).
 * Cooldown-driven figure-eight curve with sub-pixel noise and anticheat desync.
 */
public final class EnergyFuntimeRotation implements AuraRotation {

    private float phase = 0.0F;

    @Override
    public String getName() {
        return "FunTime (En)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        Vec3 eyePos = player.getEyePosition();
        AABB box = target.getBoundingBox().inflate(-0.05);
        Vec3 center = box.getCenter();
        Vec3 delta = center.subtract(eyePos);

        float baseYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float basePitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        float cooldown = player.getAttackStrengthScale(1.0F);
        float recharge = 1.0F - cooldown;

        phase += 0.35F + ThreadLocalRandom.current().nextFloat() * 0.12F;

        // Figure-eight curve when recharging cooldown
        float overshootYaw = (float) (Math.sin(phase) * (8.5F + 4.5F * recharge));
        float overshootPitch = (float) (Math.sin(phase * 2.0F) * (3.5F + 2.5F * recharge));

        // Sub-pixel micro noise to bypass pattern detection
        float noise = ThreadLocalRandom.current().nextFloat(-0.08F, 0.08F);

        float rawYaw = baseYaw + (overshootYaw * recharge) + noise;
        float rawPitch = basePitch + (overshootPitch * recharge) + noise;

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        float turnSpeed = cooldown >= 0.92F ? 110.0F : 55.0F;
        float stepYaw = Mth.clamp(deltaYaw * 0.82F, -turnSpeed, turnSpeed);
        float stepPitch = Mth.clamp(deltaPitch * 0.82F, -turnSpeed, turnSpeed);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
