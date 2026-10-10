package dev.syntrix.clienttest.client.mixin;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import io.netty.channel.ChannelFutureListener;
import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Connection.class)
public abstract class CrystalConnectionMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",at=@At("RETURN"))
    private void crystal$sent(Packet<?> packet,ChannelFutureListener listener,boolean flush,CallbackInfo ci) {
        CrystalAuraModule.sent((Connection)(Object)this,packet);
    }
}
