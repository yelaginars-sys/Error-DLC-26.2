package error.mixin.client;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import error.util.RotationHandler;

/**
 */
@Mixin(ServerboundUseItemPacket.class)
public abstract class MixinServerboundUseItemPacket {
    @ModifyVariable(method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static float modifyYaw(float yRot) {
        if (RotationHandler.isActive()) {
            return RotationHandler.getServerYaw();
        }
        return yRot;
    }


    @ModifyVariable(method = "<init>(Lnet/minecraft/world/InteractionHand;IFF)V", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static float modifyPitch(float xRot) {
        if (error.module.impl.misc.ServerHelper.isThrowingWindCharge) {
            return 90.0F;
        }
        if (error.module.impl.combat.ExpThrow.isThrowingExp) {
            return 90.0F;
        }
        if (RotationHandler.isActive()) {
            return RotationHandler.getServerPitch();
        }
        return xRot;
    }
}