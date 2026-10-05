package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class ElytraBooster extends Module {

    public static ElytraBooster INSTANCE;

    public final ModeSetting mode = mode("Режим", "Статический", "Статический", "Динамический", "Neuro", "Кастомный", "ReallyWorld", "RW 1.21", "BravoHvH");
    public final SliderSetting boostPower = slider("Сила Буста", 1.5F, 0.1F, 6.0F, 0.05F);
    public final CheckBox antiOverfly = checkbox("Анти Перелет", true);
    public final SliderSetting triggerDistance = slider("Дистанция Срабатывания", 3.0F, 0.5F, 6.0F, 0.1F);
    public final SliderSetting correctionSpeed = slider("Скорость Коррекции", 1.5F, 0.1F, 3.0F, 0.1F);

    public ElytraBooster() {
        super("ElytraBooster", "Ускорение и контроль полета на элитрах (Energy)", Category.MOVEMENT);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;
        if (!player().isFallFlying()) return;

        float power = boostPower.getValue();
        String m = mode.getValue();

        Vec3 look = player().getLookAngle();
        Vec3 vel = player().getDeltaMovement();

        switch (m) {
            case "Статический" -> {
                double speed = power * 0.45;
                Vec3 targetVel = look.scale(speed);
                player().setDeltaMovement(targetVel.x, targetVel.y, targetVel.z);
            }
            case "Динамический" -> {
                double factor = 1.0 + (power * 0.15);
                player().setDeltaMovement(vel.x * factor, vel.y * factor, vel.z * factor);
            }
            case "ReallyWorld", "RW 1.21" -> {
                // Smooth RW booster without velocity flag
                float pitch = player().getXRot();
                double hFactor = Math.cos(Math.toRadians(pitch)) * (power * 0.35);
                double vFactor = -Math.sin(Math.toRadians(pitch)) * (power * 0.35);
                player().setDeltaMovement(look.x * hFactor, vFactor, look.z * hFactor);
            }
            case "BravoHvH" -> {
                double boost = power * 0.6;
                player().setDeltaMovement(look.x * boost, look.y * boost * 0.8, look.z * boost);
            }
            case "Neuro", "Кастомный" -> {
                float yaw = Mth.wrapDegrees(player().getYRot());
                float pitch = player().getXRot();
                double curve = 1.0 + Math.sin(Math.toRadians(pitch)) * 0.2;
                player().setDeltaMovement(look.x * power * 0.4 * curve, vel.y + (look.y * 0.05), look.z * power * 0.4 * curve);
            }
        }
    }
}
