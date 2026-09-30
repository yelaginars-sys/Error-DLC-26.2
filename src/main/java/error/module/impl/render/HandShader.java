package error.module.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import error.module.Category;
import error.module.Module;
import error.setting.impl.*;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.ItemColorUtil;
import error.util.client.clients.Theme;
import error.util.render.world.module.HandShaderRenderer;

import java.util.List;

/**
 * Create by daun kvass
 */
public final class HandShader extends Module {

    public static final String FILL = "Fill";
    public static final String OUTLINE = "OutLine";
    public static final String GLOW = "Fire";

    public static final String PLASMA = "Transfusion";
    public static final String GLASS = "Glass";
    public static final String SOLID = "Solid";
    public static final String NOISE = "Noise";
    public static final String HOLOGRAM = "HoloGram";

    public static final String FLAME_CALM = "Calm";
    public static final String FLAME_UP = "Up";
    public static final String FLAME_DOWN = "Down";

    private final HeaderSetting asda = header("Main");
    public final MultiModeSetting modes = multiMode(
            "Эффекты",
            List.of(FILL, GLOW),
            FILL, OUTLINE, GLOW
    );

    public final ModeSetting fillType = mode("Type Fill", PLASMA,
            PLASMA, GLASS, SOLID, NOISE, HOLOGRAM
    ).visible(() -> modes.isEnabled(FILL));

    public final CheckBox themeColor = checkbox("Color from Theme", true);
    public final ColorSetting color = color("Color", ColorUtil.rgba(255, 120, 20, 255)).visible(() -> !themeColor.getValue());
    public final SliderSetting speeds = slider("Speed Fill", 1.4f, 0.1f, 5.0f, 0.1f).visible(() -> modes.isEnabled(FILL));

    private final HeaderSetting ass = header("Fire");
    public final CheckBox itemColor = checkbox("Color fire from item", true).visible(() -> modes.isEnabled(GLOW));
    public final CheckBox fireTrail = checkbox("Fire trail", true).visible(() -> modes.isEnabled(GLOW));
    public final ModeSetting flameDirection = mode("Behavior fire", FLAME_CALM,
            FLAME_CALM, FLAME_UP, FLAME_DOWN
    ).visible(() -> modes.isEnabled(GLOW) && fireTrail.getValue());
    public final SliderSetting flameSpeed = slider("Speed fire", 1.4f, 0.1f, 5.0f, 0.1f).visible(() -> modes.isEnabled(GLOW));
    public final SliderSetting flameTrail = slider("Length fire", 0.91f, 0.50f, 0.98f, 0.01f).visible(() -> modes.isEnabled(GLOW));
    public final SliderSetting glowRadius = slider("Radius fire", 20.0f, 2.0f, 50.0f, 1.0f).visible(() -> modes.isEnabled(GLOW));
    public final SliderSetting glowStrength = slider("Brightness fire", 1.2f, 0.1f, 3.0f, 0.05f).visible(() -> modes.isEnabled(GLOW));
    public final SliderSetting blurPasses = slider("Quality blur", 2.0f, 1.0f, 4.0f, 1.0f).visible(() -> modes.isEnabled(GLOW));

    private final HeaderSetting asd = header("Other");
    public final SliderSetting fillOpacity = slider("Transparency", 0.40f, 0.0f, 1.0f, 0.02f);
    public final SliderSetting outlineThickness = slider("Thickness outline", 1.5f, 0.5f, 5.0f, 0.1f).visible(() -> modes.isEnabled(OUTLINE));
    public final CheckBox bothHands = checkbox("Both Hand", true);

    private final HandShaderRenderer renderer = new HandShaderRenderer();

    public HandShader() {
        super("HandShader", "Пупу ww завоз", Category.RENDER);
    }

    public HandShaderRenderer getRenderer() {
        return this.renderer;
    }

    public int getBaseColor() {
        if (this.themeColor.getValue()) {
            return Theme.getAccentColor();
        }
        return this.color.getValue();
    }

    public int getLeftGlowColor() {
        int fallback = getBaseColor();
        if (!this.itemColor.getValue()) return fallback;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return fallback;

        boolean isMainRight = mc.player.getMainArm() == HumanoidArm.RIGHT;
        ItemStack leftStack = isMainRight ? mc.player.getItemInHand(InteractionHand.OFF_HAND) : mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        return ItemColorUtil.getItemColor(leftStack, fallback);
    }

    public int getRightGlowColor() {
        int fallback = getBaseColor();
        if (!this.itemColor.getValue()) return fallback;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return fallback;

        boolean isMainRight = mc.player.getMainArm() == HumanoidArm.RIGHT;
        ItemStack rightStack = isMainRight ? mc.player.getItemInHand(InteractionHand.MAIN_HAND) : mc.player.getItemInHand(InteractionHand.OFF_HAND);
        return ItemColorUtil.getItemColor(rightStack, fallback);
    }

    public boolean hasFill() {
        return this.modes.isEnabled(FILL);
    }

    public boolean hasOutline() {
        return this.modes.isEnabled(OUTLINE);
    }

    public boolean hasGlow() {
        return this.modes.isEnabled(GLOW);
    }

    public boolean hasFlame() {
        return hasGlow() && this.fireTrail.getValue();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.renderer.clearHistory();
    }
}