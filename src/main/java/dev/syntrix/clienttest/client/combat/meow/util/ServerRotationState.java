package dev.syntrix.clienttest.client.combat.meow.util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
/** Packet-facing aim. Silent rotations never modify the local player's view or movement. */
public final class ServerRotationState {
    private static boolean active;
    private static float yaw,pitch;
    public static void operation003(boolean value) {
        var player=Minecraft.getInstance().player;
        if(value&&!active&&player!=null) { yaw=player.getYRot();pitch=player.getXRot(); }
        active=value&&player!=null;
    }
    public static boolean operation005() { return active; }
    public static float getYaw() { return Minecraft.getInstance().player.getYRot(); }
    public static float getPitch() { return Minecraft.getInstance().player.getXRot(); }
    public static float serverYaw() { return active?yaw:getYaw(); }
    public static float serverPitch() { return active?pitch:getPitch(); }
    public static void setServerYaw(float value) {
        if(active)yaw=value;else Minecraft.getInstance().player.setYRot(value);
    }
    public static void setServerPitch(float value) {
        value=Mth.clamp(value,-90,90);
        if(active)pitch=value;else Minecraft.getInstance().player.setXRot(value);
    }
    public static void operation007() { active=false; }
    private ServerRotationState() {}
}
