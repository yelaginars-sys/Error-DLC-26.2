package dev.syntrix.clienttest.client.combat.meow.util;

final class MiningMath {
   private static final float state001 = 0.2F;
   private static final float state002 = 0.3F;
   private static final float state003 = 0.09F;
   private static final float state004 = 0.0027F;
   private static final float state005 = 8.1E-4F;
   private static final float state006 = 5.0F;
   private static final float state007 = 30.0F;
   private static final float state008 = 100.0F;

   static float operation001(float var0, boolean var1, boolean var2, float var3) {
      if (var0 < 0.0F) {
         return Float.POSITIVE_INFINITY;
      }

      if (var0 == 0.0F) {
         return 0.0F;
      }

      if (!(var3 > 0.0F)) {
         return Float.POSITIVE_INFINITY;
      }

      float var4 = var1 && !var2 ? state008 : state007;
      return var0 * var4 / var3;
   }

   private MiningMath() {
   }

   static float operation003(float var0, int var1, int var2, float var3, float var4, boolean var5) {
      float var6 = var0;
      if (var1 >= 0) {
         var6 *= 1.0F + (var1 + 1) * state001;
      }

      if (var2 >= 0) {
         var6 *= switch (var2) {
            case 0 -> state002;
            case 1 -> state003;
            case 2 -> state004;
            default -> state005;
         };
      }

      var6 *= var3;
      var6 *= var4;
      return var5 ? var6 : var6 / state006;
   }
}
