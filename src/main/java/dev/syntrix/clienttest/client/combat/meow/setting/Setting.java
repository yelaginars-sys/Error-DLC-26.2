package dev.syntrix.clienttest.client.combat.meow.setting;

import java.util.function.Supplier;

public abstract class Setting {
   private final String name;
   public int keyCode = -1;
   private BindMode bindMode = BindMode.TOGGLE;
   private Supplier<Boolean> visibility = () -> true;

   public Setting hideWhen(Supplier<Boolean> var1) {
      this.visibility = () -> !(Boolean)var1.get();
      return this;
   }

   public boolean isVisible() {
      return this.visibility.get();
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public Setting visibleWhen(Supplier<Boolean> var1) {
      this.visibility = var1;
      return this;
   }

   public BindMode getBindMode() {
      return this.bindMode;
   }

   public void setKeyCode(int var1) {
      this.keyCode = var1;
   }

   public void setBindMode(BindMode var1) {
      this.bindMode = var1;
   }

   public Setting(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public Supplier<Boolean> getVisibility() {
      return this.visibility;
   }
}
