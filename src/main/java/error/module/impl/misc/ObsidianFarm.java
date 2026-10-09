package error.module.impl.misc;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import error.util.RotationHandler;
import error.util.client.persiki.ChatUtil;
import error.ui.hud.impl.DynamicIslandHud;
import error.util.player.InventoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ObsidianFarm extends Module {
    public static ObsidianFarm INSTANCE;

    public final CheckBox autoSell = checkbox("Авто продажа", true);
    public final CheckBox autoRepair = checkbox("Авто починка", true);
    public final CheckBox tpSpawnVoid = checkbox("Тп на спавн при y < 0", true);
    public final ModeSetting repairMode = mode("Режим починки", "Команда", "Команда", "Опыт");
    public final SliderSetting sellPrice = slider("Цена продажи", 1.0F, 1.0F, 500.0F, 1.0F);

    // Timing constants
    private static final long SELL_DELAY = 600L;
    private static final long SWAP_DELAY = 500L;
    private static final long FIX_COOLDOWN = 5000L;
    private static final long SPAWN_COOLDOWN = 5000L;
    private static final long XP_THROW_DELAY = 100L;
    private static final double MAX_DIST_SQ = 25.0; // 5 blocks range squared
    private static final int OBSIDIAN_TRIGGER_COUNT = 1152; // 18 stacks

    // Mining state
    private BlockPos currentTarget = null;
    private BlockPos lastMiningPos = null;

    // Repair state
    private boolean repairing = false;
    private long lastRepairActionTime = 0L;
    private long lastFixCommandTime = 0L;
    private final ArrayDeque<Runnable> repairQueue = new ArrayDeque<>();

    // Sell state
    private boolean selling = false;
    private boolean waitingSellCommand = false;
    private long lastSellTime = 0L;
    private long lastSwapTime = 0L;
    private int savedSlot = -1;
    private int savedPickaxeSlot = -1;
    private int obsidianScreenSlot = -1;
    private int targetHotbarSlot = -1;

    // Void safety
    private long lastSpawnCommandTime = 0L;

    public ObsidianFarm() {
        super("ObsidianFarm", "Автокоп обсидиана с авто-продажей и починкой кирки", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        while (!repairQueue.isEmpty()) {
            Runnable r = repairQueue.poll();
            if (r != null) r.run();
        }
        resetSellingState();
        currentTarget = null;
        lastMiningPos = null;
        repairing = false;
        RotationHandler.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.isPost() || !inGame() || mc.player == null || mc.level == null) return;

        // 1. Process queued repair action
        Runnable queuedAction = repairQueue.poll();
        if (queuedAction != null) {
            queuedAction.run();
        }

        // 2. Void safety check
        if (tpSpawnVoid.getValue()) {
            handleVoidSafety();
        }

        // 3. Auto repair check
        if (autoRepair.getValue()) {
            handleAutoRepair();
        }

        // 4. Mining logic (only when not repairing or selling)
        if (!repairing && !selling && !waitingSellCommand) {
            currentTarget = findTargetBlock(isDrill());
            if (currentTarget != null) {
                rotateAndMine(currentTarget);
            } else {
                if (lastMiningPos != null && mc.gameMode != null) {
                    mc.gameMode.stopDestroyBlock();
                    lastMiningPos = null;
                }
            }
        }

        // 5. Auto sell check
        if (autoSell.getValue()) {
            handleAutoSell();
        } else {
            resetSellingState();
        }
    }

    // ==================== MINING & SCANNING ====================

    public boolean isDrill() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        if (isPickaxe(stack)) {
            String name = stack.getHoverName().getString().toLowerCase();
            if (name.contains("бур")) return true;
            var lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (var line : lore.lines()) {
                    if (line.getString().toLowerCase().contains("бур")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPickaxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.NETHERITE_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)
                || stack.is(Items.IRON_PICKAXE) || stack.is(Items.GOLDEN_PICKAXE)
                || stack.is(Items.STONE_PICKAXE) || stack.is(Items.WOODEN_PICKAXE);
    }

    private BlockPos findTargetBlock(boolean isDrill) {
        if (mc.player == null || mc.level == null) return null;
        BlockPos playerPos = mc.player.blockPosition();
        int playerY = playerPos.getY();

        List<BlockPos> blocks = new ArrayList<>();
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos bp = playerPos.offset(x, y, z);
                    if (isObsidian(bp) && isWithinDistance(bp)) {
                        blocks.add(bp);
                    }
                }
            }
        }

        if (blocks.isEmpty()) return null;

        if (isDrill) {
            // Priority 1: Above head with >= 2 neighbors
            BlockPos target = blocks.stream()
                    .filter(bp -> bp.getY() > playerY && countNeighbors(bp) >= 2)
                    .max(Comparator.<BlockPos>comparingInt(bp -> bp.getY()).thenComparingDouble(bp -> -getClusterDistance(bp)))
                    .orElse(null);

            // Priority 2: Above head closest
            if (target == null) {
                target = blocks.stream()
                        .filter(bp -> bp.getY() > playerY)
                        .max(Comparator.<BlockPos>comparingInt(bp -> bp.getY()).thenComparingDouble(bp -> mc.player.distanceToSqr(Vec3.atCenterOf(bp))))
                        .orElse(null);
            }

            // Priority 3: Eye level with >= 2 neighbors
            if (target == null) {
                target = blocks.stream()
                        .filter(bp -> bp.getY() == playerY && countNeighbors(bp) >= 2)
                        .max(Comparator.<BlockPos>comparingDouble(bp -> -getClusterDistance(bp)))
                        .orElse(null);
            }

            // Priority 4: Eye level closest
            if (target == null) {
                target = blocks.stream()
                        .filter(bp -> bp.getY() == playerY)
                        .min(Comparator.<BlockPos>comparingDouble(bp -> mc.player.distanceToSqr(Vec3.atCenterOf(bp))))
                        .orElse(null);
            }

            // Priority 5: Feet level with >= 2 neighbors
            if (target == null) {
                target = blocks.stream()
                        .filter(bp -> bp.getY() < playerY && countNeighbors(bp) >= 2)
                        .max(Comparator.<BlockPos>comparingInt(bp -> bp.getY()).thenComparingDouble(bp -> -getClusterDistance(bp)))
                        .orElse(null);
            }

            // Priority 6: Feet level closest
            if (target == null) {
                target = blocks.stream()
                        .filter(bp -> bp.getY() < playerY)
                        .max(Comparator.<BlockPos>comparingInt(bp -> bp.getY()).thenComparingDouble(bp -> mc.player.distanceToSqr(Vec3.atCenterOf(bp))))
                        .orElse(null);
            }

            return target;
        } else {
            // Normal pickaxe: highest Y, then distance
            return blocks.stream()
                    .max(Comparator.<BlockPos>comparingInt(bp -> bp.getY()).thenComparingDouble(bp -> mc.player.distanceToSqr(Vec3.atCenterOf(bp))))
                    .orElse(null);
        }
    }

    private boolean isObsidian(BlockPos pos) {
        return mc.level != null && mc.level.getBlockState(pos).is(Blocks.OBSIDIAN);
    }

    private boolean isWithinDistance(BlockPos pos) {
        return mc.player != null && mc.player.distanceToSqr(Vec3.atCenterOf(pos)) <= MAX_DIST_SQ;
    }

    private int countNeighbors(BlockPos pos) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = pos.offset(dx, 0, dz);
                if (isObsidian(n)) {
                    count++;
                }
            }
        }
        return count;
    }

    private double getClusterDistance(BlockPos pos) {
        int count = 0;
        double sumX = 0;
        double sumZ = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = pos.offset(dx, 0, dz);
                if (isObsidian(n) && isWithinDistance(n)) {
                    sumX += n.getX();
                    sumZ += n.getZ();
                    count++;
                }
            }
        }
        if (count == 0) return Double.MAX_VALUE;
        double centerX = sumX / count;
        double centerZ = sumZ / count;
        return Math.sqrt(Math.pow(pos.getX() - centerX, 2.0) + Math.pow(pos.getZ() - centerZ, 2.0));
    }

    private void rotateAndMine(BlockPos pos) {
        if (mc.player == null || mc.gameMode == null) return;
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 diff = center.subtract(eyes);

        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.hypot(diff.x, diff.z)));

        RotationHandler.setRotation(yaw, pitch);

        if (!pos.equals(lastMiningPos)) {
            mc.gameMode.startDestroyBlock(pos, Direction.UP);
            lastMiningPos = pos;
        }

        if (mc.gameMode.continueDestroyBlock(pos, Direction.UP)) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    // ==================== AUTO REPAIR ====================

    private void handleAutoRepair() {
        if (mc.player == null) {
            repairing = false;
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        int damage = mainHand.getDamageValue();

        if (isPickaxe(mainHand) && damage >= 200) {
            repairing = true;

            if ("Команда".equalsIgnoreCase(repairMode.getValue())) {
                long now = System.currentTimeMillis();
                if (now - lastFixCommandTime >= FIX_COOLDOWN) {
                    lastFixCommandTime = now;
                    sendCommand("fix");
                }
            } else { // "Опыт"
                int xpSlot = findHotbarSlot(Items.EXPERIENCE_BOTTLE);
                if (xpSlot == -1) {
                    ChatUtil.error("Опыт в хотбаре не найден, отключение...");
                    DynamicIslandHud.showNotification("Опыт в хотбаре не найден!", false, 3000L);
                    this.setState(false);
                } else {
                    long now = System.currentTimeMillis();
                    if (repairQueue.isEmpty() && now - lastRepairActionTime > XP_THROW_DELAY) {
                        // 1. Swap pickaxe to offhand
                        repairQueue.add(() -> {
                            InventoryUtil.swapSelectedWithOffhand(mc.player.getInventory().getSelectedSlot());
                            lastRepairActionTime = System.currentTimeMillis();
                        });
                        // 2. Select XP bottle in hotbar & throw downwards
                        repairQueue.add(() -> {
                            setSelectedSlot(xpSlot);
                            if (mc.getConnection() != null) {
                                mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, mc.player.getYRot(), 90.0F));
                            }
                            lastRepairActionTime = System.currentTimeMillis();
                        });
                        // 3. Swap pickaxe back to main hand
                        repairQueue.add(() -> {
                            InventoryUtil.swapSelectedWithOffhand(mc.player.getInventory().getSelectedSlot());
                            lastRepairActionTime = System.currentTimeMillis();
                        });
                    }
                }
            }
        } else {
            repairing = false;
        }
    }

    // ==================== AUTO SELL ====================

    private void handleAutoSell() {
        if (mc.player == null) {
            resetSellingState();
            return;
        }

        long now = System.currentTimeMillis();
        String priceStr = String.valueOf((int) (float) sellPrice.getValue());

        if (waitingSellCommand) {
            if (now - lastSellTime >= SELL_DELAY) {
                sendCommand("market sell " + priceStr);
                lastSellTime = now;
                waitingSellCommand = false;
                lastSwapTime = now;

                int nextObsidianSlot = findObsidianSlotInContainer();
                if (nextObsidianSlot != -1) {
                    obsidianScreenSlot = nextObsidianSlot;
                    selling = true;
                } else {
                    finishSelling();
                }
            }
        } else if (now - lastSwapTime >= SWAP_DELAY) {
            if (selling) {
                swapContainerSlots(obsidianScreenSlot, targetHotbarSlot);
                setSelectedSlot(targetHotbarSlot);
                waitingSellCommand = true;
                lastSellTime = now;
                selling = false;
                lastSwapTime = now;
            } else {
                if (countTotalObsidian() >= OBSIDIAN_TRIGGER_COUNT) {
                    int slot = findObsidianSlotInContainer();
                    if (slot == -1) return;

                    if (savedSlot == -1) {
                        savedSlot = mc.player.getInventory().getSelectedSlot();
                        savedPickaxeSlot = findPickaxeInventorySlot();
                        targetHotbarSlot = findAlternateHotbarSlot(savedSlot);
                    }

                    obsidianScreenSlot = slot;
                    selling = true;
                    lastSwapTime = now;
                }
            }
        }
    }

    private int countTotalObsidian() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.OBSIDIAN)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int findObsidianSlotInContainer() {
        if (mc.player == null) return -1;
        int firstFound = -1;
        for (Slot slot : mc.player.inventoryMenu.slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && stack.is(Items.OBSIDIAN)) {
                if (firstFound == -1) firstFound = slot.index;
                if (stack.getCount() == stack.getMaxStackSize()) {
                    return slot.index;
                }
            }
        }
        return firstFound;
    }

    private int findAlternateHotbarSlot(int currentSlot) {
        if (mc.player == null) return currentSlot == 0 ? 1 : 0;
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.isEmpty() || stack.is(Items.OBSIDIAN)) {
                    return i;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) return i;
        }
        return currentSlot == 0 ? 1 : 0;
    }

    private int findPickaxeInventorySlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (isPickaxe(mc.player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findHotbarSlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    private void setSelectedSlot(int slot) {
        if (mc.player != null && mc.getConnection() != null && slot >= 0 && slot < 9) {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    private void swapContainerSlots(int fromSlot, int button) {
        if (mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, fromSlot, button, ContainerInput.SWAP, mc.player);
        }
    }

    private void finishSelling() {
        if (savedPickaxeSlot != -1) {
            int currentTarget = targetHotbarSlot != -1 ? targetHotbarSlot : 0;
            swapContainerSlots(savedPickaxeSlot, currentTarget);
        }
        int restoreSlot = savedSlot != -1 ? savedSlot : 0;
        setSelectedSlot(restoreSlot);
        resetSellingState();
    }

    private void resetSellingState() {
        selling = false;
        waitingSellCommand = false;
        savedSlot = -1;
        savedPickaxeSlot = -1;
        obsidianScreenSlot = -1;
        targetHotbarSlot = -1;
    }

    // ==================== VOID SAFETY ====================

    private void handleVoidSafety() {
        if (mc.player == null) return;
        if (mc.player.getY() < 0.0) {
            long now = System.currentTimeMillis();
            if (now - lastSpawnCommandTime >= SPAWN_COOLDOWN) {
                lastSpawnCommandTime = now;
                sendCommand("spawn");
                ChatUtil.success("Успешно тепнул на спавн!");
                DynamicIslandHud.showNotification("Телепорт на спавн!", true, 3000L);
            }
        }
    }

    private void sendCommand(String command) {
        if (mc.getConnection() == null) return;
        String cmd = command.startsWith("/") ? command.substring(1) : command;
        mc.getConnection().sendCommand(cmd);
    }
}
