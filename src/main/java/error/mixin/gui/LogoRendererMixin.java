package error.mixin.gui;

import net.minecraft.client.gui.components.LogoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LogoRenderer.class)
public class LogoRendererMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideLogoState(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "renderLogo", at = @At("HEAD"), cancellable = true, require = 0)
    private void hideLogoRender(CallbackInfo ci) {
        ci.cancel();
    }
}
