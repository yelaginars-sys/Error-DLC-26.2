package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.KeyboardInputEvent;
import error.event.list.MouseInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.BindSetting;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.util.player.InventoryUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

public class ServerHelper extends Module {
    public static ServerHelper INSTANCE;

    public final ModeSetting spookyMode = mode("Режим", "HolyWorld", "Funtime", "HolyWorld", "ReallyWorld", "LonyGrief");
    public final BindSetting bindHealing = bind("Исцеление", GLFW.GLFW_KEY_UNKNOWN);
    public final BindSetting bindChorus = bind("Хорус", GLFW.GLFW_KEY_UNKNOWN);

    // Funtime binds
    public final BindSetting bindDisorient = bind("Дезориентация", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("Funtime"));
    public final BindSetting bindTrap = bind("Трапка", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("Funtime"));
    public final BindSetting bindPlast = bind("Пласт", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("Funtime"));
    public final BindSetting bindDust = bind("Явная пыль", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("Funtime"));
    public final BindSetting bindSnow = bind("Снег заморозки", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("Funtime"));
    public final BindSetting bindWindCharge = bind("Заряд ветра", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("Funtime"));

    // HolyWorld binds
    public final BindSetting bindStun = bind("стан", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("HolyWorld"));
    public final BindSetting bindExpTrap = bind("взрывная трапка", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("HolyWorld"));
    public final BindSetting bindNormTrap = bind("трапка ", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("HolyWorld"));
    public final BindSetting bindBomb = bind("взрывная штучка", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("HolyWorld"));
    public final BindSetting bindHwSnow = bind("ком снега", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("HolyWorld"));

    // ReallyWorld binds
    public final BindSetting bindRwTrap = bind("Ловушка (RW)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("ReallyWorld"));
    public final BindSetting bindRwEnderTrap = bind("Эндер-ловушка (RW)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("ReallyWorld"));
    public final BindSetting bindRwAntiFly = bind("Анти-полет (RW)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("ReallyWorld"));

    // LonyGrief binds
    public final BindSetting bindLgLeave = bind("Обычная ливалка (LG)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("LonyGrief"));
    public final BindSetting bindLgPlatformLeave = bind("Ливалка с платформой (LG)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("LonyGrief"));
    public final BindSetting bindLgTrap = bind("Уникальная трапка (LG)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("LonyGrief"));
    public final BindSetting bindLgFeather = bind("Уникальное перо (LG)", GLFW.GLFW_KEY_UNKNOWN).visible(() -> spookyMode.is("LonyGrief"));

    public final CheckBox snowEffect = checkbox("Эффект снежка", false).visible(() -> spookyMode.is("Funtime") || spookyMode.is("HolyWorld"));

    private boolean activeUse = false;
    private boolean isConsumableUse = false;
    private int useStage = 0;
    private int useTicks = 0;
    private int targetHotbarSlot = -1;
    private int originalSlot = -1;
    private int invSwapSlot = -1;
    private boolean isInvSwapped = false;
    private Item currentItem = null;
    private Predicate<ItemStack> currentPredicate = null;

    public ServerHelper() {
        super("ServerHelper", "Помощник для серверов", Category.MISC);
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        finishUsage();
        super.onDisable();
    }

    @EventTarget
    public void onKeyboardInput(KeyboardInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;
        checkBinds(event.getKey(), false);
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;
        checkBinds(event.getButton(), true);
    }

    private void checkBinds(int keyOrBtn, boolean isMouse) {
        if (activeUse || mc.player == null) return;

        Item targetItem = null;
        boolean isConsumable = false;
        Predicate<ItemStack> pred = null;

        if (matches(bindHealing, keyOrBtn, isMouse)) {
            targetItem = Items.POTION;
            isConsumable = true;
            pred = ServerHelper::isHealingPotion;
        } else if (matches(bindChorus, keyOrBtn, isMouse)) {
            targetItem = Items.CHORUS_FRUIT;
            isConsumable = true;
        } else if (spookyMode.is("Funtime")) {
            if (matches(bindDisorient, keyOrBtn, isMouse)) targetItem = Items.ENDER_EYE;
            else if (matches(bindTrap, keyOrBtn, isMouse)) targetItem = Items.NETHERITE_SCRAP;
            else if (matches(bindPlast, keyOrBtn, isMouse)) targetItem = Items.DRIED_KELP;
            else if (matches(bindDust, keyOrBtn, isMouse)) targetItem = Items.SUGAR;
            else if (matches(bindSnow, keyOrBtn, isMouse)) targetItem = Items.SNOWBALL;
            else if (matches(bindWindCharge, keyOrBtn, isMouse)) targetItem = Items.WIND_CHARGE;
        } else if (spookyMode.is("HolyWorld")) {
            if (matches(bindStun, keyOrBtn, isMouse)) targetItem = Items.NETHER_STAR;
            else if (matches(bindExpTrap, keyOrBtn, isMouse)) targetItem = Items.PRISMARINE_SHARD;
            else if (matches(bindNormTrap, keyOrBtn, isMouse)) targetItem = Items.POPPED_CHORUS_FRUIT;
            else if (matches(bindBomb, keyOrBtn, isMouse)) targetItem = Items.FIRE_CHARGE;
            else if (matches(bindHwSnow, keyOrBtn, isMouse)) targetItem = Items.SNOWBALL;
        } else if (spookyMode.is("ReallyWorld")) {
            if (matches(bindRwTrap, keyOrBtn, isMouse)) {
                targetItem = Items.HEART_OF_THE_SEA;
                pred = s -> s.is(Items.HEART_OF_THE_SEA) || s.getHoverName().getString().toLowerCase().contains("ловушк");
            } else if (matches(bindRwEnderTrap, keyOrBtn, isMouse)) {
                targetItem = Items.ENDER_EYE;
                pred = s -> s.is(Items.ENDER_EYE) || s.getHoverName().getString().toLowerCase().contains("эндер");
            } else if (matches(bindRwAntiFly, keyOrBtn, isMouse)) {
                targetItem = Items.PHANTOM_MEMBRANE;
                pred = s -> s.is(Items.PHANTOM_MEMBRANE) || s.getHoverName().getString().toLowerCase().contains("полет") || s.getHoverName().getString().toLowerCase().contains("полёт");
            }
        } else if (spookyMode.is("LonyGrief")) {
            if (matches(bindLgLeave, keyOrBtn, isMouse)) {
                targetItem = Items.MAGMA_CREAM;
                pred = s -> s.is(Items.MAGMA_CREAM) || s.getHoverName().getString().toLowerCase().contains("ливалк");
            } else if (matches(bindLgPlatformLeave, keyOrBtn, isMouse)) {
                targetItem = Items.CLAY_BALL;
                pred = s -> s.is(Items.CLAY_BALL) || s.getHoverName().getString().toLowerCase().contains("платформ");
            } else if (matches(bindLgTrap, keyOrBtn, isMouse)) {
                targetItem = Items.CRYING_OBSIDIAN;
                pred = s -> s.is(Items.CRYING_OBSIDIAN) || s.getHoverName().getString().toLowerCase().contains("трапк");
            } else if (matches(bindLgFeather, keyOrBtn, isMouse)) {
                targetItem = Items.FEATHER;
                pred = s -> s.is(Items.FEATHER) || s.getHoverName().getString().toLowerCase().contains("пер");
            }
        }

        if (targetItem != null) {
            triggerItemUse(targetItem, isConsumable, pred);
        }
    }

    private boolean matches(BindSetting setting, int keyOrBtn, boolean isMouse) {
        if (setting == null || setting.isEmpty()) return false;
        return isMouse ? setting.matchesMouse(keyOrBtn) : setting.matches(keyOrBtn);
    }

    private void triggerItemUse(Item item, boolean consumable, Predicate<ItemStack> pred) {
        if (mc.player == null || mc.gameMode == null) return;

        ItemStack stack = findStack(item, pred);
        if (stack == null || mc.player.getCooldowns().isOnCooldown(stack)) {
            return;
        }

        originalSlot = mc.player.getInventory().getSelectedSlot();
        invSwapSlot = -1;
        isInvSwapped = false;

        int hotbarSlot = findInHotbar(item, pred);
        if (hotbarSlot == -1) {
            int mainInvSlot = findInInventory(item, pred);
            if (mainInvSlot == -1) return;

            hotbarSlot = originalSlot;
            InventoryUtil.swapSlots(mainInvSlot, hotbarSlot);
            invSwapSlot = mainInvSlot;
            isInvSwapped = true;
        }

        targetHotbarSlot = hotbarSlot;
        currentItem = item;
        currentPredicate = pred;
        isConsumableUse = consumable;
        activeUse = true;
        useStage = 1;
        useTicks = 0;

        selectSlot(targetHotbarSlot);
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !activeUse || mc.player == null || mc.gameMode == null) return;

        useTicks++;
        if (useStage == 1) {
            if (useTicks >= 2) {
                if (isConsumableUse) {
                    mc.options.keyUse.setDown(true);
                    if (!mc.player.isUsingItem()) {
                        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    }
                    useStage = 2;
                    useTicks = 0;
                } else {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    useStage = 2;
                    useTicks = 0;
                }
            }
        } else if (useStage == 2) {
            if (!isConsumableUse || !mc.player.isUsingItem() || useTicks > 40) {
                finishUsage();
            }
        }
    }

    private void finishUsage() {
        if (mc.options != null) {
            mc.options.keyUse.setDown(false);
        }

        if (isInvSwapped && invSwapSlot != -1 && targetHotbarSlot != -1 && mc.player != null) {
            InventoryUtil.swapSlots(invSwapSlot, targetHotbarSlot);
        }

        if (originalSlot >= 0 && originalSlot < 9 && mc.player != null) {
            selectSlot(originalSlot);
        }

        activeUse = false;
        isConsumableUse = false;
        useStage = 0;
        useTicks = 0;
        targetHotbarSlot = -1;
        originalSlot = -1;
        invSwapSlot = -1;
        isInvSwapped = false;
        currentItem = null;
        currentPredicate = null;
    }

    private void selectSlot(int slot) {
        if (mc.player == null || slot < 0 || slot >= 9) return;
        if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            }
        }
    }

    private int findInHotbar(Item item, Predicate<ItemStack> pred) {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && (pred != null ? pred.test(stack) : stack.is(item))) {
                return i;
            }
        }
        return -1;
    }

    private int findInInventory(Item item, Predicate<ItemStack> pred) {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && (pred != null ? pred.test(stack) : stack.is(item))) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack findStack(Item item, Predicate<ItemStack> pred) {
        if (mc.player == null) return null;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && (pred != null ? pred.test(stack) : stack.is(item))) {
                return stack;
            }
        }
        return null;
    }

    private static boolean isHealingPotion(ItemStack stack) {
        if (!stack.isEmpty() && stack.is(Items.POTION)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) return false;
            for (MobEffectInstance effect : contents.getAllEffects()) {
                if (effect.getEffect() == MobEffects.REGENERATION) {
                    return true;
                }
            }
        }
        return false;
    }
}