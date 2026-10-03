package error.module.impl.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;
import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.ui.mainmenu.PanelRefractions;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.ModeSetting;
import error.util.player.InventorySwaps;
import error.util.player.InventoryUtil;
import error.util.player.MoveBlockUtility;

import java.util.function.Predicate;

/**
 */
public final class AutoSwap extends Module {

    private static final int OFFHAND_SLOT = 45;
    private static final int HOTBAR_SIZE = 9;

    public final ModeSetting bypassMode = mode("Bypass", "Default", "Default", "Matrix", "Grim");
    public final BindSetting key = bind("Key", GLFW.GLFW_KEY_UNKNOWN);
    public final ModeSetting item1 = mode("Item 1", "Totem", "Totem", "Enchanted Totem", "Player Head", "Golden Apple");
    public final ModeSetting item2 = mode("Item 2", "Golden Apple", "Totem", "Enchanted Totem", "Player Head", "Golden Apple");

    private enum State {
        IDLE,
        STOP_SPRINT,
        SWAP,
        COOLDOWN
    }

    private State state = State.IDLE;
    private String queuedItemType = null;
    private int cooldownTicks = 0;

    public AutoSwap() {
        super("AutoSwap", "Свипает ", Category.PLAYER);
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private void reset() {
        state = State.IDLE;
        queuedItemType = null;
        cooldownTicks = 0;
        MoveBlockUtility.unblock(this);
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || mc.gui.screen() != null || PanelRefractions.isOpen() || state != State.IDLE) return;

        if (key.matches(event.getKey())) {
            toggleSwap();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || mc.gui.screen() != null || PanelRefractions.isOpen() || state != State.IDLE) return;

        if (key.matchesMouse(event.getButton())) {
            toggleSwap();
            event.cancel();
        }
    }

    private void toggleSwap() {
        if (!inGame() || player() == null || mc.gameMode == null) return;

        LocalPlayer player = player();
        ItemStack offhand = player.getOffhandItem();

        Predicate<ItemStack> pred1 = getPredicate(item1.getValue());
        Predicate<ItemStack> pred2 = getPredicate(item2.getValue());

        String targetItemType;

        if (pred1 != null && pred1.test(offhand)) {
            targetItemType = item2.getValue();
        } else if (pred2 != null && pred2.test(offhand)) {
            targetItemType = item1.getValue();
        } else {
            if (findAnySlot(player, pred1) != -1) {
                targetItemType = item1.getValue();
            } else {
                targetItemType = item2.getValue();
            }
        }

        Predicate<ItemStack> targetPred = getPredicate(targetItemType);
        int targetSlot = findAnySlot(player, targetPred);

        if (targetSlot == -1) {
            String fallbackType = targetItemType.equals(item1.getValue()) ? item2.getValue() : item1.getValue();
            Predicate<ItemStack> fallbackPred = getPredicate(fallbackType);
            if (fallbackPred != null && !fallbackPred.test(offhand)) {
                targetSlot = findAnySlot(player, fallbackPred);
                if (targetSlot != -1) {
                    targetItemType = fallbackType;
                    targetPred = fallbackPred;
                }
            }
        }

        if (targetSlot == -1) return;

        String mode = bypassMode.getValue();
        int hotbarSlot = findInHotbar(player, targetPred);

        if (mode.equalsIgnoreCase("Default")) {
            if (hotbarSlot != -1) {
                InventorySwaps.performHotbarFSwap(player, hotbarSlot);
            } else {
                InventorySwaps.grimSwapsOffHand(player,targetSlot);
            }
        } else if (mode.equalsIgnoreCase("Matrix")) {
            if (hotbarSlot != -1) {
                InventorySwaps.performHotbarFSwap(player, hotbarSlot);
            } else {
                this.queuedItemType = targetItemType;
                this.state = State.STOP_SPRINT;
            }
        } else if (mode.equalsIgnoreCase("Grim")) {
            this.queuedItemType = targetItemType;
            this.state = State.STOP_SPRINT;
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (!inGame() || player() == null || mc.gameMode == null) {
            reset();
            return;
        }

        LocalPlayer player = player();

        switch (state) {
            case STOP_SPRINT -> {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                }
                if (mc.getConnection() != null) {
               //     mc.getConnection().send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
                }

                if (player.onGround()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                MoveBlockUtility.freeze(this, 4);
                MoveBlockUtility.blockSprint(this, 5);

                state = State.SWAP;
            }

            case SWAP -> {
                if (player.onGround()) {
                    player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
                }

                if (queuedItemType != null) {
                    Predicate<ItemStack> predicate = getPredicate(queuedItemType);
                    int targetSlot = findAnySlot(player, predicate);

                    if (targetSlot != -1) {
                        InventorySwaps.executeContainerSwap(player, targetSlot, OFFHAND_SLOT);
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
                    queuedItemType = null;
                    MoveBlockUtility.unblock(this);
                }
            }

            case IDLE -> {
            }
        }
    }





    private Predicate<ItemStack> getPredicate(String type) {
        if (type == null) return null;
        return switch (type) {
            case "Totem" -> stack -> stack.is(Items.TOTEM_OF_UNDYING) ;
            case "Enchanted Totem" -> stack->stack.is(Items.TOTEM_OF_UNDYING) && stack.isEnchanted();
            case "Player Head" -> stack -> stack.is(Items.PLAYER_HEAD);
            case "Golden Apple" -> stack -> stack.is(Items.GOLDEN_APPLE) ;
            default -> null;
        };
    }

    private boolean isEnchantedTotem(ItemStack stack) {
        if (stack == null || !stack.is(Items.TOTEM_OF_UNDYING)) return false;
        if (stack.isEnchanted() || stack.has(DataComponents.ENCHANTMENTS) || stack.has(DataComponents.CUSTOM_DATA) || stack.has(DataComponents.CUSTOM_NAME)) {
            return true;
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore != null && !lore.lines().isEmpty();
    }

    private int findInHotbar(LocalPlayer player, Predicate<ItemStack> predicate) {
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (predicate.test(stack)) return i;
        }
        return -1;
    }

    private int findAnySlot(LocalPlayer player, Predicate<ItemStack> predicate) {
        return InventoryUtil.findPlayerMenuSlot(player, predicate);
    }
}