package error.module.impl.combat;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import error.util.client.clients.ColorUtil;
import error.util.player.MoveUtility;
import error.util.render.Render3D;
import error.util.render.Render3DUtil;
import error.util.RotationHandler;
import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;

public final class CrystalAura extends Module {
    public final SliderSetting targetRange = slider("Range search", 10.0f, 1.0f, 16.0f, 0.5f);
    public final SliderSetting range = slider("Range action", 4.5f, 1.0f, 6.0f, 0.1f);
    public final SliderSetting maxSelf = slider("Max damage yourself", 9.0f, 0.0f, 20.0f, 0.1f);
    public final SliderSetting minTargetDamage = slider("Min damage targets", 3.5f, 0.0f, 20.0f, 0.1f);
    public final SliderSetting predictTicks = slider("Tick predict", 1.5f, 0.0f, 5.0f, 0.5f);

    public final SliderSetting breakDelay = slider("Break delay", 0.0f, 0.0f, 5.0f, 1.0f);
    public final SliderSetting placeDelay = slider("Place delay", 0.0f, 0.0f, 5.0f, 1.0f);
    public final CheckBox fastExplode = checkbox("Instant", true);

    public final CheckBox anchor = checkbox("Anchor aura", true);
    public final CheckBox autoObsidian = checkbox("Place obsidian", true);

    private final HeaderSetting renderHeader = header("Render");
    public final CheckBox render = checkbox("Display", true);
    public final ModeSetting renderType = mode("Modes", "Tile", "Tile", "Cube").visible(render::getValue);

    private int lastSentSlot = -1;
    private int breakCooldownTicks = 0;
    private int placeCooldownTicks = 0;

    private BlockPos currentAnchorPos = null;
    private int anchorStageTicks = 0;

    private BlockPos lockObsidianPos = null;
    private int lockObsidianTicks = 0;

    private BlockPos renderPos = null;
    private int renderColor = 0;
    private int renderTicks = 0;


    public CrystalAura() {
        super("CrystalAura", "ЧЧЧЧ", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        resetState();
        if (mc.player != null) {
            lastSentSlot = mc.player.getInventory().getSelectedSlot();
        }
    }

    @Override
    protected void onDisable() {
        RotationHandler.disengage("Smooth");
        resetState();
    }

    @EventTarget
    public void onPlayerInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;
        if (RotationHandler.isActive()) {
            MoveUtility.fixMovement(event, RotationHandler.getFreeYaw());
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (!inGame() || player() == null || mc.level == null || mc.getConnection() == null || mc.gameMode == null) {
            resetState();
            return;
        }

        if (AutoTotem.isSwapping()) {
            resetRotation();
            return;
        }

        if (renderTicks > 0 && --renderTicks == 0) renderPos = null;
        if (breakCooldownTicks > 0) breakCooldownTicks--;
        if (placeCooldownTicks > 0) placeCooldownTicks--;

        if (lockObsidianPos != null && ++lockObsidianTicks > 4) {
            lockObsidianPos = null;
            lockObsidianTicks = 0;
        }

        Player target = findTarget();
        if (target == null) {
            resetRotation();
            currentAnchorPos = null;
            return;
        }

        if (breakCooldownTicks <= 0) {
            Action breakCrystal = findBestCrystalBreak(target);
            if (breakCrystal != null) {
                syncRotation(breakCrystal.lookVec(), true);
                setRender(breakCrystal.pos(), ColorUtil.rgba(255, 0, 50, 255));
                attack(breakCrystal.crystal());

                if (!fastExplode.getValue()) return;
            }
        }

        if (anchor.getValue() && canExplodeAnchors()) {
            if (currentAnchorPos != null) {
                if (!isBlockInReach(currentAnchorPos) || ++anchorStageTicks > 8) {
                    currentAnchorPos = null;
                    anchorStageTicks = 0;
                } else {
                    BlockState aState = mc.level.getBlockState(currentAnchorPos);
                    if (aState.is(Blocks.RESPAWN_ANCHOR)) {
                        int charge = aState.getValue(RespawnAnchorBlock.CHARGE);
                        BlockHitResult hit = getVisibleHitResult(currentAnchorPos);

                        if (hit != null) {
                            if (charge > 0) {
                                if (!syncRotation(hit.getLocation(), false)) return;
                                setRender(currentAnchorPos, ColorUtil.rgba(255, 140, 0, 255));
                                explodeAnchor(hit);
                                currentAnchorPos = null;
                                anchorStageTicks = 0;
                                return;
                            } else if (findGlowstoneSlot() != -1) {
                                if (!syncRotation(hit.getLocation(), false)) return;
                                setRender(currentAnchorPos, ColorUtil.rgba(255, 220, 0, 255));
                                chargeAnchor(hit);
                                return;
                            }
                        }
                    } else if (!aState.isAir() && !aState.canBeReplaced()) {
                        currentAnchorPos = null;
                    }
                }
            }

            Action readyAnchor = findExistingChargedAnchor(target);
            if (readyAnchor != null) {
                if (!syncRotation(readyAnchor.lookVec(), false)) return;
                setRender(readyAnchor.pos(), ColorUtil.rgba(255, 140, 0, 255));
                explodeAnchor(readyAnchor.hitResult());
                currentAnchorPos = null;
                return;
            }

            if (findGlowstoneSlot() != -1) {
                Action emptyAnchor = findExistingUnchargedAnchor(target);
                if (emptyAnchor != null) {
                    if (!syncRotation(emptyAnchor.lookVec(), false)) return;
                    setRender(emptyAnchor.pos(), ColorUtil.rgba(255, 220, 0, 255));
                    chargeAnchor(emptyAnchor.hitResult());
                    currentAnchorPos = emptyAnchor.pos();
                    anchorStageTicks = 0;
                    return;
                }
            }

            if (currentAnchorPos == null && findAnchorSlot() != -1 && findGlowstoneSlot() != -1 && placeCooldownTicks <= 0) {
                Action placeAnchor = findBestAnchorPlacement(target);
                if (placeAnchor != null) {
                    if (!syncRotation(placeAnchor.lookVec(), false)) return;
                    setRender(placeAnchor.pos(), ColorUtil.rgba(255, 140, 0, 255));
                    placeBlock(placeAnchor.hitResult(), findAnchorSlot());
                    currentAnchorPos = placeAnchor.pos();
                    anchorStageTicks = 0;
                    return;
                }
            }
        }

        if (placeCooldownTicks <= 0 && findCrystalSlot() != -1) {
            Action readyPlace = findReadyObsidianPlace(target);
            if (readyPlace != null) {
                if (!syncRotation(readyPlace.lookVec(), false)) return;

                setRender(readyPlace.pos(), ColorUtil.rgba(255, 50, 110, 255));
                placeCrystal(readyPlace.hitResult());
                return;
            }
        }

        if (lockObsidianPos != null) return;

        if (autoObsidian.getValue() && placeCooldownTicks <= 0 && findObsidianSlot() != -1 && findCrystalSlot() != -1) {
            Action placeObby = findBestObsidianPlacement(target);
            if (placeObby != null) {
                if (!syncRotation(placeObby.lookVec(), false)) return;

                setRender(placeObby.pos(), ColorUtil.rgba(140, 40, 255, 255));
                placeBlock(placeObby.hitResult(), findObsidianSlot());
                lockObsidianPos = placeObby.pos();
                lockObsidianTicks = 0;
                return;
            }
        }

        resetRotation();
    }

    private boolean syncRotation(Vec3 vec, boolean fastBreakMode) {
        if (vec == null) return false;
        float[] rots = calculateRotations(mc.player.getEyePosition(), vec);
        RotationHandler.setRotation(rots[0], rots[1]);

        if (fastBreakMode) return true;

        float curYaw = RotationHandler.isActive() ? RotationHandler.getServerYaw() : mc.player.getYRot();
        float curPitch = RotationHandler.isActive() ? RotationHandler.getServerPitch() : mc.player.getXRot();
        float yawDiff = Math.abs(Mth.wrapDegrees(rots[0] - curYaw));
        float pitchDiff = Math.abs(rots[1] - curPitch);

        return yawDiff <= 45.0f && pitchDiff <= 45.0f;
    }

    private void resetRotation() {
        RotationHandler.disengage("Smooth");
    }

    private void setRender(BlockPos pos, int color) {
        this.renderPos = pos;
        this.renderColor = color;
        this.renderTicks = 5;
    }

    private boolean canExplodeAnchors() {
        return mc.level != null && mc.level.dimension() != Level.NETHER;
    }

    private Action findBestCrystalBreak(Player target) {
        Action best = null;
        float maxDmg = 0f;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive() || crystal.isRemoved()) continue;

            Vec3 pos = crystal.position();
            if (!isPosInReach(pos.add(0, 0.5, 0))) continue;

            float targetDmg = calculateDamage(pos, target, 0.0, 6.0f, null);
            float selfDmg = calculateDamage(pos, mc.player, 0.0, 6.0f, null);

            if (isGoodDamage(targetDmg, selfDmg, target) && targetDmg > maxDmg) {
                maxDmg = targetDmg;
                best = new Action(ActionType.BREAK, crystal.blockPosition(), crystal, null, pos.add(0, 0.5, 0), targetDmg, selfDmg);
            }
        }
        return best;
    }

    private Action findExistingChargedAnchor(Player target) {
        List<BlockPos> spots = getTacticalSpots(target);
        Action best = null;
        float maxDmg = 0f;

        for (BlockPos pos : spots) {
            if (!isBlockInReach(pos)) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (!state.is(Blocks.RESPAWN_ANCHOR)) continue;

            if (state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                Vec3 center = Vec3.atCenterOf(pos);
                float targetDmg = calculateDamage(center, target, 0.0, 5.0f, pos);
                float selfDmg = calculateDamage(center, mc.player, 0.0, 5.0f, pos);

                if (isGoodDamage(targetDmg, selfDmg, target) && targetDmg > maxDmg) {
                    BlockHitResult hit = getVisibleHitResult(pos);
                    if (hit != null) {
                        maxDmg = targetDmg;
                        best = new Action(ActionType.EXPLODE_ANCHOR, pos.immutable(), null, hit, hit.getLocation(), targetDmg, selfDmg);
                    }
                }
            }
        }
        return best;
    }

    private Action findExistingUnchargedAnchor(Player target) {
        List<BlockPos> spots = getTacticalSpots(target);
        Action best = null;
        float maxDmg = 0f;

        for (BlockPos pos : spots) {
            if (!isBlockInReach(pos)) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (!state.is(Blocks.RESPAWN_ANCHOR)) continue;

            if (state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
                Vec3 center = Vec3.atCenterOf(pos);
                float targetDmg = calculateDamage(center, target, 0.0, 5.0f, pos);
                float selfDmg = calculateDamage(center, mc.player, 0.0, 5.0f, pos);

                if (isGoodDamage(targetDmg, selfDmg, target) && targetDmg > maxDmg) {
                    BlockHitResult hit = getVisibleHitResult(pos);
                    if (hit != null) {
                        maxDmg = targetDmg;
                        best = new Action(ActionType.CHARGE_ANCHOR, pos.immutable(), null, hit, hit.getLocation(), targetDmg, selfDmg);
                    }
                }
            }
        }
        return best;
    }

    private Action findReadyObsidianPlace(Player target) {
        List<BlockPos> spots = getTacticalSpots(target);
        Action best = null;
        float maxDmg = 0f;

        for (BlockPos obbyPos : spots) {
            if (!isBlockInReach(obbyPos)) continue;

            BlockState base = mc.level.getBlockState(obbyPos);
            BlockPos crystalSpace = obbyPos.above();

            if ((base.is(Blocks.OBSIDIAN) || base.is(Blocks.BEDROCK))
                    && (mc.level.getBlockState(crystalSpace).isAir() || mc.level.getBlockState(crystalSpace).canBeReplaced())
                    && !hasBlockingEntityForCrystal(obbyPos)) {

                Vec3 crystalVec = getCrystalVec(obbyPos);
                float dmgCurrent = calculateDamage(crystalVec, target, 0.0, 6.0f, null);
                float dmgPred = calculateDamage(crystalVec, target, predictTicks.getValue(), 6.0f, null);
                float targetDmg = Math.max(dmgCurrent, dmgPred);
                float selfDmg = calculateDamage(crystalVec, mc.player, 0.0, 6.0f, null);

                if (isGoodDamage(targetDmg, selfDmg, target) && targetDmg > maxDmg) {
                    BlockHitResult hit = getVisibleHitResult(obbyPos);
                    if (hit != null) {
                        maxDmg = targetDmg;
                        best = new Action(ActionType.PLACE_CRYSTAL, obbyPos.immutable(), null, hit, hit.getLocation(), targetDmg, selfDmg);
                    }
                }
            }
        }
        return best;
    }

    private Action findBestAnchorPlacement(Player target) {
        List<BlockPos> spots = getTacticalSpots(target);
        Action bestAnchor = null;
        float maxScore = 0f;

        for (BlockPos pos : spots) {
            if (!isBlockInReach(pos)) continue;
            if (!canPlaceBlockAt(pos)) continue;

            BlockHitResult hit = getPlacementHitResult(pos);
            if (hit == null) continue;

            Vec3 center = Vec3.atCenterOf(pos);
            float dmgCurrent = calculateDamage(center, target, 0.0, 5.0f, pos);
            float dmgPred = calculateDamage(center, target, predictTicks.getValue(), 5.0f, pos);
            float targetDmg = Math.max(dmgCurrent, dmgPred);
            float selfDmg = calculateDamage(center, mc.player, 0.0, 5.0f, pos);

            if (!isGoodDamage(targetDmg, selfDmg, target)) continue;

            float score = targetDmg;
            if (pos.getY() >= target.getY() + 1.0) score += 3.0f;

            if (score > maxScore) {
                maxScore = score;
                bestAnchor = new Action(ActionType.PLACE_ANCHOR, pos.immutable(), null, hit, hit.getLocation(), targetDmg, selfDmg);
            }
        }
        return bestAnchor;
    }

    private Action findBestObsidianPlacement(Player target) {
        List<BlockPos> spots = getTacticalSpots(target);
        Action bestObsidian = null;
        float maxObbyDmg = 0f;

        for (BlockPos obbyPos : spots) {
            if (!isBlockInReach(obbyPos)) continue;
            if (!canPlaceBlockAt(obbyPos)) continue;

            BlockPos crystalSpace = obbyPos.above();
            if ((mc.level.getBlockState(crystalSpace).isAir() || mc.level.getBlockState(crystalSpace).canBeReplaced())
                    && !hasBlockingEntityForCrystal(obbyPos)) {
                BlockHitResult hit = getPlacementHitResult(obbyPos);
                if (hit != null) {
                    Vec3 crystalVec = getCrystalVec(obbyPos);
                    float dmgCurrent = calculateDamage(crystalVec, target, 0.0, 6.0f, obbyPos);
                    float dmgPred = calculateDamage(crystalVec, target, predictTicks.getValue(), 6.0f, obbyPos);
                    float targetDmg = Math.max(dmgCurrent, dmgPred);
                    float selfDmg = calculateDamage(crystalVec, mc.player, 0.0, 6.0f, obbyPos);

                    if (isGoodDamage(targetDmg, selfDmg, target) && targetDmg > maxObbyDmg) {
                        maxObbyDmg = targetDmg;
                        bestObsidian = new Action(ActionType.PLACE_OBSIDIAN, obbyPos.immutable(), null, hit, hit.getLocation(), targetDmg, selfDmg);
                    }
                }
            }
        }
        return bestObsidian;
    }

    /**
     * Плотный поиск позиций вокруг и строго под ногами цели
     */
    private List<BlockPos> getTacticalSpots(Player target) {
        List<BlockPos> validSpots = new ArrayList<>();
        Set<BlockPos> set = new LinkedHashSet<>();

        BlockPos curFeet = target.blockPosition();
        Vec3 pred = predictEntityPosition(target, predictTicks.getValue());
        BlockPos predFeet = BlockPos.containing(pred);

        for (BlockPos center : List.of(curFeet, predFeet)) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        if (dx * dx + dz * dz > 9) continue;
                        set.add(center.offset(dx, dy, dz));
                    }
                }
            }
        }

        for (BlockPos p : set) {
            if (isBlockInReach(p)) {
                validSpots.add(p);
            }
        }

        validSpots.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(target.getX(), target.getY() - 0.5, target.getZ())));
        return validSpots;
    }

    private void switchToSlot(int slot) {
        if (slot < 0 || slot >= 9) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
        }
        if (lastSentSlot != slot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            lastSentSlot = slot;
        }
    }

    private void placeBlock(BlockHitResult hit, int slot) {
        if (slot == -1 || hit == null) return;

        switchToSlot(slot);
        InteractionHand hand = (slot == 40) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        mc.gameMode.useItemOn(mc.player, hand, hit);
        mc.player.swing(hand);
        placeCooldownTicks = breakDelay.getValue().intValue();
    }

    private void placeCrystal(BlockHitResult hit) {
        int slot = findCrystalSlot();
        if (slot == -1 || hit == null) return;

        switchToSlot(slot);
        InteractionHand hand = (slot == 40) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        mc.gameMode.useItemOn(mc.player, hand, hit);
        mc.player.swing(hand);
        placeCooldownTicks = placeDelay.getValue().intValue();
    }

    private void chargeAnchor(BlockHitResult hit) {
        int glowSlot = findGlowstoneSlot();
        if (glowSlot == -1 || hit == null) return;

        switchToSlot(glowSlot);
        InteractionHand hand = (glowSlot == 40) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        mc.gameMode.useItemOn(mc.player, hand, hit);
        mc.player.swing(hand);
        placeCooldownTicks = placeDelay.getValue().intValue();
    }

    private void explodeAnchor(BlockHitResult hit) {
        if (hit == null) return;
        int safeSlot = findNonGlowstoneSlot();
        switchToSlot(safeSlot);

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        breakCooldownTicks = breakDelay.getValue().intValue();
    }

    private void attack(EndCrystal crystal) {
        if (crystal == null || !crystal.isAlive() || crystal.isRemoved()) return;
        mc.gameMode.attack(mc.player, crystal);
        mc.player.swing(InteractionHand.MAIN_HAND);
        breakCooldownTicks = breakDelay.getValue().intValue();
    }

    private BlockHitResult getVisibleHitResult(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Direction[] faces = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN};
        BlockHitResult bestHit = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction face : faces) {
            Vec3 testPoint = new Vec3(
                    pos.getX() + 0.5 + face.getStepX() * 0.48,
                    pos.getY() + 0.5 + face.getStepY() * 0.48,
                    pos.getZ() + 0.5 + face.getStepZ() * 0.48
            );

            BlockHitResult hit = mc.level.clip(new ClipContext(eye, testPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) {
                double dist = eye.distanceToSqr(hit.getLocation());
                if (dist < bestDist && dist <= (range.getValue() * range.getValue())) {
                    bestDist = dist;
                    bestHit = hit;
                }
            }
        }
        return bestHit != null ? bestHit : new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private BlockHitResult getPlacementHitResult(BlockPos targetPos) {
        Vec3 eye = mc.player.getEyePosition();
        Direction[] sides = {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

        BlockHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction side : sides) {
            BlockPos neighbor = targetPos.relative(side);
            BlockState state = mc.level.getBlockState(neighbor);
            if (state.isAir() || state.canBeReplaced() || !state.isSolid()) continue;

            Direction clickFace = side.getOpposite();
            Vec3 testPoint = new Vec3(
                    neighbor.getX() + 0.5 + clickFace.getStepX() * 0.48,
                    neighbor.getY() + 0.5 + clickFace.getStepY() * 0.48,
                    neighbor.getZ() + 0.5 + clickFace.getStepZ() * 0.48
            );

            BlockHitResult hit = mc.level.clip(new ClipContext(eye, testPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(neighbor)) {
                double dist = eye.distanceToSqr(hit.getLocation());
                if (dist < bestDist && dist <= (range.getValue() * range.getValue())) {
                    bestDist = dist;
                    best = hit;
                }
            }
        }
        return best;
    }

    private boolean isGoodDamage(float targetDamage, float selfDamage, Player target) {
        if (targetDamage < minTargetDamage.getValue()) return false;
        if (selfDamage > maxSelf.getValue()) return false;

        if (target != null && targetDamage >= (target.getHealth() + target.getAbsorptionAmount())) {
            return selfDamage < (mc.player.getHealth() + mc.player.getAbsorptionAmount() - 0.5f);
        }

        return !(selfDamage >= targetDamage && selfDamage > 2.0f);
    }

    private float calculateDamage(Vec3 explosionCenter, Player target, double predTicks, float power, BlockPos ignoredBlock) {
        if (explosionCenter == null || target == null || mc.level == null) return 0.0f;

        AABB box = predictBoundingBox(target, predTicks);
        Vec3 targetFeet = new Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0);
        double maxDist = power * 2.0;
        double dist = explosionCenter.distanceTo(targetFeet);
        if (dist > maxDist) return 0.0f;

        double exposure = calculateExposure(explosionCenter, box, ignoredBlock);
        if (exposure <= 0.0) return 0.0f;

        double impact = (1.0 - dist / maxDist) * exposure;
        double rawDamage = (impact * impact + impact) / 2.0 * 7.0 * maxDist + 1.0;
        return (float) applyArmorAndResistance(target, rawDamage);
    }

    private double calculateExposure(Vec3 center, AABB box, BlockPos ignoredBlock) {
        int total = 0;
        int visible = 0;

        for (int x = 0; x <= 2; x++) {
            double px = Mth.lerp(x / 2.0, box.minX, box.maxX);
            for (int y = 0; y <= 2; y++) {
                double py = Mth.lerp(y / 2.0, box.minY, box.maxY);
                for (int z = 0; z <= 2; z++) {
                    double pz = Mth.lerp(z / 2.0, box.minZ, box.maxZ);
                    total++;
                    Vec3 targetPt = new Vec3(px, py, pz);

                    if (isPointVisible(center, targetPt, ignoredBlock)) {
                        visible++;
                    }
                }
            }
        }
        return total == 0 ? 0.0 : (double) visible / total;
    }

    private boolean isPointVisible(Vec3 from, Vec3 to, BlockPos ignored) {
        Vec3 currentFrom = from;
        for (int step = 0; step < 3; step++) {
            BlockHitResult hit = mc.level.clip(new ClipContext(currentFrom, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                return true;
            }

            if (ignored != null && hit.getBlockPos().equals(ignored)) {
                Vec3 dir = to.subtract(from).normalize();
                currentFrom = hit.getLocation().add(dir.scale(0.1));
                if (currentFrom.distanceToSqr(from) >= to.distanceToSqr(from)) {
                    return true;
                }
                continue;
            }
            return false;
        }
        return false;
    }

    private double applyArmorAndResistance(Player target, double rawDamage) {
        if (target == null || rawDamage <= 0.0) return 0.0;
        float armor = target.getArmorValue();
        double toughness = target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        double effectiveArmor = Mth.clamp(armor - rawDamage / (2.0 + toughness / 4.0), armor * 0.2, 20.0);
        double reduced = rawDamage * (1.0 - effectiveArmor / 25.0);

        MobEffectInstance res = target.getEffect(MobEffects.RESISTANCE);
        if (res != null) reduced *= Math.max(0.0, 1.0 - 0.2 * (res.getAmplifier() + 1));

        int epf = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !target.getItemBySlot(slot).isEmpty()) epf += 4;
        }
        return Math.max(0.0, reduced * (1.0 - Mth.clamp(epf, 0, 20) * 0.04));
    }

    private Vec3 predictEntityPosition(LivingEntity entity, double ticks) {
        if (entity == null || ticks <= 0.0) return entity != null ? entity.position() : Vec3.ZERO;
        Vec3 vel = entity.getDeltaMovement();
        double vy = entity.onGround() ? 0.0 : Mth.clamp(vel.y, -1.0, 1.0);
        return entity.position().add(
                Mth.clamp(vel.x, -0.6, 0.6) * ticks,
                vy * ticks,
                Mth.clamp(vel.z, -0.6, 0.6) * ticks
        );
    }

    private AABB predictBoundingBox(Player target, double ticks) {
        if (target == null) return new AABB(0, 0, 0, 0, 0, 0);
        return target.getBoundingBox().move(predictEntityPosition(target, ticks).subtract(target.position()));
    }

    private boolean isPosInReach(Vec3 pos) {
        if (pos == null || mc.player == null) return false;
        return mc.player.getEyePosition().distanceTo(pos) <= range.getValue();
    }

    private boolean isBlockInReach(BlockPos pos) {
        if (pos == null || mc.player == null) return false;
        Vec3 eye = mc.player.getEyePosition();
        double cx = Mth.clamp(eye.x, pos.getX(), pos.getX() + 1.0);
        double cy = Mth.clamp(eye.y, pos.getY(), pos.getY() + 1.0);
        double cz = Mth.clamp(eye.z, pos.getZ(), pos.getZ() + 1.0);
        return eye.distanceTo(new Vec3(cx, cy, cz)) <= range.getValue();
    }

    private boolean canPlaceBlockAt(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return false;

        AABB box = new AABB(pos);
        if (mc.player.getBoundingBox().intersects(box)) return false;

        return mc.level.getEntities((Entity) null, box, e -> e.isAlive() && !e.isRemoved()
                && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrb) && !(e instanceof Projectile)).isEmpty();
    }

    private boolean hasBlockingEntityForCrystal(BlockPos pos) {
        AABB box = new AABB(pos.getX(), pos.getY() + 1.0, pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
        return !mc.level.getEntities((Entity) null, box, e -> e.isAlive() && !e.isRemoved()
                && !(e instanceof ItemEntity) && !(e instanceof ExperienceOrb) && !(e instanceof Projectile)).isEmpty();
    }

    private int findCrystalSlot() {
        if (mc.player.getOffhandItem().is(Items.END_CRYSTAL)) return 40;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.END_CRYSTAL)) return i;
        return -1;
    }

    private int findObsidianSlot() {
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Blocks.OBSIDIAN.asItem())) return i;
        return -1;
    }

    private int findAnchorSlot() {
        if (mc.player.getOffhandItem().is(Blocks.RESPAWN_ANCHOR.asItem())) return 40;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Blocks.RESPAWN_ANCHOR.asItem())) return i;
        return -1;
    }

    private int findGlowstoneSlot() {
        if (mc.player.getOffhandItem().is(Items.GLOWSTONE)) return 40;
        for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.GLOWSTONE)) return i;
        return -1;
    }

    private int findNonGlowstoneSlot() {
        int cur = mc.player.getInventory().getSelectedSlot();
        ItemStack curItem = mc.player.getInventory().getItem(cur);
        if (isSafeAnchorExplodeItem(curItem)) return cur;

        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.TOTEM_OF_UNDYING) || s.is(Items.DIAMOND_SWORD) || s.is(Items.NETHERITE_SWORD)
                    || s.is(Items.NETHERITE_PICKAXE) || s.is(Items.DIAMOND_PICKAXE) || s.is(Items.GOLDEN_APPLE)) {
                return i;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (isSafeAnchorExplodeItem(s)) return i;
        }
        return cur;
    }

    private boolean isSafeAnchorExplodeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        return !stack.is(Items.GLOWSTONE) && !stack.is(Blocks.RESPAWN_ANCHOR.asItem());
    }

    private Vec3 getCrystalVec(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    private float[] calculateRotations(Vec3 eyes, Vec3 target) {
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double xz = Math.sqrt(dx * dx + dz * dz);
        return new float[]{Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F), Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, xz)), -90.0F, 90.0F)};
    }

    private Player findTarget() {
        return mc.level.players().stream()
                .filter(p -> p != mc.player && p.isAlive() && !p.isRemoved())
                .filter(p -> mc.player.distanceTo(p) <= targetRange.getValue())
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }

    private void resetState() {
        breakCooldownTicks = 0;
        placeCooldownTicks = 0;
        currentAnchorPos = null;
        anchorStageTicks = 0;
        lockObsidianPos = null;
        lockObsidianTicks = 0;
        renderPos = null;
        renderTicks = 0;
        lastSentSlot = -1;
        resetRotation();
    }



    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue() || renderPos == null) return;
        Color fillCol = new Color(255, 50, 0, 50);
        Color outCol = new Color(255, 50, 0, 240);

        if (renderType.is("Cube")) {
            Render3D.drawBox(renderPos, fillCol, outCol, true, true);
        } else {
            Render3D.drawTile(renderPos, fillCol, outCol, true, true);
        }
    }



    private enum ActionType {
        PLACE_CRYSTAL, BREAK, PLACE_OBSIDIAN, PLACE_ANCHOR, CHARGE_ANCHOR, EXPLODE_ANCHOR
    }

    private record Action(
            ActionType type, BlockPos pos, EndCrystal crystal, BlockHitResult hitResult, Vec3 lookVec, float targetDamage, float selfDamage
    ) {}

    public boolean isBusy() {
        if (!isEnabled() || mc.player == null || mc.level == null) return false;
        if (AutoTotem.isSwapping()) return false;
        if (currentAnchorPos != null) return true;
        Player target = findTarget();
        return target != null && (findBestCrystalBreak(target) != null || (anchor.getValue() && canExplodeAnchors() && findExistingChargedAnchor(target) != null));
    }
}