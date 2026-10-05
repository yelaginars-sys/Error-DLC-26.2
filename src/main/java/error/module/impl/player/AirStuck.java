package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.EventTravel;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.player.InventoryUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AirStuck extends Module {
    public static AirStuck INSTANCE;

    public final ModeSetting mode = mode("Режим", "Обычный", "Обычный", "Lumen", "Debuda");
    public final BindSetting toGroundKey = bind("До земли", GLFW.GLFW_KEY_UNKNOWN);
    public final SliderSetting stopGroundOffset = slider("Отступ от пола", 0.1F, 0.05F, 1.0F, 0.05F);
    public final CheckBox cancelPackets = checkbox("Отменять пакеты", true);
    public final CheckBox swapElytra = checkbox("Свапать элитру", true);

    private Vec3 freezePos = null;
    private Vec3 freezeVelocity = Vec3.ZERO;
    private boolean isFrozen = false;
    private boolean isFallingToGround = false;

    public AirStuck() {
        super("AirStuck", "Замораживает тебя в воздухе (Режимы: Обычный, Lumen, Debuda)", Category.PLAYER);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        
        if (swapElytra.getValue() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            int chestSlot = InventoryUtil.findBestChestplateSlot();
            if (chestSlot != -1) {
                InventoryUtil.swapSlots(chestSlot, 6);
            }
        }

        freezePos = mc.player.position();
        freezeVelocity = mc.player.getDeltaMovement();
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

        if (mode.is("Lumen")) {
            if (!isFrozen && mc.player.fallDistance > 0.0F && mc.player.getDeltaMovement().y < 0.0) {
                freezePos = mc.player.position();
                isFrozen = true;
            }
        } else if (mode.is("Debuda")) {
            if (isToGroundKeyHeld() && !isFallingToGround) {
                isFallingToGround = true;
                isFrozen = false;
            }
        }

        if (isToGroundKeyHeld() && isFrozen && !isFallingToGround) {
            double distToGround = calculateDistanceToGround();
            if (distToGround <= stopGroundOffset.getValue()) {
                freezePos = mc.player.position();
            } else {
                isFrozen = false;
                isFallingToGround = true;
                mc.player.noPhysics = false;
            }
        }

        if (isFallingToGround) {
            double distToGround = calculateDistanceToGround();
            if (distToGround <= stopGroundOffset.getValue() || mc.player.onGround()) {
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

        if (cancelPackets.getValue() && event.is(ServerboundMovePlayerPacket.class)) {
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