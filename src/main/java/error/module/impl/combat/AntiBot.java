package error.module.impl.combat;

import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.friend.FriendManager;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.ModeSetting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AntiBot module ported from Energy client.
 * Detects server bots and fake players, prevents targeting them, with optional removal from world.
 */
public class AntiBot extends Module {
    public static AntiBot INSTANCE;

    public static final List<Player> BOTS = new CopyOnWriteArrayList<>();

    public final ModeSetting mode = mode("Режим", "Auto", "Auto", "ReallyWorld", "LonyGrief", "Generic");
    public final CheckBox removeFromWorld = checkbox("Удалять из мира", false);

    public AntiBot() {
        super("AntiBot", "Не дает бить ботов", Category.COMBAT);
        INSTANCE = this;
    }

    @Override
    protected void onEnable() {
        BOTS.clear();
    }

    @Override
    protected void onDisable() {
        BOTS.clear();
    }

    public static boolean isBot(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
        if (!(entity instanceof Player player)) return false;
        return BOTS.contains(player);
    }

    private boolean isServer(String domain) {
        if (mc.getCurrentServer() == null || mc.getCurrentServer().ip == null) return false;
        return mc.getCurrentServer().ip.toLowerCase().contains(domain.toLowerCase());
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) return;

        String currentMode = mode.getValue();
        if (currentMode.equalsIgnoreCase("Auto")) {
            if (isServer("reallyworld")) {
                currentMode = "ReallyWorld";
            } else if (isServer("lonygrief")) {
                currentMode = "LonyGrief";
            } else {
                currentMode = "Generic";
            }
        }

        switch (currentMode) {
            case "ReallyWorld" -> checkReallyWorld();
            case "LonyGrief" -> checkLonyGrief();
            default -> checkGeneric();
        }

        if (removeFromWorld.getValue() && !BOTS.isEmpty()) {
            for (Player bot : BOTS) {
                if (bot != null && !bot.isRemoved()) {
                    mc.level.removeEntity(bot.getId(), Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    private void checkReallyWorld() {
        for (Player other : mc.level.players()) {
            if (other == player() || other.isSpectator()) continue;

            ItemStack feet = other.getItemBySlot(EquipmentSlot.FEET);
            ItemStack legs = other.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack chest = other.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack head = other.getItemBySlot(EquipmentSlot.HEAD);
            ItemStack offHand = other.getOffhandItem();
            ItemStack mainHand = other.getMainHandItem();

            boolean hasAllArmor = !feet.isEmpty() && !legs.isEmpty() && !chest.isEmpty() && !head.isEmpty();
            boolean isLeatherOrIron = isLeatherOrIronPiece(feet) && isLeatherOrIronPiece(legs) && isLeatherOrIronPiece(chest) && isLeatherOrIronPiece(head);
            boolean isUndamaged = !feet.isDamaged() && !legs.isDamaged() && !chest.isDamaged() && !head.isDamaged();
            boolean fullFood = other.getFoodData().getFoodLevel() == 20;

            if (hasAllArmor && isLeatherOrIron && isUndamaged && offHand.isEmpty() && !mainHand.isEmpty() && fullFood) {
                if (!BOTS.contains(other)) {
                    BOTS.add(other);
                }
            } else {
                BOTS.remove(other);
            }
        }
    }

    private boolean isLeatherOrIronPiece(ItemStack stack) {
        return stack.is(Items.LEATHER_BOOTS) || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_CHESTPLATE) || stack.is(Items.LEATHER_HELMET)
                || stack.is(Items.IRON_BOOTS) || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_CHESTPLATE) || stack.is(Items.IRON_HELMET);
    }

    private void checkLonyGrief() {
        for (Player other : mc.level.players()) {
            if (other == player() || other.isSpectator()) continue;

            ItemStack feet = other.getItemBySlot(EquipmentSlot.FEET);
            ItemStack legs = other.getItemBySlot(EquipmentSlot.LEGS);
            ItemStack chest = other.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack head = other.getItemBySlot(EquipmentSlot.HEAD);

            boolean noDamagedArmor = !feet.isDamaged() && !legs.isDamaged() && !chest.isDamaged() && !head.isDamaged();
            boolean noEnchantedArmor = !feet.hasFoil() && !legs.hasFoil() && !chest.hasFoil() && !head.hasFoil();
            boolean hasArmor = other.getArmorValue() > 0;
            boolean nameLen6 = other.getName().getString().length() == 6;
            boolean isFriend = FriendManager.getInstance().isFriend(other);

            if (nameLen6 && !isFriend && hasArmor && noDamagedArmor && noEnchantedArmor) {
                if (!BOTS.contains(other)) {
                    BOTS.add(other);
                }
            } else {
                BOTS.remove(other);
            }
        }
    }

    private void checkGeneric() {
        for (Player other : mc.level.players()) {
            if (other == player() || other.isSpectator()) continue;

            UUID offlineUuid = UUIDUtil.createOfflinePlayerUUID(other.getName().getString());
            boolean isOfflineMismatch = !other.getUUID().equals(offlineUuid);
            boolean notInTabList = mc.getConnection() != null && mc.getConnection().getPlayerInfo(other.getUUID()) == null;

            if ((isOfflineMismatch || notInTabList) && !BOTS.contains(other)) {
                BOTS.add(other);
            }
        }
    }
}
