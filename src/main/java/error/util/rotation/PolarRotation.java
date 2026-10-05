package error.util.rotation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import error.util.AuraRotation;
import error.util.RotationHandler;

public class PolarRotation implements AuraRotation {
    @Override
    public String getName() {
        return "Polar";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 aimPos, boolean canAttack) {
        if (target == null || aimPos == null) return;

        double currentYaw = Mth.wrapDegrees(RotationHandler.isActive() ? RotationHandler.getServerYaw() : player.getYRot());
        double currentPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : player.getXRot();

        Vec3 delta = aimPos.subtract(player.getEyePosition());
        double realYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F);
        double realPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));

        double deltaYaw = Mth.wrapDegrees(realYaw - currentYaw);
        double deltaPitch = realPitch - currentPitch;

        float maxStep = 42.0F;
        double nextYaw = Mth.wrapDegrees(currentYaw + Mth.clamp((float) deltaYaw, -maxStep, maxStep));
        double nextPitch = Mth.clamp((float) (currentPitch + Mth.clamp((float) deltaPitch, -maxStep, maxStep)), -89.0F, 89.0F);

        RotationHandler.setRotation((float) nextYaw, (float) nextPitch);
    }
}
