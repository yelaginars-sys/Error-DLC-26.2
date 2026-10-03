#version 150

// Самодостаточный (без #moj_import): rdist/ralpha встроены, как в hud_rect. Логика
// 1:1 с прежним include-вариантом → вывод пиксель-в-пиксель тот же.

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Smoothness; // edge smoothness;
uniform float Aa;         // ширина AA-кромки сквиркла в GUI-px (= 1/guiScale ≈ fwidth), без производных
uniform vec4 ColorModulator; // глобальный множитель (RenderSystem.setShaderColor) — фейд плашки

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

void main() {
    float alpha;
    if (Radius.x < 0.0) {
        // сквиркл-маска (superellipse): показатель N = -Radius.x; форма без плоских граней и углов
        float N = -Radius.x;
        vec2 center = Size * 0.5;
        vec2 p = FragCoord * Size - center;
        float A = center.x - 1.0;
        float se = pow(pow(abs(p.x), N) + pow(abs(p.y), N), 1.0 / N);
        float d = se - A;
        float aa = max(Aa, 0.0001);  // derivative-free (было fwidth(d)) — работает на слабых GPU
        alpha = 1.0 - smoothstep(-aa, aa, d);
    } else {
        alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    }
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, TexCoord) * FragColor;

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color * ColorModulator;
}
