package dev.syntrix.clienttest.client.combat.meow.combat;
import dev.syntrix.clienttest.client.combat.meow.module.MinecraftAccess;
import dev.syntrix.clienttest.client.combat.meow.util.ActionCoordinator;
import dev.syntrix.clienttest.client.combat.meow.event.UpdateEvent;
/** Execute the previous plan before preparing a new one, as in Meow's scheduler. */
public final class CombatCoordinator implements MinecraftAccess {
    private static CrystalAura aura,deferred;
    private static Object player,world,deferredPlayer,deferredWorld;
    private static int tick=Integer.MIN_VALUE;
    private static boolean active;
    public static void initialize(CrystalAura module) { aura=module; }
    public static boolean isActiveTick() { return active&&player==mc.player&&world==mc.level; }
    public static void deferInventoryRestoration(CrystalAura module) {
        deferred=module;deferredPlayer=mc.player;deferredWorld=mc.level;
    }
    public static void onTick() {
        if(active||mc.player==null||mc.level==null)return;
        if(player!=mc.player||world!=mc.level) { player=mc.player;world=mc.level;tick=Integer.MIN_VALUE; }
        if(tick==mc.player.tickCount)return;
        tick=mc.player.tickCount;active=true;
        try {
            if(deferred!=null&&deferred.releaseInventoryOwnership(deferredPlayer==player&&deferredWorld==world))deferred=null;
            ActionCoordinator.INSTANCE.operation031(new UpdateEvent());
            if(aura!=null&&aura.isEnabled()) {
                aura.beginTick();
                var result=CombatScheduler.runTick(null,aura);
                if(aura.isLoggingEnabled())System.out.println("[CrystalAura] tick="+tick+" executed="+result.executed()+" prepared="+result.prepared());
            }
        } finally {
            try { if(aura!=null&&aura.isEnabled())aura.finishTick(); } finally { active=false; }
        }
    }
    private CombatCoordinator() {}
}
