package error.module.impl.misc;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import error.Client;
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;

import java.util.Locale;

/**
 */
public class AutoAccept extends Module {
    public AutoAccept(){super("AutoAccept","принимать", Category.MISC);}
    CheckBox onlyfriends = checkbox("Только от друзей",true);
    @EventTarget
    public void onEvent(final PacketEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (event.getType() != PacketEvent.Type.RECEIVE) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof ClientboundSystemChatPacket messagePacket) {
            String raw = messagePacket.content().getString().toLowerCase(Locale.ROOT);

            if (raw.contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться")) {
                if (onlyfriends.getValue()) {
                    boolean isFriend = false;

                    if (Client.INSTANCE.friendManager != null) {
                        for (String friend : Client.INSTANCE.friendManager.getFriends()) {
                            if (raw.contains(friend.toLowerCase(Locale.ROOT))) {
                                isFriend = true;
                                break;
                            }
                        }
                    }

                    if (!isFriend) {
                        return;
                    }
                }

                mc.player.connection.sendCommand("tpaccept");
            }
        }
    }
}
