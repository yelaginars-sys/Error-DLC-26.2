package dev.syntrix.clienttest.client.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dev.syntrix.clienttest.client.combat.meow.util.ServerMotionAccess;
import dev.syntrix.clienttest.client.combat.CrystalAuraModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class CrystalServerPacketMixin {
    @Unique private static Entity crystal$entity(int id) { var level=Minecraft.getInstance().level;return level==null?null:level.getEntity(id); }
    @Unique private static void crystal$position(Entity entity,Vec3 pos) {
        if(entity instanceof ServerMotionAccess access)access.meow$setServerPos(pos.x,pos.y,pos.z);
    }
    @Inject(method="handleMoveEntity",at=@At("RETURN"))
    private void crystal$move(ClientboundMoveEntityPacket packet,CallbackInfo ci) {
        var level=Minecraft.getInstance().level;if(level==null)return;
        var entity=packet.getEntity(level);
        if(entity!=null&&packet.hasPosition())crystal$position(entity,entity.getPositionCodec().getBase());
    }
    @Inject(method="handleEntityPositionSync",at=@At("RETURN"))
    private void crystal$sync(ClientboundEntityPositionSyncPacket packet,CallbackInfo ci) {
        crystal$position(crystal$entity(packet.id()),packet.values().position());
    }
    @Inject(method="handleTeleportEntity",at=@At("RETURN"))
    private void crystal$teleport(ClientboundTeleportEntityPacket packet,CallbackInfo ci) {
        var entity=crystal$entity(packet.id());if(entity!=null)crystal$position(entity,entity.getPositionCodec().getBase());
    }
    @Inject(method="handleSetEntityMotion",at=@At("RETURN"))
    private void crystal$motion(ClientboundSetEntityMotionPacket packet,CallbackInfo ci) {
        if(crystal$entity(packet.id()) instanceof ServerMotionAccess access)access.meow$setServerVelocity(packet.movement());
    }
    @Inject(method="handleMovePlayer",at=@At("RETURN"))
    private void crystal$correction(ClientboundPlayerPositionPacket packet,CallbackInfo ci) { CrystalAuraModule.serverCorrection(); }
}
