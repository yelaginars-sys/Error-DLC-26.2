package dev.syntrix.clienttest.client.combat.meow.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MultiBooleanSetting extends Setting {
   private final List<String> options;
   private final Set<String> enabledOptions;

   public MultiBooleanSetting(String var1, String... var2) {
      super(var1);
      this.options = Arrays.asList(var2);
      this.enabledOptions = new HashSet<>();
   }

   public List<String> getOptions() {
      return this.options;
   }

   public void enable(String var1) {
      if (this.options.contains(var1)) {
         this.enabledOptions.add(var1);
      }
   }

   public void disable(String var1) {
      this.enabledOptions.remove(var1);
   }

   public boolean isEnabled(String var1) {
      return this.enabledOptions.contains(var1);
   }

   public void clear() {
      this.enabledOptions.clear();
   }

   public void toggle(String var1) {
      if (this.isEnabled(var1)) {
         this.disable(var1);
      } else {
         this.enable(var1);
      }
   }

   public MultiBooleanSetting enabledByDefault(String var1) {
      if (this.options.contains(var1)) {
         this.enabledOptions.add(var1);
      }

      return this;
   }

   public List<String> getEnabledOptions() {
      return new ArrayList<>(this.enabledOptions);
   }
}
