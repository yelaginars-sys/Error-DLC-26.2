package error.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.Client;
import error.event.EventManager;
import error.event.list.Render3DEvent;
import error.module.impl.render.HandShader;
import error.module.impl.render.Removals;
import error.util.RenderExtend;
import error.util.display.DisplayUtil;
import error.util.render.Render3DUtil;

/**
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static final Render3DEvent RENDER_3D_EVENT = new Render3DEvent();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private GameRenderState gameRenderState;

    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void error$extractDisplay(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        if (minecraft.isGameLoadFinished()) {
            DisplayUtil.render(minecraft, gameRenderState);
        }
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private Matrix4f captureLevelProjection(Matrix4f projectionMatrix) {
        if (error.module.impl.render.AspectRatio.INSTANCE != null && error.module.impl.render.AspectRatio.INSTANCE.isEnabled()) {
            float r = error.module.impl.render.AspectRatio.INSTANCE.getRatio();
            if (r > 0.01F) {
                projectionMatrix.scaleLocal(1.0F / r, 1.0F, 1.0F);
            }
        }
        Render3DUtil.captureLevelProjection(projectionMatrix);
        return projectionMatrix;
    }
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void onRender3D(DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderExtend.enter3D((GameRenderer) (Object) this, deltaTracker);
        try {
            EventManager.call(RENDER_3D_EVENT.set(this.minecraft, (GameRenderer) (Object) this, deltaTracker));
        } finally {
            RenderExtend.exit3D();
        }
    }
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onBobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (Removals.INSTANCE != null && Removals.INSTANCE.isEnabled() && Removals.INSTANCE.getOverlays().isEnabled("HurtCam")) {
            ci.cancel();
        }
    }
    @WrapOperation(
            method = "renderItemInHand",
            at = @org.spongepowered.asm.mixin.injection.At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
            )
    )
    private void renderShaderHands(FeatureRenderDispatcher dispatcher, SubmitNodeStorage storage, Operation<Void> original) {
        HandShader module = Client.INSTANCE.moduleManager.getModule(HandShader.class);
        if (module == null || !module.isEnabled()) {
            original.call(dispatcher, storage);
            return;
        }

        module.getRenderer().render(module, () -> original.call(dispatcher, storage));
    }
}