package error.mixin.render;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import error.module.impl.render.Ambience;

/**
 */
@Mixin(ClientLevel.ClientLevelData.class)
public class ClientLevelDataMixin {

    @Inject(method = "getGameTime", at = @At("HEAD"), cancellable = true)
    private void onGetGameTime(CallbackInfoReturnable<Long> cir) {
        if (error.module.impl.render.Atmosphere.INSTANCE != null && error.module.impl.render.Atmosphere.INSTANCE.isEnabled() && error.module.impl.render.Atmosphere.INSTANCE.hasTimeOverride()) {
            cir.setReturnValue(error.module.impl.render.Atmosphere.INSTANCE.getTimeOverride());
            return;
        }
        if (Ambience.INSTANCE != null && Ambience.INSTANCE.isCustomTimeEnabled()) {
            cir.setReturnValue(Ambience.INSTANCE.getCustomDayTime());
        }
    }
}