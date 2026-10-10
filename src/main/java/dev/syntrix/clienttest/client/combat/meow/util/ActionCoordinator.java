package dev.syntrix.clienttest.client.combat.meow.util;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import dev.syntrix.clienttest.client.combat.meow.event.MovementInputEvent;
import dev.syntrix.clienttest.client.combat.meow.event.UpdateEvent;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;

public class ActionCoordinator implements MinecraftAccess {
   public static ActionCoordinator INSTANCE = new ActionCoordinator();
   public ActionCoordinator.Support458 rotationPhase;
   public float yawSpeed;
   public float pitchSpeed;
   public float resetYawSpeed;
   public float resetPitchSpeed;
   public int priority;
   public int timeoutTicks;
   public int rotationAge;
   public Rotation targetRotation;
   private ActionCoordinator.ActionOwner currentOwner = ActionCoordinator.ActionOwner.NONE;
   private ActionCoordinator.ActionOwner reservedOwner = ActionCoordinator.ActionOwner.NONE;
   private int reservationTicks;
   private boolean rotationFlag;
   private ActionCoordinator.RotationMode rotationMode = ActionCoordinator.RotationMode.OLD;
   private static final String TEXT_AURACORRECTIONSILENT_1 = new String("auraCorrectionSilent");
   private static final float state017 = 180.0F;
   private static final float state018 = -90.0F;
   private static final float state019 = 90.0F;
   private static final float state020 = 180.0F;
   private static final float state021 = -90.0F;
   private static final float state022 = 90.0F;
   private static final float state023 = -90.0F;
   private static final float state024 = 90.0F;
   private static final float state025 = -90.0F;
   private static final float state026 = 90.0F;
   private static final float state027 = -90.0F;
   private static final float state028 = 90.0F;

   public void operation001(int var1) {
      this.rotationAge = var1;
   }

   private static Rotation operation002(Rotation var0) {
      return new Rotation(Mth.wrapDegrees(var0.getYaw()), Mth.clamp(var0.getPitch(), state027, state028));
   }

   public void operation003(float var1) {
      this.pitchSpeed = var1;
   }

   private static boolean operation004() { return false; }

   private void operation005() {
      this.targetRotation = null;
      this.rotationPhase = ActionCoordinator.Support458.IDLE;
      this.currentOwner = ActionCoordinator.ActionOwner.NONE;
      this.priority = 0;
      this.rotationAge = 0;
      ServerRotationState.operation003(false);
   }

   private void operation006() {
      this.reservedOwner = ActionCoordinator.ActionOwner.NONE;
      this.reservationTicks = 0;
   }

   public boolean operation007() {
      return this.rotationPhase != ActionCoordinator.Support458.IDLE;
   }

   public boolean getRotationFlag() {
      return this.rotationFlag;
   }

   public static boolean operation009(ActionCoordinator.ActionOwner var0) {
      return var0 != null && INSTANCE.reservedOwner == var0;
   }

   public static void operation010(Rotation var0, float var1, float var2, float var3, float var4, int var5, int var6, boolean var7) {
      operation046(var0, var1, var2, var3, var4, var5, var6, var7, ActionCoordinator.RotationMode.OLD);
   }

   private float operation011(float var1) {
      float var2 = Rotation.getMouseStep();
      return var2 <= 0.0F ? var1 : Math.round(var1 / var2) * var2;
   }

   private static boolean operation012(ActionCoordinator var0, ActionCoordinator.ActionOwner var1, int var2) {
      if (var0.reservedOwner != ActionCoordinator.ActionOwner.NONE && var1 != var0.reservedOwner) {
         return var1.getPriority() > var0.reservedOwner.getPriority();
      } else if (var0.currentOwner == ActionCoordinator.ActionOwner.NONE) {
         return true;
      } else if (var1 == var0.currentOwner) {
         return true;
      } else if (ActionCoordinator.ActionOwner.isAttackAuraOwner(var1) && ActionCoordinator.ActionOwner.isAttackAuraOwner(var0.currentOwner)) {
         return true;
      } else {
         return var2 >= var0.priority ? true : var0.timeoutTicks > 0 && var0.rotationAge > var0.timeoutTicks;
      }
   }

   private void operation013() {
      if (this.reservedOwner != ActionCoordinator.ActionOwner.NONE && this.reservationTicks > 0) {
         this.reservationTicks--;
         if (this.reservationTicks == 0) {
            ActionCoordinator.ActionOwner var1 = this.reservedOwner;
            this.operation006();
            if (this.currentOwner == var1) {
               operation016(this);
               operation059(var1);
            }
         }
      } else {
         this.operation006();
      }
   }

   private Rotation operation014() {
      return operation002(new Rotation(ServerRotationState.getYaw(), ServerRotationState.getPitch()));
   }

   public void operation015() {
      ActionCoordinator.RotationMode var1 = this.rotationMode;
      if (var1 == ActionCoordinator.RotationMode.NEW) {
         this.operation005();
      } else {
         this.operation068();
      }

      this.rotationFlag = false;
      this.rotationMode = ActionCoordinator.RotationMode.OLD;
   }

   private static void operation016(ActionCoordinator var0) {
      var0.rotationPhase = ActionCoordinator.Support458.IDLE;
      var0.currentOwner = ActionCoordinator.ActionOwner.NONE;
      var0.priority = 0;
      var0.rotationAge = 0;
      var0.targetRotation = null;
   }

   public void operation017(float var1) {
      this.resetYawSpeed = var1;
   }

   @Subscribe
   public void operation018(MovementInputEvent event) {
      if (!ServerRotationState.operation005() && !BaritoneCompat.isControllingMovement() && this.operation007() && (this.rotationMode != RotationMode.NEW || !this.rotationFlag))
         MovementCorrection.operation002(event, ServerRotationState.getYaw());
   }

   public void operation019(float var1) {
      this.resetPitchSpeed = var1;
   }

   public float getResetPitchSpeed() {
      return this.resetPitchSpeed;
   }

   private void operation021(boolean var1) {
      Rotation var2 = this.operation014();
      if (this.operation052(var2, this.resetYawSpeed, this.resetPitchSpeed)) {
         if (var1) {
            this.operation015();
         } else {
            this.rotationPhase = ActionCoordinator.Support458.IDLE;
            this.currentOwner = ActionCoordinator.ActionOwner.NONE;
            this.priority = 0;
         }
      }
   }

   public void operation022(boolean var1) {
      this.rotationFlag = var1;
   }

   public void operation023(float var1) {
      this.yawSpeed = var1;
   }

   private static void operation024() {
      if (mc.player != null) {
         ServerRotationState.setServerYaw(mc.player.getYRot());
         ServerRotationState.setServerPitch(mc.player.getXRot());
      }
   }

   public ActionCoordinator.ActionOwner getReservedOwner() {
      return this.reservedOwner;
   }

   public int getRotationAge() {
      return this.rotationAge;
   }

   public void operation027(ActionCoordinator.Support458 var1) {
      this.rotationPhase = var1;
   }

   public float getResetYawSpeed() {
      return this.resetYawSpeed;
   }

   public static void operation029(Rotation var0, float var1, float var2, float var3, int var4, int var5) {
      operation010(var0, var1, var2, var3, var3, var4, var5, false);
   }

   public static void operation030(Rotation var0, float var1, float var2, int var3, int var4) {
      ActionCoordinator var5 = INSTANCE;
      if (mc.player != null && var0 != null && !var0.isInvalid()) {
         ActionCoordinator.ActionOwner var6 = ActionCoordinator.ActionOwner.fromPriority(var4);
         if (operation012(var5, var6, var4)) {
            operation065();
            var5.rotationFlag = false;
            var5.rotationMode = ActionCoordinator.RotationMode.OLD;
            var5.currentOwner = var6;
            var5.resetYawSpeed = Math.max(0.0F, var1);
            var5.resetPitchSpeed = Math.max(0.0F, var2);
            var5.timeoutTicks = Math.max(0, var3);
            var5.priority = var4;
            var5.rotationPhase = ActionCoordinator.Support458.RESET;
            var5.targetRotation = operation002(var0);
            var5.rotationAge = 0;
            var5.operation021(false);
         }
      }
   }

   @Subscribe
   public void operation031(UpdateEvent var1) {
      if (var1.getClass() == UpdateEvent.class) {
         if (mc.player == null) {
            this.operation041();
            this.operation006();
         } else {
            this.operation013();
            if (this.rotationPhase == ActionCoordinator.Support458.AIM && this.timeoutTicks > 0 && this.rotationAge > this.timeoutTicks && !operation004()) {
               this.rotationPhase = ActionCoordinator.Support458.RESET;
            }

            if (this.rotationPhase == ActionCoordinator.Support458.RESET) {
               if (this.rotationMode == ActionCoordinator.RotationMode.NEW) {
                  this.operation045();
               } else {
                  this.operation050();
               }
            }

            this.rotationAge++;
         }
      }
   }

   public void operation032(Rotation var1) {
      this.targetRotation = var1;
   }

   public static void operation033(Rotation var0, float var1, float var2, int var3, int var4) {
      operation010(var0, var1, var1, var2, var2, var3, var4, false);
   }

   public ActionCoordinator.ActionOwner getCurrentOwner() {
      return this.currentOwner;
   }

   public Rotation getTargetRotation() {
      return this.targetRotation;
   }

   public void operation037() {
      if (this.rotationMode == ActionCoordinator.RotationMode.NEW) {
         this.operation060();
         this.operation045();
      } else {
         this.rotationPhase = ActionCoordinator.Support458.RESET;
         this.operation050();
      }
   }

   private boolean operation038(Rotation var1, float var2, float var3, ActionCoordinator.RotationMode var4) {
      return var4 == ActionCoordinator.RotationMode.NEW ? this.operation055(var1, var2, var3) : this.operation052(var1, var2, var3);
   }

   public static boolean reserveOwner(ActionCoordinator.ActionOwner var0, int var1) {
      ActionCoordinator var2 = INSTANCE;
      if (mc.player != null && var0 != null && var0 != ActionCoordinator.ActionOwner.NONE && var1 > 0) {
         if (var2.reservedOwner != ActionCoordinator.ActionOwner.NONE && var2.reservedOwner != var0 && var2.reservedOwner.getPriority() > var0.getPriority()) {
            return false;
         }

         if (!operation012(var2, var0, var0.getPriority())) {
            return false;
         }

         var2.reservedOwner = var0;
         var2.reservationTicks = Math.max(var2.reservationTicks, var1);
         return true;
      } else {
         return false;
      }
   }

   private static float operation040(float var0, float var1) {
      return var1 + Mth.wrapDegrees(var0 - var1);
   }

   public void operation041() {
      ActionCoordinator.RotationMode var1 = this.rotationMode;
      this.operation015();
      if (var1 == ActionCoordinator.RotationMode.OLD) {
         this.rotationAge = 0;
      }
   }

   public static void releaseOwnerAndPreserveRotation(ActionCoordinator.ActionOwner var0) {
      ActionCoordinator var1 = INSTANCE;
      if (var0 != null) {
         if (var1.reservedOwner == var0) {
            var1.operation006();
         }

         if (mc.player != null && var1.currentOwner == var0) {
            operation016(var1);
            operation059(var0);
         }
      }
   }

   public void operation043(int var1) {
      this.reservationTicks = var1;
   }

   public float getPitchSpeed() {
      return this.pitchSpeed;
   }

   private void operation045() {
      Rotation var1 = this.operation014();
      if (this.operation055(var1, this.resetYawSpeed, this.resetPitchSpeed)) {
         this.operation066(var1);
         this.operation015();
      }
   }

   public static void operation046(
      Rotation var0, float var1, float var2, float var3, float var4, int var5, int var6, boolean var7, ActionCoordinator.RotationMode var8
   ) {
      requestRotation(var0, var1, var2, var3, var4, var5, var6, var7, var8, ActionCoordinator.ActionOwner.fromPriority(var6));
   }

   public static boolean operation047(ActionCoordinator.ActionOwner var0, int var1) {
      if (!reserveOwner(var0, var1)) {
         return false;
      }

      ActionCoordinator var2 = INSTANCE;
      var2.operation051();
      operation024();
      return true;
   }

   public static void requestRotation(
      Rotation var0,
      float var1,
      float var2,
      float var3,
      float var4,
      int var5,
      int var6,
      boolean var7,
      ActionCoordinator.RotationMode var8,
      ActionCoordinator.ActionOwner var9
   ) {
      ActionCoordinator var10 = INSTANCE;
      if (mc.player != null) {
         ActionCoordinator.RotationMode var11 = var8 == null ? ActionCoordinator.RotationMode.OLD : var8;
         ActionCoordinator.ActionOwner var12 = var9 != null ? var9 : ActionCoordinator.ActionOwner.fromPriority(var6);
         if (operation012(var10, var12, var6)) {
            var10.rotationFlag = var7;
            var10.rotationMode = var11;
            var10.currentOwner = var12;
            var10.priority = var6;
            if (var0 == null) {
               var10.resetYawSpeed = Math.max(0.0F, var3);
               var10.resetPitchSpeed = Math.max(0.0F, var4);
               var10.rotationPhase = ActionCoordinator.Support458.RESET;
            } else if (!var0.isInvalid()) {
               ServerRotationState.operation003(!var7);

               var10.yawSpeed = Math.max(0.0F, var1);
               var10.pitchSpeed = Math.max(0.0F, var2);
               var10.resetYawSpeed = Math.max(0.0F, var3);
               var10.resetPitchSpeed = Math.max(0.0F, var4);
               var10.timeoutTicks = Math.max(0, var5);
               var10.rotationPhase = ActionCoordinator.Support458.AIM;
               var10.targetRotation = operation002(var0);
               var10.rotationAge = 0;
               var10.operation038(var10.targetRotation, var10.yawSpeed, var10.pitchSpeed, var11);
            }
         }
      }
   }

   public static boolean isBlockedByReservation(ActionCoordinator.ActionOwner var0) {
      ActionCoordinator var1 = INSTANCE;
      return var0 != null
         && var1.reservedOwner != ActionCoordinator.ActionOwner.NONE
         && var1.reservedOwner != var0
         && var1.reservedOwner.getPriority() >= var0.getPriority();
   }

   private void operation050() {
      this.operation021(true);
   }

   public void operation051() {
      this.rotationPhase = ActionCoordinator.Support458.IDLE;
      this.currentOwner = ActionCoordinator.ActionOwner.NONE;
      this.priority = 0;
      this.rotationAge = 0;
      this.targetRotation = null;
   }

   private boolean operation052(Rotation var1, float var2, float var3) {
      if (mc.player != null && var1 != null && !var1.isInvalid()) {
         Rotation var4 = operation002(var1);
         Rotation var5 = new Rotation(ServerRotationState.serverYaw(), ServerRotationState.serverPitch());
         float var6 = Mth.wrapDegrees(var4.getYaw() - var5.getYaw());
         float var7 = var4.getPitch() - var5.getPitch();
         float var8 = Math.abs(var6) + Math.abs(var7);
         float var9 = var8 == 0.0F ? 0.0F : Math.abs(var6 / var8) * Math.max(var2, 0.0F);
         float var10 = var8 == 0.0F ? 0.0F : Math.abs(var7 / var8) * Math.max(var3, 0.0F);
         Vec2 var11 = RotationUtil.operation009(
            new Vec2(ServerRotationState.serverYaw() + Mth.clamp(var6, -var9, var9), Mth.clamp(ServerRotationState.serverPitch() + Mth.clamp(var7, -var10, var10), state023, state024)),
            new Vec2(ServerRotationState.serverYaw(), ServerRotationState.serverPitch())
         );
         ServerRotationState.setServerYaw(var11.x);
         ServerRotationState.setServerPitch(var11.y);
         Rotation var12 = new Rotation(ServerRotationState.serverYaw(), ServerRotationState.serverPitch());
         this.rotationAge = 0;
         float var13 = this.rotationPhase == ActionCoordinator.Support458.RESET
            ? (float)Math.hypot(this.resetYawSpeed, this.resetPitchSpeed)
            : (float)Math.hypot(this.yawSpeed, this.pitchSpeed);
         return var12.angularDistance(var4) < var13;
      } else {
         return false;
      }
   }

   public void operation053(ActionCoordinator.ActionOwner var1) {
      this.reservedOwner = var1;
   }

   public ActionCoordinator.RotationMode getRotationMode() {
      return this.rotationMode;
   }

   private boolean operation055(Rotation var1, float var2, float var3) {
      if (mc.player != null && var1 != null && !var1.isInvalid()) {
         Rotation var4 = operation002(var1);
         Rotation var5 = new Rotation(ServerRotationState.serverYaw(), ServerRotationState.serverPitch());
         float var6 = Mth.wrapDegrees(var4.getYaw() - var5.getYaw());
         float var7 = var4.getPitch() - var5.getPitch();
         float var8 = Math.min(Math.abs(var6), Math.max(var2, 0.0F));
         float var9 = Math.min(Math.abs(var7), Math.max(var3, 0.0F));
         ServerRotationState.setServerYaw(var5.getYaw() + this.operation011(Mth.clamp(var6, -var8, var8)));
         ServerRotationState.setServerPitch(Mth.clamp(var5.getPitch() + this.operation011(Mth.clamp(var7, -var9, var9)), state025, state026));
         this.rotationAge = 0;
         return new Rotation(ServerRotationState.serverYaw(), ServerRotationState.serverPitch()).angularDistance(var4) < 1.0F;
      } else {
         return false;
      }
   }

   public static void releaseOwner(ActionCoordinator.ActionOwner var0) {
      ActionCoordinator var1 = INSTANCE;
      if (var0 != null) {
         if (var1.reservedOwner == var0) {
            var1.operation006();
         }

         if (var1.currentOwner == var0) {
            ServerRotationState.operation007();
            var1.operation041();
            operation059(var0);
         }
      }
   }

   public int getPriority() {
      return this.priority;
   }

   public int getReservationTicks() {
      return this.reservationTicks;
   }

   private static void operation059(ActionOwner owner) {}

   public void operation060() {
      this.rotationPhase = ActionCoordinator.Support458.RESET;
      this.rotationAge = 0;
   }

   public void operation061(ActionCoordinator.ActionOwner var1) {
      this.currentOwner = var1;
   }

   public ActionCoordinator() {
      this.rotationPhase = ActionCoordinator.Support458.IDLE;
      this.targetRotation = new Rotation(0.0F, 0.0F);
   }

   public static boolean operation063() {
      ActionCoordinator.ActionOwner var0 = INSTANCE.currentOwner;
      return isBlockedByReservation(ActionCoordinator.ActionOwner.AURA)
         || var0 != ActionCoordinator.ActionOwner.NONE && var0 != ActionCoordinator.ActionOwner.POLAR && !ActionCoordinator.ActionOwner.isAttackAuraOwner(var0);
   }

   public int getTimeoutTicks() {
      return this.timeoutTicks;
   }

   public static void operation065() {
      ServerRotationState.operation003(true);
   }

   private void operation066(Rotation var1) {
      if (mc.player != null && var1 != null) {
         Rotation var2 = operation002(var1);
         float var3 = operation040(var2.getYaw(), ServerRotationState.serverYaw());
         ServerRotationState.setServerYaw(var3);
         ServerRotationState.setServerPitch(var2.getPitch());
      }
   }

   public void operation067(int var1) {
      this.priority = var1;
   }

   private void operation068() {
      this.rotationPhase = ActionCoordinator.Support458.IDLE;
      this.currentOwner = ActionCoordinator.ActionOwner.NONE;
      this.priority = 0;
      ServerRotationState.operation003(false);
   }

   public void operation069(int var1) {
      this.timeoutTicks = var1;
   }

   public void operation070(ActionCoordinator.RotationMode var1) {
      this.rotationMode = var1;
   }

   public ActionCoordinator.Support458 getRotationPhase() {
      return this.rotationPhase;
   }

   public float getYawSpeed() {
      return this.yawSpeed;
   }

   public enum ActionOwner {
      NONE(0),
      AURA(2),
      AURA_ELEVATED(6),
      WEB_TRAP(9),
      UTILITY(10),
      PEARL(20),
      ANTI_CRYSTAL(48),
      CRYSTAL(50),
      CRYSTAL_AURA(50),
      CRYSTAL_OPTIMIZER(49),
      POLAR(55);

      private final int state012;

      ActionOwner(int var3) {
         this.state012 = var3;
      }

      public int getPriority() {
         return this.state012;
      }

      public static ActionCoordinator.ActionOwner fromPriority(int var0) {
         if (var0 <= AURA.getPriority()) {
            return AURA;
         } else if (var0 <= AURA_ELEVATED.getPriority()) {
            return AURA_ELEVATED;
         } else if (var0 <= UTILITY.getPriority()) {
            return UTILITY;
         } else if (var0 <= PEARL.getPriority()) {
            return PEARL;
         } else {
            return var0 <= CRYSTAL.getPriority() ? CRYSTAL : POLAR;
         }
      }

      public static boolean isAttackAuraOwner(ActionCoordinator.ActionOwner var0) {
         return var0 == AURA || var0 == AURA_ELEVATED;
      }
   }

   public enum RotationMode {
      OLD,
      NEW;
   }

   public enum Support458 {
      AIM,
      RESET,
      IDLE;
   }
}
