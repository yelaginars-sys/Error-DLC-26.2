#version 150

// SDF rounded rectangle: fill + 1px border + offset drop-shadow в одном проходе.
//
// Координаты:
// Quad на экране = RectSize + 2*Padding (Padding оставляет место под shadow).
// TexCoord ∈ [0,1] по этому quad'у. p — позиция фрагмента относительно
// центра rect'а в пикселях (отрицательная за пределами rect'а).
//
// Для отключения компонента — выставить альфу в 0:
//   shadow off: ShadowColor.a = 0
//   border off: BorderColor.a = 0 || BorderWidth = 0

in vec2 TexCoord;

uniform vec2  RectSize;     // визуальный размер rect'а в пикселях
uniform vec2  Padding;      // запас на shadow с каждой стороны (симметричный)
uniform float Radius;       // скругление углов в пикселях
uniform float BorderWidth;  // толщина бордера в пикселях (0 = выключен)
uniform vec4  FillColor;    // цвет заливки (RGBA, straight alpha)
uniform vec4  FillColor2;   // второй цвет диагонального градиента заливки
uniform float GradientMix;  // 0 = сплошная заливка FillColor, 1 = градиент 135° FillColor→FillColor2
uniform vec4  BorderColor;  // цвет бордера
uniform vec2  ShadowOffset; // смещение тени относительно rect'а
uniform float ShadowBlur;   // мягкость тени в пикселях (расстояние до 0 alpha)
uniform vec4  ShadowColor;  // цвет тени
uniform float Smoothness;   // ширина AA-кромки fill/border (~1px)

// Переливающийся oil-slick глоу (бывший glow_conic) — слит сюда, чтобы панель
// рисовалась ОДНИМ draw'ом вместо двух с переключением программы (glow → rect).
// GlowAlpha = GlowInnerAlpha = 0 → ветка целиком пропускается, обычные rect'ы
// не платят ничего. Семантика 1:1 с glow_conic.fsh.
uniform float GlowWidth;      // дистанция затухания кольца наружу (px)
uniform float GlowTime;       // фаза перелива 0..1
uniform float GlowAlpha;      // прозрачность внешнего кольца 0..1
uniform float GlowInnerAlpha; // подсветка ПОД панелью 0..1

out vec4 OutColor;

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

vec4 over(vec4 under, vec4 above) {
    float a = above.a + under.a * (1.0 - above.a);
    if (a < 0.0001) return vec4(0.0);
    vec3 rgb = (above.rgb * above.a + under.rgb * under.a * (1.0 - above.a)) / a;
    return vec4(rgb, a);
}

// Oil-slick палитра: фиолет → циан → розовый → глубокий фиолет → мята.
vec3 glowPalette(float t) {
    const vec3 c0 = vec3(0.710, 0.482, 1.000); // #B57BFF
    const vec3 c1 = vec3(0.384, 0.847, 1.000); // #62D8FF
    const vec3 c2 = vec3(1.000, 0.482, 0.835); // #FF7BD5
    const vec3 c3 = vec3(0.478, 0.235, 0.878); // #7A3CE0
    const vec3 c4 = vec3(0.612, 1.000, 0.878); // #9CFFE0
    float s = fract(t) * 5.0;
    vec3 a; vec3 b;
    if      (s < 1.0) { a = c0; b = c1; }
    else if (s < 2.0) { a = c1; b = c2; }
    else if (s < 3.0) { a = c2; b = c3; }
    else if (s < 4.0) { a = c3; b = c4; }
    else              { a = c4; b = c0; }
    return mix(a, b, smoothstep(0.0, 1.0, fract(s)));
}

void main() {
    vec2 fragInQuad = TexCoord * (RectSize + 2.0 * Padding);
    vec2 p = fragInQuad - Padding - RectSize * 0.5;
    vec2 halfSize = RectSize * 0.5;

    float distMain = sdRoundedRect(p, halfSize, Radius);
    float distShadow = sdRoundedRect(p - ShadowOffset, halfSize, Radius);

    // Shadow: гладкое затухание от края тени до 0 на расстоянии ShadowBlur.
    // Внутри shadow rect (distShadow <= 0) альфа = 1.
    float shadowA = 1.0 - smoothstep(0.0, max(ShadowBlur, 0.0001), distShadow);

    // ----- AA маски для трёх зон: outside / border / fill -----
    // Outer edge AA: 1 внутри rect'а, 0 снаружи. Половина перехода — sub-pixel
    // AA только, чтобы не размазывать линию.
    float aaOuter = 1.0 - smoothstep(-Smoothness, +Smoothness, distMain);
    // Inner edge AA: 1 в зоне fill (глубже чем BorderWidth от края), 0 в border-полосе.
    // distMain + BorderWidth = расстояние до ВНУТРЕННЕЙ кромки бордера.
    float aaInner = (BorderWidth > 0.0)
            ? smoothstep(-Smoothness, +Smoothness, distMain + BorderWidth)
            : 1.0;

    // Fill — там где aaOuter=1 И aaInner=1. На границе border'а aaInner спадает.
    float fillA   = aaOuter * aaInner;
    // Border — там где aaOuter=1 НО aaInner=0 (пиксель в border-полосе).
    float borderA = aaOuter * (1.0 - aaInner);

    // Диагональный (135°) градиент заливки: t растёт от верхнего-левого угла
    // к нижнему-правому. GradientMix=0 — обычная сплошная заливка.
    vec2 uvRect = clamp((p + halfSize) / max(RectSize, vec2(0.0001)), 0.0, 1.0);
    float diag = clamp((uvRect.x + uvRect.y) * 0.5, 0.0, 1.0);
    vec4 fillMixed = mix(FillColor, FillColor2, GradientMix * diag);

    vec4 cShadow = vec4(ShadowColor.rgb, ShadowColor.a * shadowA);
    vec4 cFill   = vec4(fillMixed.rgb,   fillMixed.a   * fillA);
    vec4 cBorder = vec4(BorderColor.rgb, BorderColor.a * borderA);

    // Глоу-слой — САМЫЙ НИЖНИЙ (как раньше отдельный glow_conic-проход до
    // rect'а): кольцо снаружи + ровная подсветка под панелью; стекло сверху
    // приглушает её до лёгкого перелива.
    vec4 cGlow = vec4(0.0);
    if (GlowAlpha > 0.0 || GlowInnerAlpha > 0.0) {
        float falloff = 1.0 - smoothstep(0.0, max(GlowWidth, 0.0001), distMain);
        float outside = smoothstep(-1.5, 0.5, distMain);
        float glowA = falloff * falloff * outside * GlowAlpha
                    + (1.0 - outside) * GlowInnerAlpha;
        // Нормировка диагонали по экстенту glow-quad'а (halfSize + GlowWidth),
        // как в glow_conic с Padding=GlowWidth — плотность полос не зависит от
        // фактического Padding (он мог вырасти из-за shadow).
        vec2 ext = halfSize + vec2(GlowWidth);
        vec2 uvN = p / max(ext, vec2(0.0001));
        float gt = (uvN.x + uvN.y) * 0.5;
        cGlow = vec4(glowPalette(gt * 0.7 + GlowTime), glowA);
    }

    // Композиция снизу-вверх: глоу → тень → заливка → бордер.
    vec4 color = vec4(0.0);
    color = over(color, cGlow);
    color = over(color, cShadow);
    color = over(color, cFill);
    color = over(color, cBorder);

    if (color.a < 0.001) discard;
    OutColor = color;
}
