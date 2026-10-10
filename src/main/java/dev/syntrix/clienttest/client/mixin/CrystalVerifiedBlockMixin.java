package dev.syntrix.clienttest.client.mixin;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import dev.syntrix.clienttest.client.combat.meow.util.ServerActionState;
import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientLevel.class)
public abstract class CrystalVerifiedBlockMixin {
    @Inject(method="setServerVerifiedBlockState",at=@At("RETURN"))
    private void crystal$verified(BlockPos pos,BlockState state,int flags,CallbackInfo ci) {
        if(CrystalAuraModule.enabled()&&Minecraft.getInstance().level==(Object)this)ServerActionState.verified(pos,state);
    }
}
