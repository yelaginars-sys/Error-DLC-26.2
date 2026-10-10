package dev.syntrix.clienttest.client.combat.meow.util;
import net.minecraft.client.Minecraft;
public final class BaritoneCompat {
    public static boolean isControllingMovement() {
        var player=Minecraft.getInstance().player;
        return player!=null&&player.input!=null&&player.input.getClass().getName().startsWith("baritone.");
    }
    private BaritoneCompat() {}
}
