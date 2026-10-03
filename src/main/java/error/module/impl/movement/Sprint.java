package error.module.impl.movement;

import net.minecraft.world.effect.MobEffects;
import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;

/**
 */
public class Sprint extends Module {

    public Sprint() {
        super("Sprint", "Само за тебя Стантит", Category.MOVEMENT);
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;
        boolean canSprint = mc.options.keyUp.isDown()
                && !player().isShiftKeyDown()
                && !player().hasEffect(MobEffects.BLINDNESS)
                && player().getFoodData().getFoodLevel() > 6;

        if (canSprint) {
            player().setSprinting(true);
        }
    }
}