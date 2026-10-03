#version 150

// self-contained (без #moj_import — include ломался на части GPU): rdist встроен
float rdist(vec2 pos, vec2 size, vec4 radius) {
    radius.xy = (pos.x > 0.0) ? radius.xy : radius.wz;
    radius.x  = (pos.y > 0.0) ? radius.x : radius.y;
    vec2 v = abs(pos) - size + radius.x;
    return min(max(v.x, v.y), 0.0) + length(max(v, 0.0)) - radius.x;
}

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size; // rectangle size
uniform vec4 Radius; // radius for each vertex
uniform float Thickness; // border thickness
uniform vec2 Smoothness; // internal and external edge smoothness
uniform vec4 ColorModulator; // глобальный множитель (RenderSystem.setShaderColor) — фейд ClickGui

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist = rdist(center - (FragCoord.xy * Size), center - 1.0, Radius);
    float alpha = smoothstep(1.0 - Thickness - Smoothness.x - Smoothness.y,
        1.0 - Thickness - Smoothness.y, dist); // internal edge
    alpha *= 1.0 - smoothstep(1.0 - Smoothness.y, 1.0, dist); // external edge
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color * ColorModulator;
}