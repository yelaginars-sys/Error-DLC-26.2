package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.SliderSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/**
 * BowSpammer module.
 * Fast bow drawing and releasing cycle.
 */
public class BowSpammer extends Module {

    public final SliderSetting speed = slider("Скорость", 250.0f, 1.0f, 1000.0f, 1.0f);

    private boolean pendingRelease = false;
    private InteractionHand activeHand = null;
    private long lastShotTime = 0L;

    public BowSpammer() {
        super("BowSpammer", "Быстрый цикл отпускания и натяжки лука", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        pendingRelease = false;
        activeHand = null;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.gameMode == null) return;

        if (pendingRelease) {
            if (activeHand != null) {
                mc.gameMode.useItem(player(), activeHand);
            }
            pendingRelease = false;
            activeHand = null;
        } else if (player().isUsingItem()) {
            if (player().getUseItem().is(Items.BOW)) {
                if (player().getTicksUsingItem() >= 3) {
                    long now = System.currentTimeMillis();
                    if (now - lastShotTime >= speed.getValue().longValue()) {
                        activeHand = player().getUsedItemHand();
                        mc.gameMode.releaseUsingItem(player());
                        pendingRelease = true;
                        lastShotTime = now;
                    }
                }
            }
        }
    }
}
