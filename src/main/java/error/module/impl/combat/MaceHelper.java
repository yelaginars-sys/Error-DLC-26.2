package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
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

    private int previousSlot = -1;
    private boolean isMaceEquipped = false;
    private long swapTimestamp = 0L;

    public MaceHelper() {
        super("MaceHelper", "Булава: авто-взятие и крит-удар при падении", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        reset();
    }

    @Override
    public void onDisable() {
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

    private int findMaceSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.MACE)) return i;
        }
        return -1;
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
