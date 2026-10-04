package error.mixin.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.module.impl.render.BetterMinecraft;

import java.util.Map;
import java.util.UUID;

/**
 */
@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {

    @Shadow
    @Final
    private Map<UUID, LerpingBossEvent> events;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onExtractRenderState(GuiGraphicsExtractor extractor, CallbackInfo ci) {
        if (error.module.impl.render.Interface.INSTANCE != null 
                && error.module.impl.render.Interface.INSTANCE.isEnabled() 
                && error.module.impl.render.Interface.INSTANCE.dynamicIsland.getValue()) {
            if (error.ui.hud.impl.DynamicIslandHud.interceptBossBars(this.events)) {
                ci.cancel();
                return;
            }
        }
        if (BetterMinecraft.INSTANCE != null && BetterMinecraft.INSTANCE.isEnabled() && BetterMinecraft.INSTANCE.modes.isEnabled("BossBar")) {
            ci.cancel();
            if (!this.events.isEmpty()) {
                BetterMinecraft.INSTANCE.renderBossBars(this.events);
            }
        }
    }
}