package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.ClientSoundPlayer;
import error.module.impl.combat.AuraModule;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class TotemSounds extends Module {

    public static TotemSounds INSTANCE;

    public final ModeSetting sound = mode("Звук", "uwu", "uwu", "выстрел", "200");
    public final SliderSetting volume = slider("Громкость", 100.0f, 0.0f, 100.0f, 1.0f);
    public final CheckBox onlyTarget = checkbox("Только таргет", true);

    private long lastPlayTime = 0L;

    public TotemSounds() {
        super("TotemSounds", "Воспроизводит звук при снятии тотема у таргета", Category.MISC);
        INSTANCE = this;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isReceive() || event.getPacket() == null) return;
        if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;

        if (packet.getEventId() != 35 || mc.level == null || mc.player == null) return;

        Entity entity = packet.getEntity(mc.level);
        if (entity == null || entity == mc.player) return;

        LivingEntity currentTarget = AuraModule.INSTANCE != null ? AuraModule.INSTANCE.getTarget() : null;
        boolean isTarget = (currentTarget != null && currentTarget == entity);

        if (onlyTarget.getValue() && !isTarget) return;

        playTotemSound();
    }

    public void playTotemSound() {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastPlayTime < 50) return;
        lastPlayTime = now;

        double vol = Math.max(0.0, Math.min(1.0, volume.getValue() / 100.0));
        String selected = sound.getValue();
        String fileName = switch (selected) {
            case "выстрел" -> "выстрел";
            case "200" -> "200";
            default -> "uwu";
        };

        ClientSoundPlayer.playSound(fileName, vol, 1.0f);
    }
}
