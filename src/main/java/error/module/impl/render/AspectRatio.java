package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.SliderSetting;

public final class AspectRatio extends Module {
    public static AspectRatio INSTANCE;

    public final SliderSetting ratio = slider("Значение", 1.0F, 0.5F, 2.5F, 0.05F);

    public AspectRatio() {
        super("AspectRatio", "Растягивает картинку мира по горизонтали", Category.RENDER);
        INSTANCE = this;
    }

    public float getRatio() {
        return isEnabled() ? ratio.getValue() : 1.0F;
    }
}
