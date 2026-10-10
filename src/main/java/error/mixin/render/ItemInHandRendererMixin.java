package error.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.module.impl.combat.ExpThrow;
import error.module.impl.player.HoldMyItems;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final
    private Minecraft minecraft;

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;

    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack modifyArmItemStack(ItemStack itemStack, AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && ExpThrow.isVisualSwordActive()) {
            ItemStack sword = ExpThrow.getVisualSword();
            if (sword != null && !sword.isEmpty()) {
                return sword;
            }
        }
        return itemStack;
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onHoldMyItems(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                               float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                               SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (player.isScoping()) return;

        ItemStack effectiveStack = (hand == InteractionHand.MAIN_HAND && ExpThrow.isVisualSwordActive())
                ? ExpThrow.getVisualSword()
                : itemStack;

        if (HoldMyItems.INSTANCE != null && HoldMyItems.INSTANCE.isEnabled()) {
            ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
            HoldMyItems.INSTANCE.handleRenderItem(renderer, player, frameInterp, xRot, hand, attack, effectiveStack,
                    inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
            ci.cancel();
            return;
        }

        if (error.module.impl.render.ViewModel.INSTANCE != null && error.module.impl.render.ViewModel.INSTANCE.isEnabled()) {
            var vm = error.module.impl.render.ViewModel.INSTANCE;
            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            net.minecraft.world.entity.HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            if (arm == net.minecraft.world.entity.HumanoidArm.RIGHT) {
                poseStack.translate(vm.rightX.getValue(), vm.rightY.getValue(), vm.rightZ.getValue());
                float s = vm.rightScale.getValue();
                poseStack.scale(s, s, s);
            } else {
                poseStack.translate(vm.leftX.getValue(), vm.leftY.getValue(), vm.leftZ.getValue());
                float s = vm.leftScale.getValue();
                poseStack.scale(s, s, s);
            }
        }
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void onItemUsed(InteractionHand hand, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND && ExpThrow.isVisualSwordActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (ExpThrow.isVisualSwordActive()) {
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
        }
    }
}