#version 150

// Фрагмент батч-градиента. Логика подбора стопа и маски скругления перенесена из
// hud_gradient.fsh дословно; Size/Radius стали varying'ами, параметры градиента
// остались uniform'ами (они и есть ключ группы батча).

in vec2 FragCoord;
in vec4 FragColor;
in vec2 vSize;
in vec4 vRadius;

uniform float Smoothness;
uniform vec4 Meta;      // x: тип (0 — линейный, 1 — радиальный), y: число стопов
uniform vec4 Points;    // линейный: A.xy -> B.xy ; радиальный: центр.xy, радиусы.zw
uniform vec4 Positions; // позиции стопов 0..1
uniform vec4 Stop0;
uniform vec4 Stop1;
uniform vec4 Stop2;
uniform vec4 Stop3;

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

vec4 stopColor(int i) {
    if (i == 0) return Stop0;
    if (i == 1) return Stop1;
    if (i == 2) return Stop2;
    return Stop3;
}

float stopPos(int i) {
    if (i == 0) return Positions.x;
    if (i == 1) return Positions.y;
    if (i == 2) return Positions.z;
    return Positions.w;
}

void main() {
    float mask = ralpha(vSize, FragCoord, vRadius, Smoothness);

    float t;
    if (Meta.x < 0.5) {
        vec2 a = Points.xy;
        vec2 d = Points.zw - a;
        t = clamp(dot(FragCoord - a, d) / max(dot(d, d), 1e-6), 0.0, 1.0);
    } else {
        vec2 delta = (FragCoord - Points.xy) / max(Points.zw, vec2(1e-6));
        t = clamp(length(delta), 0.0, 1.0);
    }

    int count = int(Meta.y + 0.5);
    vec4 col = stopColor(0);
    for (int i = 0; i < 3; i++) {
        if (i + 1 >= count) break;
        float p0 = stopPos(i);
        float p1 = stopPos(i + 1);
        if (t >= p0) {
            float f = clamp((t - p0) / max(p1 - p0, 1e-6), 0.0, 1.0);
            col = mix(stopColor(i), stopColor(i + 1), f);
        }
    }

    vec4 color = vec4(col.rgb, col.a * mask) * FragColor;
    if (color.a == 0.0) {
        discard;
    }

    OutColor = color;
}
