package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;

/**
 */
public final class BlockHighlight extends Module {
    public static final String VARIANT_CAUSTICS = "Water Caustics";
    public static final String VARIANT_GLOSSY = "Glossy Gradients";
    public static final String VARIANT_DEEP_SPACE = "Deep Space";

    public final ModeSetting modes = mode("Shader", VARIANT_CAUSTICS, VARIANT_GLOSSY, VARIANT_DEEP_SPACE,VARIANT_CAUSTICS).visible(this::enables);
    public final CheckBox themeColor = checkbox("Color from Theme", true).visible(this::enables);
    public final ColorSetting customColor = color("Color", ColorUtil.rgba(140, 170, 255, 255)).visible(() -> enables() && !this.themeColor.getValue());
    public final CheckBox ignoreDepth = checkbox("Ignore Wall", false).visible(this::enables);

    public final SliderSetting animationSpeed = slider("Speed Animation", 12.0F, 1.0F, 30.0F, 0.5F).visible(this::enables);
    public final SliderSetting shaderSpeed = slider("Speed shader", 1.0F, 0.1F, 3.0F, 0.05F).visible(this::enables);
    public final SliderSetting shaderIntensity = slider("Intensity", 1.4F, 0.1F, 3.0F, 0.05F).visible(this::enables);

    public BlockHighlight() {
        super("BlockHighLight", "Шейдоровое Свечение блоков", Category.RENDER);
    }

    public boolean enables() {
        return this.isState();
    }

    public int getColors() {
        return this.themeColor.getValue() ? Theme.getAccentColor() : this.customColor.getValue();
    }
}