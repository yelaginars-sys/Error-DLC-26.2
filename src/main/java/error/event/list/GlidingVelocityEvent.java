package error.event.list;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import error.event.Event;

/**
 */
@Getter
@Setter
public class GlidingVelocityEvent extends Event {
    private Vec3 velocity;

    public GlidingVelocityEvent(Vec3 velocity) {
        this.velocity = velocity;
    }

    public GlidingVelocityEvent set(Vec3 velocity) {
        this.velocity = velocity;
        return this;
    }
}