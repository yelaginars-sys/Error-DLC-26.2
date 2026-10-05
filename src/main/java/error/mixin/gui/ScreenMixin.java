package error.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;
import error.event.EventManager;
import error.event.list.ScreenCloseEvent;

/**
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Unique
    private static final ScreenCloseEvent SCREEN_CLOSE_EVENT = new ScreenCloseEvent();
    @Shadow
    @Final
    protected Minecraft minecraft;
    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void onClose(CallbackInfo ci) {
        Client.INSTANCE.eventManager.call(SCREEN_CLOSE_EVENT.set(this.minecraft, (Screen) (Object) this)).isCancelled();
    }

    @Unique private long error$screenOpenTime = System.currentTimeMillis();

    @Inject(method = "init", at = @At("HEAD"))
    private void onScreenInit(CallbackInfo ci) {
        this.error$screenOpenTime = System.currentTimeMillis();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onExtractRenderStateHead(net.minecraft.client.gui.GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        error.util.RenderExtend.enter2D(null, extractor, null);
        error.util.display.batch.DisplayBatcher.begin(extractor);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderStateTail(net.minecraft.client.gui.GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Object self = this;
        if (self instanceof net.minecraft.client.gui.screens.worldselection.SelectWorldScreen 
                || self instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
                || self instanceof error.ui.account.AccountManagerScreen) {
            long elapsed = System.currentTimeMillis() - this.error$screenOpenTime;
            if (elapsed < 220L) {
                float progress = Math.clamp((float) elapsed / 220.0F, 0.0F, 1.0F);
                float ease = 1.0F - (1.0F - progress) * (1.0F - progress) * (1.0F - progress);
                int fadeAlpha = (int) ((1.0F - ease) * 255);
                if (fadeAlpha > 2) {
                    error.util.render.Render2DUtil.beginFrame();
                    error.util.render.Render2D.drawRect(0, 0, ((Screen) self).width, ((Screen) self).height, error.util.client.clients.ColorUtil.rgba(0, 0, 0, fadeAlpha));
                    int accentFlash = error.util.client.clients.ColorUtil.withAlpha(error.util.client.clients.Theme.getAccentColor(), (int) ((1.0F - ease) * 110));
                    error.util.render.Render2D.drawShadow(0, (((Screen) self).height - 30.0F) / 2.0F, ((Screen) self).width, 30.0F, 25.0F, 16.0F, accentFlash);
                    error.util.render.Render2DUtil.flush();
                }
            }
        }
        if (self instanceof net.minecraft.client.gui.screens.ChatScreen chatScreen) {
            net.minecraft.client.gui.components.EditBox input = ((error.mixin.accessor.ChatScreenAccessor) chatScreen).getInput();
            if (input != null && !error.module.impl.misc.UnHook.unhooked) {
                error.command.ClientCommandSuggestions.getInstance().update(input.getValue());
                float screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                float screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                error.command.ClientCommandSuggestions.getInstance().render(screenW, screenH, 1.0F);
            }
        }
        error.util.display.batch.DisplayBatcher.end();
        error.util.RenderExtend.exit2D();
    }

}
