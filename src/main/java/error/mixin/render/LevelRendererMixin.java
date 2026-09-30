package error.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;
import error.module.impl.render.BlockHighlight;
import error.module.impl.render.PopEffect;
import error.module.impl.render.Removals;
import error.util.render.world.RendererWorldProvider;
import error.util.render.world.MiasmWorlds;

/**
 * Create by daun kvass
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;
    @Unique
    private CameraRenderState popEffect$cameraRenderState;
    @Inject(method = "render", at = @At("TAIL"))
    private void renderWorldEffects(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean renderBlockOutline, CameraRenderState cameraRenderState, Matrix4fc projectionMatrix, GpuBufferSlice fogParameters, Vector4f skyColor, boolean hasCapturedFrustum, CallbackInfo ci) {
        MiasmWorlds.render(new RendererWorldProvider(this.levelRenderState, cameraRenderState, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true)
    private void onAddWeatherPass(FrameGraphBuilder frame, GpuBufferSlice fog, CallbackInfo ci) {
        if (Removals.INSTANCE != null && Removals.INSTANCE.isWeatherDisabled()) {
            ci.cancel();
        }
    }
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void renderPopChams(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        PopEffect popEffect = Client.INSTANCE.moduleManager.getModule(PopEffect.class);
        if (popEffect != null && popEffect.isEnabled() && this.popEffect$cameraRenderState != null) {
            popEffect.getRenderer().submitChams(poseStack, submitNodeCollector, this.popEffect$cameraRenderState);
        }
    }
    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void replaceVanillaBlockOutline(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, CallbackInfo ci) {
        BlockHighlight blockOutline = Client.INSTANCE.moduleManager.getModule(BlockHighlight.class);
        if (blockOutline != null && blockOutline.enables()) {
            ci.cancel();
        }
    }

}