package dev.syntrix.clienttest.client;

import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import dev.syntrix.clienttest.client.gui.ClickGuiController;
import dev.syntrix.clienttest.client.visual.VisualFriends;
import dev.syntrix.clienttest.client.visual.VisualWorldRenderer;
import net.fabricmc.api.ClientModInitializer;

public final class CrystalAuraClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClickGuiController.initialize();
        CrystalAuraModule.initialize();
        VisualFriends.initialize();
        VisualWorldRenderer.initialize();
    }
}
