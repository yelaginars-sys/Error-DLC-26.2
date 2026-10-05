package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class MaceHelper extends Module {
    public static MaceHelper INSTANCE;

    public final CheckBox autoEquip = checkbox("Авто-взятие", true);
    public final SliderSetting equipHeight = slider("Высота взятия", 2.0F, 1.0F, 10.0F, 0.5F);
    public final CheckBox targetOnly = checkbox("Только с целью", false);
    public final CheckBox restoreSlot = checkbox("Возврат слота", true);
    public final SliderSetting minFall = slider("Мин. падение", 1.5F, 0.0F, 6.0F, 0.5F);
    public final SliderSetting preHitDist = slider("Досрочный удар", 2.0F, 0.5F, 6.0F, 0.5F);
    public final CheckBox waitCooldown = checkbox("Ждать заряд", true);
    public final CheckBox checkCooldown = checkbox("Учитывать кулдаун", true);
    public final SliderSetting cooldownDelay = slider("Задержка кд", 250.0F, 0.0F, 1000.0F, 50.0F);

    // Wind Charge settings
    public final CheckBox windCharge = checkbox("Заряд ветра", true);
    public final SliderSetting windChargeDelay = slider("Задержка заряда (тиков)", 20.0F, 5.0F, 100.0F, 1.0F);
    public final CheckBox checkCeiling = checkbox("Учитывать потолок", true);
    public final SliderSetting minCeiling = slider("Мин. потолок", 5.0F, 0.0F, 20.0F, 1.0F);
    public final CheckBox onlyWithTarget = checkbox("Заряд только с целью", false);

    private int previousSlot = -1;
    private boolean isMaceEquipped = false;
    private long swapTimestamp = 0L;
    private int chargeCooldown = 0;

    public MaceHelper() {
        super("MaceHelper", "Булава: авто-взятие, крит-удар и бросок заряда ветра под себя", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        chargeCooldown = 0;
        reset();
    }

    @Override
    public void onDisable() {
        chargeCooldown = 0;
        reset();
    }

    public void reset() {
        if (restoreSlot.getValue() && previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
        isMaceEquipped = false;
        swapTimestamp = 0L;
    }

    public boolean isMaceActive() {
        if (!isEnabled() || mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandItem();
        return mainHand.is(Items.MACE);
    }

    public boolean shouldHoldHit() {
        if (!isEnabled() || mc.player == null || !isMaceActive()) return false;
        if (checkCooldown.getValue()) {
            if (swapTimestamp != 0L && System.currentTimeMillis() - swapTimestamp < cooldownDelay.getValue()) {
                return true;
            }
            if (mc.player.getAttackStrengthScale(0.0F) < 0.95F) {
                return true;
            }
        }
        return false;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null || event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        // Wind Charge handling: throws wind charge directly under self (pitch = 90)
        if (chargeCooldown > 0) {
            chargeCooldown--;
        } else if (windCharge.getValue()) {
            handleWindCharge();
        }

        if (targetOnly.getValue() && AuraModule.INSTANCE != null && AuraModule.INSTANCE.getTarget() == null) {
            if (isMaceEquipped) reset();
            return;
        }

        double distanceToGround = calculateDistanceToGround();
        boolean isFalling = mc.player.fallDistance >= minFall.getValue() && mc.player.getDeltaMovement().y < -0.1D;

        if (autoEquip.getValue() && isFalling && distanceToGround <= equipHeight.getValue()) {
            int maceSlot = findMaceSlot();
            if (maceSlot != -1 && mc.player.getInventory().getSelectedSlot() != maceSlot) {
                if (previousSlot == -1) {
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                }
                mc.player.getInventory().setSelectedSlot(maceSlot);
                isMaceEquipped = true;
                swapTimestamp = System.currentTimeMillis();
            }
        } else if (mc.player.onGround() && isMaceEquipped) {
            reset();
        }
    }

    private void handleWindCharge() {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) return;

        if (onlyWithTarget.getValue() && (AuraModule.INSTANCE == null || AuraModule.INSTANCE.getTarget() == null)) {
            return;
        }

        if (checkCeiling.getValue()) {
            double clearance = getCeilingClearance();
            if (clearance < minCeiling.getValue()) return;
        }

        // Only throw when on ground or beginning jump, avoid spamming while falling
        if (!mc.player.onGround() && mc.player.fallDistance > 1.2F) {
            return;
        }

        executeWindJump();
    }

    public boolean executeWindJump() {
        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) return false;

        InteractionHand hand = null;
        int slot = -1;

        if (mc.player.getOffhandItem().is(Items.WIND_CHARGE)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(Items.WIND_CHARGE)) {
                    slot = i;
                    break;
                }
            }
            if (slot == -1) return false;
            hand = InteractionHand.MAIN_HAND;
        }

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (slot != -1) {
            mc.player.getInventory().setSelectedSlot(slot);
        }

        // Always throw directly under feet: pitch 90.0F
        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                mc.player.getYRot(), 90.0F, mc.player.onGround(), false
        ));
        mc.gameMode.useItem(mc.player, hand);
        mc.player.swing(hand);

        if (slot != -1) {
            mc.player.getInventory().setSelectedSlot(prevSlot);
        }

        chargeCooldown = Math.max(1, Math.round(windChargeDelay.getValue()));
        return true;
    }

    private int findMaceSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.MACE)) return i;
        }
        return -1;
    }

    private double getCeilingClearance() {
        if (mc.player == null || mc.level == null) return 20.0D;
        double startY = mc.player.getY() + mc.player.getEyeHeight();
        for (double y = startY; y <= startY + 20.0D; y += 0.5D) {
            BlockPos pos = BlockPos.containing(mc.player.getX(), y, mc.player.getZ());
            if (!mc.level.getBlockState(pos).isAir()) {
                return y - startY;
            }
        }
        return 20.0D;
    }

    private double calculateDistanceToGround() {
        if (mc.player == null || mc.level == null) return 10.0D;
        AABB box = mc.player.getBoundingBox();
        double startY = box.minY;
        double endY = startY - 12.0D;

        for (double y = startY; y >= endY; y -= 0.1D) {
            AABB checkBox = new AABB(box.minX, y - 0.1D, box.minZ, box.maxX, y, box.maxZ);
            if (!mc.level.noCollision(mc.player, checkBox)) {
                return startY - y;
            }
        }
        return 12.0D;
    }
}
