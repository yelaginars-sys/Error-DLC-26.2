package dev.syntrix.clienttest.client.combat.meow.event;

import net.minecraft.world.entity.player.Input;

public class MovementInputEvent extends CancellableEvent {
   private Input state001;
   private float state002;
   private float state003;
   private boolean state004;
   private boolean state005;

   public void operation001(Input var1) {
      this.state001 = var1;
   }

   @Override
   public void operation002(boolean var1) {
      this.state004 = var1;
   }

   public void operation003(float var1) {
      this.state002 = var1;
   }

   public float getState003() {
      return this.state003;
   }

   public float getState002() {
      return this.state002;
   }

   public boolean getState004() {
      return this.state004;
   }

   public void operation007(boolean var1) {
      this.state005 = var1;
   }

   public void operation008(float var1) {
      this.state003 = var1;
   }

   public boolean getState005() {
      return this.state005;
   }

   public MovementInputEvent(Input var1, float var2, float var3, boolean var4, boolean var5) {
      this.state001 = var1;
      this.state002 = var2;
      this.state003 = var3;
      this.state004 = var4;
      this.state005 = var5;
   }

   public Input getState001() {
      return this.state001;
   }
}
