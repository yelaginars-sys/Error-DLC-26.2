package error.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;
import error.event.EventManager;
import error.event.list.GameTickEvent;
import error.event.list.WorldJoinEvent;
import error.event.list.WorldLeaveEvent;
import error.account.AccountManager;

/**
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    public ClientLevel level;
    @Unique
    private static final GameTickEvent TICK_EVENT = new GameTickEvent();
    @Unique
    private static final WorldLeaveEvent WORLD_LEAVE_EVENT = new WorldLeaveEvent();
    @Unique
    private static final WorldJoinEvent WORLD_JOIN_EVENT = new WorldJoinEvent();
    @Unique
    private ClientLevel previousLevel;
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Client.INSTANCE.eventManager.call(TICK_EVENT);
    }
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void capturePreviousLevel(ClientLevel level, CallbackInfo ci) {
        this.previousLevel = this.level;
    }
    @Shadow
    @Nullable
    public LocalPlayer player;
    @Inject(method = "setLevel", at = @At("TAIL"))
    private void onSetLevel(ClientLevel level, CallbackInfo ci) {
        Minecraft client = (Minecraft) (Object) this;
        ClientLevel previous = this.previousLevel;
        Client.INSTANCE.eventManager.call(WORLD_LEAVE_EVENT.set(client, previous));
        EventManager.call(WORLD_JOIN_EVENT.set(client, level));
    }
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onMinecraftInit(GameConfig gameConfig, CallbackInfo ci) {
        AccountManager.getInstance().applyActiveSession();
        try {
            long windowHandle = ((Minecraft) (Object) this).getWindow().handle();
            error.util.WindowUtil.applyDarkTitleBar(windowHandle);
            org.lwjgl.glfw.GLFW.glfwSetWindowTitle(windowHandle, "Error DLC 26.2");
        } catch (Throwable t) {}
    }

    @Inject(method = "setScreenAndShow", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        if (error.module.impl.misc.UnHook.unhooked) return;
        if (screen != null && screen.getClass() == net.minecraft.client.gui.screens.TitleScreen.class) {
            ci.cancel();
            ((Minecraft) (Object) this).setScreenAndShow(new error.ui.mainmenu.CustomTitleScreen());
        }
    }
}