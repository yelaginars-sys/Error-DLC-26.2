package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.WebCollisionEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NoWeb extends Module {
    public static NoWeb INSTANCE;

    public final ModeSetting mode = mode("Режим", "Полноценный", "Полноценный", "Движение", "Vanilla");

    // Settings for "Движение" mode
    public final SliderSetting upSpeed = slider("Скорость вверх", 0.8F, 0.1F, 2.0F, 0.05F)
            .visible(() -> mode.is("Движение"));
    public final SliderSetting downSpeed = slider("Скорость вниз", 0.8F, 0.1F, 2.0F, 0.05F)
            .visible(() -> mode.is("Движение"));
    public final SliderSetting horizontalSpeed = slider("Скорость по горизонту", 0.22F, 0.05F, 1.5F, 0.01F)
            .visible(() -> mode.is("Движение"));

    public NoWeb() {
        super("NoWeb", "Убирает или обходит замедление в паутине", Category.MOVEMENT);
        INSTANCE = this;
    }

    @EventTarget
    public void onWebCollision(WebCollisionEvent event) {
        if (!inGame() || player() == null) return;

        if (mode.is("Полноценный")) {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        event.getPos(),
                        Direction.UP
                ));
            }
            event.cancel();
        } else if (mode.is("Vanilla")) {
            event.cancel();
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || level() == null) return;
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;
        if (!mode.is("Движение")) return;

        if (!isPlayerInWeb()) return;

        Vec3 motion = player().getDeltaMovement();
        double yMotion = motion.y;
        if (mc.options.keyJump.isDown()) {
            yMotion = upSpeed.getValue();
        } else if (mc.options.keyShift.isDown()) {
            yMotion = -downSpeed.getValue();
        }

        float yaw = (float) Math.toRadians(player().getYRot());
        float forward = player().input.getMoveVector().y;
        float strafe = player().input.getMoveVector().x;

        double xMotion = motion.x;
        double zMotion = motion.z;

        if (forward != 0 || strafe != 0) {
            float speed = horizontalSpeed.getValue();
            double sin = Math.sin(yaw);
            double cos = Math.cos(yaw);
            xMotion = (forward * -sin + strafe * cos) * speed;
            zMotion = (forward * cos + strafe * sin) * speed;
        }

        player().setDeltaMovement(new Vec3(xMotion, yMotion, zMotion));
    }

    private boolean isPlayerInWeb() {
        if (player() == null || level() == null) return false;
        AABB bb = player().getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(
                Mth.floor(bb.minX), Mth.floor(bb.minY), Mth.floor(bb.minZ),
                Mth.floor(bb.maxX), Mth.floor(bb.maxY), Mth.floor(bb.maxZ)
        )) {
            if (level().getBlockState(pos).is(Blocks.COBWEB)) {
                return true;
            }
        }
        return false;
    }
}
