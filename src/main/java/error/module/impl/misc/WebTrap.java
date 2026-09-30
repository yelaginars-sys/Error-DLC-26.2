package error.module.impl.misc;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.player.MoveUtility;
import error.util.render.Render3D;
import error.util.RotationHandler;
import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AutoTotem;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Create by daun kvass
 */
public final class WebTrap extends Module {

    public final ModeSetting mode = mode("Mode", "Silent", "Silent", "Packet");
    public final ModeSetting fillMode = mode("Fill", "All hitbox", "Only feet", "All hitbox");
    public final SliderSetting range = slider("Range", 4.5f, 1.0f, 6.0f, 0.5f);
    public final SliderSetting delay = slider("Delay", 1.0f, 0.0f, 10.0f, 1.0f);

    public final CheckBox predict = checkbox("Predict", true);
    public final SliderSetting predictTicks = slider("Tick predict", 2.0f, 0.0f, 6.0f, 0.5f).visible(predict::getValue);

    private final HeaderSetting renderHeader = header("Display");
    public final CheckBox render = checkbox("Render", true);
    public final ModeSetting renderType = mode("Mode", "Tile", "Tile", "Cube").visible(render::getValue);

    private final List<BlockPos> targetPositions = new ArrayList<>();
    private int prevSlot = -1;
    private boolean needsSwapBack = false;
    private boolean wasRotating = false;
    private int delayTimer = 0;

    public WebTrap() {
        super("WebTrap", "Застраивает цель паутиной", Category.MISC);
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

        delayTimer = 0;
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

        if (delayTimer > 0) {
            delayTimer--;
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

        int webSlot = findWebHotbarSlot(player);
        if (webSlot == -1) {
            targetPositions.clear();
            return;
        }

        Vec3 targetPos = getTargetPlacementPos(target);

        if (fillMode.is("Only Feet")) {
            placeAtFeet(targetPos, webSlot);
        } else {
            placeFullHeight(target, targetPos, webSlot);
        }

        delayTimer = delay.getValue().intValue();
    }

    private Vec3 getTargetPlacementPos(Player target) {
        if (!predict.getValue() || predictTicks.getValue() <= 0.0f) {
            return target.position();
        }
        Vec3 motion = target.getDeltaMovement();
        return target.position().add(motion.scale(predictTicks.getValue()));
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;

        if (wasRotating && RotationHandler.isActive()) {
            MoveUtility.fixMovement(event, RotationHandler.getFreeYaw());
        }
    }

    private Player findTarget() {
        if (mc.player == null || mc.level == null) return null;

        return mc.level.players().stream()
                .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
                .filter(p -> mc.player.distanceTo(p) <= range.getValue())
                .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
                .orElse(null);
    }

    private int findWebHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getItem(slot).is(Items.COBWEB)) {
                return slot;
            }
        }
        return -1;
    }

    private void placeAtFeet(Vec3 targetPos, int webSlot) {
        targetPositions.clear();
        BlockPos feetPos = new BlockPos(
                Mth.floor(targetPos.x),
                Mth.floor(targetPos.y),
                Mth.floor(targetPos.z)
        );

        if (mc.level.getBlockState(feetPos).isAir()) {
            targetPositions.add(feetPos);
            tryPlace(feetPos, webSlot);
        }
    }

    private void placeFullHeight(Player target, Vec3 predictedPos, int webSlot) {
        targetPositions.clear();
        List<BlockPos> blocksToPlace = new ArrayList<>();

        AABB box = target.getBoundingBox().move(predictedPos.subtract(target.position()));
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX - 0.001D);
        int maxY = Mth.floor(box.maxY - 0.001D);
        int maxZ = Mth.floor(box.maxZ - 0.001D);

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (mc.level.getBlockState(pos).isAir()) {
                        blocksToPlace.add(pos);
                    }
                }
            }
        }

        if (blocksToPlace.isEmpty()) {
            BlockPos feet = new BlockPos(Mth.floor(predictedPos.x), Mth.floor(predictedPos.y), Mth.floor(predictedPos.z));
            BlockPos head = feet.above();
            if (mc.level.getBlockState(feet).isAir()) blocksToPlace.add(feet);
            if (mc.level.getBlockState(head).isAir()) blocksToPlace.add(head);
        }

        targetPositions.addAll(blocksToPlace);

        if (mode.is("Packet")) {
            for (BlockPos pos : blocksToPlace) {
                tryPlace(pos, webSlot);
            }
        } else {
            for (BlockPos pos : blocksToPlace) {
                if (tryPlace(pos, webSlot)) {
                    break;
                }
            }
        }
    }

    private boolean tryPlace(BlockPos pos, int webSlot) {
        BlockState currentState = mc.level.getBlockState(pos);
        if (!currentState.isAir() && !currentState.canBeReplaced()) return false;

        Direction supportDir = null;
        BlockPos supportPos = null;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            BlockState neighborState = mc.level.getBlockState(neighbor);
            if (!neighborState.isAir()) {
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

        if (mode.is("Silent")) {
            placeSilent(webSlot, hitResult, hitVec);
        } else {
            placePacket(webSlot, hitResult);
        }

        return true;
    }

    private void placeSilent(int webSlot, BlockHitResult hitResult, Vec3 hitVec) {
        LocalPlayer player = player();
        if (player == null || mc.gameMode == null) return;

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
    }

    private void placePacket(int webSlot, BlockHitResult hitResult) {
        LocalPlayer player = player();
        if (player == null || mc.gameMode == null || mc.getConnection() == null) return;

        int currentSlot = player.getInventory().getSelectedSlot();

        if (currentSlot != webSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(webSlot));
        }

        mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
        player.swing(InteractionHand.MAIN_HAND);

        if (currentSlot != webSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(currentSlot));
        }
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