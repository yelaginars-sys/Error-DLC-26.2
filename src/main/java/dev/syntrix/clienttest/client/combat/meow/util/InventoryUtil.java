package dev.syntrix.clienttest.client.combat.meow.util;
import net.minecraft.world.item.Item;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;
public final class InventoryUtil implements MinecraftAccess {
    public static int findHotbarItem(Item item) {
        if(mc.player==null)return -1;
        for(int slot=0;slot<9;slot++)if(mc.player.getInventory().getItem(slot).is(item))return slot;
        return -1;
    }
    private InventoryUtil() {}
}
