package error.event.list;

import lombok.Getter;
import error.event.Event;

/**
 * Create by daun kvass
 */
@Getter
public final class KeyboardInputEvent extends Event {
    private long window;
    private int key;
    private int scanCode;
    private int action;
    private int modifiers;

    public KeyboardInputEvent set(long window, int key, int scanCode, int action, int modifiers) {
        this.setCancelled(false);
        this.window = window;
        this.key = key;
        this.scanCode = scanCode;
        this.action = action;
        this.modifiers = modifiers;
        return this;
    }
}