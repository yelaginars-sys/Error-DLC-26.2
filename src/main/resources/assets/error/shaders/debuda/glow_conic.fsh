#version 150

// Переливающееся oil-slick свечение вокруг rounded rect (вариант D из
// design-md/isle-glow-preview.html). Перелив — ДИАГОНАЛЬНЫЙ линейный (как
// наклонённая голографическая плёнка), полосы цвета текут по диагонали и
// движутся по времени. Раньше был conic (по углу от центра) — цвета сходились
// в одну точку = «воронка» в центре панели; линейная проекция её убирает.
//
// Два слоя:
//   снаружи — яркое кольцо с затуханием на GlowWidth;
//   внутри  — ровная слабая подсветка InnerAlpha: панель рисуется поверх и
//             приглушает её своим стеклом, фон панели мягко переливается.
//
// Quad на экране = RectSize + 2*Padding, Padding = GlowWidth.

in vec2 TexCoord;

uniform vec2  RectSize;   // визуальный размер rect'а в пикселях
uniform vec2  Padding;    // = GlowWidth с каждой стороны
uniform float Radius;     // скругление углов панели
uniform float GlowWidth;  // дистанция затухания свечения наружу
uniform float Time;       // фаза вращения 0..1 (один оборот за период)
uniform float Alpha;      // прозрачность внешнего кольца 0..1
uniform float InnerAlpha; // прозрачность подсветки ПОД панелью 0..1

out vec4 OutColor;

float sdRoundedRect(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// Oil-slick палитра: фиолет → циан → розовый → глубокий фиолет → мята.
// Те же опорные цвета, что в conic-gradient превью.
vec3 palette(float t) {
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
    float dist = sdRoundedRect(p, RectSize * 0.5, Radius);

    // Внешнее кольцо: затухание наружу, квадрат альфы — мягкий «дымчатый» хвост.
    float falloff = 1.0 - smoothstep(0.0, max(GlowWidth, 0.0001), dist);
    float outside = smoothstep(-1.5, 0.5, dist);
    float ringA = falloff * falloff * outside * Alpha;

    // Внутренняя подсветка: ровный слой под всей панелью, стекло сверху
    // приглушит его до лёгкого перелива фона.
    float innerA = (1.0 - outside) * InnerAlpha;

    float a = ringA + innerA;
    if (a < 0.004) discard;

    // Диагональная линейная проекция: цвет зависит от положения вдоль диагонали
    // (x+y), а не от угла — нет точки схождения. Нормируем по размеру панели,
    // чтобы число полос не зависело от её габаритов; Time гонит полосы.
    vec2 ext = RectSize * 0.5 + Padding;
    vec2 uvN = p / max(ext, vec2(0.0001));
    float t = (uvN.x + uvN.y) * 0.5;   // диагональ примерно [-1, 1]
    vec3 rgb = palette(t * 0.7 + Time);

    OutColor = vec4(rgb, a);
}
