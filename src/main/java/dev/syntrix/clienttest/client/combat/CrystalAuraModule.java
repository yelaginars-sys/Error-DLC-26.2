package dev.syntrix.clienttest.client.combat;
import com.google.common.eventbus.EventBus;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.*;
import java.util.*;
import dev.syntrix.clienttest.client.gui.*;
import dev.syntrix.clienttest.client.visual.VisualWorldRenderer;
import dev.syntrix.clienttest.client.combat.meow.combat.*;
import dev.syntrix.clienttest.client.combat.meow.event.*;
import dev.syntrix.clienttest.client.combat.meow.util.*;
/** Runtime bridge for the recovered Meow CrystalAura algorithm. */
public final class CrystalAuraModule {
    private static CrystalAura aura;
    private static ClickGuiState.Module state;
    private static final EventBus events=new EventBus((error,context)-> { throw new IllegalStateException("CrystalAura event failed",error); });
    private static Object player,world;
    private static boolean wasEnabled,rotationKnown,positionKnown;
    private static Vec3 sentPosition;
    private static float sentYaw,sentPitch;
    public static void initialize() {
        state=ClickGuiController.STATE.modules.stream().filter(m->m.id.equals(CrystalAuraSettings.ID)).findFirst().orElseThrow();
        aura=new CrystalAura();aura.bind(state);events.register(aura);
        CombatCoordinator.initialize(aura);
        ClientTickEvents.START_CLIENT_TICK.register(client->refresh());
    }
    private static void refresh() {
        if(aura==null)return;
        var mc=Minecraft.getInstance();
        boolean changed=player!=mc.player||world!=mc.level;
        if(changed||wasEnabled&&!state.enabled) {
            if(changed)aura.resetForWorldChange();else aura.onDisable();
            ActionCoordinator.INSTANCE.operation041();
            ServerActionState.reset();PlacementCooldowns.reset();ManualActionState.reset();
            sentPosition=null;rotationKnown=positionKnown=false;
        }
        player=mc.player;world=mc.level;wasEnabled=state.enabled;
        CrystalAuraSettings.sync(aura,state);
    }
    public static boolean enabled() { return aura!=null&&state.enabled; }
    public static void tick() { if(aura!=null)CombatCoordinator.onTick(); }
    public static void sent(Connection connection,Packet<?> packet) {
        if(aura==null)return;
        var mc=Minecraft.getInstance();
        if(!mc.isSameThread()||mc.player==null||mc.getConnection()==null||mc.getConnection().getConnection()!=connection||!connection.isConnected())return;
        if(packet instanceof ServerboundMovePlayerPacket move) {
            if(move.hasPosition()) { sentPosition=new Vec3(move.getX(0),move.getY(0),move.getZ(0));positionKnown=true; }
            if(move.hasRotation()) { sentYaw=move.getYRot(0);sentPitch=move.getXRot(0);rotationKnown=true; }
        } else if(enabled()) {
            if(packet instanceof ServerboundUseItemOnPacket||packet instanceof ServerboundUseItemPacket)ManualActionState.markUse();
            else if(packet instanceof ServerboundInteractPacket)ManualActionState.markEntityInteraction();
            else if(packet instanceof ServerboundPlayerActionPacket action&&(
                action.getAction()==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK||
                action.getAction()==ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK))ManualActionState.markDig();
        }
    }
    public static void serverCorrection() {
        rotationKnown=positionKnown=false;sentPosition=null;
        if(aura!=null)aura.getMovementReceipt().reset();
    }
    public static void movementComplete() {
        var mc=Minecraft.getInstance();
        if(enabled()&&mc.player!=null) {
            boolean synchronizedPosition=positionKnown&&sentPosition!=null&&sentPosition.distanceToSqr(mc.player.position())<=4e-8;
            boolean connected=mc.getConnection()!=null&&mc.getConnection().getConnection().isConnected();
            events.post(new MovementReceiptEvent(sentYaw,sentPitch,rotationKnown,synchronizedPosition,false,false,connected));
        }
        ManualActionState.reset();
    }
    public static Vec2 correctInput(net.minecraft.world.entity.player.Input input,Vec2 vector) {
        if(!enabled())return vector;
        var event=new MovementInputEvent(input,vector.y,vector.x,input.jump(),input.shift());
        events.post(event);return new Vec2(event.getState003(),event.getState002());
    }
    public static void appendGeometry(List<VisualWorldRenderer.Line> lines,List<VisualWorldRenderer.Box> boxes) {
        if(!enabled()||Minecraft.getInstance().level==null)return;
        if(!aura.getOptions().isEnabled("crystalAuraRender")) { aura.getRenderMarkers().clear();return; }
        long now=System.currentTimeMillis();
        var iterator=aura.getRenderMarkers().iterator();
        while(iterator.hasNext()) {
            var marker=iterator.next();float age=(now-marker.time())/300F;
            if(age>=1) { iterator.remove();continue; }
            addBox(lines,boxes,marker.pos(),.4F+.6F*age,(int)((1-age)*200),marker.crystal()?0xB446FF:0x2D7DFF);
        }
        if(aura.getRenderPosition()!=null) {
            long age=now-aura.getRenderTime();float fade=age<=140?1:Math.max(0,1-(age-140)/160F);
            if(fade>0)addBox(lines,boxes,aura.getRenderPosition(),.55F+.45F*aura.getRenderProgress(),
                (int)((.45F+.4F*aura.getRenderProgress())*fade*255),0xFF3737);
        }
    }
    private static void addBox(List<VisualWorldRenderer.Line> lines,List<VisualWorldRenderer.Box> boxes,net.minecraft.core.BlockPos pos,float scale,int alpha,int rgb) {
        Vec3 center=Vec3.atCenterOf(pos);double half=.5*Math.clamp(scale,.05F,1F);
        AABB b=new AABB(center.subtract(half,half,half),center.add(half,half,half));
        boxes.add(new VisualWorldRenderer.Box(b,Math.max(16,alpha/2)<<24|rgb,true));
        Vec3[] points={new Vec3(b.minX,b.minY,b.minZ),new Vec3(b.maxX,b.minY,b.minZ),new Vec3(b.maxX,b.minY,b.maxZ),new Vec3(b.minX,b.minY,b.maxZ),new Vec3(b.minX,b.maxY,b.minZ),new Vec3(b.maxX,b.maxY,b.minZ),new Vec3(b.maxX,b.maxY,b.maxZ),new Vec3(b.minX,b.maxY,b.maxZ)};
        for(int i=0;i<4;i++) {
            lines.add(new VisualWorldRenderer.Line(points[i],points[(i+1)%4],alpha<<24|rgb,1.5F));
            lines.add(new VisualWorldRenderer.Line(points[i+4],points[(i+1)%4+4],alpha<<24|rgb,1.5F));
            lines.add(new VisualWorldRenderer.Line(points[i],points[i+4],alpha<<24|rgb,1.5F));
        }
    }
    private CrystalAuraModule() {}
}
