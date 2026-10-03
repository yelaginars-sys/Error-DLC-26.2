package error.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import error.event.EventTarget;
import error.event.list.PlayerTickEvent;
import error.module.Category;
import error.module.Module;
import error.setting.impl.CheckBox;
import error.setting.impl.MultiModeSetting;
import error.setting.impl.SliderSetting;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.client.localization.Localization;
import error.util.client.persiki.Notify;
import error.util.render.font.IconUse;

import java.util.*;

public class PotionTracker extends Module {
    public static PotionTracker INSTANCE;

    public final MultiModeSetting display = multiMode("Уведомления", List.of("Chat", "Notify"), "Chat", "Notify");
    public final SliderSetting trackRadius = slider("Радиус отслеживания", 40.0f, 10.0f, 100.0f, 5.0f);
    public final CheckBox showHitPercent = checkbox("Показывать % попадания", true);
    public final CheckBox showEffectsDetail = checkbox("Показывать список эффектов", true);
    public final CheckBox soundAlert = checkbox("Звуковой сигнал", true);

    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private static final double SPLASH_RADIUS = 4.2;
    private static final double SPLASH_HEIGHT = 2.5;

    public PotionTracker() {
        super("PotionTracker", "Отслеживание брошенных зелий, % попадания и наложенных эффектов", Category.MISC);
        INSTANCE = this;
    }

    @Override
    protected void onDisable() {
        trackedPotions.clear();
    }

    @EventTarget
    public void onTick(PlayerTickEvent event) {
        if (event.getPhase() != PlayerTickEvent.Phase.PRE || !inGame() || player() == null || mc.level == null) {
            return;
        }

        handlePotionTracking();
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

        String potionTitle = data.info.displayName;

        for (int i = 0; i < Math.min(4, hits.size()); i++) {
            PlayerHit hit = hits.get(i);
            boolean isSelf = hit.player == player();
            String nameText = isSelf ? "Вы" : hit.player.getName().getString();

            String prefixSymbol = isSelf ? "" : "§c⚡ ";
            String pctSuffix = showHitPercent.getValue() ? " §7(" + hit.percent + "%)" : "";
            String chatMain = prefixSymbol + "§f" + nameText + "§r§7 получил эффекты от §e[★] " + potionTitle + pctSuffix;

            if (display.isEnabled("Chat") || display.isEnabled("Чат")) {
                MutableComponent msg = Component.literal("[PotionTracker] » ").withStyle(Style.EMPTY.withColor(0xFF8B5CF6).withBold(true))
                        .append(Component.literal(chatMain));

                if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.getChat() != null) {
                    mc.gui.hud.getChat().addClientSystemMessage(msg);

                    if (showEffectsDetail.getValue()) {
                        for (PotionEffectDetail detail : data.info.effects) {
                            String detailText = "§7  - " + detail.name + " " + toRoman(detail.amplifier) + " (" + detail.durationText + ")";
                            mc.gui.hud.getChat().addClientSystemMessage(Component.literal(detailText));
                        }
                    }
                }
            }

            if (display.isEnabled("Notify")) {
                String toastMsg = nameText + " получил [★] " + potionTitle + (showHitPercent.getValue() ? " " + hit.percent + "%!" : "!");
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

        return PotionInfo.HOLY_WATER;
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
