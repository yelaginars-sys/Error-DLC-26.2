package error.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static error.IMinecraft.mc;

/**
 */
public final class RayTraceUtils {

    private RayTraceUtils() {}


    public static Vec3 getLookVec(float yaw, float pitch) {
        float f = pitch * ((float) Math.PI / 180F);
        float f1 = -yaw * ((float) Math.PI / 180F);
        float cosYaw = Mth.cos(f1);
        float sinYaw = Mth.sin(f1);
        float cosPitch = Mth.cos(f);
        float sinPitch = Mth.sin(f);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }

    public static boolean isLookingAt(Entity target, double maxRange, float yaw, float pitch) {
        if (mc.player == null || target == null) return false;

        Vec3 eyes = mc.player.getEyePosition();
        Vec3 look = getLookVec(yaw, pitch);
        Vec3 reachVec = eyes.add(look.scale(maxRange));
        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                eyes, reachVec,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));

        double maxEntityDistSq = maxRange * maxRange;
        if (blockHit.getType() != HitResult.Type.MISS) {
            maxEntityDistSq = eyes.distanceToSqr(blockHit.getLocation());
        }

        AABB box = target.getBoundingBox().inflate(target.getPickRadius() + 0.05);
        Optional<Vec3> hit = box.clip(eyes, reachVec);

        if (hit.isPresent()) {
            return eyes.distanceToSqr(hit.get()) <= maxEntityDistSq;
        }

        return false;
    }


    public static boolean canSeePoint(Vec3 from, Vec3 to) {
        if (mc.level == null) return false;
        HitResult hit = mc.level.clip(new ClipContext(
                from, to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return hit.getType() == HitResult.Type.MISS;
    }
}