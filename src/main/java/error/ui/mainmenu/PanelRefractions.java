package error.ui.mainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class PanelRefractions {
    public static boolean toggle(Minecraft mc) {
        return false;
    }

    public static void open(Minecraft mc) {}

    public static void close(Minecraft mc) {}

    public static void openModal(Object modal) {}

    public static boolean isOpen() {
        return false;
    }

    public static boolean isTyping() {
        return false;
    }

    public static boolean blocksInput() {
        return false;
    }

    public static boolean handleMouseButton(Minecraft mc, int button, int action) {
        return false;
    }

    public static boolean handleKey(int key, int scanCode, int modifiers) {
        return false;
    }

    public static void handleChar(int codePoint) {}

    public static void handleScroll(double vertical) {}

    public static void render(Minecraft mc, GuiGraphicsExtractor extractor, int screenWidth, int screenHeight) {}
}