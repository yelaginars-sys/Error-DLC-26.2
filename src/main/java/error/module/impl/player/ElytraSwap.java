package error.module.impl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
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
import error.setting.impl.ModeSetting;
import error.util.client.persiki.Notify;
import error.util.player.InventorySwaps;
import error.util.player.InventoryUtil;
import error.util.player.MoveBlockUtility;

/**
 */
public final class ElytraSwap extends Module {

    private static final int CHESTPLATE_SLOT = 6;
    private static final int HOTBAR_SIZE = 9;

    public final ModeSetting mode = mode("Mode", "Packet", "Packet", "Normal");
    public final BindSetting swapKey = bind("Swap Key", GLFW.GLFW_KEY_UNKNOWN);
    public final BindSetting fireworkKey = bind("Firework Key", GLFW.GLFW_KEY_UNKNOWN);

    private enum TaskType {
        NONE,
        SWAP_ARMOR,
        FIREWORK_INV
    }

    private enum State {
        IDLE,
        STOP_SPRINT,
        EXECUTE,
        COOLDOWN
    }

    private State state = State.IDLE;
    private TaskType currentTask = TaskType.NONE;
    private boolean swapQueued = false;
    private boolean fireworkQueued = false;
    private int cooldownTicks = 0;

    public ElytraSwap() {
        super("ElytraSwap", "Быстрый свап элитры и запуск фейерверков", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private void reset() {
        state = State.IDLE;
        currentTask = TaskType.NONE;
        swapQueued = false;
        fireworkQueued = false;
        cooldownTicks = 0;
        MoveBlockUtility.unblock(this);
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (AutoTotem.isSwapping()) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        if (swapKey.matches(event.getKey())) {
            queueSwap();
        } else if (fireworkKey.matches(event.getKey())) {
            queueFirework();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (AutoTotem.isSwapping()) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        if (swapKey.matchesMouse(event.getButton()) && queueSwap()) {
            event.cancel();
        } else if (fireworkKey.matchesMouse(event.getButton()) && queueFirework()) {
            event.cancel();
        }
    }

    private boolean queueSwap() {
        if (AutoTotem.isSwapping()) return false;
        if (mc.gui.screen() == null && mc.player != null && state == State.IDLE) {
            swapQueued = true;
            return true;
        }
        return false;
    }

    private boolean queueFirework() {
        if (AutoTotem.isSwapping()) return false;
        if (mc.gui.screen() == null && mc.player != null && state == State.IDLE && mc.player.isFallFlying()) {
            fireworkQueued = true;
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

        if (swapQueued) {
            swapQueued = false;

            ItemStack chestItem = player.inventoryMenu.getSlot(CHESTPLATE_SLOT).getItem();
            boolean wearingElytra = chestItem.is(Items.ELYTRA);
            int targetSlot = findSwapTargetSlot(player);

            if (targetSlot == -1) {
                    Notify.error("ElytraSwap", wearingElytra ? "нету Нагрудника" : "нету Элитры");
            } else {
                    Notify.info("ElytraSwap", wearingElytra ? "свапнул на Нагрудник" : "свапнул на Элитру");

                if (mode.is("Packet")) {
                    InventorySwaps.grimSwapsArmor(player, targetSlot);
                } else {
                    currentTask = TaskType.SWAP_ARMOR;
                    state = State.STOP_SPRINT;
                }
            }
        }

        if (fireworkQueued) {
            fireworkQueued = false;

            if (player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
                InventoryUtil.useDirect(player);
                return;
            }
            if (player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
                InventoryUtil.useDirect(player, InteractionHand.OFF_HAND);
                return;
            }

            int hotbarSlot = findFireworkHotbarSlot(player);
            if (hotbarSlot != -1) {
                int previousSlot = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(hotbarSlot);
                InventoryUtil.useDirect(player);
                player.getInventory().setSelectedSlot(previousSlot);
                return;
            }

            int invSlot = findFireworkContainerSlot(player);
            if (invSlot != -1) {
                if (mode.is("Packet")) {
                    performPacketFirework(player, invSlot);
                } else {
                    currentTask = TaskType.FIREWORK_INV;
                    state = State.STOP_SPRINT;
                }
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

                state = State.EXECUTE;
            }

            case EXECUTE -> {
                if (player.onGround()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                if (currentTask == TaskType.SWAP_ARMOR) {
                    int targetSlot = findSwapTargetSlot(player);
                    if (targetSlot != -1) {
                        InventorySwaps.executeContainerSwap(player, targetSlot, CHESTPLATE_SLOT);
                    }
                } else if (currentTask == TaskType.FIREWORK_INV) {
                    int invSlot = findFireworkContainerSlot(player);
                    if (invSlot != -1) {
                        int selectedHotbar = player.getInventory().getSelectedSlot();

                        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, invSlot, selectedHotbar, ContainerInput.SWAP, player);
                        InventoryUtil.useDirect(player);
                        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, invSlot, selectedHotbar, ContainerInput.SWAP, player);
                    }
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
                    currentTask = TaskType.NONE;
                    MoveBlockUtility.unblock(this);
                }
            }

            case IDLE -> {
            }
        }
    }

    private void performPacketFirework(LocalPlayer player, int invSlot) {
        boolean wasSprinting = player.isSprinting();
        if (wasSprinting && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        }

        int selectedHotbar = player.getInventory().getSelectedSlot();
        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, invSlot, selectedHotbar, ContainerInput.SWAP, player);
        InventoryUtil.useDirect(player);
        mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, invSlot, selectedHotbar, ContainerInput.SWAP, player);

        if (wasSprinting && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
    }

    private int findSwapTargetSlot(LocalPlayer player) {
        ItemStack chestItem = player.inventoryMenu.getSlot(CHESTPLATE_SLOT).getItem();
        boolean wearingElytra = chestItem.is(Items.ELYTRA);

        if (wearingElytra) {
            return InventoryUtil.findPlayerMenuSlot(player, this::isChestplate);
        } else {
            return InventoryUtil.findPlayerMenuSlot(player, stack -> stack.is(Items.ELYTRA));
        }
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.NETHERITE_CHESTPLATE)
                || stack.is(Items.DIAMOND_CHESTPLATE)
                || stack.is(Items.IRON_CHESTPLATE)
                || stack.is(Items.GOLDEN_CHESTPLATE)
                || stack.is(Items.CHAINMAIL_CHESTPLATE)
                || stack.is(Items.LEATHER_CHESTPLATE);
    }

    private int findFireworkHotbarSlot(LocalPlayer player) {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (player.getInventory().getItem(slot).is(Items.FIREWORK_ROCKET)) {
                return slot;
            }
        }
        return -1;
    }

    private int findFireworkContainerSlot(LocalPlayer player) {
        return InventoryUtil.findInventorySlot(player, stack -> stack.is(Items.FIREWORK_ROCKET));
    }
}