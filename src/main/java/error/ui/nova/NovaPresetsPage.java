package error.ui.nova;

import error.Client;
import error.config.ConfigManager;
import error.ui.modern.ModernAnim;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.font.Fonts;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * NovaPresetsPage — Вкладка управления пресетами (конфигами) в стиле Liquid Glass.
 */
public final class NovaPresetsPage {

    public static String searchPreset = "";
    public static String newPresetNameBuffer = "";
    public static boolean createModalOpen = false;

    // Toast уведомление "Создан в облаке"
    public static String toastTitle = "";
    public static String toastSub = "";
    public static float toastAnim = 0.0F;

    private NovaPresetsPage() {}

    public static void render(NovaGui g, float x, float y, float w, float h) {
        g.rect(x, y, w, h, 7.0F, ColorUtil.rgba(12, 14, 22, (int)(65 * g.alpha)));
        g.outline(x, y, w, h, 7.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));

        float topH = 26.0F;
        float headerY = y + 6.0F;

        // ── Поисковая строка пресетов ──
        float searchW = 160.0F;
        float searchX = x + 10.0F;
        g.rect(searchX, headerY, searchW, 18.0F, 5.0F, ColorUtil.rgba(18, 20, 30, (int)(140 * g.alpha)));
        g.outline(searchX, headerY, searchW, 18.0F, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(18 * g.alpha)));
        var sf = g.font(6.2F);
        g.text(sf, "🔍 " + (searchPreset.isEmpty() ? "Поиск" : searchPreset), searchX + 8.0F, g.textY(headerY, 18.0F, sf), ColorUtil.multiplyAlpha(searchPreset.isEmpty() ? NovaTheme.TEXT_MUTED() : NovaTheme.TEXT(), g.alpha));

        // ── Кнопка "+ Новый" пресет ──
        float newBtnW = 75.0F;
        float newBtnX = x + w - newBtnW - 10.0F;
        boolean newHov = g.hovered(newBtnX, headerY, newBtnW, 18.0F);
        int newBtnCol = newHov ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(220 * g.alpha)) : ColorUtil.rgba(25, 28, 42, (int)(160 * g.alpha));
        g.rect(newBtnX, headerY, newBtnW, 18.0F, 5.0F, newBtnCol);
        g.outline(newBtnX, headerY, newBtnW, 18.0F, 5.0F, 0.4F, ColorUtil.withAlpha(Theme.getAccentColor(), (int)(140 * g.alpha)));
        var nbf = g.font(6.2F);
        g.textCenter(nbf, "🔄 Новый", newBtnX + newBtnW / 2.0F, g.textY(headerY, 18.0F, nbf), ColorUtil.multiplyAlpha(Theme.getAccentColor(), g.alpha));

        g.hit(newBtnX, headerY, newBtnW, 18.0F, btn -> {
            createModalOpen = true;
            newPresetNameBuffer = "";
            return true;
        });

        // ── Список пресетов ──
        float listY = y + topH + 6.0F;
        float listH = h - topH - 12.0F;

        ConfigManager cfgManager = Client.INSTANCE != null ? Client.INSTANCE.configManager : null;
        List<String> configs = cfgManager != null ? cfgManager.getAvailableConfigs() : List.of("default");

        g.pushClip(x + 5.0F, listY, w - 10.0F, listH);

        float curY = listY - g.scroll;
        float cardH = 46.0F;
        float cardW = w - 20.0F;
        float gap = 6.0F;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd.MM.yy", Locale.getDefault());

        for (String cfgName : configs) {
            if (!searchPreset.isBlank() && !cfgName.toLowerCase().contains(searchPreset.toLowerCase())) continue;

            if (curY + cardH > listY && curY < listY + listH) {
                boolean isCurrent = cfgManager != null && cfgName.equalsIgnoreCase(cfgManager.getCurrentConfig());
                boolean hovered = g.hovered(x + 10.0F, curY, cardW, cardH);

                float hAnim = ModernAnim.value("preset_card:" + cfgName, isCurrent ? 1.0F : (hovered ? 0.4F : 0.0F), 14.0F);

                int cardBg = isCurrent
                        ? ColorUtil.rgba(ColorUtil.red(Theme.getAccentColor()), ColorUtil.green(Theme.getAccentColor()), ColorUtil.blue(Theme.getAccentColor()), (int)((22 + 25 * hAnim) * g.alpha))
                        : ColorUtil.rgba(16, 18, 28, (int)((110 + 35 * hAnim) * g.alpha));

                g.rect(x + 10.0F, curY, cardW, cardH, 6.0F, cardBg);
                int borderAlpha = isCurrent ? 140 : (hovered ? 40 : 18);
                g.outline(x + 10.0F, curY, cardW, cardH, 6.0F, 0.5F, isCurrent ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(borderAlpha * g.alpha)) : ColorUtil.rgba(255, 255, 255, (int)(borderAlpha * g.alpha)));

                // Иконка гачки если текущий активен
                float titleX = x + 18.0F;
                if (isCurrent) {
                    g.text(g.font(6.5F), "✓", titleX, curY + 8.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), g.alpha));
                    titleX += 10.0F;
                }

                // Название пресета
                var tf = g.font(7.5F);
                g.text(tf, cfgName, titleX, curY + 7.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

                // Бейдж автора (walfini 👤)
                float authorX = x + cardW - 55.0F;
                g.rect(authorX, curY + 6.0F, 50.0F, 11.0F, 3.5F, ColorUtil.rgba(255, 255, 255, (int)(12 * g.alpha)));
                g.outline(authorX, curY + 6.0F, 50.0F, 11.0F, 3.5F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(20 * g.alpha)));
                var af = g.font(5.2F);
                g.textCenter(af, "walfini 👤", authorX + 25.0F, g.textY(curY + 6.0F, 11.0F, af), ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

                // Дата обновления
                File file = cfgManager != null ? cfgManager.getConfigFile(cfgName) : null;
                String timeStr = file != null && file.exists() ? sdf.format(new Date(file.lastModified())) : "01.10.26";
                var df = g.font(5.5F);
                g.text(df, "📅 Последнее обновление: " + timeStr, x + 18.0F, curY + 26.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

                // Кнопки действий справой стороны: 📤 Поделиться | 🗑️ Удалить | 💾 Сохранить/Загрузить
                float btnY = curY + 23.0F;
                float actionW = 65.0F;
                float actionX = x + cardW - actionW - 5.0F;

                // Основная кнопка "Загрузить" или "Сохранить"
                boolean actHov = g.hovered(actionX, btnY, actionW, 16.0F);
                int actCol = actHov ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(245 * g.alpha)) : ColorUtil.withAlpha(Theme.getAccentColor(), (int)(190 * g.alpha));
                g.rect(actionX, btnY, actionW, 16.0F, 4.0F, actCol);
                g.outline(actionX, btnY, actionW, 16.0F, 4.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(60 * g.alpha)));
                var abf = g.font(5.8F);
                String btnText = isCurrent ? "💾 Сохранить" : "▶ Загрузить";
                g.textCenter(abf, btnText, actionX + actionW / 2.0F, g.textY(btnY, 16.0F, abf), ColorUtil.rgba(255, 255, 255, (int)(255 * g.alpha)));

                final String targetCfg = cfgName;
                g.hit(actionX, btnY, actionW, 16.0F, btn -> {
                    if (cfgManager != null) {
                        if (isCurrent) {
                            cfgManager.saveConfig(targetCfg, true);
                            showToast("Пресет сохранён", "Локальный конфиг обновлён");
                        } else {
                            cfgManager.loadConfig(targetCfg, true);
                            showToast("Пресет загружен", "Конфигурация применена");
                        }
                    }
                    return true;
                });

                // Кнопка 🗑️ Удалить (если не default)
                if (!cfgName.equalsIgnoreCase("default")) {
                    float delX = actionX - 20.0F;
                    boolean delHov = g.hovered(delX, btnY, 16.0F, 16.0F);
                    g.rect(delX, btnY, 16.0F, 16.0F, 4.0F, delHov ? ColorUtil.rgba(220, 50, 60, (int)(180 * g.alpha)) : ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));
                    var icF = g.font(5.5F);
                    g.textCenter(icF, "🗑", delX + 8.0F, g.textY(btnY, 16.0F, icF), ColorUtil.rgba(255, 255, 255, (int)(220 * g.alpha)));
                    g.hit(delX, btnY, 16.0F, 16.0F, btn -> {
                        if (cfgManager != null) {
                            File f = cfgManager.getConfigFile(targetCfg);
                            if (f.exists()) f.delete();
                            showToast("Пресет удалён", targetCfg + " удален из папки");
                        }
                        return true;
                    });
                }
            }

            curY += cardH + gap;
        }

        g.popClip();

        float totalH = configs.size() * (cardH + gap);
        g.maxRightScroll = Math.max(0.0F, totalH - listH);

        // ── Модальное окно создания нового пресета ──
        if (createModalOpen) {
            renderCreateModal(g, x, y, w, h);
        }

        // ── Тоаст уведомление в стиле скриншота 3 ──
        if (toastAnim > 0.01F) {
            renderToast(g, x, y, w, h);
        }
    }

    private static void renderCreateModal(NovaGui g, float x, float y, float w, float h) {
        g.rect(0, 0, g.width, g.height, 0.0F, ColorUtil.rgba(0, 0, 0, (int)(160 * g.alpha)));

        float mw = 220.0F;
        float mh = 105.0F;
        float mx = (g.width - mw) / 2.0F;
        float my = (g.height - mh) / 2.0F;

        g.rect(mx, my, mw, mh, 8.0F, ColorUtil.rgba(16, 18, 28, (int)(245 * g.alpha)));
        g.outline(mx, my, mw, mh, 8.0F, 0.5F, ColorUtil.withAlpha(Theme.getAccentColor(), (int)(180 * g.alpha)));

        g.text(g.font(8.0F), "Создать новый пресет", mx + 12.0F, my + 10.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

        // Поле ввода имени
        float inputY = my + 30.0F;
        g.rect(mx + 12.0F, inputY, mw - 24.0F, 20.0F, 5.0F, ColorUtil.rgba(24, 28, 42, (int)(200 * g.alpha)));
        g.outline(mx + 12.0F, inputY, mw - 24.0F, 20.0F, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(30 * g.alpha)));

        var f = g.font(6.5F);
        String txt = newPresetNameBuffer.isEmpty() ? "Название пресета..." : newPresetNameBuffer;
        g.text(f, txt, mx + 18.0F, g.textY(inputY, 20.0F, f), ColorUtil.multiplyAlpha(newPresetNameBuffer.isEmpty() ? NovaTheme.TEXT_MUTED() : NovaTheme.TEXT(), g.alpha));

        // Кнопка Отмена
        float btnW = 90.0F;
        float btnY = my + mh - 26.0F;
        g.rect(mx + 12.0F, btnY, btnW, 18.0F, 5.0F, ColorUtil.rgba(255, 255, 255, (int)(20 * g.alpha)));
        g.textCenter(f, "Отмена", mx + 12.0F + btnW / 2.0F, g.textY(btnY, 18.0F, f), ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));
        g.hit(mx + 12.0F, btnY, btnW, 18.0F, btn -> { createModalOpen = false; return true; });

        // Кнопка Создать
        float createX = mx + mw - btnW - 12.0F;
        g.rect(createX, btnY, btnW, 18.0F, 5.0F, Theme.getAccentColor());
        g.textCenter(f, "Создать", createX + btnW / 2.0F, g.textY(btnY, 18.0F, f), ColorUtil.rgba(255, 255, 255, (int)(255 * g.alpha)));
        g.hit(createX, btnY, btnW, 18.0F, btn -> {
            if (!newPresetNameBuffer.trim().isEmpty() && Client.INSTANCE != null && Client.INSTANCE.configManager != null) {
                Client.INSTANCE.configManager.saveConfig(newPresetNameBuffer.trim(), true);
                showToast("Новый пресет", "Создан в облаке");
            }
            createModalOpen = false;
            return true;
        });
    }

    private static void renderToast(NovaGui g, float x, float y, float w, float h) {
        toastAnim = ModernAnim.approach(toastAnim, 0.0F, 25.0F);
        float tw = 140.0F;
        float th = 38.0F;
        float tx = g.width - tw - 15.0F;
        float ty = 15.0F;

        g.rect(tx, ty, tw, th, 8.0F, ColorUtil.rgba(18, 20, 32, (int)(230 * g.alpha)));
        g.outline(tx, ty, tw, th, 8.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(25 * g.alpha)));

        g.rect(tx + 4.0F, ty + 4.0F, 28.0F, 30.0F, 6.0F, ColorUtil.rgba(255, 255, 255, (int)(12 * g.alpha)));
        g.textCenter(g.font(8.0F), "☁", tx + 18.0F, ty + 11.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), g.alpha));

        var tf = g.font(6.5F);
        g.text(tf, toastTitle, tx + 38.0F, ty + 8.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));
        var sf = g.font(5.5F);
        g.text(sf, toastSub, tx + 38.0F, ty + 20.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

        g.rect(tx + tw - 4.0F, ty + 8.0F, 2.0F, 22.0F, 1.0F, Theme.getAccentColor());
    }

    public static void showToast(String title, String sub) {
        toastTitle = title;
        toastSub = sub;
        toastAnim = 1.0F;
    }
}
