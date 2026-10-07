package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.combat.MaceHelper;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public class WindHop extends Module {
    public static WindHop INSTANCE;

    public final CheckBox packetSync = checkbox("Синхронизация с взрывом", true);
    public final SliderSetting delayTicks = slider("Задержка (тики)", 2.0F, 0.0F, 5.0F, 1.0F);

    private boolean windChargePending = false;
    private int windChargePendingTicks = 0;
    private long lastLaunchTime = 0L;

    private int scheduledJumpTicks = 0;
    private int windJumpTicks = 0;
    private boolean windFlightActive = false;
    private boolean windFlightWasAirborne = false;

    public WindHop() {
        super("WindHop", "Автоматически подлетает на стабильно максимальную высоту от заряда ветра", Category.PLAYER);
        INSTANCE = this;
    }

    public static WindHop getInstance() {
        return INSTANCE;
    }

    public void onWindChargeLaunch() {
        long now = System.currentTimeMillis();
        if (now - lastLaunchTime < 150L) {
            return;
        }
        lastLaunchTime = now;
        this.windChargePending = true;
        this.windChargePendingTicks = 15; // Таймаут ~750мс на получение пакета от сервера

        int delay = Math.round(delayTicks.getValue());
        if (delay <= 0) {
            performWindJump();
            this.windChargePending = false;
        } else {
            this.scheduledJumpTicks = delay;
        }

        if (MaceHelper.INSTANCE != null && MaceHelper.INSTANCE.isEnabled() && mc.player != null) {
            MaceHelper.INSTANCE.onWindChargeUsed(mc.player.getInventory().getSelectedSlot());
        }
    }

    @EventTarget
    public void onPlayerInput(PlayerInputEvent event) {
        if (this.windJumpTicks > 0 && mc.player != null && event.getKeyPresses() != null) {
            Input cur = event.getKeyPresses();
            event.setKeyPresses(new Input(cur.forward(), cur.backward(), cur.left(), cur.right(), true, cur.shift(), cur.sprint()));
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (mc.player == null || mc.level == null) {
            resetWindFlight();
            return;
        }

        if (this.windChargePendingTicks > 0) {
            this.windChargePendingTicks--;
            if (this.windChargePendingTicks == 0) {
                this.windChargePending = false;
            }
        }

        if (this.scheduledJumpTicks > 0) {
            this.scheduledJumpTicks--;
            if (this.scheduledJumpTicks == 0) {
                performWindJump();
                this.windChargePending = false;
            }
        }

        if (this.windJumpTicks > 0) {
            this.windJumpTicks--;
            if (this.windJumpTicks == 0) {
                if (mc.options != null && mc.options.keyJump != null) {
                    if (!error.util.client.KeyCheck.isPressed(mc.options.keyJump)) {
                        mc.options.keyJump.setDown(false);
                    }
                }
            }
        }

        if (this.windFlightActive) {
            if (mc.player.onGround()) {
                if (this.windFlightWasAirborne) {
                    this.windFlightActive = false;
                    this.windFlightWasAirborne = false;
                }
            } else {
                this.windFlightWasAirborne = true;
            }
        }
    }

    public void performWindJump() {
        if (mc.player == null || mc.options == null) return;

        double currentY = mc.player.getDeltaMovement().y;

        if (mc.player.onGround()) {
            mc.player.jumpFromGround();
            if (currentY > 0.1) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, currentY + 0.42, mc.player.getDeltaMovement().z);
            }
        } else {
            if (currentY > 0.1 && currentY < 3.0) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, currentY + 0.42, mc.player.getDeltaMovement().z);
            }
        }

        if (mc.options.keyJump != null) {
            mc.options.keyJump.setDown(true);
        }
        this.windJumpTicks = 5;
        this.windFlightActive = true;
        this.windFlightWasAirborne = false;

        Input currentInput = mc.player.input != null && mc.player.input.keyPresses != null
                ? mc.player.input.keyPresses
                : new Input(false, false, false, false, false, false, false);

        Input jumpInput = new Input(
                currentInput.forward(),
                currentInput.backward(),
                currentInput.left(),
                currentInput.right(),
                true,
                currentInput.shift(),
                currentInput.sprint()
        );

        if (mc.player.input != null) {
            mc.player.input.keyPresses = jumpInput;
        }
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerInputPacket(jumpInput));
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.player == null) return;

        if (event.isSend()) {
            if (event.getPacket() instanceof ServerboundUseItemPacket packet) {
                ItemStack stackInHand = mc.player.getItemInHand(packet.getHand());
                if (stackInHand.is(Items.WIND_CHARGE)) {
                    float pitch = 90.0F;

                    event.setPacket(new ServerboundUseItemPacket(
                            packet.getHand(),
                            packet.getSequence(),
                            mc.player.getYRot(),
                            pitch
                    ));

                    onWindChargeLaunch();
                }
            }
        } else {
            if (packetSync.getValue() && this.windChargePending) {
                if (event.getPacket() instanceof ClientboundExplodePacket explosionPacket) {
                    Vec3 center = explosionPacket.center();
                    double distSq = mc.player.distanceToSqr(center.x, center.y, center.z);
                    if (distSq < 25.0) {
                        performWindJump();
                        this.windChargePending = false;
                        this.scheduledJumpTicks = 0;
                    }
                } else if (event.getPacket() instanceof ClientboundSetEntityMotionPacket velocityPacket) {
                    if (velocityPacket.id() == mc.player.getId()) {
                        Vec3 vel = velocityPacket.movement();
                        if (vel.y > 0.15) {
                            performWindJump();
                            this.windChargePending = false;
                            this.scheduledJumpTicks = 0;
                        }
                    }
                }
            }
        }
    }

    public boolean isWindFallActive() {
        return this.windFlightActive && mc.player != null && !mc.player.onGround();
    }

    private void resetWindFlight() {
        this.scheduledJumpTicks = 0;
        this.windJumpTicks = 0;
        this.windFlightActive = false;
        this.windFlightWasAirborne = false;
        this.windChargePending = false;
        this.windChargePendingTicks = 0;
    }

    @Override
    public void onDisable() {
        if (this.windJumpTicks > 0 && mc.options != null && mc.options.keyJump != null) {
            if (!error.util.client.KeyCheck.isPressed(mc.options.keyJump)) {
                mc.options.keyJump.setDown(false);
            }
        }
        resetWindFlight();
        super.onDisable();
    }
}
