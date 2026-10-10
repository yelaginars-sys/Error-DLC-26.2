package dev.syntrix.clienttest.client.combat.meow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;

public final class CombatMath implements MinecraftAccess {
   public static final double state001 = 12.0;
   private static final EquipmentSlot[] state025 = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final Map<BlockState, Float> state026 = new HashMap<>();
   private static final Map<CombatMath.ExposureCacheKey, Float> state027 = new HashMap<>();
   private static boolean state028;
   private static Holder<Enchantment> state029;
   private static Holder<Enchantment> state030;
   private static Holder<Enchantment> state031;
   private static Level state032;
   public static boolean debugEnabled;
   public static String lastScanDiagnostics = "";
   private static final float state035 = 20.0F;
   private static final float state036 = 60.0F;
   private static final float state037 = 1.0E-4F;
   private static final float state038 = 1.0E-4F;
   private static final double state039 = 2.0;
   private static final double state040 = 0.5;
   private static final double state041 = 0.5;
   private static final double state042 = 2.0;
   private static final double state043 = 2.0;
   private static final double state044 = Double.MAX_VALUE;
   private static final double state045 = 2.0;
   private static final double state046 = 2.0;
   private static final double state047 = 2.0;
   private static final float state048 = -1.0F;
   private static final double state049 = 12.0;
   private static final float state050 = -1.0F;
   private static final double state051 = 12.0;
   private static final double state052 = 2.0;
   private static final double state053 = 7.0;
   private static final float state054 = 1.5F;
   private static final float state055 = 4.0F;
   private static final float state056 = 0.2F;
   private static final float state057 = 20.0F;
   private static final float state058 = 25.0F;
   private static final float state059 = 25.0F;
   private static final float state060 = 0.2F;
   private static final double state061 = 2.0;
   private static final double state062 = 2.0;
   private static final double state063 = 2.0;
   private static final double state064 = 2.0;
   private static final double state065 = 2.0;
   private static final double state066 = 1.0E-4;
   private static final double state067 = 1.0E-4;
   private static final double state068 = 1.0E-8;
   private static final double state069 = -1.0E-8;
   private static final float state070 = -Float.MAX_VALUE;
   private static final float state071 = -Float.MAX_VALUE;
   private static final double state072 = 0.08;
   private static final double state073 = 0.5;
   private static final double state074 = 2.0;
   private static final double state075 = 1.0E-4;
   private static final float state076 = -1.0F;
   private static final float state077 = -1.0F;
   private static final double state078 = 0.5;
   private static final double state079 = 0.5;
   private static final double state080 = 3.0;
   private static final float state081 = 3.0F;
   private static final float state082 = 3.0F;
   private static final float state083 = 4.0F;
   private static final float state084 = 0.5F;
   private static final float state085 = 0.5F;
   private static final float state086 = 3.0F;
   private static final float state087 = 6.0F;
   private static final float state088 = 5.0F;
   private static final float state089 = 4.0F;
   private static final double state090 = 0.25;
   private static final double state091 = 0.5;
   private static final double state092 = 0.5;
   private static final double state093 = 0.25;
   private static final double state094 = 0.75;
   private static final double state095 = 3.1;
   private static final float state096 = 60.0F;

   public static CombatMath.ScoredCrystal findBestCrystalBasic(
      List<? extends LivingEntity> var0, Vec3 var1, float var2, float var3, float var4, float var5, List<? extends LivingEntity> var6
   ) {
      return findBestCrystalExcludingIds(var0, var1, var2, var3, var4, var5, var6, Set.of());
   }

   public static float maxCollateralDamage(Vec3 var0, List<? extends LivingEntity> var1) {
      if (var1 != null && !var1.isEmpty()) {
         float var2 = 0.0F;

         for (LivingEntity var4 : var1) {
            if (var4 != null && var4 != mc.player) {
               var2 = Math.max(var2, crystalDamageAtBox(var0, var4, var4.getBoundingBox()));
            }
         }

         return var2;
      } else {
         return 0.0F;
      }
   }

   public static boolean isObsidianPlacementBlocked(BlockPos var0) {
      return mc.level != null && !mc.level.isUnobstructed(Blocks.OBSIDIAN.defaultBlockState(), var0, CollisionContext.empty());
   }

   public static float cachedExposure(Vec3 var0, AABB var1, Entity var2) {
      if (!state028) {
         return computeExposure(var0, var1, var2, null, List.of());
      }

      CombatMath.ExposureCacheKey var3 = new CombatMath.ExposureCacheKey(
         var2.getId(), (long)Math.floor(var0.x), (long)Math.floor(var0.y), (long)Math.floor(var0.z)
      );
      Float var4 = state027.get(var3);
      if (var4 != null) {
         return var4;
      }

      float var5 = computeExposure(var0, var1, var2, null, List.of());
      state027.put(var3, var5);
      return var5;
   }

   public static Vec3 crystalPosition(BlockPos var0) {
      return new Vec3(var0.getX() + state040, var0.getY() + 1.0, var0.getZ() + state041);
   }

   public static boolean canAutoMineLegacy(BlockState var0, boolean var1) {
      return isQuicklyMineable(var0);
   }

   public static float playerDamage(Vec3 var0, Player var1) {
      return var0 != null
            && var1 != null
            && mc.level != null
            && var1.isAlive()
            && !var1.isSpectator()
            && !var1.isCreative()
            && !var1.isInvulnerable()
            && !var1.getAbilities().invulnerable
         ? computeExplosionDamage(var0, var1, var1.getBoundingBox(), state048, state049, true)
         : 0.0F;
   }

   public static boolean isSlowdownBlock(BlockState var0) {
      return var0.is(Blocks.COBWEB) || var0.is(Blocks.SWEET_BERRY_BUSH);
   }

   private static CombatMath.ScoredPlacement scanPlacementCandidates(
      List<CombatMath.PredictedPlayer> var0,
      Vec3 var1,
      float var2,
      float var3,
      float var4,
      float var5,
      boolean var6,
      boolean var7,
      float var8,
      List<? extends LivingEntity> var9,
      BlockPos var10,
      float var11,
      BlockPos var12,
      Set<BlockPos> var13,
      boolean var14,
      boolean var15
   ) {
      AABB var16 = mc.player.getBoundingBox();
      double var17 = var2 * var2;
      double var19 = var3 * var3;
      CombatMath.ScoredPlacement var21 = null;
      float var22 = state071;
      CombatMath.ScanDiagnostics var23 = new CombatMath.ScanDiagnostics();
      ArrayList<BlockPos> var24 = new ArrayList<>(4);

      label314:
      for (CombatMath.PredictedPlayer var26 : var0) {
         Vec3 var27 = var26.feet();
         BlockPos var28 = BlockPos.containing(var27.x, var27.y, var27.z);
         boolean var29 = !var26.player().onGround() && var26.player().getDeltaMovement().y > state072;
         boolean var30 = isSlowdownBlock(mc.level.getBlockState(var28.below()));
         AABB var31 = var26.box();
         AABB var32 = var26.player().getBoundingBox();
         boolean var33 = var15 && intersectsSlowdownBlock(var31) && Math.abs(var28.getY() - mc.player.blockPosition().getY()) <= 1;

         for (int var34 = -3; var34 <= 3; var34++) {
            for (int var35 = -3; var35 <= 3; var35++) {
               for (int var36 = -4; var36 <= 1; var36++) {
                  if (var23.state011 >= 150) {
                     break label314;
                  }

                  boolean var37 = var34 == 0 && var35 == 0;
                  if (!var37 || var36 <= -3 && !var30) {
                     var23.state001++;
                     BlockPos var38 = var28.offset(var34, var36, var35);
                     BlockPos var39 = var38.above();
                     BlockState var40 = mc.level.getBlockState(var38);
                     BlockState var41 = mc.level.getBlockState(var39);
                     boolean var42 = var41.isAir();
                     boolean var43 = var38.getY() + 1.0 > var31.minY + state073 && !var29;
                     if (var42 || var7 && isAllowedDigBlock(var41, var14)) {
                        boolean var45 = false;
                        boolean var44;
                        if (isCrystalBase(var40)) {
                           var44 = false;
                        } else if (var6 && var40.canBeReplaced()) {
                           var44 = true;
                        } else {
                           if (!var6 || !var7 || !isAllowedDigBlock(var40, var14)) {
                              var23.state002++;
                              continue;
                           }

                           var44 = true;
                           var45 = true;
                        }

                        var24.clear();
                        if (var37) {
                           for (int var46 = var28.getY() - 1; var46 > var39.getY(); var46--) {
                              BlockPos var47 = new BlockPos(var28.getX(), var46, var28.getZ());
                              BlockState var48 = mc.level.getBlockState(var47);
                              if (isRegularMineableBlock(var48)) {
                                 var24.add(var47);
                              }
                           }
                        }

                        if (!var42) {
                           var24.add(var39);
                        }

                        int var65 = var24.size() + (var45 ? 1 : 0);
                        if (var65 > 0 == var7) {
                           BlockPos var66 = !var24.isEmpty() ? (BlockPos)var24.get(0) : (var45 ? var38 : null);
                           if (var14 && var24.stream().anyMatch(var0x -> mc.level.getBlockState(var0x).is(Blocks.OBSIDIAN))) {
                              var23.state002++;
                           } else if (var66 != null && var13 != null && var13.contains(var66)) {
                              var23.state009++;
                           } else {
                              CombatMath.Support643 var67 = operation031(var24, var65, var38, var40, var28, var1, var17, var10, var11, var33, !var14);
                              if (var67.state001 || var67.state004 > var67.state005) {
                                 var23.state002++;
                              } else if (var67.state002 || squaredDistanceToBlock(var1, var38) > var17) {
                                 var23.state004++;
                              } else if (isCrystalSpaceOccupied(var39)) {
                                 var23.state005++;
                              } else if (var44 && var65 == 0 && !canPlaceObsidian(var38, var1, var2)) {
                                 var23.state006++;
                              } else {
                                 Vec3 var49 = crystalPosition(var38);
                                 AABB var50 = new AABB(var49.x - 1.0, var49.y, var49.z - 1.0, var49.x + 1.0, var49.y + state074, var49.z + 1.0);
                                 if (squaredDistanceToBox(var1, var50) > var19) {
                                    var23.state004++;
                                 } else {
                                    AABB var51 = var44 ? new AABB(var38).deflate(state075) : null;
                                    boolean var52 = var44 || !var24.isEmpty();
                                    float var53 = var52 ? exposureWithObstacles(var49, var31, var26.player(), var51, var24) : state076;
                                    float var54 = explosionDamageWithPower(var49, var26.player(), var31, var53);
                                    if (var54 < var5) {
                                       var23.state007++;
                                    } else {
                                       if (var52 && var31 != var32) {
                                          float var55 = exposureWithObstacles(var49, var32, var26.player(), var51, var24);
                                          var54 = Math.min(var54, explosionDamageWithPower(var49, var26.player(), var32, var55));
                                          if (var54 < var5) {
                                             var23.state007++;
                                             continue;
                                          }
                                       }

                                       var23.state011++;
                                       float var68 = var52 ? exposureWithObstacles(var49, var16, mc.player, var51, var24) : state077;
                                       float var56 = explosionDamageWithPower(var49, mc.player, var16, var68);
                                       if (!(var56 >= var8) && (!(var56 > var4) || !(var56 >= var54))) {
                                          float var57 = maxCollateralDamage(var49, var9);
                                          if (var57 > var4 && var57 >= var54) {
                                             var23.state008++;
                                          } else {
                                             var23.state010++;
                                             double var58 = var38.getX() + state078 - var27.x;
                                             double var60 = var38.getZ() + state079 - var27.z;
                                             double var62 = Math.sqrt(var58 * var58 + var60 * var60);
                                             float var64 = var54 - var56 * 2.0F - var57 * 2.0F - (float)(var62 * state080);
                                             if (var44) {
                                                var64 -= state081;
                                             }

                                             if (var43) {
                                                var64 -= state082;
                                             }

                                             if (var65 > 0) {
                                                var64 -= state083 + Math.max(0, -var36) * state084 + var67.state004 * state085;
                                                if (var65 > 1) {
                                                   var64 -= state086;
                                                }

                                                if (var67.state003) {
                                                   var64 += state087;
                                                }
                                             }

                                             if (var37) {
                                                var64 += 2.0F;
                                             }

                                             if (var33 && var65 > 0 && isBaseBetweenPoints(var38, var27, mc.player.position())) {
                                                var64 += state088;
                                             }

                                             if (var38.equals(var12)) {
                                                var64 += state089;
                                             }

                                             if (var64 > var22) {
                                                var22 = var64;
                                                var21 = new CombatMath.ScoredPlacement(var38, var49, var44, var66, var54, var56);
                                             }
                                          }
                                       } else {
                                          var23.state008++;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     } else {
                        if (var23.state012.isEmpty()) {
                           var23.state012 = var41.getBlock().getDescriptionId();
                        }

                        var23.state003++;
                     }
                  }
               }
            }
         }
      }

      if (debugEnabled) {
         lastScanDiagnostics = var23.operation001(var21, var22, var7);
      }

      return var21;
   }

   private static boolean intersectsSlowdownBlock(AABB var0) {
      BlockPos var1 = BlockPos.containing(var0.minX, var0.minY, var0.minZ);
      BlockPos var2 = BlockPos.containing(var0.maxX, var0.maxY, var0.maxZ);

      for (BlockPos var4 : BlockPos.betweenClosed(var1, var2)) {
         if (isSlowdownBlock(mc.level.getBlockState(var4))) {
            return true;
         }
      }

      return false;
   }

   public static CombatMath.ScoredPlacement findBestPlacement(
      List<CombatMath.PredictedPlayer> var0,
      Vec3 var1,
      float var2,
      float var3,
      float var4,
      float var5,
      boolean var6,
      boolean var7,
      boolean var8,
      float var9,
      List<? extends LivingEntity> var10,
      BlockPos var11,
      float var12,
      BlockPos var13,
      Set<BlockPos> var14,
      boolean var15
   ) {
      if (mc.player != null && mc.level != null && !var0.isEmpty()) {
         state026.clear();
         state027.clear();
         state028 = true;

         try {
            CombatMath.ScoredPlacement var16 = scanPlacementCandidates(
               var0, var1, var2, var3, var4, var5, var6, false, var9, var10, var11, var12, var13, var14, var8, var15
            );
            return var16 == null && var7
               ? scanPlacementCandidates(var0, var1, var2, var3, var4, var5, var6, true, var9, var10, var11, var12, var13, var14, var8, var15)
               : var16;
         } finally {
            state028 = false;
         }
      } else {
         return null;
      }
   }

   public static boolean canPlaceObsidian(BlockPos var0, Vec3 var1, float var2) {
      return mc.level != null && mc.level.getBlockState(var0).canBeReplaced()
         ? !isObsidianPlacementBlocked(var0) && findPlacementSupport(var0, var1, var2) != null
         : false;
   }

   private static boolean isShearsPreferred(ItemStack var0, int var1) {
      return !var0.isEmpty() && var0.is(Items.SHEARS) && var1 == 0;
   }

   private static double closestExitTime(double var0, double var2, double var4, double var6) {
      double var8;
      if (var2 > state068) {
         var8 = var6 / var2;
      } else {
         if (!(var2 < state069)) {
            return var0;
         }

         var8 = var4 / var2;
      }

      return var8 >= 0.0 && var8 < var0 ? var8 : var0;
   }

   public static boolean isCrystalBaseAt(BlockPos var0) {
      return mc.level != null && isCrystalBase(mc.level.getBlockState(var0));
   }

   public static float crystalDamageAtBox(Vec3 var0, LivingEntity var1, AABB var2) {
      return explosionDamageWithPower(var0, var1, var2, state050);
   }

   private static float computeExplosionDamage(Vec3 var0, LivingEntity var1, AABB var2, float var3, double var4, boolean var6) {
      if (var1 != null && var2 != null && mc.level != null && !var1.isDeadOrDying()) {
         double var7 = feetPosition(var2).distanceTo(var0);
         if (var7 > var4) {
            return 0.0F;
         }

         double var9 = var3 >= 0.0F ? var3 : cachedExposure(var0, var2, var1);
         double var11 = (1.0 - var7 / var4) * var9;
         float var13 = (float)((var11 * var11 + var11) / state052 * state053 * var4 + 1.0);
         if (var6) {
            var13 = switch (mc.level.getDifficulty()) {
               case PEACEFUL -> 0.0F;
               case EASY -> Math.min(var13 / 2.0F + 1.0F, var13);
               case NORMAL -> var13;
               case HARD -> var13 * state054;
               default -> throw new MatchException(null, null);
            };
         }

         var13 = applyArmorReduction(var1, var13);
         var13 = applyEnchantmentReduction(var1, var13);
         var13 = applyResistanceReduction(var1, var13);
         return Math.max(0.0F, var13);
      } else {
         return 0.0F;
      }
   }

   private static Vec3 exitBlockAlongRay(Vec3 var0, Vec3 var1, BlockPos var2) {
      double var3 = Double.POSITIVE_INFINITY;
      var3 = closestExitTime(var3, var1.x, var2.getX() - var0.x, var2.getX() + 1.0 - var0.x);
      var3 = closestExitTime(var3, var1.y, var2.getY() - var0.y, var2.getY() + 1.0 - var0.y);
      var3 = closestExitTime(var3, var1.z, var2.getZ() - var0.z, var2.getZ() + 1.0 - var0.z);
      return !Double.isFinite(var3) ? null : var0.add(var1.scale(var3 + state067));
   }

   public static boolean isQuicklyMineable(BlockState var0) {
      if (isRegularMineableBlock(var0) && mc.level != null) {
         float var1 = var0.getDestroySpeed(mc.level, BlockPos.ZERO);
         return var1 < 0.0F ? false : var1 == 0.0F || breakingTicks(var0) <= state035;
      } else {
         return false;
      }
   }

   private static float applyEnchantmentReduction(LivingEntity var0, float var1) {
      int var2 = 0;

      for (EquipmentSlot var6 : state025) {
         ItemStack var7 = var0.getItemBySlot(var6);
         if (!var7.isEmpty()) {
            var2 += enchantmentLevel(var7, Enchantments.PROTECTION);
            var2 += enchantmentLevel(var7, Enchantments.BLAST_PROTECTION) * 2;
         }
      }

      var2 = Mth.clamp(var2, 0, 20);
      return var1 * (1.0F - var2 / state059);
   }

   private static ItemStack getMiningStack(BlockState var0) {
      int var1 = findBestToolSlot(var0);
      return var1 >= 0 ? mc.player.getInventory().getItem(var1) : mc.player.getMainHandItem();
   }

   private static float applyResistanceReduction(LivingEntity var0, float var1) {
      MobEffectInstance var2 = var0.getEffect(MobEffects.RESISTANCE);
      if (var2 == null) {
         return var1;
      }

      float var3 = Mth.clamp((var2.getAmplifier() + 1) * state060, 0.0F, 1.0F);
      return var1 * (1.0F - var3);
   }

   public static boolean canAutoMine(BlockState var0, boolean var1) {
      return isQuicklyMineable(var0) || var1 && var0.is(Blocks.OBSIDIAN) && breakingTicks(var0) <= state036;
   }

   public static CombatMath.ScoredCrystal findBestCrystalExcludingIds(
      List<? extends LivingEntity> var0, Vec3 var1, float var2, float var3, float var4, float var5, List<? extends LivingEntity> var6, Set<Integer> var7
   ) {
      return findBestCrystal(var0, var1, var2, var3, var4, var5, var6, var7, var0x -> true);
   }

   public static float explosionDamageAtBox(Vec3 var0, LivingEntity var1, AABB var2, float var3, double var4) {
      return computeExplosionDamage(var0, var1, var2, var3, var4, false);
   }

   public static boolean isBreakable(BlockState var0) {
      return !var0.isAir() && !(var0.getBlock() instanceof LiquidBlock) && mc.level != null ? var0.getDestroySpeed(mc.level, BlockPos.ZERO) >= 0.0F : false;
   }

   public static CombatMath.ScoredCrystal findBestCrystal(
      List<? extends LivingEntity> var0,
      Vec3 var1,
      float var2,
      float var3,
      float var4,
      float var5,
      List<? extends LivingEntity> var6,
      Set<Integer> var7,
      Predicate<EndCrystal> var8
   ) {
      if (mc.player != null && mc.level != null) {
         CombatMath.ScoredCrystal var9 = null;
         float var10 = state070;

         for (Entity var12 : mc.level.entitiesForRendering()) {
            if (var12 instanceof EndCrystal var13
               && var13.isAlive()
               && !var7.contains(var13.getId())
               && !(squaredDistanceToBox(var1, var13.getBoundingBox()) > var2 * var2)
               && var8.test(var13)) {
               Vec3 var14 = var13.position();
               float var15 = 0.0F;

               for (LivingEntity var17 : var0) {
                  var15 = Math.max(var15, crystalDamage(var14, var17));
               }

               if (!(var15 < var4)) {
                  float var19 = crystalDamage(var14, mc.player);
                  if (!(var19 >= var5) && (!(var19 > var3) || !(var19 >= var15))) {
                     float var20 = maxCollateralDamage(var14, var6);
                     if (!(var20 > var3) || !(var20 >= var15)) {
                        float var18 = var15 - var19 * 2.0F - var20 * 2.0F;
                        if (var18 > var10) {
                           var10 = var18;
                           var9 = new CombatMath.ScoredCrystal(var13, var15, var19);
                        }
                     }
                  }
               }
            }
         }

         return var9;
      } else {
         return null;
      }
   }

   public static AABB crystalSpawnBox(BlockPos var0) {
      return new AABB(var0.getX(), var0.getY(), var0.getZ(), var0.getX() + 1.0, var0.getY() + state039, var0.getZ() + 1.0);
   }

   private static CombatMath.Support643 operation031(
      List<BlockPos> var0,
      int var1,
      BlockPos var2,
      BlockState var3,
      BlockPos var4,
      Vec3 var5,
      double var6,
      BlockPos var8,
      float var9,
      boolean var10,
      boolean var11
   ) {
      CombatMath.Support643 var12 = new CombatMath.Support643();

      for (int var13 = 0; var13 < var1; var13++) {
         BlockPos var14 = var13 < var0.size() ? (BlockPos)var0.get(var13) : var2;
         BlockState var15 = var13 < var0.size() ? mc.level.getBlockState(var14) : var3;
         boolean var16 = var14.getX() == var4.getX() && var14.getZ() == var4.getZ();
         boolean var17 = var16
            ? var14.getY() < var4.getY() && var14.getY() >= var4.getY() - 4
            : var14.getY() == var4.getY() || var14.getY() == var4.getY() - 1 || var10 && var14.getY() == var4.getY() + 1;
         int var18 = Math.abs(var14.getX() - var4.getX()) + Math.abs(var14.getZ() - var4.getZ());
         int var19 = var10 ? 3 : 2;
         if (!var17 || !var16 && var18 > var19 || !canAutoMine(var15, var11)) {
            var12.state001 = true;
            return var12;
         }

         if (squaredDistanceToBlock(var5, var14) > var6) {
            var12.state002 = true;
            return var12;
         }

         float var20 = breakingTicks(var15);
         if (var13 == 0 && var14.equals(var8)) {
            var20 *= 1.0F - Mth.clamp(var9, 0.0F, 1.0F);
            var12.state003 = true;
         }

         var12.state004 += var20;
         if (var11 && var15.is(Blocks.OBSIDIAN)) {
            var12.state005 = Math.max(var12.state005, state096);
         }
      }

      return var12;
   }

   private static float miningSpeed(ItemStack var0, BlockState var1) {
      if (var0 == mc.player.getMainHandItem()) {
         return mc.player.getDestroySpeed(var1);
      }

      MobEffectInstance var2 = mc.player.getEffect(MobEffects.MINING_FATIGUE);
      return MiningMath.operation003(
         stackMiningSpeed(var0, var1),
         MobEffectUtil.hasDigSpeed(mc.player) ? MobEffectUtil.getDigSpeedAmplification(mc.player) : -1,
         var2 == null ? -1 : var2.getAmplifier(),
         (float)mc.player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED),
         mc.player.isEyeInFluid(FluidTags.WATER) ? (float)mc.player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED) : 1.0F,
         mc.player.onGround()
      );
   }

   public static int findBestToolSlot(BlockState var0) {
      if (mc.player == null) {
         return -1;
      }

      int var1 = -1;
      ItemStack var2 = ItemStack.EMPTY;
      float var3 = stackMiningSpeed(var2, var0);

      for (int var4 = 0; var4 < 9; var4++) {
         ItemStack var5 = mc.player.getInventory().getItem(var4);
         float var6 = stackMiningSpeed(var5, var0);
         if (isBetterMiningStack(var5, var2, var6, var3, var0)) {
            var3 = var6;
            var1 = var4;
            var2 = var5;
         }
      }

      return var1;
   }

   public static float crystalDamage(Vec3 var0, LivingEntity var1) {
      return var1 == null ? 0.0F : crystalDamageAtBox(var0, var1, var1.getBoundingBox());
   }

   private CombatMath() {
   }

   private static int enchantmentLevel(ItemStack var0, ResourceKey<Enchantment> var1) {
      if (!var0.isEmpty() && mc.level != null) {
         if (state032 != mc.level) {
            Registry var2 = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            state029 = (Holder<Enchantment>)var2.get(Enchantments.PROTECTION).orElse(null);
            state030 = (Holder<Enchantment>)var2.get(Enchantments.BLAST_PROTECTION).orElse(null);
            state031 = (Holder<Enchantment>)var2.get(Enchantments.EFFICIENCY).orElse(null);
            state032 = mc.level;
         }

         Holder var3 = null;
         if (var1 == Enchantments.PROTECTION) {
            var3 = state029;
         } else if (var1 == Enchantments.BLAST_PROTECTION) {
            var3 = state030;
         } else if (var1 == Enchantments.EFFICIENCY) {
            var3 = state031;
         }

         return var3 == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(var3, var0);
      } else {
         return 0;
      }
   }

   public static CombatMath.PlacementSupport findPlacementSupport(BlockPos var0, Vec3 var1, float var2) {
      if (mc.level == null) {
         return null;
      }

      CombatMath.PlacementSupport var3 = null;
      double var4 = state044;

      for (Direction var9 : Direction.values()) {
         BlockPos var10 = var0.relative(var9.getOpposite());
         BlockState var11 = mc.level.getBlockState(var10);
         if (!var11.canBeReplaced() && var11.isFaceSturdy(mc.level, var10, var9)) {
            Vec3 var12 = blockFacePoint(var11, var10, var9);
            Vec3 var13 = new Vec3(var9.getStepX(), var9.getStepY(), var9.getStepZ());
            if (!(var1.subtract(var12).dot(var13) <= 0.0)) {
               double var14 = var1.distanceToSqr(var12);
               if (!(var14 > var2 * var2) && !(var14 >= var4)) {
                  var4 = var14;
                  var3 = new CombatMath.PlacementSupport(var10, var9, var12);
               }
            }
         }
      }

      return var3;
   }

   private static boolean isBaseBetweenPoints(BlockPos var0, Vec3 var1, Vec3 var2) {
      double var3 = var1.x - var2.x;
      double var5 = var1.z - var2.z;
      double var7 = var3 * var3 + var5 * var5;
      if (var7 < state090) {
         return false;
      }

      double var9 = var0.getX() + state091 - var2.x;
      double var11 = var0.getZ() + state092 - var2.z;
      double var13 = var9 * var3 + var11 * var5;
      double var15 = var9 * var9 + var11 * var11;
      return var13 > var7 + state093 && var15 > var7 + state094 && var15 < var7 + state095;
   }

   private static float applyArmorReduction(LivingEntity var0, float var1) {
      float var2 = var0.getArmorValue();
      if (var2 <= 0.0F) {
         return var1;
      }

      float var3 = (float)var0.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float var4 = 2.0F + var3 / state055;
      float var5 = Mth.clamp(var2 - var1 / var4, var2 * state056, state057);
      return var1 * (1.0F - var5 / state058);
   }

   private static boolean isExplosionRayBlocked(Vec3 var0, Vec3 var1, Entity var2, AABB var3, List<BlockPos> var4) {
      if (mc.level == null) {
         return false;
      }

      if (var3 != null && var3.clip(var0, var1).isPresent()) {
         return false;
      }

      Vec3 var5 = var0;

      for (int var6 = 0; var6 <= var4.size(); var6++) {
         BlockHitResult var7 = mc.level.clip(new ClipContext(var5, var1, Block.COLLIDER, Fluid.NONE, var2));
         if (var7.getType() == Type.MISS) {
            return true;
         }

         if (!var4.contains(var7.getBlockPos())) {
            return false;
         }

         Vec3 var8 = var1.subtract(var7.getLocation());
         double var9 = var8.length();
         if (var9 < state066) {
            return true;
         }

         Vec3 var11 = var8.scale(1.0 / var9);
         Vec3 var12 = exitBlockAlongRay(var7.getLocation(), var11, var7.getBlockPos());
         if (var12 == null) {
            return false;
         }

         if (var12.distanceToSqr(var7.getLocation()) >= var8.lengthSqr()) {
            return true;
         }

         var5 = var12;
      }

      return false;
   }

   private static boolean isBetterMiningStack(ItemStack var0, ItemStack var1, float var2, float var3, BlockState var4) {
      if (var2 > var3 + state037) {
         return true;
      }

      if (var2 + state038 < var3) {
         return false;
      }

      boolean var5 = var0.isCorrectToolForDrops(var4);
      boolean var6 = !var1.isEmpty() && var1.isCorrectToolForDrops(var4);
      if (var5 != var6) {
         return var5;
      }

      int var7 = enchantmentLevel(var0, Enchantments.EFFICIENCY);
      int var8 = enchantmentLevel(var1, Enchantments.EFFICIENCY);
      if (var7 != var8) {
         return var7 > var8;
      }

      boolean var9 = isShearsPreferred(var0, var7);
      boolean var10 = isShearsPreferred(var1, var8);
      return var9 != var10 && !var9;
   }

   private static Vec3 blockFacePoint(BlockState var0, BlockPos var1, Direction var2) {
      VoxelShape var3 = var0.getCollisionShape(mc.level, var1);
      AABB var4 = var3.isEmpty() ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : var3.bounds();
      double var5 = var1.getX() + (var4.minX + var4.maxX) / state045;
      double var7 = var1.getY() + (var4.minY + var4.maxY) / state046;
      double var9 = var1.getZ() + (var4.minZ + var4.maxZ) / state047;
      switch (var2) {
         case UP:
            var7 = var1.getY() + var4.maxY;
            break;
         case DOWN:
            var7 = var1.getY() + var4.minY;
            break;
         case NORTH:
            var9 = var1.getZ() + var4.minZ;
            break;
         case SOUTH:
            var9 = var1.getZ() + var4.maxZ;
            break;
         case WEST:
            var5 = var1.getX() + var4.minX;
            break;
         case EAST:
            var5 = var1.getX() + var4.maxX;
      }

      return new Vec3(var5, var7, var9);
   }

   public static double squaredDistanceToBlock(Vec3 var0, BlockPos var1) {
      return squaredDistanceToBox(var0, new AABB(var1.getX(), var1.getY(), var1.getZ(), var1.getX() + 1.0, var1.getY() + 1.0, var1.getZ() + 1.0));
   }

   private static float stackMiningSpeed(ItemStack var0, BlockState var1) {
      if (var0.isEmpty()) {
         return 1.0F;
      }

      float var2 = var0.getDestroySpeed(var1);
      int var3 = enchantmentLevel(var0, Enchantments.EFFICIENCY);
      if (var2 > 1.0F && var3 > 0) {
         var2 += var3 * var3 + 1.0F;
      }

      return var2;
   }

   public static boolean isCrystalBase(BlockState var0) {
      return var0.is(Blocks.OBSIDIAN) || var0.is(Blocks.BEDROCK);
   }

   public static CombatMath.ScoredPlacement findBestPlacementBasic(
      List<CombatMath.PredictedPlayer> var0,
      Vec3 var1,
      float var2,
      float var3,
      float var4,
      float var5,
      boolean var6,
      boolean var7,
      float var8,
      List<? extends LivingEntity> var9,
      BlockPos var10,
      float var11,
      BlockPos var12,
      Set<BlockPos> var13
   ) {
      return findBestPlacement(var0, var1, var2, var3, var4, var5, var6, var7, false, var8, var9, var10, var11, var12, var13, false);
   }

   public static float breakingTicksAt(BlockPos var0) {
      return mc.level == null ? 0.0F : breakingTicks(mc.level.getBlockState(var0));
   }

   public static double squaredDistanceToBox(Vec3 var0, AABB var1) {
      return var0.distanceToSqr(Mth.clamp(var0.x, var1.minX, var1.maxX), Mth.clamp(var0.y, var1.minY, var1.maxY), Mth.clamp(var0.z, var1.minZ, var1.maxZ));
   }

   public static float breakingTicks(BlockState var0) {
      Float var1 = state028 ? state026.get(var0) : null;
      if (var1 != null) {
         return var1;
      }

      float var2 = 0.0F;
      if (mc.level != null && mc.player != null) {
         float var3 = var0.getDestroySpeed(mc.level, BlockPos.ZERO);
         ItemStack var4 = getMiningStack(var0);
         var2 = MiningMath.operation001(var3, var0.requiresCorrectToolForDrops(), var4.isCorrectToolForDrops(var0), miningSpeed(var4, var0));
      }

      if (state028) {
         state026.put(var0, var2);
      }

      return var2;
   }

   public static Vec3 feetPosition(AABB var0) {
      return new Vec3((var0.minX + var0.maxX) / state042, var0.minY, (var0.minZ + var0.maxZ) / state043);
   }

   public static boolean isCrystalSpaceOccupied(BlockPos var0) {
      return mc.level == null ? false : !mc.level.getEntities((Entity)null, crystalSpawnBox(var0), var0x -> var0x.isAlive() && !var0x.isSpectator()).isEmpty();
   }

   public static float explosionDamageWithPower(Vec3 var0, LivingEntity var1, AABB var2, float var3) {
      return explosionDamageAtBox(var0, var1, var2, var3, state051);
   }

   public static boolean isCrystalSpaceClear(BlockPos var0) {
      return mc.level != null && mc.level.getBlockState(var0).isAir() && !isCrystalSpaceOccupied(var0);
   }

   public static float exposureWithObstacles(Vec3 var0, AABB var1, Entity var2, AABB var3, List<BlockPos> var4) {
      return computeExposure(var0, var1, var2, var3, var4);
   }

   private static boolean isAllowedDigBlock(BlockState var0, boolean var1) {
      return isRegularMineableBlock(var0) && (!var1 || !var0.is(Blocks.OBSIDIAN));
   }

   private static float computeExposure(Vec3 var0, AABB var1, Entity var2, AABB var3, List<BlockPos> var4) {
      double var5 = 1.0 / ((var1.maxX - var1.minX) * state061 + 1.0);
      double var7 = 1.0 / ((var1.maxY - var1.minY) * state062 + 1.0);
      double var9 = 1.0 / ((var1.maxZ - var1.minZ) * state063 + 1.0);
      if (!(var5 < 0.0) && !(var7 < 0.0) && !(var9 < 0.0)) {
         double var11 = (1.0 - Math.floor(1.0 / var5) * var5) / state064;
         double var13 = (1.0 - Math.floor(1.0 / var9) * var9) / state065;
         int var15 = 0;
         int var16 = 0;

         for (double var17 = 0.0; var17 <= 1.0; var17 += var5) {
            for (double var19 = 0.0; var19 <= 1.0; var19 += var7) {
               for (double var21 = 0.0; var21 <= 1.0; var21 += var9) {
                  Vec3 var23 = new Vec3(
                     Mth.lerp(var17, var1.minX, var1.maxX) + var11, Mth.lerp(var19, var1.minY, var1.maxY), Mth.lerp(var21, var1.minZ, var1.maxZ) + var13
                  );
                  if (isExplosionRayBlocked(var23, var0, var2, var3, var4)) {
                     var15++;
                  }

                  var16++;
               }
            }
         }

         return var16 == 0 ? 0.0F : (float)var15 / var16;
      } else {
         return 0.0F;
      }
   }

   public static float maxCollateralDamageWithObstacles(Vec3 var0, List<? extends LivingEntity> var1, AABB var2, List<BlockPos> var3) {
      if (var1 != null && !var1.isEmpty()) {
         List var4 = var3 == null ? List.of() : var3;
         float var5 = 0.0F;

         for (LivingEntity var7 : var1) {
            if (var7 != null && var7 != mc.player) {
               AABB var8 = var7.getBoundingBox();
               float var9 = exposureWithObstacles(var0, var8, var7, var2, var4);
               var5 = Math.max(var5, explosionDamageWithPower(var0, var7, var8, var9));
            }
         }

         return var5;
      } else {
         return 0.0F;
      }
   }

   public static boolean isRegularMineableBlock(BlockState var0) {
      return isBreakable(var0) && !isSlowdownBlock(var0);
   }

   private record ExposureCacheKey(int entityId, long x, long y, long z) {
   }

   public record PlacementSupport(BlockPos against, Direction side, Vec3 hitVec) {
   }

   public record PredictedPlayer(Player player, AABB box, Vec3 feet) {
      public PredictedPlayer(Player var1, AABB var2) {
         this(var1, var2, CombatMath.feetPosition(var2));
      }
   }

   private static final class ScanDiagnostics {
      private int state001;
      private int state002;
      private int state003;
      private int state004;
      private int state005;
      private int state006;
      private int state007;
      private int state008;
      private int state009;
      private int state010;
      private int state011;
      private String state012 = "";
      private static final String TEXT_BEST_NULL_1 = new String(" BEST=null");
      private static final String TEXT__2 = new String("-");
      private static final String TEXT_S_S_0FT_3 = new String("%s[%s %.0ft]");
      private static final String TEXT_BEST_S_OBBY_B_DIG_S_SCORE_1F_EDMG_1F_SDMG_1F_4 = new String(" BEST=%s obby=%b dig=%s score=%.1f eDmg=%.1f sDmg=%.1f");
      private static final String TEXT_SCAN_D_NOBASE_D_CBLOCKED_D_REACH_D_ENT_D_FACE_D_LOWDMG__5 = new String(
         "scan=%d noBase=%d cBlocked=%d reach=%d ent=%d face=%d lowDmg=%d self=%d black=%d ok=%d digOn=%b blk=%s%s"
      );

      private String operation001(CombatMath.ScoredPlacement var1, float var2, boolean var3) {
         String var4;
         if (var1 == null) {
            var4 = TEXT_BEST_NULL_1;
         } else {
            String var5 = var1.digPos() == null
               ? TEXT__2
               : String.format(
                  TEXT_S_S_0FT_3,
                  var1.digPos().toShortString(),
                  MinecraftAccess.mc.level.getBlockState(var1.digPos()).getBlock().getDescriptionId(),
                  CombatMath.breakingTicksAt(var1.digPos())
               );
            var4 = String.format(
               TEXT_BEST_S_OBBY_B_DIG_S_SCORE_1F_EDMG_1F_SDMG_1F_4,
               var1.pos().toShortString(),
               var1.needsObsidian(),
               var5,
               var2,
               var1.targetDamage(),
               var1.selfDamage()
            );
         }

         return String.format(
            TEXT_SCAN_D_NOBASE_D_CBLOCKED_D_REACH_D_ENT_D_FACE_D_LOWDMG__5,
            this.state001,
            this.state002,
            this.state003,
            this.state004,
            this.state005,
            this.state006,
            this.state007,
            this.state008,
            this.state009,
            this.state010,
            var3,
            this.state012,
            var4
         );
      }
   }

   public record ScoredCrystal(EndCrystal crystal, float targetDamage, float selfDamage) {
   }

   public record ScoredPlacement(BlockPos pos, Vec3 crystalPos, boolean needsObsidian, BlockPos digPos, float targetDamage, float selfDamage) {
      public ScoredPlacement(BlockPos pos, Vec3 crystalPos, boolean needsObsidian, BlockPos digPos, float targetDamage, float selfDamage) {
         pos = pos.immutable();
         digPos = digPos == null ? null : digPos.immutable();
         this.pos = pos;
         this.crystalPos = crystalPos;
         this.needsObsidian = needsObsidian;
         this.digPos = digPos;
         this.targetDamage = targetDamage;
         this.selfDamage = selfDamage;
      }
   }

   private static final class Support643 {
      private boolean state001;
      private boolean state002;
      private boolean state003;
      private float state004;
      private float state005;
      private static final float state006 = 30.0F;

      private Support643() {
         this.state005 = state006;
      }
   }
}
