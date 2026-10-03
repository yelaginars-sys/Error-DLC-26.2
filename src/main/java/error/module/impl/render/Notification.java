package error.module.impl.render;

import error.module.Category;
import error.module.Module;

public class Notification extends Module {
    public static Notification INSTANCE;

    public Notification() {
        super("Notification", "Выводит всплывающие уведомления вместо Dynamic Island", Category.RENDER);
        INSTANCE = this;
    }
}
