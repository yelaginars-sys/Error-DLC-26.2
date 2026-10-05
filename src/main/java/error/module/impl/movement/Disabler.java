package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

public final class Disabler extends Module {

    public static Disabler INSTANCE;

    public final ModeSetting mode = mode("Mode", "Grim Desync", "Grim Desync", "Matrix", "Timer Balance");
    public final SliderSetting tpsRate = slider("TPS Rate", 10.0F, 1.0F, 20.0F, 0.5F);

    private static long lastTickNano = 0L;

    public Disabler() {
        super("Disabler", "Десинхронизация тиков и байпасс античитов (Grim/Matrix)", Category.MOVEMENT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        lastTickNano = 0L;
    }

    @Override
    protected void onDisable() {
        lastTickNano = 0L;
    }

    public static boolean isThrottleActive() {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
        if (Minecraft.getInstance().player == null) return false;

        float rate = INSTANCE.tpsRate.getValue();
        long now = System.nanoTime();
        long targetInterval = (long) (1_000_000_000.0 / Math.max(1.0F, rate));

        if (lastTickNano != 0L && (now - lastTickNano) < targetInterval) {
            return true;
        }

        lastTickNano = now;
        return false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            // When server sends position rollback during active disabler, stabilize tick interval
            lastTickNano = System.nanoTime();
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        // Keeps desync state smooth during client ticks
    }
}
