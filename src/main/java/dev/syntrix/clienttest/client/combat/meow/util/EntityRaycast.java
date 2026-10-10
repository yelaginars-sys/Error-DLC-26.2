package dev.syntrix.clienttest.client.combat.meow.util;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;

public final class EntityRaycast implements MinecraftAccess {
   private static final float state001 = (float) (Math.PI / 180.0);
   private static final float state002 = (float) Math.PI;
   private static final float state003 = (float) (Math.PI / 180.0);
   private static final double state004 = Math.PI / 180.0;
   private static final double state005 = Math.PI / 180.0;

   public static boolean operation001(float var0, float var1, double var2, Entity var4) {
      return operation002(var0, var1, var2, var4, false);
   }

   public static boolean operation002(float var0, float var1, double var2, Entity var4, boolean var5) {
      if (mc.player == null) {
         return false;
      }

      float var6 = var5 ? 1.0F : mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
      Vec3 var7 = mc.player.getEyePosition(var6);
      return operation010(var0, var1, var2, var7, var4.getBoundingBox(), 0.0, false);
   }

   public static EntityHitResult operation003(Entity var0, Vec3 var1, Vec3 var2, AABB var3, Predicate<Entity> var4, double var5) {
      if (mc.level == null) {
         return null;
      }

      double var7 = var5;
      Entity var9 = null;
      Vec3 var10 = null;

      for (Entity var12 : mc.level.getEntities(var0, var3, var4)) {
         AABB var13 = var12.getBoundingBox().inflate(var12.getPickRadius());
         Optional<Vec3> var14 = var13.clip(var1, var2);
         if (var13.contains(var1) || var14.isPresent()) {
            double var15 = var14.<Double>map(var1::distanceToSqr).orElse(0.0);
            var15 = Math.sqrt(var15);
            if ((var15 < var7 || var7 == 0.0) && var12.getRootVehicle() != var0.getRootVehicle()) {
               var9 = var12;
               var10 = var14.orElse(var1);
               var7 = var15;
            }
         }
      }

      return var9 == null ? null : new EntityHitResult(var9, var10);
   }

   public static HitResult operation004(double var0, float var2, float var3, Entity var4, boolean var5) {
      if (mc.player != null && mc.level != null) {
         float var6 = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 var7 = mc.player.getEyePosition(var6);
         Vec3 var8 = operation005(var3, var2);
         Vec3 var9 = var7.add(var8.scale(var0));
         HitResult var10 = operation011(var7, var9, Block.COLLIDER, Fluid.NONE);
         double var11 = var10.getLocation().distanceToSqr(var7);
         AABB var13 = var4.getBoundingBox().expandTowards(var8.scale(var0)).inflate(1.0);
         EntityHitResult var14 = ProjectileUtil.getEntityHitResult(
            var4, var7, var9, var13, var0x -> !var0x.isSpectator() && var0x.isAlive() && var0x.isPickable(), var0 * var0
         );
         if (var14 == null) {
            return var10;
         } else {
            return (HitResult)(!var5 && !(var14.getLocation().distanceToSqr(var7) < var11) ? var10 : var14);
         }
      } else {
         return null;
      }
   }

   public static Vec3 operation005(float var0, float var1) {
      float var2 = -var1 * state001 - state002;
      float var3 = -var0 * state003;
      float var4 = (float)Math.cos(var2);
      float var5 = (float)Math.sin(var2);
      float var6 = -((float)Math.cos(var3));
      float var7 = (float)Math.sin(var3);
      return new Vec3(var5 * var6, var7, var4 * var6);
   }

   private EntityRaycast() {
   }

   public static boolean canHitEntity(float var0, float var1, double var2, Entity var4, boolean var5) {
      if (mc.player != null && mc.level != null && var4 != null) {
         float var6 = var5 ? 1.0F : mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 var7 = mc.player.getEyePosition(var6);
         return operation010(var0, var1, var2, var7, var4.getBoundingBox(), 0.0, true);
      } else {
         return false;
      }
   }

   public static boolean operation008(float var0, float var1, double var2, Entity var4) {
      if (mc.player != null && var4 != null) {
         Vec3 var5 = mc.player.getEyePosition(1.0F);
         return operation010(var0, var1, var2, var5, var4.getBoundingBox(), var4.getPickRadius(), false);
      } else {
         return false;
      }
   }

   public static boolean operation010(float var0, float var1, double var2, Vec3 var4, AABB var5, double var6, boolean var8) {
      if (var4 != null && var5 != null && !(var2 < 0.0)) {
         Vec3 var9 = operation005(var1, var0);
         Vec3 var10 = var4.add(var9.scale(var2));
         if (var8 && mc.level != null && mc.player != null) {
            HitResult var11 = operation011(var4, var10, Block.COLLIDER, Fluid.NONE);
            if (var11 != null && var11.getType() != Type.MISS) {
               var10 = var11.getLocation();
            }
         }

         AABB var12 = var6 > 0.0 ? var5.inflate(var6) : var5;
         return var12.contains(var4) || var12.clip(var4, var10).isPresent();
      } else {
         return false;
      }
   }

   public static HitResult operation011(Vec3 var0, Vec3 var1, Block var2, Fluid var3) {
      return mc.level != null && mc.player != null ? mc.level.clip(new ClipContext(var0, var1, var2, var3, mc.player)) : null;
   }

   public static Vec3 operation012(float var0, float var1) {
      float var2 = (float)(var1 * state004);
      float var3 = (float)(-var0 * state005);
      float var4 = (float)Math.cos(var3);
      float var5 = (float)Math.sin(var3);
      float var6 = (float)Math.cos(var2);
      float var7 = (float)Math.sin(var2);
      return new Vec3(var5 * var6, -var7, var4 * var6);
   }
}
