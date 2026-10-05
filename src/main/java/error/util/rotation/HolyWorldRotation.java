package error.util.rotation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import error.util.AuraRotation;
import error.util.RotationHandler;

import java.util.concurrent.ThreadLocalRandom;

public class HolyWorldRotation implements AuraRotation {
    @Override
    public String getName() {
        return "HolyWorld";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 aimPos, boolean canAttack) {
        if (target == null || aimPos == null) return;

        double currentYaw = Mth.wrapDegrees(RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot());
        double currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        Vec3 eye = player.getEyePosition();
        Vec3 delta = aimPos.subtract(eye);
        double realYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F);
        double realPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        double deltaYaw = Mth.wrapDegrees(realYaw - currentYaw);
        double deltaPitch = realPitch - currentPitch;

        float jitterYaw = (float) ThreadLocalRandom.current().nextDouble(-0.35D, 0.35D);
        float jitterPitch = (float) ThreadLocalRandom.current().nextDouble(-0.25D, 0.25D);

        float speed = canAttack ? 48.0F : 28.0F;
        double nextYaw = Mth.wrapDegrees(currentYaw + Mth.clamp((float) deltaYaw, -speed, speed) + jitterYaw);
        double nextPitch = Mth.clamp((float) (currentPitch + Mth.clamp((float) deltaPitch, -speed, speed) + jitterPitch), -89.5F, 89.5F);

        RotationHandler.setRotation((float) nextYaw, (float) nextPitch);
    }
}
