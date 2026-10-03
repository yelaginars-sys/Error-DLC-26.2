package error.module.impl.misc;

import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

public class AutoSell extends Module {
    public static AutoSell INSTANCE;

    public final ModeSetting commandMode = mode("Команда", "/ah sell", "/ah sell", "/sell", "/auc sell");
    public final SliderSetting price = slider("Цена продажи", 10000.0f, 10.0f, 1000000.0f, 1000.0f);
    public final SliderSetting sellDelay = slider("Задержка (сек)", 1.5f, 0.5f, 5.0f, 0.1f);
    public final CheckBox multiplyByCount = checkbox("Умножать на кол-во", false);
    public final CheckBox autoSellInventory = checkbox("Продавать из инвентаря", false);
    public final CheckBox notifyChat = checkbox("Уведомления в чат", true);

    public final BindSetting instantSellBind = bind("Быстрая продажа", GLFW.GLFW_KEY_Z);

    private long lastSellTime = 0;

    public AutoSell() {
        super("AutoSell", "Автоматическая выгрузка и продажа предметов на аукционе", Category.MISC);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || !autoSellInventory.getValue()) return;

        long now = System.currentTimeMillis();
        if (now - lastSellTime < (long) (sellDelay.getValue() * 1000.0f)) return;

        ItemStack handStack = player().getMainHandItem();
        if (!handStack.isEmpty()) {
            sellItem(handStack);
            lastSellTime = now;
        }
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && instantSellBind.matches(event.getKey())) {
            sellHandItemInstant();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && instantSellBind.matchesMouse(event.getButton())) {
            sellHandItemInstant();
        }
    }

    public void sellHandItemInstant() {
        if (!inGame() || player() == null) return;
        ItemStack handStack = player().getMainHandItem();
        if (handStack.isEmpty()) {
            ChatUtil.error("Возьмите предмет для продажи в руку!");
            return;
        }
        sellItem(handStack);
    }

    private void sellItem(ItemStack stack) {
        if (mc.getConnection() == null) return;

        long finalPrice = price.getValue().longValue();
        if (multiplyByCount.getValue() && stack.getCount() > 1) {
            finalPrice *= stack.getCount();
        }

        String itemName = stack.getHoverName().getString();
        String cmd = commandMode.getValue().replace("/", "") + " " + finalPrice;

        mc.getConnection().sendCommand(cmd);

        if (notifyChat.getValue()) {
            ChatUtil.success("Выставлен лот: §e" + itemName + " §f(x" + stack.getCount() + ") за §a$" + finalPrice);
        }
        Notify.add("AutoSell", itemName + " -> $" + finalPrice, IconUse.CHECK, Notify.COLOR_SUCCESS);
    }
}
