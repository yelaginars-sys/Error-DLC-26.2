package error.event.list;

import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import error.event.Event;

/**
 */
@Getter
public final class PlayerTickEvent extends Event {
    private LocalPlayer player;
    private Phase phase;

    public PlayerTickEvent set(LocalPlayer player, Phase phase) {
        this.player = player;
        this.phase = phase;
        return this;
    }

    public boolean isPre() {
        return this.phase == Phase.PRE;
    }

    public boolean isPost() {
        return this.phase == Phase.POST;
    }

    public enum Phase {
        PRE,
        POST
    }
}