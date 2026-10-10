package dev.syntrix.clienttest.client.mixin;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(MultiPlayerGameMode.class)
public interface CrystalInteractionManagerAccessor {
    @Invoker("ensureHasSentCarriedItem") void invokeSyncSelectedSlot();
    @Accessor("destroyDelay") int getBlockBreakingCooldown();
    @Accessor("destroyDelay") void setBlockBreakingCooldown(int value);
    @Accessor("destroyProgress") float getCurrentBreakingProgress();
}
