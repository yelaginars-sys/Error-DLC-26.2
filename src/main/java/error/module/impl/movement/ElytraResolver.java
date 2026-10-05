package error.module.impl.movement;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ElytraResolver extends Module {

    public static ElytraResolver INSTANCE;

    public final SliderSetting approachDist = slider("Долет", 3.0F, 1.5F, 6.0F, 0.1F);
    public final SliderSetting retreatDist = slider("Отлет", 8.0F, 4.0F, 16.0F, 0.5F);
    public final SliderSetting retreatAngle = slider("Угол отлета", 50.0F, 0.0F, 90.0F, 1.0F);
    public final ModeSetting retreatMode = mode("Режим отлета", "В сторону", "В сторону", "Вверх", "Диагональ");
    public final CheckBox alternateSides = checkbox("Чередовать стороны", true);

    public enum Phase {
        APPROACHING,
        RETREATING
    }

    private Phase currentPhase = Phase.APPROACHING;
    private LivingEntity currentTarget = null;
    private Vec3 retreatOffset = null;
    private long retreatStartTime = 0L;
    private boolean sideToggle = false;

    public ElytraResolver() {
        super("ElytraResolver", "Долет и отлет для элитра-ротки (Energy)", Category.MOVEMENT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        resetState();
    }

    @Override
    protected void onDisable() {
        resetState();
    }

    public void resetState() {
        this.currentPhase = Phase.APPROACHING;
        this.currentTarget = null;
        this.retreatOffset = null;
        this.retreatStartTime = 0L;
    }

    public Vec3 resolveAim(LivingEntity target, Vec3 aimPos) {
        if (!isEnabled() || player() == null || target == null || aimPos == null) {
            return aimPos;
        }

        if (!player().isFallFlying()) {
            resetState();
            return aimPos;
        }

        if (target != this.currentTarget) {
            this.currentTarget = target;
            this.currentPhase = Phase.APPROACHING;
            this.retreatOffset = null;
            this.retreatStartTime = System.currentTimeMillis();
        }

        double dist = player().position().add(0, target.getBbHeight() / 2.0F, 0).distanceTo(aimPos);

        if (this.currentPhase == Phase.APPROACHING && dist <= approachDist.getValue()) {
            this.currentPhase = Phase.RETREATING;
            this.retreatOffset = null;
            this.retreatStartTime = System.currentTimeMillis();
        } else if (this.currentPhase == Phase.RETREATING) {
            boolean farEnough = dist >= retreatDist.getValue();
            boolean timedOut = System.currentTimeMillis() - this.retreatStartTime > 2000L;
            if (farEnough || timedOut) {
                this.currentPhase = Phase.APPROACHING;
                this.retreatOffset = null;
                this.retreatStartTime = System.currentTimeMillis();
            }
        }

        if (this.currentPhase == Phase.APPROACHING) {
            return aimPos;
        } else {
            if (this.retreatOffset == null) {
                this.retreatOffset = calculateRetreatVector(aimPos, player().position());
            }
            return aimPos.add(this.retreatOffset.scale(retreatDist.getValue()));
        }
    }

    private Vec3 calculateRetreatVector(Vec3 targetPos, Vec3 playerPos) {
        Vec3 diff = playerPos.subtract(targetPos);
        Vec3 flat = new Vec3(diff.x, 0, diff.z);

        if (flat.lengthSqr() < 1.0E-6) {
            Vec3 rot = player().getLookAngle().scale(-1);
            flat = new Vec3(rot.x, 0, rot.z);
        }

        if (flat.lengthSqr() < 1.0E-6) {
            flat = new Vec3(0, 0, 1);
        }

        flat = flat.normalize();
        double angleDeg = retreatAngle.getValue();
        if (alternateSides.getValue()) {
            angleDeg = sideToggle ? angleDeg : -angleDeg;
            sideToggle = !sideToggle;
        }

        double rad = Math.toRadians(angleDeg);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        String m = retreatMode.getValue();
        return switch (m) {
            case "Вверх" -> new Vec3(flat.x * 0.3, 0.85, flat.z * 0.3).normalize();
            case "Диагональ" -> new Vec3(flat.x * cos - flat.z * sin, 0.45, flat.x * sin + flat.z * cos).normalize();
            default -> new Vec3(flat.x * cos - flat.z * sin, 0.05, flat.x * sin + flat.z * cos).normalize();
        };
    }
}
