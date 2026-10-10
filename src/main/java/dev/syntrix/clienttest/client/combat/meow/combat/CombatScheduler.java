package dev.syntrix.clienttest.client.combat.meow.combat;

public final class CombatScheduler {
   public static final int state001 = 0;
   private static final String TEXT_NONE_1 = new String("none");
   private static final String TEXT_NONE_2 = new String("none");
   private static final String TEXT_WEB_3 = new String("web");
   private static final String TEXT_CRYSTAL_4 = new String("crystal");

   private static CombatScheduler.ActionParticipant chooseParticipant(
      CombatScheduler.ActionParticipant var0, CombatScheduler.ActionParticipant var1, int var2, int var3
   ) {
      if (var2 == 0 && var3 == 0) {
         return null;
      } else {
         return var2 >= var3 ? var0 : var1;
      }
   }

   private static String participantLabel(CombatScheduler.ActionParticipant var0, CombatScheduler.ActionParticipant var1) {
      return var0 == var1 ? TEXT_WEB_3 : TEXT_CRYSTAL_4;
   }

   private CombatScheduler() {
   }

   public static CombatScheduler.TickResult runTick(CombatScheduler.ActionParticipant var0, CombatScheduler.ActionParticipant var1) {
      int var2 = var0 == null ? 0 : var0.getExecutionPriority();
      int var3 = var1 == null ? 0 : var1.getExecutionPriority();
      CombatScheduler.ActionParticipant var4 = chooseParticipant(var0, var1, var2, var3);
      CombatScheduler.ActionParticipant var5 = var4 == var0 ? var1 : var0;
      int var6 = var4 == var0 ? var3 : var2;
      boolean var7 = var4 != null && var4.executePreparedAction();
      String var8 = var7 ? participantLabel(var4, var0) : TEXT_NONE_1;
      if (!var7 && var5 != null && var6 > 0 && var5.executePreparedAction()) {
         var8 = participantLabel(var5, var0);
      }

      if (var0 != null) {
         var0.cancelPreparedAction();
      }

      if (var1 != null) {
         var1.cancelPreparedAction();
      }

      int var9 = var0 == null ? 0 : var0.getPreparationPriority();
      int var10 = var1 == null ? 0 : var1.getPreparationPriority();
      var4 = chooseParticipant(var0, var1, var9, var10);
      var5 = var4 == var0 ? var1 : var0;
      if (var4 != null && var4.prepareAction()) {
         return new CombatScheduler.TickResult(var8, participantLabel(var4, var0));
      } else {
         return var4 != null && var5 != null && var5.getPreparationPriority() > 0 && var5.prepareAction()
            ? new CombatScheduler.TickResult(var8, participantLabel(var5, var0))
            : new CombatScheduler.TickResult(var8, TEXT_NONE_2);
      }
   }

   public interface ActionParticipant {
      boolean executePreparedAction();

      int getPreparationPriority();

      void cancelPreparedAction();

      int getExecutionPriority();

      boolean prepareAction();
   }

   public record TickResult(String executed, String prepared) {
   }
}
