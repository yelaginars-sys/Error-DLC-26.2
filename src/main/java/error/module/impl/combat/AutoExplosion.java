package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.util.RotationHandler;
import error.util.player.MoveUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * AutoExplosion module ported from Energy client.
 * Automatically places and detonates end crystals / charges respawn anchors,
 * with safety checks: "Не взрывать себя" and "Не взрывать ресурсы".
 */
public final class AutoExplosion extends Module {

    public final CheckBox dontExplodeSelf = checkbox("Не взрывать себя", true);
    public final CheckBox dontExplodeItems = checkbox("Не взрывать ресурсы", true);
    public final CheckBox onClickObsidian = checkbox("Клик по обсидиану", true);

    private BlockPos pendingPlacePos = null;
    private int pendingSlot = -1;
    private boolean isAnchorCharge = false;
    private boolean needsSlotRestore = false;
    private int originalSlot = -1;

    private AABB crystalSearchBox = null;
    private BlockPos targetObsidianPos = null;

    private boolean wasUseDown = false;
    private boolean wasAttackDown = false;

    public AutoExplosion() {
        super("AutoExplosion", "Автоматически взрывает кристаллы", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        resetState();
        RotationHandler.disengage("Instant");
    }

    private void resetState() {
        pendingPlacePos = null;
        pendingSlot = -1;
        isAnchorCharge = false;
        needsSlotRestore = false;
        originalSlot = -1;
        crystalSearchBox = null;
        targetObsidianPos = null;
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
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null || mc.gameMode == null) {
            resetState();
            return;
        }

        // 1. Restore slot if needed
        if (needsSlotRestore) {
            needsSlotRestore = false;
            if (originalSlot != -1 && originalSlot < 9) {
                player().getInventory().setSelectedSlot(originalSlot);
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
                }
            }
            originalSlot = -1;
        }

        // 2. Handle pending placement on obsidian / anchor charge
        if (pendingPlacePos != null) {
            if (mc.level.getBlockState(pendingPlacePos).isAir()) {
                pendingPlacePos = null;
                pendingSlot = -1;
                isAnchorCharge = false;
            } else if (pendingSlot != -1) {
                Vec3 camera = player().getEyePosition();
                Vec3 targetCenter = Vec3.atCenterOf(pendingPlacePos);
                Vec3 dir = targetCenter.subtract(camera);
                float[] rots = calculateRotations(camera, targetCenter);
                RotationHandler.setRotation(rots[0], rots[1]);

                originalSlot = player().getInventory().getSelectedSlot();
                player().getInventory().setSelectedSlot(pendingSlot);
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundSetCarriedItemPacket(pendingSlot));
                }

                BlockHitResult hitResult = new BlockHitResult(
                        targetCenter,
                        Direction.UP,
                        pendingPlacePos,
                        false
                );
                mc.gameMode.useItemOn(player(), InteractionHand.MAIN_HAND, hitResult);
                player().swing(InteractionHand.MAIN_HAND);
                needsSlotRestore = true;

                if (!isAnchorCharge) {
                    this.targetObsidianPos = pendingPlacePos.immutable();
                    this.crystalSearchBox = new AABB(pendingPlacePos.above()).inflate(0.1);
                }

                pendingPlacePos = null;
                pendingSlot = -1;
                isAnchorCharge = false;
            }
        }

        // 3. Search and explode End Crystals
        if (crystalSearchBox != null) {
            for (Entity entity : mc.level.getEntities((Entity) null, crystalSearchBox, e -> e instanceof EndCrystal && e.isAlive())) {
                EndCrystal crystal = (EndCrystal) entity;

                // Safety 1: Не взрывать себя
                if (dontExplodeSelf.getValue()) {
                    double playerY = player().getY();
                    double crystalY = crystal.getY();
                    if (Math.abs(playerY - crystalY) < 1.0) {
                        resetExplosionTarget();
                        return;
                    }
                }

                // Safety 2: Не взрывать ресурсы
                if (hasValuableItemsNear(crystal)) {
                    resetExplosionTarget();
                    return;
                }

                // Aim and detonate
                Vec3 crystalEye = crystal.position().add(0, 0.5, 0);
                float[] rots = calculateRotations(player().getEyePosition(), crystalEye);
                RotationHandler.setRotation(rots[0], rots[1]);

                mc.gameMode.attack(player(), crystal);
                player().swing(InteractionHand.MAIN_HAND);

                resetExplosionTarget();
                return;
            }
        }

        // 4. Input checks for placing on clicked obsidian / placed obsidian
        boolean isUsing = mc.options.keyUse.isDown();
        boolean isAttacking = mc.options.keyAttack.isDown();

        if (onClickObsidian.getValue() && isAttacking && !wasAttackDown) {
            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos clickedPos = hit.getBlockPos();
                BlockState state = mc.level.getBlockState(clickedPos);

                if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                    if (mc.level.getBlockState(clickedPos.above()).isAir()) {
                        int crystalSlot = findHotbarItem(Items.END_CRYSTAL);
                        if (crystalSlot != -1) {
                            this.pendingPlacePos = clickedPos.immutable();
                            this.pendingSlot = crystalSlot;
                            this.isAnchorCharge = false;
                        }
                    }
                }
            }
        }

        if (isUsing && !wasUseDown) {
            ItemStack held = player().getMainHandItem();
            ItemStack off = player().getOffhandItem();
            boolean holdingObby = held.is(Blocks.OBSIDIAN.asItem()) || off.is(Blocks.OBSIDIAN.asItem());
            boolean holdingAnchor = held.is(Blocks.RESPAWN_ANCHOR.asItem()) || off.is(Blocks.RESPAWN_ANCHOR.asItem());

            if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos placedPos = hit.getBlockPos().relative(hit.getDirection());

                if (holdingObby) {
                    int crystalSlot = findHotbarItem(Items.END_CRYSTAL);
                    if (crystalSlot != -1) {
                        this.pendingPlacePos = placedPos.immutable();
                        this.pendingSlot = crystalSlot;
                        this.isAnchorCharge = false;
                    }
                } else if (holdingAnchor) {
                    int glowstoneSlot = findHotbarItem(Items.GLOWSTONE);
                    if (glowstoneSlot != -1) {
                        this.pendingPlacePos = placedPos.immutable();
                        this.pendingSlot = glowstoneSlot;
                        this.isAnchorCharge = true;
                    }
                }
            }
        }

        wasUseDown = isUsing;
        wasAttackDown = isAttacking;
    }

    private void resetExplosionTarget() {
        crystalSearchBox = null;
        targetObsidianPos = null;
    }

    private boolean hasValuableItemsNear(EndCrystal crystal) {
        if (!dontExplodeItems.getValue() || mc.level == null) return false;

        AABB box = new AABB(crystal.position(), crystal.position()).inflate(6.0);
        for (ItemEntity itemEntity : mc.level.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive)) {
            Item item = itemEntity.getItem().getItem();
            if (isValuableItem(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValuableItem(Item item) {
        if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.ELYTRA
                || item == Items.PLAYER_HEAD || item == Items.END_CRYSTAL || item == Items.TOTEM_OF_UNDYING) {
            return true;
        }
        String desc = item.getDescriptionId();
        return desc != null && desc.toLowerCase().contains("netherite");
    }

    private int findHotbarItem(Item item) {
        for (int i = 0; i < 9; i++) {
            if (player().getInventory().getItem(i).is(item)) {
                return i;
            }
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