#version 150

// Порт otap:core/rectangle → isle-client. SDF rounded-rect с per-corner radius
// и per-vertex цветом. rdist/ralpha встроены (были в otap:common.glsl) — логика
// идентична, поэтому вывод пиксель-в-пиксель совпадает со старым otap-рендером.

in vec2 FragCoord; // нормализованная координата фрагмента внутри примитива
in vec4 FragColor;

uniform vec2 Size;           // размер прямоугольника (px)
uniform vec4 Radius;         // радиус на каждый угол
uniform float Smoothness;    // мягкость кромки
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
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color * ColorModulator;
}
