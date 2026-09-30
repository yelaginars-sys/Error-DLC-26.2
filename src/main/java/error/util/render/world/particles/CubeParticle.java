package error.util.render.world.particles;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Create by daun kvass
 */
public class CubeParticle {
    private static final Random RANDOM = new Random();

    public Vec3 pos;
    public Vec3 motion;

    public float yaw, pitch, roll;
    public float yawSpeed, pitchSpeed, rollSpeed;

    public float size;
    public float life;
    public float maxLife;
    public float phase;
    public float floatSpeed;

    public CubeParticle(Vec3 origin, float spawnRadius, float size, float maxLifeSeconds) {
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double dist = (0.2 + 0.8 * RANDOM.nextDouble()) * spawnRadius;
        double height = (RANDOM.nextDouble() - 0.25) * (spawnRadius * 0.7);

        this.pos = origin.add(
                Math.cos(angle) * dist,
                height,
                Math.sin(angle) * dist
        );

        this.motion = new Vec3(
                (RANDOM.nextDouble() - 0.5) * 0.12,
                0.08 + RANDOM.nextDouble() * 0.15,
                (RANDOM.nextDouble() - 0.5) * 0.12
        );

        this.yaw = RANDOM.nextFloat() * 360.0F;
        this.pitch = RANDOM.nextFloat() * 360.0F;
        this.roll = RANDOM.nextFloat() * 360.0F;

        this.yawSpeed = (RANDOM.nextFloat() - 0.5F) * 45.0F;
        this.pitchSpeed = (RANDOM.nextFloat() - 0.5F) * 45.0F;
        this.rollSpeed = (RANDOM.nextFloat() - 0.5F) * 45.0F;

        this.size = size * (0.65F + RANDOM.nextFloat() * 0.7F);
        this.life = 0.0F;
        this.maxLife = maxLifeSeconds * (0.8F + RANDOM.nextFloat() * 0.4F);
        this.phase = RANDOM.nextFloat() * (float) Math.PI * 2.0F;
        this.floatSpeed = 1.2F + RANDOM.nextFloat() * 1.5F;
    }

    public void update(float dt, Vec3 center, float maxRadius) {
        this.life += dt;

        double waveY = Math.sin(this.life * this.floatSpeed + this.phase) * 0.08;
        double waveX = Math.cos(this.life * (this.floatSpeed * 0.7F) + this.phase) * 0.05;
        double waveZ = Math.sin(this.life * (this.floatSpeed * 0.7F) + this.phase) * 0.05;

        this.pos = this.pos.add(
                (this.motion.x + waveX) * dt,
                (this.motion.y + waveY) * dt,
                (this.motion.z + waveZ) * dt
        );

        this.yaw += this.yawSpeed * dt;
        this.pitch += this.pitchSpeed * dt;
        this.roll += this.rollSpeed * dt;

        if (this.pos.distanceTo(center) > maxRadius * 1.6) {
            this.life = this.maxLife;
        }
    }

    public boolean isDead() {
        return this.life >= this.maxLife;
    }

    public float getAlphaProgress() {
        float progress = this.life / this.maxLife;
        if (progress <= 0.0F || progress >= 1.0F) return 0.0F;
        return (float) Math.sin(progress * Math.PI);
    }
}