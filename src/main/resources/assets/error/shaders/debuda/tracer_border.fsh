#version 150

// «Tracer Border» — пер-пиксельная реализация стиля V7 из
// design-md/isle-hud-variants.html. Дословно повторяет CSS:
//
//   border:1.5px solid transparent; border-radius:8px;
//   background:
//     linear-gradient(bg,bg) padding-box,
//     conic-gradient(from var(--ang),
//       transparent 0deg, transparent 295deg,
//       var(--accent) 335deg, #ffffff 350deg, var(--accent) 352deg, transparent 360deg)
//     border-box;
//   box-shadow:0 0 12px rgba(181,123,255,.12);
//   animation: spin 2.8s linear infinite;   // --ang: 0deg -> 360deg
//
// Каждый пиксель бордера красится по УГЛУ ОТ ЦЕНТРА панели (conic), а не по
// длине периметра — поэтому свечение «растекается» вдоль коротких граней и
// сжимается на длинных, ровно как в браузере. Плюс ровное мягкое внешнее гало
// (box-shadow).
//
// Quad на экране = RectSize + 2*Padding, Padding = GlowWidth (запас под гало).

in vec2 TexCoord;

uniform vec2  RectSize;     // визуальный размер панели в пикселях
uniform vec2  Padding;      // = GlowWidth с каждой стороны (под внешнее гало)
uniform float Radius;       // скругление углов (тот же, что у панели)
uniform float BorderWidth;  // толщина линии-бордера (CSS border:1.5px)
uniform float Time;         // фаза вращения 0..1 (= --ang/360)
uniform vec3  Accent;       // accent-цвет (хвост/тело кометы)
uniform vec3  Head;         // цвет головы (белый #ffffff)
uniform float GlowWidth;    // дистанция затухания внешнего гало наружу (px)
uniform float GlowAlpha;    // прозрачность внешнего гало 0..1 (box-shadow ~.12)
uniform float Alpha;        // мастер-прозрачность всего эффекта 0..1

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

void main() {
    vec2 fragInQuad = TexCoord * (RectSize + 2.0 * Padding);
    vec2 p = fragInQuad - Padding - RectSize * 0.5;
    vec2 halfSize = RectSize * 0.5;
    float r = min(Radius, min(halfSize.x, halfSize.y));

    float d = sdRoundedRect(p, halfSize, r);   // <0 внутри, 0 на кромке

    // --- conic-стоп s для этого пикселя ---
    // Угол от центра, по часовой от верха (CSS conic): 0=верх, 90=право...
    float ang = degrees(atan(p.x, -p.y));      // (-180,180]
    ang = mod(ang, 360.0);
    float s = mod(ang - Time * 360.0, 360.0);  // = θ - --ang

    // --- профиль кометы: альфа + цвет, дословно по CSS-стопам ---
    float ca;            // альфа кометы
    vec3  cc = Accent;   // цвет
    if (s < 295.0) {
        ca = 0.0;
    } else if (s < 335.0) {
        ca = (s - 295.0) / 40.0;                                 // хвост: 0→1
        cc = Accent;
    } else if (s < 350.0) {
        ca = 1.0;
        cc = mix(Accent, Head, (s - 335.0) / 15.0);              // разгон в белый
    } else if (s < 352.0) {
        ca = 1.0;
        cc = mix(Head, Accent, (s - 350.0) / 2.0);               // голова → accent
    } else {
        ca = 1.0 - (s - 352.0) / 8.0;                            // спад в прозрачность
        cc = Accent;
    }

    // --- маска кольца-бордера: -BorderWidth <= d <= 0 (CSS border внутри) ---
    float aa = 0.6;
    float ring = smoothstep(-BorderWidth - aa, -BorderWidth + aa, d)
               - smoothstep(-aa, aa, d);
    ring = clamp(ring, 0.0, 1.0);

    vec4 cBorder = vec4(cc, ca * ring);

    // --- внешнее гало (box-shadow): ровное мягкое свечение наружу, accent ---
    float falloff = 1.0 - smoothstep(0.0, max(GlowWidth, 0.0001), d);
    float outside = smoothstep(-0.5, 1.0, d);   // только снаружи кромки
    vec4 cGlow = vec4(Accent, falloff * falloff * outside * GlowAlpha);

    // Композиция: гало снизу, бордер сверху.
    vec4 col = over(cGlow, cBorder);
    col.a *= Alpha;

    if (col.a < 0.002) discard;
    OutColor = col;
}
