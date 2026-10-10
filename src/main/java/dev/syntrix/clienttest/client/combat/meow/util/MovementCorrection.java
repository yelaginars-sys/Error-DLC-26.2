package dev.syntrix.clienttest.client.combat.meow.util;

import net.minecraft.util.Mth;
import dev.syntrix.clienttest.client.combat.meow.event.MovementInputEvent;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;

public final class MovementCorrection implements MinecraftAccess {
   private static final float state001 = Float.MAX_VALUE;
   private static final float state002 = -1.0F;
   private static final float state003 = -1.0F;
   private static final float state004 = Float.MAX_VALUE;
   private static final float state005 = -1.0F;
   private static final float state006 = -1.0F;
   private static final float state007 = Float.MAX_VALUE;
   private static final float state008 = -1.0F;
   private static final float state009 = -1.0F;
   private static final double state010 = 90.0;
   private static final float state011 = Float.MAX_VALUE;
   private static final float state012 = -1.0F;
   private static final float state013 = -1.0F;
   private static final float state014 = Float.MAX_VALUE;
   private static final float state015 = -1.0F;
   private static final float state016 = -1.0F;
   private static final String TEXT_THIS_IS_A_UTILITY_CLASS_AND_CANNOT_BE_INSTANTIATED_1 = new String("This is a utility class and cannot be instantiated");

   public static void operation002(MovementInputEvent var0, float var1) {
      float var2 = var0.getState002();
      float var3 = var0.getState003();
      if (var2 != 0.0F || var3 != 0.0F) {
         double var4 = Mth.wrapDegrees(Math.toDegrees(MovementMath.direction(var1, var2, var3)));
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = state001;

         for (float var9 = state002; var9 <= 1.0F; var9++) {
            for (float var10 = state003; var10 <= 1.0F; var10++) {
               if (var9 != 0.0F || var10 != 0.0F) {
                  double var11 = Mth.wrapDegrees(Math.toDegrees(MovementMath.direction(var1, var9, var10)));
                  float var13 = Math.abs(Mth.wrapDegrees((float)(var4 - var11)));
                  if (var13 < var8) {
                     var8 = var13;
                     var6 = var9;
                     var7 = var10;
                  }
               }
            }
         }

         var0.operation003(var6);
         var0.operation008(var7);
      }
   }
   public static void correctInput(MovementInputEvent var0, float var1) {
      float var2 = var0.getState002();
      float var3 = var0.getState003();
      if (var2 != 0.0F || var3 != 0.0F) {
         double var4 = Mth.wrapDegrees(Math.toDegrees(MovementMath.direction(var1, var2, var3)));
         float var6 = 0.0F;
         float var7 = 0.0F;
         float var8 = state007;

         for (float var9 = state008; var9 <= 1.0F; var9++) {
            for (float var10 = state009; var10 <= 1.0F; var10++) {
               if (var9 != 0.0F || var10 != 0.0F) {
                  double var11 = Mth.wrapDegrees(Math.toDegrees(MovementMath.direction(mc.player.getYRot(), var9, var10)));
                  float var13 = Math.abs(Mth.wrapDegrees((float)(var4 - var11)));
                  if (var13 < var8) {
                     var8 = var13;
                     var6 = var9;
                     var7 = var10;
                  }
               }
            }
         }

         var0.operation003(var6);
         var0.operation008(var7);
      }
   }
}
