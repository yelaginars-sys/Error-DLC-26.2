package dev.syntrix.clienttest.client.combat.meow.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;

public final class PlacementCooldowns implements MinecraftAccess {
   public static final int state001 = 4;
   private static final Map<Long, Integer> state003 = new HashMap<>();
   private static int state004 = PlacementCooldowns.state009;
   private static Object state005;
   private static final int state006 = Integer.MIN_VALUE;
   private static final int state007 = Integer.MIN_VALUE;
   private static final int state008 = Integer.MIN_VALUE;
   private static final int state009 = Integer.MIN_VALUE;

   public static void markPositionForTicks(BlockPos var0, int var1) {
      refreshWorld();
      if (var0 != null && mc.player != null) {
         int var2 = mc.player.tickCount;
         long var3 = var0.asLong();
         if (state003.size() > 64) {
            purgeExpired(var2);
         }

         state003.put(var3, var2 + var1);
      }
   }

   private PlacementCooldowns() {
   }

   private static void purgeExpired(int var0) {
      state003.values().removeIf(var1 -> var0 > var1);
   }

   public static boolean wasActionRecent(int var0) {
      refreshWorld();
      if (var0 >= 0 && mc.player != null && state004 != state006) {
         int var1 = mc.player.tickCount - state004;
         return var1 >= 0 && var1 <= var0;
      } else {
         return false;
      }
   }

   public static int getTrackedPositionCount() {
      refreshWorld();
      if (mc.player != null) {
         purgeExpired(mc.player.tickCount);
      }

      return state003.size();
   }

   public static void reset() {
      state003.clear();
      state004 = state007;
      state005 = mc.level;
   }

   private static void refreshWorld() {
      ClientLevel var0 = mc.level;
      if (state005 != var0) {
         state003.clear();
         state004 = state008;
         state005 = var0;
      }
   }

   public static void markPosition(BlockPos var0) {
      markPositionForTicks(var0, 4);
   }

   public static void markAction() {
      refreshWorld();
      if (mc.player != null) {
         state004 = mc.player.tickCount;
      }
   }

   public static boolean isCoolingDown(BlockPos var0) {
      refreshWorld();
      if (var0 != null && mc.player != null) {
         long var1 = var0.asLong();
         Integer var3 = state003.get(var1);
         if (var3 == null) {
            return false;
         } else if (mc.player.tickCount > var3) {
            state003.remove(var1);
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }
}
