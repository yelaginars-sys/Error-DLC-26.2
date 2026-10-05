package error.module.impl.misc;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;
import error.module.Category;
import error.module.Module;
import error.setting.impl.SliderSetting;

import java.util.HashSet;
import java.util.Set;

public class ItemScroller extends Module {
    public static ItemScroller INSTANCE;

    public final SliderSetting delay = slider("Задержка", 50.0F, 0.0F, 200.0F, 1.0F);

    private final Set<Integer> draggedSlots = new HashSet<>();
    private long lastMoveTime = 0L;

    public ItemScroller() {
        super("ItemScroller", "Убирает задержку перемещения предметов", Category.MISC);
        INSTANCE = this;
    }

    public boolean canQuickMove() {
        long now = System.currentTimeMillis();
        if (now - lastMoveTime < (long) (float) delay.getValue()) {
            return false;
        }
        lastMoveTime = now;
        return true;
    }

    public void resetTimer() {
        lastMoveTime = 0L;
    }

    @Override
    public void onDisable() {
        resetTimer();
        draggedSlots.clear();
        super.onDisable();
    }

    public boolean onMouseDragged(AbstractContainerScreen<?> screen, Slot slot, net.minecraft.client.input.MouseButtonEvent event) {
        if (!isEnabled() || mc.player == null || mc.gameMode == null) return false;

        long window = mc.getWindow().handle();
        boolean isLmb = event.button() == 0 || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean isShift = isShiftDown(window);

        if (!isLmb || !isShift) {
            draggedSlots.clear();
            return false;
        }

        if (slot == null || !slot.hasItem()) return false;
        if (draggedSlots.contains(slot.index)) return false;

        if (!canQuickMove()) return false;

        draggedSlots.add(slot.index);
        mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
        return true;
    }

    public void onMouseClicked(Slot slot) {
        if (slot != null && isShiftDown(mc.getWindow().handle())) {
            draggedSlots.add(slot.index);
        }
    }

    public void onMouseReleased() {
        draggedSlots.clear();
    }

    public boolean onKeyPressed(AbstractContainerScreen<?> screen, Slot hoveredSlot, net.minecraft.client.input.KeyEvent event) {
        if (!isEnabled() || mc.player == null || mc.gameMode == null) return false;

        long window = mc.getWindow().handle();
        if (event.key() == GLFW.GLFW_KEY_Q && isCtrlDown(window) && isShiftDown(window)) {
            if (hoveredSlot == null || !hoveredSlot.hasItem()) return false;

            Item targetItem = hoveredSlot.getItem().getItem();

            for (Slot slot : screen.getMenu().slots) {
                if (slot.hasItem() && slot.getItem().getItem() == targetItem) {
                    mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 1, ContainerInput.THROW, mc.player);
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isShiftDown(long window) {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static boolean isCtrlDown(long window) {
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }
}