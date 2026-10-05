package error.module.impl.misc;

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
import error.event.EventTarget;
import error.event.list.PacketEvent;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.HeaderSetting;
import error.setting.impl.SliderSetting;
import error.util.client.localization.Localization;

import java.util.*;

public class UseTracker extends Module {
    public static UseTracker INSTANCE;

    public final SliderSetting trackRadius = slider("Радиус отслеживания", 35.0f, 10.0f, 100.0f, 5.0f);

    private final HeaderSetting useHeader = header("Предметы");
    public final CheckBox trackGapple = checkbox("Золотое яблоко", true);
    public final CheckBox trackCrapple = checkbox("Зачарованное яблоко", true);
    public final CheckBox trackPearl = checkbox("Эндер-жемчуг", true);
    public final CheckBox trackChorus = checkbox("Хорус", true);
    public final CheckBox trackDrinkPotion = checkbox("Питьё зелий", true);
    public final CheckBox trackSplashPotion = checkbox("Бросок зелий", true);

    private final HeaderSetting totemHeader = header("Тотемы");
    public final CheckBox trackTotem = checkbox("Потеря Тотема", true);
    public final CheckBox checkEnchant = checkbox("Проверка Зачарования", true);

    private final HeaderSetting serverHeader = header("Предметы анархий");
    public final CheckBox trackDisorient = checkbox("Дезориентация", true);
    public final CheckBox trackTrap = checkbox("Трапка", true);
    public final CheckBox trackDust = checkbox("Явная пыль", true);
    public final CheckBox trackGodAura = checkbox("Божья Аура", true);
    public final CheckBox trackShard = checkbox("Шарды и Обереги", true);

    private final Map<String, PlayerItemState> playerStates = new HashMap<>();
    private final Map<String, Long> notificationCooldowns = new HashMap<>();
    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private static final long NOTIFICATION_COOLDOWN_MS = 1000L;
    private static final double SPLASH_RADIUS = 4.2;
    private static final double SPLASH_HEIGHT = 2.5;

    public UseTracker() {
        super("UseTracker", "Отслеживание использования тотемов, зелий, яблок и предметов в чат", Category.MISC);
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
        if (trackSplashPotion.getValue()) {
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

                    String dot = enchanted ? "§a●" : "§c●";
                    String chatText = name + " потерял Тотем бессмертия, зачарован: " + dot;
                    String hoverInfo = buildHoverInfo(poppedPlayer);

                    sendChatMessage(chatText, hoverInfo);
                }
            }
        }
    }

    private void handlePotionTracking() {
        Set<Integer> currentPotionIds = new HashSet<>();
        AABB searchBox = player().getBoundingBox().inflate(trackRadius.getValue());

        List<ThrownSplashPotion> potions = mc.level.getEntitiesOfClass(ThrownSplashPotion.class, searchBox, Entity::isAlive);
        for (ThrownSplashPotion potion : potions) {
            String potName = detectPotionName(potion.getItem());
            if (potName == null) continue;

            int id = potion.getId();
            currentPotionIds.add(id);

            PotionData data = trackedPotions.get(id);
            if (data == null) {
                trackedPotions.put(id, new PotionData(potName, potion.position(), potion.getItem().copy()));
            } else {
                data.lastPos = potion.position();
                data.potionName = potName;
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

            int percent = Math.max(1, Math.min(100, (int) Math.round((1.0 - (dist / SPLASH_RADIUS)) * 100.0)));
            hits.add(new PlayerHit(p, percent, dist));
        }

        hits.sort(Comparator.comparingDouble(PlayerHit::distance));
        if (hits.isEmpty()) return;

        String potionTitle = data.potionName;

        for (int i = 0; i < Math.min(4, hits.size()); i++) {
            PlayerHit hit = hits.get(i);
            boolean isSelf = hit.player == player();
            String nameText = isSelf ? "Вы" : hit.player.getName().getString();
            String prefixSymbol = isSelf ? "" : "§c⚡ ";
            String chatMain = prefixSymbol + "§f" + nameText + "§r§7 получил эффекты от §e[★] " + potionTitle + " §7(" + hit.percent + "%)";

            sendChatMessage(chatMain, buildHoverInfo(hit.player));
        }
    }

    private String detectPotionName(ItemStack stack) {
        if (stack.isEmpty()) return null;

        // 1. Text checks (strip color formatting)
        StringBuilder sb = new StringBuilder();
        sb.append(stack.getHoverName().getString()).append(" ");
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName != null) sb.append(customName.getString()).append(" ");
        var lore = stack.get(DataComponents.LORE);
        if (lore != null) sb.append(lore.toString()).append(" ");
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) sb.append(customData.toString()).append(" ");

        String rawText = sb.toString().replaceAll("§[0-9a-fk-or]", "").toLowerCase(Locale.ROOT);

        if (rawText.contains("ассасин") || rawText.contains("assassin")) return "Зелье Ассасина";
        if (rawText.contains("гнев") || rawText.contains("wrath")) return "Зелье Гнева";
        if (rawText.contains("паладин") || rawText.contains("палладин") || rawText.contains("paladin")) return "Зелье Паладина";
        if (rawText.contains("радиаци") || rawText.contains("radiation")) return "Зелье Радиации";
        if (rawText.contains("снотвор") || rawText.contains("sleep")) return "Снотворное";
        if (rawText.contains("хлопушк") || rawText.contains("flapper")) return "Хлопушка";
        if (rawText.contains("святая") || rawText.contains("holy")) return "Святая вода";

        // 2. Effects check based on Debuda mogged signature tables
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.hasEffects()) {
            boolean hasHarm = false;
            boolean hasStrength = false;
            boolean hasSpeed = false;
            boolean hasHaste = false;
            boolean hasSlowness = false;
            boolean hasRegen = false;
            boolean hasHeal = false;
            boolean hasResistance = false;
            boolean hasHealthBoost = false;
            boolean hasPoison = false;
            boolean hasWither = false;
            boolean hasWeakness = false;
            boolean hasBlindness = false;

            for (MobEffectInstance inst : contents.getAllEffects()) {
                if (inst.is(MobEffects.INSTANT_DAMAGE)) hasHarm = true;
                if (inst.is(MobEffects.STRENGTH)) hasStrength = true;
                if (inst.is(MobEffects.SPEED)) hasSpeed = true;
                if (inst.is(MobEffects.HASTE)) hasHaste = true;
                if (inst.is(MobEffects.SLOWNESS)) hasSlowness = true;
                if (inst.is(MobEffects.REGENERATION)) hasRegen = true;
                if (inst.is(MobEffects.INSTANT_HEALTH)) hasHeal = true;
                if (inst.is(MobEffects.RESISTANCE)) hasResistance = true;
                if (inst.is(MobEffects.HEALTH_BOOST)) hasHealthBoost = true;
                if (inst.is(MobEffects.POISON)) hasPoison = true;
                if (inst.is(MobEffects.WITHER)) hasWither = true;
                if (inst.is(MobEffects.WEAKNESS)) hasWeakness = true;
                if (inst.is(MobEffects.BLINDNESS)) hasBlindness = true;
            }

            if (hasHarm || (hasStrength && hasSpeed && (hasHaste || !hasSlowness))) return "Зелье Ассасина";
            if (hasStrength && hasSlowness) return "Зелье Гнева";
            if (hasResistance && hasHealthBoost) return "Зелье Паладина";
            if (hasPoison && hasWither) return "Зелье Радиации";
            if (hasWeakness && (hasBlindness || hasWither)) return "Снотворное";
            if (hasRegen || hasHeal) return "Святая вода";
        }

        // 3. Fallback to clean name of the item itself if custom, or potion name
        String cleanName = stack.getHoverName().getString().replaceAll("§[0-9a-fk-or]", "").trim();
        if (!cleanName.isEmpty() && !cleanName.equalsIgnoreCase("Взрывное зелье") && !cleanName.equalsIgnoreCase("Splash Potion")) {
            return cleanName;
        }

        return "Кастомное зелье";
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
        String hoverInfo = buildHoverInfo(target);

        sendChatMessage(chatText, hoverInfo);
        notificationCooldowns.put(key, currentTime);
    }

    private void sendChatMessage(String chatText, String hoverText) {
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

    private String buildHoverInfo(Player player) {
        if (player == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("§d§l★ Информация об игроке:\n");
        sb.append("§fИгрок: §a").append(player.getName().getString()).append("\n");

        double dist = mc.player != null ? Math.sqrt(player.distanceToSqr(mc.player)) : 0;
        sb.append("§fДистанция: §e").append(String.format(Locale.ROOT, "%.1f", dist)).append("m\n");

        ItemStack offhand = player.getOffhandItem();
        String offhandName = offhand.isEmpty() ? "Пусто" : offhand.getHoverName().getString();
        sb.append("§fЛевая рука: §b").append(offhandName).append("\n");

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

        return sb.toString().trim();
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

    private static class PotionData {
        String potionName;
        Vec3 lastPos;
        ItemStack stack;

        PotionData(String potionName, Vec3 lastPos, ItemStack stack) {
            this.potionName = potionName;
            this.lastPos = lastPos;
            this.stack = stack;
        }
    }

    private record PlayerHit(Player player, int percent, double distance) {}

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
