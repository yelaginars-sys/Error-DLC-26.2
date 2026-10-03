package error.ui.nova;

import error.util.client.clients.ColorUtil;
import error.util.client.clients.Theme;
import error.util.render.Render2D;

/**
 * NovaShader — Единый самый красивый шейдер Жидкого Стекла (Liquid Glass).
 * Не меняет цвета сам по себе, а аккуратно использует Основной и Доп цвет темы,
 * создавая эффект живого глубокого кристального стекла с жидкими переливами.
 */
public final class NovaShader {

    private NovaShader() {}

    public static void drawBackdrop(NovaGui g, float bx, float by, float bw, float bh,
                                    float radius) {
        float alpha = (g != null) ? g.alpha : 1.0F;
        drawBackdropWithAlpha(alpha, bx, by, bw, bh, radius);
    }

    public static void drawBackdropWithAlpha(float alpha, float bx, float by, float bw, float bh,
                                            float radius) {
        float time = (System.currentTimeMillis() % 100_000L) / 1000.0F;

        // Получаем основной и доп цвет темы (не сменяются сами по себе!)
        int primaryAccent   = Theme.getAccentColor();
        int secondaryAccent = Theme.getSecondaryColor();

        int pr = ColorUtil.red(primaryAccent);
        int pg = ColorUtil.green(primaryAccent);
        int pb = ColorUtil.blue(primaryAccent);

        int sr = ColorUtil.red(secondaryAccent);
        int sg = ColorUtil.green(secondaryAccent);
        int sb = ColorUtil.blue(secondaryAccent);

        // Плавное движение волн жидкого стекла (без смены тона цветности)
        float wave1 = (float) Math.sin(time * 0.75) * 0.5F + 0.5F;
        float wave2 = (float) Math.cos(time * 0.50 + 1.2) * 0.5F + 0.5F;

        // Углы градиента Жидкого Стекла с аккуратной прозрачностью
        int cTL = ColorUtil.rgba(clamp((int)(pr * 0.85F + wave1 * 15)), clamp((int)(pg * 0.85F + wave1 * 15)), clamp((int)(pb * 0.85F + wave1 * 15)), a(180, alpha));
        int cTR = ColorUtil.rgba(clamp((int)(sr * 0.80F + wave2 * 15)), clamp((int)(sg * 0.80F + wave2 * 15)), clamp((int)(sb * 0.80F + wave2 * 15)), a(165, alpha));
        int cBL = ColorUtil.rgba(clamp((int)(sr * 0.45F)), clamp((int)(sg * 0.45F)), clamp((int)(sb * 0.45F)), a(175, alpha));
        int cBR = ColorUtil.rgba(10, 12, 20, a(210, alpha));

        // 1. Единая подложка Liquid Glass
        Render2D.drawGradientRound(bx, by, bw, bh, radius, cTL, cTR, cBR, cBL);

        // 2. Наложение маски кристаллического шума noise.png
        try {
            Render2D.drawTexture("textures/system/noise.png", bx, by, bw, bh, radius, ColorUtil.rgba(255, 255, 255, a(9, alpha)));
        } catch (Throwable ignored) {}

        // 3. Градиентный блик жидкого стекла (Refraction Light)
        int shimmerTop = ColorUtil.rgba(255, 255, 255, a(12, alpha));
        int shimmerBot = ColorUtil.rgba(255, 255, 255, 0);
        Render2D.drawGradientRound(bx, by, bw, bh * 0.30F, radius, shimmerTop, shimmerTop, shimmerBot, shimmerBot);
    }

    public static float accentPulse(float time) {
        return 0.85F + 0.15F * (float) Math.sin(time * 2.2);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static int a(int base, float alpha) { return Math.max(0, Math.min(255, (int)(base * alpha))); }
}
