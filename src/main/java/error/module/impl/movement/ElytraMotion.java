package error.module.impl.movement;

import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import error.util.client.clients.Targets;
import error.event.EventTarget;
import error.event.list.GameTickEvent;
import error.module.Category;
import error.module.Module;

/**
 */
@Getter
public class ElytraMotion extends Module {

    private boolean waitTarget;

    public ElytraMotion() {
        super("ElytraMotion", "Остановка перед целью на элитрах", Category.MOVEMENT);
    }

    @Override
    public void onDisable() {
        if (inGame() && player() != null) {
            this.waitTarget = false;
            player().setNoGravity(false);
        }
        super.onDisable();
    }

    @EventTarget
    public void onGameTick(GameTickEvent event) {
        if (!inGame() || player() == null) return;

        LivingEntity target = Targets.getCurrentTarget();

        if (target == null) {
            if (!waitTarget) {
                player().setNoGravity(false);
                waitTarget = true;
            }
            return;
        } else {
            waitTarget = false;
        }

        double distance = player().getEyePosition().distanceTo(target.getBoundingBox().getCenter());
        boolean shouldChase = target.isFallFlying() && (target.getDeltaMovement().length() * 20.0 >= 13.0);
        double stopDist = target.isFallFlying() ? 1.5 : 2.8;

        if (player().isFallFlying() && distance < stopDist && !shouldChase) {
            player().setDeltaMovement(Vec3.ZERO);
            player().setNoGravity(true);
        } else {
            player().setNoGravity(false);
        }
    }
}