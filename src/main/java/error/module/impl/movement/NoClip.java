package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NoClip extends Module {
    public static NoClip INSTANCE;

    public final ModeSetting mode = mode("Режим", "Error", "Error", "Free / Phase");

    // Error mode settings
    public final SliderSetting clipDistance = slider("Дистанция клипа", 1.0F, 0.2F, 3.0F, 0.1F)
            .visible(() -> mode.is("Error"));
    public final SliderSetting checkDist = slider("Дистанция проверки", 0.15F, 0.05F, 0.6F, 0.05F)
            .visible(() -> mode.is("Error"));
    public final CheckBox flyOnly = checkbox("Только в полёте", true)
            .visible(() -> mode.is("Error"));
    public final CheckBox updateClientPos = checkbox("Обновлять позицию", true)
            .visible(() -> mode.is("Error"));

    // Phase / Free mode settings
    public final SliderSetting phaseSpeed = slider("Скорость прохождения", 0.4F, 0.1F, 2.0F, 0.05F)
            .visible(() -> mode.is("Free / Phase"));

    public final CheckBox noBlockPush = checkbox("Без выталкивания", true);

    public NoClip() {
        super("NoClip", "Клипает и проходит сквозь блоки", Category.MOVEMENT);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || level() == null || !player().isAlive() || player().isPassenger()) return;
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (mode.is("Error")) {
            if (flyOnly.getValue() && !player().getAbilities().flying) return;

            Vec3 lookVec = player().getViewVector(1.0F).normalize();
            AABB testBox = player().getBoundingBox().deflate(0.001).expandTowards(lookVec.scale(checkDist.getValue()));

            if (level().getBlockCollisions(player(), testBox).iterator().hasNext()) {
                Vec3 targetPos = player().position().add(lookVec.scale(clipDistance.getValue()));
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                            targetPos.x, targetPos.y, targetPos.z,
                            false,
                            player().horizontalCollision
                    ));
                }
                if (updateClientPos.getValue()) {
                    player().setPos(targetPos.x, targetPos.y, targetPos.z);
                }
            }
        } else if (mode.is("Free / Phase")) {
            player().noPhysics = true;
            Vec3 motion = player().getDeltaMovement();
            double y = 0.0;
            if (mc.options.keyJump.isDown()) y = phaseSpeed.getValue();
            else if (mc.options.keyShift.isDown()) y = -phaseSpeed.getValue();

            player().setDeltaMovement(motion.x, y, motion.z);
        }
    }

    @Override
    protected void onDisable() {
        if (player() != null) {
            player().noPhysics = false;
        }
        super.onDisable();
    }
}
