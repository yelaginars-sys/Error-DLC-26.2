package error.module.impl.misc;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;
import error.module.Category;
import error.module.Module;

import java.util.HashSet;
import java.util.Set;

/**
 */
public class ItemScroller extends Module {
    public static ItemScroller INSTANCE;
    private final Set<Integer> draggedSlots = new HashSet<>();

    public ItemScroller() {
        super("ItemScroller", "Слоты крутить быстро", Category.MISC);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        draggedSlots.clear();
    }


    public boolean onMouseDragged(AbstractContainerScreen<?> screen, Slot slot, MouseButtonEvent event) {
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


    public boolean onKeyPressed(AbstractContainerScreen<?> screen, Slot hoveredSlot, KeyEvent event) {
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