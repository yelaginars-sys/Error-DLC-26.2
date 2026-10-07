package error.module.impl.player;

import com.mojang.authlib.GameProfile;
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.event.list.Render2DEvent;
import error.event.list.WorldLeaveEvent;
import error.module.Category;
import error.module.Module;
import error.module.impl.misc.FakePlayerEntity;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.player.InventoryUtil;
import error.util.render.font.Fonts;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class GodMode extends Module {
    public static GodMode INSTANCE;

    public enum State {
        WAITING,
        ACTIVE,
        EATING_CHORUS
    }

    public final SliderSetting waitSeconds = slider("Ожидание (сек)", 7.0F, 1.0F, 15.0F, 0.5F);
    public final CheckBox autoChorus = checkbox("Авто-Хорус при выходе", true);
    public final CheckBox cancelDamage = checkbox("Блокировать урон", true);
    public final CheckBox timerDisplay = checkbox("Отображать статус", true);

    private FakePlayerEntity fakePlayer;
    private long enableTime;
    private Vec3 origin;
    private State state = State.WAITING;

    private int previousSlot = -1;
    private int chorusInventorySlot = -1;
    private int chorusHotbarSlot = -1;
    private int eatingTicks = 0;
    private boolean isCleaningUp = false;
    private boolean isSilent = false;

    public GodMode() {
        super("GodMode", "Десинк-бессмертие: бег обычным игроком с телом на origin и выходом через хорус", Category.PLAYER);
        INSTANCE = this;
    }

    public static GodMode getInstance() {
        return INSTANCE;
    }

    public State getGodState() {
        return state;
    }

    private void sendSilentPacket(Packet<?> packet) {
        if (mc.getConnection() == null || packet == null) return;
        this.isSilent = true;
        try {
            mc.getConnection().send(packet);
        } finally {
            this.isSilent = false;
        }
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.level == null) {
            this.setState(false);
            return;
        }

        this.origin = mc.player.position();
        this.enableTime = System.currentTimeMillis();
        this.state = State.WAITING;
        this.eatingTicks = 0;
        this.isCleaningUp = false;
        this.isSilent = false;
        this.previousSlot = -1;
        this.chorusInventorySlot = -1;
        this.chorusHotbarSlot = -1;

        spawnFakePlayer();
        super.onEnable();
    }

    private void spawnFakePlayer() {
        if (mc.player == null || mc.level == null) return;

        GameProfile profile = new GameProfile(
                UUID.fromString("69696969-6969-6969-6969-696969696969"),
                mc.player.getGameProfile().name()
        );
        this.fakePlayer = new FakePlayerEntity(mc.level, profile, true, true);
        this.fakePlayer.snapTo(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYRot(),
                mc.player.getXRot()
        );
        this.fakePlayer.setHealth(20.0F);
        this.fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        this.fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().copy());
        this.fakePlayer.setItemInHand(InteractionHand.OFF_HAND, mc.player.getOffhandItem().copy());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.fakePlayer.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
        }

        mc.level.addEntity(this.fakePlayer);
    }

    private void activateActiveMode() {
        this.state = State.ACTIVE;
    }

    @Override
    public void onDisable() {
        if (this.isCleaningUp) {
            super.onDisable();
            return;
        }

        if (this.state == State.ACTIVE && this.autoChorus.getValue() && mc.player != null) {
            if (startEatingChorus()) {
                return;
            }
        }

        cleanupAndFinish();
        super.onDisable();
    }

    private boolean startEatingChorus() {
        if (mc.player == null || mc.gameMode == null) return false;

        // 1. Hands check
        if (mc.player.getMainHandItem().is(Items.CHORUS_FRUIT) || mc.player.getOffhandItem().is(Items.CHORUS_FRUIT)) {
            this.state = State.EATING_CHORUS;
            this.eatingTicks = 0;
            if (mc.options != null && mc.options.keyUse != null) mc.options.keyUse.setDown(true);
            return true;
        }

        // 2. Hotbar check
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.CHORUS_FRUIT)) {
                this.previousSlot = mc.player.getInventory().getSelectedSlot();
                this.chorusHotbarSlot = i;
                this.chorusInventorySlot = -1;
                selectHotbarSlot(i);
                this.state = State.EATING_CHORUS;
                this.eatingTicks = 0;
                if (mc.options != null && mc.options.keyUse != null) mc.options.keyUse.setDown(true);
                return true;
            }
        }

        // 3. Inventory check
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.CHORUS_FRUIT)) {
                this.previousSlot = mc.player.getInventory().getSelectedSlot();
                this.chorusHotbarSlot = this.previousSlot;
                this.chorusInventorySlot = i;

                InventoryUtil.swapSlots(i, this.chorusHotbarSlot);
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundContainerClosePacket(0));
                }

                this.state = State.EATING_CHORUS;
                this.eatingTicks = 0;
                if (mc.options != null && mc.options.keyUse != null) mc.options.keyUse.setDown(true);
                return true;
            }
        }

        return false;
    }

    private void cleanupAndFinish() {
        this.isCleaningUp = true;

        if (mc.options != null && mc.options.keyUse != null) {
            mc.options.keyUse.setDown(false);
        }

        if (mc.player != null) {
            if (this.chorusInventorySlot != -1 && this.chorusHotbarSlot != -1 && mc.gameMode != null) {
                InventoryUtil.swapSlots(this.chorusInventorySlot, this.chorusHotbarSlot);
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundContainerClosePacket(0));
                }
            }

            if (this.previousSlot != -1) {
                selectHotbarSlot(this.previousSlot);
            }
        }

        if (this.fakePlayer != null && mc.level != null) {
            if (!this.fakePlayer.isRemoved()) {
                mc.level.removeEntity(this.fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            }
            this.fakePlayer.discard();
            this.fakePlayer = null;
        }

        this.origin = null;
        this.state = State.WAITING;
        this.previousSlot = -1;
        this.chorusInventorySlot = -1;
        this.chorusHotbarSlot = -1;
        this.eatingTicks = 0;

        if (this.isEnabled()) {
            this.setState(false);
        }
        this.isCleaningUp = false;
    }

    private void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8 || mc.player == null) return;
        mc.player.getInventory().setSelectedSlot(slot);
        if (mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    @EventTarget
    public void onWorldLeave(WorldLeaveEvent event) {
        cleanupAndFinish();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || mc.player == null) return;

        long waitMs = (long) (this.waitSeconds.getValue() * 1000L);
        long elapsed = System.currentTimeMillis() - this.enableTime;

        if (this.state == State.WAITING) {
            if (elapsed >= waitMs) {
                activateActiveMode();
            }
        } else if (this.state == State.ACTIVE) {
            if (this.cancelDamage.getValue()) {
                mc.player.setHealth(20.0F);
            }
            if (this.fakePlayer != null) {
                this.fakePlayer.setHealth(20.0F);
                this.fakePlayer.removeAllEffects();
            }

            if (mc.player.isUsingItem() && (mc.player.getMainHandItem().is(Items.CHORUS_FRUIT) || mc.player.getOffhandItem().is(Items.CHORUS_FRUIT))) {
                this.state = State.EATING_CHORUS;
                this.eatingTicks = 0;
            }
        } else if (this.state == State.EATING_CHORUS) {
            this.eatingTicks++;
            if (mc.options != null && mc.options.keyUse != null) {
                mc.options.keyUse.setDown(true);
            }

            if (this.eatingTicks > 36 && !mc.player.isUsingItem()) {
                cleanupAndFinish();
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isSilent || mc.player == null) return;

        if (event.isReceive()) {
            Packet<?> packet = event.getPacket();

            if (packet instanceof ClientboundPlayerPositionPacket lookPacket) {
                if (this.state == State.EATING_CHORUS) {
                    cleanupAndFinish();
                    return;
                }

                event.cancel();
                sendSilentPacket(new ServerboundAcceptTeleportationPacket(lookPacket.id()));
                sendSilentPacket(new ServerboundMovePlayerPacket.Rot(
                        mc.player.getYRot(),
                        mc.player.getXRot(),
                        mc.player.onGround(),
                        mc.player.horizontalCollision
                ));
                return;
            }

            if (this.cancelDamage.getValue()) {
                if (packet instanceof ClientboundSetHealthPacket
                        || packet instanceof ClientboundHurtAnimationPacket
                        || packet instanceof ClientboundDamageEventPacket
                        || packet instanceof ClientboundExplodePacket) {
                    event.cancel();
                } else if (packet instanceof ClientboundEntityEventPacket statusPacket) {
                    if (statusPacket.getEventId() == 2 || statusPacket.getEventId() == 33 || statusPacket.getEventId() == 3) {
                        if (this.fakePlayer != null && mc.level != null && statusPacket.getEntity(mc.level) == this.fakePlayer) {
                            event.cancel();
                        } else if (mc.level != null && statusPacket.getEntity(mc.level) == mc.player) {
                            event.cancel();
                        }
                    }
                }
            }
            return;
        }

        Packet<?> packet = event.getPacket();

        if (this.state == State.EATING_CHORUS) {
            if (packet instanceof ServerboundUseItemPacket || packet instanceof ServerboundSetCarriedItemPacket) {
                return;
            }
        }

        if (packet instanceof ServerboundMovePlayerPacket.PosRot full) {
            event.cancel();
            sendSilentPacket(new ServerboundMovePlayerPacket.Rot(
                    full.getYRot(mc.player.getYRot()),
                    full.getXRot(mc.player.getXRot()),
                    mc.player.onGround(),
                    mc.player.horizontalCollision
            ));
        } else if (packet instanceof ServerboundMovePlayerPacket.Pos) {
            event.cancel();
            sendSilentPacket(new ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    mc.player.onGround(),
                    mc.player.horizontalCollision
            ));
        } else if (packet instanceof ServerboundMovePlayerPacket.StatusOnly) {
            event.cancel();
            sendSilentPacket(new ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    mc.player.onGround(),
                    mc.player.horizontalCollision
            ));
        } else if (packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundPlayerActionPacket) {
            if (this.state != State.EATING_CHORUS) {
                event.cancel();
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.player == null || this.origin == null || !this.timerDisplay.getValue() || mc.getWindow() == null) return;

        long waitMs = (long) (this.waitSeconds.getValue() * 1000L);
        long elapsed = System.currentTimeMillis() - this.enableTime;

        double d = mc.player.getX() - this.origin.x;
        double e2 = mc.player.getY() - this.origin.y;
        double d2 = mc.player.getZ() - this.origin.z;
        double distance = Math.sqrt(d * d + e2 * e2 + d2 * d2);

        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0F;
        float centerY = mc.getWindow().getGuiScaledHeight() / 2.0F - 30.0F;

        if (this.state == State.WAITING) {
            int secondsLeft = (int) Math.ceil((waitMs - elapsed) / 1000.0);
            String waitText = String.format("Ждите %d секунд", Math.max(1, secondsLeft));
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, waitText, centerX, centerY, 8.0F, ColorUtil.rgba(255, 180, 50, 255));
        } else if (this.state == State.ACTIVE) {
            String activeText = String.format("GodMode Активен (Дистанция: %.1fм)", distance);
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, activeText, centerX, centerY, 8.0F, ColorUtil.rgba(100, 255, 100, 255));
        } else if (this.state == State.EATING_CHORUS) {
            String chorusText = "Кушаем плод хоруса для возврата...";
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, chorusText, centerX, centerY, 8.0F, ColorUtil.rgba(255, 100, 200, 255));
        }
    }
}
