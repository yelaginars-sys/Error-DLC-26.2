package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.EventTravel;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
    public static Speed INSTANCE;

    public final ModeSetting mode = mode("Мод", "Grim New", "Grim New", "Коллизия", "Grim LowHop", "Grim Hop");
    public final SliderSetting strength = slider("Сила", 0.03F, 0.01F, 0.1F, 0.001F);
    public final SliderSetting radius = slider("Радиус", 0.35F, 0.1F, 1.0F, 0.05F);
    public final SliderSetting predictTicks = slider("Кол-во тиков для предикта", 5.0F, 0.0F, 10.0F, 1.0F);
    public final CheckBox autoJump = checkbox("Авто прыжок", true);

    private int tickCounter = 0;
    private int lowHopStage = 0;
    private int lagbackCooldown = 0;

    public Speed() {
        super("Speed", "Увеличение скорости передвижения под различные проверки", Category.MOVEMENT);
        INSTANCE = this;

        strength.visible(() -> mode.is("Коллизия"));
        radius.visible(() -> mode.is("Коллизия"));
        predictTicks.visible(() -> mode.is("Коллизия"));
        autoJump.visible(() -> mode.is("Grim New") || mode.is("Grim Hop"));
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
        lowHopStage = 0;
        lagbackCooldown = 0;
    }

    @Override
    protected void onDisable() {
        tickCounter = 0;
        lowHopStage = 0;
        lagbackCooldown = 0;
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;

        if (lagbackCooldown > 0) {
            lagbackCooldown--;
        }

        if (!isMoving()) {
            tickCounter = 0;
            return;
        }

        if (mode.is("Grim New")) {
            if (autoJump.getValue() && player().onGround()) {
                player().jumpFromGround();
            }

            double moveYaw = Math.toRadians(getMoveYaw());
            double dirX = -Math.sin(moveYaw);
            double dirZ = Math.cos(moveYaw);

            double bonus = player().onGround() ? 0.0855D : 0.03D;

            if (tickCounter > 3) {
                if (tickCounter % 2 == 0 && !player().isInWater()) {
                    player().setDeltaMovement(player().getDeltaMovement().add(0.0D, 0.03D, 0.0D));
                }
                player().setDeltaMovement(player().getDeltaMovement().add(dirX * bonus, 0.0D, dirZ * bonus));
            }
            tickCounter++;
        } else if (mode.is("Grim Hop")) {
            if (autoJump.getValue() && player().onGround()) {
                player().jumpFromGround();
            }

            double moveYaw = Math.toRadians(getMoveYaw());
            double dirX = -Math.sin(moveYaw);
            double dirZ = Math.cos(moveYaw);

            if (player().onGround()) {
                player().setDeltaMovement(player().getDeltaMovement().add(dirX * 0.05D, 0.0D, dirZ * 0.05D));
            }
        } else if (mode.is("Grim LowHop")) {
            if (player().verticalCollision && player().onGround() && tickCounter > 2) {
                lowHopStage++;
                tickCounter = 0;
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundPlayerCommandPacket(player(), ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    mc.getConnection().send(new ServerboundPlayerCommandPacket(player(), ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
            }

            double moveYaw = Math.toRadians(getMoveYaw());
            double dirX = -Math.sin(moveYaw);
            double dirZ = Math.cos(moveYaw);
            double bonus = 0.03D;

            if (tickCounter == 1 && lowHopStage > 0) {
                player().jumpFromGround();
                player().setDeltaMovement(new Vec3(dirX * bonus, -0.03D, dirZ * bonus));
            }

            if (tickCounter == 2 && lowHopStage > 0) {
                player().setDeltaMovement(new Vec3(dirX * bonus, -0.0199D, dirZ * bonus));
            }

            tickCounter++;
        } else if (mode.is("Коллизия")) {
            handleCollisionBoost();
        }
    }

    private void handleCollisionBoost() {
        float r = radius.getValue();
        AABB box = player().getBoundingBox().inflate(r);

        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player() && e.isAlive())) {
            double moveYaw = Math.toRadians(getMoveYaw());
            double dirX = -Math.sin(moveYaw);
            double dirZ = Math.cos(moveYaw);
            double str = strength.getValue();

            player().setDeltaMovement(player().getDeltaMovement().add(dirX * str, 0.0D, dirZ * str));
            break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null) return;

        if (event.isReceive() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            lagbackCooldown = 2;
            tickCounter = 0;
        }
    }

    private boolean isMoving() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    private float getMoveYaw() {
        float yaw = player().getYRot();
        float forward = 0.0F;
        float strafe = 0.0F;
        if (mc.options.keyUp.isDown()) forward += 1.0F;
        if (mc.options.keyDown.isDown()) forward -= 1.0F;
        if (mc.options.keyLeft.isDown()) strafe += 1.0F;
        if (mc.options.keyRight.isDown()) strafe -= 1.0F;

        if (forward < 0.0F) yaw += 180.0F;
        float f = 1.0F;
        if (forward < 0.0F) f = -0.5F;
        else if (forward > 0.0F) f = 0.5F;

        if (strafe > 0.0F) yaw -= 90.0F * f;
        if (strafe < 0.0F) yaw += 90.0F * f;

        return yaw;
    }
}
