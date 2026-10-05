package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AntiBot;
import error.module.impl.combat.AutoTotem;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.RotationHandler;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.player.MoveUtility;
import error.util.render.Render3D;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * WebTrap module ported from Energy client.
 * Places cobwebs around predicted target position (Solo & Multi modes, RageMode).
 */
public final class WebTrap extends Module {

    public final ModeSetting mode = mode("Режим", "Solo", "Solo", "Multi");
    public final CheckBox rageMode = checkbox("RageMode", false);
    public final SliderSetting range = slider("Дистанция", 4.5f, 2.0f, 6.0f, 0.1f);

    private final HeaderSetting renderHeader = header("Отображение");
    public final CheckBox render = checkbox("Рендер", true);
    public final ModeSetting renderType = mode("Тип рендера", "Tile", "Tile", "Cube").visible(render::getValue);

    private final List<BlockPos> targetPositions = new ArrayList<>();
    private int prevSlot = -1;
    private boolean needsSwapBack = false;
    private boolean wasRotating = false;

    public WebTrap() {
        super("WebTrap", "Ставит паутину по предсказанной позиции цели", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        if (needsSwapBack && prevSlot != -1 && player() != null) {
            player().getInventory().setSelectedSlot(prevSlot);
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
        }

        if (wasRotating || RotationHandler.isActive()) {
            RotationHandler.disengage("Instant");
        }

        prevSlot = -1;
        needsSwapBack = false;
        wasRotating = false;
        targetPositions.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) return;

        if (AutoTotem.isSwapping()) {
            return;
        }

        LocalPlayer player = player();

        if (needsSwapBack) {
            if (prevSlot != -1 && player.getInventory().getSelectedSlot() != prevSlot) {
                player.getInventory().setSelectedSlot(prevSlot);
            }
            needsSwapBack = false;
            prevSlot = -1;

            if (wasRotating) {
                wasRotating = false;
                RotationHandler.disengage("Smooth");
            }
        }

        int webSlot = findWebHotbarSlot(player);
        if (webSlot == -1) {
            targetPositions.clear();
            return;
        }

        Player target = findTarget();
        if (target == null) {
            targetPositions.clear();
            if (wasRotating) {
                wasRotating = false;
                RotationHandler.disengage("Smooth");
            }
            return;
        }

        // Energy uses 7 ticks movement prediction
        BlockPos predictedBase = getPredictedBlockPos(target);
        List<BlockPos> placeQueue = calculateTrapPositions(predictedBase);
        targetPositions.clear();
        targetPositions.addAll(placeQueue);

        for (BlockPos pos : placeQueue) {
            if (tryPlace(pos, webSlot)) {
                if (!rageMode.getValue()) {
                    break;
                }
            }
        }
    }

    private BlockPos getPredictedBlockPos(Player target) {
        Vec3 motion = new Vec3(
                target.getX() - target.xOld,
                target.getY() - target.yOld,
                target.getZ() - target.zOld
        );
        Vec3 predicted = target.position().add(motion.scale(7.0));
        return BlockPos.containing(predicted.x, predicted.y, predicted.z);
    }

    private Player findTarget() {
        if (player() == null || mc.level == null) return null;

        Player bestTarget = null;
        double bestDistSq = Double.MAX_VALUE;
        double maxDist = range.getValue();

        for (Player other : mc.level.players()) {
            if (other == player() || !other.isAlive() || other.isSpectator()) continue;
            if (AntiBot.isBot(other)) continue;
            if (FriendManager.getInstance().isFriend(other)) continue;

            double dist = player().getEyePosition().distanceTo(other.getEyePosition());
            if (dist > maxDist) continue;

            if (!hasLineOfSight(other)) continue;

            double distSq = other.distanceToSqr(player());
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestTarget = other;
            }
        }

        return bestTarget;
    }

    private boolean hasLineOfSight(Player target) {
        Vec3 eye = player().getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        double dist = eye.distanceTo(targetEye);
        Vec3 dir = targetEye.subtract(eye).normalize();
        return target.getBoundingBox().clip(eye, eye.add(dir.scale(dist))).isPresent();
    }

    private List<BlockPos> calculateTrapPositions(BlockPos base) {
        List<BlockPos> positions = new ArrayList<>();
        addCandidate(positions, base);
        addCandidate(positions, base.above());

        if (mode.is("Multi")) {
            for (BlockPos pos : List.of(
                    base.east(), base.west(), base.south(), base.north(),
                    base.east().above(), base.west().above(), base.south().above(), base.north().above()
            )) {
                addCandidate(positions, pos);
            }
        } else {
            addCandidate(positions, base.above(2));
        }

        return positions;
    }

    private void addCandidate(List<BlockPos> list, BlockPos pos) {
        if (player() == null || mc.level == null) return;
        BlockPos playerPos = player().blockPosition();

        if (pos.equals(playerPos) || pos.equals(playerPos.above())) return;
        if (player().getBoundingBox().intersects(new AABB(pos))) return;

        BlockState state = mc.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return;
        if (state.is(Blocks.COBWEB)) return;

        list.add(pos);
    }

    private boolean tryPlace(BlockPos pos, int webSlot) {
        if (mc.level == null || player() == null) return false;

        Direction supportDir = null;
        BlockPos supportPos = null;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = mc.level.getBlockState(neighbor);
            if (!neighborState.isAir() && !neighborState.canBeReplaced()) {
                supportDir = dir.getOpposite();
                supportPos = neighbor;
                break;
            }
        }

        if (supportPos == null || supportDir == null) return false;

        Vec3 hitVec = Vec3.atCenterOf(supportPos).add(
                supportDir.getStepX() * 0.5D,
                supportDir.getStepY() * 0.5D,
                supportDir.getStepZ() * 0.5D
        );

        BlockHitResult hitResult = new BlockHitResult(hitVec, supportDir, supportPos, false);

        LocalPlayer player = player();
        if (player == null || mc.gameMode == null) return false;

        Vec3 eye = player.getEyePosition();
        double diffX = hitVec.x - eye.x;
        double diffY = hitVec.y - eye.y;
        double diffZ = hitVec.z - eye.z;
        double dist = Math.hypot(diffX, diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        RotationHandler.setRotation(yaw, pitch);
        wasRotating = true;

        int currentSlot = player.getInventory().getSelectedSlot();
        if (currentSlot != webSlot) {
            if (!needsSwapBack) {
                prevSlot = currentSlot;
            }
            player.getInventory().setSelectedSlot(webSlot);
            needsSwapBack = true;
        }

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
        player.swing(InteractionHand.MAIN_HAND);

        return true;
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;
        if (wasRotating && RotationHandler.isActive()) {
            MoveUtility.fixMovement(event, RotationHandler.getFreeYaw());
        }
    }

    private int findWebHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.COBWEB)) {
                return slot;
            }
        }
        return -1;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue() || targetPositions.isEmpty()) return;

        int accent = Theme.getAccentColor();
        Color fillCol = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 50);
        Color outCol = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 230);
        boolean isCube = renderType.is("Cube");

        for (BlockPos pos : targetPositions) {
            if (isCube) {
                Render3D.drawBox(pos, fillCol, outCol, true, true);
            } else {
                Render3D.drawTile(pos, fillCol, outCol, true, true);
            }
        }
    }
}