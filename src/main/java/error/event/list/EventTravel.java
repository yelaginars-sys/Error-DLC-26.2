package error.event.list;

import error.event.Event;

/**
 */
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;

@Getter
@Setter
public class EventTravel extends Event {
    private Vec3 movement = Vec3.ZERO;

    public EventTravel() {
    }

    public EventTravel(Vec3 movement) {
        this.movement = movement;
    }
}