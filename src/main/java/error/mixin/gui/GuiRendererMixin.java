package error.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.util.render.renders.MenuBacks;
import error.util.render.pipeline.Pipelines;

import java.util.List;
import java.util.function.Supplier;

/**
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow
    @Final
    private List<?> draws;

    @Unique
    private final IntList error$glassSplits = new IntArrayList();
    @Unique
    private boolean error$previousElementWasGlass;

    @Inject(method = "prepare", at = @At("HEAD"))
    private void error$resetGlassSplits(CallbackInfo ci) {
        this.error$glassSplits.clear();
        this.error$previousElementWasGlass = false;
    }

    @Inject(method = "addElementToMesh", at = @At("HEAD"))
    private void error$markGlassRuns(GuiElementRenderState elementState, CallbackInfo ci) {
        boolean glass = error$isGlassPipeline(elementState.pipeline());
        if (glass && !this.error$previousElementWasGlass) {
            this.error$glassSplits.add(this.draws.size());
        }
        this.error$previousElementWasGlass = glass;
    }

    @WrapOperation(
            method = "draw",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;executeDrawRange(Ljava/util/function/Supplier;Lcom/mojang/blaze3d/pipeline/RenderTarget;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;II)V"
            )
    )
    private void error$captureBackdropBeforeGlassRuns(
            GuiRenderer instance,
            Supplier<String> label,
            RenderTarget mainRenderTarget,
            GpuBufferSlice dynamicTransforms,
            int startIndex,
            int endIndex,
            Operation<Void> original
    ) {
        int cursor = startIndex;
        for (int i = 0; i < this.error$glassSplits.size(); i++) {
            int split = this.error$glassSplits.getInt(i);
            if (split < cursor || split >= endIndex) {
                continue;
            }
            if (split > cursor) {
                original.call(instance, label, mainRenderTarget, dynamicTransforms, cursor, split);
            }

            MenuBacks.captureNow(mainRenderTarget);
            cursor = split;
        }
        if (cursor < endIndex) {
            original.call(instance, label, mainRenderTarget, dynamicTransforms, cursor, endIndex);
        }
    }

    /**
     * Block CubeMap.render calls that crash when the panorama texture hasn't been
     * initialized yet (e.g. during recovery reload after a shader compile failure).
     */
    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/CubeMap;render(FF)V"
            )
    )
    private void error$safeCubeMapRender(
            CubeMap cubeMap,
            float f1, float f2,
            Operation<Void> original
    ) {
        try {
            original.call(cubeMap, f1, f2);
        } catch (IllegalStateException ignored) {
            // Panorama texture not yet initialized (shader compile failure during startup recovery)
            // Silently skip to prevent crash; custom menu renders its own background
        }
    }

    @Unique
    private static boolean error$isGlassPipeline(RenderPipeline pipeline) {
        return pipeline == Pipelines.BLUR_RECT;
    }
}