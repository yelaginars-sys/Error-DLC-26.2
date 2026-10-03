package error.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
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

public class ActionTracker extends Module {
    public static ActionTracker INSTANCE;

    public final MultiModeSetting display = multiMode("Notifications", List.of("Chat", "Notify"), "Chat", "Notify");
    private final SliderSetting trackRadius = slider("Tracking Radius", 30.0f, 10.0f, 100.0f, 5.0f);

    private final HeaderSetting useHeader = header("Usage");
    private final CheckBox trackGapple = checkbox("Golden Apple", true);
    private final CheckBox trackCrapple = checkbox("Enchanted Apple", true);
    private final CheckBox trackPearl = checkbox("Ender Pearl", true);
    private final CheckBox trackDrinkPotion = checkbox("Drinking Potions", true);

    private final HeaderSetting totemHeader = header("Totems");
    private final CheckBox trackTotem = checkbox("Totem Pop", true);
    private final CheckBox checkEnchant = checkbox("Check Enchanted", true);

    private final HeaderSetting splashHeader = header("Custom Splash Potions");
    private final CheckBox trackSplashes = checkbox("Potion Hits %", true);

    private final HeaderSetting serverHeader = header("ServerHelper Items");
    private final CheckBox trackDisorient = checkbox("Дезориентация", true);
    private final CheckBox trackTrap = checkbox("Трапка", true);
    private final CheckBox trackDust = checkbox("Явная пыль", true);
    private final CheckBox trackGodAura = checkbox("Божья Аура", true);
    private final CheckBox trackStun = checkbox("Стан", true);
    private final CheckBox trackPlast = checkbox("Пласт", true);

    private final Map<String, PlayerItemState> playerStates = new HashMap<>();
    private final Map<String, Long> notificationCooldowns = new HashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MS = 1000L;

    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private static final double SPLASH_RADIUS = 4.2;
    private static final double SPLASH_HEIGHT = 2.5;

    public ActionTracker() {
        super("UseTracker", "Отслеживание использования тотемов, зелий и яблок", Category.MISC);
        INSTANCE = this;
        setState(true);
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
                    double dist = player().distanceTo(poppedPlayer);
                    if (dist > trackRadius.getValue()) return;

                    String name = poppedPlayer.getName().getString();
                    boolean enchanted = checkEnchant.getValue() && hasEnchantedTotem(poppedPlayer);

                    // Exact format matching media_1790841241613.png
                    String dot = enchanted ? "§a●" : "§c●";
                    String chatText = name + " потерял Тотем бессмертия, зачарован: " + dot;
                    String toastTitle = name;
                    String toastDesc = "потерял Тотем!";
                    String hoverInfo = buildHoverInfo(poppedPlayer);

                    sendTrackerMessage(toastTitle, toastDesc, chatText, hoverInfo, ColorUtil.rgba(235, 75, 75, 255));
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

            // Check ServerHelper items held or used in hand
            checkServerItems(target, state, currentTime);

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
                        checkFinishedItem(target, state.lastUsedStack, currentTime);
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
                    sendUseMessage(target, "Эндер-жемчуг", currentTime);
                }
                state.lastPearlCount = currentCount;
            }
        }

        notificationCooldowns.entrySet().removeIf(entry -> currentTime - entry.getValue() > NOTIFICATION_COOLDOWN_MS * 2);
    }

    private void checkServerItems(Player target, PlayerItemState state, long currentTime) {
        ItemStack main = target.getMainHandItem();
        if (main == null || main.isEmpty()) return;
        String name = main.getHoverName().getString().toLowerCase(Locale.ROOT);

        if (trackDisorient.getValue() && (name.contains("дезориентация") || (main.is(Items.ENDER_EYE) && name.contains("дезор")))) {
            if (main.getCount() < state.lastDisorientCount && state.lastDisorientCount > 0) {
                sendUseMessage(target, "Дезориентация", currentTime);
            }
            state.lastDisorientCount = main.getCount();
        } else if (trackTrap.getValue() && (name.contains("трапка") || main.is(Items.NETHERITE_SCRAP))) {
            if (main.getCount() < state.lastTrapCount && state.lastTrapCount > 0) {
                sendUseMessage(target, "Трапка", currentTime);
            }
            state.lastTrapCount = main.getCount();
        } else if (trackDust.getValue() && (name.contains("пыль") || main.is(Items.SUGAR))) {
            if (main.getCount() < state.lastDustCount && state.lastDustCount > 0) {
                sendUseMessage(target, "Явная пыль", currentTime);
            }
            state.lastDustCount = main.getCount();
        } else if (trackGodAura.getValue() && (name.contains("божья аура") || main.is(Items.PHANTOM_MEMBRANE))) {
            if (main.getCount() < state.lastGodAuraCount && state.lastGodAuraCount > 0) {
                sendUseMessage(target, "Божья Аура", currentTime);
            }
            state.lastGodAuraCount = main.getCount();
        }
    }

    private void checkFinishedItem(Player target, ItemStack stack, long currentTime) {
        if (stack.is(Items.GOLDEN_APPLE) && trackGapple.getValue()) {
            sendUseMessage(target, "Золотое яблоко", currentTime);
        } else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE) && trackCrapple.getValue()) {
            sendUseMessage(target, "Зачарованное яблоко", currentTime);
        } else if (stack.is(Items.POTION) && trackDrinkPotion.getValue()) {
            String potName = stack.getHoverName().getString();
            sendUseMessage(target, "Зелье (" + potName + ")", currentTime);
        }
    }

    private void sendUseMessage(Player target, String itemName, long currentTime) {
        String playerName = target.getName().getString();
        String key = playerName + ":" + itemName;
        Long lastTime = notificationCooldowns.get(key);
        if (lastTime != null && currentTime - lastTime < NOTIFICATION_COOLDOWN_MS) return;

        // Exact chat format matching media_1790841295255.png: [UseTracker] » ser5pq использовал "Золотое яблоко"
        String chatText = playerName + " использовал \"" + itemName + "\"";
        String toastTitle = itemName;
        String toastDesc = "использовано!";
        String hoverInfo = buildHoverInfo(target);

        sendTrackerMessage(toastTitle, toastDesc, chatText, hoverInfo, Theme.getAccentColor());
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
                trackedPotions.put(id, new PotionData(info, potion.position(), potion.getItem().copy()));
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
            hits.add(new PlayerHit(p, percent, dist));
        }

        hits.sort(Comparator.comparingDouble(PlayerHit::distance));
        if (hits.isEmpty()) return;

        String potionTitle = data.info.displayName; // e.g. "Святая вода"

        for (int i = 0; i < Math.min(4, hits.size()); i++) {
            PlayerHit hit = hits.get(i);
            boolean isSelf = hit.player == player();
            String nameText = isSelf ? "Вы" : hit.player.getName().getString();

            // Exact chat format matching media_1790841506507.png:
            // "⚡ Fifi получил эффекты от [★] Святая вода (73%)" or "Вы получил эффекты от [★] Святая вода (82%)"
            String prefixSymbol = isSelf ? "" : "§c⚡ ";
            String chatMain = prefixSymbol + "§f" + nameText + "§r§7 получил эффекты от §e[★] " + potionTitle + " §7(" + hit.percent + "%)";

            if (display.isEnabled("Chat") || display.isEnabled("Чат")) {
                MutableComponent msg = Component.literal("[UseTracker] » ").withStyle(Style.EMPTY.withColor(0xFF8B5CF6).withBold(true))
                        .append(Component.literal(chatMain));
                if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.getChat() != null) {
                    mc.gui.hud.getChat().addClientSystemMessage(msg);

                    // Print applied effect details lines under splash chat message (media_1790841506507.png format)
                    for (PotionEffectDetail detail : data.info.effects) {
                        String detailText = "§7  - " + detail.name + " " + toRoman(detail.amplifier) + " (" + detail.durationText + ")";
                        mc.gui.hud.getChat().addClientSystemMessage(Component.literal(detailText));
                    }
                }
            }

            // Exact toast notification matching media_1790841538180.png:
            // "Fifi получил [★] Святая вода 73%!" or "Вы получил [★] Святая вода 82%!"
            if (display.isEnabled("Notify")) {
                String toastMsg = nameText + " получил [★] " + potionTitle + " " + hit.percent + "%!";
                Notify.add(toastMsg, "", IconUse.INFO, data.info.startColor);
            }
        }
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(num);
        };
    }

    private void sendTrackerMessage(String toastTitle, String toastDesc, String chatText, String hoverText, int color) {
        if (display.isEnabled("Chat") || display.isEnabled("Чат")) {
            MutableComponent prefix = Component.literal("[UseTracker] » ").withStyle(Style.EMPTY.withColor(0xFF8B5CF6).withBold(true));
            MutableComponent body = Component.literal(chatText).withStyle(Style.EMPTY.withColor(0xFFEEEEEE));

            if (hoverText != null && !hoverText.isBlank()) {
                body.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText))));
            }

            prefix.append(body);
            if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.getChat() != null) {
                mc.gui.hud.getChat().addClientSystemMessage(prefix);
            }
        }
        if (display.isEnabled("Notify")) {
            Notify.add(toastTitle, toastDesc, IconUse.CHECK, Notify.COLOR_SUCCESS);
        }
    }

    private PotionInfo detectPotionInfo(ItemStack stack) {
        if (stack.isEmpty()) return null;

        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (name.contains("святая") || name.contains("holy")) return PotionInfo.HOLY_WATER;
        if (name.contains("гнев") || name.contains("wrath")) return PotionInfo.WRATH;
        if (name.contains("паладин") || name.contains("paladin")) return PotionInfo.PALADIN;
        if (name.contains("ассасин") || name.contains("assassin")) return PotionInfo.ASSASSIN;
        if (name.contains("радиац") || name.contains("radiation")) return PotionInfo.RADIATION;
        if (name.contains("снотвор") || name.contains("sleep")) return PotionInfo.SLEEP;
        if (name.contains("хлопуш") || name.contains("clapper")) return PotionInfo.CLAPPER;

        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.hasEffects()) {
            for (MobEffectInstance inst : contents.getAllEffects()) {
                if (inst.getEffect() == MobEffects.REGENERATION && inst.getAmplifier() >= 1) return PotionInfo.HOLY_WATER;
                if (inst.getEffect() == MobEffects.STRENGTH && inst.getAmplifier() >= 3) return PotionInfo.WRATH;
                if (inst.getEffect() == MobEffects.RESISTANCE && inst.getAmplifier() >= 0) return PotionInfo.PALADIN;
                if (inst.getEffect() == MobEffects.SPEED && inst.getAmplifier() >= 1) return PotionInfo.ASSASSIN;
            }
        }

        return PotionInfo.HOLY_WATER; // Fallback
    }

    private String buildHoverInfo(Player player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("§d§l★ Информация об игроке:\n");
        sb.append("§fИгрок: §a").append(player.getName().getString()).append("\n");

        double dist = mc.player != null ? Math.sqrt(player.distanceToSqr(mc.player)) : 0;
        sb.append("§fДистанция: §e").append(String.format(Locale.ROOT, "%.1f", dist)).append("m\n");

        // Talisman / Offhand
        ItemStack offhand = player.getOffhandItem();
        String offhandName = offhand.isEmpty() ? "Пусто" : offhand.getHoverName().getString();
        boolean isTalisman = isTalismanItem(offhand);
        sb.append("§f").append(isTalisman ? "Талисман: §d" : "Левая рука: §b").append(offhandName).append("\n");

        // Active Effects & remaining durations
        sb.append("§fАктивные эффекты:\n");
        var effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            sb.append("  §7(Нет эффектов)\n");
        } else {
            for (MobEffectInstance effect : effects) {
                String effectName = effect.getEffect().value().getDescriptionId();
                effectName = Localization.get(effectName);
                int durationSeconds = effect.getDuration() / 20;
                int mins = durationSeconds / 60;
                int secs = durationSeconds % 60;
                String timeStr = String.format(Locale.ROOT, "%d:%02d", mins, secs);
                int amp = effect.getAmplifier() + 1;
                sb.append("  §e• ").append(effectName).append(" ").append(amp > 1 ? amp : "").append(" §7(").append(timeStr).append(")\n");
            }
        }

        // Armor Equipment
        sb.append("§fЭкипировка:\n");
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                String slotName = switch (slot) {
                    case HEAD -> "Шлем";
                    case CHEST -> "Нагрудник";
                    case LEGS -> "Поножи";
                    case FEET -> "Ботинки";
                    default -> slot.getName();
                };
                sb.append("  §b").append(slotName).append(": §f").append(armor.getHoverName().getString());
                if (armor.isEnchanted()) {
                    sb.append(" §7[Зачарован]");
                }
                sb.append("\n");
            }
        }

        return sb.toString().trim();
    }

    private boolean isTalismanItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return name.contains("талисман") || name.contains("talisman") || name.contains("сфера") || name.contains("шард") || name.contains("оберег") || name.contains("грааль");
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
        int lastDisorientCount = 0;
        int lastTrapCount = 0;
        int lastDustCount = 0;
        int lastGodAuraCount = 0;
    }

    private static class PotionData {
        PotionInfo info;
        Vec3 lastPos;
        ItemStack stack;

        PotionData(PotionInfo info, Vec3 lastPos, ItemStack stack) {
            this.info = info;
            this.lastPos = lastPos;
            this.stack = stack;
        }
    }

    private record PlayerHit(Player player, int percent, double distance) {}

    private record PotionEffectDetail(String name, int amplifier, String durationText) {}

    private enum PotionInfo {
        HOLY_WATER("Святая вода", ColorUtil.rgba(245, 245, 100, 255), List.of(
                new PotionEffectDetail("Регенерация", 2, "0:37"),
                new PotionEffectDetail("Невидимость", 2, "8:17")
        )),
        WRATH("Зелье Гнева", ColorUtil.rgba(220, 30, 30, 255), List.of(
                new PotionEffectDetail("Сила", 4, "0:45"),
                new PotionEffectDetail("Замедление", 3, "0:20")
        )),
        PALADIN("Зелье Паладина", ColorUtil.rgba(185, 255, 65, 255), List.of(
                new PotionEffectDetail("Сопротивление", 1, "1:00"),
                new PotionEffectDetail("Огнестойкость", 1, "3:00")
        )),
        ASSASSIN("Зелье Ассасина", ColorUtil.rgba(130, 130, 130, 255), List.of(
                new PotionEffectDetail("Сила", 3, "0:40"),
                new PotionEffectDetail("Скорость", 2, "1:30")
        )),
        RADIATION("Зелье Радиации", ColorUtil.rgba(80, 220, 80, 255), List.of(
                new PotionEffectDetail("Отравление", 1, "0:15"),
                new PotionEffectDetail("Иссушение", 1, "0:15")
        )),
        SLEEP("Снотворное", ColorUtil.rgba(140, 140, 200, 255), List.of(
                new PotionEffectDetail("Слабость", 1, "0:20"),
                new PotionEffectDetail("Усталость", 1, "0:20")
        )),
        CLAPPER("Хлопушка", ColorUtil.rgba(255, 150, 50, 255), List.of(
                new PotionEffectDetail("Замедление", 9, "0:05"),
                new PotionEffectDetail("Слепота", 9, "0:05")
        ));

        final String displayName;
        final int startColor;
        final List<PotionEffectDetail> effects;

        PotionInfo(String displayName, int startColor, List<PotionEffectDetail> effects) {
            this.displayName = displayName;
            this.startColor = startColor;
            this.effects = effects;
        }
    }
}