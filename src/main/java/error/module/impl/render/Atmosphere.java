package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.event.list.WorldJoinEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.render.Render3DUtil;
import error.util.render.WorldColorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Atmosphere Module (Ported from SystemSRC 26.2).
 * Provides atmospheric sky/fog grading, realistic seasonal/weather modes,
 * volumetric fog parameters, god rays, and ambient particulate effects.
 */
public final class Atmosphere extends Module {

    public static Atmosphere INSTANCE;
    private static Atmosphere active;

    // Modes matching SystemSRC
    public static final String MODE_DAWN = "Dawn";
    public static final String MODE_DAY = "Day";
    public static final String MODE_DUSK = "Dusk";
    public static final String MODE_NIGHT = "Night";
    public static final String MODE_DUST = "Dust";
    public static final String MODE_SNOW = "Snow";
    public static final String MODE_ASH = "Ash";

    public final ModeSetting mode = mode("mode", MODE_DAWN,
            MODE_DAWN, MODE_DAY, MODE_DUSK, MODE_NIGHT, MODE_DUST, MODE_SNOW, MODE_ASH);

    public final SliderSetting density = slider("density", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting scatterHeight = slider("scatter_height", 64.0F, 0.0F, 320.0F, 1.0F);
    public final SliderSetting godRays = slider("god_rays", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting softness = slider("softness", 1.0F, 0.0F, 3.0F, 0.05F);
    public final ColorSetting dawnColor = color("dawn_color", -20384); // 0xFFFFB060
    public final SliderSetting amount = slider("amount", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting speed = slider("speed", 1.0F, 0.0F, 5.0F, 0.1F);
    public final SliderSetting haze = slider("haze", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting grade = slider("grade", 1.0F, 0.0F, 3.0F, 0.05F);
    public final SliderSetting vignette = slider("vignette", 1.0F, 0.0F, 3.0F, 0.05F);

    private final List<AmbientParticle> ambientParticles = new ArrayList<>();
    private final error.util.render.world.particles.ParticleSpawn particleSpawn = new error.util.render.world.particles.ParticleSpawn();
    private String lastMode = "";

    public Atmosphere() {
        super("Atmosphere", "Кастомная атмосфера мира из System: Dawn, Day, Dusk, Night, Dust, Snow, Ash", Category.RENDER);
        INSTANCE = this;
    }

    public static Atmosphere active() {
        return active;
    }

    @Override
    public void onEnable() {
        active = this;
        clearParticles();
    }

    @Override
    public void onDisable() {
        active = null;
        clearParticles();
    }

    private void clearParticles() {
        this.ambientParticles.clear();
    }

    public boolean hasTimeOverride() {
        return isEnabled();
    }

    public long getTimeOverride() {
        return switch (this.mode.getValue()) {
            case MODE_DAWN -> 23000L;
            case MODE_DAY -> 6000L;
            case MODE_DUSK -> 12500L;
            case MODE_NIGHT -> 18000L;
            case MODE_DUST -> 1000L;
            case MODE_SNOW -> 6000L;
            case MODE_ASH -> 14000L;
            default -> 6000L;
        };
    }

    public int getEffectiveFogColor() {
        String m = this.mode.getValue();
        return switch (m) {
            case MODE_DAWN -> this.dawnColor.getValue();
            case MODE_DAY -> ColorUtil.rgba(200, 225, 255, 255);
            case MODE_DUSK -> ColorUtil.rgba(215, 105, 75, 255);
            case MODE_NIGHT -> ColorUtil.rgba(10, 15, 34, 255);
            case MODE_DUST -> ColorUtil.rgba(207, 166, 106, 255);
            case MODE_SNOW -> ColorUtil.rgba(232, 242, 255, 255);
            case MODE_ASH -> ColorUtil.rgba(45, 48, 56, 255);
            default -> ColorUtil.rgba(200, 225, 255, 255);
        };
    }

    public float getFogStart() {
        float dens = Math.max(0.1F, this.density.getValue().floatValue());
        float soft = Math.max(0.1F, this.softness.getValue().floatValue());
        String m = this.mode.getValue();

        float baseStart = switch (m) {
            case MODE_DAWN -> 12.0F;
            case MODE_DAY -> 25.0F;
            case MODE_DUSK -> 8.0F;
            case MODE_NIGHT -> 6.0F;
            case MODE_DUST -> 2.0F;
            case MODE_SNOW -> 2.0F;
            case MODE_ASH -> 2.0F;
            default -> 15.0F;
        };

        return Math.max(0.0F, (baseStart * soft) / dens);
    }

    public float getFogEnd() {
        float dens = Math.max(0.1F, this.density.getValue().floatValue());
        float soft = Math.max(0.1F, this.softness.getValue().floatValue());
        String m = this.mode.getValue();

        float baseEnd = switch (m) {
            case MODE_DAWN -> 110.0F;
            case MODE_DAY -> 220.0F;
            case MODE_DUSK -> 85.0F;
            case MODE_NIGHT -> 70.0F;
            case MODE_DUST -> 42.0F;
            case MODE_SNOW -> 38.0F;
            case MODE_ASH -> 34.0F;
            default -> 120.0F;
        };

        return Math.max(getFogStart() + 2.0F, (baseEnd * soft) / dens);
    }

    /**
     * Post-processing World Color Grade & Atmospheric Haze
     */
    public void renderWorldColor() {
        if (!isEnabled()) return;

        int color = getEffectiveFogColor();
        String m = this.mode.getValue();
        float gr = this.grade.getValue().floatValue();
        float hz = this.haze.getValue().floatValue();
        float sc = this.scatterHeight.getValue().floatValue();

        float saturation = switch (m) {
            case MODE_DAWN -> 1.15F * gr;
            case MODE_DAY -> 1.05F * gr;
            case MODE_DUSK -> 1.25F * gr;
            case MODE_NIGHT -> 0.90F * gr;
            case MODE_DUST -> 0.95F * gr;
            case MODE_SNOW -> 0.82F * gr;
            case MODE_ASH -> 0.50F * gr;
            default -> 1.0F;
        };

        float brightness = switch (m) {
            case MODE_DAWN -> 1.05F;
            case MODE_DAY -> 1.12F;
            case MODE_DUSK -> 0.95F;
            case MODE_NIGHT -> 0.70F;
            case MODE_DUST -> 0.90F;
            case MODE_SNOW -> 1.05F;
            case MODE_ASH -> 0.82F;
            default -> 1.0F;
        };

        float strength = 0.35F * Math.clamp(gr, 0.0F, 2.0F);
        float contrast = switch (m) {
            case MODE_DAWN -> 1.10F;
            case MODE_DAY -> 1.04F;
            case MODE_DUSK -> 1.18F;
            case MODE_NIGHT -> 1.25F;
            case MODE_DUST -> 1.08F;
            case MODE_SNOW -> 1.02F;
            case MODE_ASH -> 1.16F;
            default -> 1.0F;
        };

        float snowMask = MODE_SNOW.equals(m) ? 1.0F : 0.0F;
        float hazeStrength = 0.40F * Math.clamp(hz, 0.0F, 2.0F);
        float hazeDistance = Math.max(8.0F, sc);

        WorldColorRenderer.getInstance().render(
                color, saturation, brightness, strength, contrast, snowMask, hazeStrength, hazeDistance
        );
    }

    @EventTarget
    public void onWorldJoin(WorldJoinEvent e) {
        clearParticles();
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent event) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        String curMode = this.mode.getValue();
        if (!curMode.equals(this.lastMode)) {
            this.lastMode = curMode;
            clearParticles();
        }

        // Particle logic for modes with weather / dust / snow / ash
        boolean hasParticles = MODE_SNOW.equals(curMode) || MODE_DUST.equals(curMode) || MODE_ASH.equals(curMode);
        if (!hasParticles) {
            clearParticles();
            return;
        }

        LocalPlayer player = mc.player;
        int maxParticles = (int) (60.0F * this.amount.getValue().floatValue());
        float spd = this.speed.getValue().floatValue();

        if (this.ambientParticles.size() < maxParticles) {
            int toSpawn = Math.min(8, maxParticles - this.ambientParticles.size());

            for (int i = 0; i < toSpawn; i++) {
                double angle = Math.random() * Math.PI * 2.0;
                double dist = 1.0 + Math.random() * 22.0;
                double px = player.getX() + Math.cos(angle) * dist;
                double pz = player.getZ() + Math.sin(angle) * dist;
                double py = player.getY() + rnd(-2.0, 16.0);

                Vec3 pos = new Vec3(px, py, pz);
                Vec3 vel;
                int col;
                float sz;

                switch (curMode) {
                    case MODE_SNOW -> {
                        vel = new Vec3(rnd(-0.02, 0.02) * spd, rnd(-0.08, -0.04) * spd, rnd(-0.02, 0.02) * spd);
                        col = ColorUtil.rgba(240, 248, 255, 230);
                        sz = 0.08F + (float) Math.random() * 0.07F;
                    }
                    case MODE_DUST -> {
                        vel = new Vec3(rnd(0.04, 0.08) * spd, rnd(-0.015, 0.015) * spd, rnd(-0.02, 0.02) * spd);
                        col = ColorUtil.rgba(215, 175, 115, 210);
                        sz = 0.06F + (float) Math.random() * 0.06F;
                    }
                    case MODE_ASH -> {
                        vel = new Vec3(rnd(-0.02, 0.02) * spd, rnd(-0.05, -0.025) * spd, rnd(-0.02, 0.02) * spd);
                        col = Math.random() > 0.85
                                ? ColorUtil.rgba(255, 120, 45, 240) // glowing ember
                                : ColorUtil.rgba(65, 68, 75, 220);  // dark ash flake
                        sz = 0.07F + (float) Math.random() * 0.07F;
                    }
                    default -> {
                        vel = Vec3.ZERO;
                        col = 0xFFFFFFFF;
                        sz = 0.05F;
                    }
                }

                this.ambientParticles.add(new AmbientParticle(pos, vel, col, sz, (int) (120 + Math.random() * 120), (float) Math.random()));
            }
        }

        // Update particle life and positions
        this.ambientParticles.removeIf(p -> {
            p.pos = p.pos.add(p.vel);
            p.age++;
            return p.age >= p.maxAge || p.pos.distanceTo(player.position()) > 35.0;
        });
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled()) return;

        // Render world color grade
        renderWorldColor();

        // Render 3D ambient procedural particles
        if (!this.ambientParticles.isEmpty()) {
            List<error.util.render.world.particles.ParticleSpawn.Sprite> sprites = new ArrayList<>();
            String curMode = this.mode.getValue();
            error.util.render.world.particles.ParticleSpawn.Shape shape = switch (curMode) {
                case MODE_SNOW -> error.util.render.world.particles.ParticleSpawn.Shape.SNOWFLAKE;
                case MODE_ASH -> error.util.render.world.particles.ParticleSpawn.Shape.EMBER;
                default -> error.util.render.world.particles.ParticleSpawn.Shape.GLOW;
            };

            for (AmbientParticle p : this.ambientParticles) {
                float lifeFactor = 1.0F - ((float) p.age / (float) p.maxAge);
                int alpha = (int) (ColorUtil.alpha(p.color) * Math.sin(lifeFactor * Math.PI));
                int renderColor = ColorUtil.withAlpha(p.color, Math.max(10, alpha));

                sprites.add(new error.util.render.world.particles.ParticleSpawn.Sprite(
                        p.pos,
                        p.size,
                        renderColor,
                        shape,
                        p.age * 2.0F,
                        lifeFactor,
                        p.seed,
                        0.0F
                ));
            }

            if (!sprites.isEmpty()) {
                this.particleSpawn.render(sprites, 0.45F, true);
            }
        }
    }

    private static double rnd(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private static class AmbientParticle {
        Vec3 pos;
        Vec3 vel;
        int color;
        float size;
        int age;
        int maxAge;
        float seed;

        AmbientParticle(Vec3 pos, Vec3 vel, int color, float size, int maxAge, float seed) {
            this.pos = pos;
            this.vel = vel;
            this.color = color;
            this.size = size;
            this.age = 0;
            this.maxAge = maxAge;
            this.seed = seed;
        }
    }
}
