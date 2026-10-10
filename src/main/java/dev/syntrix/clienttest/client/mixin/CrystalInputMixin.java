package dev.syntrix.clienttest.client.mixin;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardInput.class)
public abstract class CrystalInputMixin extends ClientInput {
    @Inject(method="tick",at=@At("RETURN"))
    private void crystal$input(CallbackInfo ci) {
        if(!CrystalAuraModule.enabled())return;
        moveVector=CrystalAuraModule.correctInput(keyPresses,moveVector);
        keyPresses=new Input(moveVector.y>0,moveVector.y<0,moveVector.x>0,moveVector.x<0,keyPresses.jump(),keyPresses.shift(),keyPresses.sprint());
    }
}
