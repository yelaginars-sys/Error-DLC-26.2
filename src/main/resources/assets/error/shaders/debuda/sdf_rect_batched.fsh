#version 150

// Фрагмент батч-SDF rect (fill only). Зеркалит fill-часть sdf_rect.fsh, но все
// параметры — per-vertex varying'и (см. .vsh), кроме сглаживания.

in vec2 vLocal;   // позиция фрагмента отн. центра rect, px
in vec2 vHalf;    // полуразмер rect, px
in float vRadius; // скругление, px
in float vBorder; // толщина бордера, px (0 = без бордера)
in vec4 vFill;    // цвет заливки

uniform float Smoothness; // ширина AA-кромки (~1px)
uniform vec4 BorderColor; // цвет бордера (общий на батч-группу)

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
    float dist = sdRoundedRect(vLocal, vHalf, vRadius);
    // Внешняя AA-кромка + внутренняя кромка бордера (как в sdf_rect.fsh).
    float aaOuter = 1.0 - smoothstep(-Smoothness, Smoothness, dist);
    float aaInner = (vBorder > 0.0)
            ? smoothstep(-Smoothness, Smoothness, dist + vBorder)
            : 1.0;
    float fillA   = aaOuter * aaInner;
    float borderA = aaOuter * (1.0 - aaInner);

    vec4 cFill   = vec4(vFill.rgb, vFill.a * fillA);
    vec4 cBorder = vec4(BorderColor.rgb, BorderColor.a * borderA);
    vec4 col = over(cFill, cBorder);
    if (col.a < 0.001) discard;
    OutColor = col;
}
