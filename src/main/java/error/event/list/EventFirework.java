package error.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.LivingEntity;
import error.event.Event;

/**
 */
@Getter
@Setter
@AllArgsConstructor
public class EventFirework extends Event {
    private final LivingEntity boostedEntity;
    private float speed;
}