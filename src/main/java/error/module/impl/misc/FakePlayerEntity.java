package error.module.impl.misc;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

public class FakePlayerEntity extends RemotePlayer {
    private static final double RAD_TO_DEG = 180.0 / Math.PI;
    private static final double DEG_OFFSET = 90.0;
    private boolean infiniteHealth;
    private boolean copySkin;

    public FakePlayerEntity(ClientLevel level, GameProfile profile, boolean infiniteHealth, boolean copySkin) {
        super(level, profile);
        this.infiniteHealth = infiniteHealth;
        this.copySkin = copySkin;
    }

    public void setInfiniteHealth(boolean infiniteHealth) {
        this.infiniteHealth = infiniteHealth;
    }

    public void setCopySkin(boolean copySkin) {
        this.copySkin = copySkin;
    }

    @Override
    public PlayerSkin getSkin() {
        Minecraft mc = Minecraft.getInstance();
        if (this.copySkin && mc.player != null) {
            return mc.player.getSkin();
        }
        return super.getSkin();
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    public void healFully() {
        this.setHealth(this.getMaxHealth());
        this.clearFire();
    }

    private float damageYaw(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker == null) {
            return this.getYRot();
        } else {
            double dx = attacker.getX() - this.getX();
            double dz = attacker.getZ() - this.getZ();
            return (float) (Mth.atan2(dz, dx) * RAD_TO_DEG - DEG_OFFSET);
        }
    }

    public void takeLocalHit(DamageSource source, float amount) {
        Minecraft mc = Minecraft.getInstance();
        Entity attacker = source.getEntity();
        float finalDamage = amount;

        if (attacker instanceof Player player) {
            boolean isCrit = player.fallDistance > 0.0F
                    && !player.onGround()
                    && !player.onClimbable()
                    && !player.isInWater()
                    && !player.hasEffect(MobEffects.BLINDNESS)
                    && !player.isPassenger();
            if (isCrit) {
                finalDamage *= 1.5f;
                if (mc.particleEngine != null) {
                    mc.particleEngine.createTrackingEmitter(this, ParticleTypes.CRIT);
                }
            }
        }

        this.handleDamageEvent(source);
        this.animateHurt(this.damageYaw(source));
        this.playHurtSound(source);

        if (this.infiniteHealth) {
            float newHealth = this.getHealth() - Math.max(0.0F, finalDamage);
            this.setHealth(newHealth > 0.0F ? newHealth : this.getMaxHealth());
        } else {
            this.setHealth(Math.max(0.0F, this.getHealth() - finalDamage));
        }
    }
}
