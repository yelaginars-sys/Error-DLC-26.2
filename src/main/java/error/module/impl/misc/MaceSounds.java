package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.event.list.PacketEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.ClientSoundPlayer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MaceSounds extends Module {
    public static MaceSounds INSTANCE;

    public final ModeSetting soundMode = mode(
        "Звук", "Меганайт",
        "Меганайт", "Улетела шваль", "Кимпинтяу", "Калатушечка", "Горох", "Зусу", "Я200 нахуй",
        "Heavy Smash", "Thunder Smash", "Anvil Clank", "Crash Strike"
    );
    public final SliderSetting volume = slider("Громкость", 85.0f, 10.0f, 100.0f, 1.0f);
    public final SliderSetting pitch = slider("Высота тона", 1.0f, 0.5f, 2.0f, 0.1f);
    public final CheckBox onlyCrits = checkbox("Только для критов", false);
    public final CheckBox spawnParticles = checkbox("Эффекты всплеска", true);

    private long lastPlayTime = 0L;

    public MaceSounds() {
        super("MaceSounds", "Кастомные звуки ударов булавой из Exclusive", Category.MISC);
        INSTANCE = this;
    }

    public static boolean isMace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.is(Items.MACE)) return true;
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("булава") || name.contains("mace");
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!inGame() || player() == null || mc.level == null) return;

        ItemStack held = player().getMainHandItem();
        if (!isMace(held)) return;

        boolean isCrit = player().fallDistance > 0.0F && !player().onGround() && !player().isInWater();
        if (onlyCrits.getValue() && !isCrit) return;

        Entity target = event.getTarget();
        playMaceSound();

        if (spawnParticles.getValue() && target != null) {
            mc.level.addParticle(ParticleTypes.GUST_EMITTER_LARGE, target.getX(), target.getY() + 1.0, target.getZ(), 0.0, 0.0, 0.0);
            mc.level.addParticle(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isReceive() || event.getPacket() == null) return;

        if (event.getPacket() instanceof ClientboundSoundPacket soundPacket) {
            String soundPath = soundPacket.getSound().value().location().getPath();
            if (isMaceSound(soundPath)) {
                event.cancel();
                playMaceSound();
            }
        } else if (event.getPacket() instanceof ClientboundSoundEntityPacket entitySoundPacket) {
            String soundPath = entitySoundPacket.getSound().value().location().getPath();
            if (isMaceSound(soundPath)) {
                event.cancel();
                playMaceSound();
            }
        }
    }

    public static boolean isMaceSound(String soundPath) {
        if (soundPath == null) return false;
        String lower = soundPath.toLowerCase();
        return lower.contains("mace.smash") || lower.contains("mace_smash")
                || lower.equals("item.mace.smash_air")
                || lower.equals("item.mace.smash_ground")
                || lower.equals("item.mace.smash_ground_heavy");
    }

    public void playMaceSound() {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastPlayTime < 75) return;
        lastPlayTime = now;

        float vol = Math.max(0.01f, Math.min(1.0f, volume.getValue() / 100.0f));
        float p = pitch.getValue();
        String mode = soundMode.getValue();

        switch (mode) {
            case "Меганайт" -> ClientSoundPlayer.playSound("mace/meganight.wav", vol, p);
            case "Улетела шваль" -> ClientSoundPlayer.playSound("mace/shval.wav", vol, p);
            case "Кимпинтяу" -> ClientSoundPlayer.playSound("mace/kimpintyau.wav", vol, p);
            case "Калатушечка" -> ClientSoundPlayer.playSound("mace/kalatushechka.wav", vol, p);
            case "Горох" -> ClientSoundPlayer.playSound("mace/goroh.wav", vol, p);
            case "Зусу" -> ClientSoundPlayer.playSound("mace/zusu.wav", vol, p);
            case "Я200 нахуй" -> ClientSoundPlayer.playSound("mace/200.WAV", vol, p);
            case "Thunder Smash" -> {
                if (mc.level != null && player() != null) {
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, vol * 0.8f, p * 1.2f);
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, vol, p);
                }
            }
            case "Anvil Clank" -> {
                if (mc.level != null && player() != null) {
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, vol, p * 0.9f);
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, vol, p);
                }
            }
            case "Crash Strike" -> {
                if (mc.level != null && player() != null) {
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, vol * 1.2f, p * 1.1f);
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, vol, p);
                }
            }
            case "Heavy Smash" -> {
                if (mc.level != null && player() != null) {
                    mc.level.playSound(player(), player().getX(), player().getY(), player().getZ(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, vol, p);
                }
            }
            default -> ClientSoundPlayer.playSound("mace/meganight.wav", vol, p);
        }
    }
}
