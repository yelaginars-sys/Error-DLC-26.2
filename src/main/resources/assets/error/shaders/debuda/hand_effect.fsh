#version 150

// Процедурные shadertoy-эффекты поверх силуэта руки (стиль «Шейдер»).
// Эффект считается по экранным UV, домножается на alpha захваченной руки.
// Mode: 0 — Plasma, 1 — Galaxy, 2 — Liquid, 3 — Energy.

in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform float Time;
uniform vec2 Resolution;
uniform int Mode;
uniform float Opacity;
uniform vec3 Tint;

out vec4 OutColor;

// Маска силуэта по присутствию цвета руки (Sampler0 = color-attachment).
float hmask(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    return step(0.01, max(c.a, max(c.r, max(c.g, c.b))));
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.0 + vec2(37.0);
        a *= 0.5;
    }
    return v;
}

vec3 plasma(vec2 uv) {
    float v = sin(uv.x * 10.0 + Time)
            + sin(uv.y * 10.0 + Time * 1.3)
            + sin((uv.x + uv.y) * 10.0 + Time * 0.7)
            + sin(length(uv - 0.5) * 20.0 - Time * 1.5);
    v *= 0.25;
    return 0.5 + 0.5 * cos(6.2831 * (v + vec3(0.0, 0.33, 0.67)));
}

vec3 galaxy(vec2 uv) {
    vec2 p = (uv - 0.5) * vec2(Resolution.x / Resolution.y, 1.0) * 3.0;
    float a = atan(p.y, p.x);
    float r = length(p);
    float spiral = fbm(vec2(a * 2.0 + r * 3.0 - Time * 0.3, r * 2.0));
    float stars = pow(hash(floor(uv * Resolution * 0.5)), 40.0);
    vec3 col = mix(vec3(0.1, 0.0, 0.2), vec3(0.4, 0.2, 0.8), spiral);
    col += vec3(stars);
    col *= smoothstep(2.0, 0.0, r);
    return col;
}

vec3 liquid(vec2 uv) {
    vec2 q;
    q.x = fbm(uv * 4.0 + Time * 0.2);
    q.y = fbm(uv * 4.0 + vec2(5.2, 1.3) + Time * 0.15);
    float f = fbm(uv * 4.0 + q * 2.0);
    vec3 col = mix(vec3(0.0, 0.3, 0.5), vec3(0.4, 0.9, 1.0), f);
    col += pow(f, 4.0);
    return col;
}

vec3 energy(vec2 uv) {
    vec2 p = (uv - 0.5) * 2.0;
    float v = 0.0;
    for (int i = 1; i < 5; i++) {
        float fi = float(i);
        v += 0.03 / (abs(sin(p.x * fi * 3.0 + Time) + p.y) + 0.05);
    }
    return vec3(v * 0.6, v * 0.3, v) * 1.2;
}

void main() {
    vec2 uv = TexCoord;
    float mask = hmask(uv);
    if (mask <= 0.003) discard;

    vec3 col;
    if (Mode == 0)      col = plasma(uv);
    else if (Mode == 1) col = galaxy(uv);
    else if (Mode == 2) col = liquid(uv);
    else                col = energy(uv);

    // перекрас эффекта в выбранный цвет (нормируем tint, чтобы не темнить)
    vec3 t = Tint / max(max(Tint.r, Tint.g), max(Tint.b, 0.004));
    col *= t;

    OutColor = vec4(col, mask * Opacity);
}
