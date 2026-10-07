package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.SliderSetting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AutoDarena module ported from exclusive.
 * Automatically joins and farms the D-Arena on FunTime.
 */
public class AutoDarena extends Module {

    public final CheckBox checkPlayers = checkbox("Проверять игроков", true);
    public final SliderSetting checkRadius = slider("Радиус проверки", 10.0f, 5.0f, 50.0f, 1.0f);

    private boolean hasMessageSent = false;
    private long lastClickTime = 0L;

    public AutoDarena() {
        super("AutoDarena", "Фармит д-арену на FunTime", Category.MISC);
    }

    @Override
    protected void onEnable() {
        hasMessageSent = false;
        lastClickTime = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    protected void onDisable() {
        hasMessageSent = false;
        super.onDisable();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) return;

        if (!isPlayerInPvP() && !hasMessageSent) {
            if (mc.getConnection() != null) {
                mc.getConnection().sendCommand("darena");
            }
            hasMessageSent = true;
        }

        AbstractContainerMenu container = player().containerMenu;
        if (container != null && container != player().inventoryMenu) {
            long now = System.currentTimeMillis();
            if (now - lastClickTime >= 250L) {
                for (Slot slot : container.slots) {
                    ItemStack itemStack = slot.getItem();
                    if (!itemStack.isEmpty() && (itemStack.is(Items.PUFFERFISH) || itemStack.getItem().getDescriptionId().contains("pufferfish"))) {
                        if (mc.gameMode != null) {
                            mc.gameMode.handleContainerInput(
                                    container.containerId,
                                    slot.index,
                                    0,
                                    ContainerInput.QUICK_MOVE,
                                    player()
                            );
                        }
                        break;
                    }
                }
                lastClickTime = now;
            }
        }
    }

    private boolean isPlayerInPvP() {
        if (!checkPlayers.getValue() || mc.level == null || player() == null) {
            return false;
        }

        double radius = checkRadius.getValue();
        double radiusSq = radius * radius;

        for (Player other : mc.level.players()) {
            if (other == player() || other.isSpectator() || other.isCreative()) continue;
            if (FriendManager.getInstance().isFriend(other)) continue;

            if (player().distanceToSqr(other) <= radiusSq) {
                return true;
            }
        }

        return false;
    }
}
