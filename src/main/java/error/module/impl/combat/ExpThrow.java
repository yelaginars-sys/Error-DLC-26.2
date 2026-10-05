package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

public final class ExpThrow extends Module {
    public static ExpThrow INSTANCE;

    public final BindSetting triggerKey = bind("Кнопка", GLFW.GLFW_KEY_UNKNOWN);
    public final SliderSetting pitch = slider("Угол броска", 90.0F, -90.0F, 90.0F, 5.0F);
    public final SliderSetting turnSpeed = slider("Скорость поворота", 30.0F, 1.0F, 90.0F, 5.0F);

    private int previousSlot = -1;
    private int swappedInvSlot = -1;
    private int swappedHotbarSlot = -1;
    private boolean isThrowing = false;
    private int throwDelay = 0;

    public ExpThrow() {
        super("ExpThrow", "Быстрый бросок пузырьков опыта под себя из Debuda", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        finishThrow();
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (isThrowing && mc.player != null) {
            mc.player.setSprinting(false);
            event.setSprinting(false);
            if (mc.player.onGround()) {
                event.setMoveVector(new Vec2(0.0F, 0.0F));
            }
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            finishThrow();
            return;
        }

        if (isThrowKeyHeld()) {
            isThrowing = true;
            handleThrow();
        } else {
            if (isThrowing) {
                finishThrow();
            }
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

    private void handleThrow() {
        if (throwDelay > 0) {
            throwDelay--;
            return;
        }

        InteractionHand hand = null;
        int expSlot = -1;

        if (mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE)) {
            hand = InteractionHand.OFF_HAND;
        } else {
            expSlot = findExpSlot();
            if (expSlot == -1) {
                return;
            }

            if (expSlot < 9) {
                if (previousSlot == -1) {
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                }
                mc.player.getInventory().setSelectedSlot(expSlot);
                hand = InteractionHand.MAIN_HAND;
            } else {
                // In inventory: swap to hotbar slot 8
                if (previousSlot == -1) {
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                }
                swappedInvSlot = expSlot;
                swappedHotbarSlot = 8;
                swapInventorySlot(expSlot, swappedHotbarSlot);
                mc.player.getInventory().setSelectedSlot(swappedHotbarSlot);
                hand = InteractionHand.MAIN_HAND;
            }
        }

        if (hand != null) {
            float targetPitch = pitch.getValue();
            float currentPitch = mc.player.getXRot();
            float maxTurn = turnSpeed.getValue();
            float deltaPitch = Mth.clamp(targetPitch - currentPitch, -maxTurn, maxTurn);
            float newPitch = currentPitch + deltaPitch;

            // Turn player pitch towards target
            mc.player.setXRot(newPitch);

            // Send look packet with target pitch directly
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                        mc.player.getYRot(), targetPitch, mc.player.onGround(), false
                ));
            }

            mc.gameMode.useItem(mc.player, hand);
            mc.player.swing(hand);
            throwDelay = 1;
        }
    }

    private void finishThrow() {
        if (swappedInvSlot != -1 && swappedHotbarSlot != -1 && mc.player != null && mc.gameMode != null) {
            swapInventorySlot(swappedInvSlot, swappedHotbarSlot);
            swappedInvSlot = -1;
            swappedHotbarSlot = -1;
        }

        if (previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
        }

        isThrowing = false;
        throwDelay = 0;
    }

    private void swapInventorySlot(int invSlot, int hotbarSlot) {
        if (mc.player == null || mc.gameMode == null) return;
        int containerSlot = toContainerSlot(invSlot);
        mc.gameMode.handleContainerInput(
                mc.player.inventoryMenu.containerId,
                containerSlot,
                hotbarSlot,
                ContainerInput.SWAP,
                mc.player
        );
    }

    private static int toContainerSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    private int findExpSlot() {
        if (mc.player == null) return -1;
        // Check hotbar first
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.EXPERIENCE_BOTTLE)) return i;
        }
        // Then inventory
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.EXPERIENCE_BOTTLE)) return i;
        }
        return -1;
    }
}
