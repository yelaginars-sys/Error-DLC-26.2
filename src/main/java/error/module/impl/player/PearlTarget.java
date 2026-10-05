package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AuraModule;
import error.setting.impl.CheckBox;
import error.ui.hud.impl.DynamicIslandHud;
import error.util.RotationHandler;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.persiki.ChatUtil;
import error.util.player.InventoryUtil;
import error.util.render.Render3D;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PearlTarget extends Module {
    public static PearlTarget INSTANCE;

    public final CheckBox holyWorld = checkbox("Обход HolyWorld", true);
    public final CheckBox noEntityRaycast = checkbox("Не бросать через энтити", false);
    public final CheckBox renderTargetLanding = checkbox("Отображать точку падения", true);

    private final Set<UUID> processedPearls = new HashSet<>();

    private UUID activeTargetUuid = null;
    private Vec3 predictedTargetLanding = null;
    private Rotation bestRotation = null;
    private boolean isThrowing = false;

    private int throwStage = 0;
    private int originalSlot = -1;
    private int targetPearlSlot = -1;
    private int nextActionTick = 0;
    private int freezeTicks = 0;

    public record Rotation(float yaw, float pitch) {}
    private record ScoredRotation(Rotation rotation, Vec3 landing, double score) {}

    public PearlTarget() {
        super("PearlTarget", "Бросает эндер-жемчуг за таргетом из киллауры", Category.PLAYER);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        reset();
        super.onDisable();
    }

    private void reset() {
        activeTargetUuid = null;
        predictedTargetLanding = null;
        bestRotation = null;
        isThrowing = false;
        throwStage = 0;
        originalSlot = -1;
        targetPearlSlot = -1;
        nextActionTick = 0;
        freezeTicks = 0;
        processedPearls.clear();
        if (RotationHandler.isActive()) {
            RotationHandler.clear();
        }
    }

    @EventTarget
    public void onPlayerInput(PlayerInputEvent event) {
        if (freezeTicks > 0) {
            event.setMoveVector(new Vec2(0.0F, 0.0F));
            event.setKeyPresses(new Input(false, false, false, false, false, false, false));
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || mc.level == null) {
            reset();
            return;
        }

        if (freezeTicks > 0) {
            freezeTicks--;
        }

        // Keep aiming while throwing
        if (bestRotation != null && isThrowing) {
            RotationHandler.setRotation(bestRotation.yaw(), bestRotation.pitch());
        }

        // Throwing State Machine
        if (isThrowing) {
            handleThrowSequence();
            return;
        }

        LivingEntity auraTarget = AuraModule.INSTANCE != null && AuraModule.INSTANCE.isEnabled()
                ? AuraModule.INSTANCE.getTarget()
                : null;

        if (auraTarget == null || !auraTarget.isAlive()) {
            resetCurrentTarget();
            return;
        }

        // If target changed, reset
        if (activeTargetUuid != null && !activeTargetUuid.equals(auraTarget.getUUID())) {
            resetCurrentTarget();
        }

        if (player().getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) {
            return;
        }

        int pearlSlot = findPearlSlot();
        if (pearlSlot == -1) {
            return;
        }

        // Look for pearls thrown by the target
        Vec3 landing = detectTargetPearlLanding(auraTarget);
        if (landing == null) {
            return;
        }

        activeTargetUuid = auraTarget.getUUID();
        predictedTargetLanding = landing;

        // Calculate ballistic rotation to land at predicted spot
        bestRotation = calculateBestRotation(landing);
        if (bestRotation == null) {
            return;
        }

        RotationHandler.setRotation(bestRotation.yaw(), bestRotation.pitch());

        // Check obstacles
        if (noEntityRaycast.getValue()) {
            Vec3 eyePos = player().getEyePosition();
            if (hitsEntity(auraTarget, eyePos, landing)) {
                return;
            }
        }

        // Start throw sequence
        startThrowSequence(pearlSlot);
    }

    private void resetCurrentTarget() {
        activeTargetUuid = null;
        predictedTargetLanding = null;
        bestRotation = null;
        if (RotationHandler.isActive()) {
            RotationHandler.clear();
        }
    }

    private void startThrowSequence(int pearlSlot) {
        originalSlot = player().getInventory().getSelectedSlot();
        targetPearlSlot = pearlSlot;
        isThrowing = true;
        freezeTicks = 4;

        if (pearlSlot < 9) {
            if (holyWorld.getValue()) {
                // HolyWorld hotbar mode
                selectSlot(pearlSlot);
                throwStage = 6;
                nextActionTick = player().tickCount + 2;
            } else {
                // Packet swap hotbar mode
                sendSlotPacket(pearlSlot);
                useItem();
                sendSlotPacket(originalSlot);
                finishThrow();
            }
        } else {
            // Inventory pearl
            if (holyWorld.getValue()) {
                throwStage = 1;
                freezeTicks = 6;
            } else {
                swapInventoryToHotbar(pearlSlot, originalSlot);
                useItem();
                swapInventoryToHotbar(pearlSlot, originalSlot);
                finishThrow();
            }
        }
    }

    private void handleThrowSequence() {
        if (player().tickCount < nextActionTick) return;

        switch (throwStage) {
            case 6 -> { // HolyWorld hotbar: use item
                useItem();
                throwStage = 5;
                nextActionTick = player().tickCount + 2;
            }
            case 5 -> { // HolyWorld hotbar: restore slot
                selectSlot(originalSlot);
                finishThrow();
            }
            case 1 -> { // HolyWorld inv: initiate swap
                throwStage = 2;
            }
            case 2 -> { // HolyWorld inv: swap to hotbar
                swapInventoryToHotbar(targetPearlSlot, originalSlot);
                closeScreen();
                throwStage = 3;
                nextActionTick = player().tickCount + 2;
            }
            case 3 -> { // HolyWorld inv: use item
                useItem();
                throwStage = 4;
                nextActionTick = player().tickCount + 3;
            }
            case 4 -> { // HolyWorld inv: swap back
                swapInventoryToHotbar(targetPearlSlot, originalSlot);
                closeScreen();
                finishThrow();
            }
        }
    }

    private void finishThrow() {
        isThrowing = false;
        throwStage = 0;
        originalSlot = -1;
        targetPearlSlot = -1;
        bestRotation = null;
        predictedTargetLanding = null;
        activeTargetUuid = null;
        if (RotationHandler.isActive()) {
            RotationHandler.clear();
        }
        ChatUtil.success("Перл успешно брошен за таргетом!");
        DynamicIslandHud.showNotification("Перл брошен за таргетом!", true, 3000L);
    }

    private void useItem() {
        if (mc.gameMode != null && player() != null) {
            mc.gameMode.useItem(player(), InteractionHand.MAIN_HAND);
            player().swing(InteractionHand.MAIN_HAND);
        }
    }

    private void selectSlot(int slot) {
        if (player() != null && slot >= 0 && slot < 9) {
            player().getInventory().setSelectedSlot(slot);
            sendSlotPacket(slot);
        }
    }

    private void sendSlotPacket(int slot) {
        if (mc.getConnection() != null && slot >= 0 && slot < 9) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    private void swapInventoryToHotbar(int invSlot, int hotbarSlot) {
        if (mc.gameMode != null && player() != null) {
            mc.gameMode.handleContainerInput(player().inventoryMenu.containerId, invSlot, hotbarSlot, ContainerInput.SWAP, player());
        }
    }

    private void closeScreen() {
        if (mc.getConnection() != null && player() != null) {
            mc.getConnection().send(new ServerboundContainerClosePacket(player().inventoryMenu.containerId));
        }
    }

    private int findPearlSlot() {
        if (player() == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (player().getInventory().getItem(i).is(Items.ENDER_PEARL)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (player().getInventory().getItem(i).is(Items.ENDER_PEARL)) {
                return i;
            }
        }
        return -1;
    }

    private Vec3 detectTargetPearlLanding(LivingEntity auraTarget) {
        if (processedPearls.size() > 256) {
            processedPearls.clear();
        }

        AABB searchBox = auraTarget.getBoundingBox().inflate(12.0);
        for (Entity entity : mc.level.getEntities(auraTarget, searchBox)) {
            if (entity instanceof ThrownEnderpearl pearl && pearl.tickCount <= 3) {
                UUID pearlUuid = pearl.getUUID();
                if (!processedPearls.contains(pearlUuid)) {
                    Entity owner = pearl.getOwner();
                    if (owner == null) {
                        Player closest = null;
                        for (Player p : mc.level.players()) {
                            if (closest == null || p.distanceToSqr(pearl) < closest.distanceToSqr(pearl)) {
                                closest = p;
                            }
                        }
                        owner = closest;
                    }

                    if (owner != null && owner.getUUID().equals(auraTarget.getUUID())) {
                        processedPearls.add(pearlUuid);
                        return simulatePearlFlight(pearl.position(), pearl.getDeltaMovement(), pearl);
                    }
                }
            }
        }
        return null;
    }

    private Rotation calculateBestRotation(Vec3 targetLanding) {
        Vec3 eyePos = player().getEyePosition().subtract(0.0, 0.1, 0.0);
        Vec3 diff = targetLanding.subtract(eyePos);

        float baseYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        baseYaw = Mth.wrapDegrees(baseYaw);

        ScoredRotation best = null;

        // Coarse grid search
        for (float yawOffset = -8.0F; yawOffset <= 8.0F; yawOffset += 2.0F) {
            for (float pitch = -89.0F; pitch <= 89.0F; pitch += 3.0F) {
                best = testRotation(best, baseYaw + yawOffset, pitch, targetLanding);
            }
        }

        if (best == null) {
            double distH = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
            float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, distH)));
            return new Rotation(baseYaw, Mth.clamp(pitch, -89.0F, 89.0F));
        }

        // Fine grid refinement
        float[] steps = new float[]{1.0F, 0.35F, 0.12F, 0.04F};
        for (float step : steps) {
            Rotation cur = best.rotation();
            ScoredRotation refined = best;
            for (int dy = -4; dy <= 4; dy++) {
                for (int dp = -4; dp <= 4; dp++) {
                    refined = testRotation(refined, cur.yaw() + dy * step, cur.pitch() + dp * step, targetLanding);
                }
            }
            best = refined;
        }

        return best.rotation();
    }

    private ScoredRotation testRotation(ScoredRotation currentBest, float yaw, float pitch, Vec3 targetLanding) {
        pitch = Mth.clamp(pitch, -89.0F, 89.0F);
        yaw = Mth.wrapDegrees(yaw);
        Rotation rot = new Rotation(yaw, pitch);

        Vec3 simulatedLanding = simulateOwnPearlFlight(rot);
        double distSq = simulatedLanding.distanceToSqr(targetLanding);

        if (currentBest == null || distSq < currentBest.score()) {
            return new ScoredRotation(rot, simulatedLanding, distSq);
        }
        return currentBest;
    }

    private Vec3 simulateOwnPearlFlight(Rotation rot) {
        Vec3 eyePos = player().getEyePosition().subtract(0.0, 0.1, 0.0);
        float yawRad = (float) Math.toRadians(rot.yaw());
        float pitchRad = (float) Math.toRadians(rot.pitch());

        double vx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double vy = -Mth.sin(pitchRad);
        double vz = Mth.cos(yawRad) * Mth.cos(pitchRad);

        Vec3 dir = new Vec3(vx, vy, vz).normalize().scale(1.5D);
        Vec3 motion = dir.add(player().getDeltaMovement().x, player().onGround() ? 0.0 : player().getDeltaMovement().y, player().getDeltaMovement().z);

        return simulatePearlFlight(eyePos, motion, player());
    }

    private Vec3 simulatePearlFlight(Vec3 startPos, Vec3 velocity, Entity source) {
        if (mc.level == null) return startPos;
        Vec3 current = startPos;
        Vec3 vel = velocity;
        Vec3 prev = startPos;

        for (int i = 0; i <= 300; i++) {
            prev = current;
            current = current.add(vel);

            boolean inWater = mc.level.getBlockState(BlockPos.containing(current)).is(Blocks.WATER);
            double drag = inWater ? 0.8D : 0.99D;
            vel = vel.scale(drag).add(0.0D, -0.03D, 0.0D);

            ClipContext context = new ClipContext(prev, current, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source);
            BlockHitResult hit = mc.level.clip(context);
            if (hit.getType() == HitResult.Type.BLOCK || current.y <= mc.level.getMinY()) {
                return hit.getLocation();
            }
        }
        return prev;
    }

    private boolean hitsEntity(Entity target, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(0.3);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                player(),
                start,
                end,
                box,
                e -> !e.isSpectator() && e.isPickable() && e != player() && e != target && !(e instanceof ThrownEnderpearl),
                start.distanceToSqr(end)
        );
        return hit != null;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!renderTargetLanding.getValue() || predictedTargetLanding == null) return;

        BlockPos pos = BlockPos.containing(predictedTargetLanding);
        int accent = Theme.getAccentColor();
        Color fillCol = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 60);
        Color outCol = new Color(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 240);

        Render3D.drawBox(pos, fillCol, outCol, true, true, true);
    }
}
