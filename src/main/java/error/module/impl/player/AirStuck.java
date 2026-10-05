package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.EventTravel;
import error.event.list.KeyboardInputEvent;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.util.player.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class AirStuck extends Module {
    public static AirStuck INSTANCE;

    public final ModeSetting mode = mode("Режим", "RW 1.21", "RW 1.21", "RW 1.16");
    public final CheckBox swapChestplate = checkbox("Свапнуть нагрудник", true);
    public final BindSetting toGround = bind("До земли", GLFW.GLFW_KEY_UNKNOWN);

    private boolean isFrozen = false;
    private boolean isFallingToGround = false;
    private boolean swappedArmor = false;
    private Vec3 freezePos = Vec3.ZERO;

    public AirStuck() {
        super("AirStuck", "Зависает в воздухе с обходом античитов", Category.PLAYER);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        isFrozen = true;
        isFallingToGround = false;
        swappedArmor = false;
        if (player() != null) {
            freezePos = player().position();
            checkChestplateSwap();
        }
    }

    @Override
    protected void onDisable() {
        isFrozen = false;
        isFallingToGround = false;
        swappedArmor = false;
        freezePos = Vec3.ZERO;
        super.onDisable();
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && toGround.matches(event.getKey())) {
            isFallingToGround = true;
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;

        checkChestplateSwap();

        if (isFallingToGround) {
            // Check if player is near ground (within 0.3 blocks)
            BlockPos below = BlockPos.containing(player().getX(), player().getY() - 0.3, player().getZ());
            if (!mc.level.getBlockState(below).isAir() || player().onGround()) {
                isFallingToGround = false;
                freezePos = player().position();
            }
        }
    }

    @EventTarget
    public void onTravel(EventTravel event) {
        if (!inGame() || player() == null || isFallingToGround) return;

        player().setDeltaMovement(Vec3.ZERO);
        event.setCancelled(true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null || isFallingToGround) return;

        if (event.isSend() && event.is(ServerboundMovePlayerPacket.class)) {
            if (mode.is("RW 1.16")) {
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                            player().getX(), player().getY(), player().getZ(),
                            player().getYRot(), player().getXRot(),
                            false, false
                    ));
                }
                event.setCancelled(true);
            } else {
                // RW 1.21 mode
                event.setCancelled(true);
            }
        }
    }

    private void checkChestplateSwap() {
        if (swappedArmor || !swapChestplate.getValue() || player() == null) return;

        if (!player().getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            swappedArmor = true;
            return;
        }

        int bestChestSlot = InventoryUtil.findBestChestplateSlot();
        if (bestChestSlot != -1) {
            if (bestChestSlot < 9) {
                InventoryUtil.swapSlots(6, bestChestSlot);
            } else {
                InventoryUtil.swapSlots(bestChestSlot, 6);
            }
            swappedArmor = true;
        }
    }

    public boolean isFrozen() {
        return isEnabled() && isFrozen;
    }

    public static boolean isSneakSuppressed() {
        return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.isFrozen;
    }
}