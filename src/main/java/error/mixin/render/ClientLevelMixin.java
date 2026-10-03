package error.mixin.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import error.module.impl.render.Removals;

/**
 */
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "tickWeatherEffects", at = @At("HEAD"), cancellable = true)
    private void onTickWeatherEffects(CallbackInfo ci) {
        if (Removals.INSTANCE != null && Removals.INSTANCE.isWeatherDisabled()) {
            ci.cancel();
        }
    }
    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddDestroyBlockEffect(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        if (Removals.INSTANCE != null && Removals.INSTANCE.isBlockBreakParticlesDisabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddBreakingBlockEffect(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (Removals.INSTANCE != null && Removals.INSTANCE.isBlockBreakParticlesDisabled()) {
            ci.cancel();
        }
    }
}