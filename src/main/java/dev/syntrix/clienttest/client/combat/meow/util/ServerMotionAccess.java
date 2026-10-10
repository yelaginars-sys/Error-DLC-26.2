package dev.syntrix.clienttest.client.combat.meow.util;

import net.minecraft.world.phys.Vec3;

public interface ServerMotionAccess {
   double meow$getPrevServerX();

   void meow$setServerVelocity(Vec3 var1);

   double meow$getServerZ();

   Vec3 meow$getServerVelocity();

   double meow$getServerY();

   void meow$setServerPos(double var1, double var3, double var5);

   double meow$getPrevServerY();

   double meow$getPrevServerZ();

   double meow$getServerX();
}
