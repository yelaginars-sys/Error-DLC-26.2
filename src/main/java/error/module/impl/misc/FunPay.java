package error.module.impl.misc;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;
import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.mixin.accessor.AbstractContainerScreenAccessor;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunPay extends Module {
    public static FunPay INSTANCE;

    public final MultiModeSetting targetItems = multiMode("Целевые предметы",
            List.of("Тотемы", "Гэплы", "Талисманы", "Элитра", "Обереги", "Незерит", "Все"),
            "Тотемы", "Гэплы", "Талисманы", "Элитра", "Обереги", "Незерит", "Все"
    );
    public final SliderSetting maxPrice = slider("Макс. цена покупки", 50000.0f, 1000.0f, 2000000.0f, 5000.0f);
    public final SliderSetting buyDelay = slider("Задержка покупки (мс)", 250.0f, 100.0f, 2000.0f, 50.0f);
    public final CheckBox autoBuy = checkbox("Автоматический клик", true);
    public final CheckBox soundNotify = checkbox("Звуковой сигнал", true);
    public final CheckBox chatLog = checkbox("Логировать в чат", true);

    public final BindSetting instantBuyBind = bind("Мгновенный выкуп", GLFW.GLFW_KEY_X);

    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:цена|стоимость|цена\\s+за\\s+шт|price)[:\\s]+([\\d\\s.,]+)", Pattern.CASE_INSENSITIVE);
    private long lastBuyTime = 0;

    public FunPay() {
        super("FunPay", "Автоматическая скупка и выкуп дешёвых лотов на аукционе", Category.MISC);
        INSTANCE = this;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (!inGame() || player() == null || !autoBuy.getValue()) return;

        if (screen() instanceof AbstractContainerScreen<?> containerScreen) {
            long now = System.currentTimeMillis();
            if (now - lastBuyTime < buyDelay.getValue().longValue()) return;

            scanAndBuyoutContainer(containerScreen);
        }
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && instantBuyBind.matches(event.getKey())) {
            buyoutUnderCursor();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() == GLFW.GLFW_PRESS && instantBuyBind.matchesMouse(event.getButton())) {
            buyoutUnderCursor();
        }
    }

    private void scanAndBuyoutContainer(AbstractContainerScreen<?> container) {
        int slotsCount = container.getMenu().slots.size();
        int maxContainerSlot = Math.min(54, slotsCount - 36);

        for (int i = 0; i < maxContainerSlot; i++) {
            Slot slot = container.getMenu().getSlot(i);
            if (slot == null || !slot.hasItem()) continue;

            ItemStack stack = slot.getItem();
            if (!isTargetItem(stack)) continue;

            long price = extractPriceFromLore(stack);
            if (price > 0 && price <= maxPrice.getValue().longValue()) {
                executeBuyout(container, i, stack, price);
                lastBuyTime = System.currentTimeMillis();
                break;
            }
        }
    }

    private void buyoutUnderCursor() {
        if (screen() instanceof AbstractContainerScreen<?> container) {
            Slot slot = ((AbstractContainerScreenAccessor) container).getHoveredSlot();
            if (slot != null && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                long price = extractPriceFromLore(stack);
                executeBuyout(container, slot.index, stack, price);
            }
        }
    }

    private void executeBuyout(AbstractContainerScreen<?> container, int slotId, ItemStack stack, long price) {
        if (mc.gameMode == null || player() == null) return;

        mc.gameMode.handleContainerInput(container.getMenu().containerId, slotId, 0, ContainerInput.QUICK_MOVE, player());

        String name = stack.getHoverName().getString();
        String priceText = price > 0 ? " за $" + price : "";

        if (chatLog.getValue()) {
            ChatUtil.success("FunPay: Выкуплен лот §e" + name + " §f(x" + stack.getCount() + ")" + priceText);
        }
        Notify.add("FunPay Выкуп", name + priceText, IconUse.CHECK, Notify.COLOR_SUCCESS);
    }

    private boolean isTargetItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (targetItems.isEnabled("Все")) return true;

        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);

        if (targetItems.isEnabled("Тотемы") && (stack.is(Items.TOTEM_OF_UNDYING) || name.contains("тотем"))) return true;
        if (targetItems.isEnabled("Гэплы") && (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || name.contains("яблоко"))) return true;
        if (targetItems.isEnabled("Элитра") && (stack.is(Items.ELYTRA) || name.contains("элитра"))) return true;
        if (targetItems.isEnabled("Талисманы") && (name.contains("талисман") || name.contains("сфера"))) return true;
        if (targetItems.isEnabled("Обереги") && (name.contains("оберег") || name.contains("шард"))) return true;
        if (targetItems.isEnabled("Незерит") && (name.contains("незерит") || stack.getItem().toString().contains("netherite"))) return true;

        return false;
    }

    private long extractPriceFromLore(ItemStack stack) {
        if (stack.isEmpty()) return -1;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return -1;

        for (Component line : lore.lines()) {
            String rawLine = line.getString().toLowerCase(Locale.ROOT).replaceAll("§[0-9a-fk-or]", "");
            Matcher matcher = PRICE_PATTERN.matcher(rawLine);
            if (matcher.find()) {
                String cleanDigits = matcher.group(1).replaceAll("[^\\d]", "");
                if (!cleanDigits.isEmpty()) {
                    try {
                        return Long.parseLong(cleanDigits);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return -1;
    }
}
