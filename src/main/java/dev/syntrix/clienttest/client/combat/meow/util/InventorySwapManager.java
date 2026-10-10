package dev.syntrix.clienttest.client.combat.meow.util;
import net.minecraft.client.player.LocalPlayer;
/** A container screen owns inventory operations while it is open. */
public final class InventorySwapManager {
    public static boolean isSwapInProgress(LocalPlayer player) {
        return player==null||player.containerMenu!=player.inventoryMenu;
    }
    private InventorySwapManager() {}
}
