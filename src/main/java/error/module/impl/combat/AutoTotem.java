package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerInputEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import error.setting.impl.SliderSetting;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

@Getter
public class AutoTotem extends Module {
    public static AutoTotem INSTANCE;

    public final ModeSetting modeSetting = mode("Режим", "Matrix", "Matrix", "Default");
    public final SliderSetting floatSetting = slider("Здоровье", 5.0F, 1.0F, 20.0F, 0.5F);
    public final CheckBox booleanSetting = checkbox("Возвращать предмет", true);
    public final CheckBox booleanSetting2 = checkbox("Тотем при ТНТ/трезубце рядом", true);
    public final SliderSetting floatSetting2 = slider("Радиус угрозы", 6.0F, 2.0F, 16.0F, 0.5F);
    public final CheckBox booleanSetting3 = checkbox("Не во время еды", false);
    public final CheckBox booleanSetting4 = checkbox("Умное перемещение", true);

    private boolean flag;
    private boolean flag2;
    private int index = -1;
    private ItemStack itemStack = ItemStack.EMPTY;
    private boolean flag3 = false;
    private boolean flag4 = false;
    private boolean flag5 = false;
    private int index2 = 0;
    private int index3 = -1;
    private boolean flag6 = false;
    private int index4 = 0;
    private boolean flag7 = false;
    private boolean flag8;
    private final float[] volume = new float[20];

    public AutoTotem() {
        super("AutoTotem", "Умная установка тотема с возвратом предмета", Category.COMBAT);
        INSTANCE = this;
    }

    private boolean checkState() {
        return this.modeSetting.is("Matrix");
    }

    @Override
    public void onDisable() {
        this.checkState2();
        this.updateState8();
        this.flag5 = false;
        this.index2 = 0;
        this.index4 = 0;
        this.updateState2();
        super.onDisable();
    }

    private void updateState() {
        if (this.checkState()) {
            if (!this.flag7) {
                this.flag7 = true;
            }
        }
    }

    private void updateState2() {
        if (this.flag7) {
            this.flag7 = false;
        }
    }

    public static boolean isSwapping() {
        return INSTANCE != null && INSTANCE.isEnabled() && (INSTANCE.flag5 || INSTANCE.index4 > 0 || INSTANCE.index3 != -1);
    }

    @EventTarget
    public void onInput(PlayerInputEvent event) {
        if (this.checkState()) {
            if ((this.index4 > 0 || this.flag5) && mc.player != null) {
                mc.player.setSprinting(false);
                event.setSprinting(false);
                if (mc.player.onGround()) {
                    event.setMoveVector(new net.minecraft.world.phys.Vec2(0.0F, 0.0F));
                    if (event.getKeyPresses() != null) {
                        event.setKeyPresses(new net.minecraft.world.entity.player.Input(
                                false, false, false, false, false, false, false
                        ));
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE) return;

        if (this.index4 > 0) {
            this.index4--;
            if (this.index4 == 0) {
                this.updateState2();
            }
        }

        if (this.flag5) {
            this.updateState4();
            this.index2--;
            if (this.index2 <= 0) {
                this.flag5 = false;
                this.updateState6();
            }
        } else if (mc.player != null && mc.level != null && mc.gameMode != null) {
            System.arraycopy(this.volume, 0, this.volume, 1, 19);
            this.volume[0] = mc.player.getHealth() + (mc.player.hasEffect(MobEffects.ABSORPTION) ? mc.player.getAbsorptionAmount() : 0.0F);
            if (!mc.player.getAbilities().instabuild && !mc.player.isSpectator()) {
                this.updateState3();
                if (!this.checkState3()) {
                    this.flag2 = false;
                    this.updateState7();
                } else if (!this.flag2) {
                    this.updateState5();
                    if (this.flag && this.checkCondition5(this.itemStack)) {
                        this.updateState7();
                    }
                }
            } else {
                this.updateState7();
            }
        } else {
            this.updateState8();
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.isReceive() && event.is(ClientboundEntityEventPacket.class) && mc.player != null && mc.level != null) {
            ClientboundEntityEventPacket p = event.get(ClientboundEntityEventPacket.class);
            if (p.getEventId() == 35 && p.getEntity(mc.level) == mc.player) {
                if (this.checkState3()) {
                    this.updateState5();
                } else if (this.booleanSetting.getValue() && this.flag && !this.itemStack.isEmpty() && this.checkState2()) {
                    this.flag2 = true;
                }
            }
        }
    }

    private void updateState3() {
        if (mc.player != null) {
            ItemStack var1 = mc.player.getOffhandItem();
            if (!var1.isEmpty()) {
                if (!this.checkCondition6(var1)) {
                    if (this.itemStack.isEmpty() || !this.checkCondition(var1, this.itemStack)) {
                        this.itemStack = var1.copy();
                        this.flag3 = this.checkCondition5(var1);
                        this.flag4 = false;
                        this.flag = true;
                        this.index = this.resolveInt(var1);
                    }
                }
            }
        }
    }

    private int resolveInt(ItemStack target) {
        if (target != null && !target.isEmpty() && mc.player != null) {
            for (int var2 = 0; var2 < 36; var2++) {
                ItemStack var3 = mc.player.getInventory().getItem(var2);
                if (this.checkCondition(var3, target)) {
                    return var2;
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    private boolean checkCondition(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a.isEmpty() && b.isEmpty()) {
            return true;
        } else {
            return !a.isEmpty() && !b.isEmpty()
                    ? a.is(b.getItem()) && a.getCount() == b.getCount() && a.getComponents().equals(b.getComponents())
                    : false;
        }
    }

    private void updateState4() {
        if (this.checkState()) {
            if (mc.player != null) {
                mc.player.setSprinting(false);
                if (mc.options != null) {
                    mc.options.keySprint.setDown(false);
                }

                if (mc.player.onGround()) {
                    if (mc.options != null) {
                        mc.options.keyUp.setDown(false);
                        mc.options.keyDown.setDown(false);
                        mc.options.keyLeft.setDown(false);
                        mc.options.keyRight.setDown(false);
                        mc.options.keyJump.setDown(false);
                    }
                }
            }
        }
    }

    private void updateState5() {
        if (mc.player == null) return;
        ItemStack var1 = mc.player.getOffhandItem();
        if (!this.checkCondition6(var1)) {
            if (!this.checkCondition7(var1)) {
                int var3 = this.resolveInt4();
                if (var3 != -1) {
                    if (!var1.isEmpty()) {
                        this.itemStack = var1.copy();
                        this.flag3 = false;
                        this.flag = true;
                    }

                    this.index3 = var3;
                    this.flag6 = false;
                    this.flag5 = true;
                    this.index2 = 2;
                    this.updateState();
                    this.updateState4();
                }
            } else if (!this.flag4 || !this.checkCondition2(var1)) {
                int var2 = this.checkCondition8(var1) ? this.resolveInt3() : this.resolveInt2();
                if (var2 != -1) {
                    this.itemStack = var1.copy();
                    this.flag3 = true;
                    this.flag = true;
                    this.index3 = var2;
                    this.flag6 = false;
                    this.flag5 = true;
                    this.index2 = 2;
                    this.updateState();
                    this.updateState4();
                } else {
                    if (this.itemStack.isEmpty() || !this.checkCondition(var1, this.itemStack)) {
                        this.itemStack = var1.copy();
                        this.flag3 = true;
                        this.flag = true;
                        this.index = this.resolveInt(var1);
                    }
                }
            }
        }
    }

    private void updateState6() {
        if (this.index3 != -1) {
            if (this.flag6) {
                if (swapToOffhand(this.index3)) {
                    this.index4 = this.checkState() ? 2 : 0;
                    if (this.checkState3() && this.checkCondition5(this.itemStack)) {
                        this.flag4 = true;
                    } else {
                        this.itemStack = ItemStack.EMPTY;
                        this.flag3 = false;
                        this.flag4 = false;
                        this.flag = false;
                        this.index = -1;
                    }
                }
            } else if (swapToOffhand(this.index3)) {
                this.index4 = this.checkState() ? 2 : 0;
                this.index = this.index3;
            }

            this.index3 = -1;
            this.flag6 = false;
        }
    }

    private static int toContainerSlot(int inventoryIndex) {
        return inventoryIndex < 9 ? inventoryIndex + 36 : inventoryIndex;
    }

    private static boolean swapToOffhand(int inventoryIndex) {
        if (mc.player == null || mc.gameMode == null) {
            return false;
        }
        if (inventoryIndex < 0 || inventoryIndex > 35) {
            return false;
        }
        int containerSlot = toContainerSlot(inventoryIndex);
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, containerSlot, 40, ContainerInput.SWAP, mc.player);
        return true;
    }

    private int resolveInt2() {
        if (mc.player == null) return -1;
        for (int var1 = 0; var1 < 36; var1++) {
            if (this.checkCondition6(mc.player.getInventory().getItem(var1))) {
                return var1;
            }
        }
        return -1;
    }

    private int resolveInt3() {
        int var1 = this.resolveInt2();
        if (var1 != -1) {
            return var1;
        }

        if (mc.player == null) return -1;
        for (int var2 = 0; var2 < 36; var2++) {
            ItemStack var3 = mc.player.getInventory().getItem(var2);
            if (this.checkCondition7(var3) && !this.checkCondition8(var3)) {
                return var2;
            }
        }

        return -1;
    }

    private int resolveInt4() {
        if (mc.player == null) return -1;
        for (int var1 = 0; var1 < 36; var1++) {
            ItemStack var2 = mc.player.getInventory().getItem(var1);
            if (this.checkCondition6(var2)) {
                return var1;
            }
        }

        for (int var3 = 0; var3 < 36; var3++) {
            ItemStack var5 = mc.player.getInventory().getItem(var3);
            if (this.checkCondition7(var5) && !this.checkCondition8(var5)) {
                return var3;
            }
        }

        for (int var4 = 0; var4 < 36; var4++) {
            ItemStack var6 = mc.player.getInventory().getItem(var4);
            if (this.checkCondition8(var6)) {
                return var4;
            }
        }

        return -1;
    }

    private void updateState7() {
        if (this.booleanSetting.getValue()) {
            if (this.itemStack.isEmpty()) {
                this.flag4 = false;
            } else if (mc.player != null) {
                ItemStack var1 = mc.player.getOffhandItem();
                if (!var1.isEmpty() && this.checkCondition3(var1, this.itemStack)) {
                    this.itemStack = ItemStack.EMPTY;
                    this.flag3 = false;
                    this.flag4 = false;
                    this.flag = false;
                    this.index = -1;
                } else if (!this.flag4) {
                    if (this.checkCondition5(this.itemStack) || !this.checkState3()) {
                        this.checkState2();
                    }
                }
            }
        }
    }

    private boolean checkState2() {
        if (mc.player == null || mc.gameMode == null) {
            return false;
        } else if (this.itemStack.isEmpty()) {
            return false;
        } else {
            int var1 = this.resolveInt5(this.itemStack);
            if (var1 == -1) {
                this.itemStack = ItemStack.EMPTY;
                this.flag3 = false;
                this.flag4 = false;
                this.flag = false;
                this.index = -1;
                return false;
            } else {
                this.index3 = var1;
                this.flag6 = true;
                this.flag5 = true;
                this.index2 = 2;
                this.updateState();
                this.updateState4();
                return true;
            }
        }
    }

    private boolean checkCondition2(ItemStack stack) {
        return !this.itemStack.isEmpty() && this.checkCondition3(stack, this.itemStack);
    }

    private int resolveInt5(ItemStack target) {
        if (target != null && !target.isEmpty() && mc.player != null) {
            for (int var2 = 0; var2 < 36; var2++) {
                ItemStack var3 = mc.player.getInventory().getItem(var2);
                if (this.checkCondition3(var3, target)) {
                    return var2;
                }
            }
            return -1;
        } else {
            return -1;
        }
    }

    private boolean checkCondition3(ItemStack a, ItemStack b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else if (a.isEmpty() && b.isEmpty()) {
            return true;
        } else {
            return !a.isEmpty() && !b.isEmpty() ? a.is(b.getItem()) && a.getComponents().equals(b.getComponents()) : false;
        }
    }

    private boolean checkState3() {
        if (mc.player == null) {
            return false;
        }

        if (this.booleanSetting3.getValue() && this.checkState5()) {
            return this.flag8;
        }

        if (this.booleanSetting4.getValue() && this.checkState6()) {
            return this.flag8;
        }

        boolean var1 = this.checkState4();
        if (var1 != this.flag8) {
            this.flag8 = var1;
        }

        return var1 || (this.booleanSetting2.getValue() && this.checkState7());
    }

    private boolean checkState4() {
        float var1 = mc.player.getHealth() + (mc.player.hasEffect(MobEffects.ABSORPTION) ? mc.player.getAbsorptionAmount() : 0.0F);
        float var2 = this.flag8 ? this.floatSetting.getValue() + 0.5F : this.floatSetting.getValue();
        return var1 <= var2;
    }

    private boolean checkState5() {
        ItemStack var1 = mc.player.getUseItem();
        return mc.player.isUsingItem() && !var1.isEmpty() && var1.get(DataComponents.FOOD) != null;
    }

    private boolean checkState6() {
        ItemStack var1 = mc.player.getUseItem();
        if (this.checkState5() && var1.getUseDuration(mc.player) > 5) {
            int var2 = mc.player.getUseItemRemainingTicks();
            return var2 < 10 ? true : var2 < 30 && (this.volume[19] - this.volume[0]) / 20.0F * var2 < this.volume[0];
        } else {
            return false;
        }
    }

    private boolean checkState7() {
        if (mc.player != null && mc.level != null) {
            AABB var1 = mc.player.getBoundingBox().inflate(this.floatSetting2.getValue());
            if (!mc.level.getEntitiesOfClass(PrimedTnt.class, var1, Entity::isAlive).isEmpty()) {
                return true;
            }

            for (ThrownTrident var3 : mc.level.getEntitiesOfClass(ThrownTrident.class, var1, Entity::isAlive)) {
                if (!this.checkCondition4(var3) && var3.getDeltaMovement().lengthSqr() > 0.05) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    private boolean checkCondition4(ThrownTrident trident) {
        if (mc.player == null) {
            return false;
        }

        if (trident.getOwner() == mc.player) {
            return true;
        }

        if (trident.getOwner() != null) {
            return false;
        }

        Vec3 var2 = trident.position().subtract(mc.player.getEyePosition());
        return var2.lengthSqr() > 9.0 ? false : trident.getDeltaMovement().normalize().dot(var2.normalize()) > 0.5;
    }

    private boolean checkCondition5(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    private boolean checkCondition6(ItemStack stack) {
        return this.checkCondition5(stack) && !stack.isEnchanted();
    }

    private boolean checkCondition7(ItemStack stack) {
        return this.checkCondition5(stack) && stack.isEnchanted();
    }

    private boolean checkCondition8(ItemStack stack) {
        if (!this.checkCondition5(stack)) {
            return false;
        }

        String var2 = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return var2.contains("infinity") || var2.contains("eternity") || var2.contains("stinger");
    }

    private void updateState8() {
        this.flag = false;
        this.flag2 = false;
        this.index = -1;
        this.itemStack = ItemStack.EMPTY;
        this.flag3 = false;
        this.flag4 = false;
        this.flag8 = false;
    }
}