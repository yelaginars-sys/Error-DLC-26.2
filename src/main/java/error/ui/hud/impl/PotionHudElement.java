package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.ui.hud.HudElement;
import error.ui.hud.HudManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import error.util.render.font.MsdfFont;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

public final class PotionHudElement extends HudElement implements IMinecraft {

    private static final Map<MobEffect, String> RU_NAMES = new HashMap<>();

    static {
        if (MobEffects.SPEED != null) RU_NAMES.put(MobEffects.SPEED.value(), "Скорость");
        if (MobEffects.SLOWNESS != null) RU_NAMES.put(MobEffects.SLOWNESS.value(), "Замедление");
        if (MobEffects.HASTE != null) RU_NAMES.put(MobEffects.HASTE.value(), "Спешка");
        if (MobEffects.MINING_FATIGUE != null) RU_NAMES.put(MobEffects.MINING_FATIGUE.value(), "Усталость");
        if (MobEffects.STRENGTH != null) RU_NAMES.put(MobEffects.STRENGTH.value(), "Сила");
        if (MobEffects.INSTANT_HEALTH != null) RU_NAMES.put(MobEffects.INSTANT_HEALTH.value(), "Исцеление");
        if (MobEffects.INSTANT_DAMAGE != null) RU_NAMES.put(MobEffects.INSTANT_DAMAGE.value(), "Мгн. урон");
        if (MobEffects.JUMP_BOOST != null) RU_NAMES.put(MobEffects.JUMP_BOOST.value(), "Прыгучесть");
        if (MobEffects.NAUSEA != null) RU_NAMES.put(MobEffects.NAUSEA.value(), "Тошнота");
        if (MobEffects.REGENERATION != null) RU_NAMES.put(MobEffects.REGENERATION.value(), "Регенерация");
        if (MobEffects.RESISTANCE != null) RU_NAMES.put(MobEffects.RESISTANCE.value(), "Сопротивление");
        if (MobEffects.FIRE_RESISTANCE != null) RU_NAMES.put(MobEffects.FIRE_RESISTANCE.value(), "Огнестойкость");
        if (MobEffects.WATER_BREATHING != null) RU_NAMES.put(MobEffects.WATER_BREATHING.value(), "Подводное дыхание");
        if (MobEffects.INVISIBILITY != null) RU_NAMES.put(MobEffects.INVISIBILITY.value(), "Невидимость");
        if (MobEffects.BLINDNESS != null) RU_NAMES.put(MobEffects.BLINDNESS.value(), "Слепота");
        if (MobEffects.NIGHT_VISION != null) RU_NAMES.put(MobEffects.NIGHT_VISION.value(), "Ночное зрение");
        if (MobEffects.HUNGER != null) RU_NAMES.put(MobEffects.HUNGER.value(), "Голод");
        if (MobEffects.WEAKNESS != null) RU_NAMES.put(MobEffects.WEAKNESS.value(), "Слабость");
        if (MobEffects.POISON != null) RU_NAMES.put(MobEffects.POISON.value(), "Отравление");
        if (MobEffects.WITHER != null) RU_NAMES.put(MobEffects.WITHER.value(), "Иссушение");
        if (MobEffects.HEALTH_BOOST != null) RU_NAMES.put(MobEffects.HEALTH_BOOST.value(), "Доп. здоровье");
        if (MobEffects.ABSORPTION != null) RU_NAMES.put(MobEffects.ABSORPTION.value(), "Поглощение");
        if (MobEffects.GLOWING != null) RU_NAMES.put(MobEffects.GLOWING.value(), "Свечение");
        if (MobEffects.LEVITATION != null) RU_NAMES.put(MobEffects.LEVITATION.value(), "Левитация");
        if (MobEffects.SLOW_FALLING != null) RU_NAMES.put(MobEffects.SLOW_FALLING.value(), "Плавное падение");
        if (MobEffects.CONDUIT_POWER != null) RU_NAMES.put(MobEffects.CONDUIT_POWER.value(), "Морской источник");
        if (MobEffects.DOLPHINS_GRACE != null) RU_NAMES.put(MobEffects.DOLPHINS_GRACE.value(), "Грация дельфина");
        if (MobEffects.BAD_OMEN != null) RU_NAMES.put(MobEffects.BAD_OMEN.value(), "Дурное знамение");
        if (MobEffects.HERO_OF_THE_VILLAGE != null) RU_NAMES.put(MobEffects.HERO_OF_THE_VILLAGE.value(), "Герой деревни");
        if (MobEffects.DARKNESS != null) RU_NAMES.put(MobEffects.DARKNESS.value(), "Тьма");
    }

    private static record SamplePotion(String name, String level, String duration, int color) {}

    private static final List<SamplePotion> PREVIEW_POTIONS = List.of(
            new SamplePotion("Скорость", "3", "4m 20s", ColorUtil.rgba(120, 200, 255, 255)),
            new SamplePotion("Исцеление", "2", "1s", ColorUtil.rgba(255, 100, 100, 255)),
            new SamplePotion("Свечение", "", "39s", ColorUtil.rgba(255, 230, 80, 255)),
            new SamplePotion("Замедление", "10", "1s", ColorUtil.rgba(140, 140, 170, 255)),
            new SamplePotion("Сила", "4", "41s", ColorUtil.rgba(230, 70, 70, 255)),
            new SamplePotion("Сопротивление", "2", "4m 46s", ColorUtil.rgba(90, 140, 230, 255)),
            new SamplePotion("Невидимость", "2", "9m 56s", ColorUtil.rgba(200, 200, 210, 255)),
            new SamplePotion("Ночное зрение", "2", "39s", ColorUtil.rgba(80, 210, 120, 255)),
            new SamplePotion("Регенерация", "2", "42s", ColorUtil.rgba(240, 120, 180, 255)),
            new SamplePotion("Спешка", "", "41s", ColorUtil.rgba(240, 180, 60, 255)),
            new SamplePotion("Огнестойкость", "", "4m 46s", ColorUtil.rgba(255, 140, 50, 255)),
            new SamplePotion("Слабость", "", "1s", ColorUtil.rgba(160, 110, 180, 255))
    );

    public PotionHudElement() {
        super("potions", "Potions", 10.0F, 38.0F, 140.0F, 220.0F, true);
    }

    @Override
    public void draw(Render2DEvent event) {
        MsdfFont font = Fonts.SF_MEDIUM;
        float headerSize = 7.5F;
        float itemSize = 6.5F;

        List<SamplePotion> activeList = new ArrayList<>();

        if (mc.player != null && !mc.player.getActiveEffects().isEmpty()) {
            for (MobEffectInstance instance : mc.player.getActiveEffects()) {
                MobEffect effect = instance.getEffect().value();
                String name = RU_NAMES.getOrDefault(effect, effect.getDescriptionId());
                int amp = instance.getAmplifier();
                String lvlStr = amp > 0 ? String.valueOf(amp + 1) : "";

                int totalSec = instance.getDuration() / 20;
                String durStr;
                if (totalSec >= 60) {
                    durStr = (totalSec / 60) + "m " + (totalSec % 60) + "s";
                } else {
                    durStr = totalSec + "s";
                }

                int color = effect.getColor();
                int argbColor = ColorUtil.rgba((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255);

                activeList.add(new SamplePotion(name, lvlStr, durStr, argbColor));
            }
        } else if (HudManager.getInstance().isDraggableScreenOpen()) {
            activeList.addAll(PREVIEW_POTIONS);
        }

        if (activeList.isEmpty()) {
            this.height = 0;
            return;
        }

        float padding = 8.0F;
        float headerH = 20.0F;
        float itemH = 16.0F;
        float totalH = headerH + activeList.size() * itemH + padding;
        float maxW = 145.0F;

        this.width = maxW;
        this.height = totalH;

        int bgColor = ColorUtil.rgba(18, 18, 24, 210);
        int outlineColor = ColorUtil.withAlpha(Theme.getAccentColor(), 100);

        Render2D.drawBlur(x, y, width, height, 8.0F, bgColor, 1.0F);
        Render2D.drawRoundedRectWithOutline(x, y, width, height, 8.0F, bgColor, 1.0F, outlineColor);

        // Header
        Fonts.drawString(font, "Potions", x + 10.0F, y + 4.0F, headerSize, ColorUtil.rgba(240, 240, 250, 255));
        Render2D.drawRect(x + 10.0F, y + headerH - 2.0F, width - 20.0F, 1.0F, ColorUtil.rgba(255, 255, 255, 30));

        float curY = y + headerH + 2.0F;
        for (SamplePotion p : activeList) {
            // Color dot
            Render2D.drawCircle(x + 12.0F, curY + itemH / 2.0F - 1.0F, 2.5F, p.color());

            // Name + level
            String label = p.name() + (p.level().isEmpty() ? "" : " " + p.level());
            Fonts.drawString(font, label, x + 20.0F, curY + (itemH - font.lineHeight(itemSize)) / 2.0F, itemSize, ColorUtil.rgba(230, 230, 230, 255));

            // Duration
            float durW = font.getWidth(p.duration(), itemSize);
            Fonts.drawString(font, p.duration(), x + width - 10.0F - durW, curY + (itemH - font.lineHeight(itemSize)) / 2.0F, itemSize, ColorUtil.rgba(170, 170, 180, 255));

            curY += itemH;
        }
    }
}
