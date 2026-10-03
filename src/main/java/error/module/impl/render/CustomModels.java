package error.module.impl.render;

import error.Client;
import error.cosmetic.IrcCosmetics;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

public class CustomModels extends Module {

    public static CustomModels INSTANCE;

    public static final String RABBIT = "Rabbit";
    public static final String CHICKEN = "Chicken";
    public static final String VERITY = "Verity";
    public static final String AMOGUS = "Amogus";
    public static final String FREDDY = "Freddy";
    public static final String RED_DEMON = "RedDemon";
    public static final String WHITE_DEMON = "WhiteDemon";
    public static final String NONE = "None";

    public final ModeSetting model = mode("Модель", NONE, NONE, RABBIT, CHICKEN, VERITY, AMOGUS, FREDDY, RED_DEMON, WHITE_DEMON);
    public final CheckBox self = checkbox("Себя", true);
    public final CheckBox friends = checkbox("Друзей", true);
    public final CheckBox ircUsers = checkbox("IRC Игроков", true);
    public final CheckBox others = checkbox("Остальных", false);
    public final CheckBox hand = checkbox("Модель руки", true);
    public final SliderSetting size = slider("Размер", 1.0f, 0.3f, 3.0f, 0.05f);
    public final ModeSetting face = mode("Лицо Verity", "verity",
            "verity", "veritytalk", "veritydisturbing", "veritydisturbingtalk",
            "veritydisturbingyell", "veritybad", "verityverybad");

    public CustomModels() {
        super("CustomModels", "Отображение кастомных 3D моделей игроков и синхра по IRC", Category.COSMETICS);
        INSTANCE = this;
    }

    public boolean handFromModel() {
        return isState() && CHICKEN.equalsIgnoreCase(model.getValue()) && self.getValue() && hand.getValue();
    }

    public String modelFor(Avatar avatar) {
        if (!isState() || avatar == null || mc.player == null) {
            return null;
        }

        // 1. Check local player
        if (avatar.getUUID().equals(mc.player.getUUID())) {
            return self.getValue() && !NONE.equalsIgnoreCase(model.getValue()) ? model.getValue() : null;
        }

        // 2. Check IRC Cosmetics Sync (works even if not in friends list!)
        if (ircUsers.getValue()) {
            String ircModel = IrcCosmetics.getModel(avatar.getUUID());
            if (ircModel != null && !NONE.equalsIgnoreCase(ircModel)) {
                return ircModel;
            }
        }

        // 3. Check Friends
        if (avatar instanceof Player player && Client.INSTANCE.friendManager.isFriend(player.getGameProfile().name())) {
            return friends.getValue() && !NONE.equalsIgnoreCase(model.getValue()) ? model.getValue() : null;
        }

        // 4. Check Others
        return others.getValue() && !NONE.equalsIgnoreCase(model.getValue()) ? model.getValue() : null;
    }

    public float size() {
        return size.getValue();
    }

    public Identifier verityTexture() {
        String f = face.getValue();
        return Identifier.fromNamespaceAndPath("error", "models/verity/" + f + ".png");
    }
}
