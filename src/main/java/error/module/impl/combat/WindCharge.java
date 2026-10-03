package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class WindCharge extends Module {
    public static WindCharge INSTANCE;

    public final CheckBox equipMace = checkbox("Брать булаву", true);
    public final CheckBox checkCeiling = checkbox("Учитывать потолок", true);
    public final SliderSetting minCeiling = slider("Мин. потолок", 5.0F, 0.0F, 20.0F, 1.0F);

    private boolean isJumpingWithWind = false;

    public WindCharge() {
        super("WindCharge", "Сайлент-бросок заряда ветра и мега-прыжок под себя", Category.COMBAT);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null || event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (isJumpingWithWind) {
            if (equipMace.getValue()) {
                int maceSlot = findItemSlot(Items.MACE);
                if (maceSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(maceSlot);
                }
            }
            isJumpingWithWind = false;
        }
    }

    public boolean executeWindJump() {
        if (mc.player == null || mc.level == null) return false;

        if (checkCeiling.getValue()) {
            double clearance = getCeilingClearance();
            if (clearance < minCeiling.getValue()) return false;
        }

        int windChargeSlot = findItemSlot(Items.WIND_CHARGE);
        InteractionHand hand = null;

        if (mc.player.getOffhandItem().is(Items.WIND_CHARGE)) {
            hand = InteractionHand.OFF_HAND;
        } else if (windChargeSlot != -1) {
            mc.player.getInventory().setSelectedSlot(windChargeSlot);
            hand = InteractionHand.MAIN_HAND;
        }

        if (hand != null) {
            mc.player.jumpFromGround();
            mc.getConnection().send(new ServerboundUseItemPacket(hand, 0, mc.player.getYRot(), 90.0F));
            isJumpingWithWind = true;
            return true;
        }
        return false;
    }

    private int findItemSlot(net.minecraft.world.item.Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(item)) return i;
        }
        return -1;
    }

    private double getCeilingClearance() {
        if (mc.player == null || mc.level == null) return 20.0D;
        double startY = mc.player.getY() + mc.player.getEyeHeight();
        for (double y = startY; y <= startY + 20.0D; y += 0.5D) {
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(mc.player.getX(), y, mc.player.getZ());
            if (!mc.level.getBlockState(pos).isAir()) {
                return y - startY;
            }
        }
        return 20.0D;
    }
}
