package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class CooldownHud extends HudElement implements error.IMinecraft {

    private record CooldownDef(Item item, String name, int defaultTicks) {}

    private static final List<CooldownDef> TRACKED_ITEMS = List.of(
            new CooldownDef(Items.ENDER_PEARL, "Эндер-перл", 300),
            new CooldownDef(Items.CHORUS_FRUIT, "Хорус", 200),
            new CooldownDef(Items.TOTEM_OF_UNDYING, "Тотем", 100),
            new CooldownDef(Items.ENCHANTED_GOLDEN_APPLE, "Энч. яблоко", 400),
            new CooldownDef(Items.GOLDEN_APPLE, "Золотое яблоко", 100),
            new CooldownDef(Items.FIREWORK_ROCKET, "Фейерверк", 200),
            new CooldownDef(Items.SHIELD, "Щит", 100),
            new CooldownDef(Items.CROSSBOW, "Арбалет", 100),
            new CooldownDef(Items.WIND_CHARGE, "Ветряной заряд", 100),
            new CooldownDef(Items.GOAT_HORN, "Козий рог", 100),
            new CooldownDef(Items.ENDER_EYE, "Дезориентация", 160),
            new CooldownDef(Items.NETHERITE_SCRAP, "Трапка", 200),
            new CooldownDef(Items.DRIED_KELP, "Пласт", 100),
            new CooldownDef(Items.SUGAR, "Явная пыль", 100),
            new CooldownDef(Items.SNOWBALL, "Ком снега", 60),
            new CooldownDef(Items.PHANTOM_MEMBRANE, "Божья аура", 200),
            new CooldownDef(Items.NETHER_STAR, "Стан", 240),
            new CooldownDef(Items.PRISMARINE_SHARD, "Взрывная трапка", 160),
            new CooldownDef(Items.FIRE_CHARGE, "Взрывная штучка", 160),
            new CooldownDef(Items.POTION, "Исцеление", 60)
    );

    private final Map<Item, Animation> anims = new LinkedHashMap<>();
    private final Map<Item, Float> activeProgress = new HashMap<>();
    private final Animation widthAnim = new Animation(80.0F, 0.20F);

    public CooldownHud() {
        super("cooldowns", "Cooldowns", 20.0F, 100.0F, 90.0F, 20.0F, true);
    }

    @Override
    public boolean shouldRender() {
        Interface iface = Interface.getInstance();
        if (iface != null && (!iface.isEnabled() || !iface.cooldowns.getValue())) {
            return false;
        }
        return super.shouldRender();
    }

    private static String formatTime(float seconds) {
        int total = Math.max(0, (int) Math.ceil(seconds));
        int min = total / 60;
        int sec = total % 60;
        return (min < 10 ? "0" + min : String.valueOf(min)) + ":" + (sec < 10 ? "0" + sec : String.valueOf(sec));
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;

        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;

        // Poll cooldowns from player
        activeProgress.clear();
        for (CooldownDef def : TRACKED_ITEMS) {
            ItemStack stack = new ItemStack(def.item);
            float progress = mc.player.getCooldowns().getCooldownPercent(stack, 0.0F);
            if (progress > 0.001F) {
                activeProgress.put(def.item, progress);
            }
        }

        // In ChatScreen, mock entries if empty for easy dragging and preview
        if (activeProgress.isEmpty() && inChat) {
            activeProgress.put(Items.ENDER_PEARL, 0.65F);
            activeProgress.put(Items.GOLDEN_APPLE, 0.40F);
        }

        // Update entry animations
        for (CooldownDef def : TRACKED_ITEMS) {
            Animation anim = anims.computeIfAbsent(def.item, i -> new Animation(0.0F, 0.18F));
            boolean active = activeProgress.containsKey(def.item);
            anim.setTarget(active ? 1.0F : 0.0F);
            anim.update();
        }

        // Count visible entries
        List<CooldownDef> visibleDefs = new ArrayList<>();
        float maxEntryW = 75.0F;

        for (CooldownDef def : TRACKED_ITEMS) {
            Animation anim = anims.get(def.item);
            if (anim != null && anim.getValue() > 0.01F) {
                visibleDefs.add(def);

                float prog = activeProgress.getOrDefault(def.item, 0.0F);
                float sec = (prog * def.defaultTicks) / 20.0F;
                String timeStr = formatTime(sec);

                float timeW = Fonts.SF_MEDIUM.getWidth(timeStr, 8.5F);
                float nameW = Fonts.SF_MEDIUM.getWidth(def.name, 9.0F);
                float leftPillW = 14.0F + timeW + 4.0F;
                float rightPillW = nameW + 8.0F;
                float entryW = leftPillW + 2.0F + rightPillW;
                if (entryW > maxEntryW) maxEntryW = entryW;
            }
        }

        if (visibleDefs.isEmpty() && !inChat) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Header Dimensions
        String headerTitle = "Cooldowns";
        float headerIconW = Fonts.getIconWidth(error.util.render.font.IconUse.CLOCK, 9.0F);
        float headerTextW = Fonts.SF_MEDIUM.getWidth(headerTitle, 9.5F);
        float headerW = 6.0F + headerIconW + 4.0F + headerTextW + 7.0F;

        float targetWidth = Math.max(headerW, maxEntryW);
        widthAnim.setTarget(targetWidth);
        widthAnim.update();

        this.width = widthAnim.getValue();

        int accent = Theme.getAccentColor();

        // 1. Draw Header Capsule [ ⏱ Cooldowns ]
        float curY = this.y;
        Render2D.drawLiquidGlass(this.x, curY, this.width, 14.0F, 4.0F, 1.0F, accent);
        Fonts.drawIcon(error.util.render.font.IconUse.CLOCK, this.x + 6.0F, curY + 2.5F, 9.0F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, headerTitle, this.x + 6.0F + headerIconW + 4.0F, curY + 2.5F, 9.5F, 0xFFFFFFFF);

        curY += 16.0F;

        var extractor = event.getGuiGraphicsExtractor();

        // 2. Draw each active cooldown entry
        for (CooldownDef def : visibleDefs) {
            Animation anim = anims.get(def.item);
            float a = anim.getValue();
            if (a <= 0.01F) continue;

            float prog = activeProgress.getOrDefault(def.item, 0.0F);
            float sec = (prog * def.defaultTicks) / 20.0F;
            String timeStr = formatTime(sec);

            float timeW = Fonts.SF_MEDIUM.getWidth(timeStr, 8.5F);
            float nameW = Fonts.SF_MEDIUM.getWidth(def.name, 9.0F);

            float leftPillW = 14.0F + timeW + 4.0F;
            float rightPillW = Math.max(nameW + 8.0F, this.width - leftPillW - 2.0F);
            float rowH = 14.0F;

            // Left Pill: Item icon + Time
            Render2D.drawLiquidGlass(this.x, curY, leftPillW, rowH, 3.5F, a, accent);

            // Right Pill: Name
            Render2D.drawLiquidGlass(this.x + leftPillW + 2.0F, curY, rightPillW, rowH, 3.5F, a, accent);

            // Render Item Icon in left pill
            if (extractor != null) {
                Render2DUtil.flush();
                try {
                    var pose = extractor.pose();
                    pose.pushMatrix();
                    pose.translate(this.x + 2.0F, curY + 2.0F);
                    pose.scale(0.625F, 0.625F); // 10px / 16px
                    extractor.item(new ItemStack(def.item), 0, 0);
                    pose.popMatrix();
                } catch (Throwable ignored) {}
            }

            // Time string in left pill
            Fonts.drawString(Fonts.SF_MEDIUM, timeStr, this.x + 13.5F, curY + 3.0F, 8.5F, ColorUtil.rgba(255, 255, 255, (int) (240 * a)));

            // Item Name in right pill
            Fonts.drawString(Fonts.SF_MEDIUM, def.name, this.x + leftPillW + 2.0F + 4.0F, curY + 2.5F, 9.0F, ColorUtil.rgba(240, 240, 240, (int) (240 * a)));

            curY += (rowH + 2.0F) * a;
        }

        this.height = curY - this.y;
    }
}
