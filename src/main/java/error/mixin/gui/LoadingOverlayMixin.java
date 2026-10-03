package error.mixin.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;

/**
 * LoadingOverlay — vanilla behaviour restored.
 * Custom splash screen removed; vanilla loading screen is shown instead.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    // Vanilla loading overlay — no overrides.
}