package error.mixin.input;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;
import error.event.Events;

/**
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharacterTyped(long window, CharacterEvent characterEvent, CallbackInfo ci) {
        if (!characterEvent.isAllowedChatCharacter()) return;

        if (Client.getInstance().getEventManager().call(
                Events.CHARACTER_INPUT.set(window, characterEvent.codepoint())
        ).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (Client.getInstance() != null && Client.getInstance().getModuleManager() != null) {
            Client.getInstance().getModuleManager().onKey(keyEvent.key(), action);
        }

        if (Client.getInstance() != null && Client.getInstance().getEventManager() != null) {
            if (Client.getInstance().getEventManager().call(
                    Events.KEYBOARD_INPUT.set(window, keyEvent.key(), keyEvent.scancode(), action, keyEvent.modifiers())
            ).isCancelled()) {
                ci.cancel();
            }
        }
    }
}