package dev.syntrix.clienttest.client.combat.meow.event;

public class MovementReceiptEvent implements Event {
   private final float yaw;
   private final float pitch;
   private final boolean rotationKnown;
   private final boolean positionSynchronized;
   private final boolean receiptFlag5;
   private final boolean receiptFlag6;
   private final boolean connectionOpen;

   public boolean getReceiptFlag6() {
      return this.receiptFlag6;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public boolean getReceiptFlag5() {
      return this.receiptFlag5;
   }

   public boolean getPositionSynchronized() {
      return this.positionSynchronized;
   }

   public boolean getConnectionOpen() {
      return this.connectionOpen;
   }

   public boolean getRotationKnown() {
      return this.rotationKnown;
   }

   public MovementReceiptEvent(float var1, float var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7) {
      this.yaw = var1;
      this.pitch = var2;
      this.rotationKnown = var3;
      this.positionSynchronized = var4;
      this.receiptFlag5 = var5;
      this.receiptFlag6 = var6;
      this.connectionOpen = var7;
   }
}
