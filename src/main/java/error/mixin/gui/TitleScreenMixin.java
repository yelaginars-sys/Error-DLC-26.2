package error.mixin.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.ui.mainmenu.CustomTitleScreen;

/**
 * Redirect vanilla TitleScreen → CustomTitleScreen.
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void onInit(CallbackInfo ci) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        if (this.minecraft != null && !(this.minecraft.gui.screen() instanceof CustomTitleScreen)) {
            ci.cancel();
            this.minecraft.setScreenAndShow(new CustomTitleScreen());
        }
    }
}