package dev.syntrix.clienttest.client.combat.meow.setting;

import java.util.Arrays;

public class ModeSetting extends Setting {
   private int selectedIndex;
   public String[] modes;
   public int keyCode;
   private static final String TEXT_ERROR_1 = new String("ERROR");
   private static final String TEXT_ERROR_2 = new String("ERROR");

   public void setModes(String... var1) {
      String var2 = this.getSelectedMode();
      this.modes = var1 != null && var1.length != 0 ? var1 : new String[]{TEXT_ERROR_2};
      int var3 = Arrays.asList(this.modes).indexOf(var2);
      this.selectedIndex = var3 >= 0 ? var3 : 0;
   }

   public void setMode(String var1) {
      this.selectedIndex = Arrays.asList(this.modes).indexOf(var1);
   }

   public void setSelectedIndex(int var1) {
      this.selectedIndex = var1;
   }

   public ModeSetting(String var1, String var2, String... var3) {
      super(var1);
      this.modes = var3;
      this.selectedIndex = Arrays.asList(var3).indexOf(var2);
   }

   public int getSelectedIndex() {
      return this.selectedIndex;
   }

   @Override
   public int getKeyCode() {
      return this.keyCode;
   }

   public ModeSetting(String var1, int var2, String var3, String... var4) {
      super(var1);
      this.modes = var4;
      this.selectedIndex = Arrays.asList(var4).indexOf(var3);
      this.keyCode = var2;
   }

   @Override
   public void setKeyCode(int var1) {
      this.keyCode = var1;
   }

   public String[] getModes() {
      return this.modes;
   }

   public void selectIndex(int var1) {
      this.selectedIndex = var1;
   }

   public String getSelectedMode() {
      try {
         return this.selectedIndex >= 0 && this.selectedIndex < this.modes.length ? this.modes[this.selectedIndex] : this.modes[0];
      } catch (ArrayIndexOutOfBoundsException var2) {
         return TEXT_ERROR_1;
      }
   }

   public boolean isMode(String var1) {
      return this.getSelectedMode().equals(var1);
   }
}
