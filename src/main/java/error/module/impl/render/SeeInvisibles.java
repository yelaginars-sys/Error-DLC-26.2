package error.module.impl.render;

import error.module.Category;
import error.module.Module;
import error.setting.impl.SliderSetting;

public class SeeInvisibles extends Module {
    public static SeeInvisibles INSTANCE;

    public final SliderSetting alpha = slider("Прозрачность", 0.6F, 0.0F, 1.0F, 0.05F);

    public SeeInvisibles() {
        super("SeeInvisibles", "Позволяет видеть невидимых существ и игроков", Category.RENDER);
        INSTANCE = this;
    }
}
