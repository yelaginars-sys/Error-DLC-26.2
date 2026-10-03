package error.ui.nova;

import error.account.AccountManager;
import error.ui.modern.ModernAnim;
import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.font.Fonts;

import java.util.List;

/**
 * NovaAccountsPage — Вкладка управления аккаунтами (Alt Manager) в стиле Liquid Glass.
 * Полное соответствие макетам скриншотов 1 и 4.
 */
public final class NovaAccountsPage {

    public static String nicknameBuffer = "";
    public static boolean addModalOpen = false;

    private NovaAccountsPage() {}

    public static void render(NovaGui g, float x, float y, float w, float h) {
        g.rect(x, y, w, h, 7.0F, ColorUtil.rgba(12, 14, 22, (int)(65 * g.alpha)));
        g.outline(x, y, w, h, 7.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));

        float topH = 26.0F;
        float headerY = y + 6.0F;

        // ── Поисковая строка аккаунтов ──
        float searchW = 150.0F;
        float searchX = x + 10.0F;
        g.rect(searchX, headerY, searchW, 18.0F, 5.0F, ColorUtil.rgba(18, 20, 30, (int)(140 * g.alpha)));
        g.outline(searchX, headerY, searchW, 18.0F, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(18 * g.alpha)));
        var sf = g.font(6.2F);
        g.text(sf, "🔍 Поиск аккаунтов...", searchX + 8.0F, g.textY(headerY, 18.0F, sf), ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

        // ── Кнопка "+ Добавить" аккаунт ──
        float addBtnW = 80.0F;
        float addBtnX = x + w - addBtnW - 10.0F;
        boolean addHov = g.hovered(addBtnX, headerY, addBtnW, 18.0F);
        int addBtnCol = addHov ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(220 * g.alpha)) : ColorUtil.rgba(25, 28, 42, (int)(160 * g.alpha));
        g.rect(addBtnX, headerY, addBtnW, 18.0F, 5.0F, addBtnCol);
        g.outline(addBtnX, headerY, addBtnW, 18.0F, 5.0F, 0.4F, ColorUtil.withAlpha(Theme.getAccentColor(), (int)(140 * g.alpha)));
        var abf = g.font(6.2F);
        g.textCenter(abf, "👤 Добавить", addBtnX + addBtnW / 2.0F, g.textY(headerY, 18.0F, abf), ColorUtil.multiplyAlpha(Theme.getAccentColor(), g.alpha));

        g.hit(addBtnX, headerY, addBtnW, 18.0F, btn -> {
            addModalOpen = true;
            nicknameBuffer = "";
            return true;
        });

        // ── Список аккаунтов ──
        float listY = y + topH + 6.0F;
        float listH = h - topH - 12.0F;

        AccountManager accManager = AccountManager.getInstance();
        List<String> accounts = accManager.getAccounts();

        g.pushClip(x + 5.0F, listY, w - 10.0F, listH);

        float curY = listY - g.scroll;
        float cardH = 46.0F;
        float cardW = w - 20.0F;
        float gap = 6.0F;

        for (String accName : accounts) {
            if (curY + cardH > listY && curY < listY + listH) {
                boolean isActive = accName.equalsIgnoreCase(accManager.getActiveAccount());
                boolean isFav = accManager.isFavorite(accName);
                boolean hovered = g.hovered(x + 10.0F, curY, cardW, cardH);

                float hAnim = ModernAnim.value("acc_card:" + accName, isActive ? 1.0F : (hovered ? 0.4F : 0.0F), 14.0F);

                int cardBg = isActive
                        ? ColorUtil.rgba(ColorUtil.red(Theme.getAccentColor()), ColorUtil.green(Theme.getAccentColor()), ColorUtil.blue(Theme.getAccentColor()), (int)((22 + 25 * hAnim) * g.alpha))
                        : ColorUtil.rgba(16, 18, 28, (int)((110 + 35 * hAnim) * g.alpha));

                g.rect(x + 10.0F, curY, cardW, cardH, 6.0F, cardBg);
                int borderAlpha = isActive ? 140 : (hovered ? 40 : 18);
                g.outline(x + 10.0F, curY, cardW, cardH, 6.0F, 0.5F, isActive ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(borderAlpha * g.alpha)) : ColorUtil.rgba(255, 255, 255, (int)(borderAlpha * g.alpha)));

                // Аватарка аккаунта (квадратик головы с иконкой)
                float headX = x + 16.0F;
                float headY = curY + 11.0F;
                g.rect(headX, headY, 24.0F, 24.0F, 5.0F, ColorUtil.rgba(40, 44, 62, (int)(200 * g.alpha)));
                g.outline(headX, headY, 24.0F, 24.0F, 5.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(30 * g.alpha)));
                g.textCenter(g.font(9.0F), "👤", headX + 12.0F, headY + 5.0F, ColorUtil.multiplyAlpha(Theme.getAccentColor(), g.alpha));

                // Никнейм
                float titleX = headX + 30.0F;
                var tf = g.font(7.5F);
                g.text(tf, accName, titleX, curY + 9.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

                // Бейдж типа профиля (офлайн 🔒 / майкрософт 🔲)
                float badgeX = x + cardW - 60.0F;
                g.rect(badgeX, curY + 8.0F, 55.0F, 11.0F, 3.5F, ColorUtil.rgba(255, 255, 255, (int)(12 * g.alpha)));
                g.outline(badgeX, curY + 8.0F, 55.0F, 11.0F, 3.5F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(20 * g.alpha)));
                var bf = g.font(5.2F);
                g.textCenter(bf, "офлайн 🔒", badgeX + 27.5F, g.textY(curY + 8.0F, 11.0F, bf), ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

                // Время создания
                var df = g.font(5.5F);
                g.text(df, "📅 Создан: 20:50 30.09.26", titleX, curY + 26.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

                // Кнопки справа: ⭐ Избранное | 🗑️ Удалить | 🚪 Войти
                float btnY = curY + 23.0F;
                float loginW = 55.0F;
                float loginX = x + cardW - loginW - 5.0F;

                boolean loginHov = g.hovered(loginX, btnY, loginW, 16.0F);
                int loginCol = isActive ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(255 * g.alpha)) : (loginHov ? ColorUtil.withAlpha(Theme.getAccentColor(), (int)(220 * g.alpha)) : ColorUtil.rgba(255, 255, 255, (int)(18 * g.alpha)));
                g.rect(loginX, btnY, loginW, 16.0F, 4.0F, loginCol);
                g.outline(loginX, btnY, loginW, 16.0F, 4.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(50 * g.alpha)));

                var lbf = g.font(5.8F);
                g.textCenter(lbf, isActive ? "Активен" : "🚪 Войти", loginX + loginW / 2.0F, g.textY(btnY, 16.0F, lbf), ColorUtil.rgba(255, 255, 255, (int)(255 * g.alpha)));

                final String targetAcc = accName;
                g.hit(loginX, btnY, loginW, 16.0F, btn -> {
                    accManager.setActiveAccount(targetAcc);
                    accManager.applyActiveSession();
                    return true;
                });

                // Кнопка 🗑️ Удалить
                float delX = loginX - 20.0F;
                boolean delHov = g.hovered(delX, btnY, 16.0F, 16.0F);
                g.rect(delX, btnY, 16.0F, 16.0F, 4.0F, delHov ? ColorUtil.rgba(220, 50, 60, (int)(180 * g.alpha)) : ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));
                var icF = g.font(5.5F);
                g.textCenter(icF, "🗑", delX + 8.0F, g.textY(btnY, 16.0F, icF), ColorUtil.rgba(255, 255, 255, (int)(220 * g.alpha)));
                g.hit(delX, btnY, 16.0F, 16.0F, btn -> {
                    accManager.removeAccount(targetAcc);
                    return true;
                });

                // Кнопка ⭐ Избранное
                float favX = delX - 20.0F;
                boolean favHov = g.hovered(favX, btnY, 16.0F, 16.0F);
                int favCol = isFav ? ColorUtil.rgba(255, 190, 40, (int)(230 * g.alpha)) : (favHov ? ColorUtil.rgba(255, 255, 255, (int)(40 * g.alpha)) : ColorUtil.rgba(255, 255, 255, (int)(15 * g.alpha)));
                g.rect(favX, btnY, 16.0F, 16.0F, 4.0F, favCol);
                g.textCenter(icF, isFav ? "★" : "☆", favX + 8.0F, g.textY(btnY, 16.0F, icF), ColorUtil.rgba(255, 255, 255, (int)(255 * g.alpha)));
                g.hit(favX, btnY, 16.0F, 16.0F, btn -> {
                    accManager.toggleFavorite(targetAcc);
                    return true;
                });
            }

            curY += cardH + gap;
        }

        g.popClip();

        float totalH = accounts.size() * (cardH + gap);
        g.maxRightScroll = Math.max(0.0F, totalH - listH);

        // ── Модальное окно "Добавить аккаунт" (Скриншот 1) ──
        if (addModalOpen) {
            renderAddAccountModal(g, x, y, w, h);
        }
    }

    private static void renderAddAccountModal(NovaGui g, float x, float y, float w, float h) {
        g.rect(0, 0, g.width, g.height, 0.0F, ColorUtil.rgba(0, 0, 0, (int)(170 * g.alpha)));

        float mw = 260.0F;
        float mh = 175.0F;
        float mx = (g.width - mw) / 2.0F;
        float my = (g.height - mh) / 2.0F;

        // Модальное окно с закругленными углами в стиле скриншота 1
        g.rect(mx, my, mw, mh, 10.0F, ColorUtil.rgba(18, 20, 28, (int)(250 * g.alpha)));
        g.outline(mx, my, mw, mh, 10.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(30 * g.alpha)));

        // Крестик закрытия справа вверху (X)
        float closeX = mx + mw - 22.0F;
        float closeY = my + 10.0F;
        g.text(g.font(7.5F), "✕", closeX, closeY, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));
        g.hit(closeX - 2.0F, closeY - 2.0F, 14.0F, 14.0F, btn -> { addModalOpen = false; return true; });

        // Шапка модального окна
        g.text(g.font(9.0F), "👤  Добавить аккаунт", mx + 14.0F, my + 12.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

        var subF = g.font(5.5F);
        g.text(subF, "Введите оффлайн-никнейм, чтобы создать локальный профиль,", mx + 14.0F, my + 27.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));
        g.text(subF, "или авторизуйте свой официальный аккаунт Microsoft.", mx + 14.0F, my + 37.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

        // Поле ввода Никнейма (как на скриншоте 1)
        g.text(g.font(6.2F), "Никнейм", mx + 14.0F, my + 54.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_SEC(), g.alpha));

        float inputY = my + 66.0F;
        g.rect(mx + 14.0F, inputY, mw - 28.0F, 22.0F, 6.0F, ColorUtil.rgba(26, 30, 44, (int)(220 * g.alpha)));
        g.outline(mx + 14.0F, inputY, mw - 28.0F, 22.0F, 6.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(25 * g.alpha)));

        g.text(g.font(7.0F), "👤", mx + 22.0F, inputY + 4.0F, ColorUtil.multiplyAlpha(NovaTheme.TEXT_MUTED(), g.alpha));

        var f = g.font(6.5F);
        String txt = nicknameBuffer.isEmpty() ? "Никнейм" : nicknameBuffer;
        g.text(f, txt, mx + 36.0F, g.textY(inputY, 22.0F, f), ColorUtil.multiplyAlpha(nicknameBuffer.isEmpty() ? NovaTheme.TEXT_MUTED() : NovaTheme.TEXT(), g.alpha));

        // Кнопка 1: "👤 Добавить" (Локальный профиль)
        float btn1Y = my + 98.0F;
        float btnW = mw - 28.0F;
        g.rect(mx + 14.0F, btn1Y, btnW, 26.0F, 7.0F, Theme.getAccentColor());
        g.outline(mx + 14.0F, btn1Y, btnW, 26.0F, 7.0F, 0.5F, ColorUtil.rgba(255, 255, 255, (int)(60 * g.alpha)));
        g.textCenter(g.font(7.2F), "👤  Добавить", mx + 14.0F + btnW / 2.0F, g.textY(btn1Y, 26.0F, g.font(7.2F)), ColorUtil.rgba(255, 255, 255, (int)(255 * g.alpha)));

        g.hit(mx + 14.0F, btn1Y, btnW, 26.0F, btn -> {
            if (!nicknameBuffer.trim().isEmpty()) {
                AccountManager.getInstance().addAccount(nicknameBuffer.trim());
                addModalOpen = false;
            }
            return true;
        });

        // Кнопка 2: "田 Microsoft" (Microsoft Авторизация)
        float btn2Y = my + 130.0F;
        g.rect(mx + 14.0F, btn2Y, btnW, 26.0F, 7.0F, ColorUtil.rgba(28, 32, 48, (int)(200 * g.alpha)));
        g.outline(mx + 14.0F, btn2Y, btnW, 26.0F, 7.0F, 0.4F, ColorUtil.rgba(255, 255, 255, (int)(30 * g.alpha)));
        g.textCenter(g.font(7.2F), "田  Microsoft", mx + 14.0F + btnW / 2.0F, g.textY(btn2Y, 26.0F, g.font(7.2F)), ColorUtil.multiplyAlpha(NovaTheme.TEXT(), g.alpha));

        g.hit(mx + 14.0F, btn2Y, btnW, 26.0F, btn -> {
            // Microsoft auth trigger
            addModalOpen = false;
            return true;
        });
    }
}
