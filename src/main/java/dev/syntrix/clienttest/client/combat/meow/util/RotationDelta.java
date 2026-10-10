package dev.syntrix.clienttest.client.combat.meow.util;

import net.minecraft.world.phys.Vec2;

public record RotationDelta(float deltaYaw, float deltaPitch) {
   public float length() {
      return (float)Math.sqrt(this.deltaYaw * this.deltaYaw + this.deltaPitch * this.deltaPitch);
   }

   public Vec2 toVec2f() {
      return new Vec2(this.deltaYaw, this.deltaPitch);
   }

   public boolean isInRange(float var1, float var2) {
      return Math.abs(this.deltaYaw) < var1 && Math.abs(this.deltaPitch) < var2;
   }

   public boolean isInRange(float var1) {
      return this.isInRange(var1, var1);
   }
}
