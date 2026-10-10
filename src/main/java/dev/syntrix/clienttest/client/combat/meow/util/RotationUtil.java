package dev.syntrix.clienttest.client.combat.meow.util;

import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;

public final class RotationUtil implements MinecraftAccess {
   private static final double state001 = 180.0;
   private static final double state002 = Math.PI;
   private static final float state003 = 90.0F;
   private static final double state004 = 180.0;
   private static final double state005 = Math.PI;
   private static final double state006 = 1.5;
   private static final double state007 = 0.05F;
   private static final double state008 = 0.1F;
   private static final float state009 = 1.2F;
   private static final double state010 = 180.0 / Math.PI;
   private static final double state011 = 90.0;
   private static final float state012 = 180.0F;
   private static final float state013 = -90.0F;
   private static final float state014 = 90.0F;
   private static final double state015 = 0.6;
   private static final double state016 = 0.2;
   private static final double state017 = 3.0;
   private static final double state018 = 1.2F;
   private static final float state019 = (float) (Math.PI / 180.0);
   private static final float state020 = (float) Math.PI;
   private static final float state021 = (float) (Math.PI / 180.0);
   private static final String TEXT_THIS_IS_A_UTILITY_CLASS_AND_CANNOT_BE_INSTANTIATED_1 = new String("This is a utility class and cannot be instantiated");

   public static float operation001(Vector2d var0) {
      if (mc.player == null) {
         return 0.0F;
      }

      double var1 = var0.x - mc.player.position().x;
      double var3 = var0.y - mc.player.position().z;
      return (float)(-(Math.atan2(var1, var3) * state010));
   }

   public static boolean operation002(Vec3 var0, double var1, AABB var3) {
      if (mc.player == null) {
         return false;
      }

      Vec3 var4 = mc.player.getEyePosition();
      return var3.contains(var4) || var3.clip(var4, var4.add(var0.scale(var1))).isPresent();
   }

   public static boolean operation003(float var0, float var1, float var2, AABB var3) {
      if (mc.player == null) {
         return false;
      }

      Vec3 var4 = mc.player.getEyePosition();
      Rotation var5 = new Rotation(var0, var1);
      Vec3 var6 = var5.toDirection();
      Vec3 var7 = var4.add(var6.scale(var2));
      return var3.contains(var4) || var3.clip(var4, var7).isPresent();
   }

   private RotationUtil() {
      throw new UnsupportedOperationException(TEXT_THIS_IS_A_UTILITY_CLASS_AND_CANNOT_BE_INSTANTIATED_1);
   }

   public static Rotation operation005(float var0, float var1) {
      if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
         var0 -= state012;
         var1 = -var1;
      }

      return new Rotation(Mth.wrapDegrees(var0), Mth.clamp(var1, state013, state014));
   }

   public static Vector2d operation006(Vec3 var0) {
      double var1 = var0.x - mc.player.getX();
      double var3 = var0.y - (mc.player.getY() + mc.player.getEyeHeight());
      double var5 = var0.z - mc.player.getZ();
      double var7 = Mth.sqrt((float)(var1 * var1 + var5 * var5));
      float var9 = (float)(Math.atan2(var5, var1) * state001 / state002) - state003;
      float var10 = (float)(-(Math.atan2(var3, var7) * state004 / state005));
      float var11 = (float)(Math.pow((Double)mc.options.sensitivity().get(), state006) * state007 + state008);
      float var12 = var11 * var11 * var11 * state009;
      var9 -= var9 % var12;
      var10 -= var10 % (var12 * var11);
      return new Vector2d(var9, var10);
   }

   public static Vec3 operation007(float var0, float var1) {
      float var2 = -var1 * state019 - state020;
      float var3 = -var0 * state021;
      float var4 = Mth.cos(var2);
      float var5 = Mth.sin(var2);
      float var6 = -Mth.cos(var3);
      float var7 = Mth.sin(var3);
      return new Vec3(var5 * var6, var7, var4 * var6);
   }

   public static Rotation fromDirection(Vec3 var0) {
      return new Rotation(
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(var0.z, var0.x)) - state011),
         (float)Mth.wrapDegrees(Math.toDegrees(-Math.atan2(var0.y, Math.hypot(var0.x, var0.z))))
      );
   }

   public static Vec2 operation009(Vec2 var0, Vec2 var1) {
      double var2 = (Double)mc.options.sensitivity().get();
      float var4 = (float)(Math.pow(var2 * state015 + state016, state017) * state018);
      float var5 = var0.x - var1.x;
      float var6 = var0.y - var1.y;
      var5 -= var5 % var4;
      var6 -= var6 % var4;
      return new Vec2(var1.x + var5, var1.y + var6);
   }
}
