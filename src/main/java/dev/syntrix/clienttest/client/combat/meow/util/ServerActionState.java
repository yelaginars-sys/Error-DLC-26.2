package dev.syntrix.clienttest.client.combat.meow.util;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;
/** Local placement predictions must receive an authoritative block update before reuse. */
public final class ServerActionState implements MinecraftAccess {
    private record Pending(BlockState verified,int tick) {}
    private static final Map<BlockPos,Pending> pending=new HashMap<>();
    public static void reset() { pending.clear(); }
    public static void predicted(BlockPos pos) {
        if(mc.player!=null)pending.put(pos.immutable(),new Pending(null,mc.player.tickCount));
    }
    public static void verified(BlockPos pos,BlockState state) {
        var old=pending.get(pos);
        if(old!=null)pending.put(pos.immutable(),new Pending(state,old.tick));
    }
    public static boolean isSupportUnconfirmed(BlockPos pos) {
        var old=pending.get(pos);
        if(old==null)return false;
        if(mc.level!=null&&old.verified!=null&&mc.level.getBlockState(pos).equals(old.verified)) {
            pending.remove(pos);return false;
        }
        return true;
    }
    private ServerActionState() {}
}
