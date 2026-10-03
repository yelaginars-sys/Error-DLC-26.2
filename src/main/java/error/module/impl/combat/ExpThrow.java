package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public final class ExpThrow extends Module {
    public static ExpThrow INSTANCE;

    public final BindSetting triggerKey = bind("Кнопка Броска", GLFW.GLFW_KEY_UNKNOWN);
    public final SliderSetting pitch = slider("Угол броска", 90.0F, -90.0F, 90.0F, 5.0F);
    public final SliderSetting turnSpeed = slider("Скорость поворота", 30.0F, 5.0F, 90.0F, 5.0F);

    private boolean isThrowing = false;

    public ExpThrow() {
        super("ExpThrow", "Быстрый сайлент-бросок пузырьков опыта под себя", Category.COMBAT);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null || event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (isThrowKeyHeld()) {
            throwExperience();
        }
    }

    public boolean isThrowKeyHeld() {
        if (triggerKey == null || triggerKey.isEmpty()) return false;
        long window = mc.getWindow().handle();
        for (int code : triggerKey.getValue()) {
            if (BindSetting.isKeyboard(code) && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS) {
                return true;
            } else if (BindSetting.isMouse(code) && GLFW.glfwGetMouseButton(window, BindSetting.rawButton(code)) == GLFW.GLFW_PRESS) {
                return true;
            }
        }
        return false;
    }

    private void throwExperience() {
        if (mc.player == null || mc.getConnection() == null) return;

        int expSlot = findExpSlot();
        InteractionHand hand = null;

        if (mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE)) {
            hand = InteractionHand.OFF_HAND;
        } else if (expSlot != -1) {
            mc.player.getInventory().setSelectedSlot(expSlot);
            hand = InteractionHand.MAIN_HAND;
        }

        if (hand != null) {
            float targetPitch = pitch.getValue();
            mc.getConnection().send(new ServerboundUseItemPacket(hand, 0, mc.player.getYRot(), targetPitch));
            mc.player.swing(hand);
        }
    }

    private int findExpSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.EXPERIENCE_BOTTLE)) return i;
        }
        return -1;
    }
}
