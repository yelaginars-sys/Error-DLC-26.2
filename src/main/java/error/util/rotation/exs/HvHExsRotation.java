package error.util.rotation.exs;

import error.util.AuraRotation;
import error.util.RotationHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HvHExsRotation implements AuraRotation {

    @Override
    public String getName() {
        return "HvH (exs)";
    }

    @Override
    public void tick(LocalPlayer player, LivingEntity target, Vec3 targetEyePos, boolean attackLikely) {
        if (target == null || player == null) return;

        Vec3 eyePos = player.getEyePosition();
        Vec3 diff = targetEyePos.subtract(eyePos);

        float targetYaw = (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));
        targetPitch = Mth.clamp(targetPitch, -89.9F, 89.9F);

        RotationHandler.setRotation(targetYaw, targetPitch);

        player.yBodyRot = targetYaw;
        player.yHeadRot = targetYaw;
    }
}
