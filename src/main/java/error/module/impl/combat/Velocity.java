package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Module {
    public static Velocity INSTANCE;

    public final ModeSetting mode = mode("Режим", "Cancel", "Cancel", "Custom", "Grim");
    public final SliderSetting horizontal = slider("По горизонтали %", 0.0F, 0.0F, 100.0F, 1.0F)
            .visible(() -> mode.is("Custom"));
    public final SliderSetting vertical = slider("По вертикали %", 0.0F, 0.0F, 100.0F, 1.0F)
            .visible(() -> mode.is("Custom"));
    public final CheckBox explosions = checkbox("Взрывы", true);

    public Velocity() {
        super("Velocity", "Снижает или убирает отдачу от ударов и взрывов", Category.COMBAT);
        INSTANCE = this;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null) return;
        if (!event.isReceive()) return;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() == player().getId()) {
                if (mode.is("Cancel") || mode.is("Grim")) {
                    event.cancel();
                } else if (mode.is("Custom")) {
                    if (horizontal.getValue() == 0.0F && vertical.getValue() == 0.0F) {
                        event.cancel();
                    } else {
                        Vec3 motion = packet.movement();
                        double hFactor = horizontal.getValue() / 100.0;
                        double vFactor = vertical.getValue() / 100.0;
                        player().setDeltaMovement(new Vec3(
                                motion.x * hFactor,
                                motion.y * vFactor,
                                motion.z * hFactor
                        ));
                        event.cancel();
                    }
                }
            }
        } else if (explosions.getValue() && event.getPacket() instanceof ClientboundExplodePacket) {
            if (mode.is("Cancel") || mode.is("Grim")) {
                event.cancel();
            } else if (mode.is("Custom")) {
                if (horizontal.getValue() == 0.0F && vertical.getValue() == 0.0F) {
                    event.cancel();
                }
            }
        }
    }
}
