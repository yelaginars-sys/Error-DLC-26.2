package error.module.impl.movement;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Input;
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;

/**
 * Create by daun kvass
 */
public class NoFall extends Module {

    private int ticksToJump = 0;
    private boolean falling = false;
    private boolean jumping = false;
    private long lastFallBackTime = 0;

    public NoFall() {
        super("NoFall", "Отменяет урон от падения", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
    }

    public void reset() {
        ticksToJump = 0;
        falling = false;
        jumping = false;
        lastFallBackTime = 0;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        falling = player().fallDistance > 3.0F;

        if (ticksToJump > 0) {
            ticksToJump--;
        }

        if (System.currentTimeMillis() - lastFallBackTime > 300) {
            jumping = false;
            ticksToJump = 0;
        }
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (!inGame() || player() == null) return;

        Input current = event.getKeyPresses();
        if (current == null) return;

        if (jumping || ticksToJump == 2) {
            event.setKeyPresses(new Input(
                    current.forward(),
                    current.backward(),
                    current.left(),
                    current.right(),
                    false,
                    current.shift(),
                    current.sprint()
            ));
        } else if (ticksToJump == 1) {
            event.setKeyPresses(new Input(
                    current.forward(),
                    current.backward(),
                    current.left(),
                    current.right(),
                    true,
                    current.shift(),
                    current.sprint()
            ));
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null) return;

        if (event.isSend()) {
            if (event.is(ServerboundMovePlayerPacket.class)) {
                ServerboundMovePlayerPacket movePacket = event.getPacket();

                if (falling) {
                    if (movePacket.isOnGround() && ticksToJump == 0) {
                        sendDirect(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                        lastFallBackTime = System.currentTimeMillis();
                        jumping = true;
                    }
                }
            }
        } else if (event.isReceive()) {
            if (event.is(ClientboundLoginPacket.class) || event.is(ClientboundRespawnPacket.class)) {
                reset();
            }

            if (event.is(ClientboundPlayerPositionPacket.class)) {
                if (jumping) {
                    ticksToJump = 2;
                    jumping = false;
                }
            }
        }
    }

    private void sendDirect(Packet<?> packet) {
        if (mc.getConnection() != null) {
            mc.getConnection().send(packet);
        }
    }
}