package error.event.list;

import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import error.event.Event;

/**
 */
@Getter
public final class ScreenKeyEvent extends Event {
    public enum Action { PRESS, RELEASE }

    private Screen screen;
    private KeyEvent keyEvent;
    private Action action;

    public ScreenKeyEvent set(Screen screen, KeyEvent keyEvent, Action action) {
        this.screen = screen;
        this.keyEvent = keyEvent;
        this.action = action;
        return this;
    }
}