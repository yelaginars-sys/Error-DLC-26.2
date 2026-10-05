package error.mixin.render;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import error.Client;

/**
 */
@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {

    @Shadow
    private float deltaTicks;

    @Inject(method = "advanceGameTime", at = @At("HEAD"), cancellable = true)
    private void onAdvanceGameTimeHead(long currentMs, CallbackInfoReturnable<Integer> cir) {
        if (error.module.impl.movement.Disabler.isThrottleActive()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(
            method = "advanceGameTime",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/DeltaTracker$Timer;deltaTickResidual:F",
                    ordinal = 0
            )
    )
    private void onAdvanceGameTime(long currentMs, CallbackInfoReturnable<Integer> cir) {
        if (Client.Timer != 1.0F) {
            this.deltaTicks *= Client.Timer;
        }
    }
}