package dev.syntrix.clienttest.client.mixin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dev.syntrix.clienttest.client.combat.meow.util.ServerMotionAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(AbstractClientPlayer.class)
public abstract class CrystalServerMotionMixin implements ServerMotionAccess {
    @Unique private Vec3 crystal$current,crystal$previous,crystal$velocity;
    @Unique private Vec3 crystal$current() { return crystal$current==null?((Entity)(Object)this).position():crystal$current; }
    @Unique private Vec3 crystal$previous() { return crystal$previous==null?crystal$current():crystal$previous; }
    public double meow$getPrevServerX() { return crystal$previous().x; }
    public double meow$getPrevServerY() { return crystal$previous().y; }
    public double meow$getPrevServerZ() { return crystal$previous().z; }
    public double meow$getServerX() { return crystal$current().x; }
    public double meow$getServerY() { return crystal$current().y; }
    public double meow$getServerZ() { return crystal$current().z; }
    public Vec3 meow$getServerVelocity() { return crystal$velocity; }
    public void meow$setServerVelocity(Vec3 value) { crystal$velocity=value; }
    public void meow$setServerPos(double x,double y,double z) {
        var value=new Vec3(x,y,z);crystal$previous=crystal$current==null?value:crystal$current;crystal$current=value;
    }
}
