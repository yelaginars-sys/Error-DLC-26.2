package error.module.impl.render;

import error.event.EventTarget;
import error.event.list.Render3DEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.List;

public class Chams extends Module {

    public static Chams INSTANCE;

    public static final String TARGET_PLAYERS = "Игроков";
    public static final String TARGET_FRIENDS = "Друзей";
    public static final String TARGET_SELF = "Себя";

    private static final int DEFAULT_FILL_ALPHA = 130;
    private static final float CLIENT_FILL_SATURATION = 1.18f;
    private static final float CLIENT_FILL_BRIGHTNESS = 1.12f;
    private static final float CLIENT_OUTLINE_SATURATION = 1.12f;
    private static final float CLIENT_OUTLINE_BRIGHTNESS = 1.08f;
    private static final float MIN_PULSE_ALPHA = 0.65f;
    private static final float PULSE_SWING = 0.35f;
    private static final int FRIEND_FILL_COLOR = new Color(85, 255, 85, 60).getRGB();
    private static final int FRIEND_OUTLINE_COLOR = new Color(100, 255, 100, 255).getRGB();

    public final MultiModeSetting rendering = multiMode("Отображать", List.of(TARGET_PLAYERS, TARGET_FRIENDS), TARGET_PLAYERS, TARGET_FRIENDS, TARGET_SELF);

    public final CheckBox waves = checkbox("Волны", true);
    public final SliderSetting waveSpeedX = slider("Скорость X", 0.22f, 0.0f, 1.5f, 0.01f);
    public final SliderSetting waveSpeedY = slider("Скорость Y", 0.15f, 0.0f, 1.5f, 0.01f);
    public final SliderSetting waveScale = slider("Размер волн", 1.35f, 0.2f, 4.0f, 0.05f);
    public final SliderSetting waveDensity = slider("Плотность волн", 1.15f, 0.5f, 3.0f, 0.05f);
    public final SliderSetting waveGlow = slider("Сила волн", 1.0f, 0.2f, 3.0f, 0.05f);

    public final CheckBox glow = checkbox("Свечение", true);
    public final SliderSetting glowIntensity = slider("Сила свечения", 2.0f, 1.0f, 5.0f, 0.1f);
    public final SliderSetting glowLayers = slider("Слои свечения", 3.0f, 1.0f, 6.0f, 1.0f);

    public final CheckBox pulse = checkbox("Пульсирование", false);
    public final SliderSetting pulseSpeed = slider("Скорость пульсации", 2.0f, 0.5f, 5.0f, 0.1f);

    public final CheckBox hideOriginal = checkbox("Скрыть оригинал", false);
    public final CheckBox hideItemsAndCape = checkbox("Скрывать предметы и плащ", false);

    private final long startTime = System.currentTimeMillis();

    public Chams() {
        super("Chams", "Подсветка сущностей сквозь стены текстурой", Category.RENDER);
        INSTANCE = this;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isState() || mc.level == null || mc.player == null) {
            return;
        }

        for (Player player : mc.level.players()) {
            if (!affects(player)) {
                continue;
            }
            if (player == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
                continue;
            }
        }
    }

    public boolean affects(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (!isState() || player == null || !player.isAlive()) {
            return false;
        }
        if (player == mc.player) {
            return rendering.isEnabled(TARGET_SELF) && mc.options.getCameraType() != CameraType.FIRST_PERSON;
        }
        if (isFriend(player)) {
            return rendering.isEnabled(TARGET_FRIENDS);
        }
        return rendering.isEnabled(TARGET_PLAYERS);
    }

    public boolean shouldHideBaseModel(Player player) {
        return hideOriginal.get() && affects(player);
    }

    public boolean shouldHideItemsAndCape(Player player) {
        return hideItemsAndCape.get() && affects(player);
    }

    public int resolveFillColor(Player player) {
        return applyPulse(baseFillColor(player));
    }

    public int resolveOutlineColor(Player player) {
        return applyPulse(baseOutlineColor(player));
    }

    private int baseFillColor(Player player) {
        if (isFriend(player)) {
            return FRIEND_FILL_COLOR;
        }
        return vividWithAlpha(ColorUtil.rgba(0, 180, 255, 255), CLIENT_FILL_SATURATION, CLIENT_FILL_BRIGHTNESS, DEFAULT_FILL_ALPHA);
    }

    private int baseOutlineColor(Player player) {
        if (isFriend(player)) {
            return FRIEND_OUTLINE_COLOR;
        }
        return vividWithAlpha(ColorUtil.rgba(0, 180, 255, 255), CLIENT_OUTLINE_SATURATION, CLIENT_OUTLINE_BRIGHTNESS, 255);
    }

    private int applyPulse(int color) {
        if (!pulse.get()) {
            return color;
        }
        float elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0f;
        float pulseValue = (float) ((Math.sin(elapsedSeconds * pulseSpeed.getValue() * Math.PI) + 1.0) * 0.5);
        float alphaMul = MIN_PULSE_ALPHA + PULSE_SWING * pulseValue;
        return ColorUtil.withAlpha(color, Math.round(ColorUtil.alpha(color) * alphaMul));
    }

    private int vividWithAlpha(int color, float saturationBoost, float brightnessBoost, int alpha) {
        float[] hsb = Color.RGBtoHSB(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), null);
        float saturation = Mth.clamp(hsb[1] * saturationBoost, 0.0f, 1.0f);
        float brightness = Mth.clamp(Math.max(hsb[2], 0.8f) * brightnessBoost, 0.0f, 1.0f);
        int rgb = Color.HSBtoRGB(hsb[0], saturation, brightness);
        return ColorUtil.rgba((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }

    private boolean isFriend(Player player) {
        return FriendManager.getInstance().isFriend(player.getName().getString());
    }
}
