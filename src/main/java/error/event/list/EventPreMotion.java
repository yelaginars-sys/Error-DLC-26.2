package error.event.list;

import lombok.Getter;
import lombok.Setter;
import error.event.Event;

/**
 */
@Getter
@Setter
public class EventPreMotion extends Event {
    private double posX;
    private double posY;
    private double posZ;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean horizontalCollision;

    public EventPreMotion set(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, boolean horizontalCollision) {
        this.setCancelled(false);
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        return this;
    }
}