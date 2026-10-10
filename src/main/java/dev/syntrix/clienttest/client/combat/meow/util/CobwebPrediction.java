package dev.syntrix.clienttest.client.combat.meow.util;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CobwebPrediction {
   private static final double state001 = 0.1;
   private static final double state002 = 0.1;
   private static final double state003 = 4.0;
   private static final double state004 = 1.5;
   private static final double state005 = 1.0E-7;
   private static final double state006 = 1.0E-7;
   private static final double state007 = 1.0E-7;
   private static final double state008 = 1.0E-7;
   private static final double state009 = 1.0E-7;
   private static final double state010 = 1.0E-7;

   public static boolean operation001(AABB var0, Vec3 var1, Predicate<BlockPos> var2) {
      AABB var3 = var0.deflate(state001, 0.0, state002);
      if (!intersectsMatchingBlock(var3, var2)) {
         return false;
      } else {
         return var1 != null && Double.isFinite(var1.x) && Double.isFinite(var1.y) && Double.isFinite(var1.z) && !(var1.lengthSqr() > state003)
            ? intersectsMatchingBlock(var3.move(var1.scale(state004)), var2)
            : false;
      }
   }

   private CobwebPrediction() {
   }

   public static boolean intersectsMatchingBlock(AABB var0, Predicate<BlockPos> var1) {
      BlockPos var2 = BlockPos.containing(var0.minX + state005, var0.minY + state006, var0.minZ + state007);
      BlockPos var3 = BlockPos.containing(var0.maxX - state008, var0.maxY - state009, var0.maxZ - state010);

      for (BlockPos var5 : BlockPos.betweenClosed(var2, var3)) {
         if (var1.test(var5)) {
            return true;
         }
      }

      return false;
   }
}
