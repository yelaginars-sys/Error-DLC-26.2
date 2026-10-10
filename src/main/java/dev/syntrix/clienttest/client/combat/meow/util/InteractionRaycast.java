package dev.syntrix.clienttest.client.combat.meow.util;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class InteractionRaycast {
   public static final int state001 = 64;
   private static final String TEXT_SOURCE_1 = new String("source");
   private static final String TEXT_TARGETPOS_2 = new String("targetPos");
   private static final String TEXT_DISTANCE_MUST_BE_FINITE_AND_NON_NEGATIVE_3 = new String("distance must be finite and non-negative");
   private static final String TEXT_SOURCE_4 = new String("source");
   private static final String TEXT_START_5 = new String("start");
   private static final String TEXT_END_6 = new String("end");
   private static final String TEXT_TARGETPOS_7 = new String("targetPos");
   private static final String TEXT_SHAPETYPE_8 = new String("shapeType");
   private static final String TEXT_FLUIDHANDLING_9 = new String("fluidHandling");
   private static final double state014 = 1.0E-8;
   private static final double state015 = 1.0E-4;
   private static final double state016 = 1.0E-8;
   private static final double state017 = 0.51;
   private static final double state018 = 0.51;
   private static final double state019 = 1.0E-4;
   private static final double state020 = 1.0E-8;
   private static final double state021 = -1.0E-8;

   private static Vec3 advancePastHit(Vec3 var0, BlockHitResult var1) {
      if (!var1.isInside()) {
         return var1.getLocation().add(var0.scale(state015));
      } else {
         Vec3 var2 = exitBlockAlongRay(var1.getLocation(), var0, var1.getBlockPos());
         if (var2 != null) {
            return var2;
         } else {
            return var0.lengthSqr() < state016
               ? var1.getLocation().add(0.0, state017, 0.0)
               : Vec3.atCenterOf(var1.getBlockPos()).add(var0.normalize().scale(state018));
         }
      }
   }

   private static Vec3 exitBlockAlongRay(Vec3 var0, Vec3 var1, BlockPos var2) {
      double var3 = var2.getX();
      double var5 = var2.getY();
      double var7 = var2.getZ();
      double var9 = Double.POSITIVE_INFINITY;
      var9 = closestExitTime(var9, var1.x, var3 - var0.x, var3 + 1.0 - var0.x);
      var9 = closestExitTime(var9, var1.y, var5 - var0.y, var5 + 1.0 - var0.y);
      var9 = closestExitTime(var9, var1.z, var7 - var0.z, var7 + 1.0 - var0.z);
      return Double.isFinite(var9) ? var0.add(var1.scale(var9 + state019)) : null;
   }

   public static boolean hitsBlock(BlockHitResult var0, BlockPos var1) {
      return var0 != null && var1 != null && var0.getType() == Type.BLOCK && var1.equals(var0.getBlockPos());
   }

   public static BlockHitResult raycastToTarget(Entity var0, Vec3 var1, Vec3 var2, BlockPos var3, Block var4, Fluid var5) {
      Objects.requireNonNull(var0, TEXT_SOURCE_4);
      Objects.requireNonNull(var1, TEXT_START_5);
      Objects.requireNonNull(var2, TEXT_END_6);
      Objects.requireNonNull(var3, TEXT_TARGETPOS_7);
      Objects.requireNonNull(var4, TEXT_SHAPETYPE_8);
      Objects.requireNonNull(var5, TEXT_FLUIDHANDLING_9);
      Level var6 = var0.level();
      BlockHitResult var7 = raycastWorld(var6, var0, var1, var2, var4, var5);
      if (var7.getType() != Type.MISS && !hitsBlock(var7, var3)) {
         Vec3 var8 = var2.subtract(var1);
         double var9 = var8.lengthSqr();
         if (var9 < state014) {
            return var7;
         }

         Vec3 var11 = var8.scale(1.0 / Math.sqrt(var9));

         for (int var12 = 0; var12 < 64; var12++) {
            Vec3 var13 = advancePastHit(var11, var7);
            if (var13.distanceToSqr(var1) >= var9) {
               return BlockHitResult.miss(var2, var7.getDirection(), var7.getBlockPos());
            }

            var7 = raycastWorld(var6, var0, var13, var2, var4, var5);
            if (var7.getType() == Type.MISS || hitsBlock(var7, var3)) {
               return var7;
            }
         }

         return var7;
      } else {
         return var7;
      }
   }

   private InteractionRaycast() {
   }

   private static BlockHitResult raycastWorld(Level var0, Entity var1, Vec3 var2, Vec3 var3, Block var4, Fluid var5) {
      return var0.clip(new ClipContext(var2, var3, var4, var5, var1));
   }

   public static BlockHitResult raycastOutlineToTarget(Entity var0, Vec3 var1, Vec3 var2, BlockPos var3) {
      return raycastToTarget(var0, var1, var2, var3, Block.OUTLINE, Fluid.NONE);
   }

   private static double closestExitTime(double var0, double var2, double var4, double var6) {
      double var8;
      if (var2 > state020) {
         var8 = var6 / var2;
      } else {
         if (!(var2 < state021)) {
            return var0;
         }

         var8 = var4 / var2;
      }

      return var8 >= 0.0 && var8 < var0 ? var8 : var0;
   }

   public static BlockHitResult raycastViewToTarget(Entity var0, BlockPos var1, double var2, float var4) {
      Objects.requireNonNull(var0, TEXT_SOURCE_1);
      Objects.requireNonNull(var1, TEXT_TARGETPOS_2);
      if (Double.isFinite(var2) && !(var2 < 0.0)) {
         Vec3 var5 = var0.getEyePosition(var4);
         Vec3 var6 = var5.add(var0.getViewVector(var4).scale(var2));
         return raycastOutlineToTarget(var0, var5, var6, var1);
      } else {
         throw new IllegalArgumentException(TEXT_DISTANCE_MUST_BE_FINITE_AND_NON_NEGATIVE_3);
      }
   }
}
