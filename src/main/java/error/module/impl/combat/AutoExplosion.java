package error.module.impl.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import error.util.player.MoveUtility;
import error.util.RotationHandler;
import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;

/**
 */
public final class AutoExplosion extends Module {

    public final CheckBox onClickObsidian = checkbox("Click on Obsidian", true);

    private BlockPos targetPos = null;
    private int originalSlot = -1;
    private boolean waitingForBreak = false;
    private int timeoutTicks = 0;

    private boolean wasUseDown = false;
    private boolean wasAttackDown = false;

    public AutoExplosion() {
        super("AutoExplosion", "Члев", Category.COMBAT);
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
        if (!inGame() || player() == null || mc.level == null || mc.gameMode == null) {
            resetState();
            return;
        }

        if (waitingForBreak && targetPos != null) {
            EndCrystal crystal = findCrystalAbove(targetPos);
            if (crystal != null) {
                Vec3 crystalCenter = crystal.position().add(0, 0.5, 0);
                float[] rots = calculateRotations(mc.player.getEyePosition(), crystalCenter);
                RotationHandler.setRotation(rots[0], rots[1]);

                mc.gameMode.attack(mc.player, crystal);
                mc.player.swing(InteractionHand.MAIN_HAND);

                restoreOriginalSlot();
                resetState();
                return;
            }

            if (++timeoutTicks > 5) {
                restoreOriginalSlot();
                resetState();
            }
            return;
        }

        boolean isUsing = mc.options.keyUse.isDown();
        boolean isAttacking = mc.options.keyAttack.isDown();

        if (onClickObsidian.getValue() && isAttacking && !wasAttackDown) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos clickedPos = hit.getBlockPos();
                BlockState state = mc.level.getBlockState(clickedPos);

                if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                    if (mc.level.getBlockState(clickedPos.above()).isAir()) {
                        startExplosionProcess(clickedPos);
                    }
                }
            }
        }

        if (isUsing && !wasUseDown) {
            ItemStack held = mc.player.getMainHandItem();
            ItemStack off = mc.player.getOffhandItem();
            boolean holdingObby = held.is(Blocks.OBSIDIAN.asItem()) || off.is(Blocks.OBSIDIAN.asItem());

            if (holdingObby && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos placedPos = hit.getBlockPos().relative(hit.getDirection());
                startExplosionProcess(placedPos);
            }
        }

        wasUseDown = isUsing;
        wasAttackDown = isAttacking;
    }


    private void startExplosionProcess(BlockPos obbyPos) {
        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;

        this.originalSlot = mc.player.getInventory().getSelectedSlot();
        this.targetPos = obbyPos.immutable();

        if (crystalSlot != 40) {
            mc.player.getInventory().setSelectedSlot(crystalSlot);
        }

        Vec3 placeVec = new Vec3(obbyPos.getX() + 0.5, obbyPos.getY() + 1.0, obbyPos.getZ() + 0.5);
        float[] rots = calculateRotations(mc.player.getEyePosition(), placeVec);
        RotationHandler.setRotation(rots[0], rots[1]);

        InteractionHand hand = (crystalSlot == 40) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        BlockHitResult hit = new BlockHitResult(placeVec, Direction.UP, obbyPos, false);
        mc.gameMode.useItemOn(mc.player, hand, hit);
        mc.player.swing(hand);

        this.waitingForBreak = true;
        this.timeoutTicks = 0;
    }

    private void restoreOriginalSlot() {
        if (originalSlot >= 0 && originalSlot < 9) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    private void resetState() {
        targetPos = null;
        originalSlot = -1;
        waitingForBreak = false;
        timeoutTicks = 0;
        RotationHandler.disengage("Smooth");
    }

    private EndCrystal findCrystalAbove(BlockPos pos) {
        AABB box = new AABB(
                pos.getX() - 0.2, pos.getY() + 0.5, pos.getZ() - 0.2,
                pos.getX() + 1.2, pos.getY() + 2.5, pos.getZ() + 1.2
        );
        for (Entity e : mc.level.getEntities((Entity) null, box, entity -> entity instanceof EndCrystal && entity.isAlive())) {
            return (EndCrystal) e;
        }
        return null;
    }

    private int findCrystalSlot() {
        if (mc.player.getOffhandItem().is(Items.END_CRYSTAL)) return 40;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.END_CRYSTAL)) return i;
        }
        return -1;
    }

    private float[] calculateRotations(Vec3 eyes, Vec3 target) {
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double xz = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F),
                Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, xz)), -90.0F, 90.0F)
        };
    }
}