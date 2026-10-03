#version 150

// Фрагмент батч-текстуры. Обе ветки (обычное скругление и сквиркл-маска для
// аватарок) перенесены из hud_texture.fsh дословно; параметры формы пришли
// varying'ами, Sampler0/Smoothness/Aa остались uniform'ами — они общие на группу
// батча (группа = одна текстура).

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;
in vec2 vSize;
in float vRadius;
in float vSquircle;

uniform sampler2D Sampler0;
uniform float Smoothness;
uniform float Aa; // ширина AA-кромки сквиркла в GUI-px (= 1/guiScale), без производных

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
    if (vSquircle > 0.0) {
        // сквиркл (superellipse): форма без плоских граней и углов
        float N = vSquircle;
        vec2 center = vSize * 0.5;
        vec2 p = FragCoord * vSize - center;
        float A = center.x - 1.0;
        float se = pow(pow(abs(p.x), N) + pow(abs(p.y), N), 1.0 / N);
        float d = se - A;
        float aa = max(Aa, 0.0001);
        alpha = 1.0 - smoothstep(-aa, aa, d);
    } else {
        alpha = ralpha(vSize, FragCoord, vec4(vRadius), Smoothness);
    }

    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, TexCoord) * FragColor;

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}
