package error.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {

    @Shadow
    @Final
    private HumanoidModel<AvatarRenderState> model;

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V", at = @At("TAIL"))
    private void tintCapeModel(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (state.isInvisible || !state.showCape || state.skin == null || state.skin.cape() == null) {
            return;
        }

        Identifier capeId = state.skin.cape().texturePath();
        if (capeId == null) return;

        int accent = Theme.getAccentColor();
        int capeColor = ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), 255);

        poseStack.pushPose();
        collector.submitModel(
                this.model,
                state,
                poseStack,
                RenderTypes.entitySolid(capeId),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                capeColor,
                null
        );
        poseStack.popPose();
    }
}

