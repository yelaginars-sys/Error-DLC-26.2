package error.module.impl.player;

import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.player.InventoryUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class AutoSwap extends Module {
    public static AutoSwap INSTANCE;

    public final ModeSetting itemSetting = mode("Предмет", "Тотем", "Голова", "Тотем", "Гапл", "Щит", "Талисман");
    public final ModeSetting secondItemSetting = mode("Второй предмет", "Гапл", "Голова", "Тотем", "Гапл", "Щит", "Талисман");
    public final ModeSetting swapMode = mode("Режим", "Легит", "Легит", "Мгновенно");
    public final SliderSetting delay = slider("Задержка из инвентаря", 2.0F, 0.0F, 5.0F, 1.0F);
    public final BindSetting bindSwap = bind("Бинд свапа", GLFW.GLFW_KEY_UNKNOWN);
    public final BindSetting bindSphere = bind("Бинд сферы", GLFW.GLFW_KEY_UNKNOWN);
    public final String sphereNameFilter = "Сфера Цербера";

    private Item activeTargetItem = null;
    private boolean isSphereSwap = false;
    private boolean isTalismanSwap = false;
    private int delayTimer = 0;
    private boolean swapPending = false;
    private int pendingSlot = -1;

    public AutoSwap() {
        super("AutoSwap", "Свап предмета в левую руку по бинду", Category.PLAYER);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        reset();
        super.onDisable();
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;

        int key = event.getKey();
        if (bindSwap != null && !bindSwap.isEmpty() && bindSwap.matches(key)) {
            triggerNormalSwap();
        } else if (bindSphere != null && !bindSphere.isEmpty() && bindSphere.matches(key)) {
            triggerSphereSwap();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;

        int btn = event.getButton();
        if (bindSwap != null && !bindSwap.isEmpty() && bindSwap.matchesMouse(btn)) {
            triggerNormalSwap();
        } else if (bindSphere != null && !bindSphere.isEmpty() && bindSphere.matchesMouse(btn)) {
            triggerSphereSwap();
        }
    }

    private void triggerNormalSwap() {
        if (mc.player == null) return;

        Item primaryItem = getItemFromMode(itemSetting.getValue());
        Item secondaryItem = getItemFromMode(secondItemSetting.getValue());
        boolean primaryTalisman = itemSetting.is("Талисман");
        boolean secondaryTalisman = secondItemSetting.is("Талисман");

        ItemStack offhand = mc.player.getOffhandItem();
        boolean primaryInOffhand = isMatchingOffhand(offhand, primaryItem, primaryTalisman);

        Item target = primaryInOffhand ? secondaryItem : primaryItem;
        boolean talisman = primaryInOffhand ? secondaryTalisman : primaryTalisman;

        startSwap(target, false, talisman);
    }

    private void triggerSphereSwap() {
        if (mc.player == null) return;
        startSwap(Items.PLAYER_HEAD, true, false);
    }

    private void startSwap(Item item, boolean sphere, boolean talisman) {
        if (mc.player == null) return;

        int slot = findTargetSlot(item, sphere, talisman);
        if (slot == -1) return;

        activeTargetItem = item;
        isSphereSwap = sphere;
        isTalismanSwap = talisman;
        pendingSlot = slot;
        delayTimer = swapMode.is("Легит") ? (int) (float) delay.getValue() : 0;
        swapPending = true;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !swapPending || mc.player == null) return;

        if (delayTimer > 0) {
            delayTimer--;
            return;
        }

        performSwap(pendingSlot);
        reset();
    }

    private void performSwap(int slot) {
        if (mc.player == null || slot < 0 || slot >= 36) return;

        if (slot < 9) {
            InventoryUtil.swapSelectedWithOffhand(slot);
        } else {
            InventoryUtil.swapSlots(slot, 40); // 40 is offhand container slot
        }
    }

    private void reset() {
        activeTargetItem = null;
        isSphereSwap = false;
        isTalismanSwap = false;
        delayTimer = 0;
        swapPending = false;
        pendingSlot = -1;
    }

    private int findTargetSlot(Item item, boolean sphere, boolean talisman) {
        if (mc.player == null) return -1;

        Inventory inv = mc.player.getInventory();
        if (sphere) {
            return findSphereSlot();
        }
        if (talisman) {
            return findTalismanSlot();
        }

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findSphereSlot() {
        if (mc.player == null) return -1;
        String nameFilter = sphereNameFilter.toLowerCase(Locale.ROOT);
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.PLAYER_HEAD)) {
                String stackName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
                if (stackName.contains(nameFilter)) return i;
            }
        }
        return -1;
    }

    private int findTalismanSlot() {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING) && stack.isEnchanted()) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMatchingOffhand(ItemStack offhand, Item item, boolean talisman) {
        if (offhand.isEmpty()) return false;
        if (talisman) {
            return offhand.is(Items.TOTEM_OF_UNDYING) && offhand.isEnchanted();
        }
        return offhand.is(item);
    }

    private Item getItemFromMode(String modeName) {
        return switch (modeName) {
            case "Голова" -> Items.PLAYER_HEAD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING;
            case "Гапл" -> Items.GOLDEN_APPLE;
            case "Щит" -> Items.SHIELD;
            case "Талисман" -> Items.TOTEM_OF_UNDYING;
            default -> Items.AIR;
        };
    }
}