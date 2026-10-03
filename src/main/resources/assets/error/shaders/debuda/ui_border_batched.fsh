#version 150

// Фрагмент батч-обводки. Формулы внутренней/внешней кромки 1:1 с mre:border.fsh,
// параметры пришли varying'ами вместо uniform'ов.

in vec2 FragCoord;
in vec4 FragColor;
in vec2 vSize;
in float vRadius;
in float vThickness;
in vec2 vSmoothness;

out vec4 OutColor;

float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;

    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

void main() {
    vec2 center = vSize * 0.5;
    vec4 radius = vec4(vRadius);
    float dist = rdist(center - (FragCoord * vSize), center - 1.0, radius);

    float alpha = smoothstep(1.0 - vThickness - vSmoothness.x - vSmoothness.y,
                             1.0 - vThickness - vSmoothness.y, dist);   // внутренняя кромка
    alpha *= 1.0 - smoothstep(1.0 - vSmoothness.y, 1.0, dist);          // внешняя кромка

    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}
