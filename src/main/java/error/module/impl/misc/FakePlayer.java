package error.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;

import java.lang.reflect.Field;

/**
 */
public final class FakePlayer extends Module {

    private static final int FAKE_PLAYER_ID = -1337;
    private static final EntityDataAccessor<Byte> SKIN_CUSTOMISATION = resolveSkinCustomisation();

    public final CheckBox copyInventory = checkbox("Copy Inv", true);
    public final CheckBox copyHealth = checkbox("Copy hp", true);

    private RemotePlayer fakePlayer;

    public FakePlayer() {
        super("FakePlayer", "Спавнит фек игрока", Category.MISC);
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

        if (SKIN_CUSTOMISATION != null) {
            byte skinLayers = mc.player.getEntityData().get(SKIN_CUSTOMISATION);
            this.fakePlayer.getEntityData().set(SKIN_CUSTOMISATION, skinLayers);
        }

        if (this.copyHealth.getValue()) {
            this.fakePlayer.setHealth(mc.player.getHealth());
            this.fakePlayer.setAbsorptionAmount(mc.player.getAbsorptionAmount());
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
            this.fakePlayer.discard();
            this.fakePlayer = null;
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> resolveSkinCustomisation() {
        Class<?> clazz = Player.class;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if ("DATA_PLAYER_MODE_CUSTOMISATION".equals(field.getName())) {
                    try {
                        field.setAccessible(true);
                        return (EntityDataAccessor<Byte>) field.get(null);
                    } catch (Exception ignored) {
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}