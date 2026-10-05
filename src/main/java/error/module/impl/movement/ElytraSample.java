package error.module.impl.movement;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ElytraSample extends Module {

    public static ElytraSample INSTANCE;

    public final ModeSetting mode = mode("Режим точки предикта", "Плавная", "Плавная", "Резкая");
    public final SliderSetting predictForce = slider("Сила предикта", 2.5F, 0.5F, 5.0F, 0.5F);
    public final CheckBox showPredict = checkbox("Показывать предикт", true);

    public ElytraSample() {
        super("ElytraSample", "Режим точки предикта цели на элитрах (Energy)", Category.MOVEMENT);
        INSTANCE = this;
    }

    public Vec3 predictTarget(LivingEntity target, Vec3 currentPos, Vec3 currentVel) {
        if (!isEnabled() || target == null || currentPos == null) {
            return currentPos;
        }

        if (!target.isFallFlying()) {
            return currentPos;
        }

        Vec3 vel = (currentVel != null) ? currentVel : target.getDeltaMovement();
        float force = predictForce.getValue();
        boolean isSmooth = mode.is("Плавная");

        if (isSmooth) {
            // Smooth extrapolation with drag decay
            double decay = 0.99;
            double predX = currentPos.x + vel.x * force * decay;
            double predY = currentPos.y + vel.y * force - (0.05 * force);
            double predZ = currentPos.z + vel.z * force * decay;
            return new Vec3(predX, predY, predZ);
        } else {
            // Sharp direct projection
            return currentPos.add(vel.scale(force));
        }
    }
}
