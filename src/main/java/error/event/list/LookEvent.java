package error.event.list;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import error.event.Event;

/**
 */
@Getter
@Setter
@AllArgsConstructor
public class LookEvent extends Event {
    private double cursorDeltaX;
    private double cursorDeltaY;
}