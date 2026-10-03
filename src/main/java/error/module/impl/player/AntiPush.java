package error.module.impl.player;

import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.MultiModeSetting;

/**
 */
public final class AntiPush extends Module {

    public static AntiPush INSTANCE;
    public final MultiModeSetting modes = multiMode("No push","Entity","Blocks","Water");

    public AntiPush() {
        super("AntiPush", "Убирает толкание от чего-то", Category.PLAYER);
        INSTANCE = this;
    }

}