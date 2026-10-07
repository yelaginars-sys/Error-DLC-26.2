package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * AutoResell module ported from exclusive.
 * Automatically relists expired items on the auction every 60 seconds.
 */
public class AutoResell extends Module {
    private static final int PERIOD_TICKS = 20 * 60;
    private static final int OPEN_TIMEOUT_TICKS = 100;
    private static final int TICK_DELAY = 6;

    private State state = State.IDLE;
    private int cooldownTicks;
    private int waitTicks;
    private int noScreenTicks;

    private enum State {
        IDLE,
        OPEN_AH,
        CLICK_STORAGE,
        CLICK_RELIST,
        CLOSE
    }

    public AutoResell() {
        super("AutoResell", "Автоматическая перепродажа товаров на аукционе", Category.MISC);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        resetCycle();
    }

    @Override
    protected void onDisable() {
        state = State.IDLE;
        cooldownTicks = 0;
        waitTicks = 0;
        noScreenTicks = 0;
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.gameMode == null) {
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        switch (state) {
            case IDLE -> tickIdle();
            case OPEN_AH -> tickOpenAh();
            case CLICK_STORAGE -> tickClickStorage();
            case CLICK_RELIST -> tickClickRelist();
            case CLOSE -> finishCycle();
        }
    }

    private void tickIdle() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        resetCycle();
    }

    private void resetCycle() {
        if (player() == null) {
            state = State.IDLE;
            return;
        }
        if (player().containerMenu != null && player().containerMenu != player().inventoryMenu) {
            player().closeContainer();
        }
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("ah");
        }
        state = State.OPEN_AH;
        noScreenTicks = 0;
        waitTicks = 20;
    }

    private void tickOpenAh() {
        AbstractContainerMenu menu = player().containerMenu;
        if (menu instanceof ChestMenu) {
            noScreenTicks = 0;
            state = State.CLICK_STORAGE;
            waitTicks = 2;
            return;
        }

        if (++noScreenTicks >= OPEN_TIMEOUT_TICKS) {
            if (mc.getConnection() != null) {
                mc.getConnection().sendCommand("ah");
            }
            noScreenTicks = 0;
            waitTicks = 20;
            return;
        }
        waitTicks = TICK_DELAY;
    }

    private void tickClickStorage() {
        AbstractContainerMenu menu = player().containerMenu;
        if (!(menu instanceof ChestMenu chestMenu)) {
            state = State.OPEN_AH;
            waitTicks = TICK_DELAY;
            return;
        }

        if (findRelistSlot(chestMenu) >= 0) {
            state = State.CLICK_RELIST;
            waitTicks = 2;
            return;
        }

        int storage = findStorageSlot(chestMenu);
        if (storage < 0) {
            waitTicks = TICK_DELAY;
            return;
        }

        mc.gameMode.handleContainerInput(chestMenu.containerId, storage, 0, ContainerInput.PICKUP, player());
        state = State.CLICK_RELIST;
        waitTicks = 8;
    }

    private void tickClickRelist() {
        AbstractContainerMenu menu = player().containerMenu;
        if (!(menu instanceof ChestMenu chestMenu)) {
            state = State.OPEN_AH;
            waitTicks = TICK_DELAY;
            return;
        }

        int relist = findRelistSlot(chestMenu);
        if (relist < 0) {
            waitTicks = TICK_DELAY;
            return;
        }

        mc.gameMode.handleContainerInput(chestMenu.containerId, relist, 0, ContainerInput.PICKUP, player());
        state = State.CLOSE;
        waitTicks = 6;
    }

    private void finishCycle() {
        if (player() != null && player().containerMenu != null && player().containerMenu != player().inventoryMenu) {
            player().closeContainer();
        }
        state = State.IDLE;
        cooldownTicks = PERIOD_TICKS;
        waitTicks = 0;
        noScreenTicks = 0;
    }

    private int findStorageSlot(ChestMenu handler) {
        int containerSlots = handler.getRowCount() * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = handler.getSlot(i);
            if (slot.hasItem() && slot.getItem().is(Items.ENDER_CHEST)) {
                return i;
            }
        }
        return findTextSlot(handler, "хранилище");
    }

    private int findRelistSlot(ChestMenu handler) {
        int containerSlots = handler.getRowCount() * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = handler.getSlot(i);
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            String text = stackText(stack);
            if (stack.is(Items.CLOCK) && text.contains("перевыстав")) {
                return i;
            }
        }
        return findTextSlot(handler, "перевыставить предметы");
    }

    private int findTextSlot(ChestMenu handler, String needle) {
        int containerSlots = handler.getRowCount() * 9;
        for (int i = 0; i < containerSlots; i++) {
            Slot slot = handler.getSlot(i);
            if (slot.hasItem() && stackText(slot.getItem()).contains(needle)) {
                return i;
            }
        }
        return -1;
    }

    private String stackText(ItemStack stack) {
        StringBuilder text = new StringBuilder(clean(stack.getHoverName().getString()));
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                text.append(' ').append(clean(line.getString()));
            }
        }
        return text.toString();
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("§.", "").trim().toLowerCase();
    }
}
