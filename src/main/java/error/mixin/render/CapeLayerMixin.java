package error.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
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
    private void renderCapeGlow(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (state.isInvisible || !state.showCape || state.skin == null || state.skin.cape() == null) {
            return;
        }

        Identifier capeId = state.skin.cape().texturePath();
        if (capeId == null) return;

        int accent = Theme.getAccentColor();
        float anim = (float) (Math.sin(System.currentTimeMillis() * 0.0035) * 0.5 + 0.5);
        int alpha = (int) (140 + 80 * anim);
        int glowColor = ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), alpha);

        poseStack.pushPose();

        // 3D glow aura expansion
        poseStack.scale(1.03F, 1.03F, 1.03F);

        // Submit glowing emissive pass (full brightness 15728880)
        collector.submitModel(
                this.model,
                state,
                poseStack,
                RenderTypes.entityTranslucentEmissive(capeId),
                15728880,
                OverlayTexture.NO_OVERLAY,
                glowColor,
                null
        );

        // Submit enchanted glint sheen pass for extra cool visual depth
        RenderType glintType = RenderTypes.glintTranslucent();
        if (glintType != null) {
            int glintAlpha = (int) (70 * anim);
            collector.submitModel(
                    this.model,
                    state,
                    poseStack,
                    glintType,
                    15728880,
                    OverlayTexture.NO_OVERLAY,
                    ColorUtil.rgba(ColorUtil.red(accent), ColorUtil.green(accent), ColorUtil.blue(accent), glintAlpha),
                    null
            );
        }

        poseStack.popPose();
    }
}
