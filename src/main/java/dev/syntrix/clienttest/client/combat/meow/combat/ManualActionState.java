package dev.syntrix.clienttest.client.combat.meow.combat;

public final class ManualActionState {
   private static boolean usePending;
   private static boolean entityPending;
   private static boolean digPending;
   private static Object playerIdentity;
   private static Object worldIdentity;

   public static void reset() {
      usePending = false;
      entityPending = false;
      digPending = false;
   }

   public static boolean hasPendingAction() {
      return usePending || entityPending || digPending;
   }

   public static void markUse() {
      usePending = true;
   }

   public static boolean canAttackEntity() {
      return !usePending && !entityPending;
   }

   public static boolean refreshIdentity(Object var0, Object var1) {
      if (playerIdentity == var0 && worldIdentity == var1) {
         return false;
      }

      playerIdentity = var0;
      worldIdentity = var1;
      reset();
      return true;
   }

   public static boolean canDig() {
      return !usePending && !digPending;
   }

   public static boolean isUsePending() {
      return usePending;
   }

   private ManualActionState() {
   }

   public static String describePendingTypes() {
      return "use=" + usePending + ",entity=" + entityPending + ",dig=" + digPending;
   }

   public static void markDig() {
      digPending = true;
   }

   public static void markEntityInteraction() {
      entityPending = true;
   }
}
