package error;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import error.IMinecraft;

/**
 */
public interface ScreenProvider extends IMinecraft {
    default Screen currentScreen() {
        return screen();
    }
    default AbstractContainerScreen<?> containerScreen() {
        return currentScreen() instanceof AbstractContainerScreen<?> containerScreen ? containerScreen : null;
    }
}
