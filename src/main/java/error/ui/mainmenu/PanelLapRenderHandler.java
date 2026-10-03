package error.ui.mainmenu;

import net.minecraft.client.Minecraft;
import error.util.RenderExtend;
import error.event.EventTarget;
import error.event.list.MenuRenderEvent;
import error.util.render.Render2DUtil;

/**
 */

public final class PanelLapRenderHandler {
    @EventTarget
    public void пошелкатынахуйкомпоти(MenuRenderEvent event) {
        Minecraft minecraft = event.getClient();
        RenderExtend.enter2D(event.getGui(), event.getGuiGraphicsExtractor(), event.getDeltaTracker());
        try {int screenWidth = minecraft.getWindow().getGuiScaledWidth();int screenHeight = minecraft.getWindow().getGuiScaledHeight();Render2DUtil.beginFrame();
            PanelRefractions.render(minecraft, event.getGuiGraphicsExtractor(), screenWidth, screenHeight);Render2DUtil.flush();
        } finally {
            RenderExtend.exit2D();}
    }
}