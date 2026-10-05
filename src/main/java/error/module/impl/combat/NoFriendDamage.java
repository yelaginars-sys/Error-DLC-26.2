package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.AttackEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import net.minecraft.world.entity.player.Player;

/**
 * NoFriendDamage module ported from Energy client.
 * Prevents attacking friends registered in FriendManager.
 */
public class NoFriendDamage extends Module {

    public NoFriendDamage() {
        super("NoFriendDamage", "Не дает атаковать друзей", Category.COMBAT);
    }

    @EventTarget(priority = 100)
    public void onAttack(AttackEvent event) {
        if (event.getTarget() instanceof Player player) {
            if (FriendManager.getInstance().isFriend(player)) {
                event.cancel();
            }
        }
    }
}
