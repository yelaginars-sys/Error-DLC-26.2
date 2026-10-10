package error.module.impl.render;

import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import error.Client;
import error.module.Category;
import error.module.Module;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.render.AtmospherePostPipeline;

/**
 * Atmosphere Module ported from System.
 * Modes: Рассвет (Dawn), Пыль (Dust), Снег (Snow), Угли (Embers).
 * Renders volumetric sky, haze, god rays, atmospheric fog, and 3D particulates.
 */
public class Atmosphere extends Module {
    private static Atmosphere instance;
    public static Atmosphere INSTANCE;

    public static Atmosphere getInstance() {
        return instance != null ? instance : (Client.INSTANCE != null && Client.INSTANCE.moduleManager != null
                ? Client.INSTANCE.moduleManager.getModule(Atmosphere.class) : null);
    }

    public final ModeSetting mode = mode("Режим", "Рассвет", "Рассвет", "Пыль", "Снег", "Угли");
    public final SliderSetting density = slider("Плотность", 0.35f, 0.05f, 0.8f, 0.01f)
            .visible(() -> !isMood());
    public final SliderSetting scatterHeight = slider("Высота рассеивания", 76f, 60f, 120f, 1f)
            .visible(() -> !isMood());
    public final SliderSetting godRays = slider("Солнечные лучи", 75f, 0f, 100f, 1f)
            .visible(() -> !isMood());
    public final SliderSetting softness = slider("Мягкость", 60f, 0f, 100f, 1f)
            .visible(() -> !isMood());
    public final ColorSetting dawnColor = color("Цвет рассвета", 0xffffad7a)
            .visible(() -> !isMood());
    public final SliderSetting amount = slider("Частицы", 50f, 0f, 100f, 1f)
            .visible(this::isMood);
    public final SliderSetting speed = slider("Скорость", 100f, 10f, 300f, 5f)
            .visible(this::isMood);
    public final SliderSetting haze = slider("Дымка", 50f, 0f, 100f, 1f)
            .visible(this::isMood);
    public final SliderSetting grade = slider("Тонировка", 70f, 0f, 100f, 1f)
            .visible(this::isMood);
    public final SliderSetting vignette = slider("Виньетка", 40f, 0f, 100f, 1f)
            .visible(this::isMood);

    public Atmosphere() {
        super("Atmosphere", "Рассвет, пыль, снег и угли", Category.RENDER);
        instance = this;
        INSTANCE = this;
    }

    public int modeIndex() {
        int idx = mode.getModes().indexOf(mode.getValue());
        return Math.max(0, idx);
    }

    public boolean isMood() {
        return modeIndex() != 0;
    }

    @Override
    public void onEnable() {
        AtmospherePostPipeline.getInstance().reset();
    }

    @Override
    public void onDisable() {
        AtmospherePostPipeline.getInstance().close();
    }

    public void render(Camera camera, Matrix4f view, Matrix4f projection) {
        if (isEnabled()) {
            AtmospherePostPipeline.getInstance().render(camera, view, projection);
        }
    }

    /** Native values; semantic color names are inferred for the independent renderer. */
    public VisualState parameters(long worldTime) {
        int index = modeIndex();
        if (index == 0) {
            int argb = dawnColor.getValue();
            float[] warm = {(argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f};
            return new VisualState(0,
                mixLms(new float[]{.16f, .19f, .38f}, warm, .14f),
                scaled(warm, 1.12f, .88f, .62f), new float[]{.56f, .62f, .8f},
                mixLms(warm, new float[]{.95f, .55f, .63f}, .42f),
                new float[]{.6f, .67f, .82f}, scaled(warm, 1.08f, .94f, .72f),
                sunDirection(worldTime, .11f),
                clamp(density.getValue(), .05f, .8f), scatterHeight.getValue(),
                percent(godRays), percent(softness), 0, 1, 0, 0, 0);
        }
        int p = index - 1;
        return new VisualState(index, NativePalettes.rgb(p, 4), NativePalettes.rgb(p, 0x10),
            NativePalettes.rgb(p, 0x28), NativePalettes.rgb(p, 0x34),
            NativePalettes.rgb(p, 0x40), NativePalettes.rgb(p, 0x4c),
            sunDirection(worldTime, NativePalettes.value(p, 0x88)),
            0, 76, NativePalettes.sunEnabled(p) ? NativePalettes.value(p, 0x68) : 0, .35f,
            percent(amount), speed.getValue() / 100f,
            percent(haze) * NativePalettes.value(p, 0x8c) * 2, percent(grade), percent(vignette));
    }

    public record VisualState(int mode, float[] skyTop, float[] skyHorizon,
        float[] fogCool, float[] fogWarm, float[] shadow, float[] sunColor, float[] sunDirection,
        float density, float scatterHeight, float godRays, float softness,
        float amount, float speed, float haze, float grade, float vignette) {}

    /** Native RVA 0xedb20: day-time sign switches a fixed-height sun east/west. */
    public static float[] sunDirection(long worldTime, float elevation) {
        double cycle = Math.floorMod(worldTime, 24000L) / 24000.0 - .25;
        cycle -= Math.floor(cycle);
        double angle = (2 * cycle + .5 - Math.cos(cycle * Math.PI) * .5) / 3 * 2 * Math.PI;
        float sign = -(float) Math.sin((float) angle) < 0 ? -1 : 1;
        return new float[]{sign * (float) Math.cos(elevation), (float) Math.sin(elevation), 0};
    }

    /** Native RVA 0xed640: interpolate cube-root LMS, then return sRGB. */
    public static float[] mixLms(float[] a, float[] b, float weight) {
        float[] x = toLms(a), y = toLms(b);
        float t = clamp(weight, 0, 1);
        for (int i = 0; i < 3; i++) {
            float value = x[i] + (y[i] - x[i]) * t;
            x[i] = value * value * value;
        }
        return new float[]{
            srgb(4.0767417f*x[0] - 3.3077116f*x[1] + .23096994f*x[2]),
            srgb(-1.268438f*x[0] + 2.6097574f*x[1] - .34131938f*x[2]),
            srgb(-.0041960864f*x[0] - .70341861f*x[1] + 1.7076147f*x[2])};
    }

    private static float[] toLms(float[] color) {
        float r = linear(color[0]), g = linear(color[1]), b = linear(color[2]);
        return new float[]{
            (float) Math.cbrt(.41222147f*r + .53633255f*g + .051445995f*b),
            (float) Math.cbrt(.2119035f*r + .6806995f*g + .10739696f*b),
            (float) Math.cbrt(.08830246f*r + .28171885f*g + .6299787f*b)};
    }

    private static float linear(float v) {
        return v <= .04045f ? v / 12.92f : (float) Math.pow((v + .055f) / 1.055f, 2.4);
    }

    private static float srgb(float v) {
        v = clamp(v, 0, 1);
        return v <= .0031308f ? v * 12.92f : (float) (1.055 * Math.pow(v, 1 / 2.4) - .055);
    }

    private static float[] scaled(float[] a, float r, float g, float b) {
        return new float[]{clamp(a[0]*r,0,1), clamp(a[1]*g,0,1), clamp(a[2]*b,0,1)};
    }

    private static float percent(SliderSetting s) {
        return clamp(s.getValue() / 100f, 0, 1);
    }

    public static float clamp(float v, float low, float high) {
        return Float.isNaN(v) ? low : Math.max(low, Math.min(high, v));
    }

    /** Exact 0xac-byte palette records extracted from ClientGUI.dll, RVA 0x42f8e0.
     * Field meanings are partly inferred. Word 0 is an integer; word 36 is a flag.
     * This is original data, not the original shader implementation.
     */
    private static final class NativePalettes {
        private NativePalettes() {}
        private static final int[][] WORDS = {
            {0x00000000, 0x3f000000, 0x3f23d70a, 0x3f6147ae, 0x3f570a3d, 0x3f4ccccd, 0x3f333333, 0x3f800000, 0x3f6e147b, 0x3f400000, 0x3f666666, 0x3f570a3d, 0x3f3851ec, 0x3f800000, 0x3f733333, 0x3f5c28f6, 0x3e99999a, 0x3e851eb8, 0x3e75c28f, 0x3f800000, 0x3f75c28f, 0x3f6147ae, 0x3f828f5c, 0x3f866666, 0x3f733333, 0x00000000, 0x3f666666, 0x3e99999a, 0x3eb33333, 0x3e19999a, 0x00000000, 0x3f800000, 0x3f800000, 0x3f800000, 0x3ed70a3d, 0x3eb33333, 0x00000001, 0x3e99999a, 0x3e6b851f, 0x3e051eb8, 0x3f59999a, 0x3f333333, 0x00000000},
            {0x00000001, 0x3f19999a, 0x3f2b851f, 0x3f47ae14, 0x3f5c28f6, 0x3f6147ae, 0x3f6e147b, 0x00000000, 0x00000000, 0x00000000, 0x3f570a3d, 0x3f6147ae, 0x3f70a3d7, 0x3f800000, 0x3f800000, 0x3f800000, 0x3e6147ae, 0x3e8f5c29, 0x3ecccccd, 0x3f733333, 0x3f7851ec, 0x3f800000, 0x3f800000, 0x3f733333, 0x3f400000, 0x00000000, 0x00000000, 0x00000000, 0x3f666666, 0x00000000, 0x00000000, 0x3f800000, 0x3f800000, 0x3f800000, 0x00000000, 0x3f0ccccd, 0x00000000, 0x3e23d70a, 0x3e3851ec, 0x3e75c28f, 0x3f800000, 0x3f266666, 0x3f800000},
            {0x00000002, 0x3d75c28f, 0x3d23d70a, 0x3d23d70a, 0x3f0ccccd, 0x3e6147ae, 0x3da3d70a, 0x3f800000, 0x3f000000, 0x3e4ccccd, 0x3e8f5c29, 0x3e23d70a, 0x3dcccccd, 0x3f800000, 0x3f0ccccd, 0x3e3851ec, 0x3e23d70a, 0x3d75c28f, 0x3d23d70a, 0x3f800000, 0x3f1eb852, 0x3e99999a, 0x3f4ccccd, 0x3f99999a, 0x3f800000, 0x3ca3d70a, 0x3eb33333, 0x3f19999a, 0x3f800000, 0x3ecccccd, 0x3f333333, 0x3f800000, 0x3f19999a, 0x3e99999a, 0x3df5c28f, 0x3f19999a, 0x00000001, 0x3e99999a, 0x3df5c28f, 0x3d23d70a, 0x3f333333, 0x3f333333, 0x00000000}
        };

        public static float value(int palette, int byteOffset) {
            if (palette < 0 || palette > 2 || byteOffset < 0 || byteOffset >= 0xac || byteOffset % 4 != 0)
                throw new IllegalArgumentException("Invalid native palette offset");
            return Float.intBitsToFloat(WORDS[palette][byteOffset / 4]);
        }

        public static float[] rgb(int palette, int byteOffset) {
            return new float[]{value(palette, byteOffset), value(palette, byteOffset + 4), value(palette, byteOffset + 8)};
        }

        public static boolean sunEnabled(int palette) {
            return WORDS[palette][0x90 / 4] != 0;
        }
    }
}
