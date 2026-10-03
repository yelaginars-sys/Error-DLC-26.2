package error.ui.hud.impl;

import error.IMinecraft;
import error.event.list.Render2DEvent;
import error.module.impl.misc.ServerHelper;
import error.setting.impl.BindSetting;
import error.ui.hud.HudElement;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import error.util.render.font.Fonts;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class HelperBindsHud extends HudElement implements IMinecraft {

    public static class HelperBindItem {
        final String name;
        final Item item;
        final BindSetting bind;

        public HelperBindItem(String name, Item item, BindSetting bind) {
            this.name = name;
            this.item = item;
            this.bind = bind;
        }
    }

    public HelperBindsHud() {
        super("helper_binds", "Server Helper Binds", 10.0F, 160.0F, 140.0F, 20.0F, true);
    }

    private List<HelperBindItem> getActiveBinds() {
        ServerHelper sh = ServerHelper.INSTANCE;
        List<HelperBindItem> list = new ArrayList<>();
        if (sh == null || !sh.isEnabled()) return list;

        if (sh.server.is("FunTime")) {
            addBind(list, "Дезориентация", Items.ENDER_EYE, sh.ftDisorient);
            addBind(list, "Пыль", Items.SUGAR, sh.ftDust);
            addBind(list, "Пласт", Items.DRIED_KELP, sh.ftPlast);
            addBind(list, "Снежок", Items.SNOWBALL, sh.ftSnow);
            addBind(list, "Божья Аура", Items.PHANTOM_MEMBRANE, sh.ftGodAura);
            addBind(list, "Трапка", Items.NETHERITE_SCRAP, sh.ftTrap);
            addBind(list, "Смерч", Items.FIRE_CHARGE, sh.ftFireSwirl);
            addBind(list, "Святая вода", Items.SPLASH_POTION, sh.ftHolyWater);
            addBind(list, "Гнев", Items.SPLASH_POTION, sh.ftAnger);
        } else if (sh.server.is("HolyWorld")) {
            addBind(list, "Взр. трапка", Items.PRISMARINE_SHARD, sh.hwExplosiveTrap);
            addBind(list, "Трапка", Items.POPPED_CHORUS_FRUIT, sh.hwNormalTrap);
            addBind(list, "Стан", Items.NETHER_STAR, sh.hwStun);
            addBind(list, "Снежок", Items.SNOWBALL, sh.hwSnow);
            addBind(list, "Бомба", Items.FIRE_CHARGE, sh.hwBomb);
        }
        return list;
    }

    private void addBind(List<HelperBindItem> list, String name, Item item, BindSetting bind) {
        if (bind != null && bind.isBound()) {
            list.add(new HelperBindItem(name, item, bind));
        }
    }

    @Override
    public void draw(Render2DEvent event) {
        if (mc.player == null) return;

        List<HelperBindItem> binds = getActiveBinds();
        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;

        if (binds.isEmpty() && !chatOpen) {
            this.width = 0.0F;
            this.height = 0.0F;
            return;
        }

        float barH = 18.0F;
        float padX = 7.0F;
        float itemGap = 6.0F;

        float contentW = padX * 2.0F;

        if (binds.isEmpty()) {
            contentW += Fonts.SF_MEDIUM.getWidth("ServerHelper Binds (Нет биндов)", 7.0F);
        } else {
            for (int i = 0; i < binds.size(); i++) {
                HelperBindItem b = binds.get(i);
                String label = b.name + " [" + b.bind.getDisplayValue() + "]";
                contentW += Fonts.SF_MEDIUM.getWidth(label, 7.0F);
                if (i < binds.size() - 1) {
                    contentW += itemGap;
                }
            }
        }

        this.width = Math.max(80.0F, contentW);
        this.height = barH;

        float x = getX();
        float y = getY();

        int shadowCol = ColorUtil.rgba(0, 0, 0, 145);
        int glassFill = ColorUtil.rgba(18, 16, 26, 220);
        int glassBorder = ColorUtil.rgba(255, 255, 255, 35);
        int themeAccent = Theme.getAccentColor();

        // Background Panel
        Render2D.drawShadow(x, y, this.width, barH, 5.0F, 8.0F, shadowCol);
        Render2D.drawBlur(x, y, this.width, barH, 5.0F, 14.0F, glassFill, 1.0F);
        Render2D.drawRoundedRect(x, y, this.width, barH, 5.0F, glassFill);
        Render2D.drawRoundedOutline(x, y, this.width, barH, 5.0F, 1.0F, glassBorder);

        float curX = x + padX;
        float textY = y + (barH - 7.0F) / 2.0F;

        if (binds.isEmpty()) {
            Fonts.drawString(Fonts.SF_MEDIUM, "ServerHelper Binds", curX, textY, 7.0F, ColorUtil.rgba(180, 180, 200, 200));
        } else {
            for (int i = 0; i < binds.size(); i++) {
                HelperBindItem b = binds.get(i);
                String keyName = b.bind.getDisplayValue();

                // Item Name
                Fonts.drawString(Fonts.SF_MEDIUM, b.name, curX, textY, 7.0F, ColorUtil.rgba(235, 235, 245, 235));
                curX += Fonts.SF_MEDIUM.getWidth(b.name, 7.0F) + 2.0F;

                // Key Badge
                float badgeW = Fonts.SF_MEDIUM.getWidth(keyName, 6.5F) + 5.0F;
                float badgeH = 10.0F;
                float badgeY = y + (barH - badgeH) / 2.0F;

                Render2D.drawRoundedRect(curX, badgeY, badgeW, badgeH, 2.5F, ColorUtil.rgba(ColorUtil.red(themeAccent), ColorUtil.green(themeAccent), ColorUtil.blue(themeAccent), 160));
                Render2D.drawRoundedOutline(curX, badgeY, badgeW, badgeH, 2.5F, 0.8F, ColorUtil.rgba(255, 255, 255, 60));
                Fonts.drawString(Fonts.SF_MEDIUM, keyName, curX + 2.5F, badgeY + 1.5F, 6.5F, ColorUtil.rgba(255, 255, 255, 245));

                curX += badgeW;

                // Cooldown Overlay
                float cdProgress = mc.player.getCooldowns().getCooldownPercent(new ItemStack(b.item), 0.0F);
                if (cdProgress > 0.01F) {
                    Render2D.drawRoundedRect(curX - badgeW - Fonts.SF_MEDIUM.getWidth(b.name, 7.0F) - 2.0F, y + 1.0F, badgeW + Fonts.SF_MEDIUM.getWidth(b.name, 7.0F) + 2.0F, barH - 2.0F, 3.0F, ColorUtil.rgba(255, 255, 255, 45));
                }

                if (i < binds.size() - 1) {
                    curX += itemGap;
                }
            }
        }
    }
}
