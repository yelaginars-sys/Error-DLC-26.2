package dev.syntrix.clienttest.client.combat.meow.setting;

public class NumberSetting extends Setting {
   private double value;
   private final float min;
   private final float max;
   private final float step;

   public void setValue(double var1) {
      this.value = Math.max(this.min, Math.min(this.max, var1));
   }

   public float getMin() {
      return this.min;
   }

   public float getStep() {
      return this.step;
   }

   public NumberSetting(String var1, float var2, float var3, float var4, float var5) {
      super(var1);
      this.value = var2;
      this.min = var3;
      this.max = var4;
      this.step = var5;
   }

   public float getMax() {
      return this.max;
   }

   public float getValue() {
      return (float)this.value;
   }
}
