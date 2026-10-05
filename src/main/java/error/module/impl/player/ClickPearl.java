package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.util.player.InventoryUtil;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class ClickPearl extends Module {
    public static ClickPearl INSTANCE;

    public final BindSetting bind = bind("Бинд", GLFW.GLFW_KEY_UNKNOWN);

    private boolean active = false;
    private int stage = 0;
    private int ticks = 0;
    private int originalSlot = -1;
    private int targetHotbarSlot = -1;
    private int invSwapSlot = -1;
    private boolean isInvSwapped = false;
    private boolean isOffhandPearl = false;

    public ClickPearl() {
        super("ClickPearl", "Быстрый бросок жемчуга по бинду", Category.PLAYER);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;
        if (bind != null && !bind.isEmpty() && bind.matches(event.getKey())) {
            trigger();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;
        if (bind != null && !bind.isEmpty() && bind.matchesMouse(event.getButton())) {
            trigger();
        }
    }

    private void trigger() {
        if (active || mc.player == null || mc.gameMode == null) return;
        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) return;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        invSwapSlot = -1;
        isInvSwapped = false;
        isOffhandPearl = false;

        if (mc.player.getOffhandItem().is(Items.ENDER_PEARL)) {
            isOffhandPearl = true;
        } else {
            int hotbarSlot = findInHotbar();
            if (hotbarSlot == -1) {
                int mainInvSlot = findInInventory();
                if (mainInvSlot == -1) return;

                hotbarSlot = originalSlot;
                InventoryUtil.swapSlots(mainInvSlot, hotbarSlot);
                invSwapSlot = mainInvSlot;
                isInvSwapped = true;
            }
            targetHotbarSlot = hotbarSlot;
            selectSlot(targetHotbarSlot);
        }

        active = true;
        stage = 0;
        ticks = 0;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !active || mc.player == null || mc.gameMode == null) return;

        ticks++;
        switch (stage) {
            case 0 -> {
                if (ticks >= 2) {
                    if (isOffhandPearl) {
                        mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
                        mc.player.swing(InteractionHand.OFF_HAND);
                        stage = 2;
                        ticks = 0;
                    } else {
                        stage = 1;
                        ticks = 0;
                    }
                }
            }
            case 1 -> {
                if (ticks >= 2) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    stage = 2;
                    ticks = 0;
                }
            }
            case 2 -> {
                if (ticks >= 2) {
                    reset();
                }
            }
        }
    }

    private void reset() {
        if (isInvSwapped && invSwapSlot != -1 && targetHotbarSlot != -1 && mc.player != null) {
            InventoryUtil.swapSlots(invSwapSlot, targetHotbarSlot);
        }
        if (originalSlot >= 0 && originalSlot < 9 && mc.player != null) {
            selectSlot(originalSlot);
        }

        active = false;
        stage = 0;
        ticks = 0;
        originalSlot = -1;
        targetHotbarSlot = -1;
        invSwapSlot = -1;
        isInvSwapped = false;
        isOffhandPearl = false;
    }

    private void selectSlot(int slot) {
        if (mc.player == null || slot < 0 || slot >= 9) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            }
        }
    }

    private int findInHotbar() {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(Items.ENDER_PEARL)) return i;
        }
        return -1;
    }

    private int findInInventory() {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).is(Items.ENDER_PEARL)) return i;
        }
        return -1;
    }
}