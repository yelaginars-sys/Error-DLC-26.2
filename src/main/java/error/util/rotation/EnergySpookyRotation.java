package error.util.rotation;

import error.module.impl.combat.AuraModule;
import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Energy Spooky Rotation algorithm port (Util119).
 * Features micro-jerk deviations with customizable smoothing and speed.
 */
public final class EnergySpookyRotation implements AuraRotation {

    private float jerkPhase = 0.0F;

    @Override
    public String getName() {
        return "Spooky (En)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (player == null || target == null) return;

        AuraModule aura = AuraModule.INSTANCE;
        float jerkSmoothing = aura != null ? aura.enJerkSmoothing.getValue() : 3.0F;
        float jerkSpeed = aura != null ? aura.enJerkSpeed.getValue() : 0.5F;

        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 delta = targetCenter.subtract(eyePos);

        float rawYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
        float rawPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        // Micro jerk oscillation
        jerkPhase += jerkSpeed * 0.4F;
        float jerkMagnitude = (18.0F / Math.max(1.0F, jerkSmoothing));
        float jerkYaw = (float) (Math.sin(jerkPhase) * jerkMagnitude + ThreadLocalRandom.current().nextDouble(-0.35, 0.35));
        float jerkPitch = (float) (Math.cos(jerkPhase * 1.5F) * (jerkMagnitude * 0.45F) + ThreadLocalRandom.current().nextDouble(-0.25, 0.25));

        rawYaw += jerkYaw;
        rawPitch = Mth.clamp(rawPitch + jerkPitch, -89.5F, 89.5F);

        float currentYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot();
        float currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        float deltaYaw = Mth.wrapDegrees(rawYaw - currentYaw);
        float deltaPitch = rawPitch - currentPitch;

        float maxSpeed = 75.0F / Math.max(0.5F, jerkSmoothing * 0.3F);
        float stepYaw = Mth.clamp(deltaYaw * 0.75F, -maxSpeed, maxSpeed);
        float stepPitch = Mth.clamp(deltaPitch * 0.75F, -maxSpeed, maxSpeed);

        RotationHandler.setRotation(currentYaw + stepYaw, Mth.clamp(currentPitch + stepPitch, -89.5F, 89.5F));
    }
}
