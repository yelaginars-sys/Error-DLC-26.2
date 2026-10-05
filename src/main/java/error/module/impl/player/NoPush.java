package error.module.impl.player;

import error.module.Category;
import error.module.Module;
import error.setting.impl.MultiModeSetting;

import java.util.List;

public class NoPush extends Module {
    public static NoPush INSTANCE;

    public final MultiModeSetting collisions = multiMode("Коллизия", List.of("Блоки", "Вода", "Удочка", "Игроки"), "Блоки", "Вода", "Удочка", "Игроки");

    public NoPush() {
        super("NoPush", "Отключает коллизию и толкание", Category.PLAYER);
        INSTANCE = this;
    }
}
