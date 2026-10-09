package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.event.list.Render3DEvent;
import error.event.list.WorldJoinEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Atmosphere extends Module {

    public static Atmosphere INSTANCE;

    public static final String VIBE_WINTER = "Зимний";
    public static final String VIBE_SUMMER = "Летний";
    public static final String VIBE_AUTUMN = "Осенний";
    public static final String VIBE_SPRING = "Весенний";

    public final ModeSetting vibeMode = mode(
            "Атмосфера",
            VIBE_WINTER,
            VIBE_WINTER, VIBE_SUMMER, VIBE_AUTUMN, VIBE_SPRING
    );

    public final CheckBox fog = checkbox(
            "Кастомный туман",
            true
    );

    public final CheckBox vignette = checkbox(
            "Виньетка",
            true
    );

    public final CheckBox bloom = checkbox(
            "Свечение",
            true
    );

    public final CheckBox particles = checkbox(
            "Частицы атмосферы",
            true
    );

    public final SliderSetting particleAmount = slider(
            "Количество частиц",
            50.0F,
            5.0F,
            160.0F,
            5.0F
    );

    private final List<AmbientParticle> ambientParticles = new ArrayList<>();
    private final List<GroundRipple> groundRipples = new ArrayList<>();
    private String lastVibe = "";

    public Atmosphere() {
        super("Atmosphere", "Кастомная атмосфера мира: Зимняя, Летняя, Осенняя, Весенняя", Category.RENDER);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        clearState();
        reloadWorldRender();
    }

    @Override
    protected void onDisable() {
        clearState();
        reloadWorldRender();
    }

    private void clearState() {
        this.ambientParticles.clear();
        this.groundRipples.clear();
    }

    private void reloadWorldRender() {
        clearState();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null) {
            try {
                for (Method m : mc.levelRenderer.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == void.class) {
                        if (m.getName().equals("allChanged") || m.getName().equals("reload")) {
                            m.setAccessible(true);
                            m.invoke(mc.levelRenderer);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    public int getVibeIndex() {
        String current = this.vibeMode.getValue();
        return switch (current) {
            case VIBE_SUMMER -> 1;
            case VIBE_AUTUMN -> 2;
            case VIBE_SPRING -> 3;
            default -> 0; // Winter
        };
    }

    public int getGrassColor() {
        return switch (getVibeIndex()) {
            case 1 -> 0xFF35E035; // Summer
            case 2 -> 0xFFE08226; // Autumn
            case 3 -> 0xFF4ADE80; // Spring
            default -> 0xFFFFFFFF; // Winter
        };
    }

    public int getFoliageColor() {
        return switch (getVibeIndex()) {
            case 1 -> 0xFF22C55E;
            case 2 -> 0xFFEA580C;
            case 3 -> 0xFF22C55E;
            default -> 0xFFF8FCFF;
        };
    }

    public int getWaterColor() {
        return switch (getVibeIndex()) {
            case 1 -> 0xFF06B6D4;
            case 2 -> 0xFF0F766E;
            case 3 -> 0xFF38BDF8;
            default -> 0xFF93C5FD;
        };
    }

    public int getFogColor() {
        return switch (getVibeIndex()) {
            case 1 -> 0xFF60A5FA;
            case 2 -> 0xFFD97706;
            case 3 -> 0xFF94A3B8;
            default -> 0xFFDCE8F5;
        };
    }

    public int getCustomBlockColor(BlockState state, BlockGetter world, BlockPos pos, int tintIndex) {
        if (state == null) return 0;
        int vibe = getVibeIndex();

        Block block = state.getBlock();
        if (block instanceof LeavesBlock || block instanceof BushBlock
                || block instanceof VineBlock || block instanceof CarpetBlock
                || block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            return getFoliageColor();
        }

        if (block instanceof GrassBlock) {
            return getGrassColor();
        }

        if (block instanceof LiquidBlock) {
            return getWaterColor();
        }

        if (vibe == 0) {
            return 0xFFFFFFFF;
        } else if (vibe == 2) {
            return 0xFFF5E6D3;
        }

        return 0;
    }

    @EventTarget
    public void onWorldJoin(WorldJoinEvent e) {
        clearState();
    }

    @EventTarget
    public void onPlayerTick(PlayerTickEvent event) {
        if (!isState()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        String currentVibe = this.vibeMode.getValue();
        if (!currentVibe.equals(this.lastVibe)) {
            this.lastVibe = currentVibe;
            clearState();
            reloadWorldRender();
        }

        if (!this.particles.getValue()) {
            return;
        }

        LocalPlayer player = mc.player;
        int maxParticles = this.particleAmount.getValue().intValue();
        int vibe = getVibeIndex();

        if (this.ambientParticles.size() < maxParticles) {
            int toSpawn = Math.min(10, maxParticles - this.ambientParticles.size());

            for (int i = 0; i < toSpawn; i++) {
                double angle = Math.random() * Math.PI * 2.0;
                double dist = 1.0 + Math.random() * 20.0;
                double px = player.getX() + Math.cos(angle) * dist;
                double pz = player.getZ() + Math.sin(angle) * dist;

                Vec3 pos;
                Vec3 vel;
                int color;
                float size;
                boolean isRain = false;

                switch (vibe) {
                    case 1 -> { // Summer
                        double py = player.getY() + rnd(-1.0, 16.0);
                        pos = new Vec3(px, py, pz);
                        vel = new Vec3(rnd(-0.015, 0.015), rnd(0.01, 0.03), rnd(-0.015, 0.015));
                        color = ColorUtil.rgba(255, 235, 140, 220);
                        size = 0.08F + (float) Math.random() * 0.06F;
                    }
                    case 2 -> { // Autumn
                        double py = player.getY() + rnd(-1.0, 16.0);
                        pos = new Vec3(px, py, pz);
                        vel = new Vec3(rnd(-0.03, 0.03) + 0.02, rnd(-0.05, -0.02), rnd(-0.03, 0.03));
                        int[] autumnCols = {0xFFEA580C, 0xFFF59E0B, 0xFFDC2626, 0xFFD97706};
                        color = autumnCols[(int) (Math.random() * autumnCols.length)];
                        size = 0.12F + (float) Math.random() * 0.08F;
                    }
                    case 3 -> { // Spring (Raindrops falling to ground floor with splashes)
                        double py = player.getY() + rnd(6.0, 18.0);
                        pos = new Vec3(px, py, pz);
                        vel = new Vec3(rnd(-0.04, -0.01), rnd(-0.6, -0.35), rnd(-0.02, 0.02));
                        color = ColorUtil.rgba(147, 197, 253, 230);
                        size = 0.05F + (float) Math.random() * 0.04F;
                        isRain = true;
                    }
                    default -> { // Winter
                        double py = player.getY() + rnd(1.0, 18.0);
                        pos = new Vec3(px, py, pz);
                        vel = new Vec3(rnd(-0.04, 0.01), rnd(-0.08, -0.03), rnd(-0.03, 0.03));
                        color = ColorUtil.rgba(240, 248, 255, 240);
                        size = 0.07F + (float) Math.random() * 0.06F;
                    }
                }

                this.ambientParticles.add(new AmbientParticle(pos, vel, color, size, isRain));
            }
        }

        double pX = player.getX();
        double pY = player.getY();
        double pZ = player.getZ();

        for (int i = this.ambientParticles.size() - 1; i >= 0; i--) {
            AmbientParticle p = this.ambientParticles.get(i);
            p.pos = p.pos.add(p.vel);
            p.rotation += p.rotationSpeed;

            if (p.isRain) {
                int gx = Mth.floor(p.pos.x);
                int gz = Mth.floor(p.pos.z);
                int groundY = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, gx, gz);

                if (p.pos.y <= groundY + 0.1) {
                    if (Math.random() < 0.8) {
                        this.groundRipples.add(new GroundRipple(new Vec3(p.pos.x, groundY + 0.02, p.pos.z), p.color));
                    }
                    this.ambientParticles.remove(i);
                    continue;
                }
            }

            if (p.pos.distanceToSqr(pX, pY, pZ) > 900.0 || p.pos.y < pY - 8.0) {
                this.ambientParticles.remove(i);
            }
        }

        for (int i = this.groundRipples.size() - 1; i >= 0; i--) {
            GroundRipple r = this.groundRipples.get(i);
            r.age++;
            r.size += 0.035F;
            r.alpha -= 0.045F;
            if (r.alpha <= 0.0F || r.age > r.maxAge) {
                this.groundRipples.remove(i);
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isState()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
    }

    private static double rnd(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private static class AmbientParticle {
        Vec3 pos;
        Vec3 vel;
        int color;
        float size;
        float rotation;
        float rotationSpeed;
        boolean isRain;

        AmbientParticle(Vec3 pos, Vec3 vel, int color, float size, boolean isRain) {
            this.pos = pos;
            this.vel = vel;
            this.color = color;
            this.size = size;
            this.rotation = (float) (Math.random() * 360.0);
            this.rotationSpeed = (float) (Math.random() * 4.0 - 2.0);
            this.isRain = isRain;
        }
    }

    private static class GroundRipple {
        Vec3 pos;
        int color;
        float size = 0.1F;
        float alpha = 0.8F;
        int age = 0;
        int maxAge = 18;

        GroundRipple(Vec3 pos, int color) {
            this.pos = pos;
            this.color = color;
        }
    }
}
