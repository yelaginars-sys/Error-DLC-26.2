package error.util.display;

import error.event.EventManager;
import error.event.list.EventDisplay;
import error.util.display.batch.DisplayBatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.GameRenderState;

public final class DisplayUtil {
    public static final float SCALE = 2.0F;

    private DisplayUtil() {
    }

    public static float width() {
        return Minecraft.getInstance().getWindow().getWidth() / SCALE;
    }

    public static float height() {
        return Minecraft.getInstance().getWindow().getHeight() / SCALE;
    }

    public static void render(Minecraft minecraft, GameRenderState state) {
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, state.guiRenderState, 0, 0);
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().scale(SCALE / (float) minecraft.getWindow().getGuiScale());
        DisplayBatcher.begin(graphics);
        try {
            EventManager.call(EventDisplay.get(graphics));
        } finally {
            DisplayBatcher.end();
            graphics.pose().popMatrix();
        }
    }

    public static void flush() {
        DisplayBatcher.flush();
    }

    public static void nextStratum(GuiGraphicsExtractor graphics) {
        DisplayBatcher.flush();
        graphics.nextStratum();
    }
}