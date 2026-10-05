package error.module.impl.misc;

import com.mojang.authlib.GameProfile;
import error.event.EventTarget;
import error.event.list.GameTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class FakePlayer extends Module {

    private static final int BASE_ID = 2147483646;

    public final CheckBox copyEquipment = checkbox("Copy equipment", true);
    public final CheckBox copySkin      = checkbox("Copy skin", true);
    public final CheckBox infiniteHealth = checkbox("Infinite health", true);
    public final CheckBox spawnInFront  = checkbox("Spawn in front", false);
    public final SliderSetting health   = slider("Здоровье", 20.0f, 1.0f, 100.0f, 1.0f).visible(() -> !infiniteHealth.getValue());

    private FakePlayerEntity fakePlayer;

    public FakePlayer() {
        super("FakePlayer", "Creates a client-side player dummy with infinite health", Category.MISC);
    }

    private void removeFakePlayer() {
        if (this.fakePlayer != null) {
            java.util.UUID fakeUuid = this.fakePlayer.getUUID();
            try {
                error.Client client = error.Client.getInstance();
                if (client != null && client.getModuleManager() != null) {
                    error.module.impl.render.NameTags nameTags = client.getModuleManager().getNameTags();
                    if (nameTags != null) {
                        nameTags.removeLoggedPlayer(fakeUuid);
                    }
                }
            } catch (Throwable ignored) {}

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && !this.fakePlayer.isRemoved()) {
                mc.level.removeEntity(this.fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
            }
            this.fakePlayer.discard();
            this.fakePlayer = null;
        }
    }

    @Override
    public void onDisable() {
        this.removeFakePlayer();
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.spawnFakePlayer();
    }

    private void spawnFakePlayer() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return;
        }

        this.removeFakePlayer();

        int safeId = findSafeId(level);
        if (safeId == -1) {
            return;
        }

        GameProfile profile = this.copySkin.getValue()
                ? new GameProfile(UUID.randomUUID(), "FakePlayer", player.getGameProfile().properties())
                : new GameProfile(UUID.randomUUID(), "FakePlayer");

        FakePlayerEntity dummy = new FakePlayerEntity(level, profile, this.infiniteHealth.getValue(), this.copySkin.getValue());
        dummy.setId(safeId);

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        if (this.spawnInFront.getValue()) {
            double rad = Math.toRadians(yaw);
            x -= Math.sin(rad) * 2.0;
            z += Math.cos(rad) * 2.0;
            yaw += 180.0F;
            pitch = 0.0F;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (level.getBlockState(pos.below()).isAir()) {
                BlockPos cur = pos;
                while (cur.getY() > level.getMinY() && level.getBlockState(cur.below()).isAir()) {
                    cur = cur.below();
                }
                y = cur.getY();
            }
        }

        dummy.snapTo(x, y, z, yaw, pitch);
        dummy.setYHeadRot(yaw);
        dummy.yHeadRotO = yaw;
        dummy.setYBodyRot(yaw);
        dummy.yBodyRotO = yaw;
        dummy.setOnGround(player.onGround());

        try {
            java.lang.reflect.Field field = Player.class.getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            net.minecraft.network.syncher.EntityDataAccessor<Byte> accessor = (net.minecraft.network.syncher.EntityDataAccessor<Byte>) field.get(null);
            byte skinLayers = player.getEntityData().get(accessor);
            dummy.getEntityData().set(accessor, skinLayers);
        } catch (Throwable ignored) {}

        if (this.copyEquipment.getValue()) {
            dummy.getInventory().replaceWith(player.getInventory());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                dummy.setItemSlot(slot, player.getItemBySlot(slot).copy());
            }
        }

        if (this.infiniteHealth.getValue()) {
            dummy.healFully();
        } else {
            dummy.setHealth(this.health.getValue());
        }

        level.addEntity(dummy);
        this.fakePlayer = dummy;
    }

    private static int findSafeId(ClientLevel level) {
        for (int i = 0; i < 512; i++) {
            int id = BASE_ID - i;
            if (level.getEntity(id) == null) {
                return id;
            }
        }
        return -1;
    }

    @EventTarget
    public void onTick(GameTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            if (this.fakePlayer == null || this.fakePlayer.isRemoved() || !this.fakePlayer.isAlive() || mc.level.getEntity(this.fakePlayer.getId()) != this.fakePlayer) {
                this.spawnFakePlayer();
            } else {
                this.fakePlayer.setInfiniteHealth(this.infiniteHealth.getValue());
                this.fakePlayer.setCopySkin(this.copySkin.getValue());
            }
        } else {
            this.fakePlayer = null;
        }
    }
}