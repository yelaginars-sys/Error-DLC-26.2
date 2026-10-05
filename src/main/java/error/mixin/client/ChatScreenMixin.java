package error.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;

import static error.IMinecraft.mc;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow protected EditBox input;


    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onChatInput(String message, boolean addToRecentChat, CallbackInfo ci) {
        if (Client.INSTANCE.commandManager.execute(message)) {
            if (addToRecentChat) {
                mc.gui.hud.getChat().addRecentChat(message);
            }
            Minecraft.getInstance().gui.setScreen(null);
            ci.cancel();
        }
    }
}