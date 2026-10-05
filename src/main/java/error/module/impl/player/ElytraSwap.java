package error.module.impl.player;

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
import error.util.player.InventoryUtil;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class ElytraSwap extends Module {
    public static ElytraSwap INSTANCE;

    public final BindSetting bindSwap = bind("Бинд свапа", GLFW.GLFW_KEY_UNKNOWN);
    public final BindSetting bindFirework = bind("Бинд фейерверка", GLFW.GLFW_KEY_UNKNOWN);
    public final ModeSetting mode = mode("Режим", "Легит", "Легит", "Мгновенно");
    public final SliderSetting delay = slider("Задержка", 2.0F, 0.0F, 5.0F, 1.0F);
    public final CheckBox hotbarOnly = checkbox("Только хотбар", false);
    public final CheckBox autoChestplate = checkbox("Авто нагрудник", true);

    private boolean swapPending = false;
    private boolean fireworkPending = false;
    private int delayTimer = 0;
    private int fireworkStage = 0;
    private int fireworkTicks = 0;
    private int originalSlot = -1;
    private int fireworkSlot = -1;

    public ElytraSwap() {
        super("ElytraSwap", "Легитный свап элитры и фейерверк как AutoSwap", Category.PLAYER);
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
            triggerSwap();
        } else if (bindFirework != null && !bindFirework.isEmpty() && bindFirework.matches(key)) {
            triggerFirework();
        }
    }

    @EventTarget
    public void onMouseInput(MouseInputEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS || screen() != null || mc.player == null) return;

        int btn = event.getButton();
        if (bindSwap != null && !bindSwap.isEmpty() && bindSwap.matchesMouse(btn)) {
            triggerSwap();
        } else if (bindFirework != null && !bindFirework.isEmpty() && bindFirework.matchesMouse(btn)) {
            triggerFirework();
        }
    }

    private void triggerSwap() {
        if (mc.player == null) return;

        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean currentIsElytra = currentChest.is(Items.ELYTRA);

        int targetSlot = currentIsElytra ? findBestChestplateSlot() : findBestElytraSlot();
        if (targetSlot == -1) return;

        swapPending = true;
        delayTimer = mode.is("Легит") ? (int) (float) delay.getValue() : 0;
    }

    private void triggerFirework() {
        if (mc.player == null || mc.gameMode == null) return;

        if (mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            mc.player.swing(InteractionHand.OFF_HAND);
            return;
        }

        int slot = findFireworkSlot();
        if (slot == -1) return;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        fireworkSlot = slot;
        fireworkPending = true;
        fireworkStage = 0;
        fireworkTicks = 0;
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || mc.player == null) return;

        // Auto chestplate when on ground
        if (autoChestplate.getValue() && mc.player.onGround() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            int chestSlot = findBestChestplateSlot();
            if (chestSlot != -1) {
                InventoryUtil.swapSlots(chestSlot, 6); // 6 is chest armor slot in container
            }
        }

        if (swapPending) {
            if (delayTimer > 0) {
                delayTimer--;
                return;
            }
            performElytraSwap();
            swapPending = false;
        }

        if (fireworkPending) {
            handleFireworkSequence();
        }
    }

    private void performElytraSwap() {
        if (mc.player == null) return;

        ItemStack currentChest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean currentIsElytra = currentChest.is(Items.ELYTRA);
        int targetSlot = currentIsElytra ? findBestChestplateSlot() : findBestElytraSlot();
        if (targetSlot != -1) {
            InventoryUtil.swapSlots(targetSlot, 6);
        }
    }

    private void handleFireworkSequence() {
        fireworkTicks++;
        switch (fireworkStage) {
            case 0 -> {
                if (fireworkSlot < 9) {
                    selectSlot(fireworkSlot);
                } else {
                    InventoryUtil.swapSlots(fireworkSlot, originalSlot);
                }
                fireworkStage = 1;
                fireworkTicks = 0;
            }
            case 1 -> {
                if (fireworkTicks >= 1) {
                    mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    fireworkStage = 2;
                    fireworkTicks = 0;
                }
            }
            case 2 -> {
                if (fireworkTicks >= 1) {
                    if (fireworkSlot >= 9) {
                        InventoryUtil.swapSlots(fireworkSlot, originalSlot);
                    }
                    if (originalSlot >= 0 && originalSlot < 9) {
                        selectSlot(originalSlot);
                    }
                    fireworkPending = false;
                    fireworkStage = 0;
                    fireworkTicks = 0;
                }
            }
        }
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

    private void reset() {
        swapPending = false;
        fireworkPending = false;
        delayTimer = 0;
        fireworkStage = 0;
        fireworkTicks = 0;
        originalSlot = -1;
        fireworkSlot = -1;
    }

    private int findBestElytraSlot() {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        int max = hotbarOnly.getValue() ? 9 : 36;
        for (int i = 0; i < max; i++) {
            if (inv.getItem(i).is(Items.ELYTRA)) return i;
        }
        return -1;
    }

    private int findBestChestplateSlot() {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        int max = hotbarOnly.getValue() ? 9 : 36;
        for (int i = 0; i < max; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && isChestplate(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item == Items.NETHERITE_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.IRON_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE || item == Items.LEATHER_CHESTPLATE) {
            return true;
        }
        String name = stack.getItem().toString().toLowerCase(Locale.ROOT);
        return name.contains("chestplate");
    }

    private int findFireworkSlot() {
        if (mc.player == null) return -1;
        Inventory inv = mc.player.getInventory();
        int max = hotbarOnly.getValue() ? 9 : 36;
        for (int i = 0; i < max; i++) {
            if (inv.getItem(i).is(Items.FIREWORK_ROCKET)) return i;
        }
        return -1;
    }
}