package error.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import error.module.impl.render.Removals;

/**
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Unique
    private static final Identifier ERROR_CAPE_ID = Identifier.fromNamespaceAndPath("error", "images/cape/bkgroup.png");

    @Unique
    private static final ClientAsset.ResourceTexture ERROR_CAPE_TEXTURE = new ClientAsset.ResourceTexture(ERROR_CAPE_ID, ERROR_CAPE_ID);

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void onGetSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (player.isLocalPlayer()) {
            error.cosmetic.CosmeticItem capeItem = error.cosmetic.CosmeticsManager.getInstance().getEquipped(error.cosmetic.CosmeticType.CAPE);
            if (capeItem != null && capeItem.isEnabled()) {
                PlayerSkin skin = cir.getReturnValue();
                if (skin != null) {
                    Identifier capeTex = Identifier.fromNamespaceAndPath("error", "textures/cosmetics/cape.png");
                    ClientAsset.ResourceTexture resourceTex = new ClientAsset.ResourceTexture(capeTex, capeTex);
                    PlayerSkin customSkin = new PlayerSkin(skin.body(), resourceTex, skin.elytra() != null ? skin.elytra() : resourceTex, skin.model(), skin.secure());
                    cir.setReturnValue(customSkin);
                }
            }
        }
    }
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void onGetFieldOfViewModifier(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        if (Removals.INSTANCE != null && Removals.INSTANCE.isBadEffectsDisabled()) {
            AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
            if (player.hasEffect(MobEffects.SLOWNESS)) {
                if (cir.getReturnValueF() < 1.0F) {
                    cir.setReturnValue(1.0F);
                }
            }
        }
    }
}