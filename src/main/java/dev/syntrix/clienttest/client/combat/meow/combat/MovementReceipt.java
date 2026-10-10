package dev.syntrix.clienttest.client.combat.meow.combat;

import net.minecraft.world.phys.Vec3;

public final class MovementReceipt {
   private int observedTick;
   private Vec3 observedPosition;
   private boolean valid;
   private String invalidReason;
   private boolean rotationKnown;
   private boolean positionSynchronized;
   private boolean connectionOpen;
   private static final int state008 = Integer.MIN_VALUE;
   private static final String TEXT_MISSING_RECEIPT_1 = new String("missing-receipt");
   private static final String TEXT_INVALID_RECEIPT_2 = new String("invalid-receipt");
   private static final String TEXT_CONNECTION_CLOSED_3 = new String("connection-closed");
   private static final String TEXT_ROTATION_UNKNOWN_4 = new String("rotation-unknown");
   private static final String TEXT_POSITION_UNSYNCHRONIZED_5 = new String("position-unsynchronized");
   private static final double state014 = 4.0E-8;
   private static final String TEXT_TICK_MISMATCH_6 = new String("tick-mismatch");
   private static final String TEXT_NOT_NEXT_TICK_7 = new String("not-next-tick");
   private static final String TEXT_MISSING_OBSERVED_POSITION_8 = new String("missing-observed-position");
   private static final String TEXT_MISSING_CURRENT_POSITION_9 = new String("missing-current-position");
   private static final String TEXT_INVALID_POSITION_10 = new String("invalid-position");
   private static final double state020 = 4.0E-8;
   private static final String TEXT_POSITION_CHANGED_11 = new String("position-changed");
   private static final String TEXT_READY_12 = new String("ready");
   private static final int state023 = Integer.MIN_VALUE;
   private static final String TEXT_MISSING_RECEIPT_13 = new String("missing-receipt");

   public void reset() {
      this.observedTick = state023;
      this.observedPosition = null;
      this.valid = false;
      this.invalidReason = TEXT_MISSING_RECEIPT_13;
      this.rotationKnown = false;
      this.positionSynchronized = false;
      this.connectionOpen = false;
   }

   public MovementReceipt() {
      this.observedTick = state008;
      this.invalidReason = TEXT_MISSING_RECEIPT_1;
   }

   public void observe(int var1, Vec3 var2, boolean var3, boolean var4, boolean var5) {
      this.observedTick = var1;
      this.observedPosition = var2;
      this.rotationKnown = var3;
      this.positionSynchronized = var4;
      this.connectionOpen = var5;
      this.valid = var3 && var4 && var5;
      this.invalidReason = !var5 ? TEXT_CONNECTION_CLOSED_3 : (!var3 ? TEXT_ROTATION_UNKNOWN_4 : (!var4 ? TEXT_POSITION_UNSYNCHRONIZED_5 : null));
   }

   public void observeValidity(int var1, Vec3 var2, boolean var3) {
      this.observe(var1, var2, var3, var3, var3);
      if (!var3) {
         this.invalidReason = TEXT_INVALID_RECEIPT_2;
      }
   }

   public String describeWindow(int var1, int var2, Vec3 var3) {
      double var4 = this.observedPosition != null && var3 != null ? this.observedPosition.distanceToSqr(var3) : Double.NaN;
      String var6 = !this.valid
         ? this.invalidReason
         : (
            this.observedTick != var1
               ? TEXT_TICK_MISMATCH_6
               : (
                  (long)var2 - var1 != 1L
                     ? TEXT_NOT_NEXT_TICK_7
                     : (
                        this.observedPosition == null
                           ? TEXT_MISSING_OBSERVED_POSITION_8
                           : (
                              var3 == null
                                 ? TEXT_MISSING_CURRENT_POSITION_9
                                 : (!Double.isFinite(var4) ? TEXT_INVALID_POSITION_10 : (var4 > state020 ? TEXT_POSITION_CHANGED_11 : TEXT_READY_12))
                           )
                     )
               )
         );
      return "reason="
         + var6
         + " observedTick="
         + this.observedTick
         + " positionDeltaSq="
         + var4
         + " rotationKnown="
         + this.rotationKnown
         + " positionSynchronized="
         + this.positionSynchronized
         + " connectionOpen="
         + this.connectionOpen;
   }

   public boolean isReadyForNextTick(int var1, int var2, Vec3 var3) {
      return this.valid
         && this.observedTick == var1
         && (long)var2 - var1 == 1L
         && this.observedPosition != null
         && var3 != null
         && this.observedPosition.distanceToSqr(var3) <= state014;
   }
}
