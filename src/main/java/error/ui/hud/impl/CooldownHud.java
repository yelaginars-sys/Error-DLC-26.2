package error.ui.hud.impl;

import error.event.list.Render2DEvent;
import error.module.impl.render.Interface;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.math.Animation;
import error.util.render.Render2D;
import error.util.display.blur.Blur;
import error.util.display.blur.BlurType;
import error.util.display.color.Color;
import error.util.display.outline.Outline;
import error.util.render.Render2DUtil;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
            new CooldownDef(Items.GOAT_HORN, "Козий рог", 100)
    );

    private record Entry(CooldownDef def, String timeStr, float seconds, float alpha) {}

    private final Map<Item, Animation> anims = new LinkedHashMap<>();
    private final Map<Item, Float> activeProgress = new HashMap<>();
    private final Animation widthAnim = new Animation(85.0F, 0.22F);
    private final Animation heightAnim = new Animation(18.0F, 0.22F);

    private static final float ROW_H = 13.0F;
    private static final float HEADER_H = 14.5F;
    private static final float PILL_R = 6.25F;
    private static final float GAP_Y = 2.0F;

    public CooldownHud() {
        super("cooldowns", "Cooldowns", 20.0F, 100.0F, 75.0F, 20.0F, true);
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
        int total = (int) Math.ceil(Math.max(seconds, 0.0F));
        int min = total / 60;
        int sec = total % 60;
        return String.format("%d:%02dс", min, sec);
    }

    private static int getTimeColor(float seconds, float alpha) {
        int a = (int) (240 * alpha);
        if (seconds <= 1.5F) {
            return ColorUtil.rgba(255, 85, 85, a);
        } else if (seconds <= 3.0F) {
            return ColorUtil.rgba(255, 170, 0, a);
        }
        return ColorUtil.rgba(235, 235, 235, a);
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null || mc.level == null) return;

        boolean inChat = mc.gui != null && mc.gui.screen() instanceof ChatScreen;

        activeProgress.clear();
        for (CooldownDef def : TRACKED_ITEMS) {
            ItemStack stack = new ItemStack(def.item);
            float progress = mc.player.getCooldowns().getCooldownPercent(stack, 0.0F);
            if (progress > 0.001F) {
                activeProgress.put(def.item, progress);
            }
        }

        if (activeProgress.isEmpty() && inChat) {
            activeProgress.put(Items.ENDER_PEARL, 0.65F);
            activeProgress.put(Items.GOLDEN_APPLE, 0.35F);
        }

        for (CooldownDef def : TRACKED_ITEMS) {
            Animation anim = anims.computeIfAbsent(def.item, i -> new Animation(0.0F, 0.20F));
            boolean active = activeProgress.containsKey(def.item);
            anim.setTarget(active ? 1.0F : 0.0F);
            anim.update();
        }

        List<Entry> entries = new ArrayList<>();
        for (CooldownDef def : TRACKED_ITEMS) {
            Animation anim = anims.get(def.item);
            if (anim != null && anim.getValue() > 0.01F) {
                float prog = activeProgress.getOrDefault(def.item, 0.0F);
                float sec = (prog * def.defaultTicks) / 20.0F;
                entries.add(new Entry(def, formatTime(sec), sec, anim.getValue()));
            }
        }

        if (entries.isEmpty()) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        // Calculate dynamic width
        float maxRowW = 72.0F;
        float headerTitleW = Fonts.SF_MEDIUM.getWidth("Cooldowns", 7.5F);
        float headerMinW = 18.0F + headerTitleW + 6.0F;
        maxRowW = Math.max(maxRowW, headerMinW);

        for (Entry e : entries) {
            float nameW = Fonts.SF_MEDIUM.getWidth(e.def.name, 7.0F);
            float timeW = Fonts.SF_MEDIUM.getWidth(e.timeStr, 6.5F);
            float rowTotalW = (13.5F + nameW + 6.0F) + 5.0F + (timeW + 9.0F);
            maxRowW = Math.max(maxRowW, rowTotalW);
        }

        float totalH = HEADER_H;
        for (Entry e : entries) {
            totalH += (ROW_H + GAP_Y) * e.alpha;
        }

        widthAnim.setTarget(maxRowW);
        widthAnim.update();
        heightAnim.setTarget(totalH);
        heightAnim.update();

        this.width = widthAnim.getValue();
        this.height = heightAnim.getValue();

        int accent = Theme.getAccentColor();
        float curX = this.x;
        float curY = this.y;

        // 1. Header Capsule
        GuiGraphicsExtractor extractor = event.getGuiGraphicsExtractor();
        Render2D.drawHudCard(extractor, curX, curY, this.width, HEADER_H, PILL_R, 1.0F, accent);

        // Icon "s"
        Fonts.drawString(Fonts.ERROR_ICONS, "s", curX + 5.0F, curY + 2.0F, 8.5F, accent);
        Fonts.drawString(Fonts.SF_MEDIUM, "Cooldowns", curX + 16.0F, curY + 2.2F, 7.5F, 0xFFFFFFFF);

        curY += HEADER_H + GAP_Y;

        // 2. Entries: Left capsule (Item + Name) and Right capsule (Time)
        for (Entry e : entries) {
            if (e.alpha <= 0.01F) continue;

            int textWhite = ColorUtil.rgba(255, 255, 255, (int) (245 * e.alpha));
            int timeCol = getTimeColor(e.seconds, e.alpha);

            float nameW = Fonts.SF_MEDIUM.getWidth(e.def.name, 7.0F);
            float timeW = Fonts.SF_MEDIUM.getWidth(e.timeStr, 6.5F);

            float leftPillW = 13.5F + nameW + 6.0F;
            float rightPillW = timeW + 9.0F;
            float rightPillX = curX + this.width - rightPillW;

            // Pills (Left & Right)
            Render2D.drawHudPill(extractor, curX, curY, leftPillW, ROW_H, PILL_R, e.alpha, accent);
            Render2D.drawHudPill(extractor, rightPillX, curY, rightPillW, ROW_H, PILL_R, e.alpha, accent);

            // Item Icon
            if (extractor != null) {
                Render2DUtil.flush();
                try {
                    var pose = extractor.pose();
                    pose.pushMatrix();
                    pose.translate(curX + 2.5F, curY + 2.0F);
                    pose.scale(0.50F, 0.50F); // 8px / 16px
                    extractor.item(new ItemStack(e.def.item), 0, 0);
                    pose.popMatrix();
                } catch (Throwable ignored) {}
            }

            // Name
            Fonts.drawString(Fonts.SF_MEDIUM, e.def.name, curX + 13.5F, curY + 1.8F, 7.0F, textWhite);

            // Time centered in right capsule
            Fonts.drawCenteredString(Fonts.SF_MEDIUM, e.timeStr, rightPillX + rightPillW * 0.5F, curY + 1.8F, 6.5F, timeCol);

            curY += (ROW_H + GAP_Y) * e.alpha;
        }
    }
}
