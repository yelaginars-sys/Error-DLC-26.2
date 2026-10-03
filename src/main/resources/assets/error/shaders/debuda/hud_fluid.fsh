#version 150

// Порт wonderful:core/fluidhud/fluid → isle-client. Анимированный «жидкий» фон
// плашек HUD: три процедурных режима (Mode 1/2/3), обрезанные по SDF скруглённого
// прямоугольника, перекрашенные в цвет темы. rdist/ralpha встроены (были в
// wonderful:common.glsl) — hud_* шейдеры обязаны быть самодостаточными.

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;           // размер плашки (px)
uniform vec4 Radius;         // радиус на каждый угол
uniform float Smoothness;    // мягкость кромки
uniform float Time;          // секунды, уже умноженные на скорость из настроек
uniform int Mode;            // 1 = поток, 2 = плазма, 3 = варп-fBM
uniform vec4 Tint;           // rgb — цвет темы, a — сила перекраски
uniform vec4 ColorModulator; // глобальный множитель (фейд плашки при открытии чата)

out vec4 OutColor;

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;

    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - smoothness, 1.0, dist);
}

// ===== Шейдер 3: warp fBM (iq) =====
const mat2 m2 = mat2(0.80, 0.60, -0.60, 0.80);

float n2(vec2 p) {
    return sin(p.x) * sin(p.y);
}

float fbm4(vec2 p) {
    float f = 0.0;
    f += 0.5000 * n2(p); p = m2 * p * 2.02;
    f += 0.2500 * n2(p); p = m2 * p * 2.02;
    f += 0.1250 * n2(p); p = m2 * p * 2.02;
    f += 0.0625 * n2(p);
    return f / 0.9375;
}

float fbm6(vec2 p) {
    float f = 0.0;
    f += 0.500000 * (0.5 + 0.5 * n2(p)); p = m2 * p * 2.02;
    f += 0.500000 * (0.5 + 0.5 * n2(p)); p = m2 * p * 2.02;
    f += 0.500000 * (0.5 + 0.5 * n2(p)); p = m2 * p * 2.02;
    f += 0.250000 * (0.5 + 0.5 * n2(p));
    return f / 0.96875;
}

vec2 fbm4_2(vec2 p) { return vec2(fbm4(p), fbm4(p + vec2(7.8))); }
vec2 fbm6_2(vec2 p) { return vec2(fbm6(p + vec2(16.8)), fbm6(p + vec2(11.5))); }

float warpFunc(vec2 q, float t, out vec4 ron) {
    // В оригинале коэффициенты времени крошечные (0.03 при 0.27) — на маленькой
    // плашке движение незаметно, поэтому амплитуда/частота усилены и добавлен дрейф.
    q += vec2(t * 0.06, -t * 0.04);
    q += 0.18 * sin(vec2(0.9, 0.8) * t + length(q) * vec2(4.1, 4.3));
    vec2 o = fbm4_2(0.9 * q);
    o += 0.20 * sin(vec2(0.7, 0.8) * t + length(o));
    vec2 n = fbm6_2(3.0 * o + 0.2 * t);
    ron = vec4(o, n);
    float f = 0.5 + 0.5 * fbm4(1.8 * q + 6.0 * n);
    return mix(f, f * f * f * 3.5, f * abs(n.x));
}

// ===== Шейдер 2: плазма =====
vec3 plasma(vec2 uv, float t) {
    for (int i = 1; i < 10; i++) {
        float fi = float(i);
        uv.x += 0.6 / fi * cos(fi * 2.5 * uv.y + t);
        uv.y += 0.6 / fi * cos(fi * 1.5 * uv.x + t);
    }
    return vec3(0.1) / abs(sin(t - uv.y - uv.x));
}

// ===== Шейдер 1: органический поток =====
vec3 flow(vec2 uv, float t) {
    vec2 p = uv * 3.0;
    float amp = 0.5;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        p += amp * vec2(sin(p.y * 1.3 + t * 0.7 + fi * 1.7),
                        cos(p.x * 1.3 - t * 0.6 + fi * 1.3));
        amp *= 0.78;
    }
    float v = 0.5 + 0.5 * sin(p.x * 0.6 + p.y * 0.6 + t * 0.4);
    float w = 0.5 + 0.5 * cos(p.y * 0.5 - p.x * 0.4 - t * 0.3);
    vec3 a = vec3(0.10, 0.32, 0.62);
    vec3 b = vec3(0.85, 0.30, 0.55);
    vec3 c = vec3(0.20, 0.85, 0.80);
    vec3 col = mix(a, b, v);
    col = mix(col, c, w * 0.5);
    col += pow(v, 4.0) * 0.6;
    return col;
}

void main() {
    float clip = ralpha(Size, FragCoord, Radius, Smoothness);
    if (clip <= 0.0) {
        discard;
    }

    vec2 res = Size;
    vec2 fc = FragCoord * Size;
    float t = Time;

    vec3 col;
    if (Mode == 2) {
        vec2 uv = (2.0 * fc - res) / min(res.x, res.y);
        col = plasma(uv, t);
    } else if (Mode == 3) {
        vec2 p = (2.0 * fc - res) / res.y;
        vec4 on = vec4(0.0);
        float f = warpFunc(p, t, on);
        col = vec3(f);
    } else {
        vec2 uv = (2.0 * fc - res) / min(res.x, res.y);
        col = flow(uv, t);
    }

    col = clamp(col, 0.0, 1.0);
    // Перекрашиваем эффект в цвет темы: яркость шейдера задаёт интенсивность,
    // а сам цвет берём из (осветлённого) цвета темы — фон чётко «под тему».
    float lum = dot(col, vec3(0.299, 0.587, 0.114));
    vec3 themed = Tint.rgb * (0.30 + 0.85 * lum);
    col = mix(col, themed, Tint.a);
    col *= 0.95;

    float alpha = clip * FragColor.a;
    OutColor = vec4(col, alpha) * ColorModulator;
}
