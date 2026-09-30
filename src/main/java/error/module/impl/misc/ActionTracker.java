package error.module.impl.misc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.client.persiki.ChatUtil;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;

import java.util.*;

/**
 * Create by daun kvass
 */
public class ActionTracker extends Module {
    public static ActionTracker INSTANCE;

    public final MultiModeSetting display = multiMode("Notifications", List.of("Chat", "Notify"), "Chat", "Notify");

    private final HeaderSetting useHeader = header("Usage");
    private final CheckBox trackGapple = checkbox("Golden Apple", true);
    private final CheckBox trackCrapple = checkbox("Enchanted Apple", true);
    private final CheckBox trackPearl = checkbox("Ender Pearl", true);
    private final CheckBox trackDrinkPotion = checkbox("Drinking Potions", true);

    private final HeaderSetting totemHeader = header("Totems");
    private final CheckBox trackTotem = checkbox("Totem Pop", true);
    private final CheckBox checkEnchant = checkbox("Check Enchanted", true);

    private final HeaderSetting splashHeader = header("Splash Potions");
    private final CheckBox trackSplashes = checkbox("Potion Hits", true);
    private final SliderSetting trackRadius = slider("Tracking Radius", 50.0f, 10.0f, 100.0f, 5.0f);

    private final Map<String, PlayerItemState> playerStates = new HashMap<>();
    private final Map<String, Long> notificationCooldowns = new HashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MS = 1000L;

    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private static final double SPLASH_RADIUS = 4.0;
    private static final double SPLASH_HEIGHT = 2.0;

    public ActionTracker() {
        super("ActionTracker", "Tracks enemy actions (Totems, Potions, Items)", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        playerStates.clear();
        notificationCooldowns.clear();
        trackedPotions.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        handleItemUsage(currentTime);

        if (trackSplashes.getValue()) {
            handlePotionTracking();
        }
    }

    @EventTarget
    public void onPacketReceive(PacketEvent event) {
        if (!inGame() || player() == null || mc.level == null) return;

        if (trackTotem.getValue() && event.getPacket() instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEventId() == 35) {
                Entity entity = packet.getEntity(mc.level);
                if (entity instanceof Player poppedPlayer && poppedPlayer != player()) {
                    String name = poppedPlayer.getName().getString();
                    boolean enchanted = checkEnchant.getValue() && hasEnchantedTotem(poppedPlayer);

                    String chatMsg;
                    String notifyMsg;

                    if (Localization.isRussian()) {
                        chatMsg = "Игрок §f" + name + "§r потерял тотем!" +
                                (checkEnchant.getValue() ? " §7[Зачарован: " + (enchanted ? "§aДа" : "§cНет") + "§7]" : "");
                        notifyMsg = name + " потерял тотем" + (checkEnchant.getValue() && enchanted ? " (Зачарован)" : "");
                    } else {
                        chatMsg = "Player §f" + name + "§r popped a totem!" +
                                (checkEnchant.getValue() ? " §7[Enchanted: " + (enchanted ? "§aYes" : "§cNo") + "§7]" : "");
                        notifyMsg = name + " popped a totem" + (checkEnchant.getValue() && enchanted ? " (Enchanted)" : "");
                    }

                    sendNotification(Localization.get("Totem Pop"), chatMsg, notifyMsg, ColorUtil.rgba(235, 75, 75, 255));
                }
            }
        }
    }

    private void handleItemUsage(long currentTime) {
        for (Player target : mc.level.players()) {
            if (target == null || target == player() || !target.isAlive()) continue;
            if (player().distanceToSqr(target) > trackRadius.getValue() * trackRadius.getValue()) continue;

            String playerName = target.getName().getString();
            PlayerItemState state = playerStates.computeIfAbsent(playerName, k -> new PlayerItemState());
            if (target.isUsingItem()) {
                ItemStack activeStack = target.getUseItem();
                if (!activeStack.isEmpty()) {
                    if (!state.isUsing || !ItemStack.matches(state.lastUsedStack, activeStack)) {
                        state.isUsing = true;
                        state.lastUsedStack = activeStack.copy();
                        state.useStartTime = currentTime;
                    }
                }
            } else {
                if (state.isUsing && state.lastUsedStack != null) {
                    long duration = currentTime - state.useStartTime;
                    if (duration > 150) {
                        checkFinishedItem(playerName, state.lastUsedStack, currentTime);
                    }
                    state.isUsing = false;
                    state.lastUsedStack = null;
                }
            }

            if (trackPearl.getValue()) {
                ItemStack mainHand = target.getMainHandItem();
                ItemStack offHand = target.getOffhandItem();

                int currentCount = 0;
                if (mainHand.is(Items.ENDER_PEARL)) currentCount += mainHand.getCount();
                if (offHand.is(Items.ENDER_PEARL)) currentCount += offHand.getCount();

                if (state.lastPearlCount > currentCount && state.lastPearlCount > 0) {
                    sendUseMessage(playerName, "Ender Pearl", "§d", currentTime);
                }
                state.lastPearlCount = currentCount;
            }
        }

        notificationCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > NOTIFICATION_COOLDOWN_MS * 2);
    }

    private void checkFinishedItem(String playerName, ItemStack stack, long currentTime) {
        if (stack.is(Items.GOLDEN_APPLE) && trackGapple.getValue()) {
            sendUseMessage(playerName, "Golden Apple", "§6", currentTime);
        } else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE) && trackCrapple.getValue()) {
            sendUseMessage(playerName, "Enchanted Apple", "§d", currentTime);
        } else if (stack.is(Items.POTION) && trackDrinkPotion.getValue()) {
            String potName = stack.getHoverName().getString();
            sendUseMessage(playerName, "Potion (" + potName + ")", "§b", currentTime);
        }
    }

    private void sendUseMessage(String playerName, String rawItemName, String colorCode, long currentTime) {
        String key = playerName + ":" + rawItemName;
        Long lastTime = notificationCooldowns.get(key);
        if (lastTime != null && currentTime - lastTime < NOTIFICATION_COOLDOWN_MS) return;

        String localizedItem = Localization.get(rawItemName);

        String chatMsg;
        String notifyMsg;

        if (Localization.isRussian()) {
            chatMsg = "§f" + playerName + "§7 использовал " + colorCode + localizedItem;
            notifyMsg = playerName + " использовал " + localizedItem;
        } else {
            chatMsg = "§f" + playerName + "§7 used " + colorCode + localizedItem;
            notifyMsg = playerName + " used " + localizedItem;
        }

        sendNotification(Localization.get("Usage"), chatMsg, notifyMsg, Theme.getAccentColor());
        notificationCooldowns.put(key, currentTime);
    }

    private void handlePotionTracking() {
        Set<Integer> currentPotionIds = new HashSet<>();
        AABB searchBox = player().getBoundingBox().inflate(trackRadius.getValue());

        List<ThrownSplashPotion> potions = mc.level.getEntitiesOfClass(ThrownSplashPotion.class, searchBox, Entity::isAlive);
        for (ThrownSplashPotion potion : potions) {
            PotionInfo info = detectPotionInfo(potion.getItem());
            if (info == null) continue;

            int id = potion.getId();
            currentPotionIds.add(id);

            PotionData data = trackedPotions.get(id);
            if (data == null) {
                trackedPotions.put(id, new PotionData(info, potion.position()));
            } else {
                data.lastPos = potion.position();
                data.info = info;
            }
        }

        Set<Integer> removed = new HashSet<>(trackedPotions.keySet());
        removed.removeAll(currentPotionIds);

        for (int id : removed) {
            PotionData data = trackedPotions.remove(id);
            if (data != null) {
                processPotionSplash(data);
            }
        }
    }

    private void processPotionSplash(PotionData data) {
        Vec3 pos = data.lastPos;
        AABB splashBox = new AABB(
                pos.x - SPLASH_RADIUS, pos.y - SPLASH_HEIGHT, pos.z - SPLASH_RADIUS,
                pos.x + SPLASH_RADIUS, pos.y + SPLASH_HEIGHT, pos.z + SPLASH_RADIUS
        );

        List<PlayerHit> hits = new ArrayList<>();
        for (Player p : mc.level.players()) {
            if (p == null || !p.isAlive() || !splashBox.contains(p.position())) continue;

            double dx = p.getX() - pos.x;
            double dz = p.getZ() - pos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > SPLASH_RADIUS) continue;

            double proximity = Math.max(0.0, 1.0 - (dist / SPLASH_RADIUS));
            int percent = Math.max(1, Math.min(100, (int) Math.round(proximity * 100.0)));
            hits.add(new PlayerHit(p.getName().getString(), percent, dist));
        }

        hits.sort(Comparator.comparingDouble(PlayerHit::distance));

        String potionName = Localization.get(data.info.displayName);

        for (int i = 0; i < Math.min(4, hits.size()); i++) {
            PlayerHit hit = hits.get(i);

            String chatMsg;
            String notifyMsg;

            if (Localization.isRussian()) {
                chatMsg = "§f" + hit.name + "§7 получил " + potionName + " §7" + hit.percent + "%";
                notifyMsg = hit.name + " получил " + potionName + " (" + hit.percent + "%)";
            } else {
                chatMsg = "§f" + hit.name + "§7 received " + potionName + " §7" + hit.percent + "%";
                notifyMsg = hit.name + " received " + potionName + " (" + hit.percent + "%)";
            }

            sendNotification(Localization.get("Splash Potions"), chatMsg, notifyMsg, data.info.startColor);
        }
    }

    private void sendNotification(String title, String chatText, String notifyText, int color) {
        if (display.isEnabled("Chat") || display.isEnabled("Чат")) {
            ChatUtil.info(chatText);
        }
        if (display.isEnabled("Notify")) {
            Notify.add(title, notifyText, IconUse.INFO, color);
        }
    }

    private PotionInfo detectPotionInfo(ItemStack stack) {
        if (stack.isEmpty()) return null;

        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("святая") || name.contains("вода") || name.contains("holy")) return PotionInfo.HOLY_WATER;
        if (name.contains("гнев") || name.contains("гнёва") || name.contains("wrath")) return PotionInfo.WRATH;
        if (name.contains("паладин") || name.contains("paladin")) return PotionInfo.PALADIN;
        if (name.contains("ассасин") || name.contains("assassin")) return PotionInfo.ASSASSIN;

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.hasEffects()) {
            for (MobEffectInstance inst : contents.getAllEffects()) {
                if (inst.getEffect() == MobEffects.REGENERATION && inst.getAmplifier() >= 1) return PotionInfo.HOLY_WATER;
                if (inst.getEffect() == MobEffects.STRENGTH && inst.getAmplifier() >= 4) return PotionInfo.WRATH;
                if (inst.getEffect() == MobEffects.HEALTH_BOOST && inst.getAmplifier() >= 2) return PotionInfo.PALADIN;
                if (inst.getEffect() == MobEffects.STRENGTH && inst.getAmplifier() >= 3) return PotionInfo.ASSASSIN;
            }
        }

        return null;
    }

    private boolean hasEnchantedTotem(Player p) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = p.getItemBySlot(slot);
            if (stack.is(Items.TOTEM_OF_UNDYING) && stack.isEnchanted()) {
                return true;
            }
        }
        return false;
    }

    private static class PlayerItemState {
        boolean isUsing = false;
        ItemStack lastUsedStack = null;
        long useStartTime = 0;
        int lastPearlCount = 0;
    }

    private static class PotionData {
        PotionInfo info;
        Vec3 lastPos;

        PotionData(PotionInfo info, Vec3 lastPos) {
            this.info = info;
            this.lastPos = lastPos;
        }
    }

    private record PlayerHit(String name, int percent, double distance) {}

    private enum PotionInfo {
        HOLY_WATER("Holy Water", ColorUtil.rgba(245, 245, 100, 255), ColorUtil.rgba(180, 255, 65, 255)),
        WRATH("Potion of Wrath", ColorUtil.rgba(220, 30, 30, 255), ColorUtil.rgba(255, 175, 60, 255)),
        PALADIN("Potion of Paladin", ColorUtil.rgba(185, 255, 65, 255), ColorUtil.rgba(255, 240, 160, 255)),
        ASSASSIN("Potion of Assassin", ColorUtil.rgba(130, 130, 130, 255), ColorUtil.rgba(180, 40, 40, 255));

        final String displayName;
        final int startColor;
        final int endColor;

        PotionInfo(String displayName, int startColor, int endColor) {
            this.displayName = displayName;
            this.startColor = startColor;
            this.endColor = endColor;
        }
    }
}