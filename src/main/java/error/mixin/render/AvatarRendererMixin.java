package error.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import error.Client;
import error.interfaces.CustomModelCarrier;
import error.module.impl.render.CustomModels;
import error.module.impl.render.NameTags;
import error.util.render.model.ChickenMesh;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void captureCustomModel(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
        CustomModels models = CustomModels.INSTANCE;
        if (state instanceof CustomModelCarrier carrier) {
            String custom = models == null ? null : models.modelFor(avatar);
            carrier.error$setCustomModel(custom);
            if (custom != null && !CustomModels.NONE.equalsIgnoreCase(custom)) {
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;
            }
        }
    }

    @Inject(method = "shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z", at = @At("HEAD"), cancellable = true)
    private void hideLayersOnCustomModel(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
        if (state instanceof CustomModelCarrier carrier && carrier.error$customModel() != null) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }

    @Inject(method = "renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V", at = @At("HEAD"), cancellable = true)
    private void renderCustomRightHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci) {
        CustomModels models = CustomModels.INSTANCE;
        if (models != null && models.handFromModel()) {
            ChickenMesh.submitArm(pose, collector, light, true);
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V", at = @At("HEAD"), cancellable = true)
    private void renderCustomLeftHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci) {
        CustomModels models = CustomModels.INSTANCE;
        if (models != null && models.handFromModel()) {
            ChickenMesh.submitArm(pose, collector, light, false);
            ci.cancel();
        }
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("HEAD"), cancellable = true)
    private void hideVanillaNameTag(Avatar avatar, double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
        NameTags module = Client.INSTANCE.moduleManager.getNameTags();
        if (module != null && module.isState()) {
            cir.setReturnValue(false);
        }
    }
}