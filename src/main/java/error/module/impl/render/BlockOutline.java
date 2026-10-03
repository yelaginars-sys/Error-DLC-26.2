package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ColorSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

public final class BlockOutline extends Module {
    public static BlockOutline INSTANCE;

    public final ModeSetting shaderMode = mode("Шейдер", "Частицы", "Частицы", "Облака", "Узор", "Octgrams");
    public final SliderSetting particlesForce = slider("Частицы", 1.0F, 0.0F, 1.5F, 0.05F);
    public final SliderSetting animSpeed = slider("Скорость", 1.0F, 0.1F, 3.0F, 0.1F);
    public final CheckBox fullBlock = checkbox("Фулл блок", false);
    public final ColorSetting color1 = color("Цвет 1", 0xFF8050FF);
    public final ColorSetting color2 = color("Цвет 2", 0xFFFF5080);

    public BlockOutline() {
        super("BlockOutline", "Кастомная анимированная обводка выбранного блока", Category.RENDER);
        INSTANCE = this;
    }
}
