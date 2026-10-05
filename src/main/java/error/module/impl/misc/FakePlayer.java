package error.module.impl.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;

import java.util.UUID;

public final class FakePlayer extends Module {

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

        this.fakePlayer = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "FakePlayer")) {
            @Override
            public PlayerSkin getSkin() {
                return mc.player != null ? mc.player.getSkin() : super.getSkin();
            }
        };

        double rad = Math.toRadians(mc.player.getYRot());
        double x = mc.player.getX() - Math.sin(rad) * 2.0;
        double z = mc.player.getZ() + Math.cos(rad) * 2.0;
        double y = mc.player.getY();
        BlockPos pos = BlockPos.containing(x, y, z);
        if (mc.level.getBlockState(pos.below()).isAir()) {
            BlockPos cur = pos;
            while (cur.getY() > mc.level.getMinY() && mc.level.getBlockState(cur.below()).isAir()) {
                cur = cur.below();
            }
            y = cur.getY();
        }

        this.fakePlayer.snapTo(x, y, z, mc.player.getYRot() + 180.0F, 0.0F);
        this.fakePlayer.setYHeadRot(mc.player.getYRot() + 180.0F);
        this.fakePlayer.yHeadRotO = mc.player.getYRot() + 180.0F;
        this.fakePlayer.setYBodyRot(mc.player.getYRot() + 180.0F);
        this.fakePlayer.yBodyRotO = mc.player.getYRot() + 180.0F;
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

        this.fakePlayer.setInvulnerable(false);
        mc.level.addEntity(this.fakePlayer);
    }

    private void removeFakePlayer() {
        if (this.fakePlayer != null) {
            if (mc.level != null) {
                mc.level.removeEntity(this.fakePlayer.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
            this.fakePlayer.discard();
            this.fakePlayer = null;
        }
    }
}