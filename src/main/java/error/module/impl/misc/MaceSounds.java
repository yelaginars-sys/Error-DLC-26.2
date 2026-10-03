package error.module.impl.misc;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

public class MaceSounds extends Module {
    public static MaceSounds INSTANCE;

    public final ModeSetting soundMode = mode("Звук", "Heavy Smash", "Heavy Smash", "Thunder Smash", "Anvil Clank", "Crash Strike");
    public final SliderSetting volume = slider("Громкость", 1.0f, 0.1f, 2.0f, 0.1f);
    public final SliderSetting pitch = slider("Высота тона", 1.0f, 0.5f, 2.0f, 0.1f);
    public final CheckBox onlyCrits = checkbox("Только для критов", true);
    public final CheckBox spawnParticles = checkbox("Эффекты всплеска", true);

    public MaceSounds() {
        super("MaceSounds", "Кастомные мощные звуки и визуальные эффекты удара булавой", Category.MISC);
        INSTANCE = this;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!inGame() || player() == null || mc.level == null) return;

        ItemStack held = player().getMainHandItem();
        if (!held.is(Items.MACE)) return;

        boolean isCrit = player().fallDistance > 0.0F && !player().onGround() && !player().isInWater();
        if (onlyCrits.getValue() && !isCrit) return;

        Entity target = event.getTarget();
        playMaceSound(target != null ? target.getX() : player().getX(), target != null ? target.getY() : player().getY(), target != null ? target.getZ() : player().getZ());

        if (spawnParticles.getValue() && target != null) {
            mc.level.addParticle(ParticleTypes.GUST_EMITTER_LARGE, target.getX(), target.getY() + 1.0, target.getZ(), 0.0, 0.0, 0.0);
            mc.level.addParticle(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(), 0.0, 0.0, 0.0);
        }
    }

    private void playMaceSound(double x, double y, double z) {
        if (mc.level == null) return;

        float vol = volume.getValue();
        float p = pitch.getValue();

        switch (soundMode.getValue()) {
            case "Thunder Smash" -> {
                mc.level.playSound(player(), x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, vol * 0.8f, p * 1.2f);
                mc.level.playSound(player(), x, y, z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, vol, p);
            }
            case "Anvil Clank" -> {
                mc.level.playSound(player(), x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, vol, p * 0.9f);
                mc.level.playSound(player(), x, y, z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, vol, p);
            }
            case "Crash Strike" -> {
                mc.level.playSound(player(), x, y, z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, vol * 1.2f, p * 1.1f);
                mc.level.playSound(player(), x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, vol, p);
            }
            default -> {
                mc.level.playSound(player(), x, y, z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, vol, p);
            }
        }
    }
}
