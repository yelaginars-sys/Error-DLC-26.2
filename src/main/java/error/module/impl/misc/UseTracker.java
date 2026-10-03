package error.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

import java.util.*;

public class UseTracker extends Module {
    public static UseTracker INSTANCE;

    public final MultiModeSetting display = multiMode("Уведомления", List.of("Chat", "Notify"), "Chat", "Notify");
    public final SliderSetting trackRadius = slider("Радиус отслеживания", 35.0f, 10.0f, 100.0f, 5.0f);

    private final HeaderSetting useHeader = header("Предметы");
    public final CheckBox trackGapple = checkbox("Золотое яблоко", true);
    public final CheckBox trackCrapple = checkbox("Зачарованное яблоко", true);
    public final CheckBox trackPearl = checkbox("Эндер-жемчуг", true);
    public final CheckBox trackChorus = checkbox("Хорус", true);
    public final CheckBox trackDrinkPotion = checkbox("Питьё зелий", true);

    private final HeaderSetting totemHeader = header("Тотемы");
    public final CheckBox trackTotem = checkbox("Потеря Тотема", true);
    public final CheckBox checkEnchant = checkbox("Проверка Зачарования", true);

    private final HeaderSetting serverHeader = header("Предметы анархий");
    public final CheckBox trackDisorient = checkbox("Дезориентация", true);
    public final CheckBox trackTrap = checkbox("Трапка", true);
    public final CheckBox trackDust = checkbox("Явная пыль", true);
    public final CheckBox trackGodAura = checkbox("Божья Аура", true);
    public final CheckBox trackStun = checkbox("Стан", true);
    public final CheckBox trackPlast = checkbox("Пласт", true);
    public final CheckBox trackShard = checkbox("Шарды и Обереги", true);

    private final Map<String, PlayerItemState> playerStates = new HashMap<>();
    private final Map<String, Long> notificationCooldowns = new HashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MS = 1000L;

    public UseTracker() {
        super("UseTracker", "Отслеживание использования тотемов, зелий, яблок и предметов анархий", Category.MISC);
        INSTANCE = this;
        setState(true);
    }

    @Override
    protected void onDisable() {
        playerStates.clear();
        notificationCooldowns.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        handleItemUsage(currentTime);
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
        } else if (trackShard.getValue() && (name.contains("шард") || name.contains("оберег") || name.contains("талисман"))) {
            if (main.getCount() < state.lastShardCount && state.lastShardCount > 0) {
                sendUseMessage(target, main.getHoverName().getString(), currentTime);
            }
            state.lastShardCount = main.getCount();
        }
    }

    private void checkFinishedItem(Player target, ItemStack stack, long currentTime) {
        if (stack.is(Items.GOLDEN_APPLE) && trackGapple.getValue()) {
            sendUseMessage(target, "Золотое яблоко", currentTime);
        } else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE) && trackCrapple.getValue()) {
            sendUseMessage(target, "Зачарованное яблоко", currentTime);
        } else if (stack.is(Items.CHORUS_FRUIT) && trackChorus.getValue()) {
            sendUseMessage(target, "Хорус", currentTime);
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

        String chatText = playerName + " использовал \"" + itemName + "\"";
        String toastTitle = itemName;
        String toastDesc = "использовано!";
        String hoverInfo = buildHoverInfo(target);

        sendTrackerMessage(toastTitle, toastDesc, chatText, hoverInfo, Theme.getAccentColor());
        notificationCooldowns.put(key, currentTime);
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

    private String buildHoverInfo(Player player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("§d§l★ Информация об игроке:\n");
        sb.append("§fИгрок: §a").append(player.getName().getString()).append("\n");

        double dist = mc.player != null ? Math.sqrt(player.distanceToSqr(mc.player)) : 0;
        sb.append("§fДистанция: §e").append(String.format(Locale.ROOT, "%.1f", dist)).append("m\n");

        ItemStack offhand = player.getOffhandItem();
        String offhandName = offhand.isEmpty() ? "Пусто" : offhand.getHoverName().getString();
        boolean isTalisman = isTalismanItem(offhand);
        sb.append("§f").append(isTalisman ? "Талисман: §d" : "Левая рука: §b").append(offhandName).append("\n");

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
        int lastShardCount = 0;
    }
}
