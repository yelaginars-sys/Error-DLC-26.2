package error.mixin.client;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import error.module.impl.render.Ambience;

/**
 * Create by daun kvass
 */
@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {

    @Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
    private void onGetTotalTicks(Holder<WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        if (Ambience.INSTANCE != null && Ambience.INSTANCE.isCustomTimeEnabled()) {
            cir.setReturnValue(Ambience.INSTANCE.getCustomDayTime());
        }
    }
}