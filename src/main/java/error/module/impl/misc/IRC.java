package error.module.impl.misc;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;

public final class IRC extends Module {
    public static IRC INSTANCE;

    public final CheckBox syncCosmetics = checkbox("Синхронизация косметики", true);
    public final CheckBox showUnfriended = checkbox("Показывать не друзьям", true);

    public IRC() {
        super("IRC", "Чат и передача выбранной косметики между пользователями клиента", Category.MISC);
        INSTANCE = this;
    }
}
