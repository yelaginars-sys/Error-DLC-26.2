package dev.syntrix.clienttest.client.mixin;
import net.minecraft.client.player.LocalPlayer;
import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import dev.syntrix.clienttest.client.combat.meow.util.ServerRotationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class CrystalPlayerTickMixin {
    @Inject(method="aiStep",at=@At("HEAD"))
    private void crystal$tick(CallbackInfo ci) { CrystalAuraModule.tick(); }
    // Apply the same aim to packet construction, change detection and the last-sent angles.
    // Other reads (rendering, movement, mouse input and the crosshair) keep the local view.
    @Redirect(method={"sendPosition","tick"},at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float crystal$packetYaw(LocalPlayer player) { return ServerRotationState.serverYaw(); }
    @Redirect(method={"sendPosition","tick"},at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float crystal$packetPitch(LocalPlayer player) { return ServerRotationState.serverPitch(); }
    @Inject(method="sendPosition",at=@At("RETURN"))
    private void crystal$movementReceipt(CallbackInfo ci) { CrystalAuraModule.movementComplete(); }
    @Inject(method="tick",at=@At("RETURN"))
    private void crystal$ridingReceipt(CallbackInfo ci) {
        if(((LocalPlayer)(Object)this).isPassenger())CrystalAuraModule.movementComplete();
    }
}
