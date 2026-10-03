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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.module.impl.player.HoldMyItems;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final
    private Minecraft minecraft;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onHoldMyItems(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
                               float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
                               SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (player.isScoping()) return;
        if (HoldMyItems.INSTANCE == null || !HoldMyItems.INSTANCE.isEnabled()) return;

        ItemInHandRenderer renderer = (ItemInHandRenderer) (Object) this;
        HoldMyItems.INSTANCE.handleRenderItem(renderer, player, frameInterp, xRot, hand, attack, itemStack,
                inverseArmHeight, poseStack, submitNodeCollector, lightCoords);
        ci.cancel();
    }
}