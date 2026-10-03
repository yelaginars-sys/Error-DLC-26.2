package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;

public final class AutoCaptcha extends Module {
    public static AutoCaptcha INSTANCE;

    public final CheckBox notifyUser = checkbox("Уведомление", true);
    public final CheckBox autoSubmit = checkbox("Авто-ввод", true);

    public AutoCaptcha() {
        super("AutoCaptcha", "Автоматическое решение графических капч и ввод ответа", Category.MISC);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (mc.player == null || mc.level == null) return;
        // Captcha OCR / Pattern Solver logic
    }
}
