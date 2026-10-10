package dev.syntrix.clienttest.client.combat.meow.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Rotation {
   private final float yaw;
   private final float pitch;
   private final boolean quantized;
   public static final Rotation ZERO = new Rotation(0.0F, 0.0F);
   private static final float state005 = 180.0F;
   private static final float state006 = (float) (Math.PI / 180.0);
   private static final float state007 = (float) (Math.PI / 180.0);
   private static final double state008 = 0.6F;
   private static final double state009 = 0.2F;
   private static final double state010 = 8.0;
   private static final double state011 = 0.15F;

   public Rotation(float var1, float var2, boolean var3) {
      this.yaw = var1;
      this.pitch = var2;
      this.quantized = var3;
   }

   public float getPitch() {
      return this.pitch;
   }

   public final Vec3 toDirection() {
      float var1 = this.pitch * state006;
      float var2 = -this.yaw * state007;
      float var3 = Mth.cos(var2);
      float var4 = Mth.sin(var2);
      float var5 = Mth.cos(var1);
      float var6 = Mth.sin(var1);
      return new Vec3(var4 * var5, -var6, var3 * var5);
   }

   public boolean getQuantized() {
      return this.quantized;
   }

   public boolean isInvalid() {
      return Float.isInfinite(this.yaw) || Float.isNaN(this.yaw) || Float.isInfinite(this.pitch) || Float.isNaN(this.pitch);
   }

   public Rotation(float var1, float var2) {
      this(var1, var2, false);
   }

   public float angleTo(Rotation var1) {
      return Math.min(this.deltaTo(var1).length(), state005);
   }

   public static float getMouseStep() {
      double var0 = (Double)Minecraft.getInstance().options.sensitivity().get() * state008 + state009;
      return (float)(var0 * var0 * var0 * state010 * state011);
   }

   public Vec3 toPolarDirection() {
      return Vec3.directionFromRotation(this.pitch, this.yaw);
   }

   public Rotation quantizeAgainst(Rotation var1) {
      if (!this.quantized && !this.equals(var1)) {
         RotationDelta var2 = var1.deltaTo(this);
         double var3 = getMouseStep();
         int var5 = (int)(var2.deltaYaw() / var3);
         int var6 = (int)(var2.deltaPitch() / var3);
         return new Rotation((float)(var1.getYaw() + var5 * var3), (float)(var1.getPitch() + var6 * var3), true);
      } else {
         return this;
      }
   }

   public float getYaw() {
      return this.yaw;
   }

   public RotationDelta deltaTo(Rotation var1) {
      return new RotationDelta(this.wrappedDifference(var1.yaw, this.yaw), this.wrappedDifference(var1.pitch, this.pitch));
   }

   public float angularDistance(Rotation var1) {
      float var2 = Mth.wrapDegrees(var1.getYaw() - this.yaw);
      float var3 = var1.getPitch() - this.pitch;
      return (float)Math.hypot(Math.abs(var2), Math.abs(var3));
   }

   public boolean isWithinAngle(Rotation var1, float var2) {
      return this.angleTo(var1) <= var2;
   }

   public Rotation addDelta(RotationDelta var1) {
      return new Rotation(this.yaw + var1.deltaYaw(), this.pitch + var1.deltaPitch());
   }

   public Rotation stepToward(Rotation var1, float var2, float var3) {
      RotationDelta var4 = this.deltaTo(var1);
      float var5 = var4.length();
      float var6 = Math.abs(var4.deltaYaw() / var5) * var2;
      float var7 = Math.abs(var4.deltaPitch() / var5) * var3;
      float var8 = Mth.clamp(var4.deltaYaw(), -var6, var6);
      float var9 = Mth.clamp(var4.deltaPitch(), -var7, var7);
      return new Rotation(this.yaw + var8, this.pitch + var9);
   }

   private float wrappedDifference(float var1, float var2) {
      return Mth.wrapDegrees(var1 - var2);
   }
}
