package error.module.impl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.AutoTotem;
import error.setting.impl.BindSetting;
import error.util.player.InventoryUtil;
import error.util.player.MoveBlockUtility;

/**
 * Create by daun kvass
 */
public final class ClickPearl extends Module {
    private static final int HOTBAR_SIZE = 9;

    public final BindSetting key = bind("Key", GLFW.GLFW_KEY_UNKNOWN);

    private enum State {
        IDLE,
        STOP_SPRINT,
        SWAP_AND_THROW,
        COOLDOWN
    }

    private State state = State.IDLE;
    private boolean throwQueued = false;
    private int cooldownTicks = 0;

    public ClickPearl() {
        super("ClickPearl", "Кидает по бинду чё-то", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private void reset() {
        state = State.IDLE;
        throwQueued = false;
        cooldownTicks = 0;
        MoveBlockUtility.unblock(this);
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (AutoTotem.isSwapping()) return;
        if (event.getAction() == GLFW.GLFW_PRESS && key.matches(event.getKey())) {
            queueThrow();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (AutoTotem.isSwapping()) return;
        if (event.getAction() == GLFW.GLFW_PRESS && key.matchesMouse(event.getButton()) && queueThrow()) {
            event.cancel();
        }
    }

    private boolean queueThrow() {
        if (AutoTotem.isSwapping()) return false;
        if (mc.gui.screen() == null && mc.player != null && state == State.IDLE) {
            throwQueued = true;
            return true;
        }
        return false;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (!inGame() || player() == null || mc.gameMode == null) {
            reset();
            return;
        }

        if (AutoTotem.isSwapping()) {
            reset();
            return;
        }

        LocalPlayer player = player();

        if (throwQueued) {
            throwQueued = false;

            if (player.getMainHandItem().is(Items.ENDER_PEARL)) {
                InventoryUtil.useDirect(player);
                return;
            }

            int hotbarSlot = findPearlHotbarSlot(player);
            if (hotbarSlot != -1) {
                int previousSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(hotbarSlot);
                InventoryUtil.useDirect(player);
                player.getInventory().setSelectedSlot(previousSlot);
                return;
            }

            int invSlot = findPearlContainerSlot(player);
            if (invSlot != -1) {
                state = State.STOP_SPRINT;
            }
        }

        switch (state) {
            case STOP_SPRINT -> {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }

                if (player.onGround()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                MoveBlockUtility.freeze(this, 4);
                MoveBlockUtility.blockSprint(this, 5);

                state = State.SWAP_AND_THROW;
            }

            case SWAP_AND_THROW -> {
                if (player.onGround()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                int invSlot = findPearlContainerSlot(player);
                if (invSlot != -1) {
                    int selectedHotbar = player.getInventory().getSelectedSlot();
                    mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, invSlot, selectedHotbar, ContainerInput.SWAP, player);
                    InventoryUtil.useDirect(player);
                    mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, invSlot, selectedHotbar, ContainerInput.SWAP, player);
                }

                cooldownTicks = 2;
                state = State.COOLDOWN;
            }

            case COOLDOWN -> {
                MoveBlockUtility.freeze(this, 2);
                if (player.onGround()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                if (--cooldownTicks <= 0) {
                    state = State.IDLE;
                    MoveBlockUtility.unblock(this);
                }
            }

            case IDLE -> {
            }
        }
    }

    private int findPearlHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (player.getInventory().getItem(slot).is(Items.ENDER_PEARL)) {
                return slot;
            }
        }
        return -1;
    }

    private int findPearlContainerSlot(LocalPlayer player) {
        return InventoryUtil.findInventorySlot(player, stack -> stack.is(Items.ENDER_PEARL));
    }
}