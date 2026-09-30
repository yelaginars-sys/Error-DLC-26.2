package error.module.impl.misc;

import error.module.Category;
import error.module.Module;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;

public class ClientSounds extends Module {
    public static ClientSounds INSTANCE;
    public final ModeSetting stateSounds = mode("Режим", "Третий", "Нет", "Первый", "Второй", "Третий", "Четвертый", "Пятый", "Шестой");
    public final SliderSetting volume = slider("Громкость", 50.0F, 1.0F, 100.0F, 0.5F);

    public ClientSounds() {
        super("ClientSounds", "Добавляет звуки действия и взаимодействия с клиентом", Category.MISC);
        INSTANCE = this;
        setEnabled(true);
    }
}
