package error.module.impl.movement;

import error.event.EventTarget;
import error.event.list.EventTravel;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.ModeSetting;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.Vec3;

/**
 * GrimGlide module ported from exclusive.
 * Infinite elytra gliding on GrimAC and SlimeWorld.
 */
public class GrimGlide extends Module {
    public static GrimGlide INSTANCE;

    public final ModeSetting mode = mode("Режим", "Бесконечный RW", "Бесконечный RW", "SlimeWorld");

    private int flagTicks = 0;
    private int ticksTwo = 0;
    private long lastGlideTime = 0L;

    public GrimGlide() {
        super("GrimGlide", "Бесконечный глайд на элитрах для GrimAC и SlimeWorld", Category.MOVEMENT);
        INSTANCE = this;
    }

    public static boolean isGlideFlyActive() {
        if (mc.player != null && mc.player.isFallFlying()) {
            return INSTANCE != null && INSTANCE.isEnabled();
        }
        return false;
    }

    private void blockEating() {
        if (player() == null) return;
        if (player().isUsingItem()) {
            ItemStack stack = player().getUseItem();
            ItemUseAnimation anim = stack.getUseAnimation();
            if (anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK) {
                if (mc.gameMode != null) {
                    mc.gameMode.releaseUsingItem(player());
                }
            }
        }
        mc.options.keyUse.setDown(false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!inGame() || player() == null) return;
        if (mode.is("Бесконечный RW")) {
            if (event.isReceive() && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                flagTicks = 2;
            }
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null) return;

        if (flagTicks > 0) {
            flagTicks--;
        }
        if (isGlideFlyActive()) {
            blockEating();
        }
    }

    @EventTarget
    public void onTravel(EventTravel event) {
        if (!inGame() || player() == null || mc.level == null) return;
        if (!player().isFallFlying()) return;

        if (mode.is("Бесконечный RW")) {
            Vec3 oldVelocity = player().getDeltaMovement();
            Vec3 lookVec = player().getLookAngle();
            float pitchRad = (float) Math.toRadians(player().getXRot());
            double horizontalLook = Math.hypot(lookVec.x, lookVec.z);
            double horizontalSpeed = Math.hypot(oldVelocity.x, oldVelocity.z);

            double gravity = 0.08;
            if (oldVelocity.y <= 0.0 && player().hasEffect(MobEffects.SLOW_FALLING)) {
                gravity = Math.min(0.08, 0.01);
            }

            double pitchFactor = Math.pow(Math.cos(pitchRad), 2);
            oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + pitchFactor * 0.75), 0.0);
            if (oldVelocity.y < 0.0 && horizontalLook > 0.0) {
                double lift = oldVelocity.y * -0.1 * pitchFactor;
                oldVelocity = oldVelocity.add(lookVec.x * lift / horizontalLook, lift, lookVec.z * lift / horizontalLook);
            }

            if (pitchRad < 0.0F && horizontalLook > 0.0) {
                double dive = horizontalSpeed * (-Math.sin(pitchRad)) * 0.04;
                oldVelocity = oldVelocity.add(-lookVec.x * dive / horizontalLook, dive * 3.2, -lookVec.z * dive / horizontalLook);
            }

            if (horizontalLook > 0.0) {
                oldVelocity = oldVelocity.add(
                        (lookVec.x / horizontalLook * horizontalSpeed - oldVelocity.x) * 0.1,
                        0.0,
                        (lookVec.z / horizontalLook * horizontalSpeed - oldVelocity.z) * 0.1
                );
            }

            double yaw = Math.toRadians(player().getYRot());
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            if (flagTicks >= 1) {
                double boost = 0.09;
                Vec3 movement = oldVelocity.multiply(0.99, 0.98, 0.99).add(forwardX * boost, 0.03, forwardZ * boost);
                player().setDeltaMovement(movement);
            } else {
                Vec3 movement = oldVelocity.multiply(0.3, 0.3, 0.3);
                player().setDeltaMovement(movement);
            }
        } else if (mode.is("SlimeWorld")) {
            ticksTwo++;
            Vec3 pos = player().position();
            float yaw = player().getYRot();
            double speedBase = 0.087;

            double dx = -Math.sin(Math.toRadians(yaw)) * speedBase;
            double dz = Math.cos(Math.toRadians(yaw)) * speedBase;
            float randomMultiplier = (float) (1.5 + Math.random() * 0.2);
            player().setDeltaMovement(dx * randomMultiplier, player().getDeltaMovement().y - 0.02, dz * randomMultiplier);

            long now = System.currentTimeMillis();
            if (now - lastGlideTime >= 50L) {
                player().setPos(pos.x + dx, pos.y, pos.z + dz);
                lastGlideTime = now;
            }

            player().setDeltaMovement(dx * randomMultiplier, player().getDeltaMovement().y + 0.016, dz * randomMultiplier);
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        flagTicks = 0;
        ticksTwo = 0;
        lastGlideTime = System.currentTimeMillis();
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        flagTicks = 0;
        ticksTwo = 0;
        blockEating();
    }
}
