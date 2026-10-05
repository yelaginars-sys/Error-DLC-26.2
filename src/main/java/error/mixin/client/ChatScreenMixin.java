package error.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import error.Client;
import error.command.ClientCommandSuggestions;

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


    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (input != null && !error.module.impl.misc.UnHook.unhooked) {
            if (ClientCommandSuggestions.getInstance().onKeyPressed(event.key(), input)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (input != null && !error.module.impl.misc.UnHook.unhooked) {
            if (ClientCommandSuggestions.getInstance().onMouseClicked(event.x(), event.y(), event.button(), input)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderStateTail(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.input != null && !error.module.impl.misc.UnHook.unhooked) {
            ClientCommandSuggestions.getInstance().update(this.input.getValue());
            float screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            float screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            error.util.RenderExtend.enter2D(null, extractor, null);
            error.util.display.batch.DisplayBatcher.begin(extractor);
            ClientCommandSuggestions.getInstance().render(screenW, screenH, 1.0F);
            error.util.display.batch.DisplayBatcher.end();
            error.util.RenderExtend.exit2D();
        }
    }
}