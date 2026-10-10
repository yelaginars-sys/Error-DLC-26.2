package dev.syntrix.clienttest.client.combat.meow.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MotionPrediction {
   private static final Minecraft state001 = Minecraft.getInstance();
   private static final float state002 = 25.0F;
   private static final double state006 = 20.0;
   private static final double state007 = 20.0;
   private static final double state008 = 5.0;
   private static final double state009 = 5.0;
   private static final double state010 = 1.0E-4;
   private static final double state011 = 20.0;
   private static final double state012 = 5.0;
   private static final double state013 = 20.0;
   private static final double state014 = 5.0;
   private static final double state015 = 0.4;
   private static final double state016 = -0.08;
   private static final double state017 = -1.0;
   private static final double state018 = 0.75;
   private static final double state019 = -0.1;
   private static final double state020 = 0.04;
   private static final double state021 = 3.2;
   private static final double state022 = 0.1;
   private static final double state023 = 0.1;
   private static final double state024 = 0.99;
   private static final double state025 = 0.98;
   private static final double state026 = 0.99;
   private static final float state027 = 6.0F;
   private static final double state028 = 0.3;
   private static final double state029 = 0.017453293005625408;
   private static final double state030 = 0.01;
   private static final double state031 = -1.0;
   private static final double state032 = 0.75;
   private static final double state033 = -0.1;
   private static final double state034 = 0.04;
   private static final double state035 = 3.2;
   private static final double state036 = 0.1;
   private static final double state037 = 0.1;
   private static final double state038 = 0.0025;
   private static final double state039 = 0.001;
   private static final double state040 = 0.0025;
   private static final double state041 = 0.99F;
   private static final double state042 = 0.98F;
   private static final double state043 = 0.99F;
   private static final double state044 = 0.3;
   private static final double state045 = 1.0E-4;
   private static final double state046 = 1.0E-4;
   private static final float state047 = 25.0F;
   private static final double state048 = 1.0E-4;
   private static final double state049 = 1.0E-8;
   private static final double state050 = 1.0E-4;
   private static final double state051 = 16.0;
   private static final double state052 = 1.0E-4;
   private static final double state053 = 16.0;
   private static final double state054 = 20.0;
   private static final double state055 = 20.0;
   private static final double state056 = 20.0;
   private static final double state057 = 20.0;
   private static final float state058 = 6.0F;
   private static final double state059 = 20.0;
   private static final double state060 = 20.0;
   private static final double state061 = 0.01;
   private static final double state062 = -1.0;
   private static final double state063 = 0.75;
   private static final double state064 = -0.1;
   private static final double state065 = 0.04;
   private static final double state066 = 3.2;
   private static final double state067 = 0.1;
   private static final double state068 = 0.1;
   private static final double state069 = 0.99;
   private static final double state070 = 0.98;
   private static final double state071 = 0.99;
   private static final double state072 = 0.35;
   private static final double state073 = 0.4;
   private static final double state074 = -0.08;
   private static final double state075 = -1.0;
   private static final double state076 = 0.75;
   private static final double state077 = -0.1;
   private static final double state078 = 0.04;
   private static final double state079 = 3.2;
   private static final double state080 = 0.1;
   private static final double state081 = 0.1;
   private static final double state082 = 0.99;
   private static final double state083 = 0.98;
   private static final double state084 = 0.99;
   private static final float state085 = 3.0F;
   private static final float state086 = 0.35F;
   private static final float state087 = 3.0F;
   private static final float state088 = 0.4F;
   private static final float state089 = 50.0F;
   private static final float state090 = 1.5F;
   private static final double state091 = 20.0;
   private static final double state092 = 1.0E-4;

   public static boolean operation001(LivingEntity var0) {
      if (var0 == null || state001.player == null) {
         return false;
      } else {
         return var0.isFallFlying() && !var0.onGround() && !operation018(var0) ? operation002(var0) >= state047 && operation020(var0) : false;
      }
   }

   private static float operation002(LivingEntity var0) {
      double var1 = var0.getX() - var0.xOld;
      double var3 = var0.getY() - var0.yOld;
      double var5 = var0.getZ() - var0.zOld;
      return (float)(Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5) * state091);
   }

   public static Vec3 operation003(LivingEntity var0) {
      if (var0 == null) {
         return Vec3.ZERO;
      }

      Vec3 var1 = operation022(var0);
      if (var1.lengthSqr() > state050 && var1.lengthSqr() < state051) {
         return var1;
      }

      Vec3 var2 = new Vec3(var0.getX() - var0.xo, var0.getY() - var0.yo, var0.getZ() - var0.zo);
      return var2.lengthSqr() > state052 && var2.lengthSqr() < state053 ? var2 : var0.getDeltaMovement();
   }

   private MotionPrediction() {
   }

   public static boolean operation005(LivingEntity var0, double var1) {
      if (var0 != null && var0.isFallFlying()) {
         Vec3 var3 = operation021(var0);
         double var4 = var3.horizontalDistance() * state056;
         double var6 = Math.abs(var3.y) * state057;
         return var4 <= var1 && var6 <= var1;
      } else {
         return true;
      }
   }

   public static Vec3 operation006(LivingEntity var0, float var1) {
      if (var0 instanceof ServerMotionAccess var2) {
         double var3 = var2.meow$getServerX();
         double var5 = var2.meow$getServerY();
         double var7 = var2.meow$getServerZ();
         Vec3 var9 = var2.meow$getServerVelocity();
         if ((var3 != 0.0 || var5 != 0.0 || var7 != 0.0) && var9 != null && var9.lengthSqr() > 0.0) {
            return new Vec3(var3 + var9.x * var1, var5 + var9.y * var1, var7 + var9.z * var1);
         }
      }

      return var0.getPosition(var1);
   }

   public static Vec3 operation007(LivingEntity var0) {
      return new Vec3(var0.xo, var0.yo, var0.zo);
   }

   private static double operation008(LivingEntity var0, float var1) {
      float var2 = Mth.clamp(var1 / state085, state086, state087);
      return operation028(var0) * var2;
   }

   public static Vec3 operation009(Vec3 var0) {
      if (var0 == null) {
         return Vec3.ZERO;
      }

      Vec3 var1 = new Vec3(var0.x, 0.0, var0.z);
      return var1.lengthSqr() <= state049 ? Vec3.ZERO : var1.normalize();
   }

   public static Vec3 predictPosition(LivingEntity var0, Vec3 var1, float var2) {
      if (var0 != null && var1 != null) {
         float var3 = Math.max(0.0F, var2);
         if (var3 <= 0.0F) {
            return var1;
         } else if (!var0.isFallFlying()) {
            return var1.add(var0.getDeltaMovement().scale(var3));
         } else {
            Vec3 var4 = operation021(var0);
            double var5 = var4.horizontalDistance() * state006;
            double var7 = Math.abs(var4.y) * state007;
            if (var5 <= state008 && var7 <= state009) {
               return var1;
            } else if (!var0.onGround() && !operation018(var0)) {
               Vec3 var9 = operation023(var0, var1, var3);
               Vec3 var10 = operation022(var0);
               return var10.lengthSqr() <= state010 ? var9 : var9.add(var10.scale(operation008(var0, var3)));
            } else {
               return var1;
            }
         }
      } else {
         return var1;
      }
   }

   public static double operation011(LivingEntity var0) {
      return var0 == null ? 0.0 : operation021(var0).horizontalDistance() * state054;
   }

   public static Vec3 operation012(LivingEntity var0, float var1) {
      return var0 == null ? Vec3.ZERO : predictPosition(var0, var0.position(), var1).subtract(var0.position());
   }

   public static Vec3 operation013(LivingEntity var0) {
      if (var0 == null) {
         return Vec3.ZERO;
      }

      AABB var1 = var0.getBoundingBox();
      Vec3 var2 = var1.getCenter();
      if (!var0.isFallFlying()) {
         return var2;
      }

      Vec3 var3 = operation015(var0);
      return new Vec3(var3.x, var2.y, var3.z);
   }

   public static Vec3 operation014(LivingEntity var0, Vec3 var1, float var2) {
      return var0 != null && var1 != null ? predictPosition(var0, var1, var2).subtract(var1) : Vec3.ZERO;
   }

   public static Vec3 operation015(LivingEntity var0) {
      if (var0 instanceof ServerMotionAccess var1) {
         double var2 = var1.meow$getServerX();
         double var4 = var1.meow$getServerY();
         double var6 = var1.meow$getServerZ();
         if (var2 != 0.0 || var4 != 0.0 || var6 != 0.0) {
            return new Vec3(var2, var4, var6);
         }
      }

      return var0.position();
   }

   public static Vec3 operation017(LivingEntity var0, Vec3 var1, float var2) {
      if (var0 != null && var1 != null) {
         float var3 = Mth.clamp(var2, 0.0F, state058);
         if (var3 <= 0.0F) {
            return var1;
         }

         if (var0.isFallFlying() && !var0.onGround() && !operation018(var0)) {
            Vec3 var4 = operation021(var0);
            double var5 = var4.horizontalDistance() * state059;
            double var7 = Math.abs(var4.y) * state060;
            if (var5 <= 0.0 && var7 <= 0.0) {
               return var1;
            }

            Vec3 var9 = var0.getLookAngle();
            float var10 = (float)Math.toRadians(var0.getXRot());
            double var11 = Math.sqrt(var9.x * var9.x + var9.z * var9.z);
            Vec3 var13 = var4;
            double var14 = var13.horizontalDistance();
            boolean var16 = var0.getDeltaMovement().y <= 0.0;
            double var17 = var16 && var0.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(var0.getGravity(), state061) : var0.getGravity();
            double var19 = Mth.square(Math.cos(var10));
            var13 = var13.add(0.0, var17 * (state062 + var19 * state063), 0.0);
            if (var13.y < 0.0 && var11 > 0.0) {
               double var21 = var13.y * state064 * var19;
               var13 = var13.add(var9.x * var21 / var11, var21, var9.z * var21 / var11);
            }

            if (var10 < 0.0F && var11 > 0.0) {
               double var24 = var14 * -Mth.sin(var10) * state065;
               var13 = var13.add(-var9.x * var24 / var11, var24 * state066, -var9.z * var24 / var11);
            }

            if (var11 > 0.0) {
               var13 = var13.add((var9.x / var11 * var14 - var13.x) * state067, 0.0, (var9.z / var11 * var14 - var13.z) * state068);
            }

            Vec3 var25 = var13.multiply(state069, state070, state071);
            return var1.add(var25.scale(var3)).add(0.0, state072, 0.0);
         } else {
            return var1;
         }
      } else {
         return var1;
      }
   }

   public static boolean operation018(LivingEntity var0) {
      if (var0 != null && state001.getDeltaTracker() != null) {
         Vec3 var1 = new Vec3(var0.xOld, var0.yOld, var0.zOld);
         Vec3 var2 = var0.getPosition(state001.getDeltaTracker().getGameTimeDeltaPartialTick(true));
         return var2.distanceToSqr(var1) <= state048;
      } else {
         return false;
      }
   }

   public static Vec3 operation019(LivingEntity var0, Vec3 var1, float var2) {
      return var0 != null && var1 != null ? operation027(var0, var1, var0.getDeltaMovement(), var2) : var1;
   }

   private static boolean operation020(LivingEntity var0) {
      Vec3 var1 = operation022(var0);
      if (var1.lengthSqr() <= state092) {
         var1 = var0.getDeltaMovement();
      }

      Vec3 var2 = var0.position().subtract(state001.player.position());
      return var1.dot(var2) > 0.0;
   }

   public static Vec3 operation021(LivingEntity var0) {
      Vec3 var1 = operation022(var0);
      if (var1.lengthSqr() > state045) {
         return var1;
      }

      if (var0 instanceof ServerMotionAccess var2 && var2.meow$getServerVelocity() != null) {
         Vec3 var3 = var2.meow$getServerVelocity();
         if (var3.lengthSqr() > state046) {
            return var3;
         }
      }

      return var0.getDeltaMovement();
   }

   public static Vec3 operation022(LivingEntity var0) {
      return operation015(var0).subtract(operation007(var0));
   }

   private static Vec3 operation023(LivingEntity var0, Vec3 var1, float var2) {
      int var3 = Math.round(var2);
      Vec3 var4 = var0.getDeltaMovement();

      for (int var5 = 0; var5 < var3; var5++) {
         Vec3 var6 = var0.getLookAngle();
         float var7 = (float)Math.toRadians(var0.getXRot());
         double var8 = Math.sqrt(var6.x * var6.x + var6.z * var6.z);
         double var10 = var4.length();
         float var12 = Mth.cos(var7);
         var12 = (float)(var12 * var12 * Math.min(1.0, var6.length() / state073));
         var4 = var4.add(0.0, state074 * (state075 + var12 * state076), 0.0);
         if (var4.y < 0.0 && var8 > 0.0) {
            double var13 = var4.y * state077 * var12;
            var4 = var4.add(var6.x * var13 / var8, var13, var6.z * var13 / var8);
         }

         if (var7 < 0.0F && var8 > 0.0) {
            double var17 = var10 * -Mth.sin(var7) * state078;
            var4 = var4.add(-var6.x * var17 / var8, var17 * state079, -var6.z * var17 / var8);
         }

         if (var8 > 0.0) {
            var4 = var4.add((var6.x / var8 * var10 - var4.x) * state080, 0.0, (var6.z / var8 * var10 - var4.z) * state081);
         }

         var4 = var4.multiply(state082, state083, state084);
         var1 = var1.add(var4);
      }

      return var1;
   }

   public static Vec3 operation024(LivingEntity var0, Vec3 var1, float var2, boolean var3) {
      return var3 ? operation006(var0, var2) : predictPosition(var0, var1, var2);
   }

   public static Vec3 operation025(LivingEntity var0, Vec3 var1, float var2) {
      if (var0.isFallFlying()
         && Math.hypot(var0.getX() - var0.xo, var0.getZ() - var0.zo) * state011 <= state012
         && (var0.getY() - var0.yo) * state013 <= state014) {
         return var1;
      }

      int var3 = Math.round(var2);
      Vec3 var4 = var0.getDeltaMovement();

      for (int var5 = 0; var5 < var3; var5++) {
         Vec3 var6 = var0.getLookAngle();
         float var7 = (float)Math.toRadians(var0.getXRot());
         double var8 = Math.sqrt(var6.x * var6.x + var6.z * var6.z);
         double var10 = var4.length();
         float var12 = Mth.cos(var7);
         var12 = (float)(var12 * var12 * Math.min(1.0, var6.length() / state015));
         var4 = var4.add(0.0, state016 * (state017 + var12 * state018), 0.0);
         if (var4.y < 0.0 && var8 > 0.0) {
            double var13 = var4.y * state019 * var12;
            var4 = var4.add(var6.x * var13 / var8, var13, var6.z * var13 / var8);
         }

         if (var7 < 0.0F && var8 > 0.0) {
            double var17 = var10 * -Mth.sin(var7) * state020;
            var4 = var4.add(-var6.x * var17 / var8, var17 * state021, -var6.z * var17 / var8);
         }

         if (var8 > 0.0) {
            var4 = var4.add((var6.x / var8 * var10 - var4.x) * state022, 0.0, (var6.z / var8 * var10 - var4.z) * state023);
         }

         var4 = var4.multiply(state024, state025, state026);
         var1 = var1.add(var4);
      }

      return var1;
   }

   public static double operation026(LivingEntity var0) {
      return var0 == null ? 0.0 : Math.hypot(var0.getX() - var0.xo, var0.getZ() - var0.zo) * state055;
   }

   public static Vec3 operation027(LivingEntity var0, Vec3 var1, Vec3 var2, float var3) {
      float var4 = Mth.clamp(var3, 0.0F, state027);
      if (var4 <= 0.0F) {
         return var1.add(0.0, state028, 0.0);
      }

      Vec3 var5 = var2;
      Vec3 var6 = var0.getLookAngle();
      float var7 = (float)(var0.getXRot() * state029);
      double var8 = Math.sqrt(var6.x * var6.x + var6.z * var6.z);
      double var10 = var5.horizontalDistance();
      boolean var12 = var0.getDeltaMovement().y <= 0.0;
      double var13 = var12 && var0.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(var0.getGravity(), state030) : var0.getGravity();
      double var15 = Mth.square(Math.cos(var7));
      var5 = var5.add(0.0, var13 * (state031 + var15 * state032), 0.0);
      if (var5.y < 0.0 && var8 > 0.0) {
         double var17 = var5.y * state033 * var15;
         var5 = var5.add(var6.x * var17 / var8, var17, var6.z * var17 / var8);
      }

      if (var7 < 0.0F && var8 > 0.0) {
         double var23 = var10 * -Mth.sin(var7) * state034;
         var5 = var5.add(-var6.x * var23 / var8, var23 * state035, -var6.z * var23 / var8);
      }

      if (var8 > 0.0) {
         var5 = var5.add((var6.x / var8 * var10 - var5.x) * state036, 0.0, (var6.z / var8 * var10 - var5.z) * state037);
      }

      float var24 = Mth.wrapDegrees(var0.getYRot() - var0.yRotO);
      float var18 = Mth.wrapDegrees(var0.getXRot() - var0.xRotO);
      float var19 = 2.0F;
      Vec3 var20 = var0.getLookAngle().multiply(var24 * state038 * var19, var18 * state039 * var19, var24 * state040 * var19);
      Vec3 var21 = var5.multiply(state041, state042, state043).add(var20);
      return var1.add(var21.scale(var4)).add(0.0, state044, 0.0);
   }

   private static float operation028(LivingEntity var0) {
      if (!operation001(var0)) {
         return state088;
      }

      if (var0 instanceof AbstractClientPlayer var1 && state001.getConnection() != null) {
         PlayerInfo var2 = state001.getConnection().getPlayerInfo(var1.getUUID());
         if (var2 != null) {
            return Math.max(1.0F, var2.getLatency() / state089);
         }
      }

      return state090;
   }
}
