package dev.syntrix.clienttest.client.combat.meow.combat;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dev.syntrix.clienttest.client.combat.meow.event.MovementInputEvent;
import dev.syntrix.clienttest.client.combat.meow.event.MovementReceiptEvent;
import dev.syntrix.clienttest.client.combat.meow.event.UpdateEvent;
import dev.syntrix.clienttest.client.mixin.CrystalInteractionManagerAccessor;
import dev.syntrix.clienttest.client.combat.meow.module.Module;
import dev.syntrix.clienttest.client.combat.meow.module.ModuleCategory;
import dev.syntrix.clienttest.client.combat.meow.module.ModuleMetadata;
import dev.syntrix.clienttest.client.combat.meow.setting.ModeSetting;
import dev.syntrix.clienttest.client.combat.meow.setting.MultiBooleanSetting;
import dev.syntrix.clienttest.client.combat.meow.setting.NumberSetting;
import dev.syntrix.clienttest.client.combat.meow.util.ActionCoordinator;
import dev.syntrix.clienttest.client.combat.meow.util.BaritoneCompat;
import dev.syntrix.clienttest.client.combat.meow.util.CobwebPrediction;
import dev.syntrix.clienttest.client.combat.meow.util.CombatMath;
import dev.syntrix.clienttest.client.combat.meow.util.EntityRaycast;
import dev.syntrix.clienttest.client.combat.meow.util.InteractionContextGuard;
import dev.syntrix.clienttest.client.combat.meow.util.InteractionRaycast;
import dev.syntrix.clienttest.client.combat.meow.util.InventorySwapManager;
import dev.syntrix.clienttest.client.combat.meow.util.InventoryUtil;
import dev.syntrix.clienttest.client.combat.meow.util.MotionPrediction;
import dev.syntrix.clienttest.client.combat.meow.util.MovementCorrection;
import dev.syntrix.clienttest.client.combat.meow.util.PlacementCooldowns;
import dev.syntrix.clienttest.client.combat.meow.util.Rotation;
import dev.syntrix.clienttest.client.combat.meow.util.RotationUtil;
import dev.syntrix.clienttest.client.combat.meow.util.ServerActionState;
import dev.syntrix.clienttest.client.combat.meow.util.ServerRotationState;

public class CrystalAura extends Module implements CombatScheduler.ActionParticipant {
   private static final String PLACE_OBSIDIAN_KEY = "crystalAuraPlaceObsidian";
   private final ModeSetting mode;
   private final NumberSetting targetRange;
   private final NumberSetting placeRange;
   private final NumberSetting breakRange;
   private final NumberSetting minDamage;
   private final NumberSetting maxSelfDamage;
   private final NumberSetting actionDelay;
   private final NumberSetting predictionTicks;
   private final MultiBooleanSetting options;
   private static final int crystalColor = argb(45, 125, 255, 255);
   private static final int obsidianColor = argb(180, 70, 255, 255);
   private static final int digColor = argb(255, 55, 55, 255);
   private final Map<BlockPos, Integer> positionCooldowns;
   private final Map<Integer, CrystalAura.TrackedCrystal> attackedCrystals;
   private final List<CrystalAura.RenderMarker> renderMarkers;
   private BlockPos miningTarget;
   private int digProgressRegressionCount;
   private int digNoProgressTicks;
   private float previousDigProgress;
   private int miningAttempts;
   private int toolSlot;
   private int originalHotbarSlot;
   private BlockPos pendingCrystalBase;
   private int pendingCrystalTick;
   private BlockPos lastCrystalBase;
   private int lastCrystalPlaceTick;
   private BlockPos plannedBasePosition;
   private long lastActionTime;
   private int rotationRequestTick;
   private int lastRotationTick;
   private CrystalAura.PlacementAction pendingPlacement;
   private CrystalAura.PendingCrystal pendingBreak;
   private CrystalAura.DigAction pendingDig;
   private float serverYaw;
   private float serverPitch;
   private final MovementReceipt movementReceipt;
   private int preparedTick;
   private int lastExecutionTick;
   private LocalPlayer preparedPlayer;
   private ClientLevel preparedWorld;
   private BlockPos renderPosition;
   private float renderProgress;
   private long renderTime;
   private Player chosenTarget;
   private boolean prepared;
   private boolean targetListsReady;
   private int executionPreparedTick;
   private EndCrystal crystalToBreak;
   private BlockHitResult preparedHit;
   private List<Player> targets;
   private List<Player> collateralPlayers;
   private CombatMath.ScoredCrystal scoredCrystal;
   private Player activeTarget;
   private float selfDamageLimit;
   private static final String TEXT_CRYSTAL_AURA_1 = new String("Crystal Aura");
   private static final String TEXT_CRYSTALAURAMODE_2 = new String("crystalAuraMode");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_3 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_4 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODERW_5 = new String("crystalAuraModeRw");
   private static final String TEXT_CRYSTALAURATARGETRANGE_6 = new String("crystalAuraTargetRange");
   private static final float DEFAULT_TARGET_RANGE = 8.0F;
   private static final float MIN_TARGET_RANGE = 4.0F;
   private static final float MAX_TARGET_RANGE = 12.0F;
   private static final float TARGET_RANGE_STEP = 0.5F;
   private static final String TEXT_CRYSTALAURAPLACERANGE_7 = new String("crystalAuraPlaceRange");
   private static final float DEFAULT_PLACE_RANGE = 4.5F;
   private static final float MIN_PLACE_RANGE = 3.0F;
   private static final float MAX_PLACE_RANGE = 6.0F;
   private static final float PLACE_RANGE_STEP = 0.1F;
   private static final String TEXT_CRYSTALAURABREAKRANGE_8 = new String("crystalAuraBreakRange");
   private static final float DEFAULT_BREAK_RANGE = 3.0F;
   private static final float MAX_BREAK_RANGE = 6.0F;
   private static final float BREAK_RANGE_STEP = 0.1F;
   private static final String TEXT_CRYSTALAURAMINDAMAGE_9 = new String("crystalAuraMinDamage");
   private static final float DEFAULT_MIN_DAMAGE = 6.0F;
   private static final float MAX_MIN_DAMAGE = 20.0F;
   private static final float MIN_DAMAGE_STEP = 0.5F;
   private static final String TEXT_CRYSTALAURAMAXSELFDAMAGE_10 = new String("crystalAuraMaxSelfDamage");
   private static final float DEFAULT_MAX_SELF_DAMAGE = 8.0F;
   private static final float MAX_SELF_DAMAGE_VALUE = 20.0F;
   private static final float SELF_DAMAGE_STEP = 0.5F;
   private static final String TEXT_CRYSTALAURADELAY_11 = new String("crystalAuraDelay");
   private static final float MAX_DELAY_MS = 500.0F;
   private static final float DELAY_STEP_MS = 10.0F;
   private static final String TEXT_CRYSTALAURAPREDICTTICKS_12 = new String("crystalAuraPredictTicks");
   private static final float MAX_PREDICTION_TICKS = 4.0F;
   private static final String TEXT_CRYSTALAURAOPTIONS_13 = new String("crystalAuraOptions");
   private static final String TEXT_CRYSTALAURAPLACEOBSIDIAN_14 = new String("crystalAuraPlaceObsidian");
   private static final String TEXT_CRYSTALAURALEGIT_15 = new String("crystalAuraLegit");
   private static final String TEXT_CRYSTALAURAANTISUICIDE_16 = new String("crystalAuraAntiSuicide");
   private static final String TEXT_CRYSTALAURAAUTODIG_17 = new String("crystalAuraAutoDig");
   private static final String TEXT_CRYSTALAURASKIPOBSIDIANDIG_18 = new String("crystalAuraSkipObsidianDig");
   private static final String TEXT_CRYSTALAURAYIELDWEBTRAP_19 = new String("crystalAuraYieldWebTrap");
   private static final String TEXT_CRYSTALAURARENDER_20 = new String("crystalAuraRender");
   private static final String TEXT_CRYSTALAURALOGS_21 = new String("crystalAuraLogs");
   private static final String TEXT_CRYSTALAURASELFSHIELD_22 = new String("crystalAuraSelfShield");
   private static final String TEXT_CRYSTALAURAPLACEOBSIDIAN_23 = new String("crystalAuraPlaceObsidian");
   private static final String TEXT_CRYSTALAURAANTISUICIDE_24 = new String("crystalAuraAntiSuicide");
   private static final String TEXT_CRYSTALAURAAUTODIG_25 = new String("crystalAuraAutoDig");
   private static final String TEXT_CRYSTALAURAYIELDWEBTRAP_26 = new String("crystalAuraYieldWebTrap");
   private static final String TEXT_CRYSTALAURARENDER_27 = new String("crystalAuraRender");
   private static final int NO_INITIAL_PREPARED_TICK = Integer.MIN_VALUE;
   private static final int NO_INITIAL_EXECUTION_TICK = Integer.MIN_VALUE;
   private static final int NO_INITIAL_EXECUTION_PREPARED_TICK = Integer.MIN_VALUE;
   private static final int NO_DISABLED_TICK = Integer.MIN_VALUE;
   private static final int NO_WORLD_CHANGE_TICK = Integer.MIN_VALUE;
   private static final String TEXT_CRYSTALAURALOGS_28 = new String("crystalAuraLogs");
   private static final int NO_EVALUATED_EXECUTION_TICK = Integer.MIN_VALUE;
   private static final String TEXT_CRYSTALAURALEGIT_29 = new String("crystalAuraLegit");
   private static final String TEXT_CRYSTALAURAAUTODIG_30 = new String("crystalAuraAutoDig");
   private static final int NO_CONSUMED_EXECUTION_TICK = Integer.MIN_VALUE;
   private static final int NO_CANCELLED_EXECUTION_TICK = Integer.MIN_VALUE;
   private static final String TEXT_CRYSTALAURAYIELDWEBTRAP_31 = new String("crystalAuraYieldWebTrap");
   private static final String TEXT_CRYSTALAURAANTISUICIDE_32 = new String("crystalAuraAntiSuicide");
   private static final float UNLIMITED_PLANNING_SELF_DAMAGE = Float.MAX_VALUE;
   private static final String TEXT_CRYSTALAURAAUTODIG_33 = new String("crystalAuraAutoDig");
   private static final String TEXT_CRYSTALAURALEGIT_34 = new String("crystalAuraLegit");
   private static final String TEXT_CRYSTALAURAPLACEOBSIDIAN_35 = new String("crystalAuraPlaceObsidian");
   private static final String TEXT_CRYSTALAURAAUTODIG_36 = new String("crystalAuraAutoDig");
   private static final String TEXT_CRYSTALAURASKIPOBSIDIANDIG_37 = new String("crystalAuraSkipObsidianDig");
   private static final String TEXT_CRYSTALAURALOGS_38 = new String("crystalAuraLogs");
   private static final String TEXT_S_S_QUEUED_B_39 = new String("-> %s %s queued=%b");
   private static final String TEXT_OBSIDIAN_40 = new String("OBSIDIAN");
   private static final String TEXT_CRYSTAL_41 = new String("CRYSTAL");
   private static final float COLLATERAL_RANGE_EXTRA = 8.0F;
   private static final double PREDICTION_MOVEMENT_EPSILON_SQ = 1.0E-6;
   private static final double CRYSTAL_SEARCH_HEIGHT = 2.0;
   private static final double AIM_PADDING_MIN_X = 0.05;
   private static final double AIM_PADDING_MAX_X = 0.05;
   private static final double AIM_PADDING_MIN_Y = 0.05;
   private static final double AIM_PADDING_MAX_Y = 0.05;
   private static final double AIM_PADDING_MIN_Z = 0.05;
   private static final double AIM_PADDING_MAX_Z = 0.05;
   private static final double HIGH_AIM_PADDING_A = 0.15;
   private static final double HIGH_AIM_PADDING_B = 0.15;
   private static final double BLOCK_CENTER_X = 0.5;
   private static final double BLOCK_CENTER_Y = 0.5;
   private static final double BLOCK_CENTER_Z = 0.5;
   private static final String TEXT_CRYSTALAURALOGS_42 = new String("crystalAuraLogs");
   private static final String TEXT_CRYSTALAURALEGIT_43 = new String("crystalAuraLegit");
   private static final String TEXT_S_S_PLACED_B_44 = new String("-> %s %s placed=%b");
   private static final String TEXT_CRYSTAL_45 = new String("CRYSTAL");
   private static final String TEXT_CRYSTALAURASELFSHIELD_46 = new String("crystalAuraSelfShield");
   private static final double INITIAL_NEAREST_TARGET_DISTANCE = Double.MAX_VALUE;
   private static final float SHIELD_RANGE_EXTRA = 1.5F;
   private static final float DIG_PROGRESS_EPSILON = 0.001F;
   private static final String TEXT_CRYSTALAURARENDER_47 = new String("crystalAuraRender");
   private static final String TEXT_DIG_WANT_S_SIDE_S_CONT_B_PROG_2F_HELD_D_48 = new String("  dig want=%s side=%s cont=%b prog=%.2f held=%d");
   private static final String TEXT_CRYSTALAURASKIPOBSIDIANDIG_49 = new String("crystalAuraSkipObsidianDig");
   private static final String TEXT_CRYSTALAURASKIPOBSIDIANDIG_50 = new String("crystalAuraSkipObsidianDig");
   private static final float AIM_YAW_SPEED = 360.0F;
   private static final float AIM_PITCH_SPEED = 360.0F;
   private static final float RESET_YAW_SPEED = 180.0F;
   private static final float RESET_PITCH_SPEED = 180.0F;
   private static final String TEXT_CRYSTALAURALOGS_51 = new String("crystalAuraLogs");
   private static final String TEXT_CRYSTALAURAYIELDWEBTRAP_52 = new String("crystalAuraYieldWebTrap");
   private static final String TEXT_CRYSTALAURALOGS_53 = new String("crystalAuraLogs");
   private static final String TEXT_CRYSTALAURAANTISUICIDE_54 = new String("crystalAuraAntiSuicide");
   private static final float UNLIMITED_EXECUTION_SELF_DAMAGE = Float.MAX_VALUE;
   private static final String TEXT_CRYSTALAURAMODERW_55 = new String("crystalAuraModeRw");
   private static final float RW_TARGET_RANGE = 6.0F;
   private static final float RW_PLACE_RANGE = 3.2F;
   private static final float RW_BREAK_RANGE = 3.0F;
   private static final float RW_MIN_DAMAGE = 6.0F;
   private static final float RW_MAX_SELF_DAMAGE = 8.0F;
   private static final float RW_DELAY_MS = 100.0F;
   private static final double TICK_DURATION_MS = 50.0;
   private static final String TEXT_CRYSTALAURALOGS_56 = new String("crystalAuraLogs");
   private static final String TEXT_CRYSTALAURARENDER_57 = new String("crystalAuraRender");
   private static final String TEXT_CRYSTALAURARENDER_58 = new String("crystalAuraRender");
   private static final float PLACEMENT_MARKER_LIFETIME_MS = 300.0F;
   private static final float PLACEMENT_MARKER_INITIAL_SCALE = 0.4F;
   private static final float PLACEMENT_MARKER_SCALE_GROWTH = 0.6F;
   private static final float PLACEMENT_MARKER_INITIAL_ALPHA = 200.0F;
   private static final float OUTLINE_WIDTH = 1.5F;
   private static final long DIG_MARKER_HOLD_MS = 140L;
   private static final long DIG_MARKER_FADE_DELAY_MS = 140L;
   private static final float DIG_MARKER_FADE_MS = 160.0F;
   private static final float DIG_MARKER_BASE_SCALE = 0.55F;
   private static final float DIG_MARKER_PROGRESS_SCALE = 0.45F;
   private static final float DIG_MARKER_ALPHA_BASE = 0.45F;
   private static final float DIG_MARKER_ALPHA_PROGRESS = 0.4F;
   private static final float DIG_MARKER_MAX_ALPHA = 255.0F;
   private static final float FILL_RED_DENOMINATOR = 255.0F;
   private static final float FILL_GREEN_DENOMINATOR = 255.0F;
   private static final float FILL_BLUE_DENOMINATOR = 255.0F;
   private static final float FILL_ALPHA_DENOMINATOR = 255.0F;
   private static final float OUTLINE_RED_DENOMINATOR = 255.0F;
   private static final float OUTLINE_GREEN_DENOMINATOR = 255.0F;
   private static final float OUTLINE_BLUE_DENOMINATOR = 255.0F;
   private static final float OUTLINE_ALPHA_DENOMINATOR = 255.0F;
   private static final double BOX_HALF_SIZE = 0.5;
   private static final float MIN_BOX_SCALE = 0.05F;
   private static final double BOX_CENTER_X = 0.5;
   private static final double BOX_CENTER_Y = 0.5;
   private static final double BOX_CENTER_Z = 0.5;
   private static final int RGB_MASK = 16777215;
   private static final String TEXT_CRYSTALAURAMODECUSTOM_59 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_60 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_61 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_62 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_63 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_64 = new String("crystalAuraModeCustom");
   private static final String TEXT_CRYSTALAURAMODECUSTOM_65 = new String("crystalAuraModeCustom");

   private static int argb(int r,int g,int b,int a) { return a<<24|r<<16|g<<8|b; }

   public float getRenderProgress() {
      return this.renderProgress;
   }

   private List<Player> collectNearbyPlayers(Vec3 var1) {
      ArrayList var2 = new ArrayList();
      float var3 = this.getTargetRange() + COLLATERAL_RANGE_EXTRA;
      double var4 = var3 * var3;

      for (Player var7 : mc.level.players()) {
         if (var7 != mc.player
            && !(var7 instanceof LocalPlayer)
            && var7.isAlive()
            && !var7.isSpectator()
            && dev.syntrix.clienttest.client.visual.VisualFriends.contains(var7)
            && CombatMath.squaredDistanceToBox(var1, var7.getBoundingBox()) <= var4) {
            var2.add(var7);
         }
      }

      return var2;
   }

   public BlockPos getLastCrystalBase() {
      return this.lastCrystalBase;
   }

   private float getMinDamage() {
      return this.isRwMode() ? RW_MIN_DAMAGE : this.minDamage.getValue();
   }

   @Override
   public boolean prepareAction() {

      this.refreshTargetLists();
      if (this.targets.isEmpty()) {
         return false;
      }


      if (!this.options.isEnabled(TEXT_CRYSTALAURAAUTODIG_33) && this.miningTarget != null) {
         this.clearMiningTarget();
      }

      if (this.miningTarget != null && this.shouldSkipObsidianDig(mc.level.getBlockState(this.miningTarget))) {
         this.clearMiningTarget();
      }

      if (this.scoredCrystal != null) {
         boolean var1 = this.prepareCrystalAttack(this.scoredCrystal.crystal(), false);
         this.clearMiningTarget();
         if (!var1) {
            this.releaseActionPriority();
         }

         return var1 && this.finalizePreparedAction();
      } else {
         if (this.preparePendingCrystalAttack(this.selfDamageLimit) && this.hasPendingAction()) {
            return this.finalizePreparedAction();
         }

         if (this.prepareSelfShield(this.targets)) {
            this.clearMiningTarget();
            return this.finalizePreparedAction();
         }

         this.selectPlacementOrDigAction(this.targets, this.collateralPlayers, mc.player.getEyePosition(), this.selfDamageLimit);
         if (!this.hasPendingAction()) {
            this.releaseActionPriority();
         }

         return this.hasPendingAction() && this.finalizePreparedAction();
      }
   }

   private EndCrystal findCrystalAt(BlockPos var1) {
      AABB var2 = new AABB(var1.getX(), var1.getY(), var1.getZ(), var1.getX() + 1.0, var1.getY() + CRYSTAL_SEARCH_HEIGHT, var1.getZ() + 1.0);
      List var3 = mc.level.getEntitiesOfClass(EndCrystal.class, var2, var1x -> var1x.isAlive() && !this.isTrackedCrystal(var1x));
      return var3.isEmpty() ? null : (EndCrystal)var3.get(0);
   }

   @Subscribe
   private void onMovementReceipt(MovementReceiptEvent var1) {
      this.serverYaw = var1.getYaw();
      this.serverPitch = var1.getPitch();
      this.movementReceipt
         .observe(
            this.preparedTick,
            mc.player == null ? null : mc.player.position(),
            var1.getRotationKnown(),
            var1.getPositionSynchronized(),
            var1.getConnectionOpen()
         );
   }

   public NumberSetting getBreakRangeSetting() {
      return this.breakRange;
   }

   public List<CrystalAura.RenderMarker> getRenderMarkers() {
      return this.renderMarkers;
   }

   public NumberSetting getPredictionTicksSetting() {
      return this.predictionTicks;
   }

   @Override
   public int getPreparationPriority() {

      this.refreshTargetLists();
      if (this.targets.isEmpty()) {
         if (this.miningTarget != null) {
            this.resetPlan();
         }

         return 0;
      } else if (this.scoredCrystal != null) {
         return 90;
      } else {
         return this.options.isEnabled(TEXT_CRYSTALAURAYIELDWEBTRAP_31) ? 10 : 21;
      }
   }

   private boolean isDelayElapsed() {
      long var1 = (long)this.getActionDelayMs();
      return var1 <= 0L || System.currentTimeMillis() - this.lastActionTime >= var1;
   }

   public BlockPos getRenderPosition() {
      return this.renderPosition;
   }



   private void resetPlan() {
      this.plannedBasePosition = null;
      this.miningAttempts = 0;
      this.restoreHotbar();
      this.clearMiningTarget();
      this.releaseActionPriority();
   }



   private void releaseActionPriority() {
      this.releaseRotation();
   }

   public Player getActiveTarget() {
      return this.activeTarget;
   }

   private boolean prepareCrystalAttack(EndCrystal var1, boolean var2) {
      if (this.isTrackedCrystal(var1)) {
         return false;
      } else {
         Vec3 var3 = this.findCrystalAimPoint(var1);
         if (var3 != null && this.acquireRotation(var3)) {
            this.clearPendingActions();
            this.pendingBreak = new CrystalAura.PendingCrystal(var1.getId(), var2, mc.player.tickCount);
            return true;
         } else {
            return false;
         }
      }
   }

   public int getPendingCrystalTick() {
      return this.pendingCrystalTick;
   }

   private void debugLog(String var1) {
      if (this.options.isEnabled(TEXT_CRYSTALAURALOGS_56)) {
         System.out.println("[CrystalAura] " + var1);
      }
   }

   private float getPlaceRange() {
      return this.isRwMode() ? RW_PLACE_RANGE : this.placeRange.getValue();
   }

   private List<Player> collectTargets(Vec3 var1) {
      ArrayList var2 = new ArrayList();
      double var3 = this.getTargetRange() * this.getTargetRange();


      for (Player var7 : mc.level.players()) {
         if (var7 != mc.player
            && !(var7 instanceof LocalPlayer)
            && var7.isAlive()
            && !var7.isSpectator()
            && !dev.syntrix.clienttest.client.visual.VisualFriends.contains(var7)

            && !(CombatMath.squaredDistanceToBox(var1, var7.getBoundingBox()) > var3)) {
            var2.add(var7);
         }
      }

      return var2;
   }

   public NumberSetting getDelaySetting() {
      return this.actionDelay;
   }

   public List<Player> getCollateralPlayers() {
      return this.collateralPlayers;
   }

   public boolean isLoggingEnabled() {
      return this.options.isEnabled(TEXT_CRYSTALAURALOGS_53);
   }

   public CrystalAura() {
      super(new ModuleMetadata(TEXT_CRYSTAL_AURA_1, ModuleCategory.Combat));
      this.mode = new ModeSetting(TEXT_CRYSTALAURAMODE_2, TEXT_CRYSTALAURAMODECUSTOM_3, TEXT_CRYSTALAURAMODECUSTOM_4, TEXT_CRYSTALAURAMODERW_5);
      this.targetRange = (NumberSetting)new NumberSetting(
            TEXT_CRYSTALAURATARGETRANGE_6, DEFAULT_TARGET_RANGE, MIN_TARGET_RANGE, MAX_TARGET_RANGE, TARGET_RANGE_STEP
         )
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_65));
      this.placeRange = (NumberSetting)new NumberSetting(TEXT_CRYSTALAURAPLACERANGE_7, DEFAULT_PLACE_RANGE, MIN_PLACE_RANGE, MAX_PLACE_RANGE, PLACE_RANGE_STEP)
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_64));
      this.breakRange = (NumberSetting)new NumberSetting(TEXT_CRYSTALAURABREAKRANGE_8, DEFAULT_BREAK_RANGE, 1.0F, MAX_BREAK_RANGE, BREAK_RANGE_STEP)
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_63));
      this.minDamage = (NumberSetting)new NumberSetting(TEXT_CRYSTALAURAMINDAMAGE_9, DEFAULT_MIN_DAMAGE, 1.0F, MAX_MIN_DAMAGE, MIN_DAMAGE_STEP)
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_62));
      this.maxSelfDamage = (NumberSetting)new NumberSetting(
            TEXT_CRYSTALAURAMAXSELFDAMAGE_10, DEFAULT_MAX_SELF_DAMAGE, 0.0F, MAX_SELF_DAMAGE_VALUE, SELF_DAMAGE_STEP
         )
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_61));
      this.actionDelay = (NumberSetting)new NumberSetting(TEXT_CRYSTALAURADELAY_11, 0.0F, 0.0F, MAX_DELAY_MS, DELAY_STEP_MS)
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_60));
      this.predictionTicks = (NumberSetting)new NumberSetting(TEXT_CRYSTALAURAPREDICTTICKS_12, 2.0F, 0.0F, MAX_PREDICTION_TICKS, 1.0F)
         .visibleWhen(() -> this.mode.isMode(TEXT_CRYSTALAURAMODECUSTOM_59));
      this.options = new MultiBooleanSetting(
            TEXT_CRYSTALAURAOPTIONS_13,
            TEXT_CRYSTALAURAPLACEOBSIDIAN_14,
            TEXT_CRYSTALAURALEGIT_15,
            TEXT_CRYSTALAURAANTISUICIDE_16,
            TEXT_CRYSTALAURAAUTODIG_17,
            TEXT_CRYSTALAURASKIPOBSIDIANDIG_18,
            TEXT_CRYSTALAURAYIELDWEBTRAP_19,
            TEXT_CRYSTALAURARENDER_20,
            TEXT_CRYSTALAURALOGS_21,
            TEXT_CRYSTALAURASELFSHIELD_22
         )
         .enabledByDefault(TEXT_CRYSTALAURAPLACEOBSIDIAN_23)
         .enabledByDefault(TEXT_CRYSTALAURAANTISUICIDE_24)
         .enabledByDefault(TEXT_CRYSTALAURAAUTODIG_25)
         .enabledByDefault(TEXT_CRYSTALAURAYIELDWEBTRAP_26)
         .enabledByDefault(TEXT_CRYSTALAURARENDER_27);
      this.positionCooldowns = new HashMap<>();
      this.attackedCrystals = new HashMap<>();
      this.renderMarkers = new ArrayList<>();
      this.toolSlot = -1;
      this.originalHotbarSlot = -1;
      this.pendingCrystalTick = -100;
      this.lastCrystalPlaceTick = -100;
      this.lastActionTime = System.currentTimeMillis();
      this.rotationRequestTick = -100;
      this.lastRotationTick = -100;
      this.movementReceipt = new MovementReceipt();
      this.preparedTick = NO_INITIAL_PREPARED_TICK;
      this.lastExecutionTick = NO_INITIAL_EXECUTION_TICK;
      this.executionPreparedTick = NO_INITIAL_EXECUTION_PREPARED_TICK;
      this.targets = List.of();
      this.collateralPlayers = List.of();
      this.addSettings(
         this.mode,
         this.targetRange,
         this.placeRange,
         this.breakRange,
         this.minDamage,
         this.maxSelfDamage,
         this.actionDelay,
         this.predictionTicks,
         this.options
      );
   }

   private float getMaxSelfDamage() {
      return this.isRwMode() ? RW_MAX_SELF_DAMAGE : this.maxSelfDamage.getValue();
   }

   private boolean preparePendingCrystalAttack(float var1) {
      if (this.pendingCrystalBase == null) {
         return false;
      } else if (mc.player.tickCount - this.pendingCrystalTick > 20) {
         this.pendingCrystalBase = null;
         return false;
      } else {
         EndCrystal var2 = this.findCrystalAt(this.pendingCrystalBase);
         if (var2 == null) {
            return false;
         } else {
            float var3 = this.getBreakRange();
            if (CombatMath.squaredDistanceToBox(mc.player.getEyePosition(), var2.getBoundingBox()) > var3 * var3) {
               this.pendingCrystalBase = null;
               return false;
            } else if (CombatMath.crystalDamage(var2.position(), mc.player) >= var1) {
               this.pendingCrystalBase = null;
               return false;
            } else {
               return this.prepareCrystalAttack(var2, true);
            }
         }
      }
   }

   public int getDigNoProgressTicks() {
      return this.digNoProgressTicks;
   }

   private boolean isPositionCoolingDown(BlockPos var1) {
      Integer var2 = this.positionCooldowns.get(var1);
      if (var2 == null) {
         return false;
      }

      if (mc.player.tickCount <= var2) {
         return true;
      }

      this.positionCooldowns.remove(var1);
      return false;
   }

   private boolean ensurePreparedItemHeld() {
      if (this.pendingPlacement != null) {
         return this.hasHeldItem(this.pendingPlacement.item());
      } else {
         return this.pendingBreak != null && this.options.isEnabled(TEXT_CRYSTALAURALEGIT_34) ? this.hasHeldItem(Items.END_CRYSTAL) : true;
      }
   }

   public NumberSetting getMaxSelfDamageSetting() {
      return this.maxSelfDamage;
   }

   private void restoreHotbar() {
      if(this.originalHotbarSlot!=-1 && mc.player!=null && !ManualActionState.hasPendingAction()
            && !this.didExecuteThisTick() && this.selectHotbarSlot(this.originalHotbarSlot))this.originalHotbarSlot=-1;
   }

   public CombatMath.ScoredCrystal getScoredCrystal() {
      return this.scoredCrystal;
   }

   private BlockHitResult raycastSupport(BlockPos var1, Vec3 var2) {
      return this.raycastSupportWithRotation(var1, var2, ServerRotationState.serverYaw(), ServerRotationState.serverPitch());
   }

   private void restoreTool() {
      this.pendingDig = null;
      if (this.miningTarget != null && mc.gameMode != null && mc.gameMode.isDestroying()) {
         mc.gameMode.stopDestroyBlock();
      }

      this.miningTarget = null;
      if (this.toolSlot != -1) {

         this.toolSlot = -1;
      }

      this.originalHotbarSlot = -1;
   }

   @Subscribe
   private void onMovementInput(MovementInputEvent event) {
      if (!ServerRotationState.operation005() && mc.player != null && this.rotationRequestTick == mc.player.tickCount && !BaritoneCompat.isControllingMovement()) {
         MovementCorrection.correctInput(event, ServerRotationState.getYaw());
      }
   }

   public BlockPos getPlannedBasePosition() {
      return this.plannedBasePosition;
   }

   public int getOriginalHotbarSlot() {
      return this.originalHotbarSlot;
   }

   private AABB getAnimatedBox(BlockPos var1, float var2) {
      double var3 = BOX_HALF_SIZE * Mth.clamp(var2, MIN_BOX_SCALE, 1.0F);
      double var5 = var1.getX() + BOX_CENTER_X;
      double var7 = var1.getY() + BOX_CENTER_Y;
      double var9 = var1.getZ() + BOX_CENTER_Z;
      return new AABB(var5 - var3, var7 - var3, var9 - var3, var5 + var3, var7 + var3, var9 + var3);
   }

   private boolean isTrackedCrystal(EndCrystal var1) {
      CrystalAura.TrackedCrystal var2 = this.attackedCrystals.get(var1.getId());
      return var2 != null && var2.crystal() == var1 && mc.player.tickCount < var2.expiresTick();
   }

   private boolean acquireRotation(Vec3 var1) {
      if (this.lastRotationTick == mc.player.tickCount) {
         return false;
      }

      ActionCoordinator var2 = ActionCoordinator.INSTANCE;
      ActionCoordinator.ActionOwner var3 = var2.getCurrentOwner();
      if (var3 != ActionCoordinator.ActionOwner.NONE
         && var3 != ActionCoordinator.ActionOwner.CRYSTAL_AURA
         && var3.getPriority() >= ActionCoordinator.ActionOwner.CRYSTAL_AURA.getPriority()
         && var2.getRotationAge() <= 2) {
         return false;
      }

      if (ActionCoordinator.isBlockedByReservation(ActionCoordinator.ActionOwner.CRYSTAL_AURA)) {
         return false;
      }

      if (!ActionCoordinator.reserveOwner(ActionCoordinator.ActionOwner.CRYSTAL_AURA, 3)) {
         return false;
      }

      this.requestRotation(var1);
      if (ActionCoordinator.INSTANCE.getCurrentOwner() != ActionCoordinator.ActionOwner.CRYSTAL_AURA) {
         return false;
      }

      this.lastRotationTick = mc.player.tickCount;
      this.rotationRequestTick = mc.player.tickCount;
      return true;
   }

   public CrystalAura.PlacementAction getPendingPlacement() {
      return this.pendingPlacement;
   }

   public Player getChosenTarget() {
      return this.chosenTarget;
   }

   public MovementReceipt getMovementReceipt() {
      return this.movementReceipt;
   }

   private boolean filterTargetPlayer(Player var1) {
      return var1 != null && this.isPlayerInCobweb(var1) ? Math.abs(var1.blockPosition().getY() - mc.player.blockPosition().getY()) <= 1 : false;
   }

   private BlockHitResult resolvePlacementRaycast(CrystalAura.PlacementAction var1) {
      if (ServerActionState.isSupportUnconfirmed(var1.against())) {
         return null;
      }

      if (InventoryUtil.findHotbarItem(var1.item()) == -1) {
         return null;
      }

      boolean var2 = var1.crystal()
         ? CombatMath.isCrystalBase(mc.level.getBlockState(var1.target()))
            && mc.level.getBlockState(var1.target().above()).isAir()
            && !CombatMath.isCrystalSpaceOccupied(var1.target().above())
         : mc.level.getBlockState(var1.target()).canBeReplaced() && !CombatMath.isObsidianPlacementBlocked(var1.target());
      if (!var2) {
         return null;
      }

      BlockHitResult var3 = this.raycastSupportWithRotation(var1.against(), var1.aimPoint(), this.serverYaw, this.serverPitch);
      double var4 = Math.min(this.getPlaceRange(), mc.player.blockInteractionRange());
      return InteractionRaycast.hitsBlock(var3, var1.against())
            && !(mc.player.getEyePosition().distanceToSqr(var3.getLocation()) > var4 * var4)
            && (var1.crystal() || var3.getBlockPos().relative(var3.getDirection()).equals(var1.target()))
         ? var3
         : null;
   }

   private List<CombatMath.PredictedPlayer> predictPlayerBoxes(List<Player> var1) {
      ArrayList<Player> var2 = new ArrayList<>(var1);
      Vec3 var3 = mc.player.getEyePosition();
      var2.sort(Comparator.comparingDouble(var1x -> CombatMath.squaredDistanceToBox(var3, var1x.getBoundingBox())));
      ArrayList var4 = new ArrayList(Math.min(var2.size(), 6));
      float var5 = this.getPredictionTicks();

      for (int var6 = 0; var6 < Math.min(var2.size(), 6); var6++) {
         Player var7 = (Player)var2.get(var6);
         AABB var8 = var7.getBoundingBox();
         if (var5 > 0.0F && !this.isPlayerInCobweb(var7)) {
            Vec3 var9 = MotionPrediction.predictPosition(var7, var7.position(), var5);
            Vec3 var10 = var9.subtract(var7.position());
            if (var10.lengthSqr() > PREDICTION_MOVEMENT_EPSILON_SQ) {
               var8 = var8.move(var10);
            }
         }

         var4.add(new CombatMath.PredictedPlayer(var7, var8));
      }

      return var4;
   }

   private float getActionDelayMs() {
      return this.isRwMode() ? RW_DELAY_MS : this.actionDelay.getValue();
   }

   public int getToolSlot() {
      return this.toolSlot;
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   private float getTargetRange() {
      return this.isRwMode() ? RW_TARGET_RANGE : this.targetRange.getValue();
   }

   public BlockPos getPendingCrystalBase() {
      return this.pendingCrystalBase;
   }

   public float getSelfDamageLimit() {
      return this.selfDamageLimit;
   }

   private float getPredictionTicks() {
      return this.isRwMode() ? 1.0F : this.predictionTicks.getValue();
   }

   public BlockPos getMiningTarget() {
      return this.miningTarget;
   }

   public long getRenderTime() {
      return this.renderTime;
   }

   private float getBreakRange() {
      return this.isRwMode() ? RW_BREAK_RANGE : this.breakRange.getValue();
   }

   public boolean isYieldWebTrapEnabled() {
      return this.isEnabled() && this.options.isEnabled(TEXT_CRYSTALAURAYIELDWEBTRAP_52);
   }

   public Map<Integer, CrystalAura.TrackedCrystal> getAttackedCrystals() {
      return this.attackedCrystals;
   }

   @Override
   public void cancelPreparedAction() {
      this.executionPreparedTick = NO_CANCELLED_EXECUTION_TICK;
      this.crystalToBreak = null;
      this.preparedHit = null;
      this.clearPendingActions();
      this.releaseRotation();
      this.targetListsReady = false;
   }

   public int getLastRotationTick() {
      return this.lastRotationTick;
   }

   public boolean getTargetListsReady() {
      return this.targetListsReady;
   }

   private void selectPlacementOrDigAction(List<Player> var1, List<Player> var2, Vec3 var3, float var4) {
      boolean var5 = InventoryUtil.findHotbarItem(Items.END_CRYSTAL) != -1;
      boolean var6 = InventoryUtil.findHotbarItem(Items.OBSIDIAN) != -1;
      if (!var5) {
         if (!this.continueMiningPlan(var3)) {
            this.resetPlan();
         }
      } else {
         List var7 = this.predictPlayerBoxes(var1);
         float var8 = this.miningTarget != null ? this.getMiningProgress() : 0.0F;
         BlockPos var9 = this.lastCrystalBase != null && mc.player.tickCount - this.lastCrystalPlaceTick <= 20 ? this.lastCrystalBase : null;
         BlockPos var10 = this.plannedBasePosition != null ? this.plannedBasePosition : var9;
         CombatMath.ScoredPlacement var11 = CombatMath.findBestPlacement(
            var7,
            var3,
            this.getPlaceRange(),
            this.getBreakRange(),
            this.getMaxSelfDamage(),
            this.getMinDamage(),
            this.options.isEnabled(TEXT_CRYSTALAURAPLACEOBSIDIAN_35) && var6,
            this.options.isEnabled(TEXT_CRYSTALAURAAUTODIG_36),
            this.options.isEnabled(TEXT_CRYSTALAURASKIPOBSIDIANDIG_37),
            var4,
            var2,
            this.miningTarget,
            var8,
            var10,
            this.positionCooldowns.keySet(),
            this.filterTargetPlayer(this.chosenTarget)
         );
         if (this.options.isEnabled(TEXT_CRYSTALAURALOGS_38)) {
            this.debugLog(CombatMath.lastScanDiagnostics);
         }

         if (var11 == null) {
            if (!this.continueMiningPlan(var3)) {
               this.resetPlan();
            }
         } else {
            this.miningAttempts = 0;
            this.plannedBasePosition = var11.pos();
            if (var11.digPos() != null) {
               this.restoreHotbar();
               this.debugLog("-> DIG " + var11.digPos().toShortString());
               this.prepareDig(var11.digPos());
            } else {
               boolean var12;
               if (var11.needsObsidian()) {
                  var12 = this.prepareBlockPlacement(var11.pos(), Items.OBSIDIAN, true);
               } else {
                  var12 = this.prepareCrystalPlacement(var11.pos());
               }

               this.debugLog(
                  String.format(TEXT_S_S_QUEUED_B_39, var11.needsObsidian() ? TEXT_OBSIDIAN_40 : TEXT_CRYSTAL_41, var11.pos().toShortString(), var12)
               );
               this.clearMiningTarget();
               if (!var12) {
                  this.releaseActionPriority();
               }
            }
         }
      }
   }

   private void executePendingAction() {
      CrystalAura.PendingCrystal var1 = this.pendingBreak;
      CrystalAura.DigAction var2 = this.pendingDig;
      CrystalAura.PlacementAction var3 = this.pendingPlacement;
      this.clearPendingActions();
      if (this.options.isEnabled(TEXT_CRYSTALAURALOGS_42)) {
         this.debugLog("execute phase=update tick=" + mc.player.tickCount + " action=" + (var1 != null ? var1 : (var2 != null ? var2 : var3)));
      }

      try {
         if (var1 != null) {
            this.executeCrystalAttack(var1, this.serverYaw, this.serverPitch);
         } else if (var2 != null) {
            this.executeDig(var2, this.serverYaw, this.serverPitch);
         } else if (var3 != null) {
            this.executePlacement(var3, this.serverYaw, this.serverPitch);
         }
      } finally {
         this.releaseRotation();
      }
   }

   private int withAlpha(int var1, int var2) {
      return var1 & RGB_MASK | Mth.clamp(var2, 0, 255) << 24;
   }

   public long getLastActionTime() {
      return this.lastActionTime;
   }

   private void executePlacement(CrystalAura.PlacementAction var1, float var2, float var3) {
      if (this.isDelayElapsed() && this.validateNextTickReceipt(var1.preparedTick())) {
         int var4 = InventoryUtil.findHotbarItem(var1.item());
         if (var4 == -1) {
            this.releaseActionPriority();
         } else {
            boolean var5 = var1.crystal()
               ? CombatMath.isCrystalBase(mc.level.getBlockState(var1.target()))
                  && mc.level.getBlockState(var1.target().above()).isAir()
                  && !CombatMath.isCrystalSpaceOccupied(var1.target().above())
               : mc.level.getBlockState(var1.target()).canBeReplaced() && !CombatMath.isObsidianPlacementBlocked(var1.target());
            float var6 = this.getPlaceRange();
            if (var5 && (!var1.crystal() || !(CombatMath.squaredDistanceToBlock(mc.player.getEyePosition(), var1.target()) > var6 * var6))) {
               BlockHitResult var7 = this.preparedHit;
               if (!InteractionRaycast.hitsBlock(var7, var1.against())) {
                  this.releaseActionPriority();
               } else if (!var1.crystal() && !var7.getBlockPos().relative(var7.getDirection()).equals(var1.target())) {
                  this.releaseActionPriority();
               } else if (!var1.crystal() && mc.player.getEyePosition().distanceToSqr(var7.getLocation()) > var6 * var6) {
                  this.releaseActionPriority();
               } else if (!this.selectItemFromHotbar(var1.item())) {
                  this.releaseActionPriority();
               } else {
                  this.lastExecutionTick = mc.player.tickCount;
                  InteractionContextGuard.enter();

                  boolean var8;
                  try {
                     var8 = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, var7).consumesAction();
                  } finally {
                     InteractionContextGuard.exit();
                  }

                  if (var8) {
                     if(!var1.crystal())ServerActionState.predicted(var1.target());
                     this.swingMainHand();
                     this.markActionTime();
                     PlacementCooldowns.markPosition(var1.target());
                     if (var1.crystal()) {
                        PlacementCooldowns.markPosition(var1.target().above());
                     }

                     if (var1.crystal()) {
                        this.plannedBasePosition = null;
                        this.pendingCrystalBase = var1.target().above().immutable();
                        this.pendingCrystalTick = mc.player.tickCount;
                        this.addRenderMarker(var1.target().above(), true);
                     } else {
                        if (var1.rememberObsidian()) {
                           this.lastCrystalBase = var1.target().immutable();
                           this.lastCrystalPlaceTick = mc.player.tickCount;
                        }

                        this.addRenderMarker(var1.target(), false);
                     }
                  }

                  this.debugLog(
                     String.format(TEXT_S_S_PLACED_B_44, var1.crystal() ? TEXT_CRYSTAL_45 : var1.item().toString(), var1.target().toShortString(), var8)
                  );
                  if (!var8) {
                     this.releaseActionPriority();
                  }
               }
            } else {
               this.releaseActionPriority();
            }
         }
      } else {
         this.releaseActionPriority();
      }
   }



   private boolean equipMiningTool(BlockPos var1) {
      int var2 = CombatMath.findBestToolSlot(mc.level.getBlockState(var1));
      if (var2 == -1) {
         return true;
      }

      if (this.toolSlot == -1) {
         this.toolSlot = mc.player.getInventory().getSelectedSlot();

      }

      return this.selectHotbarSlot(var2);
   }

   private boolean validateNextTickReceipt(int var1) {
      boolean var2 = this.isEnabled()
         && this.isInGame()
         && CombatCoordinator.isActiveTick()
         && mc.gameMode != null
         && !mc.player.isSpectator()
         && !InventorySwapManager.isSwapInProgress(mc.player)
         && !ManualActionState.hasPendingAction()
         && this.lastExecutionTick != mc.player.tickCount
         && this.movementReceipt.isReadyForNextTick(var1, mc.player.tickCount, mc.player.position())
         && ActionCoordinator.INSTANCE.getCurrentOwner() == ActionCoordinator.ActionOwner.CRYSTAL_AURA;
      if (!var2 && this.options.isEnabled(TEXT_CRYSTALAURALOGS_51)) {
         this.debugLog(
            "execution-skipped prepared="
               + var1
               + " tick="
               + (mc.player == null ? -1 : mc.player.tickCount)
               + " receiptReady="
               + (mc.player != null && this.movementReceipt.isReadyForNextTick(var1, mc.player.tickCount, mc.player.position()))
               + " actionPending="
               + ManualActionState.hasPendingAction()
               + " owner="
               + ActionCoordinator.INSTANCE.getCurrentOwner()
               + " pendingTypes="
               + ManualActionState.describePendingTypes()
               + " window={"
               + this.movementReceipt.describeWindow(var1, mc.player == null ? -1 : mc.player.tickCount, mc.player == null ? null : mc.player.position())
               + "}"
         );
      }

      return var2;
   }

   public int getRotationRequestTick() {
      return this.rotationRequestTick;
   }

   public boolean didExecuteThisTick() {
      return mc.player != null && this.lastExecutionTick == mc.player.tickCount;
   }

   private void addRenderMarker(BlockPos var1, boolean var2) {
      if (this.options.isEnabled(TEXT_CRYSTALAURARENDER_57)) {
         this.renderMarkers.add(new CrystalAura.RenderMarker(var1.immutable(), var2, System.currentTimeMillis()));
      }
   }

   private void executeDig(CrystalAura.DigAction var1, float var2, float var3) {
      if (!this.validateNextTickReceipt(var1.preparedTick()) || this.miningTarget == null || !this.miningTarget.equals(var1.target())) {
         this.releaseActionPriority();
      } else if (!this.shouldSkipObsidianDig(mc.level.getBlockState(var1.target())) && CombatMath.isBreakable(mc.level.getBlockState(var1.target()))) {
         float var4 = this.getPlaceRange();
         if (CombatMath.squaredDistanceToBlock(mc.player.getEyePosition(), var1.target()) > var4 * var4) {
            this.clearMiningTarget();
            this.releaseActionPriority();
         } else {
            BlockHitResult var5 = this.preparedHit;
            if (!InteractionRaycast.hitsBlock(var5, var1.target())) {
               this.releaseActionPriority();
            } else {
               int var6 = CombatMath.findBestToolSlot(mc.level.getBlockState(var1.target()));
               if (var6 == -1 || mc.player.getInventory().getSelectedSlot() == var6) {
                  ((CrystalInteractionManagerAccessor)mc.gameMode).setBlockBreakingCooldown(0);
                  this.lastExecutionTick = mc.player.tickCount;
                  boolean var7 = mc.gameMode.continueDestroyBlock(var1.target(), var5.getDirection());
                  if (var7) {
                     PlacementCooldowns.markAction();
                     this.swingMainHand();
                  }

                  float var8 = this.getMiningProgress();
                  if (var8 < this.previousDigProgress - DIG_PROGRESS_EPSILON) {
                     this.digProgressRegressionCount++;
                  }

                  if (var8 > this.previousDigProgress) {
                     this.digNoProgressTicks = 0;
                  } else {
                     this.digNoProgressTicks++;
                  }

                  this.previousDigProgress = var8;
                  if (!CombatMath.isBreakable(mc.level.getBlockState(var1.target()))) {
                     this.miningTarget = null;
                  }

                  if (this.options.isEnabled(TEXT_CRYSTALAURARENDER_47)) {
                     this.renderPosition = var1.target();
                     this.renderProgress = var8;
                     this.renderTime = System.currentTimeMillis();
                  }

                  this.debugLog(
                     String.format(
                        TEXT_DIG_WANT_S_SIDE_S_CONT_B_PROG_2F_HELD_D_48,
                        var1.target().toShortString(),
                        var5.getDirection(),
                        var7,
                        var8,
                        mc.player.getInventory().getSelectedSlot()
                     )
                  );
               }
            }
         }
      } else {
         this.clearMiningTarget();
         this.releaseActionPriority();
      }
   }

   private void releaseRotation() {
      this.rotationRequestTick = -100;
      ActionCoordinator.releaseOwner(ActionCoordinator.ActionOwner.CRYSTAL_AURA);
   }

   public ModeSetting getModeSetting() {
      return this.mode;
   }

   @Override
   public boolean executePreparedAction() {
      if (this.prepared
         && mc.player != null
         && this.executionPreparedTick == mc.player.tickCount
         && this.lastExecutionTick != mc.player.tickCount
         && this.hasPendingAction()) {
         this.executionPreparedTick = NO_CONSUMED_EXECUTION_TICK;
         this.executePendingAction();
         return this.lastExecutionTick == mc.player.tickCount;
      } else {
         return false;
      }
   }

   private void swingMainHand() {
      if (mc.player != null) {
         mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   private EndCrystal resolvePendingCrystal(CrystalAura.PendingCrystal var1) {
      if (mc.level.getEntity(var1.entityId()) instanceof EndCrystal var3 && var3.isAlive() && !this.isTrackedCrystal(var3)) {
         float var4 = this.getBreakRange();
         float var5 = this.options.isEnabled(TEXT_CRYSTALAURAANTISUICIDE_54)
            ? Math.max(0.0F, mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0F)
            : UNLIMITED_EXECUTION_SELF_DAMAGE;
         return CombatMath.squaredDistanceToBox(mc.player.getEyePosition(), var3.getBoundingBox()) <= var4 * var4
               && this.canRaycastCrystal(var3, this.serverYaw, this.serverPitch)
               && CombatMath.crystalDamage(var3.position(), mc.player) < var5
            ? var3
            : null;
      } else {
         return null;
      }
   }

   private boolean prepareSelfShield(List<Player> var1) {
      if (this.options.isEnabled(TEXT_CRYSTALAURASELFSHIELD_46) && InventoryUtil.findHotbarItem(Items.OBSIDIAN) != -1) {
         Player var2 = null;
         double var3 = INITIAL_NEAREST_TARGET_DISTANCE;

         for (Player var6 : var1) {
            double var7 = mc.player.distanceToSqr(var6);
            if (var7 < var3) {
               var3 = var7;
               var2 = var6;
            }
         }

         if (var2 == null) {
            return false;
         }

         double var12 = var2.getX() - mc.player.getX();
         double var13 = var2.getZ() - mc.player.getZ();
         if (!(Math.sqrt(var12 * var12 + var13 * var13) > this.getPlaceRange() + SHIELD_RANGE_EXTRA) && !(Math.abs(var2.getY() - mc.player.getY()) > 1.0)) {
            Direction var9 = Math.abs(var12) > Math.abs(var13)
               ? (var12 > 0.0 ? Direction.EAST : Direction.WEST)
               : (var13 > 0.0 ? Direction.SOUTH : Direction.NORTH);
            BlockPos var10 = mc.player.blockPosition().relative(var9);
            if (mc.level.getBlockState(var10).canBeReplaced() && !CombatMath.isObsidianPlacementBlocked(var10)) {
               boolean var11 = this.prepareBlockPlacement(var10, Items.OBSIDIAN, false);
               this.debugLog("-> SHIELD " + var10.toShortString() + " queued=" + var11);
               if (!var11) {
                  this.releaseActionPriority();
               }

               return var11;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public List<Player> getTargets() {
      return this.targets;
   }

   private boolean prepareBlockPlacement(BlockPos var1, Item var2, boolean var3) {
      if (PlacementCooldowns.isCoolingDown(var1)) {
         return false;
      }

      CombatMath.PlacementSupport var4 = CombatMath.findPlacementSupport(var1, mc.player.getEyePosition(), this.getPlaceRange());
      if (var4 != null && !ServerActionState.isSupportUnconfirmed(var4.against()) && InventoryUtil.findHotbarItem(var2) != -1) {
         Rotation var5 = RotationUtil.fromDirection(var4.hitVec().subtract(mc.player.getEyePosition()));
         BlockHitResult var6 = this.raycastSupportWithRotation(var4.against(), var4.hitVec(), var5.getYaw(), var5.getPitch());
         if (!InteractionRaycast.hitsBlock(var6, var4.against()) || !var6.getBlockPos().relative(var6.getDirection()).equals(var1)) {
            return false;
         } else if (!this.acquireRotation(var4.hitVec())) {
            return false;
         } else {
            BlockHitResult var7 = this.raycastSupport(var4.against(), var4.hitVec());
            if (InteractionRaycast.hitsBlock(var7, var4.against()) && var7.getBlockPos().relative(var7.getDirection()).equals(var1)) {
               this.clearPendingActions();
               this.pendingPlacement = new CrystalAura.PlacementAction(
                  var1.immutable(), var4.against().immutable(), var4.hitVec(), var2, false, var3, mc.player.tickCount
               );
               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public int getMiningAttempts() {
      return this.miningAttempts;
   }

   private boolean prepareCrystalPlacement(BlockPos var1) {
      if (ServerActionState.isSupportUnconfirmed(var1)) {
         return false;
      }

      if (InventoryUtil.findHotbarItem(Items.END_CRYSTAL) == -1) {
         return false;
      }

      boolean var2 = var1.equals(this.lastCrystalBase) && mc.player.tickCount - this.lastCrystalPlaceTick <= 20;
      if (var2 || !PlacementCooldowns.isCoolingDown(var1) && !PlacementCooldowns.isCoolingDown(var1.above())) {
         Vec3 var3 = new Vec3(var1.getX() + BLOCK_CENTER_X, var1.getY() + BLOCK_CENTER_Y, var1.getZ() + BLOCK_CENTER_Z);
         if (!this.acquireRotation(var3)) {
            return false;
         }

         BlockHitResult var4 = this.raycastSupport(var1, var3);
         if (!InteractionRaycast.hitsBlock(var4, var1)) {
            return false;
         }

         this.clearPendingActions();
         this.pendingPlacement = new CrystalAura.PlacementAction(var1.immutable(), var1.immutable(), var3, Items.END_CRYSTAL, true, false, mc.player.tickCount);
         return true;
      } else {
         return false;
      }
   }

   private boolean hasPendingAction() {
      return this.pendingPlacement != null || this.pendingBreak != null || this.pendingDig != null;
   }





   public void beginTick() {
      this.prepared = false;
      this.targetListsReady = false;
      if (this.preparedPlayer != mc.player || this.preparedWorld != mc.level) {
         this.clearPendingActions();
         this.attackedCrystals.clear();
         this.movementReceipt.reset();
         this.preparedTick = this.lastExecutionTick = NO_WORLD_CHANGE_TICK;
         this.preparedPlayer = mc.player;
         this.preparedWorld = mc.level;
      }

      this.preparedTick = mc.player.tickCount;
      if (!this.isInGame() || mc.gameMode == null || mc.player.isSpectator() || mc.player.isPassenger() || mc.gui.screen() != null) {
         this.clearPendingActions();
         this.restoreHotbar();
         this.clearMiningTarget();
         this.releaseActionPriority();
         this.chosenTarget = null;
      } else if (InventorySwapManager.isSwapInProgress(mc.player)) {
         this.clearPendingActions();
         this.restoreTool();
         this.releaseActionPriority();
         this.chosenTarget = null;
      } else {
         CombatMath.debugEnabled = this.options.isEnabled(TEXT_CRYSTALAURALOGS_28);
         this.clearPendingCrystal();
         this.attackedCrystals
            .entrySet()
            .removeIf(
               var0 -> mc.player.tickCount >= var0.getValue().expiresTick()
                  || !var0.getValue().crystal().isAlive()
                  || mc.level.getEntity(var0.getKey()) != var0.getValue().crystal()
            );
         this.prepared = true;
      }
   }

   public MultiBooleanSetting getOptions() {
      return this.options;
   }

   public CrystalAura.DigAction getPendingDig() {
      return this.pendingDig;
   }

   private void executeCrystalAttack(CrystalAura.PendingCrystal var1, float var2, float var3) {
      if (this.isDelayElapsed() && this.validateNextTickReceipt(var1.preparedTick())) {
         EndCrystal var4 = this.crystalToBreak;
         if (var4 == null || mc.level.getEntity(var1.entityId()) != var4 || !var4.isAlive() || this.isTrackedCrystal(var4)) {
            this.releaseActionPriority();
         } else if (this.options.isEnabled(TEXT_CRYSTALAURALEGIT_43) && !this.selectItemFromHotbar(Items.END_CRYSTAL)) {
            this.releaseActionPriority();
         } else {
            this.lastExecutionTick = mc.player.tickCount;
            mc.gameMode.attack(mc.player, var4);
            this.attackedCrystals.put(var4.getId(), new CrystalAura.TrackedCrystal(var4, mc.player.tickCount + this.getCrystalTrackingTicks()));
            this.swingMainHand();
            this.markActionTime();
            this.clearMiningTarget();
            if (var1.clearsPendingCrystal()) {
               this.pendingCrystalBase = null;
            }
         }
      } else {
         this.releaseActionPriority();
      }
   }

   private void markPositionCooldown(BlockPos var1) {
      if (var1 != null && mc.player != null) {
         this.positionCooldowns.put(var1.immutable(), mc.player.tickCount + 10);
      }
   }

   private boolean ensureSlotItem(Item var1, int var2) {
      return mc.player != null && mc.player.getInventory().getSelectedSlot() == var2 && mc.player.getMainHandItem().is(var1);
   }

   public CrystalAura.PendingCrystal getPendingBreak() {
      return this.pendingBreak;
   }

   private void markActionTime() {
      this.lastActionTime = System.currentTimeMillis();
      PlacementCooldowns.markAction();
   }

   public void restoreSavedHotbarSlot() {
      this.restoreHotbar();
   }

   private void prepareDig(BlockPos var1) {
      if (var1 != null) {
         if (this.shouldSkipObsidianDig(mc.level.getBlockState(var1))) {
            this.clearMiningTarget();
         } else if (this.isPositionCoolingDown(var1)) {
            this.clearMiningTarget();
         } else if (!var1.equals(this.miningTarget) || this.digProgressRegressionCount < 4 && this.digNoProgressTicks <= 10) {
            if (!CombatMath.isBreakable(mc.level.getBlockState(var1))) {
               this.clearMiningTarget();
            } else {
               Vec3 var2 = Vec3.atCenterOf(var1);
               if (this.acquireRotation(var2)) {
                  if (!var1.equals(this.miningTarget)) {
                     if (this.miningTarget != null && mc.gameMode.isDestroying()) {
                        mc.gameMode.stopDestroyBlock();
                     }

                     this.miningTarget = var1.immutable();
                     this.digProgressRegressionCount = 0;
                     this.digNoProgressTicks = 0;
                     this.previousDigProgress = 0.0F;
                  }

                  if (this.equipMiningTool(var1)) {
                     BlockHitResult var3 = this.raycastSupport(var1, var2);
                     if (!InteractionRaycast.hitsBlock(var3, var1)) {
                        this.markPositionCooldown(var1);
                        this.clearMiningTarget();
                        this.releaseActionPriority();
                     } else {
                        this.clearPendingActions();
                        this.pendingDig = new CrystalAura.DigAction(var1.immutable(), mc.player.tickCount);
                     }
                  }
               }
            }
         } else {
            this.markPositionCooldown(var1);
            this.clearMiningTarget();
            this.releaseActionPriority();
         }
      }
   }

   @Override
   public int getExecutionPriority() {
      this.executionPreparedTick = NO_EVALUATED_EXECUTION_TICK;
      this.crystalToBreak = null;
      this.preparedHit = null;
      if (!this.prepared || this.lastExecutionTick == mc.player.tickCount || !this.hasPendingAction()) {
         return 0;
      }

      if (this.pendingDig == null && !this.isDelayElapsed()) {
         return 0;
      }


      Player var1 = null;
      if (var1 != null && var1 != this.activeTarget) {
         return 0;
      }

      int var2 = this.pendingBreak != null
         ? this.pendingBreak.preparedTick()
         : (this.pendingPlacement != null ? this.pendingPlacement.preparedTick() : this.pendingDig.preparedTick());
      if (!this.validateNextTickReceipt(var2)) {
         return 0;
      }

      if (this.pendingBreak != null) {
         if (this.options.isEnabled(TEXT_CRYSTALAURALEGIT_29) && !this.selectItemFromHotbar(Items.END_CRYSTAL)) {
            return 0;
         }

         this.crystalToBreak = this.resolvePendingCrystal(this.pendingBreak);
         if (this.crystalToBreak == null) {
            return 0;
         }

         this.executionPreparedTick = mc.player.tickCount;
         return 90;
      } else if (this.pendingPlacement == null) {
         float var3 = this.getPlaceRange();
         if (this.miningTarget != null
            && this.miningTarget.equals(this.pendingDig.target())
            && this.options.isEnabled(TEXT_CRYSTALAURAAUTODIG_30)
            && !this.shouldSkipObsidianDig(mc.level.getBlockState(this.miningTarget))
            && CombatMath.isBreakable(mc.level.getBlockState(this.miningTarget))
            && !(CombatMath.squaredDistanceToBlock(mc.player.getEyePosition(), this.miningTarget) > var3 * var3)) {
            this.preparedHit = this.raycastSupportWithRotation(this.miningTarget, Vec3.atCenterOf(this.miningTarget), this.serverYaw, this.serverPitch);
            if (!InteractionRaycast.hitsBlock(this.preparedHit, this.miningTarget)) {
               return 0;
            }

            this.executionPreparedTick = mc.player.tickCount;
            return 10;
         } else {
            return 0;
         }
      } else {
         if (!this.selectItemFromHotbar(this.pendingPlacement.item())) {
            return 0;
         }

         this.preparedHit = this.resolvePlacementRaycast(this.pendingPlacement);
         if (this.preparedHit == null) {
            return 0;
         }

         this.executionPreparedTick = mc.player.tickCount;
         return 10;
      }
   }

   private void refreshTargetLists() {
      if (!this.targetListsReady) {
         this.targetListsReady = true;
         this.scoredCrystal = null;
         this.targets = List.of();
         this.collateralPlayers = List.of();
         this.selfDamageLimit = 0.0F;
         if (this.prepared) {
            Vec3 var1 = mc.player.getEyePosition();
            this.targets = this.collectTargets(var1);
            Player var2 = null;
            if (var2 != null && this.targets.contains(var2)) {
               this.targets = List.of(var2);
            }

            if (!this.targets.isEmpty()) {
               this.chosenTarget = this.targets
                  .stream()
                  .min(Comparator.comparingDouble(var1x -> CombatMath.squaredDistanceToBox(var1, var1x.getBoundingBox())))
                  .orElseThrow();
               this.selfDamageLimit = this.options.isEnabled(TEXT_CRYSTALAURAANTISUICIDE_32)
                  ? Math.max(0.0F, mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0F)
                  : UNLIMITED_PLANNING_SELF_DAMAGE;
               this.collateralPlayers = this.collectNearbyPlayers(var1);
               this.scoredCrystal = CombatMath.findBestCrystal(
                  this.targets,
                  var1,
                  this.getBreakRange(),
                  this.getMaxSelfDamage(),
                  this.getMinDamage(),
                  this.selfDamageLimit,
                  this.collateralPlayers,
                  this.attackedCrystals.keySet(),
                  var1x -> this.findCrystalAimPoint(var1x) != null
               );
            }
         }
      }
   }

   public ClientLevel getPreparedWorld() {
      return this.preparedWorld;
   }

   public int getPreparedTick() {
      return this.preparedTick;
   }

   public int getExecutionPreparedTick() {
      return this.executionPreparedTick;
   }

   public float getPreviousDigProgress() {
      return this.previousDigProgress;
   }

   public int getLastCrystalPlaceTick() {
      return this.lastCrystalPlaceTick;
   }

   private boolean isPlayerInCobweb(Player var1) {
      return CobwebPrediction.intersectsMatchingBlock(var1.getBoundingBox(), var0 -> mc.level.getBlockState(var0).is(Blocks.COBWEB));
   }

   private boolean shouldSkipObsidianDig(BlockState var1) {
      return this.options.isEnabled(TEXT_CRYSTALAURASKIPOBSIDIANDIG_49) && var1.is(Blocks.OBSIDIAN);
   }

   private void clearPendingActions() {
      this.activeTarget = null;
      this.pendingPlacement = null;
      this.pendingBreak = null;
      this.pendingDig = null;
   }

   private boolean selectItemFromHotbar(Item var1) {
      int var2 = InventoryUtil.findHotbarItem(var1);
      return var2 != -1 && this.ensureSlotItem(var1, var2);
   }

   public void finishTick() {
      if (!this.hasPendingAction() && this.miningTarget == null) {
         this.clearMiningTarget();
         this.restoreSavedHotbarSlot();
         this.chosenTarget = null;
      }
   }

   private boolean selectHotbarSlot(int var1) {
      if (mc.player == null || var1 < 0 || var1 > 8) {
         return false;
      }

      if (mc.player.getInventory().getSelectedSlot() == var1) {
         return true;
      }

      if (CombatCoordinator.isActiveTick() && !this.didExecuteThisTick() && !ManualActionState.hasPendingAction() && mc.gameMode != null) {

         mc.player.getInventory().setSelectedSlot(var1);
         ((CrystalInteractionManagerAccessor)mc.gameMode).invokeSyncSelectedSlot();
         return true;
      } else {
         return false;
      }
   }

   private float getMiningProgress() {
      return mc.gameMode == null ? 0.0F : Mth.clamp(((CrystalInteractionManagerAccessor)mc.gameMode).getCurrentBreakingProgress(), 0.0F, 1.0F);
   }

   public LocalPlayer getPreparedPlayer() {
      return this.preparedPlayer;
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   public boolean getPrepared() {
      return this.prepared;
   }



   private boolean hasHeldItem(Item var1) {
      int var2 = InventoryUtil.findHotbarItem(var1);
      if (var2 == -1) {
         return false;
      }

      if (this.ensureSlotItem(var1, var2)) {
         return true;
      }

      if (ManualActionState.hasPendingAction()) {
         return false;
      }

      if (this.originalHotbarSlot == -1) {
         this.originalHotbarSlot = mc.player.getInventory().getSelectedSlot();
      }

      return !this.selectHotbarSlot(var2) ? false : this.ensureSlotItem(var1, var2);
   }

   @Subscribe
   public void onUpdate(UpdateEvent var1) {
      if (var1.getClass() == UpdateEvent.class) {
         CombatCoordinator.onTick();
      }
   }

   public int getDigProgressRegressionCount() {
      return this.digProgressRegressionCount;
   }

   private boolean canRaycastCrystal(EndCrystal var1, float var2, float var3) {
      double var4 = Math.min(this.getBreakRange(), mc.player.entityInteractionRange());
      return EntityRaycast.canHitEntity(var2, var3, var4, var1, true);
   }

   private void requestRotation(Vec3 var1) {
      Rotation var2 = RotationUtil.fromDirection(var1.subtract(mc.player.getEyePosition()));
      ActionCoordinator.requestRotation(
         var2,
         AIM_YAW_SPEED,
         AIM_PITCH_SPEED,
         RESET_YAW_SPEED,
         RESET_PITCH_SPEED,
         3,
         ActionCoordinator.ActionOwner.CRYSTAL_AURA.getPriority(),
         false,
         ActionCoordinator.RotationMode.OLD,
         ActionCoordinator.ActionOwner.CRYSTAL_AURA
      );
   }

   private void clearMiningTarget() {
      this.pendingDig = null;
      if (this.miningTarget != null) {
         if (!CombatCoordinator.isActiveTick()) {
            return;
         }

         if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
         }

         this.miningTarget = null;
      }

      if (this.toolSlot != -1 && this.selectHotbarSlot(this.toolSlot)) {

         this.toolSlot = -1;
      }
   }

   private void clearPendingCrystal() {
      if (!this.positionCooldowns.isEmpty()) {
         int var1 = mc.player.tickCount;
         this.positionCooldowns.values().removeIf(var1x -> var1 > var1x);
      }
   }

   private boolean isRwMode() {
      return this.mode.isMode(TEXT_CRYSTALAURAMODERW_55);
   }

   private Vec3 findCrystalAimPoint(EndCrystal var1) {
      if (!var1.isAlive()) {
         return null;
      }

      AABB var2 = var1.getBoundingBox();
      Vec3 var3 = mc.player.getEyePosition();
      Vec3 var4 = var2.getCenter();
      Vec3 var5 = new Vec3(
         Mth.clamp(var3.x, var2.minX + AIM_PADDING_MIN_X, var2.maxX - AIM_PADDING_MAX_X),
         Mth.clamp(var3.y, var2.minY + AIM_PADDING_MIN_Y, var2.maxY - AIM_PADDING_MAX_Y),
         Mth.clamp(var3.z, var2.minZ + AIM_PADDING_MIN_Z, var2.maxZ - AIM_PADDING_MAX_Z)
      );
      Vec3[] var6 = new Vec3[]{var4, var5, new Vec3(var4.x, var2.maxY - HIGH_AIM_PADDING_A, var4.z), new Vec3(var5.x, var2.maxY - HIGH_AIM_PADDING_B, var5.z)};

      for (Vec3 var10 : var6) {
         Rotation var11 = RotationUtil.fromDirection(var10.subtract(var3));
         if (this.canRaycastCrystal(var1, var11.getYaw(), var11.getPitch())) {
            return var10;
         }
      }

      return null;
   }

   public NumberSetting getPlaceRangeSetting() {
      return this.placeRange;
   }

   public boolean shouldSuppressVanillaMining() {
      if (this.isEnabled()
         && this.prepared
         && mc.player != null
         && mc.level != null
         && this.preparedPlayer == mc.player
         && this.preparedWorld == mc.level
         && mc.gui.screen() == null
         && !InventorySwapManager.isSwapInProgress(mc.player)
         && this.pendingDig != null
         && this.miningTarget != null
         && this.miningTarget.equals(this.pendingDig.target())
         && ActionCoordinator.INSTANCE.getCurrentOwner() == ActionCoordinator.ActionOwner.CRYSTAL_AURA) {
         long var1 = (long)mc.player.tickCount - this.pendingDig.preparedTick();
         return var1 >= 0L && var1 <= 1L;
      } else {
         return false;
      }
   }

   public NumberSetting getTargetRangeSetting() {
      return this.targetRange;
   }

   public NumberSetting getMinDamageSetting() {
      return this.minDamage;
   }

   private BlockHitResult raycastSupportWithRotation(BlockPos var1, Vec3 var2, float var3, float var4) {
      double var5 = mc.player.getEyePosition().distanceTo(var2) + 1.0;
      Vec3 var7 = mc.player.getEyePosition(1.0F);
      Vec3 var8 = var7.add(new Rotation(var3, var4).toDirection().scale(var5));
      return InteractionRaycast.raycastOutlineToTarget(mc.player, var7, var8, var1);
   }

   public boolean releaseInventoryOwnership(boolean var1) {
      if (this.isEnabled()) {
         return true;
      }

      if (!var1) {
         this.toolSlot = this.originalHotbarSlot = -1;
         this.miningTarget = null;

         return true;
      }

      this.clearMiningTarget();
      this.restoreHotbar();
      return this.miningTarget == null && this.toolSlot == -1 && this.originalHotbarSlot == -1;
   }

   private boolean finalizePreparedAction() {
      if (this.ensurePreparedItemHeld()) {
         this.activeTarget = this.chosenTarget;
         return true;
      } else {
         this.clearPendingActions();
         this.releaseActionPriority();
         return false;
      }
   }

   private int getCrystalTrackingTicks() {
      ClientPacketListener var1 = mc.getConnection();
      PlayerInfo var2 = var1 == null ? null : var1.getPlayerInfo(mc.player.getUUID());
      int var3 = var2 == null ? 0 : Math.max(0, var2.getLatency());
      return Mth.clamp((int)Math.ceil(var3 / TICK_DURATION_MS) + 2, 2, 12);
   }

   public Map<BlockPos, Integer> getPositionCooldowns() {
      return this.positionCooldowns;
   }

   /** Forget ownership tied to a discarded player/world without changing the new player's slot. */
   public void resetForWorldChange() {
      this.miningTarget=null;
      this.toolSlot=this.originalHotbarSlot=-1;
      this.onDisable();
   }

   @Override
   public void onDisable() {
      this.clearPendingActions();
      this.clearMiningTarget();
      this.restoreHotbar();
      if (this.miningTarget != null || this.toolSlot != -1 || this.originalHotbarSlot != -1) {
         CombatCoordinator.deferInventoryRestoration(this);
      }

      ActionCoordinator.releaseOwner(ActionCoordinator.ActionOwner.CRYSTAL_AURA);
      this.renderMarkers.clear();
      this.positionCooldowns.clear();
      this.attackedCrystals.clear();
      this.renderPosition = null;
      this.pendingCrystalBase = null;
      this.lastCrystalBase = null;
      this.plannedBasePosition = null;
      this.chosenTarget = null;
      this.miningAttempts = 0;
      this.rotationRequestTick = -100;
      this.lastRotationTick = -100;
      this.movementReceipt.reset();
      this.preparedPlayer = null;
      this.preparedWorld = null;
      this.preparedTick = this.lastExecutionTick = NO_DISABLED_TICK;
      CombatMath.debugEnabled = false;
      super.onDisable();
   }

   public BlockHitResult getPreparedHit() {
      return this.preparedHit;
   }

   public int getLastExecutionTick() {
      return this.lastExecutionTick;
   }

   private boolean continueMiningPlan(Vec3 var1) {
      if (this.miningTarget == null || this.miningAttempts >= 4) {
         return false;
      }

      if (!this.isPositionCoolingDown(this.miningTarget) && this.canMineBlock(mc.level.getBlockState(this.miningTarget))) {
         float var2 = this.getPlaceRange();
         if (CombatMath.squaredDistanceToBlock(var1, this.miningTarget) > var2 * var2) {
            return false;
         }

         this.miningAttempts++;
         this.prepareDig(this.miningTarget);
         return true;
      } else {
         return false;
      }
   }

   public EndCrystal getCrystalToBreak() {
      return this.crystalToBreak;
   }

   private boolean canMineBlock(BlockState var1) {
      return !this.shouldSkipObsidianDig(var1) && CombatMath.canAutoMine(var1, !this.options.isEnabled(TEXT_CRYSTALAURASKIPOBSIDIANDIG_50));
   }

   private record DigAction(BlockPos target, int preparedTick) {
   }

   private record PendingCrystal(int entityId, boolean clearsPendingCrystal, int preparedTick) {
   }

   private record PlacementAction(BlockPos target, BlockPos against, Vec3 aimPoint, Item item, boolean crystal, boolean rememberObsidian, int preparedTick) {
   }

   public record RenderMarker(BlockPos pos, boolean crystal, long time) {
   }

   private record TrackedCrystal(EndCrystal crystal, int expiresTick) {
   }
}
