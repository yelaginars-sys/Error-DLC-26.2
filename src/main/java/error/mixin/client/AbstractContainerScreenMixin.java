package error.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import error.util.client.persiki.AHUtils;
import error.module.impl.misc.AHHelper;
import error.module.impl.misc.ItemScroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by daun kvass
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {

    @Shadow @Nullable protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    @Shadow private @Nullable Slot getHoveredSlot(final double x, final double y) { return null; }

    @Unique private boolean error$isDroppingInv = false;
    @Unique private boolean error$isLootingChest = false;
    @Unique private boolean error$isStoringChest = false;
    @Unique private boolean error$isDroppingChest = false;

    @Unique private Button error$dropInvBtn;
    @Unique private Button error$lootChestBtn;
    @Unique private Button error$storeChestBtn;
    @Unique private Button error$dropChestBtn;

    @Unique private boolean error$isAuction = false;
    @Unique private Slot error$cheapSlot = null;
    @Unique private Slot error$goodSlot = null;
    @Unique private long error$lastAhScan = 0L;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void error$initButtons(CallbackInfo ci) {
        if ((Object) this instanceof CreativeModeInventoryScreen) return;

        boolean isPlayerInv = (Object) this instanceof InventoryScreen;
        int btnY = this.topPos - 22;

        if (isPlayerInv) {
            int btnWidth = 100;
            int btnX = this.leftPos + (this.imageWidth - btnWidth) / 2;
            this.error$dropInvBtn = Button.builder(Component.literal("Выбросить все"), b -> {
                this.error$isDroppingInv = !this.error$isDroppingInv;
                this.error$updateButtonLabels();
            }).bounds(btnX, btnY, btnWidth, 18).build();
            this.addRenderableWidget(this.error$dropInvBtn);
        } else {
            int btnWidth = 56;
            int gap = 3;
            int totalWidth = btnWidth * 3 + gap * 2;
            int startX = this.leftPos + (this.imageWidth - totalWidth) / 2;

            this.error$lootChestBtn = Button.builder(Component.literal("Забрать"), b -> {
                this.error$isLootingChest = !this.error$isLootingChest;
                this.error$isStoringChest = false;
                this.error$isDroppingChest = false;
                this.error$updateButtonLabels();
            }).bounds(startX, btnY, btnWidth, 18).build();

            this.error$storeChestBtn = Button.builder(Component.literal("Сложить"), b -> {
                this.error$isStoringChest = !this.error$isStoringChest;
                this.error$isLootingChest = false;
                this.error$isDroppingChest = false;
                this.error$updateButtonLabels();
            }).bounds(startX + btnWidth + gap, btnY, btnWidth, 18).build();

            this.error$dropChestBtn = Button.builder(Component.literal("Выкинуть"), b -> {
                this.error$isDroppingChest = !this.error$isDroppingChest;
                this.error$isLootingChest = false;
                this.error$isStoringChest = false;
                this.error$updateButtonLabels();
            }).bounds(startX + (btnWidth + gap) * 2, btnY, btnWidth, 18).build();

            this.addRenderableWidget(this.error$lootChestBtn);
            this.addRenderableWidget(this.error$storeChestBtn);
            this.addRenderableWidget(this.error$dropChestBtn);
        }
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void error$onContainerTick(CallbackInfo ci) {
        if (this.minecraft.player == null || this.minecraft.gameMode == null) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;

        int itemsPerTick = 1;
        int processed = 0;

        if (this.error$isDroppingInv) {
            boolean hasMore = false;
            for (Slot slot : screen.getMenu().slots) {
                if (slot.hasItem() && !slot.getItem().isEmpty()) {
                    this.minecraft.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 1, ContainerInput.THROW, this.minecraft.player);
                    hasMore = true;
                    if (++processed >= itemsPerTick) break;
                }
            }
            if (!hasMore) { this.error$isDroppingInv = false; this.error$updateButtonLabels(); }
        } else if (this.error$isLootingChest) {
            boolean hasMore = false;
            for (Slot slot : screen.getMenu().slots) {
                if (error$isContainerSlot(slot) && slot.hasItem() && !slot.getItem().isEmpty()) {
                    this.minecraft.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, this.minecraft.player);
                    hasMore = true;
                    if (++processed >= itemsPerTick) break;
                }
            }
            if (!hasMore) { this.error$isLootingChest = false; this.error$updateButtonLabels(); }
        } else if (this.error$isStoringChest) {
            boolean hasMore = false;
            for (Slot slot : screen.getMenu().slots) {
                if (!error$isContainerSlot(slot) && slot.hasItem() && !slot.getItem().isEmpty()) {
                    this.minecraft.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE, this.minecraft.player);
                    hasMore = true;
                    if (++processed >= itemsPerTick) break;
                }
            }
            if (!hasMore) { this.error$isStoringChest = false; this.error$updateButtonLabels(); }
        } else if (this.error$isDroppingChest) {
            boolean hasMore = false;
            for (Slot slot : screen.getMenu().slots) {
                if (error$isContainerSlot(slot) && slot.hasItem() && !slot.getItem().isEmpty()) {
                    this.minecraft.gameMode.handleContainerInput(screen.getMenu().containerId, slot.index, 1, ContainerInput.THROW, this.minecraft.player);
                    hasMore = true;
                    if (++processed >= itemsPerTick) break;
                }
            }
            if (!hasMore) { this.error$isDroppingChest = false; this.error$updateButtonLabels(); }
        }

        if (AHHelper.INSTANCE != null && AHHelper.INSTANCE.isEnabled()) {
            this.error$isAuction = AHUtils.isAuction(screen.getMenu(), this.title);
            long now = System.currentTimeMillis();
            if (this.error$isAuction && now - this.error$lastAhScan > 100L) {
                this.error$lastAhScan = now;
                error$scanAuctionSlots(screen);
            }
        } else {
            this.error$cheapSlot = null;
            this.error$goodSlot = null;
        }
    }

    @Unique
    private void error$scanAuctionSlots(AbstractContainerScreen<?> screen) {
        List<Slot> validSlots = new ArrayList<>();
        int maxSlots = Math.min(45, screen.getMenu().slots.size());

        for (int i = 0; i < maxSlots; i++) {
            Slot slot = screen.getMenu().slots.get(i);
            ItemStack stack = slot.getItem();
            if (AHHelper.INSTANCE.shouldIgnore(stack)) continue;

            long price = AHUtils.getPrice(stack);
            if (price > 0 && price != Long.MAX_VALUE) {
                validSlots.add(slot);
            }
        }

        Slot green = null;
        Slot yellow = null;

        if (!validSlots.isEmpty()) {
            long lowestTotal = Long.MAX_VALUE;
            int bestGreenScore = -1;

            for (Slot slot : validSlots) {
                ItemStack stack = slot.getItem();
                long price = AHUtils.getPrice(stack);
                int score = AHHelper.INSTANCE.getEnchantPriorityScore(stack);

                if (score > bestGreenScore || (score == bestGreenScore && price < lowestTotal)) {
                    bestGreenScore = score;
                    lowestTotal = price;
                    green = slot;
                }
            }

            if (validSlots.size() > 1) {
                long lowestUnit = Long.MAX_VALUE;
                int bestYellowScore = -1;

                for (Slot slot : validSlots) {
                    if (slot == green) continue;
                    ItemStack stack = slot.getItem();
                    long price = AHUtils.getPrice(stack);
                    int count = Math.max(1, stack.getCount());
                    long unitPrice = price / count;
                    int score = AHHelper.INSTANCE.getEnchantPriorityScore(stack);

                    if (score > bestYellowScore || (score == bestYellowScore && unitPrice < lowestUnit)) {
                        bestYellowScore = score;
                        lowestUnit = unitPrice;
                        yellow = slot;
                    }
                }
            }
        }

        this.error$cheapSlot = green;
        this.error$goodSlot = yellow;
    }

    @Inject(method = "extractSlots", at = @At("TAIL"))
    private void error$drawAhHighlights(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (AHHelper.INSTANCE == null || !AHHelper.INSTANCE.isEnabled() || !this.error$isAuction) return;

        if (this.error$cheapSlot != null) {
            int sx = this.error$cheapSlot.x;
            int sy = this.error$cheapSlot.y;
            graphics.fill(sx, sy, sx + 16, sy + 16, AHHelper.INSTANCE.cheapSlotColor.getValue());
        }

        if (this.error$goodSlot != null) {
            int sx = this.error$goodSlot.x;
            int sy = this.error$goodSlot.y;
            graphics.fill(sx, sy, sx + 16, sy + 16, AHHelper.INSTANCE.goodSlotColor.getValue());
        }
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void error$addUnitPriceToTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        if (AHHelper.INSTANCE == null || !AHHelper.INSTANCE.isEnabled() || !AHHelper.INSTANCE.showPricePerUnit.get() || !this.error$isAuction) return;
        if (stack.isEmpty() || stack.getCount() <= 1) return;

        long totalPrice = AHUtils.getPrice(stack);
        if (totalPrice == Long.MAX_VALUE || totalPrice <= 0) return;

        long unitPrice = totalPrice / stack.getCount();
        List<Component> original = new ArrayList<>(cir.getReturnValue());

        Component unitText = Component.literal("§7Цена за шт: §a$" + formatPrice(unitPrice));
        original.add(unitText);
        cir.setReturnValue(original);
    }

    @Unique
    private String formatPrice(long price) {
        if (price >= 1_000_000) return String.format("%.1fM", price / 1_000_000.0);
        if (price >= 1_000) return String.format("%.1fK", price / 1_000.0);
        return String.valueOf(price);
    }

    @Unique
    private boolean error$isContainerSlot(Slot slot) {
        if (this.minecraft.player == null) return false;
        return slot.container != this.minecraft.player.getInventory();
    }

    @Unique
    private void error$updateButtonLabels() {
        if (this.error$dropInvBtn != null) this.error$dropInvBtn.setMessage(Component.literal(this.error$isDroppingInv ? "§cСтоп..." : "Выбросить все"));
        if (this.error$lootChestBtn != null) this.error$lootChestBtn.setMessage(Component.literal(this.error$isLootingChest ? "§cСтоп..." : "Забрать"));
        if (this.error$storeChestBtn != null) this.error$storeChestBtn.setMessage(Component.literal(this.error$isStoringChest ? "§cСтоп..." : "Сложить"));
        if (this.error$dropChestBtn != null) this.error$dropChestBtn.setMessage(Component.literal(this.error$isDroppingChest ? "§cСтоп..." : "Выкинуть"));
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void error$onRemoved(CallbackInfo ci) {
        this.error$isDroppingInv = false;
        this.error$isLootingChest = false;
        this.error$isStoringChest = false;
        this.error$dropChestBtn = null;
        this.error$cheapSlot = null;
        this.error$goodSlot = null;
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onItemScrollerMouseDragged(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (ItemScroller.INSTANCE != null && ItemScroller.INSTANCE.isEnabled()) {
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            if (ItemScroller.INSTANCE.onMouseDragged((AbstractContainerScreen<?>) (Object) this, slot, event)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onItemScrollerMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ItemScroller.INSTANCE != null && ItemScroller.INSTANCE.isEnabled()) {
            Slot slot = this.getHoveredSlot(event.x(), event.y());
            ItemScroller.INSTANCE.onMouseClicked(slot);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onItemScrollerMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ItemScroller.INSTANCE != null) {
            ItemScroller.INSTANCE.onMouseReleased();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onItemScrollerKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ItemScroller.INSTANCE != null && ItemScroller.INSTANCE.isEnabled()) {
            if (ItemScroller.INSTANCE.onKeyPressed((AbstractContainerScreen<?>) (Object) this, this.hoveredSlot, event)) {
                cir.setReturnValue(true);
            }
        }
    }
}