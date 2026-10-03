package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.HeaderSetting;
import error.setting.impl.SliderSetting;

/**
 */
public final class ViewModel extends Module {

    public static ViewModel INSTANCE;
    private final HeaderSetting as = header("Левая");
    public final SliderSetting leftX = slider("Левая рука X", 0.0F, -2.0F, 2.0F, 0.05F);
    public final SliderSetting leftY = slider("Левая рука Y", 0.0F, -2.0F, 2.0F, 0.05F);
    public final SliderSetting leftZ = slider("Левая рука Z", 0.0F, -2.0F, 2.0F, 0.05F);
    public final SliderSetting leftScale = slider("Размер левой руки", 1.0F, 0.1F, 2.5F, 0.05F);
    private final HeaderSetting ass = header("Правая");
    public final SliderSetting rightX = slider("Правая рука X", 0.0F, -2.0F, 2.0F, 0.05F);
    public final SliderSetting rightY = slider("Правая рука Y", 0.0F, -2.0F, 2.0F, 0.05F);
    public final SliderSetting rightZ = slider("Правая рука Z", 0.0F, -2.0F, 2.0F, 0.05F);
    public final SliderSetting rightScale = slider("Размер правой руки", 1.0F, 0.1F, 2.5F, 0.05F);

    public ViewModel() {
        super("ViewModel", "Кастомизация смещение и размер рук", Category.RENDER);
        INSTANCE = this;
    }
}