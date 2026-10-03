package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.EventTravel;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AirStuck extends Module {
    public static AirStuck INSTANCE;

    public final BindSetting toGroundKey = bind("До земли", GLFW.GLFW_KEY_UNKNOWN);
    public final SliderSetting stopGroundOffset = slider("Отступ от пола", 0.1F, 0.05F, 1.0F, 0.05F);

    private Vec3 freezePos = null;
    private Vec3 freezeVelocity = Vec3.ZERO;
    private float freezeYaw = 0.0F;
    private float freezePitch = 0.0F;
    private boolean isFrozen = false;
    private boolean isFallingToGround = false;

    public AirStuck() {
        super("AirStuck", "Замораживает тебя в воздухе с функцией авто-спуска до пола", Category.PLAYER);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        freezePos = mc.player.position();
        freezeVelocity = mc.player.getDeltaMovement();
        freezeYaw = mc.player.getYRot();
        freezePitch = mc.player.getXRot();
        isFrozen = true;
        isFallingToGround = false;
    }

    @Override
    public void onDisable() {
        if (mc.player != null && freezePos != null) {
            mc.player.noPhysics = false;
        }
        isFrozen = false;
        isFallingToGround = false;
        freezePos = null;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null || event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (isToGroundKeyHeld() && isFrozen && !isFallingToGround) {
            double distToGround = calculateDistanceToGround();
            if (distToGround <= stopGroundOffset.getValue()) {
                // Already at offset, keep frozen
                freezePos = mc.player.position();
            } else {
                // Temporarily unfreeze to fall by physics
                isFrozen = false;
                isFallingToGround = true;
                mc.player.noPhysics = false;
            }
        }

        if (isFallingToGround) {
            double distToGround = calculateDistanceToGround();
            if (distToGround <= stopGroundOffset.getValue() || mc.player.onGround()) {
                // Re-freeze 0.1 block above floor
                isFallingToGround = false;
                isFrozen = true;
                freezePos = mc.player.position();
                freezeVelocity = Vec3.ZERO;
            }
        }

        if (isFrozen && freezePos != null) {
            mc.player.setPos(freezePos.x, freezePos.y, freezePos.z);
            mc.player.setDeltaMovement(Vec3.ZERO);
        }
    }

    @EventTarget
    public void onTravel(EventTravel event) {
        if (isFrozen) {
            event.cancel();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isSend() || !isFrozen) return;

        if (event.is(ServerboundMovePlayerPacket.class)) {
            event.cancel();
        }
    }

    public boolean isToGroundKeyHeld() {
        if (toGroundKey == null || toGroundKey.isEmpty()) return false;
        long window = mc.getWindow().handle();
        for (int code : toGroundKey.getValue()) {
            if (BindSetting.isKeyboard(code) && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS) {
                return true;
            } else if (BindSetting.isMouse(code) && GLFW.glfwGetMouseButton(window, BindSetting.rawButton(code)) == GLFW.GLFW_PRESS) {
                return true;
            }
        }
        return false;
    }

    private double calculateDistanceToGround() {
        if (mc.player == null || mc.level == null) return 8.0D;
        AABB box = mc.player.getBoundingBox();
        double startY = box.minY;
        double endY = startY - 8.0D;
        
        for (double y = startY; y >= endY; y -= 0.05D) {
            AABB checkBox = new AABB(box.minX, y - 0.05D, box.minZ, box.maxX, y, box.maxZ);
            if (!mc.level.noCollision(mc.player, checkBox)) {
                return startY - y;
            }
        }
        return 8.0D;
    }
}