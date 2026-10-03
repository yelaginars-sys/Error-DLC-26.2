#version 150

// Фрагмент батч-версии скруглённого прямоугольника. Математика 1:1 с
// hud_rect.fsh / mre:rectangle.fsh (rdist/ralpha встроены, без #moj_import —
// include не резолвится на части GPU), отличие только в том, что Size и Radius
// приходят varying'ами, а не uniform'ами.
//
// ColorModulator намеренно НЕ объявлен: глобальный множитель (фейд плашки через
// RenderSystem.setShaderColor) свёрнут в вершинный цвет ещё на этапе постановки в
// очередь — иначе к моменту отложенного флаша он был бы уже другим.

in vec2 FragCoord;
in vec4 FragColor;
in vec2 vSize;
in vec4 vRadius;

uniform float Smoothness; // мягкость кромки — общая на группу батча

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
    float alpha = ralpha(vSize, FragCoord, vRadius, Smoothness);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}
