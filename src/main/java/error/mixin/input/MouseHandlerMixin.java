package error.mixin.input;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;
import error.util.RotationHandler;
import error.event.Events;
import error.event.list.LookEvent;
import error.ui.mainmenu.PanelRefractions;
import error.module.impl.misc.FreeCam;

/**
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (Client.getInstance() != null && Client.getInstance().getModuleManager() != null) {
            Client.getInstance().getModuleManager().onMouse(buttonInfo.button(), action);
        }

        if (Client.getInstance() != null && Client.getInstance().getEventManager() != null) {
            if (Client.getInstance().getEventManager().call(
                    Events.MOUSE_INPUT.set(window, buttonInfo.button(), action, buttonInfo.modifiers())
            ).isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (PanelRefractions.blocksInput()) {
            PanelRefractions.handleScroll(vertical);
            ci.cancel();
        }
    }

    @Redirect(
            method = "turnPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")
    )
    private void onTurnPlayer(LocalPlayer player, double yawDelta, double pitchDelta) {
        if (FreeCam.INSTANCE != null && FreeCam.INSTANCE.isEnabled()) {
            FreeCam.INSTANCE.turn(yawDelta, pitchDelta);
            return;
        }

        LookEvent event = new LookEvent(yawDelta, pitchDelta);
        Client.getInstance().getEventManager().call(event);

        if (event.isCancelled()) {
            return;
        }

        if (RotationHandler.onMouseTurn(event.getCursorDeltaX(), event.getCursorDeltaY())) {
            return;
        }

        player.turn(event.getCursorDeltaX(), event.getCursorDeltaY());
    }
}