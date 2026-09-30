package error.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.util.RotationHandler;
import error.module.impl.render.SwingAnimation;
import error.module.impl.render.ViewModel;

/**
 * Create by daun kvass
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final
    private Minecraft minecraft;

    @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot(F)F"))
    private float useFreeLookPitch(LocalPlayer player, float partialTick) {
        if (RotationHandler.isActive()) {
            return RotationHandler.getFreePitch();
        }
        return player.getXRot(partialTick);
    }

    @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"))
    private float useCameraPitch(LocalPlayer player, float partialTick) {
        return this.minecraft.gameRenderer.mainCamera().xRot();
    }

    @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"))
    private float useCameraYaw(LocalPlayer player, float partialTick) {
        return this.minecraft.gameRenderer.mainCamera().yRot();
    }

    @ModifyVariable(method = "applyItemArmTransform", at = @At("HEAD"), argsOnly = true)
    private float fixReequipDrop(float inverseArmHeight) {
        if (SwingAnimation.INSTANCE != null && SwingAnimation.INSTANCE.isEnabled() && SwingAnimation.INSTANCE.noReequip.getValue()) {
            return 0.0F;
        }
        return inverseArmHeight;
    }

    @Inject(method = "applyEatTransform", at = @At("HEAD"), cancellable = true)
    private void cancelEatTransform(PoseStack poseStack, float frameInterp, HumanoidArm arm, ItemStack itemStack, Player player, CallbackInfo ci) {
        if (SwingAnimation.INSTANCE != null && SwingAnimation.INSTANCE.isEnabled() && SwingAnimation.INSTANCE.noEat.getValue()) {
            ci.cancel();

            float remaining = (float) player.getUseItemRemainingTicks() - frameInterp + 1.0F;
            float eatProgress = remaining / (float) itemStack.getUseDuration(player);
            float f3 = 1.0F - (float) Math.pow((double) eatProgress, 27.0D);
            int i = arm == HumanoidArm.RIGHT ? 1 : -1;

            poseStack.translate(f3 * 0.6F * (float) i, f3 * -0.5F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) i * f3 * 90.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(f3 * 10.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) i * f3 * 30.0F));
        }
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void applyViewModel(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (ViewModel.INSTANCE != null && ViewModel.INSTANCE.isEnabled()) {
            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            if (arm == HumanoidArm.RIGHT) {
                poseStack.translate(ViewModel.INSTANCE.rightX.getValue(), ViewModel.INSTANCE.rightY.getValue(), ViewModel.INSTANCE.rightZ.getValue());
                float scale = ViewModel.INSTANCE.rightScale.getValue();
                poseStack.scale(scale, scale, scale);
            } else {
                poseStack.translate(ViewModel.INSTANCE.leftX.getValue(), ViewModel.INSTANCE.leftY.getValue(), ViewModel.INSTANCE.leftZ.getValue());
                float scale = ViewModel.INSTANCE.leftScale.getValue();
                poseStack.scale(scale, scale, scale);
            }
        }
    }

    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void onSwingArm(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        if (SwingAnimation.INSTANCE != null && SwingAnimation.INSTANCE.isEnabled()) {
            if (arm == HumanoidArm.LEFT && !SwingAnimation.INSTANCE.offhand.getValue()) {
                return;
            }
            ci.cancel();
            SwingAnimation.INSTANCE.applySwing(attack, poseStack, invert, arm);
        }
    }
}