package error.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;

public final class FakePlayer extends Module {

    private static final int FAKE_PLAYER_ID = -1337;

    public final CheckBox copyInventory = checkbox("Copy Inv", true);
    public final CheckBox copyHealth    = checkbox("Copy hp", true);
    public final SliderSetting health   = slider("Здоровье", 20.0f, 1.0f, 100.0f, 1.0f);

    private RemotePlayer fakePlayer;

    public FakePlayer() {
        super("FakePlayer", "Спавнит фейкового игрока для тестов", Category.MISC);
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }

        spawnFakePlayer(mc);
    }

    @Override
    public void onDisable() {
        removeFakePlayer();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (this.fakePlayer == null || !this.fakePlayer.isAlive()) {
            if (this.isEnabled()) {
                spawnFakePlayer(mc);
            }
            return;
        }

        if (!this.copyHealth.getValue()) {
            this.fakePlayer.setHealth(this.health.getValue());
        }
    }

    private void spawnFakePlayer(Minecraft mc) {
        removeFakePlayer();

        this.fakePlayer = new RemotePlayer(mc.level, mc.player.getGameProfile());
        this.fakePlayer.setId(FAKE_PLAYER_ID);

        this.fakePlayer.copyPosition(mc.player);
        this.fakePlayer.setYHeadRot(mc.player.getYHeadRot());
        this.fakePlayer.yHeadRotO = mc.player.getYHeadRot();
        this.fakePlayer.setYBodyRot(mc.player.yBodyRot);
        this.fakePlayer.yBodyRotO = mc.player.yBodyRot;
        this.fakePlayer.setOnGround(mc.player.onGround());

        try {
            java.lang.reflect.Field field = Player.class.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            net.minecraft.network.syncher.EntityDataAccessor<Byte> accessor = (net.minecraft.network.syncher.EntityDataAccessor<Byte>) field.get(null);
            byte skinLayers = mc.player.getEntityData().get(accessor);
            this.fakePlayer.getEntityData().set(accessor, skinLayers);
        } catch (Throwable ignored) {}

        if (this.copyHealth.getValue()) {
            this.fakePlayer.setHealth(mc.player.getHealth());
            this.fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
        } else {
            this.fakePlayer.setHealth(this.health.getValue());
        }

        if (this.copyInventory.getValue()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                this.fakePlayer.setItemSlot(slot, mc.player.getItemBySlot(slot).copy());
            }
        }

        mc.level.addEntity(this.fakePlayer);
    }

    private void removeFakePlayer() {
        if (this.fakePlayer != null) {
            if (mc.level != null) {
                mc.level.removeEntity(FAKE_PLAYER_ID, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            this.fakePlayer.discard();
            this.fakePlayer = null;
        }
    }
}