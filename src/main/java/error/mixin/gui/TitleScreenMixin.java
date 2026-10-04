package error.mixin.gui;

import error.IMinecraft;
import error.ui.mainmenu.CustomTitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen implements IMinecraft {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInitHead(CallbackInfo ci) {
        if (mc != null && mc.gui != null && !(mc.gui.screen() instanceof CustomTitleScreen)) {
            ci.cancel();
            mc.setScreenAndShow(new CustomTitleScreen());
        }
    }
}