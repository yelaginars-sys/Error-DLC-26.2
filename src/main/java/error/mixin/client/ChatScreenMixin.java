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

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (input != null && !error.module.impl.misc.UnHook.unhooked) {
            ClientCommandSuggestions.getInstance().update(input.getValue());
            float screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            float screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            boolean setExtractor = error.util.RenderExtend.currentGuiGraphicsExtractor() == null;
            if (setExtractor && graphics != null) {
                error.util.RenderExtend.enter2D(Minecraft.getInstance().gui, graphics, null);
            }
            try {
                ClientCommandSuggestions.getInstance().render(screenW, screenH, 1.0F);
            } finally {
                if (setExtractor) {
                    error.util.RenderExtend.exit2D();
                }
            }
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
}