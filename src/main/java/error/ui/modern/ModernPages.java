package error.ui.modern;

import error.Client;
import error.config.ConfigManager;
import error.cosmetic.CosmeticItem;
import error.cosmetic.CosmeticType;
import error.cosmetic.CosmeticsManager;
import error.friend.FriendManager;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.List;

public final class ModernPages {

    private static String configInput = "";
    private static String friendInput = "";
    private static int selectedCosmeticTypeIndex = 0;

    private static final String[] COSMETIC_TABS = new String[]{"Все", "Крылья", "Шляпы", "Маски", "Рюкзаки", "Питомцы", "3D Модели", "Плащи"};
    private static final CosmeticType[] COSMETIC_TYPES = new CosmeticType[]{
        null, CosmeticType.WINGS, CosmeticType.HAT, CosmeticType.MASK, CosmeticType.BACKPACK, CosmeticType.PET, CosmeticType.MODEL, CosmeticType.CAPE
    };

    private ModernPages() {
    }

    // --- 1. PROFILE / COSMETICS PAGE ---
    public static void profile(ModernGui g, float x, float y, float w, float h) {
        float previewW = Math.min(120.0F, w * 0.32F);
        float previewH = h;

        int cardBg = ColorUtil.rgba(16, 18, 25, 160);
        int cardBorder = ColorUtil.rgba(255, 255, 255, 15);
        g.rect(x, y, previewW, previewH, 7.0F, cardBg);
        g.outline(x, y, previewW, previewH, 7.0F, 0.5F, cardBorder);

        float pCenterX = x + previewW / 2.0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            try {
                int scale = (int) (previewH * 0.36F);
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                    error.util.RenderExtend.currentGuiGraphicsExtractor(),
                    (int) (x + 6.0F), (int) (y + 6.0F),
                    (int) (x + previewW - 6.0F), (int) (y + previewH - 22.0F),
                    scale, 0.0F, (float) g.mouseX, (float) g.mouseY, mc.player
                );
            } catch (Throwable ignored) {
                Render2D.drawCustomAvatar(pCenterX - 20.0F, y + previewH / 2.0F - 25.0F, 40.0F, 20.0F, g.alpha);
            }
        } else {
            Render2D.drawCustomAvatar(pCenterX - 20.0F, y + previewH / 2.0F - 25.0F, 40.0F, 20.0F, g.alpha);
        }

        String username = mc != null && mc.getUser() != null ? mc.getUser().getName() : "Player";
        var titleFont = g.font(8.5F);
        g.textCenter(titleFont, g.clip(titleFont, username, previewW - 10.0F), pCenterX, y + previewH - 18.0F, ModernTheme.accent());

        // Right side: Cosmetics Category Tabs & List
        float rightX = x + previewW + 6.0F;
        float rightW = w - previewW - 6.0F;
        float tabW = (rightW - 9.0F) / 4.0F;
        float tabH = 14.0F;

        for (int i = 0; i < COSMETIC_TABS.length; i++) {
            int index = i;
            float tx = rightX + (i % 4) * (tabW + 3.0F);
            float ty = y + (i / 4) * (tabH + 3.0F);
            boolean active = selectedCosmeticTypeIndex == i;

            g.pill(tx, ty, tabW, tabH, COSMETIC_TABS[i], null, null, active, "costab" + i, button -> {
                selectedCosmeticTypeIndex = index;
                return true;
            });
        }

        float listY = y + 2 * (tabH + 3.0F) + 4.0F;
        float listH = h - (listY - y);

        g.pushClip(rightX, listY, rightW, listH);

        List<CosmeticItem> all = CosmeticsManager.getInstance().getCosmetics();
        CosmeticType filter = COSMETIC_TYPES[selectedCosmeticTypeIndex];

        float itemY = listY - g.scroll();
        float itemH = 20.0F;

        for (CosmeticItem c : all) {
            if (filter != null && c.getType() != filter) continue;

            if (itemY + itemH > listY && itemY < listY + listH) {
                int rowBg = ColorUtil.rgba(19, 21, 28, c.isEnabled() ? 130 : 70);
                g.rect(rightX, itemY, rightW, itemH, 5.0F, rowBg);
                g.outline(rightX, itemY, rightW, itemH, 5.0F, 0.4F, cardBorder);

                g.circle(rightX + 8.0F, itemY + 10.0F, 3.5F, c.getColor());

                var font = g.font(7.5F);
                var subFont = g.font(6.0F);
                g.text(font, c.getName(), rightX + 16.0F, itemY + 3.0F, ModernTheme.TEXT());
                g.text(subFont, c.getType().getDisplayName(), rightX + 16.0F, itemY + 11.5F, ModernTheme.TEXT_MUTED());

                g.toggle(rightX + rightW - 22.0F, itemY + 5.5F, c.isEnabled(), "cositem:" + c.getId());

                g.hit(rightX, itemY, rightW, itemH, button -> {
                    c.setEnabled(!c.isEnabled());
                    CosmeticsManager.getInstance().onToggleCosmetic(c);
                    error.util.client.ClientSoundPlayer.playGuiClick();
                    return true;
                });
            }
            itemY += itemH + 3.0F;
        }

        g.setContentHeight(itemY - (listY - g.scroll()), listH);
        g.popClip();
    }

    // --- 2. CONFIGS PAGE ---
    public static void configs(ModernGui g, float x, float y, float w, float h) {
        var titleFont = g.font(9.0F);
        g.text(titleFont, "Менеджер Конфигов", x, y, ModernTheme.TEXT());

        // Create new config field
        float fieldY = y + 14.0F;
        float fieldW = 140.0F;
        float fieldH = 16.0F;
        boolean focused = g.focus == OBJECT_CFG;

        g.rect(x, fieldY, fieldW, fieldH, 4.0F, ModernTheme.FIELD());
        if (focused) g.outline(x, fieldY, fieldW, fieldH, 4.0F, 0.5F, ModernTheme.accent());

        var inputFont = g.font(7.5F);
        String val = focused ? g.buffer : configInput;
        if (val.isEmpty()) {
            g.text(inputFont, "Имя конфига...", x + 6.0F, g.textY(fieldY, fieldH, inputFont), ModernTheme.TEXT_FAINT());
        } else {
            g.text(inputFont, val, x + 6.0F, g.textY(fieldY, fieldH, inputFont), ModernTheme.TEXT());
        }
        g.hit(x, fieldY, fieldW, fieldH, button -> {
            g.focus = OBJECT_CFG;
            g.buffer = configInput;
            return true;
        });

        // Save Button
        float btnX = x + fieldW + 6.0F;
        g.pill(btnX, fieldY, 65.0F, fieldH, "Сохранить", null, null, false, "cfgsave", button -> {
            String name = configInput.trim();
            if (!name.isEmpty() && Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                Client.INSTANCE.configManager.saveConfig(name, true);
                configInput = "";
                g.focus = null;
            }
            return true;
        });

        // Config List
        float listY = fieldY + fieldH + 8.0F;
        float listH = h - (listY - y);

        g.pushClip(x, listY, w, listH);

        ConfigManager cfgManager = Client.INSTANCE != null ? Client.INSTANCE.configManager : null;
        List<String> configs = cfgManager != null ? cfgManager.getAvailableConfigs() : List.of("default");

        float itemY = listY - g.scroll();
        float itemH = 24.0F;
        int cardBorder = ColorUtil.rgba(255, 255, 255, 12);

        for (String cfgName : configs) {
            if (itemY + itemH > listY && itemY < listY + listH) {
                boolean active = cfgManager != null && cfgName.equalsIgnoreCase(cfgManager.getCurrentConfig());
                int cardBg = ColorUtil.rgba(19, 21, 28, active ? 140 : 80);

                g.rect(x, itemY, w, itemH, 5.0F, cardBg);
                g.outline(x, itemY, w, itemH, 5.0F, 0.4F, cardBorder);

                g.text(g.font(8.0F), cfgName, x + 8.0F, itemY + 4.0F, active ? ModernTheme.accent() : ModernTheme.TEXT());
                g.text(g.font(6.5F), active ? "Активный конфиг" : "Сохраненный конфиг", x + 8.0F, itemY + 14.0F, ModernTheme.TEXT_MUTED());

                // Load button
                float loadX = x + w - 110.0F;
                g.pill(loadX, itemY + 4.0F, 50.0F, 16.0F, "Загрузить", null, null, active, "loadcfg" + cfgName, button -> {
                    if (cfgManager != null) cfgManager.loadConfig(cfgName, true);
                    return true;
                });

                // Delete button
                float delX = x + w - 55.0F;
                g.pill(delX, itemY + 4.0F, 50.0F, 16.0F, "Удалить", null, null, false, "delcfg" + cfgName, button -> {
                    if (cfgManager != null) cfgManager.deleteConfig(cfgName);
                    return true;
                });
            }
            itemY += itemH + 4.0F;
        }

        g.setContentHeight(itemY - (listY - g.scroll()), listH);
        g.popClip();
    }

    // --- 3. FRIENDS PAGE ---
    public static void friends(ModernGui g, float x, float y, float w, float h) {
        var titleFont = g.font(9.0F);
        g.text(titleFont, "Список Друзей", x, y, ModernTheme.TEXT());

        // Add friend field
        float fieldY = y + 14.0F;
        float fieldW = 140.0F;
        float fieldH = 16.0F;
        boolean focused = g.focus == OBJECT_FRIEND;

        g.rect(x, fieldY, fieldW, fieldH, 4.0F, ModernTheme.FIELD());
        if (focused) g.outline(x, fieldY, fieldW, fieldH, 4.0F, 0.5F, ModernTheme.accent());

        var inputFont = g.font(7.5F);
        String val = focused ? g.buffer : friendInput;
        if (val.isEmpty()) {
            g.text(inputFont, "Никнейм друга...", x + 6.0F, g.textY(fieldY, fieldH, inputFont), ModernTheme.TEXT_FAINT());
        } else {
            g.text(inputFont, val, x + 6.0F, g.textY(fieldY, fieldH, inputFont), ModernTheme.TEXT());
        }
        g.hit(x, fieldY, fieldW, fieldH, button -> {
            g.focus = OBJECT_FRIEND;
            g.buffer = friendInput;
            return true;
        });

        // Add Button
        float btnX = x + fieldW + 6.0F;
        g.pill(btnX, fieldY, 65.0F, fieldH, "Добавить", null, null, false, "friendadd", button -> {
            String nick = friendInput.trim();
            if (!nick.isEmpty() && Client.INSTANCE != null && Client.INSTANCE.friendManager != null) {
                Client.INSTANCE.friendManager.addFriend(nick);
                friendInput = "";
                g.focus = null;
            }
            return true;
        });

        // Friends List
        float listY = fieldY + fieldH + 8.0F;
        float listH = h - (listY - y);

        g.pushClip(x, listY, w, listH);

        FriendManager friendManager = Client.INSTANCE != null ? Client.INSTANCE.friendManager : null;
        java.util.Collection<String> friends = friendManager != null ? friendManager.getFriends() : List.of();

        float itemY = listY - g.scroll();
        float itemH = 22.0F;
        int cardBorder = ColorUtil.rgba(255, 255, 255, 12);

        if (friends.isEmpty()) {
            g.text(g.font(7.5F), "Список друзей пуст", x, listY + 10.0F, ModernTheme.TEXT_MUTED());
        } else {
            for (String friendNick : friends) {
                if (itemY + itemH > listY && itemY < listY + listH) {
                    int cardBg = ColorUtil.rgba(19, 21, 28, 90);
                    g.rect(x, itemY, w, itemH, 5.0F, cardBg);
                    g.outline(x, itemY, w, itemH, 5.0F, 0.4F, cardBorder);

                    g.text(g.font(8.0F), friendNick, x + 8.0F, itemY + 3.5F, ModernTheme.TEXT());
                    g.text(g.font(6.5F), "Друг клиента", x + 8.0F, itemY + 13.0F, ModernTheme.TEXT_MUTED());

                    float delX = x + w - 55.0F;
                    g.pill(delX, itemY + 3.0F, 50.0F, 16.0F, "Удалить", null, null, false, "delfriend" + friendNick, button -> {
                        if (friendManager != null) friendManager.removeFriend(friendNick);
                        return true;
                    });
                }
                itemY += itemH + 3.0F;
            }
        }

        g.setContentHeight(itemY - (listY - g.scroll()), listH);
        g.popClip();
    }

    // --- 4. THEMES PAGE ---
    public static void themes(ModernGui g, float x, float y, float w, float h) {
        var titleFont = g.font(9.0F);
        g.text(titleFont, "Темы и Цвета Интерфейса", x, y, ModernTheme.TEXT());

        float cardY = y + 14.0F;
        float cardH = 30.0F;
        int cardBg = ColorUtil.rgba(19, 21, 28, 100);
        int cardBorder = ColorUtil.rgba(255, 255, 255, 15);

        g.rect(x, cardY, w, cardH, 6.0F, cardBg);
        g.outline(x, cardY, w, cardH, 6.0F, 0.4F, cardBorder);

        g.text(g.font(8.0F), "Акцентная тема", x + 8.0F, cardY + 5.0F, ModernTheme.TEXT());
        g.text(g.font(6.5F), "Выберите пресет акцентного цвета", x + 8.0F, cardY + 16.0F, ModernTheme.TEXT_MUTED());

        float boxW = 20.0F;
        float boxH = 14.0F;
        float startBoxX = x + w - (ModernTheme.ACCENTS.length * (boxW + 4.0F)) - 8.0F;

        for (int i = 0; i < ModernTheme.ACCENTS.length; i++) {
            int color = ModernTheme.ACCENTS[i];
            float bx = startBoxX + i * (boxW + 4.0F);

            g.rect(bx, cardY + 8.0F, boxW, boxH, 3.0F, color);
            g.outline(bx, cardY + 8.0F, boxW, boxH, 3.0F, 0.6F, ColorUtil.rgba(255, 255, 255, 60));

            g.hit(bx, cardY + 8.0F, boxW, boxH, button -> {
                Theme.setAccentColor(color);
                return true;
            });
        }
    }

    // --- 5. AUTOBUY PAGE ---
    public static void market(ModernGui g, float x, float y, float w, float h) {
        var titleFont = g.font(9.0F);
        g.text(titleFont, "Авто-Покупка (AHHelper)", x, y, ModernTheme.TEXT());

        float cardY = y + 14.0F;
        int cardBg = ColorUtil.rgba(19, 21, 28, 90);
        int cardBorder = ColorUtil.rgba(255, 255, 255, 12);
        g.rect(x, cardY, w, 44.0F, 6.0F, cardBg);
        g.outline(x, cardY, w, 44.0F, 6.0F, 0.4F, cardBorder);

        g.text(g.font(8.0F), "Модуль автоскупки айтемов", x + 10.0F, cardY + 8.0F, ModernTheme.TEXT());
        g.text(g.font(6.5F), "Автоматически скупает предметы на аукционе по заданной минимальной цене", x + 10.0F, cardY + 22.0F, ModernTheme.TEXT_MUTED());
    }

    // --- 6. AUTOSET PAGE ---
    public static void autoset(ModernGui g, float x, float y, float w, float h) {
        var titleFont = g.font(9.0F);
        g.text(titleFont, "Авто-Снаряжение (AutoSet)", x, y, ModernTheme.TEXT());

        float cardY = y + 14.0F;
        int cardBg = ColorUtil.rgba(19, 21, 28, 90);
        int cardBorder = ColorUtil.rgba(255, 255, 255, 12);
        g.rect(x, cardY, w, 44.0F, 6.0F, cardBg);
        g.outline(x, cardY, w, 44.0F, 6.0F, 0.4F, cardBorder);

        g.text(g.font(8.0F), "Автоматический выбор сетов брони и хотбара", x + 10.0F, cardY + 8.0F, ModernTheme.TEXT());
        g.text(g.font(6.5F), "Быстрая экипировка сохраненных комбинаций экипировки", x + 10.0F, cardY + 22.0F, ModernTheme.TEXT_MUTED());
    }

    private static final Object OBJECT_CFG = "cfg_input";
    private static final Object OBJECT_FRIEND = "friend_input";
}
