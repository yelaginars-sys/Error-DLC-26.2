package error.event.list;

import error.event.Event;
import net.minecraft.core.BlockPos;

/**
 * Event called when a player enters/collides with a cobweb block.
 */
public final class WebCollisionEvent extends Event {
    private final BlockPos pos;

    public WebCollisionEvent(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}
