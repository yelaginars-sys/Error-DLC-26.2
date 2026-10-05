package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayList;
import java.util.List;

public class PlayerFakelags extends Module {
    public static PlayerFakelags INSTANCE;

    public final ModeSetting mode = mode("Режим", "Blink", "Blink", "Pulse");
    public final SliderSetting delay = slider("Задержка (MS)", 500.0F, 50.0F, 2000.0F, 50.0F);
    public final CheckBox onlyMovement = checkbox("Только движение", true);

    private final List<Packet<?>> packetQueue = new ArrayList<>();
    private long lastFlushTime = 0L;
    private boolean isFlushing = false;

    public PlayerFakelags() {
        super("PlayerFakeLags", "Искусственные сетевые лаги (Blink / Pulse)", Category.MOVEMENT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        packetQueue.clear();
        lastFlushTime = System.currentTimeMillis();
        isFlushing = false;
    }

    @Override
    protected void onDisable() {
        flushQueue();
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;

        if (mode.is("Pulse")) {
            long now = System.currentTimeMillis();
            if (now - lastFlushTime >= delay.getValue().longValue()) {
                flushQueue();
                lastFlushTime = now;
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null || isFlushing) return;

        if (event.isSend()) {
            Packet<?> p = event.getPacket();
            if (onlyMovement.getValue()) {
                if (p instanceof ServerboundMovePlayerPacket) {
                    packetQueue.add(p);
                    event.setCancelled(true);
                }
            } else {
                packetQueue.add(p);
                event.setCancelled(true);
            }
        }
    }

    private void flushQueue() {
        if (packetQueue.isEmpty()) return;

        isFlushing = true;
        if (mc.getConnection() != null) {
            for (Packet<?> packet : packetQueue) {
                mc.getConnection().send(packet);
            }
        }
        packetQueue.clear();
        isFlushing = false;
    }
}
