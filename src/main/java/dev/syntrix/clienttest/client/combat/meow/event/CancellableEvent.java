package dev.syntrix.clienttest.client.combat.meow.event;



public abstract class CancellableEvent implements Event {
   private boolean state001;

   public void operation002(boolean var1) {
      this.state001 = var1;
   }

   public boolean operation001() {
      return this.state001;
   }

   public void operation004() {
      this.state001 = false;
   }

   public void operation003() {
      this.state001 = true;
   }
}
